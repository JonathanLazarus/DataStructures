package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
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
    private HashTableImpl<URI, DocumentImpl> hashTable;
    private StackImpl<Undoable> commandStack;
    private TrieImpl<Document> trie;

    public DocumentStoreImpl() {
        this.hashTable = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.trie = new TrieImpl<>();
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
        try{
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
            if (!returnedDoc.equals(newDoc)) {
                /*[replace took place]
                 * if we are in this if statement body, that means that we just added @newDoc to the hashTable in place of, (deleted) @returnedDoc.
                 */
                this.addToTrie(newDoc);
                this.removeFromTrie(returnedDoc);
                Function<URI, Boolean> undoReplace = replacedURI -> {
                    /*method body logic of how to undo the replace goes here:
                     @return true is the undo worked, @return false if not.
                     */
                    boolean replaced = this.hashTable.put(replacedURI, returnedDoc).equals(newDoc);
                    if (replaced) {
                        this.addToTrie(returnedDoc);
                        this.removeFromTrie(newDoc);
                    }
                    return replaced; };
                pushCommand(uri, undoReplace);
            }
            return returnedDoc.hashCode();
        }catch (NullPointerException e){
            //finally, if a NullPointerException was thrown, that means that a new (key, value) pair was added
            // and the hashtable returned null and we can't call hashcode on null.
            // So return the hashcode of the added value (in this case newDoc)
            this.addToTrie(newDoc);

            Function<URI, Boolean> undoAdd = addedURI -> {
                DocumentImpl addedDoc = this.hashTable.get(addedURI);
                if (addedDoc == null) {
                    return false;
                }
                boolean addUndone = addedDoc.equals(this.hashTable.put(addedURI, null));
                if (addUndone) {
                    this.removeFromTrie(addedDoc);
                }
                return addUndone;
            };
            pushCommand(uri, undoAdd);
            return newDoc.hashCode();
        }
    }

    private void addToTrie(DocumentImpl newDoc) {
        HashMap<String,Integer> wordCount = newDoc.getWordCountHashMap();
        for(String word : wordCount.keySet()) {
            this.trie.put(word, newDoc);
        }
    }

    private void removeFromTrie(DocumentImpl deletedDoc) {
        HashMap<String,Integer> wordCount = deletedDoc.getWordCountHashMap();
        for(String word : wordCount.keySet()) {
            this.trie.delete(word, deletedDoc);
        }
    }

    /*we need this method since we have to @return the hashcode of the old value being deleted.
     therefore, we need to temporarily hold on to its text while the entry is being deleted. */
    private int streamIsNull(URI uri) {
        if(this.hashTable.get(uri) != null) {
            //[delete is taking place here] (and the trie updating will take place in this.deleteDocument() method)
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
        //use PDFbox to use 'input' to read the content of the PDF given
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
        try {
            return hashTable.get(uri).getDocumentAsPdf();
        }catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document as TXT, i.e. a String, or null if no document exists with that URI
     */
    @Override
    public String getDocumentAsTxt(URI uri) {
        try {
            return this.hashTable.get(uri).getDocumentAsTxt();
        }catch (NullPointerException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if (this.hashTable.get(uri) == null) return false;
        //[delete is taking place]

        DocumentImpl deletedDoc = this.hashTable.put(uri, null);
        //@deletedDoc was deleted - update the trie. removeFromTrie()
        this.removeFromTrie(deletedDoc);
        //add undo to command stack
        Function<URI, Boolean> undoDelete = deletedURI -> {
            boolean added = this.hashTable.put(deletedURI, deletedDoc) == null;
            if (added) {
                this.addToTrie(deletedDoc);
            }
            return added;
        };
        pushCommand(uri, undoDelete);
        return this.hashTable.get(uri) == null;
    }

    private void pushCommand(URI uri, Function<URI, Boolean> undo) {
        GenericCommand<URI> cmd = new GenericCommand<>(uri, undo);
        this.commandStack.push(cmd);
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void undo(URI uri) throws IllegalStateException {
        checkStackNotEmpty();//throws Exc if stack is initially null
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

    private void undo(URI uri, boolean undoneInSet) {
        Undoable temp = this.commandStack.pop();
        //Base case
        if (temp instanceof CommandSet) {
            CommandSet<URI> tempSet = (CommandSet) temp;
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
        else if (((GenericCommand) temp).getTarget() == uri) {
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
        List<Document> sortedDocList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<String> docTexts = new ArrayList<>();
        for(Document doc : sortedDocList) {
            docTexts.add(doc.getDocumentAsTxt());
        }
        return docTexts;
    }

    private Comparator<Document> comparatorDescending(String keyword) {
         Comparator<Document> comp = (doc1, doc2) -> {
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
        List<Document> sortedDocList = this.trie.getAllSorted(keyword, this.comparatorDescending(keyword));
        ArrayList<byte[]> docTexts = new ArrayList<>();
        for(Document doc : sortedDocList) {
            docTexts.add(doc.getDocumentAsPdf());
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
        List<Document> docs = this.trie.getAllWithPrefixSorted(keywordPrefix, this.comparatorDescending(keywordPrefix));
        HashMap<DocumentImpl, Integer> mapDocToPrefixCount = new HashMap<>();
        for(Document document : docs) {
            mapDocToPrefixCount
                    .put((DocumentImpl) document, this.getPrefixCount((DocumentImpl) document, keywordPrefix));
        }
        Comparator<Document> comparator =  (doc1, doc2) -> {
            int d1 = mapDocToPrefixCount.get(doc1);
            int d2 = mapDocToPrefixCount.get(doc2);
            //descending order as per assignment
            return Integer.compare(d2, d1);
        };
        return docs.stream().sorted(comparator).
                map(Document::getDocumentAsTxt)
                .collect(Collectors.toList());
    }

    /**
     * same logic as searchByPrefix, but returns the docs as PDFs instead of as Strings
     *
     * @param keywordPrefix
     */
    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) {
        List<Document> docs = this.trie.getAllWithPrefixSorted(keywordPrefix, this.comparatorDescending(keywordPrefix));
        HashMap<DocumentImpl, Integer> mapDocToPrefixCount = new HashMap<>();
        for(Document document : docs) {
            mapDocToPrefixCount
                    .put((DocumentImpl) document, this.getPrefixCount((DocumentImpl) document, keywordPrefix));
        }
        Comparator<Document> comparator =  (doc1, doc2) -> {
            int d1 = mapDocToPrefixCount.get(doc1);
            int d2 = mapDocToPrefixCount.get(doc2);
            //descending order as per assignment
            return Integer.compare(d2, d1);
        };
        return docs.stream().sorted(comparator).
                map(Document::getDocumentAsPdf)
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
        Set<URI> deletedURIs = new HashSet<>();
        //get all of the Documents containing the word
        Set<Document> allDocs = this.trie.deleteAll(keyword);
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = new CommandSet<>();
        for (Document doc : allDocs) {
            URI docURI = doc.getKey();
            //delete from the hashtable
            this.hashTable.put(docURI, null);
            //delete from the trie
            this.removeFromTrie((DocumentImpl) doc);
            //make a GenCMD
            Function<URI, Boolean> undoDelete = deletedURI -> {
                boolean added = this.hashTable.put(deletedURI, (DocumentImpl) doc) == null;
                if (added) {
                    this.addToTrie((DocumentImpl) doc);
                }
                return added;
            };
            GenericCommand<URI> genCmd = new GenericCommand<>(docURI, undoDelete);
            cmdSet.addCommand(genCmd);
            deletedURIs.add(docURI);
        }
        //push the CommandSet onto the stack.
        this.commandStack.push(cmdSet);
        //return the deleted URI set
        return deletedURIs;
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
        Set<URI> deletedURIs = new HashSet<>();
        //get all of the Documents containing the word
        Set<Document> allDocsWithPrefix = this.trie.deleteAllWithPrefix(keywordPrefix);
        //CommandSet to hold the GenericCommands done to each document.
        CommandSet<URI> cmdSet = new CommandSet<>();
        for (Document document : allDocsWithPrefix) {
            DocumentImpl doc = (DocumentImpl) document;
            URI docURI = doc.getKey();
            //delete from the hashtable
            this.hashTable.put(docURI, null);
            //delete from the trie
            //this.removeFromTrie(doc);
            //make a GenCMD
            Function<URI, Boolean> undoDelete = deletedURI -> {
                boolean added = this.hashTable.put(deletedURI, doc) == null;
                if (added) {
                    this.addToTrie(doc);
                }
                return added;
            };
            GenericCommand<URI> genCmd = new GenericCommand<>(docURI, undoDelete);
            cmdSet.addCommand(genCmd);
            deletedURIs.add(docURI);
        }
        //push the CommandSet onto the stack.
        this.commandStack.push(cmdSet);
        //return the deleted URI set
        return deletedURIs;
    }

    /**
     * @return the Document object stored at that URI, or null if there is no such
    Document */
    protected Document getDocument(URI uri){
        return this.hashTable.get(uri);
    }

    //testing purposes only
    //TODO delete before submission
    protected int getStackSize() {
        return this.commandStack.size();
    }
}
