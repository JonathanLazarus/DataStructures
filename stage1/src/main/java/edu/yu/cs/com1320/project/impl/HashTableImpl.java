package edu.yu.cs.com1320.project.impl;
import edu.yu.cs.com1320.project.HashTable;

public class HashTableImpl<Key,Value> implements HashTable<Key,Value> {
    private Entry<Key,Value>[] hashArrayOfLists;

    @SuppressWarnings("unchecked")
    public HashTableImpl() {
        hashArrayOfLists = new Entry[5];
        for (int i = 0; i < 5; i++) {
            hashArrayOfLists[i] = null;
        }
    }

    public Value put(Key k, Value v) {
        if (k == null) { throw new IllegalArgumentException(); }
        int k2 = k.hashCode();
        int index = hashFunction(k2);
        if (v == null) {
            if (this.get(k) == null) {//NEW KEY WITH NULL VALUE
                throw new IllegalArgumentException();//q asked on piazza
            }
            return deleteEntry(k, index);
        }
        //if a key was never hashed to this index, add the entry (as the first entry/head: [i])
        if (hashArrayOfLists[index] == null) {
            hashArrayOfLists[index] = new Entry<>(k,v);
            return null;
        }
        Entry<Key,Value> temp = hashArrayOfLists[index];
        while (temp != null && temp.key != k) {
            temp = temp.next;
        }
        /* since the while loop broke, and according to the next if statement, the key at temp
        must be equal to the given key. two choices now:
        1) if the values are the same, do nothing, return the existing value.
        2) if they are not equal, swap them out (but @return the old value)[@37] */
        if (temp != null) {
            if (temp.value.equals(v)){
                return v;
            }
            if (!temp.value.equals(v)) {
                Value oldValue = temp.value;
                temp.value = v;
                return oldValue;
            }
        }
        //otherwise, since we've reached the end of the list, and the key was not present, add this <K,V> to the list.
        hashArrayOfLists[index] = new Entry<Key,Value>(k, v, hashArrayOfLists[index]);
        return null;
    }

    public Value get(Key k) {
        if (k == null) { throw new IllegalArgumentException(); }
        int k2 = k.hashCode();
        int index = hashFunction(k2);
        if (hashArrayOfLists[index] == null) return null;
        Entry<Key,Value> temp = hashArrayOfLists[index];
        while (temp != null && temp.key != k) {
            temp = temp.next;
        }
        if (temp != null) {// key == key at temp
            return temp.value;
        }
        return null;
    }

    private Value deleteEntry (Key key, int index) {
        //Entry<Key, Value> previous = null;
        Entry<Key, Value> current = hashArrayOfLists[index];
        Value valueBeingDeleted = null;
        if (current.key == key) {
            valueBeingDeleted = current.value;
            this.hashArrayOfLists[index] = null;
            return valueBeingDeleted;
        }
        while(current.next != null && current.next.key != key) {
            current = current.next;
        }
        if(current.next != null) {
            valueBeingDeleted = current.next.value;
            current.next = current.next.next;
        }
        return valueBeingDeleted;
    }

    private int hashFunction(int key) {
        return (key & 0x7fffffff) % this.hashArrayOfLists.length;

    }

    class Entry<K,V> {
        private Entry<K, V> next;
        private K key;
        private V value; // ie: the element in this spot in the list.

        private Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }
        private Entry(K key, V value, Entry<K, V> newEntry) {
            this.key = key;
            this.value = value;
            this.next = newEntry;
        }
    }

}
