package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable> extends MinHeap<E> {
    //constructor required

    @SuppressWarnings({"unchecked"})
    public MinHeapImpl () {
        this.elements = (E[]) new Comparable[5];
        this.elementsToArrayIndex = new HashMap<>();
    }

    public void reHeapify(E element) {
        int index = this.getArrayIndex(element);
        this.downHeap(index);
        this.upHeap(index);
    }

    protected int getArrayIndex(E element) {
        int index = this.elementsToArrayIndex.getOrDefault(element, -1);
        if (index == -1) {
            throw new NoSuchElementException("Element not in MinHeap");
        }
        return index;
    }


    protected void doubleArraySize() {
        this.elements = Arrays.copyOf(this.elements, this.elements.length * 2);
    }

    //for testing purposes
    protected int getHeapCount() {
        return this.count;
    }
    protected int getArraySize() {
        return this.elements.length;
    }
    protected HashMap<E, Integer> getMap() {
        return (HashMap<E, Integer>) this.elementsToArrayIndex;
    }

    /**
     * swap the values stored at elements[i] and elements[j]
     *
     * @param i
     * @param j
     */
    @Override
    protected void swap(int i, int j) {
        super.swap(i, j);
        //update array indexes in map
        this.elementsToArrayIndex.put(this.elements[i], i);
        this.elementsToArrayIndex.put(this.elements[j], j);
    }

    @Override
    public void insert(E x) {
        super.insert(x);
        //add x to the map with index
        //it is very likely that @element x is the last in the trie, ie located at elements[count],
        //so before we get into a for loop that could be quite costly, lets check that first
        if (this.elements[this.count].equals(x)) {
            this.elementsToArrayIndex.put(this.elements[this.count], this.count);
            return;
        } //else
        for (int i = 1; i <= this.count; i++) {
            if (x.equals(this.elements[i])) {
                this.elementsToArrayIndex.put(this.elements[i], i);
            }
        }
    }

    @Override
    public E removeMin() {
        E min = super.removeMin();
        //update map
        this.elementsToArrayIndex.remove(min);
        return min;
    }
}
