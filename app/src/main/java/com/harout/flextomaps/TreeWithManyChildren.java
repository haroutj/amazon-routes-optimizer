package com.harout.flextomaps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class TreeWithManyChildren {
    private HashSet<TreeWithManyChildren> child;
    private String address;

    public TreeWithManyChildren(String address) {
        child = new HashSet<TreeWithManyChildren>();
        this.address = address;
    }

    public TreeWithManyChildren(HashSet<TreeWithManyChildren> children, String address) {
        child = children;
        this.address = address;
    }

    public HashSet<TreeWithManyChildren> getChild() {
        return child;
    }

    public String getAddress() {
        return address;
    }

    public boolean isALeaf() {
        return child.size() == 0;
    }

    public void remove(String address) {
        child.remove(new TreeWithManyChildren(address));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TreeWithManyChildren && this.address.equals(((TreeWithManyChildren)o).getAddress());
    }
}
