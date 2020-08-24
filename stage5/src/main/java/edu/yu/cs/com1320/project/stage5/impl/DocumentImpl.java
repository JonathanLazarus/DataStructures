package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class DocumentImpl implements Document {
    private URI uri;
    private String text;
    private byte[] pdf;
    private int textHashCode;
    private HashMap<String, Integer> wordCount = new HashMap<>();
    long lastUsedTime;

    private UriTimeVault vault;

    public DocumentImpl() {
    }

    //constructor to be used when deserializing from disk.
    public  DocumentImpl(URI uri, String text, int textHashCode, HashMap<String,Integer> wordMap) {
        this.uri = uri;
        this.text = text;
        this.textHashCode = textHashCode;
        this.wordCount = wordMap;
        this.pdf = this.getAsPDF();
        this.lastUsedTime = System.nanoTime();
        this.vault = new UriTimeVault(this.uri, this.lastUsedTime, this.getDocumentByteCount());
    }

    public DocumentImpl(URI uri, String txt, int txtHash) {
        if (txt == null) {
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.text = txt;
        this.textHashCode = txtHash;
        this.pdf = this.getAsPDF();
        this.getWordCount();
        this.lastUsedTime = System.nanoTime();
        this.vault = new UriTimeVault(this.uri, this.lastUsedTime, this.getDocumentByteCount());
    }

    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes) {
        if (txt == null) {
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.text = txt;
        this.textHashCode = txtHash;
        this.pdf = pdfBytes;
        this.getWordCount();
        this.lastUsedTime = System.nanoTime();
        this.vault = new UriTimeVault(this.uri, this.lastUsedTime, this.getDocumentByteCount());
    }

    public int getDocumentTextHashCode() {
        return this.textHashCode;
    }

    public byte[] getDocumentAsPdf() {
        return this.pdf;
    }

    private byte[] getAsPDF() {
        String textRemovedChars = this.text.replaceAll("[[^a-zA-Z0-9]&&[\\S]&&[^-]&&[^.]]+", " ");
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contents = new PDPageContentStream(document, page)) {
            contents.beginText();
            contents.setFont(PDType1Font.TIMES_ROMAN, 10);
            contents.newLineAtOffset(100, 700);
            contents.showText(textRemovedChars);
            contents.endText();
            contents.close();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            document.save(stream);
            document.close();
            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("should not have reached this point");
    }

    public String getDocumentAsTxt() {
        return this.text;
    }

    public URI getKey() {
        return this.uri;
    }

    /**
     * how many times does the given word appear in the document?
     *
     * @param word
     * @return the number of times the given words appears in the document
     */
    @Override
    public int wordCount(String word) {
        return this.wordCount.getOrDefault(word.toUpperCase(), 0);
    }

    /**
     * return the last time this document was used, via put/get or via a search result
     * (for stage 4 of project)
     */
    @Override
    public long getLastUseTime() {
        return this.lastUsedTime;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUsedTime = timeInNanoseconds;
        this.vault.setLastUsedTime(timeInNanoseconds);
    }

    /**
     * @return a copy of the word to count map so it can be serialized
     */
    @Override
    public Map<String, Integer> getWordMap() {

        //TODO STAGE5: copy of???

        return this.wordCount;
    }

    /**
     * This must set the word to count map during deserialization
     *
     * @param wordMap
     */
    //TODO STAGE5
    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        if (wordMap == null) throw new IllegalArgumentException("cannot set null word map.");
        if (!(wordMap instanceof HashMap)) throw new IllegalArgumentException("must pass in Map of type HashMap");
        this.wordCount = (HashMap<String, Integer>) wordMap;
    }

    private void getWordCount() {
        String textNoChars = text.replaceAll("[[^a-zA-Z0-9]&&[\\S]&&[^-]]+", "");
        Arrays.stream(textNoChars.split("[^a-zA-Z0-9]+"))
                .forEach(word -> this.wordCount
                        .put(word.toUpperCase(), this.wordCount.getOrDefault(word.toUpperCase(), 0)+1));
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;
        DocumentImpl docObj = (DocumentImpl) obj;
        if (this.uri.equals(docObj.uri)) {
            return this.textHashCode == docObj.textHashCode;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.textHashCode;
    }

    protected HashMap<String, Integer> getWordCountHashMap() {
        return this.wordCount;
    }


    //TODO check piazza. a lot of hock about this on there
    @Override
    public int compareTo(Document o) {
        return Long.compare(this.lastUsedTime, o.getLastUseTime());
    }

    //for DocumentStore Use
    protected int getDocumentByteCount() {
        return this.text.getBytes().length + this.pdf.length;
    }

    //for testing purposes only
    protected void setText (String text) {
        this.text = text;
    }

    protected UriTimeVault getVault() {
        return this.vault;
    }

}