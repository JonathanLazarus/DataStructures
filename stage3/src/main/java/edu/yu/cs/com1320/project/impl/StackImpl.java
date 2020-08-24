package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {
    private Element<T> head;
    private int count;

    public StackImpl() {
        this.head = null;
        this.count = 0;
    }

    /**
     * @param element object to add to the Stack
     */
    public void push(T element) {
        Element<T> add = new Element<>(element);
        if (this.count > 0) {
            add.next = this.head;
        }
        this.head = add;
        this.count++;
    }

    /**
     * removes and returns element at the top of the stack
     *
     * @return element at the top of the stack, null if the stack is empty
     */
    public T pop() {
        if (this.count == 0) {
            return null;
        }
        Element<T> temp = this.head;
        this.head = head.next;
        this.count--;
        return temp.value;
    }

    /**
     * @return the element at the top of the stack without removing it, null if stack is empty
     */
    public T peek() {
        if (this.count == 0) {
            return null;
        }
        return this.head.value;
    }

    /**
     * @return how many elements are currently in the stack
     */
    public int size() {
        return this.count;
    }
}
