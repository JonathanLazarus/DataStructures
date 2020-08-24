package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;


public class DocumentImpl implements Document {
    private URI uri;
    private String text;
    private byte[] pdf;
    private int textHashCode;
    private boolean isPDF = false;

    public DocumentImpl() { }

    public DocumentImpl(URI uri, String txt, int txtHash){
        if (txt == null) { throw new IllegalArgumentException(); }
        this.uri = uri;
        this.text = txt;
        this.textHashCode = txtHash;
        this.isPDF = false;
    }

    public DocumentImpl(URI uri, String txt, int txtHash, byte[] pdfBytes){
        if (txt == null) { throw new IllegalArgumentException(); }
        this.uri = uri;
        this.text = txt;
        this.textHashCode = txtHash;
        this.pdf = pdfBytes;
        this.isPDF = true;
    }

    public int getDocumentTextHashCode() {
        return this.textHashCode;
    }

    public byte[] getDocumentAsPdf() {
        if (!isPDF) {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contents = new PDPageContentStream(document, page)) {
                contents.beginText();
                contents.setFont(PDType1Font.TIMES_ROMAN, 10);
                contents.newLineAtOffset(100, 700);
                contents.showText(this.text);
                contents.endText();
                contents.close();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                document.save(stream);
                document.close();
                return stream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.pdf;
    }

    public String getDocumentAsTxt() {
        return this.text;
    }

    public URI getKey() {
        return this.uri;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;
        DocumentImpl docObj = (DocumentImpl) obj;
        if (this.uri.equals(docObj.uri)) {
            boolean test = this.textHashCode == docObj.textHashCode;
            return this.textHashCode == docObj.textHashCode;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.textHashCode;
    }
}