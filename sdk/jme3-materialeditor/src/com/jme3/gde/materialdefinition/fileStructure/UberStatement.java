/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.fileStructure;

import com.jme3.util.blockparser.Statement;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class UberStatement extends Statement {

    private List listeners = Collections.synchronizedList(new LinkedList());

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        listeners.add(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        listeners.remove(pcl);
    }

    protected void fire(String propertyName, Object old, Object nue) {
        //Passing 0 below on purpose, so you only synchronize for one atomic call:
        PropertyChangeListener[] pcls = (PropertyChangeListener[]) listeners.toArray(new PropertyChangeListener[0]);
        for (int i = 0; i < pcls.length; i++) {
            pcls[i].propertyChange(new PropertyChangeEvent(this, propertyName, old, nue));
        }
    }

    public UberStatement(int lineNumber, String line) {
        super(lineNumber, line);
    }

    protected <T extends Statement> T getBlock(Class<T> blockType) {
        for (Statement statement : contents) {
            if (statement.getClass().isAssignableFrom(blockType)) {
                return (T) statement;
            }
        }
        return null;
    }

    protected <T extends Statement> List<T> getBlocks(Class<T> blockType) {
        List<T> blocks = new ArrayList<T>();
        for (Statement statement : contents) {
            if (statement.getClass().isAssignableFrom(blockType)) {
                blocks.add((T) statement);
            }
        }
        return blocks;
    }
}
