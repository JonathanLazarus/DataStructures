package edu.yu.cs.com1320.project.impl;

class BTNode<Key extends Comparable<Key>, Value>  {
    protected BTEntry<Key, Value>[] entries;
    protected int entryCount; //keeps track of how many entries are currently in this node.

    BTNode () {
        this.entries = (BTEntry<Key, Value>[]) new BTEntry[BTreeImpl.MAX];
        this.entryCount = 0;
    }

    //@param initSize creation of a BTNode with an intended size.
    BTNode(int initSize) {
        this.entries = (BTEntry<Key, Value>[]) new BTEntry[BTreeImpl.MAX];
        this.entryCount = initSize;
    }
}
