package com.jme3.vulkan.sync;

public interface Semaphore {

    long getSemaphoreObject();

    long getTargetPayload();

}
