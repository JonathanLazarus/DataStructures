package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.*;

public class MyStage4DocumentStoreImplTest {
    private DocumentStoreImpl fullDSI;
    private DocumentStoreImpl dsi;

    //TXT Documents
    //variables to hold possible values for doc1
    private URI uri1;
    private String txt1;

    //variables to hold possible values for doc2
    private URI uri2;
    private String txt2;

    //variables to hold possible values for doc2
    private URI uri3;
    private String txt3;

    //variables to hold possible values for doc2
    private URI uri4;
    private String txt4;

    //PDF documents
    //variables to hold possible values for doc5 [PDF]
    private URI pdfuri1;
    private String pdftxt1;

    //variables to hold possible values for doc6 [PDF]
    private URI pdfuri2;
    private String pdftxt2;

    //variables to hold possible values for doc7 [PDF]
    private URI pdfuri3;
    private String pdftxt3;

    //variables to hold possible values for doc8 [PDF]
    private URI pdfuri4;
    private String pdftxt4;

    public MyStage4DocumentStoreImplTest() throws Exception {
        //init possible values for doc1
        this.uri1 = new URI("http://edu.yu.cs/com1320/project/doc1");
        this.txt1 = "doc1: This is the text of doc1, in plain text. No fancy file format - just plain old String. I am adding more words to each documents text to make the testing for search more efficient and to make each document more unique.";

        //init possible values for doc2
        this.uri2 = new URI("http://edu.yu.cs/com1320/project/doc2");
        this.txt2 = "doc2: Text for doc2. A plain old String. But here is my life story. In a plain old string, yes. No i am not actually going to type it all up here right now. that would be ridiculous. but again - I'm just trying to lengthen these strings/texts";

        //init possible values for doc1
        this.uri3 = new URI("http://edu.yu.cs/com1320/project/doc3");
        this.txt3 = "doc3: This is the text of doc3 - doc doc goose. this is a nice pun. it's like the game duck duck goose. I'm actually too lazy to come up with something interesting for this string so I will just leave this here.";

        //init possible values for doc2
        this.uri4 = new URI("http://edu.yu.cs/com1320/project/doc4");
        this.txt4 = "doc4: how much wood would a woodchuck chuck... well let's try to find out. lets start with just a simple google search - \"what is a woodchuck?\" It seems that a woodchuck is some sort of groundhog. so my guess is that these rodents don't chuck any wood to begin with. End of research";

        //PDFs
        //init possible values for doc1
        this.pdfuri1 = new URI("http://edu.yu.cs/com1320/project/pdf/PDF1");
        this.pdftxt1 = "PDF1: i wonder how the word search will work with apostrophes that I've added to these texts. it is a good question - theoretically, i can search for the word \"it's\" right? but the way that I've coded it, if an apostrophe shows up in a string, it will split the word at the apostraphe, so \"it's\" will be 2 different words in each Document: \"it\" and \"s\". just something to think about";

        //init possible values for doc2
        this.pdfuri2 = new URI("http://edu.yu.cs/com1320/project/pdf/PDF2");
        this.pdftxt2 = "PDF2: I brought up the question about apostrophes in the text of PDFdoc1. The same question would also apply for hyphens. ie: if a document contains the word \"mother-in-law,\" the Document object will register it as 3 different words: \"mother,\" \"in,\" and \"law.\" Another thing to think about, another thing to work on";

        //init possible values for doc1
        this.pdfuri3 = new URI("http://edu.yu.cs/com1320/project/pdf/PDF3");
        this.pdftxt3 = "PDF3: in the piazza post @285-f1, it says that for all purposes of this assignment, any method called with the string \"str%ing,\" for example, should be registered as the word \"string.\" I asked about it on piazza. We'll see what happens";

        //init possible values for doc2
        this.pdfuri4 = new URI("http://edu.yu.cs/com1320/project/pdf/PDF4");
        this.pdftxt4 = "PDF4: PSA I changed the Trie.prefix() methods to include the prefixNode's value. ie: deletePrefix(\"the\") will clear the value set at node THE and delete its subtrie.";

        //create the DSIs
        this.fullDSI = new DocumentStoreImpl();
        this.dsi = new DocumentStoreImpl();

        //Add documents.
        //doc1
        ByteArrayInputStream bas = new ByteArrayInputStream(this.txt1.getBytes());
        this.fullDSI.putDocument(bas,this.uri1, DocumentStore.DocumentFormat.TXT);
        //pdfdoc1
        bas = this.getDocumentAsPdfIS(this.pdftxt1);
        this.fullDSI.putDocument(bas,this.pdfuri1, DocumentStore.DocumentFormat.PDF);
        //doc2
        bas = new ByteArrayInputStream(this.txt2.getBytes());
        this.fullDSI.putDocument(bas,this.uri2, DocumentStore.DocumentFormat.TXT);
        //pdfdoc2
        bas = this.getDocumentAsPdfIS(this.pdftxt2);
        this.fullDSI.putDocument(bas,this.pdfuri2, DocumentStore.DocumentFormat.PDF);
        //doc3
        bas = new ByteArrayInputStream(this.txt3.getBytes());
        this.fullDSI.putDocument(bas,this.uri3, DocumentStore.DocumentFormat.TXT);
        //pdfdoc3
        bas = this.getDocumentAsPdfIS(this.pdftxt3);
        this.fullDSI.putDocument(bas,this.pdfuri3, DocumentStore.DocumentFormat.PDF);
        //doc4
        bas = new ByteArrayInputStream(this.txt4.getBytes());
        this.fullDSI.putDocument(bas,this.uri4, DocumentStore.DocumentFormat.TXT);
        //pdfdoc4
        bas = this.getDocumentAsPdfIS(this.pdftxt4);
        this.fullDSI.putDocument(bas,this.pdfuri4, DocumentStore.DocumentFormat.PDF);
    }

