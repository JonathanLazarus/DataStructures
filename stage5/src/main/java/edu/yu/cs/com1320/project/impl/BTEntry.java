package edu.yu.cs.com1320.project.impl;

//internal nodes: only use key and child
//external nodes: only use key and value
class BTEntry<Key extends Comparable<Key>, Value> {
    protected Key key;
    protected Value val;
    protected BTNode<Key, Value> child;

    BTEntry(Key key, Value val, BTNode<Key, Value> child)
    {
        this.key = key;
        this.val = val;
        this.child = child;
    }
    protected Value getValue()
    {
        return this.val;
    }
    protected Key getKey()
    {
        return this.key;
    }
}
