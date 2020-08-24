package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.io.File.listRoots;

public class DocumentStoreImpl implements DocumentStore {
    final private DocumentPersistenceManager dpm;
    final private BTreeImpl<URI, Document> bTree; //primary storage structure for this Document Store
    final private TrieImpl<URI> trie; //keyword search, case insensitive
    final private MinHeapImpl<UriTimeVault> minHeap; //tracks memory
    final private StackImpl<Undoable> commandStack; //holds the undo commands

    HashMap<URI, Boolean> inMemory;
    HashMap<URI, Boolean> onDisk;

    final private File baseDir;
    private int maxDocumentCount;
    private int maxByteCount;
    private int documentCount;
    private int byteCount;

    public DocumentStoreImpl() {
        //sets baseDir to user home directory
        this.dpm = new DocumentPersistenceManager();
        this.baseDir = new File(System.getProperty("user.dir"));

        //creates a btree with new URI as its sentinel (least value)
        this.bTree = new BTreeImpl<>();
        try {
            this.bTree.put(new URI(null, null, null, null, null), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.bTree.setPersistenceManager(this.dpm);

        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.maxDocumentCount = this.maxByteCount = Integer.MAX_VALUE; //starts with no limit
        this.documentCount = this.byteCount = 0;

        this.inMemory = new HashMap<>();
        this.onDisk = new HashMap<>();
    }

    public DocumentStoreImpl(File baseDir) {
        //sets baseDir to root/baseDir
        this.dpm = new DocumentPersistenceManager(baseDir);
        //todo set this.baseDir to whatever the constructor of dpm ends up doing (root issue)
        //this.baseDir = new File(System.getProperty("user.dir") + baseDir);
        this.baseDir = new File(listRoots()[0].toString() + baseDir);
        //creates a btree with new URI as its sentinel (least value)
        this.bTree = new BTreeImpl<>();
        try {
            this.bTree.put(new URI(null, null, null, null, null), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.bTree.setPersistenceManager(this.dpm);

        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.maxDocumentCount = this.maxByteCount = Integer.MAX_VALUE; //starts with no limit
        this.documentCount = this.byteCount = 0;

        this.inMemory = new HashMap<>();
        this.onDisk = new HashMap<>();
    }

    /**
     * @param input  the document being put
     * @param uri    unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the String version of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     */
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (uri == null || format == null) { throw new IllegalArgumentException(); }

        boolean wasOnDisk = this.isOnDisk(uri);

        //we have to make a document
        DocumentImpl doc = this.makeDocument(input, uri, format); //returns null if input is null
        //case 1: @doc is null: we are deleting the value @uri key.
        if (doc == null) {
            return this.docIsNull(uri);
        }
        //@326: if the total doc byte count is in itself larger than the maxByteCount limit, don't add, and don't remove anything.
        if (doc.getDocumentByteCount() > this.maxByteCount) {
            return 0;
        }
        //add the doc to the bTree
        DocumentImpl returnedDoc = (DocumentImpl) this.bTree.put(uri, doc);
        //1. we added a new Document: @returnedDoc is null
        if (returnedDoc == null) {
            //fully add this new document
            this.totallyNewAdd(doc);

            Function<URI, Boolean> undoAdd = addedURI -> {
                boolean addUndone = this.bTree.put(addedURI, null).equals(doc);
                if (addUndone) {
                    //fully remove the doc which was added
                    this.fullyRemove(doc);
                }
                return addUndone;
            };
            this.pushCommand(uri, undoAdd);
            return doc.hashCode();
        }
        //2. we replaced a document: (doc is the current value at @uri key, and the replaced doc is returnedDoc)
        else if (!returnedDoc.equals(doc)) {
            //fully remove the returnedDoc
            this.fullyRemove(returnedDoc);
            //fully add the new doc @doc
            this.totallyNewAdd(doc);
            //then make function that undoes everything above.
            Function<URI, Boolean> undoReplace = replacedURI -> {
                boolean replaced = this.bTree.put(replacedURI, returnedDoc).equals(doc);
                if (replaced) {
                    //fully remove the new doc which replaced
                    this.fullyRemove(doc);
                    //fully add the doc which was replaced
                    this.totallyNewAdd(returnedDoc);
                }
                return replaced; };
            pushCommand(uri, undoReplace);
            return returnedDoc.hashCode();
        }
        //3. we're trying to put an existing <uri, doc> pair: ie, returnedDoc == doc
        else {
            //1. if doc was on disk, then we want to treat it as a replace since bringing it back to memory might push the count over the limit.
            if (this.onDisk.get(uri)) {
                //undoing this type of action: where all we did was bring something back into memory, undoing this would send it back to disk which we dont want/need, so I think an empty command should be pushed.
                this.addToMemory(doc);
            }
            else if (this.inMemory.get(uri)) {
                //2. doc was in bTree memory
                //update the time
                doc.setLastUseTime(System.nanoTime());
                this.minHeap.reHeapify(doc.getVault());
                //but keep everything as it is since nothing was changed.
            }
            //push an empty command
            this.pushCommand(null, null);
            return doc.hashCode();
        }
    }

    protected boolean isOnDisk(URI uri) {
        File location = new File(this.baseDir + uri.getSchemeSpecificPart() + ".json");
        return location.exists();
    }

    private void updateByteCount(DocumentImpl newDoc, DocumentImpl returnedDoc) {
        int newDocByteCount = newDoc.getDocumentByteCount();
        int returnedDocByteCount = returnedDoc.getDocumentByteCount();
        // if they're equal: do nothing - but chances are slim, so lets not test for it.
        if (returnedDocByteCount > newDocByteCount) {
            //[the document that replaced (@newDoc) the old document (@returnedDoc) is smaller in byte size]
            //decrement the difference between the two
            this.byteCount -= returnedDocByteCount - newDocByteCount;
        }
        if (newDocByteCount > returnedDocByteCount) {
            //[the document that replaced (@newDoc) the old document (@returnedDoc) is larger in byte size]
            //increment the difference between the two
            this.byteCount += newDocByteCount - returnedDocByteCount;
            //now the byte count can be more than the limit:
            checkMemoryLimit();
        }
    }

    private void incrementCounts(DocumentImpl doc) {
        //increment counts
        this.byteCount += doc.getDocumentByteCount();
        this.documentCount += 1;
        //todo
        this.checkMemoryLimit();
    }

    private void decrementCounts(int docByteCount) {
        //decrement counts
        this.byteCount -= docByteCount;
        this.documentCount -= 1;
    }

    //for documents being brought back from disk
    private void addToMemory(DocumentImpl doc) {
        //add to heap - insert
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.insert(doc.getVault());
        //update memory maps
        this.inMemory.put(doc.getKey(), true);
        this.onDisk.put(doc.getKey(), false);
        //decrement counts
        this.incrementCounts(doc);
    }

    //for documents that are moved to disk
    private void removeFromMemory(DocumentImpl doc) {
        if (this.onDisk.get(doc.getKey())) {
            return;
        }
        //remove from heap - make doc the minimum, then reHeapify, then remove min.
        doc.setLastUseTime(Long.MIN_VALUE);
        this.minHeap.reHeapify(doc.getVault());
        this.minHeap.removeMin();
        //update the memory maps
        this.inMemory.put(doc.getKey(), false);
        this.onDisk.put(doc.getKey(), true);
        //decrement counts
        this.decrementCounts(doc.getDocumentByteCount());
    }

    //for docs that are newly added ie: undo delete, undo replace, add new doc
    private void totallyNewAdd(DocumentImpl doc) {
        this.addToMemory(doc);
        this.addToTrie(doc);
    }

    //for docs being fully removed ie: delete or replace
    private void fullyRemove(DocumentImpl doc) {
        //a doc can either be in memory or on disk
        //remove from memory if it is in memory
        if (this.inMemory.get(doc.getKey())) {
            this.removeFromMemory(doc);
        }
        //remove from disk if it is on disk
        else if (this.onDisk.get(doc.getKey())) {
            this.onDisk.put(doc.getKey(), false);
        }
        //then remove from trie no matter what
        this.removeFromTrie(doc);
    }

    private void addToTrie(DocumentImpl newDoc) {
        HashMap<String,Integer> wordCount = newDoc.getWordCountHashMap();
        for(String word : wordCount.keySet()) {
            this.trie.put(word, newDoc.getKey());
        }
    }

    private void removeFromTrie(DocumentImpl deletedDoc) {
        Set<String> words = deletedDoc.getWordMap().keySet();
        for(String word : words) {
            this.trie.delete(word, deletedDoc.getKey());
        }
    }

    private void pushCommand(URI uri, Function<URI, Boolean> undo) {
        if (undo == null) {
            //makes an empty function for when public calls to theDocStore are made but nothing is changed
            undo = emptyURI -> true;
        }
        GenericCommand<URI> cmd = new GenericCommand<>(uri, undo);
        this.commandStack.push(cmd);
    }

    private DocumentImpl makeDocument(InputStream input, URI uri, DocumentFormat format) {
        if (input == null) {
            return null;
        }
        DocumentImpl newDoc = new DocumentImpl();
        try{
            //convert the byte array
            byte[] bArray = new byte[8192];
            ByteArrayOutputStream writer = new ByteArrayOutputStream();
            int amountOfBytes;
            while ((amountOfBytes = input.read(bArray)) != -1) {
                writer.write(bArray, 0, amountOfBytes);
            }
            byte[] bytes = writer.toByteArray();

            //now make a Document object based on the type of format
            switch (format) {
                case TXT:
                    String text = new String(bytes);
                    newDoc = new DocumentImpl(uri, text, text.hashCode());
                    break;
                case PDF:
                    //use PDF box to use 'input' to read the content of the PDF given
                    PDDocument pdf = PDDocument.load(bytes);
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    stripper.setStartPage(1);
                    stripper.setEndPage(1);
                    String pdfText = stripper.getText(pdf).trim();
                    pdf.close();
                    newDoc = new DocumentImpl(uri, pdfText, pdfText.hashCode(), bytes);
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return newDoc;
    }

    /*we need this method since we have to @return the hashcode of the old value being deleted.
     therefore, we need to temporarily hold on to its text while the entry is being deleted. */
    private int docIsNull(URI uri) {
        DocumentImpl doc = (DocumentImpl) this.bTree.get(uri);

        boolean isOnDisk = this.onDisk.getOrDefault(uri, Boolean.FALSE);
        if (doc != null) {
            if (isOnDisk) {
                try {
                    this.bTree.moveToDisk(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //save the hashCode
            int deletedDocumentHashcode = doc.hashCode();
            //use the public delete() method to delete the document
            if (this.deleteDocument(uri)) {
                return deletedDocumentHashcode;
            }
        }
        //since input is null, and the uri is not in the hashtable (via the above if statement),
        // ie: trying to "put" a new key with null value
        return 0;
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as a PDF, or null if no document exists with that URI
     */
    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        DocumentImpl doc = this.getDocNoProblem(uri, System.nanoTime());
        if (doc != null) {
            return doc.getDocumentAsPdf();
        }
        return null;
    }

    private DocumentImpl getDocNoProblem(URI uri, long currentTime) {
        DocumentImpl doc = (DocumentImpl) this.bTree.get(uri);
        if (doc != null) {
            if (this.inMemory.get(uri)) {
                //update time & reHeapify
                doc.setLastUseTime(currentTime);
                this.minHeap.reHeapify(doc.getVault());
            }
            if (this.onDisk.get(uri)) {
                this.addToMemory(doc);
            }
            return doc;
        }
        return null;
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as TXT, i.e. a String, or null if no document exists with that URI
     */
    @Override
    public String getDocumentAsTxt(URI uri) {
        DocumentImpl doc = this.getDocNoProblem(uri, System.nanoTime());
        if (doc != null) {
            return doc.getDocumentAsTxt();
        }
        return null;
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        DocumentImpl returnedDoc = (DocumentImpl) this.bTree.put(uri, null);
        //1. there was no value @uri in btree: returnedDoc is null
        if(returnedDoc == null) {
            //don't increment counts or check limits
            //don't add to trie
            //don't add to minheap
            //push an empty command onto stack
            this.pushCommand(null, null);
            return false;
        }
        //2. there was a value @uri in bTree: we deleted the value (whether from on disk or not) which is now @returnedDoc (deletedValue)
        else {
            //fully remove the document
            this.fullyRemove(returnedDoc);

            //undo logic:
            Function<URI, Boolean> undoDelete = deletedURI -> {
                boolean added = this.bTree.put(deletedURI, returnedDoc) == null;
                if (added) {
                    //fully add the deleted doc back
                    this.totallyNewAdd(returnedDoc);
                }
                return added;
            };
            this.pushCommand(uri, undoDelete);
            return true;
        }
    }


    /**
     * undo the last put or delete that was done with the given URI as its key
     *
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    @Override
    public void undo(URI uri) throws IllegalStateException {
        checkStackNotEmpty();//throws Exc if stack is initially null
        //TODO remove this method probably unnecessary and inefficient
        this.stackContainsURI(uri, false, 0);
        this.undo(uri, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean stackContainsURI(URI uri, boolean contains, int stackNumber) throws IllegalStateException{
        Undoable temp = this.commandStack.pop();
        if (temp instanceof CommandSet && !contains) {
            contains = ((CommandSet) temp).containsTarget(uri);
        }
        else if (!contains) {
            contains = ((GenericCommand) temp).getTarget() == uri;
        }
        if (this.commandStack.size() > 0 && !contains) {
            contains = this.stackContainsURI(uri, contains, stackNumber + 1);
        }
        this.commandStack.push(temp);
        if (stackNumber == 0 && !contains) {
            throw new IllegalStateException();
        }
        return contains;
    }

    @SuppressWarnings("unchecked")
    private void undo(URI uri, boolean undoneInSet) {
        Undoable temp = this.commandStack.pop();
        boolean isCmdSet = temp instanceof CommandSet;
        //Base case
        if (isCmdSet) {
            CommandSet<URI> tempSet = (CommandSet<URI>) temp;
            if (tempSet.undo(uri)) {
                //if the command set object is empty pop it off the command stack. if its not, put it back in its place.
                undoneInSet = true;
                temp = tempSet;
                if (tempSet.size() == 0) {
                    //return gets rid of the temp variable in this stack frame (since the commandSet is empty
                    return;
                }
            }
        }
        //the command is a generic command
        else if (((GenericCommand<URI>) temp).getTarget() == uri) {
            temp.undo();
            return;
        }
        //recursively .pop()s each command off the stack, calls .undo() on the most recent one with the given URI
        if (!undoneInSet && this.commandStack.size() > 0) {
            this.undo(uri, undoneInSet);
        }
        //then recursively .push()es each command back onto the stack
        this.commandStack.push(temp);
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        this.checkStackNotEmpty();
        boolean undone = this.commandStack.pop().undo();
        if (!undone) {
            throw new IllegalArgumentException("undo did not work");
        }
    }

    private void checkStackNotEmpty()throws IllegalStateException {
        if (this.commandStack.size() == 0) {
            throw new IllegalStateException();
        }
    }

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<String> search(String keyword) {
        long time = System.nanoTime();
        List<URI> sortedDocURIList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<String> docTexts = new ArrayList<>();
        for(URI uri : sortedDocURIList) {
            docTexts.add(this.getDocNoProblem(uri, time).getDocumentAsTxt());
        }
        return docTexts;
    }

    private Comparator<URI> comparatorDescending(String keyword) {
        Comparator<URI> comp = (uri1, uri2) -> {
            DocumentImpl doc1 = (DocumentImpl) this.getDocument(uri1);
            DocumentImpl doc2 = (DocumentImpl) this.getDocument(uri2);
            //will fix reprecussions in method that used this.
            int doc1Count = doc1.wordCount(keyword);
            int doc2Count = doc2.wordCount(keyword);
            return Integer.compare(doc1Count, doc2Count);
        };
        //descending order - as per assignment
        return comp.reversed();
    }

    /**
     * same logic as search, but returns the docs as PDFs instead of as Strings
     *
     * @param keyword
     */
    @Override
    public List<byte[]> searchPDFs(String keyword) {
        long time = System.nanoTime();
        List<URI> sortedDocURIList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<byte[]> docBytes = new ArrayList<>();
        for(URI uri : sortedDocURIList) {
            docBytes.add(this.getDocNoProblem(uri, time).getDocumentAsPdf());
        }
        return docBytes;
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     *
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<String> searchByPrefix(String keywordPrefix) {
        List<DocumentImpl> docs = this.searchDocumentByPrefix(keywordPrefix);
        return docs.stream()
                .map(DocumentImpl::getDocumentAsTxt)
                .collect(Collectors.toList());
    }

    private List<DocumentImpl> searchDocumentByPrefix(String keywordPrefix) {
        long time = System.nanoTime();

        List<URI> docUris = this.trie.getAllWithPrefixSorted(keywordPrefix, this.comparatorDescending(keywordPrefix));
        HashMap<DocumentImpl, Integer> mapDocToPrefixCount = new HashMap<>();
        ArrayList<DocumentImpl> docs = new ArrayList<>(docUris.size());
        for(URI uri : docUris) {
            DocumentImpl doc = this.getDocNoProblem(uri, time);
            mapDocToPrefixCount
                    .put(doc, this.getPrefixCount(doc, keywordPrefix));
            docs.add(doc);
        }
        Comparator<DocumentImpl> comparator =  (doc1, doc2) -> {
            int d1 = mapDocToPrefixCount.get(doc1);
            int d2 = mapDocToPrefixCount.get(doc2);
            //descending order as per assignment
            return Integer.compare(d2, d1);
        };
        return docs;
    }

    private int getPrefixCount(DocumentImpl doc, String prefix) {
        prefix = prefix.toUpperCase();

        HashMap<String, Integer> wordCount = doc.getWordCountHashMap();

        //to be used with comparator for prefix methods
        int prefixCount = 0;
        for(String word : wordCount.keySet()) {
            word = word.toUpperCase();
            for (int c = 0; c < prefix.length(); c++) {
                //compare each character of all words in doc to the prefix.
                if(word.length() >= prefix.length()) {
                    if (word.charAt(c) != prefix.charAt(c)) {
                        break;
                    }
                }
                //if we reached the end of the prefix, that means its a
                // match - add @word to the collection to be returned (as an array)
                if (c == prefix.length() - 1) {
                    //word is a prefix
                    prefixCount += doc.wordCount(word);
                }
            }
        }
        return prefixCount;
    }

    /**
     * same logic as searchByPrefix, but returns the docs as PDFs instead of as Strings
     *
     * @param keywordPrefix
     */
    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        List<DocumentImpl> docs = this.searchDocumentByPrefix(keywordPrefix);
        return docs.stream()
                .map(DocumentImpl::getDocumentAsPdf)
                .collect(Collectors.toList());
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     *
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        //the set to be returned containing the deleted URIs
        HashSet<URI> deletedURIs = new HashSet<>();
        //get all of the Documents containing the word
        List<URI> allURIs = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        if (allURIs.isEmpty()) {
            //push an empty command
            pushCommand(null, null);
            return deletedURIs;
        }
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = this.deleteDocReturnUndoCommand(allURIs);
        this.commandStack.push(cmdSet);
        //return the deleted URI set
        deletedURIs.addAll(allURIs);
        return deletedURIs;
    }

    private CommandSet<URI> deleteDocReturnUndoCommand(Collection<URI> c) {
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = new CommandSet<>();
        for (URI uri : c) {
            DocumentImpl doc = (DocumentImpl) this.bTree.put(uri, null);
            this.fullyRemove(doc);
            //make a GenCMD
            Function<URI, Boolean> undoDelete = deletedURI -> {
                boolean added = this.bTree.put(deletedURI, doc) == null;
                if (added) {
                    this.totallyNewAdd(doc);
                }
                return added;
            };
            cmdSet.addCommand(new GenericCommand<>(doc.getKey(), undoDelete));
        }
        return cmdSet;
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE INSENSITIVE.
     *
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        //the set to be returned containing the deleted URIs
        HashSet<URI> deletedURIs = new HashSet<>();
        //delete all with prefix from trie && get all of the Documents containing the word
        Set<URI> allURIsWithPrefix = this.trie.deleteAllWithPrefix(keywordPrefix);
        //case: nothing was deleted - push empty command adn return empty set.
        if (allURIsWithPrefix.isEmpty()) {
            pushCommand(null, null);
            return deletedURIs;
        }
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = this.deleteDocReturnUndoCommand(allURIsWithPrefix);
        //push the CommandSet onto the stack.
        this.commandStack.push(cmdSet);
        //return the deleted URI set
        deletedURIs.addAll(allURIsWithPrefix);
        return deletedURIs;
    }

    /**
     * set maximum number of documents that may be stored
     *
     * @param limit
     */
    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit < 0) throw new IllegalArgumentException("Cannot set negative limit");
        this.maxDocumentCount = limit;
        checkMemoryLimit();
    }

    //increment counts before use of this method
    private void checkMemoryLimit() {
        while (this.documentCount > this.maxDocumentCount || this.byteCount > this.maxByteCount)  {
            //remove from heap
            UriTimeVault lastUsedDoc = this.minHeap.removeMin();
            //move to disk

            try {
                this.bTree.moveToDisk(lastUsedDoc.getKey());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //update memory maps
            this.onDisk.put(lastUsedDoc.getKey(), true);
            this.inMemory.put(lastUsedDoc.getKey(), false);

            //decrement the counts
            this.decrementCounts(lastUsedDoc.getByteCount());
        }
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     *
     * @param limit
     */
    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) throw new IllegalArgumentException("Cannot set negative limit");
        this.maxByteCount = limit;
        checkMemoryLimit();
    }

    protected Document getDocument(URI uri) {
        DocumentImpl doc = (DocumentImpl) this.bTree.get(uri);
        if (doc == null) {
            System.out.println("doc was not in memory");
            return null;
        }
        if (this.onDisk.get(uri)) {
            try {
                this.bTree.moveToDisk(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    protected int getCommandStackSize() {
        return this.commandStack.size();
    }

    protected int getDocumentCount() {
        return this.documentCount;
    }
}
