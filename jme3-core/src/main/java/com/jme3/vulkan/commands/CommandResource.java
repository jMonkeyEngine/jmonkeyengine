package com.jme3.vulkan.commands;

public interface CommandResource {

    void consume();

    void release();

}
