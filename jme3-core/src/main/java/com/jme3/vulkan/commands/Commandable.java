package com.jme3.vulkan.commands;

public interface Commandable {

    void acquireControl();

    void releaseControl();

}
