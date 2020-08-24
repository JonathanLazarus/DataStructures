package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.impl.StackImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MyStackImplTest {
    private StackImpl<Integer> stack;
    private Integer[] numbers = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    @Before
    public void setUp() throws Exception {
        this.stack = new StackImpl<>();
        for (Integer number : this.numbers) {
            this.stack.push(number);
        }
    }

    @Test
    public void push() {
        this.stack = new StackImpl<>();
        assertEquals(0, this.stack.size()); //check that stack is empty

        this.stack.push(14);
        assertEquals(1, this.stack.size()); //check that stack size is 1
        assertEquals(Integer.valueOf(14), Integer.valueOf(this.stack.peek()));
    }

    @Test
    public void pop() {
        assertEquals(10, this.stack.size()); //check that stack size is 10 (has 10 elements)
        assertEquals(Integer.valueOf(9), this.stack.pop());
        assertEquals(9, this.stack.size()); //check that stack size is now 9
        assertEquals(Integer.valueOf(8), Integer.valueOf(this.stack.peek()));

        //pop() should return null if there are no more elements.
        this.stack = new StackImpl<>(); //clear the stack
        assertNull(this.stack.pop()); //check that pop() returns null
    }

    @Test
    public void peek() {
        assertEquals(Integer.valueOf(9), Integer.valueOf(this.stack.peek())); //check that the top element is 9

        //peek should return null if there are no elements in the stack. So lets check that:
        this.stack = new StackImpl<>(); //clear the stack
        assertNull(this.stack.peek());
    }

    @Test
    public void size() {
        assertEquals(10, this.stack.size());
        this.stack = new StackImpl<>(); //clear the stack
        assertEquals(0, this.stack.size());
    }
}