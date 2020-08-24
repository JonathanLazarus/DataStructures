package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.DocumentStore;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import java.io.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, DocumentImpl> hashTable;

    public DocumentStoreImpl() {
        this.hashTable = new HashTableImpl<URI, DocumentImpl>();
    }

    public boolean deleteDocument(URI uri) {
        if (this.hashTable.get(uri) == null) return false;
        return this.hashTable.get(uri).equals(this.hashTable.put(uri, null));
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

    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (uri == null || format == null) { throw new IllegalArgumentException(); }
        //if the input is null but the key exists in the hashtable already, delete the entire entry completely.
        if (input == null) { return streamIsNull(uri); }
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
        try{
            //if the key exists in the hashtable already and:
            //1)the values are the same, then the hashtable will return the value.
            //2)the values are different, then the hashtable with return the old value and we call hashcode on that.
            return this.hashTable.put(uri, newDoc).hashCode();
        }catch (NullPointerException e){
            //finally, if a nullpointerException was thrown, that means that a new (key, value) pair was added
            // and the hashtable returned null and we cant call hashcode on null.
            // So return the hashcode of the added value (in this case newDoc)
            return newDoc.hashCode();
        }
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

    public String getDocumentAsTxt(URI uri) {
        try {
            return this.hashTable.get(uri).getDocumentAsTxt();
        }catch (NullPointerException | IllegalArgumentException e) {
            return null;
        }
    }
}