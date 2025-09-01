package com.jme3.vulkan.data;

/**
 * Transforms incoming data into a useful format.
 */
public interface DataPipe<T> {

    T execute();

}
