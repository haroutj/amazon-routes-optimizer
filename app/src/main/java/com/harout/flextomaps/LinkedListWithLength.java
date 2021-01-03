package com.harout.flextomaps;

import java.util.ArrayList;
import java.util.LinkedList;

public class LinkedListWithLength extends LinkedList<String> implements Comparable<LinkedListWithLength> {
    private double length;

    public LinkedListWithLength() {
        super();
        length = 0;
    }

    public LinkedListWithLength(LinkedListWithLength toClone) {
        super(toClone);
        length = toClone.getLength();
    }

    public LinkedListWithLength(ArrayList<String> toClone) {
        super(toClone);
        length = 0;
    }

    public double getLength() {
        return length;
    }

    public LinkedListWithLength addLength(double length) {
        this.length += length;

        return this;
    }

    public LinkedListWithLength setLength(double length) {
        this.length = length;

        return this;
    }

    @Override
    public int compareTo(LinkedListWithLength otherLinkedList) {
        return (this.getLength() > otherLinkedList.getLength()) ? 1 : (getLength() == otherLinkedList.getLength()) ? 0 : -1;
    }
}
