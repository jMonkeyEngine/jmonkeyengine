package com.jme3.util.struct;

/**
 * Struct whose exact position and/or layout depends on an index.
 *
 * @param <T>
 */
public interface StructuredArray <T extends Struct> {

    T index(int i);

    int getIndex();

    int getLength();

}
