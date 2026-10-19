package com.jme3.vulkan.concurrent;

import com.jme3.vulkan.buffer.DataBuffer;

import java.util.*;

public class JmeBuffer {

    private Access hostAccess, deviceAccess;

    /**
     * Thread dispatching command buffers that use this buffer. This thread
     * can read/write directly from pipes as it can't interfere with cache
     * invalidation or flushes. If null, any thread can make dispatches and
     * no threads can read/write directly from pipes.
     */
    private Thread dispatchThread;

    private final Queue<DataBuffer> tempHostMemory = new ArrayDeque<>();

    /**
     *
     *
     * @return
     */
    public DataBuffer acquireHost(int offset, int size) {
        if (hostAccess.isWrite()) {
            // acquire unused memory (can be pipe memory)
            if (hostAccess.isRead()) {
                // get current pipe
                // invalidate pipe cache
                // copy from pipe
            }
            // write data
            // if not pipe memory: stage pipe update from temp memory (occurs after all previous overlapping writes are complete)
        }
    }

}
