package edu.yu.cs.com1320.project.impl;

class Entry<Key, Value> {
    Entry<Key, Value> next;
    final Key key;
    Value value; // ie: the element in this spot in the list.

    Entry(Key key, Value value) {
        this.key = key;
        this.value = value;
        this.next = null;
    }
    Entry(Key key, Value value, Entry<Key, Value> newEntry) {
        this.key = key;
        this.value = value;
        this.next = newEntry;
    }
}
