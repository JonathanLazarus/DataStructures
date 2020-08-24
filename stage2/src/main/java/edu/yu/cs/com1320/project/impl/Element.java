package edu.yu.cs.com1320.project.impl;

class Element<T> {
    Element<T> next;
    final T value;

    Element(T value) {
        this.value = value;
        this.next = null;
    }
}
