package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.DocumentStore.DocumentFormat;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class MyDocumentStoreImplTest {
    public DocumentStoreImpl docStore;
    private ByteArrayInputStream newIS;
    private ByteArrayInputStream newTextStream;
    private URI newPdfURI;
    private URI newTextURI;
    private ByteArrayInputStream existingPdfIS;
    private ByteArrayInputStream existingTxtIS;
    private URI existingPdfURI;
    private URI existingTxtURI;

    private URI uri1;
    private ByteArrayInputStream is1;

    private URI textURI1;
    private ByteArrayInputStream textStream1;

    public MyDocumentStoreImplTest() throws IOException, URISyntaxException {
        this.newIS = this.getPdfInputStream("this is the text of a new document");
        this.newTextStream = this.getTextInputStream("new content");
        this.newPdfURI = this.getURI("newURI/newestURI/");
        this.newTextURI = this.getURI("thisIsTheNewTextURI/");
        this.existingPdfIS = this.getPdfInputStream("this is the text of the second PDF document.");
        this.existingTxtIS = this.getTextInputStream("existing TXT document content.");
        this.existingPdfURI = this.getURI("pdfURI2/");
        this.existingTxtURI = this.getURI("uriTXTexisting/");

        //create another PDF to add to the DSI in the @Before method
        this.is1 = this.getPdfInputStream("this is the text of the first PDF document. test.");
        this.uri1 = this.getURI("pdfURI1/");

        //create another TXT to add to the DSI in the @Before method
        this.textURI1 = this.getURI("uriTXT1/");
        this.textStream1 = this.getTextInputStream("first TXT document content.");


    }

    @Before
    public void setUp() throws Exception {
        this.docStore = new DocumentStoreImpl();

        this.docStore.putDocument(this.is1, this.uri1, DocumentFormat.PDF);
        this.docStore.putDocument(this.existingPdfIS, this.existingPdfURI, DocumentFormat.PDF);

        //text files
        this.docStore.putDocument(this.textStream1, this.textURI1, DocumentFormat.TXT);
        this.docStore.putDocument(this.existingTxtIS, this.existingTxtURI, DocumentFormat.TXT);
    }


    @After
    public void tearDown() throws Exception {
        this.docStore = null;
    }

    private ByteArrayInputStream getPdfInputStream(String text) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page1 = new PDPage();
        doc.addPage(page1);
        try (PDPageContentStream contents = new PDPageContentStream(doc, page1)) {
            contents.beginText();
            contents.setFont(PDType1Font.TIMES_ROMAN, 10);
            contents.newLineAtOffset(100, 700);
            contents.showText(text);
            contents.endText();
            contents.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private URI getURI(String text) throws URISyntaxException {
        return new URI(text);
    }

    private ByteArrayInputStream getTextInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private int putDocument(InputStream input, URI uri, DocumentFormat format){
        return this.docStore.putDocument(input,uri,format);
    }

    private String getDocumentAsTxt(URI uri) {
        return this.docStore.getDocumentAsTxt(uri);
    }


    @Test (expected = IllegalArgumentException.class)
    public void nullDocInputStreamNullURI() {
        putDocument(null,null,DocumentFormat.PDF);
    }

    @Test
    public void nullDocInputStreamNewURI() {
        //do nothing, return zero
        assertEquals(0, putDocument(null, newPdfURI,DocumentFormat.PDF));
        assertEquals(0, putDocument(null, newTextURI,DocumentFormat.TXT));
    }

    @Test
    public void nullDocInputStreamExistingURI() {
        //delete doc from hashtable, return deletedDoc's hashcode
        //first check that they're in the hashtable
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));
        assertEquals("existing TXT document content.", getDocumentAsTxt(existingTxtURI));

        //then delete,
        assertEquals("this is the text of the second PDF document.".hashCode(), putDocument(null, existingPdfURI, DocumentFormat.PDF));
        assertEquals("existing TXT document content.".hashCode(), putDocument(null, existingTxtURI, DocumentFormat.TXT));

        //now check that they are not in the hashtable
        assertNull(getDocumentAsTxt(existingPdfURI));
        assertNull(getDocumentAsTxt(existingTxtURI));
    }

    @Test (expected = IllegalArgumentException.class)
    public void newDocInputStreamNullURI() {
        putDocument(newIS, null, DocumentFormat.PDF);
    }

    @Test
    public void newDocInputStreamNewURI() {
        //add to hashtable and return hashcode of newDoc(text)
        assertEquals("this is the text of a new document".hashCode(), putDocument(newIS, newPdfURI, DocumentFormat.PDF));
        assertEquals("new content".hashCode(), putDocument(newTextStream, newTextURI, DocumentFormat.TXT));

    }

    @Test
    public void newDocInputStreamExistingURI() {
        //replace, return replacedDoc's hashcode
        //first check that its there
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));

        //then replace
        assertEquals("this is the text of the second PDF document.".hashCode(), putDocument(newIS, existingPdfURI, DocumentFormat.PDF));

        //then check that the new PDF is there instead @ old URI
        assertEquals("this is the text of a new document", getDocumentAsTxt(existingPdfURI));
    }

    @Test (expected = IllegalArgumentException.class)
    public void existingDocInputStreamNullURI() {
        putDocument(this.existingPdfIS, null, DocumentFormat.PDF);
    }

    @Test
    public void existingDocInputStreamNewURI() throws IOException {
        //add the doc and return its hashcode
        //first check that its not there
        assertNull(getDocumentAsTxt(this.newTextURI));
        assertNull(getDocumentAsTxt(this.newPdfURI));

        //then put
        assertEquals("existing TXT document content.".hashCode(), putDocument(getTextInputStream("existing TXT document content."), newTextURI, DocumentFormat.TXT));

        //assertEquals("existing TXT document content.".hashCode(), putDocument(existingTxtIS, newTextURI, DocumentFormat.TXT));
        assertEquals("this is the text of the second PDF document.".hashCode(), putDocument(getPdfInputStream("this is the text of the second PDF document."), newPdfURI, DocumentFormat.PDF));
    }

    @Test
    public void existingDocInputStreamExistingURI() throws IOException {
        //does nothing, and returns the existingDoc's text.hashcode()
        assertEquals("this is the text of the second PDF document.".hashCode(), putDocument(getPdfInputStream("this is the text of the second PDF document."), existingPdfURI, DocumentFormat.PDF));
        assertEquals("existing TXT document content.".hashCode(), putDocument(getTextInputStream("existing TXT document content."), existingTxtURI, DocumentFormat.TXT));
    }

    @Test
    public void deleteDocument() throws URISyntaxException {
        assertFalse(this.docStore.deleteDocument(newTextURI));
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));
        assertEquals("existing TXT document content.", getDocumentAsTxt(existingTxtURI));
        assertTrue(this.docStore.deleteDocument(existingPdfURI));
        assertNotEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));
        assertNull(getDocumentAsTxt(existingPdfURI));
        assertTrue(this.docStore.deleteDocument(existingTxtURI));
        assertNotEquals("existing TXT document content.", getDocumentAsTxt(existingTxtURI));
        assertNull(getDocumentAsTxt(existingTxtURI));
    }

    @Test (expected = IllegalArgumentException.class)
    public void nullFormatPassed() {
        putDocument(newIS, newTextURI, null);
    }

    @Test
    public void getDocumentAsPdf_forPDF() throws IOException {
        byte[] existingPDFasPDFBytes = txtTOPDFBytes("this is the text of the second PDF document.");//toBytes(getPdfIn// utStream("first TXT document content."));
        byte[] existingPDFReturnedAsPDFBytes = this.docStore.getDocumentAsPdf(existingPdfURI);
        assertEquals("this is the text of the second PDF document.", isPDF(existingPDFasPDFBytes));
        assertEquals(isPDF(existingPDFasPDFBytes), isPDF(existingPDFReturnedAsPDFBytes));
    }

    @Test
    public void getDocumentAsPdf_forTXT() throws IOException, URISyntaxException {
        byte[] existingTXTasPDFBytes = txtTOPDFBytes("first TXT document content.");//toBytes(getPdfIn// utStream("first TXT document content."));
        byte[] existingTXTReturnedAsPDFBytes = this.docStore.getDocumentAsPdf(textURI1);
        assertEquals(isPDF(existingTXTasPDFBytes), isPDF(existingTXTReturnedAsPDFBytes));
    }
    private String isPDF(byte[] bytes) throws IOException {
        //use PDFbox to use 'input' to read the content of the PDF given
        PDDocument pdf = PDDocument.load(bytes);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        String text = "";
        for (int p = 1; p <= pdf.getNumberOfPages(); ++p) {
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            text = stripper.getText(pdf).trim();
        }
        pdf.close();
        return text;
    }

    private byte[] txtTOPDFBytes(String text) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream contents = new PDPageContentStream(document, page)) {
            contents.beginText();
            contents.setFont(PDType1Font.TIMES_ROMAN, 10);
            contents.newLineAtOffset(100, 700);
            contents.showText(text);
            contents.endText();
            contents.close();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            document.save(stream);
            document.close();
            return stream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException();
    }

    @Test
    public void getDocumentAsTxt() throws URISyntaxException {
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));

        //null input == null output
        assertNull(getDocumentAsTxt(null));

        //new URI -> null output
        assertNull(getDocumentAsTxt(newTextURI));
    }

    @Test
    public void deleteDocumentTest() {
        //check that they're there
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));

        //now delete,
        assertTrue(this.docStore.deleteDocument(this.existingTxtURI));
        assertTrue(this.docStore.deleteDocument(this.existingPdfURI));

        //now check that they're gone
        assertNull(getDocumentAsTxt(this.existingTxtURI));
        assertNull(getDocumentAsTxt(existingPdfURI));

        //now make sure method returns false for doc that is not in the DocStore:
        assertFalse(this.docStore.deleteDocument(this.newTextURI));
        assertFalse(this.docStore.deleteDocument(this.newPdfURI));
    }

    //STAGE 2 SPECIFIC TESTS (including undo logic):
    @Test
    public void undoReplace_newISExistingURI() {
        //check that this Doc is in the docStore.
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));

        //replace the doc with a nw value (e.g. putDocument(newISExistingURI):
        assertEquals("existing TXT document content.".hashCode(), putDocument(newTextStream, existingTxtURI, DocumentFormat.TXT));

        //check that the Doc has been replaced (e.g. new text):
        assertEquals("new content", getDocumentAsTxt(this.existingTxtURI));
        assertNotEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));

        //undo the replace (e.g. getDocumentAsTxt() should return the first string, not the second):
        this.docStore.undo();
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));
    }

    @Test (expected = IllegalStateException.class)
    public void undoingNothing() {
        this.docStore = new DocumentStoreImpl();
        this.docStore.undo();
    }

    @Test
    public void undoAdd() {
        //check that this Doc is added to the docStore.
        assertEquals("new content".hashCode(), putDocument(this.newTextStream, this.newTextURI, DocumentFormat.TXT));

        this.docStore.undo();
        assertNull(getDocumentAsTxt(this.newTextURI));
    }

    @Test
    public void undoDelete() {
        //check that they're there
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));

        //now delete,
        assertTrue(this.docStore.deleteDocument(this.existingTxtURI));
        assertTrue(this.docStore.deleteDocument(this.existingPdfURI));

        //now check that they're gone
        assertNull(getDocumentAsTxt(this.existingTxtURI));
        assertNull(getDocumentAsTxt(existingPdfURI));

        //now make sure method returns false for doc that is not in the DocStore:
        assertFalse(this.docStore.deleteDocument(this.newTextURI));
        assertFalse(this.docStore.deleteDocument(this.newPdfURI));

        //undo the last delete (existingPdfURI)
        this.docStore.undo();
        this.docStore.undo();
        this.docStore.undo();
        //undone 3 times because we pushed two empty commands ontp the stack.
        assertEquals("this is the text of the second PDF document.", this.docStore.getDocument(existingPdfURI).getDocumentAsTxt());

        //check that the txt doc is still not there
        assertNull(getDocumentAsTxt(this.existingTxtURI));

        //now undo the txt doc delete
        this.docStore.undo();

        //check that they're both there:
        assertEquals("existing TXT document content.", getDocumentAsTxt(this.existingTxtURI));
        assertEquals("this is the text of the second PDF document.", getDocumentAsTxt(existingPdfURI));
    }

    @Test
    public void undoToURI() throws IllegalStateException, URISyntaxException {
        URI testUri1 = getURI("//testURI1one.html");
        URI testUri2 = getURI("//testURI2two.html");
        URI testUri3 = getURI("//testURI3three.html");
        URI testUri4 = getURI("//testURI4four.html");
        URI testUri5 = getURI("//testURI5five.html");
        URI testUri6 = getURI("//testURI6six.html");
        URI testUri7 = getURI("//testURI7seven.html");
        URI testUri8 = getURI("//testURI8eight.html");
        URI testUri9 = getURI("//testURI9nine.html");
        URI testUri10 =  getURI("//testUR10ten.html");

        putDocument(getTextInputStream("test text 1"), testUri1, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 2"), testUri2, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 3"), testUri3, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 4"), testUri4, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 5"), testUri5, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 6"), testUri6, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 7"), testUri7, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 8"), testUri8, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 9"), testUri9, DocumentFormat.TXT);
        putDocument(getTextInputStream("test text 10"), testUri10, DocumentFormat.TXT);
        //see that the last doc was added
        assertEquals("test text 10", getDocumentAsTxt(testUri10));
        //undo via uri
        this.docStore.undo(testUri10);
        assertNull(getDocumentAsTxt(testUri10));
        //that was a test on the last element added. Now lets try to do it on a Doc 3, which is 5th-to-last in the stack, not first.
        //see that the doc 3 was added
        assertEquals("test text 3", getDocumentAsTxt(testUri3));
        //undo doc 3 put via uri
        this.docStore.undo(testUri3);
        assertNull(getDocumentAsTxt(testUri3));
        //now check that all other commands were not effected:
        assertEquals("test text 9", getDocumentAsTxt(testUri9));
    }

    @Test
    public void undoToUriThrowsExc() throws URISyntaxException {
        URI newTestURI = getURI("//newTextUriNeverAdded.html");

        boolean pass = false;
        boolean secondPass = false;
        try{
            this.docStore.undo(newTestURI);
        }catch (IllegalStateException e) {
            pass = true;
        }
        try{
            this.docStore = new DocumentStoreImpl();
            this.docStore.undo(newTestURI);
        }catch (IllegalStateException e) {
            secondPass = true;
        }
        assertTrue(pass && secondPass);
    }

    //STAGE 3 tests

    @Test
    public void wordCount() throws URISyntaxException {
        String text = "test text 1. This is the test for the getWordCount() method in the DocumentImpl class. This string has the word test in it 5 times. Note: that the word Test and tESt should also be factored into the count. we don't care about capitalization - this is YU, after all.";
        URI testUri1 = getURI("//testURI1one.html");
        putDocument(getTextInputStream(text), testUri1, DocumentFormat.TXT);
        assertEquals(5, this.docStore.getDocument(testUri1).wordCount("test"));
    }
}