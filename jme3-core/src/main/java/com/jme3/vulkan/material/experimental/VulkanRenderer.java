package com.jme3.vulkan.material.experimental;

import com.jme3.renderer.ViewPort;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;

public class VulkanRenderer implements RenderGlobals {

    private final CommandBuffer cmd;
    private final Semaphore imageAcquired, renderComplete;
    private final Fence inFlight;

    public VulkanRenderer(CommandBuffer cmd) {
        this.cmd = cmd;
    }

    @Override
    public void update(ViewPort vp) {

    }

}
