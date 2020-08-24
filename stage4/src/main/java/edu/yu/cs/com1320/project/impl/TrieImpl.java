package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TrieImpl<Value> implements Trie<Value> {
    protected static int alphabetCharSize;
    private Node<Value> root;

    public TrieImpl() {
        //TODO uncomment after project submission
        //alphabetCharSize = 256; //ASCII extended character count
        alphabetCharSize = 36; //0-9A-Z
        this.root = new Node<>();
    }

    //constructor for trie implementation of only digits and uppercase letters.
    //0-9: 10 digits
    //A-Z: 26 letters
    //links[] needs 36 array slots
    //size should equal 36
    public TrieImpl(int size) {
        if (size == 36 || size == 10) {
            alphabetCharSize = size;
        } else {
            alphabetCharSize = 256;
        }
        this.root = new Node<>();
    }

    //maps char to smaller number for purposes of this project - also keeps works as regular trie (including lowercase tries)
    private int getCharValue(char c) {
        switch ((int) c) {
            //valueOf(0) == 48
            case 48:
                return 0;
            //valueOf(1) == 49
            case 49:
                return 1;
            //valueOf(2)
            case 50:
                return 2;
            //valueOf(3)
            case 51:
                return 3;
            //valueOf(4)
            case 52:
                return 4;
            //valueOf(5)
            case 53:
                return 5;
            //valueOf(6)
            case 54:
                return 6;
            //valueOf(7)
            case 55:
                return 7;
            //valueOf(8)
            case 56:
                return 8;
            //valueOf(9)
            case 57:
                return 9;
            //valueOf(A | a)
            case 65: case 97:
                return 10;
            //valueOf(B | b)
            case 66: case 98:
                return 11;
            //valueOf(C | c)
            case 67: case 99:
                return 12;
            //valueOf(D | d)
            case 68: case 100:
                return 13;
            //valueOf(E | e)
            case 69: case 101:
                return 14;
            //valueOf(F | f)
            case 70: case 102:
                return 15;
            //valueOf(G | g)
            case 71: case 103:
                return 16;
            //valueOf(H | h)
            case 72: case 104:
                return 17;
            //valueOf(I | i)
            case 73: case 105:
                return 18;
            //valueOf(J | j)
            case 74: case 106:
                return 19;
            //valueOf(K | k)
            case 75: case 107:
                return 20;
            //valueOf(L | l)
            case 76: case 108:
                return 21;
            //valueOf(M | m)
            case 77: case 109:
                return 22;
            //valueOf(N | n)
            case 78: case 110:
                return 23;
            //valueOf(O | o)
            case 79: case 111:
                return 24;
            //valueOf(P | p)
            case 80: case 112:
                return 25;
            //valueOf(Q | q)
            case 81: case 113:
                return 26;
            //valueOf(R | r)
            case 82: case 114:
                return 27;
            //valueOf(S | s)
            case 83: case 115:
                return 28;
            //valueOf(T | t)
            case 84: case 116:
                return 29;
            //valueOf(U | u)
            case 85: case 117:
                return 30;
            //valueOf(V | v)
            case 86: case 118:
                return 31;
            //valueOf(W | w)
            case 87: case 119:
                return 32;
            //valueOf(X | x)
            case 88: case 120:
                return 33;
            //valueOf(Y | y)
            case 89: case 121:
                return 34;
            //valueOf(Z | z)
            case 90: case 122:
                return 35;
            default:
                throw new ArrayIndexOutOfBoundsException((int) c);
        }
    }

    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if (key == null || key.isEmpty() || val == null) { //@201
            return;
        }
        this.root = this.put(this.root, key, val, 0);
    }

    private Node put(Node currentNode, String key, Value val, int index) {
        //~base case 1~: the character node we are currently at is not in the trie yet, so create a new node.
        if (currentNode == null) {
            currentNode = new Node();
        }
        //~base case 2~ currentNode is what we're looking for: set its value to val.
        if (key.length() == index) {
            currentNode.value.add(val);
            return currentNode;
        }
        char currentChar = key.charAt(index);
        int i;
        if (alphabetCharSize == 36) {
            i = getCharValue(currentChar);
        } else {
            i = currentChar;
        }
        currentNode.links[i] = this.put(currentNode.links[i], key, val, index + 1);
        return currentNode;
    }

    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     *
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */

    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        //cannot get null key
        if (key == null) {
            return new ArrayList<>(); //return empty list
        }
        Node x = this.get(this.root, key, 0);
        if (x == null) {
            return new ArrayList<>();
        }
        ArrayList<Value> sortedList = new ArrayList<>(x.value);
        sortedList.sort(comparator);
        return sortedList;
    }

    //MUST check that the returned node is not null when using this method!!
    private Node get(Node currentNode, String key, int index) {
        //~base case 1~ get miss: hit a null link before we finished iterating the key's characters
        if (currentNode == null) {
            return null; //return an empty set
        }
        //~base case 2~ get hit: we are at the correct node corresponding to the key, return it's value
        if (key.length() == index) {
            return currentNode;
        }
        //~recursive case~ we still haven't iterated over every char of the key yet
        char currentChar = key.charAt(index);
        int i;
        if (alphabetCharSize == 36) {
            i = this.getCharValue(currentChar);
        } else {
            i = currentChar;
        }
        return this.get(currentNode.links[i], key, index + 1);
    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     *
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        ArrayList<Value> sortedList = new ArrayList<>();
        //cannot get null key
        if (prefix == null) {
            return sortedList; //return empty list
        }
        //get the node which represents the prefix
        Node prefixNode = this.get(this.root, prefix, 0);
        if (prefixNode == null) {
            return sortedList;
        }
        //else, add all Values in prefix's subtree
        sortedList.addAll(this.getAll(prefixNode, new HashSet<>()));
        sortedList.sort(comparator);
        return sortedList;
    }

    private HashSet<Value> getAll(Node<Value> currentNode, HashSet<Value> completeSet) {
        //add all elements of currentNode's Value set starting with the prefix node.
        completeSet.addAll(currentNode.value);
        //visit each non-null child/link
        for (char c = 0; c < alphabetCharSize; c++) {
            //~base case 1~ get hit: we are at a node in the subtree of the prefix, recursively add its value
            if(currentNode.links[c] != null) {
                //~recursive case~ recursively add all node's children's value sets.
                this.getAll(currentNode.links[c], completeSet);
            }
        }
        return completeSet;
    }

    /**
     * Delete the subtrie rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        HashSet<Value>  deletedValues = new HashSet<>();
        if (prefix == null) {
            return deletedValues;
        }
        //get the prefix node so we can save all values in the subtrie.
        Node prefixNode = this.get(this.root, prefix, 0);
        if (prefixNode == null) {
            //prefixNode doesn't exist - return empty collection.
            return deletedValues;
        }
        //otherwise, save/add all Values in prefix's subtree
        deletedValues.addAll(this.getAll(prefixNode, new HashSet<>()));
        //then delete the subtrie at prefixNode.
        this.root = this.deleteAllWithPrefix(this.root, prefix, 0);
        return deletedValues;
    }

    private Node deleteAllWithPrefix(Node<Value> currentNode, String prefix, int index) {
        if (currentNode == null) {
            return null;
        }
        //we have reached the prefix node -  delete its subtrie.
        if (index == prefix.length()) {
            currentNode.value.clear();
            currentNode.setNewLinks();
        }
        //otherwise, continue down the trie to the prefix node
        else {
            char currentChar = prefix.charAt(index);
            int i;
            if (alphabetCharSize == 36) {
                i = this.getCharValue(currentChar);
            } else {
                i = currentChar;
            }
            currentNode.links[i] = this.deleteAllWithPrefix(currentNode.links[i], prefix, index + 1);
        }
        //this node has a value (in its set) – do nothing (ie: don't delete it), return the node
        if (!currentNode.value.isEmpty()) {
            return currentNode;
        }
        //remove subtrie rooted at currentNode IF it is completely empty
        for (int c = 0; c < alphabetCharSize; c++) {
            if (currentNode.links[c] != null) {
                return currentNode; //not empty
            }
        }
        //otherwise, empty (ie: it doesn't have a value or any children so it is of no use to us) - set this link to null in the parent
        return null;
    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAll(String key) {
        HashSet<Value> deletedSet = new HashSet<>();
        if (key == null) {
            return deletedSet;
        }
        //get the keyNode
        Node keyNode = this.get(this.root, key, 0);
        //if keyNode is null - return empty set.
        if (keyNode == null) {
            return deletedSet;
        }
        //add/save all elements of keyNode's Value set to the set to be returned.
        deletedSet.addAll(keyNode.value);
        //now go and erase all values from the keyNode's value set using recursive delete.
        this.root = this.delete(this.root, key, null, 0, true);
        return deletedSet;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if (key == null || val == null) { return null; }
        //get the keyNode
        Node keyNode = this.get(this.root, key, 0);
        //if keyNode is null or keyNode's set doesn't contain the val - val can't be deleted - return null.
        if (keyNode == null || !keyNode.value.contains(val)) {
            return null;
        }
        //keyNode contains val in value set: go and delete val from the set using recursive delete.
        this.root = this.delete(this.root, key, val, 0, false);
        //removed the value - return the value.
        return val;
    }

    //@param deleteAll - true if the method caller is deleteAll(); false if delete() calls it.
    private Node delete(Node currentNode, String key, Value val, int index, boolean deleteAll) {
        if (currentNode == null) {
            return null;
        }
        //we have reached the node to delete.
        if (index == key.length()) {
            //deleteAll() called this method: erase all elements in its value set
            if (deleteAll) {
                currentNode.value.clear();
            }
            //otherwise, delete() called this method - only wants us to delete a specific value, not all values.
            else {
                //remove val from the value set.
                currentNode.value.remove(val);
            }
        }
        //otherwise, continue down the trie to the target node
        else {
            char currentChar = key.charAt(index);
            int i;
            if (alphabetCharSize == 36) {
                i = this.getCharValue(currentChar);
            } else {
                i = currentChar;
            }
            currentNode.links[i] = this.delete(currentNode.links[i], key, val, index + 1, deleteAll);
        }
        //this node has a value (in its set) – do nothing (ie: don't delete it), return the node
        if (!currentNode.value.isEmpty()) {
            return currentNode;
        }
        //remove subtrie rooted at currentNode IF it is completely empty
        for (int c = 0; c < alphabetCharSize; c++) {
            if (currentNode.links[c] != null) {
                return currentNode; //not empty
            }
        }
        //otherwise, empty (ie: it doesn't have a value or any children so it is of no use to us) - set this link to null in the parent
        return null;
    }
}
