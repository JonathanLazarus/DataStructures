package edu.yu.cs.com1320.project.impl;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class TrieImplTest4 {
    private TrieImpl<Integer> trie = new TrieImpl<>(36);
    private TrieImpl<Integer> myTrie;
    private Comparator<Integer> intComparator;


    public TrieImplTest4() {
        trie.put("odd", 1);
        trie.put("odd", 3);
        trie.put("odd", 5);
        trie.put("odd", 7);

        trie.put("even", 2);
        trie.put("even", 4);
        trie.put("even", 6);
        trie.put("even", 8);

        trie.put("event", 20);
        trie.put("event", 40);
        trie.put("event", 60);
        trie.put("event", 80);

        trie.put("evenZings", 51);

        trie.put("events", 1);
        trie.put("events", 3);
        trie.put("events", 5);
        trie.put("events", 7);
        trie.put("events", 2);
        trie.put("events", 4);
        trie.put("events", 6);
        trie.put("events", 8);

        this.intComparator = (o1, o2) -> {
            if (o1 > o2) {
                return 1;
            }
            if (o1.equals(o2)) {
                return 0;
            }
            return -1;
        };
    }

    @Before
    public void setUp() throws Exception {
        this.myTrie = this.trie;
    }

    /* shows that:
     * 1. put() and getAllSorted() is not case specific
     */
    @Test
    public void put() {
        this.myTrie.put("MothEr", 5);
        List<Integer> intList = this.myTrie.getAllSorted("moTher", this.intComparator);
        Integer[] intArray = {5};
        assertArrayEquals( intArray, intList.toArray());
    }

    @Test
    public void getAllSorted() {
        List<Integer> intList = this.myTrie.getAllSorted("Events", this.intComparator);
        Integer[] intArray = {1,2,3,4,5,6,7,8};
        assertArrayEquals( intArray, intList.toArray());

        //assert that "zoo" which is not in the trie, returns an empty list.
        assertTrue(this.myTrie.getAllSorted("zoo", this.intComparator).isEmpty());
    }

    @Test
    public void getAllWithPrefixSorted() {
        List<Integer> evList = this.getAllWithPrefixSorted("eV");
        Integer[] evArray = {1,2,3,4,5,6,7,8,20,40,51,60,80};
        String evMessage = "prefix: eV did not return all values of even, evenZings, event, and events.";
        assertArrayEquals(evMessage, evArray, evList.toArray());

        List<Integer> evenList = this.getAllWithPrefixSorted("eVeN");
        Integer[] evenArray = {1,2,3,4,5,6,7,8,20,40,51,60,80};
        String evenMessage = "prefix: eVeN did not return all values of event, evenZings, and events.";
        assertArrayEquals(evenMessage, evenArray, evenList.toArray());

        List<Integer> evenzList = this.getAllWithPrefixSorted("evenz");
        Integer[] evenzArray = {51};
        String evenzMessage = "prefix: evenz did not return all values of evenZings";
        assertArrayEquals(evenzMessage, evenzArray, evenzList.toArray());

        //assert that "zoo" which is not in the trie, returns an empty list.
        assertTrue("zoo did not return empty", this.getAllWithPrefixSorted("zoo").isEmpty());
        //assert that "events" which is a prefix for events, returns an 8.
        assertEquals("events did not return empty", 8, this.getAllWithPrefixSorted("evenTs").size());
    }

    private List<Integer> getAllWithPrefixSorted(String prefix) {
        return this.myTrie.getAllWithPrefixSorted(prefix, this.intComparator);
    }

    @Test
    public void deleteAllWithPrefix() {
        Integer[] eArray = {1,2,2,3,4,4,5,6,6,7,8,8,20,40,51,60,80};
        HashSet<Integer> eSet = new HashSet<>(Arrays.asList(eArray));
        assertEquals("deleted values of prefix e did not match values of prefix e", eSet, this.myTrie.deleteAllWithPrefix("e"));

        //now check that they were really deleted.
        List<Integer> intList = this.myTrie.getAllSorted("evenZings", this.intComparator);
        assertTrue("values of \"evenzings\" weren't deleted.", intList.isEmpty());
    }

    @Test
    public void deleteAll() {
        //assert empty collection for word not in trie.
        assertTrue(this.myTrie.deleteAll("fire").isEmpty());

        Integer[] eventArray = {20,40,60,80};
        HashSet<Integer> eventSet = new HashSet<>(Arrays.asList(eventArray));
        assertEquals(eventSet, this.myTrie.deleteAll("eveNT"));
        assertTrue(this.myTrie.deleteAll("evenT").isEmpty());

        List<Integer> evList = this.getAllWithPrefixSorted("eV");
        Integer[] evArray = {1,2,3,4,5,6,7,8,51};
        String evMessage = "prefix: eV did not return a set of all values of even, evenZings, and events.";
        assertArrayEquals(evMessage, evArray, evList.toArray());
    }

    @Test
    public void delete() {
        assertEquals((Integer) 5, this.myTrie.delete("ODd", 5));
        Integer[] oddArray = {1,3,7};
        assertArrayEquals(oddArray, this.myTrie.getAllSorted("odd", this.intComparator).toArray());
    }
}