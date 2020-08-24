package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DocumentStoreImpl implements DocumentStore {
    final private HashTableImpl<URI, DocumentImpl> hashTable;
    final private StackImpl<Undoable> commandStack;
    final private TrieImpl<DocumentImpl> trie;
    final private MinHeapImpl<DocumentImpl> minHeap;

    private int maxDocumentCount;
    private int maxByteCount;
    private int documentCount;
    private int byteCount;

    public DocumentStoreImpl() {
        this.hashTable = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.maxDocumentCount = this.maxByteCount = Integer.MAX_VALUE; //no limit
        this.documentCount = this.byteCount = 0;
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
        //if the input is null but the key exists in the hashtable already, delete the entire entry completely.
        if (input == null) { return streamIsNull(uri); }
        DocumentImpl newDoc = makeDocument(input, uri, format);
        //@326: if the total doc byte count is in itself larger than the maxByteCount limit, don't add, and don't remove anything.
        if (newDoc.getDocumentByteCount() > this.maxByteCount) {
            return 0;
        }
        /*if the key exists in the hashtable already and:
          1)the values are the same, then the hashtable will return the value.
          2)the values are different, then the hashtable with return the old value and we call hashcode on that.
          */
        DocumentImpl returnedDoc = this.hashTable.put(uri, newDoc);
        /*in the above line, if the newDoc isn't the same as the returnedDoc, then it was replaced
          and the value returned by the hashtable.put() is that of the replaced (old/deleted) Doc.
          Therefore we need to make a Command object if we want to undo the replace.
          So first we check if they are not the same Document (ie: a replace took place):
          */
        if (returnedDoc == null){
            //a new (key, value) pair was added if returnedDoc was null
            // So return the hashcode of the added value (in this case newDoc)
            this.addToTrie(newDoc);
            //increment counts & check limits
            this.incrementCounts(newDoc); //method also checks limits
            //update time & add to heap
            this.addToHeap(newDoc);

            Function<URI, Boolean> undoAdd = addedURI -> {
                DocumentImpl addedDoc = this.hashTable.get(addedURI);
                if (addedDoc == null) {
                    return false;
                }
                boolean addUndone = addedDoc.equals(this.hashTable.put(addedURI, null));
                if (addUndone) {
                    this.removeFromTrie(addedDoc);
                    //decrement counts
                    this.decrementCounts(addedDoc);
                    //remove from heap
                    this.removeFromHeap(addedDoc);
                }
                return addUndone;
            };
            pushCommand(uri, undoAdd);
            return newDoc.hashCode();
        }
        else if (!returnedDoc.equals(newDoc)) {
            /*[replace took place]
             * if we are in this if statement body, that means that we just added @newDoc to the hashTable in place of, (deleted) @returnedDoc.
             * */
            //update trie accordingly:
            this.removeFromTrie(returnedDoc);
            this.addToTrie(newDoc);
            //update heap accordingly:
            this.removeFromHeap(returnedDoc);
            this.addToHeap(newDoc); //also updates time.
            //DocCount remains the same since it was only a replace, but we have to update ByteCount & check limit
            this.updateByteCount(newDoc, returnedDoc);
            //then make function that undoes everything above.
            Function<URI, Boolean> undoReplace = replacedURI -> {
                /*method body logic of how to undo the replace goes here:
                 @return true is the undo worked, @return false if not.
                 */
                boolean replaced = this.hashTable.put(replacedURI, returnedDoc).equals(newDoc);
                if (replaced) {
                    //update trie accordingly:
                    this.removeFromTrie(newDoc);
                    this.addToTrie(returnedDoc);
                    //update heap accordingly:
                    this.removeFromHeap(newDoc);
                    this.addToHeap(returnedDoc);
                    //DocCount remains the same since it is only a replace, but we have to update ByteCount & check limit
                    this.updateByteCount(returnedDoc, newDoc);
                }
                return replaced; };
            pushCommand(uri, undoReplace);
            return returnedDoc.hashCode();
        } else {
            //else, since they're the same doc, it was accessed -> update time and reHeapify (time updated above). no need to update counts
            returnedDoc.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(returnedDoc);
            //push an empty command
            this.pushCommand(uri, null);
            //return the hashcode
            return returnedDoc.hashCode();
        }
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

    //increment counts before use of this method
    private void checkMemoryLimit() {
        while (this.documentCount > this.maxDocumentCount || this.byteCount > this.maxByteCount)  {
            this.removeAllInstances();
        }
    }

    //removes @doc from hashtable, trie, heap, and from any undos in commandStack.
    private void removeAllInstances() {
        //remove from heap
        DocumentImpl lastUsedDoc = this.minHeap.removeMin();
        //remove from hashTable
        this.hashTable.put(lastUsedDoc.getKey(), null);
        //remove from trie
        this.removeFromTrie(lastUsedDoc);
        //remove from all commands in stack
        this.checkStackNotEmpty();//precaution before using removeFromStack()
        this.removeFromStack(lastUsedDoc.getKey());
        //decrement the counts
        this.decrementCounts(lastUsedDoc);
    }

    private void decrementCounts(DocumentImpl doc) {
        //decrement counts
        this.byteCount -= doc.getDocumentByteCount();
        this.documentCount -= 1;
    }

    private void removeFromHeap(DocumentImpl doc) {
        //remove from heap - make doc the minimum, then reHeapify, then remove min.
        doc.setLastUseTime(Long.MIN_VALUE);
        this.minHeap.reHeapify(doc);
        this.minHeap.removeMin();
    }

    private void incrementCounts(DocumentImpl doc) {
        //decrement counts
        this.byteCount += doc.getDocumentByteCount();
        this.documentCount += 1;
        checkMemoryLimit();
    }

    private void addToHeap(DocumentImpl doc) {
        //add to heap - insert
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.insert(doc);
    }

    @SuppressWarnings("unchecked")
    private void removeFromStack(URI uri) {
        Undoable temp = this.commandStack.pop();
        boolean isCommandSet = temp instanceof CommandSet;
        //false if temp is a genericCommand, and true if it is a commandSet

        if (isCommandSet) {
            ((CommandSet<URI>) temp).removeIf(uriGenericCommand -> uriGenericCommand.getTarget() == uri);
        }
        if (this.commandStack.size() > 0) {
            //recursively pop all elements off the stack
            this.removeFromStack(uri);
        }
        //if it is a command set
        if (isCommandSet) {
            if (((CommandSet<URI>) temp).size() == 0) {
                return; //don't push back onto stack
            }
        }
        //else, temp is a genericCommand
        else if (((GenericCommand<URI>) temp).getTarget() == uri) {
            return; //don't push back onto stack
        }
        //recursively push all elements back on
        this.commandStack.push(temp);
    }

    private void addToTrie(DocumentImpl newDoc) {
        HashMap<String,Integer> wordCount = newDoc.getWordCountHashMap();
        for(String word : wordCount.keySet()) {
            this.trie.put(word, newDoc);
        }
    }

    private void removeFromTrie(DocumentImpl deletedDoc) {
        Set<String> words = deletedDoc.getWordCountHashMap().keySet();
        for(String word : words) {
            this.trie.delete(word, deletedDoc);
        }
    }

    /*we need this method since we have to @return the hashcode of the old value being deleted.
     therefore, we need to temporarily hold on to its text while the entry is being deleted. */
    private int streamIsNull(URI uri) {
        if(this.hashTable.get(uri) != null) {
            //[delete is taking place here]  the trie updating will take place in this.deleteDocument() method
            String deletedDocumentAsTxt = getDocumentAsTxt(uri);
            if (deleteDocument(uri)) {
                return deletedDocumentAsTxt.hashCode();
            }
        }
        //since input is null, and the uri is not in the hashtable (via the above if statement),
        // ie: trying to "put" a new key with null value
        return 0;
    }

    private DocumentImpl makeDocument(InputStream input, URI uri, DocumentFormat format) {
        DocumentImpl newDoc = new DocumentImpl();
        try{
            byte[] bytes = this.toBytes(input);
            switch (format) {
                case TXT:
                    newDoc = this.isText(bytes, uri);
                    break;
                case PDF:
                    newDoc = this.isPDF(bytes, uri);
                    break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return newDoc;
    }

    private byte[] toBytes(InputStream input) throws IOException {
        byte[] bytes = new byte[8192];
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        int amountOfBytes;
        while ((amountOfBytes = input.read(bytes)) != -1) {
            writer.write(bytes, 0, amountOfBytes);
        }
        return writer.toByteArray();
    }

    private DocumentImpl isPDF(byte[] bytes, URI uri) throws IOException {
        //use PDF box to use 'input' to read the content of the PDF given
        PDDocument pdf = PDDocument.load(bytes);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text = stripper.getText(pdf).trim();
        pdf.close();
        return new DocumentImpl(uri, text, text.hashCode(), bytes);
    }

    private DocumentImpl isText(byte[] bytes, URI uri) {
        String text = new String(bytes);
        return new DocumentImpl(uri, text, text.hashCode());
    }


    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as a PDF, or null if no document exists with that URI
     */
    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        DocumentImpl doc = this.hashTable.get(uri);
        if (doc == null) {
            return null;
        }
        //update time & reHeapify
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.reHeapify(doc);
        return doc.getDocumentAsPdf();
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as TXT, i.e. a String, or null if no document exists with that URI
     */
    @Override
    public String getDocumentAsTxt(URI uri) {
        if (uri == null) return null;
        DocumentImpl doc = this.hashTable.get(uri);
        if (doc == null) {
            return null;
        }
        //update time & reHeapify
        doc.setLastUseTime(System.nanoTime());
        this.minHeap.reHeapify(doc);
        return doc.getDocumentAsTxt();
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        DocumentImpl doc = this.hashTable.get(uri);
        if (doc == null) {
            //push an empty command
            pushCommand(uri, null);
            return false;
        }
        //[delete is taking place] time doesn't have to be updated
        GenericCommand<URI> genCmd = this.deleteDocReturnUndoCommand(doc);
        this.commandStack.push(genCmd);
        return this.hashTable.get(uri) == null;
    }

    private void pushCommand(URI uri, Function<URI, Boolean> undo) {
        if (undo == null) {
            //makes an empty function for when public calls to theDocStore are made but nothing is changed
            undo = emptyURI -> true;
        }
        GenericCommand<URI> cmd = new GenericCommand<>(uri, undo);
        this.commandStack.push(cmd);
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    //all time updates happen in the undo function
    @Override
    public void undo() throws IllegalStateException {
        checkStackNotEmpty();
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
     * undo the last put or delete that was done with the given URI as its key
     *
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    //all time updates happen in the undo function
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
        List<DocumentImpl> sortedDocList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<String> docTexts = new ArrayList<>();
        for(DocumentImpl doc : sortedDocList) {
            docTexts.add(doc.getDocumentAsTxt());
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(doc);
        }
        return docTexts;
    }

    private Comparator<DocumentImpl> comparatorDescending(String keyword) {
         Comparator<DocumentImpl> comp = (doc1, doc2) -> {
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
        List<DocumentImpl> sortedDocList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<byte[]> docTexts = new ArrayList<>();
        for(DocumentImpl doc : sortedDocList) {
            docTexts.add(doc.getDocumentAsPdf());
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(doc);
        }
        return docTexts;
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
        List<DocumentImpl> docs = this.trie.getAllWithPrefixSorted(keywordPrefix, this.comparatorDescending(keywordPrefix));
        HashMap<DocumentImpl, Integer> mapDocToPrefixCount = new HashMap<>();
        for(DocumentImpl document : docs) {
            mapDocToPrefixCount
                    .put(document, this.getPrefixCount(document, keywordPrefix));
        }
        Comparator<DocumentImpl> comparator =  (doc1, doc2) -> {
            int d1 = mapDocToPrefixCount.get(doc1);
            int d2 = mapDocToPrefixCount.get(doc2);
            //descending order as per assignment
            return Integer.compare(d2, d1);
        };
        //update time and reHeapify
        long time = System.nanoTime();
        docs.forEach(doc -> {
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(doc);
        });
        docs.sort(comparator);
        return docs;
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
        List<DocumentImpl> allDocs = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        if (allDocs.isEmpty()) {
            //push an empty command
            pushCommand(null, null);
            return deletedURIs;
        }
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = new CommandSet<>();
        for (DocumentImpl doc : allDocs) {
            GenericCommand<URI> genCmd = this.deleteDocReturnUndoCommand(doc);
            cmdSet.addCommand(genCmd);
            deletedURIs.add(doc.getKey());
        }
        //push the CommandSet onto the stack.
        this.commandStack.push(cmdSet);
        //return the deleted URI set
        return deletedURIs;
    }

    private GenericCommand<URI> deleteDocReturnUndoCommand(DocumentImpl doc) {
        //delete from the hashtable
        this.hashTable.put(doc.getKey(), null);
        //delete from the trie
        this.removeFromTrie(doc);
        //remove from heap
        this.removeFromHeap(doc);
        //decrement counts
        this.decrementCounts(doc);
        //make a GenCMD
        Function<URI, Boolean> undoDelete = deletedURI -> {
            boolean added = this.hashTable.put(deletedURI, doc) == null;
            if (added) {
                //add back to trie
                this.addToTrie(doc);
                //update time & add back to heap.
                this.addToHeap(doc);
                //increment counts & check limits
                this.incrementCounts(doc);
            }
            return added;
        };
        return new GenericCommand<>(doc.getKey(), undoDelete);
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
        Set<DocumentImpl> allDocsWithPrefix = this.trie.deleteAllWithPrefix(keywordPrefix);
        //case: nothing was deleted - push empty command adn return empty set.
        if (allDocsWithPrefix.isEmpty()) {
            pushCommand(null, null);
            return deletedURIs;
        }
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = new CommandSet<>();
        for (DocumentImpl doc : allDocsWithPrefix) {
            GenericCommand<URI> genCmd = this.deleteDocReturnUndoCommand(doc);
            cmdSet.addCommand(genCmd);
            deletedURIs.add(doc.getKey());
        }
        //push the CommandSet onto the stack.
        this.commandStack.push(cmdSet);
        //return the deleted URI set
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

    /**
     * @return the Document object stored at that URI, or null if there is no such
    Document. should not effect the lastUsedTIme of a document. */
    protected DocumentImpl getDocument(URI uri){
        return this.hashTable.get(uri);
    }

    //testing purposes only
    protected int getStackSize() {
        return this.commandStack.size();
    }

    //testing purposed only
    protected ArrayList<DocumentImpl> getDocsWIthWordFromTrie(String word) {
        return (ArrayList<DocumentImpl>) this.trie.getAllSorted(word, this.comparatorDescending(word));
    }

    protected void printCounts () {
        System.out.println("maxDocCount == " + maxDocumentCount);
        System.out.println("docCount == " + documentCount);
        System.out.println("maxByteCount == " + maxByteCount);
        System.out.println("byteCount == " + byteCount);
    }

    protected int getDocCount() {
        return this.documentCount;
    }

    protected int getByteCount() {
        return this.byteCount;
    }
}
