package com.jme3.vulkan.buffer.bridge;

import com.jme3.vulkan.buffer.BufferStream;
import com.jme3.vulkan.buffer.BufferTracker;
import com.jme3.vulkan.commands.ImmediateCommandStream;

public class VulkanCopy implements DataBridge {

    private final ImmediateCommandStream cmd;
    private final BufferStream stream;

    public void copyFromHostToDeviceLocal(HostBuffer host, DeviceBuffer device, BufferTracker regions) {
        stream.stream(cmd.acquire(), host, device, regions);
    }

}
