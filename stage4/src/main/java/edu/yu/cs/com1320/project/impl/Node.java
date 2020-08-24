package edu.yu.cs.com1320.project.impl;

import java.util.HashSet;
import java.util.Set;

class Node<Value> {
    @SuppressWarnings("rawtypes")
    Node[] links;
    Set<Value> value;

    Node() {
        links = new Node[TrieImpl.alphabetCharSize];
        value = new HashSet<>();
    }

    void setNewLinks() {
        this.links = new Node[TrieImpl.alphabetCharSize];
    }
}
