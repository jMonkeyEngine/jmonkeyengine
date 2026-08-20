package com.jme3.vulkan.buffer;

import com.jme3.util.natives.Destructor;
import com.jme3.vulkan.alloc.RelativeBuffer;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public class DynamicBuffer <T extends RelativeBuffer> implements EngineBuffer {

    private final MemoryAllocator alloc;
    private T structure;
    private EngineBuffer buffer;
    private BufferType type;

    public DynamicBuffer(MemoryAllocator alloc, T structure, BufferType type, Flag<Role> roles) {
        this.alloc = alloc;
        this.structure = structure;
        this.buffer = alloc.createBuffer(type, pickNextSize(0, structure.capacity()), roles.add(Role.TransferSrc));
        this.structure.bind(buffer);
    }

    @Override
    public Destructor getDestructor() {
        return buffer.getDestructor();
    }

    @Override
    public void update(CommandBuffer cmd) {
        buffer.update(cmd);
    }

    @Override
    public DataBuffer cache() {
        return buffer.cache();
    }

    @Override
    public void invalidateCache() {
        buffer.invalidateCache();
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public int getBufferLocalOffset() {
        return buffer.getBufferLocalOffset();
    }

    @Override
    public long getHandle() {
        return buffer.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return buffer.getDeviceAddress();
    }

    @Override
    public Flag<Role> getRoles() {
        return buffer.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return buffer.getMemoryProperties();
    }

    @Override
    public boolean isDeviceAccessible() {
        return buffer.isDeviceAccessible();
    }

    public OpLocation update(CommandBuffer cmd, BufferType type, Flag<Role> roles, OpLocation copyLocation) {
        if (this.type != type || buffer.capacity() < structure.capacity() || !buffer.getRoles().contains(roles)) {
            EngineBuffer temp = alloc.createBuffer(type, pickNextSize(buffer.capacity(), structure.capacity()), roles.add(buffer.getRoles(), Role.TransferSrc, Role.TransferDst));
            copyLocation = copy(cmd, buffer, temp, copyLocation);
            buffer = temp;
        } else {
            copyLocation = OpLocation.DontCare;
        }
        this.structure.bind(buffer);
        this.type = type;
        return copyLocation;
    }

    public OpLocation update(CommandBuffer cmd, T structure, BufferType type, Flag<Role> roles, OpLocation copyLocation) {
        this.structure = structure;
        return update(cmd, type, roles, copyLocation);
    }

    public OpLocation update(CommandBuffer cmd, T structure, OpLocation copyLocation) {
        return update(cmd, structure, type, buffer.getRoles(), copyLocation);
    }

    protected OpLocation copy(CommandBuffer cmd, EngineBuffer src, EngineBuffer dst, OpLocation copyLocation) {
        return cmd.cmdCopy(src, dst, new BufferCopy().add(src, 0, dst, 0), copyLocation);
    }

    protected int pickNextSize(int currentSize, int requestedSize) {
        // next power of two at or above requestedSize
        return Math.max(Integer.highestOneBit(requestedSize - 1) << 1, Math.max(1, currentSize));
    }

    public T getStructure() {
        return structure;
    }

    public BufferType getType() {
        return type;
    }

}
