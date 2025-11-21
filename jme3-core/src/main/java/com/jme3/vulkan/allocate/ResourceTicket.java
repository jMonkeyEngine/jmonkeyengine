package com.jme3.vulkan.allocate;

public interface ResourceTicket <T> {

    Float selectResource(T resource);

    T createResource();

}
