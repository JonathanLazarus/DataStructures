package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.DocumentStore;
import jdk.nashorn.internal.ir.debug.ClassHistogramElement;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

import static org.junit.Assert.*;

public class MyLatestDocumentStoreImplTest {
    private DocumentStoreImpl dsi;
    private DocumentImpl[] docsInOrder;
    private URI[] uris;

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

    public MyLatestDocumentStoreImplTest() throws Exception {
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
        this.dsi = new DocumentStoreImpl();
        this.dsi = new DocumentStoreImpl();

        this.uris = new URI[]{this.uri1, this.pdfuri1, this.uri2, this.pdfuri2, this.uri3, this.pdfuri3, this.uri4, this.pdfuri4};
        this.docsInOrder = new DocumentImpl[8];
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

    @Before
    public void setUp() throws Exception {
        this.dsi = new DocumentStoreImpl();
        //Add documents.
        //doc1
        ByteArrayInputStream bas = new ByteArrayInputStream(this.txt1.getBytes());
        this.dsi.putDocument(bas,this.uri1, DocumentStore.DocumentFormat.TXT);
        //pdfdoc1
        bas = this.getDocumentAsPdfIS(this.pdftxt1);
        this.dsi.putDocument(bas,this.pdfuri1, DocumentStore.DocumentFormat.PDF);
        //doc2
        bas = new ByteArrayInputStream(this.txt2.getBytes());
        this.dsi.putDocument(bas,this.uri2, DocumentStore.DocumentFormat.TXT);
        //pdfdoc2
        bas = this.getDocumentAsPdfIS(this.pdftxt2);
        this.dsi.putDocument(bas,this.pdfuri2, DocumentStore.DocumentFormat.PDF);
        //doc3
        bas = new ByteArrayInputStream(this.txt3.getBytes());
        this.dsi.putDocument(bas,this.uri3, DocumentStore.DocumentFormat.TXT);
        //pdfdoc3
        bas = this.getDocumentAsPdfIS(this.pdftxt3);
        this.dsi.putDocument(bas,this.pdfuri3, DocumentStore.DocumentFormat.PDF);
        //doc4
        bas = new ByteArrayInputStream(this.txt4.getBytes());
        this.dsi.putDocument(bas,this.uri4, DocumentStore.DocumentFormat.TXT);
        //pdfdoc4
        bas = this.getDocumentAsPdfIS(this.pdftxt4);
        this.dsi.putDocument(bas,this.pdfuri4, DocumentStore.DocumentFormat.PDF);

        for (int i = 0; i < this.uris.length; i++) {
            this.docsInOrder[i] = this.dsi.getDocument(this.uris[i]);
        }
    }

    @Test
    public void putDocument() {
        assertEquals(this.dsi.getDocCount(), 8);
        
        //TODO put() an existing document: returns

    }

    @Test
    public void testReplacePut() {
        //test that when putting a new doc with an existing uri replaces it
        //put a new doc with an existing uri: returns the old hashcode, removes old one from the store
        assertEquals(this.txt3, this.dsi.search("goose").get(0));
        //put new doc (with text) at uri3
        String text = "this text contains armadillo";
        assertEquals("replace put did not return the hashcode of the doc that was replaced.", this.txt3.hashCode(), this.dsi.putDocument(new ByteArrayInputStream(text.getBytes()), this.uri3, DocumentStore.DocumentFormat.TXT));
        //now check that "goose," previously in the trie, is not there anymore
        assertTrue("goose was in the trie. doc3 was not replaced & removed.", this.dsi.search("goose").isEmpty());
        //now check that the text was updated
        assertEquals("text was not updated at uri3.", text, this.dsi.getDocumentAsTxt(uri3));
        //now check the trie has been updated:
        assertEquals(text, this.dsi.search("armadillo").get(0));

        //now undo the replace
        long time = System.nanoTime();
        this.dsi.undo();
        //check that a trie search for armadillo is empty
        assertTrue(this.dsi.search("armadillo").isEmpty());
        //check that the time was updated after the undo
        assertTrue(time < this.getDocLastTime(uri3));
        //assert goose is back in trie.
        assertEquals(this.txt3, this.dsi.search("goose").get(0));

    }

    private long getDocLastTime(URI uri) {
        return this.dsi.getDocument(uri).getLastUseTime();
    }

    private void assertCommandStack(int expected) {
        assertEquals("Incorrect amount of commands on commandStack", expected, this.dsi.getStackSize());
    }
    
    @Test
    public void putExistingDocument() {
        //returns the docs hashcode.
        //updates the docs time. (reheapify)
        //pushes an empty command

        //for this test we'll use Doc1 since it was the latest used.
        long time = this.getDocLastTime(uri1);
        assertCommandStack(8);

        //assert that doc2 time is more recent since it was put second
        assertTrue(time < this.dsi.getDocument(uri2).getLastUseTime());
        //assert that the hashCode returned is the same
        assertEquals(txt1.hashCode(), this.dsi.putDocument(new ByteArrayInputStream(this.txt1.getBytes()) ,this.uri1, DocumentStore.DocumentFormat.TXT));
        //assert that a new command has been added
        assertCommandStack(9);
        //assert that the new Doc1 time is greater than its put in time
        //System.out.println("updated doc1 time: " + this.getDocLastTime(uri1));
        assertTrue(time < getDocLastTime(uri1));
        // assert that doc2 time is less recent now since Doc1 was just accessed and
        assertTrue(getDocLastTime(uri1) > getDocLastTime(uri2));

        //only undoes the empty cmd
        this.dsi.undo(uri1);
        assertCommandStack(8);

        //assert that only the empty command with this uri was undone. not the other.
        assertNotNull(this.dsi.getDocument(uri1));
    }

    @Test
    public void getDocumentAsPdf() {
        //test update time
        assertNotNull(this.dsi.getDocumentAsPdf(uri3));
        //to delete doc3 gauge
        this.dsi.setMaxDocumentCount(3);
        assertCommandStack(3);
        //assert its still in store
        assertEquals(this.txt3, this.dsi.getDocument(uri3).getDocumentAsTxt());

    }

    @Test
    public void getDocumentAsTxt() {
        //test update time
        assertEquals(this.txt3, this.dsi.getDocumentAsTxt(uri3));
        //to delete doc3 gauge
        this.dsi.setMaxDocumentCount(3);
        assertCommandStack(3);
        //assert its still in store
        assertEquals(this.txt3, this.dsi.getDocument(uri3).getDocumentAsTxt());
    }

    @Test
    public void deleteDocument() {
        assertCommandStack(8);
        //delete pdf1
        assertTrue(this.dsi.deleteDocument(pdfuri1));
        assertCommandStack(9);
        //undo the delete - should update the docs last used time.
        this.dsi.undo();
        this.dsi.setMaxDocumentCount(1);
        this.assertCommandStack(1);
        //check that the only doc left is the one that was just updated.
        assertEquals(pdftxt1, this.dsi.getDocumentAsTxt(pdfuri1));

        //check that this pushes an empty command
        assertFalse(this.dsi.deleteDocument(uri1));
        //assert there is now one more (empty) command
        assertCommandStack(2);
    }

    @Test
    public void undo() {
    }

    @Test
    public void testUndo() {
    }

    @Test
    public void search() {
        this.dsi.setMaxDocumentCount(3);
        //make sure that "unique" is not in the trie - ie doc1 was deleted.
        assertTrue(this.dsi.search("unique").isEmpty());

        long timePDF4 = this.getDocLastTime(pdfuri4);
        long timeDoc4 = this.getDocLastTime(uri4);
        long timePDF3 = this.getDocLastTime(pdfuri3);
        //make sure everything is in order
        assertTrue(timePDF4 > timeDoc4 && timeDoc4 > timePDF3);
        //check that search works - update time.
        assertEquals(pdftxt3, this.dsi.search("PDF3").get(0));
        assertTrue("time was not updated", getDocLastTime(pdfuri3) > timePDF3);
        assertTrue("time was not updated", getDocLastTime(pdfuri3) > timePDF4);
    }

    @Test
    public void searchPDFs() {
    }

    @Test
    public void searchByPrefix() {
        for(String txt : this.dsi.searchByPrefix("for")) {
            System.out.println(txt);
        }
        //doc4 and pdf4 don't contain the word "for".
        this.dsi.setMaxDocumentCount(6);
        //doc4 and pdf4 should have been deleted.
        assertNull(this.dsi.getDocument(uri4));
        assertNull(this.dsi.getDocument(pdfuri4));
        assertCommandStack(6);

        //check that they've all ben set to the same time
        long time = this.getDocLastTime(uri1);
        for (URI uri : this.uris) {
            if (uri == this.pdfuri4 || uri == this.uri4) continue;
            assertEquals("time was not the same", time, this.getDocLastTime(uri));
        }

    }

    @Test
    public void searchPDFsByPrefix() {
    }

    @Test
    public void deleteAll() {
        assertCommandStack(8);
        //first check that an empty cmd is pushed for an empty deleteAll
        assertTrue(this.dsi.deleteAll("unanimously").isEmpty());
        assertCommandStack(9);
        //undo the empty cmd
        this.dsi.undo();

        //now back to normal. lets check that deleteAll Works
        int thisSearch = this.dsi.search("this").size();
        Set<URI> set = this.dsi.deleteAll("this"); //doc1, PDF3, doc3
        assertEquals(thisSearch, set.size());
        for (URI uri : set) {
            assertNull(this.dsi.getDocument(uri));
        }
        assertNull(this.dsi.getDocument(uri3));

        this.dsi.undo(uri3);
        //uri3 should be put back in
        assertNotNull(this.dsi.getDocument(uri3));
        //uri1 should still be deleted
        assertNull(this.dsi.getDocument(uri1));

        this.dsi.setMaxDocumentCount(5);
        //pdf1 is deleted
        assertNull(this.dsi.getDocument(pdfuri1));
        //make sure that doc2 and pdf2 are there - they're on deck to be deleted.
        assertNotNull(this.dsi.getDocument(uri2));
        assertNotNull(this.dsi.getDocument(pdfuri2));
        //undo the deleteAll completely. now doc1 and pdf3 are back, but they remove doc2 and pdf2
        this.dsi.undo();
        //assert they've been deleted.
        assertNull(this.dsi.getDocument(uri2));
        assertNull(this.dsi.getDocument(pdfuri2));
        //now a search of 'this' should return size 3
        assertEquals(3, this.dsi.search("this").size());
    }

    @Test
    public void deleteAllWithPrefix() {
        assertCommandStack(8);
        //first check that an empty cmd is pushed for an empty deleteAll
        assertTrue(this.dsi.deleteAllWithPrefix("unanimously").isEmpty());
        assertCommandStack(9);
        //undo the empty cmd
        this.dsi.undo();

        //now back to normal. only PDF4 and DOC4 don't contain the prefix "for"
        Set<URI> set = this.dsi.deleteAllWithPrefix("for");
        assertEquals(6, set.size());
        assertTrue(this.dsi.search("apostrophe").isEmpty());
        assertTrue(this.dsi.searchByPrefix("apostrophe").isEmpty());

        this.dsi.undo();
        assertFalse(this.dsi.searchByPrefix("apostrophe").isEmpty());
    }

    @Test
    public void setMaxDocumentCount() {
        assertCommandStack(8);

        this.dsi.setMaxDocumentCount(5);
        //deletes three documents
        assertCommandStack(5);

        assertNull(this.dsi.getDocument(uri1));
        assertNull(this.dsi.getDocument(pdfuri1));
        assertNull(this.dsi.getDocument(uri2));
        this.dsi.printCounts();
        assertEquals(5, this.dsi.getDocCount());

        //check that PDF2 is in the store (its currently now the last used doc. its on deck to be deleted.
        assertEquals(pdftxt2, this.dsi.getDocument(pdfuri2).getDocumentAsTxt());
        //now @Test that adding a doc to a maxed out store removes the last used doc.
        this.dsi.putDocument(new ByteArrayInputStream(txt1.getBytes()), uri1, DocumentStore.DocumentFormat.TXT);
        //now that another doc has been added, PDF2 should have been removed and stack size should still be 5
        assertCommandStack(5);
        assertNull(this.dsi.getDocument(pdfuri2));
    }

    @Test
    public void setMaxDocCount () {
        //search for a word so then the doc time is updated
        assertCommandStack(8);

        //PDF2 contains this word - now this should update the time for PDF2
        assertEquals(pdftxt1, this.dsi.search("PDF1").get(0));
        //deletes three documents
        this.dsi.setMaxDocumentCount(5);
        assertEquals(5, this.dsi.getDocCount());

        //check that the last three objects were removed: doc1, doc2, pdfdoc2.
        assertNull(this.dsi.getDocument(uri1));
        assertNull(this.dsi.getDocument(pdfuri2));
        assertNull(this.dsi.getDocument(uri2));
    }

    @Test
    public void setMaxDocumentBytes() {
        int limit = 500;
        int dsBytes = this.dsi.getByteCount();
        int byteCount = 0;
        int docCount = 0;
        for (DocumentImpl document : this.docsInOrder) {
            if (byteCount >= dsBytes - limit) {
                break;
            }
            int bytes = document.getDocumentByteCount();
            byteCount += bytes;
            docCount++;
        }
        int numb = this.dsi.getDocCount();
        this.dsi.setMaxDocumentBytes(limit);
        assertEquals(numb - docCount, this.dsi.getDocCount());
        //try putting in a doc that's over the byte max - does nothing
        assertEquals("test failed. file was added",0, this.dsi.putDocument(new ByteArrayInputStream("my armadillo is the best penguin ever".getBytes()), this.uri3, DocumentStore.DocumentFormat.TXT));
        //make sure that armadillo isn't in trie.
        assertTrue(this.dsi.search("armadillo").isEmpty());
        //amd that uri3 isnt in the hashtable
        assertNull(this.dsi.getDocument(uri3));
    }
}