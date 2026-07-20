package com.jme3.vulkan.commands;

public interface CommandCycleListener {

    void onCmdSubmit();

    void onCmdComplete();

}
