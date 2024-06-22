/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.scene;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Iterates through the given spatials and their ancestors.
 * 
 * @author codex
 */
public class ParentIterator implements Iterable<Spatial>, Iterator<Spatial> {
    
    private static final String VISITED_TAG = ParentIterator.class.getName()+":visited";
    
    private final LinkedList<Spatial> spatials = new LinkedList<>();
    private final LinkedList<Spatial> current = new LinkedList<>();
    private final Iterator<Spatial> iterator;

    public ParentIterator(Iterable<? extends Spatial> startingPoints) {
        for (Spatial s : startingPoints) {
            if (!isVisited(s)) {
                visit(s);
            }
        }
        while (!current.isEmpty()) {
            Spatial parent = current.removeFirst().getParent();
            if (parent != null && !isVisited(parent)) {
                visit(parent);
            }
        }
        iterator = spatials.iterator();
    }
    
    private boolean isVisited(Spatial s) {
        return s.getUserData(VISITED_TAG) != null;
    }
    private void visit(Spatial s) {
        s.setUserData(VISITED_TAG, true);
        current.addFirst(s);
        spatials.add(s);
    }
    
    @Override
    public Iterator<Spatial> iterator() {
        return this;
    }
    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }
    @Override
    public Spatial next() {
        Spatial s = iterator.next();
        s.setUserData(VISITED_TAG, null);
        return s;
    }
    
}
