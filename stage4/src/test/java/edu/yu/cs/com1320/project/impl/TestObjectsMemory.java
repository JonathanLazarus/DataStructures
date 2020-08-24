package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.stage4.impl.DocumentImpl;

import java.net.URI;
import java.net.URISyntaxException;

public class TestObjectsMemory {

    public static void main (String[] args) throws URISyntaxException {
        DocumentImpl d = new DocumentImpl(new URI("https//:yu.cs.edu.com"), "text", "text".hashCode());
        DocumentImpl[] array = {d};
        StackImpl<DocumentImpl> stack = new StackImpl<>();
        stack.push(d);
        System.out.println(array[0].getLastUseTime());
        if (array[0].getLastUseTime() != stack.peek().getLastUseTime()) {
            throw new IllegalArgumentException("first test failed: last used time for both initially werent the same");
        }
        array[0].setLastUseTime(-1);
        if (stack.peek().getLastUseTime() != -1) {
            throw new IllegalArgumentException("time was not changed in stack also");
        }
        DocumentImpl dd = array[0];
        array[0] = null;
        dd.setLastUseTime(2908);
        if (stack.peek().getLastUseTime() != 2908) {
            throw new IllegalArgumentException("time was not changed in stack also after remove");
        }
        stack.pop().setLastUseTime(7012);
        System.out.println(dd.getLastUseTime());
        if (dd.getLastUseTime() != 7012) throw new IllegalArgumentException("test 3 failed");

    }
}
