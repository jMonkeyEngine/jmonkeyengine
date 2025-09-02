package com.jme3.vulkan.data;

import com.jme3.vulkan.commands.CommandBuffer;

/**
 * Transforms incoming data into a useful format.
 */
public interface DataPipe<T> {

    T execute(CommandBuffer cmd);

}