    private ByteArrayInputStream getDocumentAsPdfIS(String pdfText) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        byte[] ba = {24, 56, 10};
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        try (PDPageContentStream contents = new PDPageContentStream(document, page)) {
            contents.beginText();
            contents.setFont(PDType1Font.TIMES_ROMAN, 10);
            contents.newLineAtOffset(100, 700);
            contents.showText(pdfText);
            contents.endText();
            contents.close();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            document.save(stream);
            document.close();
            bais = new ByteArrayInputStream(stream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bais;
    }

    private String getPDFText(byte[] bytes) throws IOException {
        //use PDFbox to use 'input' to read the content of the PDF given
        PDDocument pdf = PDDocument.load(bytes);
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String text = stripper.getText(pdf).trim();
        pdf.close();
        return text;
    }

    private DocumentStoreImpl createStoreAndPutOne(){
        DocumentStoreImpl dsi = new DocumentStoreImpl();
        ByteArrayInputStream bas1 = new ByteArrayInputStream(this.txt1.getBytes());
        dsi.putDocument(bas1,this.uri1, DocumentStore.DocumentFormat.TXT);
        return dsi;
    }

    private DocumentStoreImpl createStoreAndPutAll(){
        return this.fullDSI;
    }

    @Before
    public void init() throws Exception {
        this.dsi = this.fullDSI;
    }

    //TODO delete
    @Test
    public void memoryTest() {
        DocumentImpl d = (DocumentImpl) this.dsi.getDocument(this.uri2);
        assertEquals(this.txt2, d.getDocumentAsTxt());
        String newText = "new text penguin";
        d.setText(newText);
        ArrayList<DocumentImpl> dlist = this.dsi.getDocsWIthWordFromTrie("doc2");
        assertEquals(1, dlist.size());
        DocumentImpl docFromList = dlist.get(0);
        assertEquals(newText, docFromList.getDocumentAsTxt());



    }


    @Test
    public void undo() {
        this.assertCommandStack(8);
        //last doc added to store - PDF4
        String lastDocAddedText = this.dsi.getDocumentAsTxt(this.pdfuri4);
        assertEquals(lastDocAddedText, this.pdftxt4);
        this.dsi.undo();
        this.assertCommandStack(7);
        assertNull(this.dsi.getDocument(this.pdfuri4));

        lastDocAddedText = this.dsi.getDocumentAsTxt((this.uri4));
        assertEquals(lastDocAddedText, this.txt4);
        this.dsi.undo();
        this.assertCommandStack(6);
        assertNull(this.dsi.getDocument(this.uri4));

        lastDocAddedText = this.dsi.getDocumentAsTxt((this.pdfuri3));
        assertEquals(lastDocAddedText, this.pdftxt3);
        this.dsi.undo();
        this.assertCommandStack(5);
        assertNull(this.dsi.getDocument(this.pdfuri3));

        lastDocAddedText = this.dsi.getDocumentAsTxt((this.uri3));
        assertEquals(lastDocAddedText, this.txt3);
        this.dsi.undo();
        this.assertCommandStack(4);
        assertNull(this.dsi.getDocument(this.uri3));

        lastDocAddedText = this.dsi.getDocument(this.pdfuri2).getDocumentAsTxt();
        assertEquals(lastDocAddedText, this.pdftxt2);
        this.dsi.undo();
        this.assertCommandStack(3);
        assertNull(this.dsi.getDocument(this.pdfuri2));


        lastDocAddedText = this.dsi.getDocumentAsTxt((this.uri2));
        assertEquals(lastDocAddedText, this.txt2);
        this.dsi.undo();
        this.assertCommandStack(2);
        assertNull(this.dsi.getDocument(this.uri2));

        lastDocAddedText = this.dsi.getDocumentAsTxt((this.pdfuri1));
        assertEquals(lastDocAddedText, this.pdftxt1);
        this.dsi.undo();
        this.assertCommandStack(1);
        assertNull(this.dsi.getDocument(this.pdfuri1));


        lastDocAddedText = this.dsi.getDocumentAsTxt((this.uri1));
        assertEquals(lastDocAddedText, this.txt1);
        this.dsi.undo();
        this.assertCommandStack(0);
        assertNull(this.dsi.getDocument(this.uri1));

        this.dsi = this.createStoreAndPutOne();
        this.assertCommandStack(1);
        lastDocAddedText = this.dsi.getDocumentAsTxt((this.uri1));
        assertEquals(lastDocAddedText, this.txt1);
        this.dsi.undo();
        this.assertCommandStack(0);
        assertNull(this.dsi.getDocument(this.uri1));
    }

    private void assertCommandStack(int expected) {
        assertEquals("Incorrect amount of commands on commandStack", expected, this.dsi.getStackSize());
    }

    @Test
    public void testUndo() {
    }

    @Test
    public void search() {
        int max = 0;
        String word = "piazza";
        for (String text : this.dsi.search(word)) {
            if (max == 0) {
                max = this.getWordCount(text, word);
            }else {
                assertTrue(max >= this.getWordCount(text, word));
            }
            //System.out.println("\n" + text);
        }
    }

    private int getWordCount(String text, String word) {
        HashMap<String, Integer> wordCount = new HashMap<>();
        Arrays.stream(text.split("[^a-zA-Z0-9]+"))
                .forEach(words -> wordCount
                        .put(words.toUpperCase(), wordCount.getOrDefault(words.toUpperCase(), 0)+1));
        return wordCount.get(word.toUpperCase());
    }

    @Test
    public void searchPDFs() {
    }

    @Test
    public void searchByPrefix() {
        int max = 0;
        String word = "this";
        for (String text : this.dsi.searchByPrefix(word)) {
            System.out.println("\n" + text);
        }
    }

    @Test
    public void searchPDFsByPrefix() {
        HashMap<String, Integer> wordCount = new HashMap<>();
        String text = "hi my name is yo&n%i and my brother-in-law's name is J*oshua    how do you*know him?";
        String textNoChars = text.replaceAll("[[^a-zA-Z0-9]&&[\\S]&&[^-]]+", "");
        Arrays.stream(textNoChars.split("[^a-zA-Z0-9]+"))
                .forEach(word -> wordCount
                        .put(word.toUpperCase(), wordCount.getOrDefault(word.toUpperCase(), 0)+1));
        if (wordCount.isEmpty()) {
            fail("no words");
        }
        for (String word : wordCount.keySet()){
            System.out.println(word + "\t -> \tw " + wordCount.get(word));
        }
    }

    @Test
    public void deleteAll() {
        //docs with the word this: doc3, PDF3, and doc1.

        Set<URI> set = this.dsi.deleteAll("this");
        for (URI uri : set) {
            assertNull(this.dsi.getDocument(uri));
        }
        assertTrue(this.dsi.search("this").isEmpty());

        assertTrue(this.dsi.search("285").isEmpty());
        //push another command
        this.dsi.deleteDocument(pdfuri2);

        int stacknumber = this.dsi.getStackSize();
        this.dsi.undo(this.pdfuri3);
        assertEquals(this.pdftxt3, this.dsi.getDocumentAsTxt(this.pdfuri3));
        assertEquals(this.pdftxt3, this.dsi.search("285").get(0));

        assertEquals(stacknumber, this.dsi.getStackSize());
        assertEquals(1, this.dsi.search("this").size());
        assertEquals(pdftxt3, this.dsi.search("this").get(0));

        this.dsi.undo(this.uri3);
        assertEquals(stacknumber, this.dsi.getStackSize());

        this.dsi.undo(uri1);
        //show that the commandSet was popped off indefinitely.
        assertEquals(stacknumber - 1, this.dsi.getStackSize());

    }

    @Test
    public void deleteAllWithPrefix() {
        assertEquals(8, this.dsi.getStackSize());


        for (String s : this.dsi.searchByPrefix("doc")) {
            System.out.println("#" + s);
        }

        //all docs contain prefix doc besides PDF3 and PDF4. so to make sure that we can get to the bottom of the stack, push acommand after this.
        Set<URI> set = this.dsi.deleteAllWithPrefix("doc");
        assertEquals(9, this.dsi.getStackSize());

        assertFalse(this.dsi.search("registered").isEmpty());
        this.dsi.deleteDocument(pdfuri3);
        assertTrue(this.dsi.search("registered").isEmpty());
        this.dsi.deleteDocument(pdfuri4);

        assertNull(this.dsi.getDocument(pdfuri1));
        assertNull(this.dsi.getDocument(pdfuri3));
        assertNull(this.dsi.getDocument(pdfuri4));
        for (URI uri : set) {
            //System.out.println(1);
            assertNull(this.dsi.getDocument(uri));
        }
        //now the stack should be: undoDelete, undoDelete, undoDeleteAll.
        //now we call undo(uri) on a uri in the command set at the bottom of the stack.
        assertEquals(11, this.dsi.getStackSize());
        this.dsi.undo(pdfuri1);
        assertEquals(11, this.dsi.getStackSize());

        //now that PDF1 is back in the store, search for "document" which was previously deleted (prefix doc).
        assertNotNull(this.dsi.getDocument(pdfuri1));
        System.out.println(this.dsi.getDocument(pdfuri1).getDocumentAsTxt());
        assertEquals(1, this.dsi.search("DOCUMENT").size());
        assertEquals(this.pdftxt1, this.dsi.search("document").get(0));

        //undo it again, PDF1 should be unadded/deleted.
        this.dsi.undo(pdfuri1);
        assertEquals(10, this.dsi.getStackSize());
        assertTrue(this.dsi.search("document").isEmpty());
        assertNull(this.dsi.getDocument(pdfuri1));

        boolean pass = false;
        try {
            this.dsi.undo(pdfuri1);
        }catch (IllegalStateException e) {
            pass = true;
        }
        assertTrue(pass);
    }
}
