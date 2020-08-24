package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, DocumentImpl> hashTable;
    private StackImpl<Command> commandStack;

    public DocumentStoreImpl() {
        this.hashTable = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
    }

    @Override
    public boolean deleteDocument(URI uri) {
        if (this.hashTable.get(uri) == null) return false;
        DocumentImpl deletedDoc = this.hashTable.put(uri, null);
        Function<URI, Boolean> undoDelete = deletedURI -> this.hashTable.put(deletedURI, deletedDoc) == null;
        pushCommand(uri, undoDelete);
        return this.hashTable.get(uri) == null;
    }

    @Override
    public String getDocumentAsTxt(URI uri) {
        try {
            return this.hashTable.get(uri).getDocumentAsTxt();
        }catch (NullPointerException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * undo the last put or delete command
     *
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        checkStackNotEmpty();
        this.commandStack.pop().undo();
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
    @Override
    public void undo(URI uri) throws IllegalStateException {
        checkStackNotEmpty();//throws Exc if stack is initially null
        Command temp = this.commandStack.pop();
        //Base case
        if (temp.getUri() == uri) {
            temp.undo();
            return;
        }
        //recursively .pop()s each command off the stack, calls .undo() on the most recent one with the given URI
        this.undo(uri);
        //then recursively .push()es each command back onto the stack
        this.commandStack.push(temp);
    }

    public byte[] getDocumentAsPdf(URI uri) {
        try {
            return hashTable.get(uri).getDocumentAsPdf();
        }catch (NullPointerException e) {
            return null;
        }
    }

    //Might want to look over this put method since a lot of the stuff it checks for
    // for example, case 1 and case 2, theyre already checked for in the HashTableImpl.put() method.

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
            DocumentImpl returnedDocValue = this.hashTable.put(uri, newDoc);
            /*in the above line, if the newDoc wasn't the same as the existingDoc, then it was replaced
              but the value returned is that of the replaced Doc's.
              Therefore we need to make a Command object if we want to undo the replace.
              So first we check if they are not the same Document (meaning that a replace took place):
              */
            if (!returnedDocValue.equals(newDoc)) {
                Function<URI, Boolean> undoReplace = replacedURI -> {
                    /*method body logic of how to undo the replace goes here:
                     @return true is the undo worked, @return false if not.
                     */
                    return this.hashTable.put(replacedURI, returnedDocValue).equals(newDoc); };
                pushCommand(uri, undoReplace);
            }
            return returnedDocValue.hashCode();
        }catch (NullPointerException e){
            //finally, if a NullPointerException was thrown, that means that a new (key, value) pair was added
            // and the hashtable returned null and we can't call hashcode on null.
            // So return the hashcode of the added value (in this case newDoc)
            Function<URI, Boolean> undoAdd = addedURI -> {
                DocumentImpl addedDoc = this.hashTable.get(addedURI);
                if (addedDoc == null) {
                    return false;
                }
                return addedDoc.equals(this.hashTable.put(addedURI, null));
                };
            pushCommand(uri, undoAdd);
            return newDoc.hashCode();
        }
    }

    private void pushCommand(URI uri, Function<URI, Boolean> undo) {
        Command cmd = new Command(uri, undo);
        this.commandStack.push(cmd);
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

    /*we need this method since we have to @return the hashcode of the old value being deleted.
     therefore, we need to temporarily hold on to its text while the entry is being deleted. */
    private int streamIsNull(URI uri) {
        if(this.hashTable.get(uri) != null) {
            String deletedDocumentAsTxt = getDocumentAsTxt(uri);
            if (deleteDocument(uri)) {
                return deletedDocumentAsTxt.hashCode();
            }
        }
        //since input is null, and the uri is not in the hashtable (via the above if statement),
        // ie: trying to "put" a new key with null value
        return 0;
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
     * @return the Document object stored at that URI, or null if there is no such
    Document */
    protected Document getDocument(URI uri){
        return this.hashTable.get(uri);
    }
}