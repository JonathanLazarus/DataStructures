package edu.yu.cs.com1320.project.stage3.impl;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class DocumentWordCountTest {
    private final HashMap<String, Integer> wordCount = new HashMap<>();

    /*tests the wordCount method in DOcumentImpl.
      Instead of creating a new document object here, i just copied the method, and instead of using this.text in DOcument, i will pass the text i want to test as the first parameter.
      @param text: the string i want to test on
      @param word the word i want the count of.
    */

    public int wordCount(String word) {
        return this.wordCount.getOrDefault(word.toUpperCase(), 0);
    }

    private void getWordCount(String text) {
        Arrays.stream(text.split("[^a-zA-Z0-9]+"))
                .forEach(word -> this.wordCount
                        .put(word.toUpperCase(), this.wordCount.getOrDefault(word.toUpperCase(), 0)+1));
    }

    /*
    public int wordCount(String text, String word) {
        if (word == null) { throw new IllegalArgumentException(); }
        return Arrays.stream(text.split("[^a-zA-Z0-9]+"))
                .filter(docWord -> docWord.compareToIgnoreCase(word) == 0)
                .mapToInt(docWord -> 1)
                .sum();
    }*/

    @Test
    public void periodsAndSpaces() {
        String text = "We are testing this string using the word example. Even though example is followed by a period in the first sentence, it should still be counted. Example should also be counted even though it is capitalized. same goes for the word eXamplE, EXAMPLE, and EXampLe. Example word count is 7";
        getWordCount(text);
        assertEquals(7, wordCount("exaMple"));
    }

    @Test
    public void extraSpaceAndCharacters() {
        String text = "We are testing this     string using the word     example. Even though example\tis followed by a period in the first sentence, it should still be counted. Example 1 should also be counted even though it is capitalized. same goes for the word /forty/eXamplE], EXAMPLE@, and EXampLe-. Example word count is 7";
        getWordCount(text);
        assertEquals(7, wordCount("Example"));
    }

    @Test
    public void numbers() {
        String text = "we want to know how many times 407 is used in this string. 4079. 407. 40 7, 40.7, 0407, 4 0 7, 4-0-7, @407&&, 407-987+270 = N. the count for number 407 is 5.";
        getWordCount(text);
        assertEquals(5, wordCount("407"));
    }

    @Test
    public void wordNotInTextReturns0() {
        String text = "we want to know how many times 407 is used in this string. 4079. 407. 40 7, 40.7, 0407, 4 0 7, 4-0-7, @407&&, 407-987+270 = N. the count for number 407 is 5.";
        getWordCount(text);
        assertEquals(0, wordCount("Lazarus"));
    }
}
