package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.IOException;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key, Value> {
    //max Entry(s) per B-tree node = MAX-1
    protected static int MAX = 6; //MAX must be an even number and greater than 2
    private BTNode<Key, Value> root;
    private int height; //height of the B-Tree
    private Key sentinel;
    private PersistenceManager<Key, Value> pm;

    //for testing purposes only
    protected int nodeCount = 0;

    //no param constructor. If this constructor was used, then the first this.put value should be the Sentinel.
    public BTreeImpl() {
        this.root = new BTNode<>();
        this.height = 0;
    }

    //constructor sets MAX to 6 by default.
    //@param sentinel: the min value that should be used as the Sentinel for this specific Key type.
    public BTreeImpl(Key sentinel) {
        this.root = new BTNode<>();
        //set the first entry in the root node to the sentinel
        this.root.entries[0] = new BTEntry<>(sentinel, null, null);
        this.root.entryCount++;
        this.height = 0;
        this.sentinel = sentinel;
    }

    //constructor that sets @param max to be the MAX
    public BTreeImpl(int max, Key sentinel) {
        if (max % 2 != 0) throw new IllegalArgumentException("MAX must be an even number.");
        this.root = new BTNode<>(1);
        //set the first entry in the root node to the sentinel
        this.root.entries[0] = new BTEntry<>(sentinel, null, null);
        this.height = 0;
        MAX = max;
    }

    @Override
    public Value get(Key k) {
        if (k == null) throw new IllegalArgumentException("User may not pass \"null\" as a parameter for BTree.get()");
        BTEntry<Key, Value> entryReceived = this.get(this.root, k, this.height);
        if (entryReceived != null) {
            if (entryReceived.val != null) {
                return entryReceived.val;
            }
            //else, check if the value has been written to disk. Deserialize method will return null if it was not serialized, or the Value if it has been serialized.
            try {
                return this.pm.deserialize(k);
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private BTEntry<Key, Value> get(BTNode<Key, Value> currentNode, Key k, int height) {
        //if the node is external, the value is at the Entry with key==k.
        if (height == 0) {
            for (int i = 0; i < currentNode.entryCount; i++) {
                //if the keys match, return the current entry.
                if (currentNode.entries[i].key.compareTo(k) == 0) {
                    return currentNode.entries[i];
                }
            }
            //else, the key is not present in this BTree.
            return null;
        } else //the node is internal
        {
            for (int i = 0; i < currentNode.entryCount; i++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be in the subtree below the current entry, a.k.a.
                //entries[i]), then recurse into the current entry’s child
                if (i + 1 == currentNode.entryCount || this.isLess(k, currentNode.entries[i + 1].key)) {
                    //recurse into the current Entry's child
                    return this.get(currentNode.entries[i].child, k, height - 1);
                }
            }
        }
        //else, key is not in tree, return null.
        return null;
    }

    private boolean isLess(Key k, Key key) {
        if (k == null || key == null) throw new IllegalArgumentException();
        int compared = k.compareTo(key);
        return compared < 0;
    }


    @Override
    public Value put(Key k, Value v) {
        if (k == null)
            throw new IllegalArgumentException("User may not pass null as the \"key\" parameter for BTree.put()");
        BTEntry<Key, Value> entry = this.get(this.root, k, this.height);
        if (entry != null) {
            //return the value we are replacing
            Value temp;
            //the value can either be on disk or in memory. Let's check memory first:
            if (entry.val != null) {
                temp = entry.val;
            }
            else {
                //the value is on disk, if not, then we return null anyways
                try {
                    temp = this.pm.deserialize(k);
                } catch (IOException e) {
                    temp = null;
                }
            }
            //replace/put the new value; @null is effectively a delete here.
            entry.val = v;
            return temp;
        }
        //at this point, if the value is null (essentially a delete), and the Key wasn't in the btree, then there's no point of adding a <k, null> pair, so just return null
        if (v == null) {
            return null;
        }
        BTNode<Key, Value> newNode = this.put(this.root, k, v, this.height);
        //a new value was added - return null
        if (newNode == null) {
            return null;
        }
        //else, if the newNode wasn't null, that means that the root was split in the recursive put(),
        //and the method returned the second half of the root that was split (@newNode). So now we have to restructure the root.
        BTNode<Key, Value> newRoot = new BTNode<>(2);
        //the first entry in newRoot is the first entry of currentRoot with currentRoot as its child
        BTEntry<Key, Value> first = new BTEntry<>(this.root.entries[0].key, null, this.root);
        newRoot.entries[0] = first;
        //the second entry in newRoot is the first entry of newNode with newNode as its child
        BTEntry<Key, Value> second = new BTEntry<>(newNode.entries[0].key, null, newNode);
        newRoot.entries[1] = second;
        //now set the new root
        this.root = newRoot;
        //a split at the root always increases the tree height by 1
        this.height++;
        return null;
    }

    //@return null if no new node was created (i.e. just added a new Entry into an existing node). If a new node was created due to the need to split, returns the new node
    private BTNode<Key, Value> put(BTNode<Key, Value> currentNode, Key k, Value v, int height) {
        int j;
        BTEntry<Key, Value> newEntry = new BTEntry<>(k, v, null);
        //external node
        if (height == 0) {
            //find index in currentNode’s entry[] to insert new entry
            //we look for key < entry.key since we want to leave j
            //pointing to the slot to insert the new entry, hence we want to find
            //the first entry in the current node that key is LESS THAN
            for (j = 0; j < currentNode.entryCount; j++) {
                if (this.isLess(k, currentNode.entries[j].key)) {
                    break;
                }
            }
        }

        // internal node
        else {
            //find index in node entry array to insert the new entry
            for (j = 0; j < currentNode.entryCount; j++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be added to the subtree below the current entry),
                //then do a recursive call to put on the current entry’s child
                if ((j + 1 == currentNode.entryCount) || this.isLess(k, currentNode.entries[j + 1].key)) {
                    //increment j (j++) after the call so that a new entry created by a split will be inserted in the next slot
                    BTNode<Key, Value> newNode = this.put(currentNode.entries[j++].child, k, v, height - 1);
                    if (newNode == null) {
                        return null;
                    }
                    //if the call to put returned a node, it means I need to add a new entry to the current node
                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        //Both for external/internal nodes: shift entries over one place to make room for new entry - at [j]
        for (int i = currentNode.entryCount; i > j; i--) {
            currentNode.entries[i] = currentNode.entries[i - 1];
        }
        //add new entry
        currentNode.entries[j] = newEntry;
        currentNode.entryCount++;
        if (currentNode.entryCount < MAX) {
            //no structural changes needed in the tree so just return null
            return null;
        }
        //else, the MAX limit for the node has been reached. Must split now.
        //will have to create new entry in the parent due to the split, so return the new node, which is the node for which the new entry will be created
        return this.split(currentNode, height);
    }

    /**
     * split node in half
     *
     * @param currentNode node to split
     * @return new node
     */
    private BTNode<Key, Value> split(BTNode<Key, Value> currentNode, int height) {
        BTNode<Key, Value> newNode = new BTNode<>(MAX / 2);
        //by changing currentNode.entryCount, we will treat any value
        //at index higher than the new currentNode.entryCount as if it doesn't exist
        currentNode.entryCount = MAX / 2;
        //copy top half of currentNode into newNode
        for (int j = 0; j < MAX / 2; j++) {
            newNode.entries[j] = currentNode.entries[MAX / 2 + j];
            //null out top half of currentNode to avoid memory leaks
            currentNode.entries[MAX / 2 + j] = null;
        }
        return newNode;
    }

    @Override
    public void moveToDisk(Key k) throws Exception {
        BTEntry<Key, Value> entry = this.get(this.root, k, this.height);
        if (entry != null) {
            //serialize it
            this.pm.serialize(k, entry.val);
            //remove value (Document) from memory.
            entry.val = null;
        }
    }

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.pm = pm;
    }

    //for testing purposes
    protected void printTree() {
        this.printTree(this.root, this.height);

    }

    private void printTree(BTNode<Key, Value> node, int height) {
        //(print the node's height)
        //print the node's entries[]
            //give the next node to the method above (recursion)
        //do the same with the node's children

        if (node == null) { return; }
        //TODO distinguish between external node level (h == 0), and others. makes difference for (3)
        //external node
        if (height == 0) {
            //1. print height - only for the first node of each line (only when the node.entries[0] is the sentinel). sentinel is always first entry's key of root.
            if (node.entries[0].key.compareTo(this.root.entries[0].key) == 0) {
                System.out.println("height " + (height) + ": ");
            }
            //print the node's entries
            System.out.print(this.printEntriesAsString(node) + "  ");
            //do same for next node
            BTNode<Key, Value> nextNode = this.getNext(this.root, this.height, node.entries[node.entryCount - 1].key, height);
            this.printTree(nextNode, height);
            //no children.
        }
        //internal node
        else if (height > 0) {
            //1. print height - only for the first node of each line (only when the node.entries[0] is the sentinel). sentinel is always first entry's key of root.
            if (node.entries[0].key.compareTo(this.sentinel) == 0) {
                System.out.println("height " + (height) + ": ");
            }
            //print the node's entries
            System.out.print(this.printEntriesAsString(node) + "  ");
            //get the next node and do the same for it
            BTNode<Key, Value> nextNode = this.getNext(this.root, this.height, node.entries[node.entryCount - 1].key, height);
            if (nextNode != null) {
                this.printTree(nextNode, height);
            }
            else {
                System.out.println("next node was null");
            }
            if (node.entries[0].key.compareTo(this.sentinel) == 0) {
                this.printTree(node.entries[0].child, height - 1);
            }
        }
    }

    protected void printTreeWorking() {
        //print the root
        System.out.println("height " + this.height + ": \n" + this.printEntriesAsString(this.root));
        //print the subtrees of root
        this.printTreeWorking(this.root, this.height);
    }
    private void printTreeWorking(BTNode<Key, Value> node, int height) {
        //external node
        if (height == 0) {
            System.out.print(this.printEntriesAsString(node) + " # ");
        }
        else //internal
        {
            for (int i = 0; i < node.entryCount; i++) {
                if (i == 0) {
                    System.out.println("height " + (height - 1) + ": ");
                }
                if (i + 1 == node.entryCount) {
                    System.out.print(this.printEntriesAsString(node.entries[i].child) + "\n");
                    break;
                }
                //print each node's child's entries with a # between
                System.out.print(this.printEntriesAsString(node.entries[i].child) + " # ");
            }
            for (int j = 0; j < node.entryCount; j++) {
                this.printTreeWorking(node.entries[j].child, height - 1);
            }
        }
    }

    private BTNode<Key, Value> getNext (BTNode < Key, Value > currentNode,int currentHeight, Key prevKey, int prevLevel){
        //if we can't go up anymore, or we have reached the top, then there is no "next node" - return null
        if (currentHeight == prevLevel) {
            System.out.println("test 0 - we have reached the top");

            if (prevKey.compareTo(currentNode.entries[currentNode.entryCount - 1].key) < 0) {

            }

            return null;
        }
        //to "go up" we must go down until @height is 1 greater than @prevLevel
        if (currentHeight > prevLevel + 1) {
            //get the index of the correct node to recurse into
            for (int i = 0; i < currentNode.entryCount; i++) {
                //if (we are at the last key in this node OR the key we
                //are looking for is less than the next key, i.e. the
                //desired key must be in the subtree below the current entry, a.k.a.
                //entries[i]), then recurse into the current entry’s child
                if (i + 1 == currentNode.entryCount || this.isLess(prevKey, currentNode.entries[i + 1].key)) {
                    //recurse into the current Entry's child until we are 1 level above it.
                    System.out.println("test 1 - skipping to the right level");

                    return this.getNext(currentNode.entries[i].child, currentHeight - 1, prevKey, prevLevel);
                }
            }
        }
        //we're at the right level, now we go over one
        if (currentHeight == prevLevel + 1) {
            //get the index of the correct node to recurse into
            for (int i = 0; i < currentNode.entryCount; i++) {
                //if i+1 == entryCount then the "next node" is not in the next entry spot in currentNode. It is one over in the level up above.
                if (i + 1 == currentNode.entryCount) {
                    //start a new search with different info (prevLevel + 1). This level is useless to us now, now we care about the one above.
                    System.out.println("test 2 - can't move over one. starting new search");
                    return this.getNext(this.root, this.height, prevKey, prevLevel +1);
                }
                //else, we still have room to move over one spot. the "next node" is the child of entries[i]
                else if (this.isLess(prevKey, currentNode.entries[i + 1].key)) {
                    System.out.println("test 3 - this is the next node");
                    return currentNode.entries[i+1].child;
                }
            }
        }
        //we go one level up and then over one.
        //if when going over one, the entries[over one] == entryCount, then we have to go up again
        //if when going up and over, we cant go up anymore, then return null (we're at the root)
        System.out.println("test 4");
        return null;
    }

    private BTNode<Key,Value> getNextDifSide(BTNode<Key,Value> currentNode, int currentHeight, Key prevKey, int prevLevel) {
        if (currentHeight > prevLevel) {
            //go down until we hit - on the other side
            //find the right spot where j would go in the root, but move one over

        }
        return null;
    }

    private String printEntriesAsString (BTNode < Key, Value > node){
        StringBuilder asString = new StringBuilder("[");
        for (int i = 0; i < node.entryCount; i++) {
            asString.append(node.entries[i].key.toString());
            if (i + 1 != node.entryCount) {
                asString.append(",");
            }
        }
        asString.append("]");
        return asString.toString();
    }
}
