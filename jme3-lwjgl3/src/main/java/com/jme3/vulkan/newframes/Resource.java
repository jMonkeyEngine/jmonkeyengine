package com.jme3.vulkan.newframes;

/**
 * Transforms incoming data into a useful format.
 */
public interface Resource <T> {

    T execute();

}
