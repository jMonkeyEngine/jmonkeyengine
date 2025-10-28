package com.jme3.util;

public interface Versionable {

    /**
     * Returns the version number reflecting the current state
     * of this object. If the state of this object is changed,
     * the version number must be incremented by at least one.
     *
     * @return version number for the current state
     */
    long getVersionNumber();

}
