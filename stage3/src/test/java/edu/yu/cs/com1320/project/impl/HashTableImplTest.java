package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HashTableImplTest {

    public HashTableImpl<Integer,String> hashtable = new HashTableImpl<>(1, .20);

    @Before
    public void setup() {
        hashtable.put(96, "ninety six");
        hashtable.put(24, "twenty four");
        hashtable.put(11, "eleven");
        hashtable.put(100, "one hundred");
        hashtable.put(18, "eighteen");
        hashtable.put(12, "twelve");
        hashtable.put(300, "three hundred");
        hashtable.put(4, "four");
        hashtable.put(5, "five");
        hashtable.put(6, "six");
        hashtable.put(7, "seven");
        hashtable.put(8, "eight");
        hashtable.put(9, "nine");
        hashtable.put(10, "ten");
        hashtable.put(34, "thirty four");
        hashtable.put(17, "seventeen");
        hashtable.put(15, "fifteen");

    }

    @After
    public void teardown(){
        hashtable = new HashTableImpl<>();
    }

    @Test
    public void getExistingKey() {
        assertEquals("twenty four", hashtable.get(24));
    }

    @Test
    public void getNonExistentKey() {
        //return null
        assertNull(this.hashtable.get(1234));
    }

    @Test (expected = IllegalArgumentException.class)
    public void getNullKey() {
        //throw exception
        assertNull(this.hashtable.get(null));

    }


    @Test (expected = IllegalArgumentException.class) //@76
    public void nullKeyNullValue() {
        this.hashtable.put(null, null);
    }

    @Test (expected = IllegalArgumentException.class) //@76
    public void nullKeyExistingValue() { //should not add
        this.hashtable.put(null, "eleven");
    }

    @Test (expected = IllegalArgumentException.class) //@76
    public void nullKeyNewValue() {
        this.hashtable.put(null, "zero");
    }

    @Test (expected = IllegalArgumentException.class)
    public void newKeyNullValue() {
        //don't add, throw exception
        this.hashtable.put(2, null);
    }

    @Test
    public void newKeyNewValue() {
        //add to hashtable, return null
        assertNull(this.hashtable.put(2, "two"));
        assertEquals("two", this.hashtable.get(2));
    }

    @Test
    public void newKeyExistingValue() {
        //add, return null
        assertNull(this.hashtable.put(3, "three"));
        assertEquals("three", this.hashtable.get(3));
    }

    @Test
    public void existingKeyNullValue() {
        //delete value @ key, return value deleted.
        assertEquals("18 didn't return eighteen", "eighteen", hashtable.get(18));
        hashtable.put(18, null);
        assertNull("18 key-value pair wasn't deleted", hashtable.get(18));
    }

    @Test
    public void existingKeyNewValue() {
        //replace oldValue with newValue, return oldValue
        assertEquals("one hundred", this.hashtable.put(100, "one-hundred, replaced."));
        assertEquals("one-hundred, replaced.", this.hashtable.get(100));
    }

    @Test
    public void existingKeyExistingValue() {
        //don't add, return existing value
        assertEquals( "ninety six", this.hashtable.put(96, "ninety six"));
    }
}