package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HashTableImpl<Key,Value> implements HashTable<Key,Value> {
    private Entry[] hashArray;
    private final double loadFactor; //user-specified load factor for efficiency
    private int entryCount; //keeps count of how many entries are in the hashtable

    public HashTableImpl() {
        this.hashArray = new Entry[5];
        this.loadFactor = 0.75;
    }
    public HashTableImpl(int length, double loadFactor) {
        hashArray = new Entry[length];
        this.loadFactor = loadFactor;
        this.entryCount = 0;
    }

    /* this method checks that the entryCount/arraySize is less than the load factor.
     * if not, double the underlying array.
     */
    private void checkLoadFactor() {
        entryCount++;
        if ((double) this.entryCount / (double) this.hashArray.length > this.loadFactor) {
            Entry[] tempArray = this.hashArray;
            this.hashArray = new Entry[2 * this.hashArray.length];
            for (Entry entry : tempArray) {
                while (entry != null) {
                    put((Key) entry.key, (Value) entry.value);
                    entry = entry.next;
                }
            }
        }

    }

    public Value put(Key k, Value v) {
        if (k == null) { throw new IllegalArgumentException(); }
        int k2 = k.hashCode();
        int index = hashFunction(k2);
        if (v == null) {
            if (this.get(k) == null) {//NEW KEY WITH NULL VALUE
                throw new IllegalArgumentException("cannot add a null Value with a new Key");//q asked on piazza
            }
            return deleteEntry(k, index);
        }
        //if a key was never hashed to this index, add the entry (as the first entry/head: [i])
        if (hashArray[index] == null) {
            hashArray[index] = new Entry(k, v);
            return null;
        }
        Entry temp = hashArray[index];

        while (temp != null && temp.key != k) {
            temp = temp.next;
        }

        /* since the while loop broke, and according to the next if statement, the key at temp
        must be equal to the given key. two choices now:
        1) if the values are the same, do nothing, return the existing value.
        2) if they are not equal, swap them out (but @return the old value)[@37] */
        if (temp != null) {
            if (temp.value.equals(v)){
                return (Value) temp.value;
            }
            else {
                Value oldValue = (Value) temp.value;
                temp.value = v;
                return oldValue;
            }
        }
        //otherwise, since we've reached the end of the list, and the key was not present, add this <K,V> to the list.
        Entry list = hashArray[index];
        hashArray[index] = new Entry(k, v, list);
        checkLoadFactor();
        return null;
    }

    public Value get(Key k) {
        if (k == null) {
            throw new IllegalArgumentException();
        }
        int k2 = k.hashCode();
        int index = hashFunction(k2);
        Entry temp = hashArray[index];
        if (temp == null) {
            return null;
        }
        while (temp != null && temp.key != k) {
            temp = temp.next;
        }
        if (temp != null) {// key == key at temp
            return (Value) temp.value;
        }
        return null;
    }

    private Value deleteEntry (Key key, int index) {
        Entry current = hashArray[index];
        Value valueBeingDeleted = null;
        if (current.key == key) {
            valueBeingDeleted = (Value) current.value;
            this.hashArray[index] = current.next;
            return valueBeingDeleted;
        }
        while(current.next != null && current.next.key != key) {
            current = current.next;
        }
        if(current.next != null) {
            valueBeingDeleted = (Value) current.next.value;
            current.next = current.next.next;
        }
        return valueBeingDeleted;
    }

    private int hashFunction(int key) {
        return (key & 0x7fffffff) % this.hashArray.length;
    }
}