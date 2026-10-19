package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.checkerframework.checker.nullness.qual.NonNull;

@Deprecated
public class AutoBuffer implements StaticBuffer {

    public interface ResizeFunction {
        int nextSize(int requested);
    }

    public static final ResizeFunction EXACT = r -> r;
    public static final ResizeFunction DOUBLE = r -> r << 1;
    public static final ResizeFunction POWER_OF_TWO = r -> Integer.highestOneBit(r - 1) << 1;

    private final MemoryAllocator alloc;
    private int size;
    private GpuBuffer buffer;
    private BufferType currentType, targetType;
    private Flag<Role> roles;
    private ResizeFunction resize;

    public AutoBuffer(MemoryAllocator alloc, int size, BufferType type, Flag<Role> roles) {
        this(alloc, size, type, roles, DOUBLE);
    }

    public AutoBuffer(MemoryAllocator alloc, int size, BufferType type, Flag<Role> roles, @NonNull ResizeFunction resize) {
        this.alloc = alloc;
        this.size = size;
        this.targetType = type;
        this.roles = roles;
        this.resize = resize;
    }

    @Override
    public void update(CommandBuffer cmd) {
        if (size > buffer.capacity()) {
            GpuBuffer temp = alloc.createBuffer(resize.nextSize(size), roles);
            cmd.cmdCopy(buffer, temp, new BufferCopy().add(0, 0, buffer.capacity()), OpLocation.DontCare);
            buffer = temp;
        }
    }

    @Override
    public DataBuffer cache() {
        if (buffer == null) {
            initBuffer();
        }
        return buffer.cache();
    }

    @Override
    public void flushCache() {
        if (buffer != null) {
            buffer.flushCache();
        }
    }

    @Override
    public void invalidateCache() {
        if (buffer != null) {
            buffer.invalidateCache();
        }
    }

    @Override
    public int capacity() {
        return buffer != null ? buffer.capacity() : size;
    }

    @Override
    public long getHandle() {
        if (buffer == null) {
            initBuffer();
        }
        return buffer.getHandle();
    }

    @Override
    public Flag<Role> getRoles() {
        return buffer != null ? buffer.getRoles() : roles;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        if (buffer == null) {
            initBuffer();
        }
        return buffer.getMemoryProperties();
    }

    @Override
    public boolean isDeviceAccessible() {
        if (buffer == null) {
            initBuffer();
        }
        return buffer.isDeviceAccessible();
    }

    @Override
    public OpLocation resize(int capacity, Flag<Role> roles) {
        return null;
    }

    @Override
    public OpLocation resize(CommandBuffer cmd, int bytes, OpLocation location) {
        if (buffer != null && buffer.resize(cmd, bytes, location) == OpLocation.Failed) {
            return update(cmd, location);
        }
        return OpLocation.NoOp;
    }

    private void initBuffer() {
        buffer = alloc.createBuffer(resize.nextSize(size), roles);
        currentType = targetType;
    }

    public OpLocation update(CommandBuffer cmd, OpLocation copyLocation) {
        if (buffer != null && (buffer.capacity() < size || currentType != targetType || !buffer.getRoles().contains(roles))) {
            GpuBuffer temp = alloc.createBuffer(Math.max(Math.max(resize.nextSize(size), buffer.capacity()), size), roles);
            copyLocation = cmd.cmdCopy(buffer, temp, new BufferCopy().add(0, 0, buffer.capacity()), copyLocation);
            buffer = temp;
        } else {
            copyLocation = OpLocation.NoOp;
        }
        currentType = targetType;
        return copyLocation;
    }

    public AutoBuffer setSize(int size) {
        this.size = size;
        return this;
    }

    public AutoBuffer setType(BufferType type) {
        targetType = type;
        return this;
    }

    public AutoBuffer addRoles(Flag<GpuBuffer.Role> roles) {
        this.roles = this.roles.add(roles);
        return this;
    }

    public AutoBuffer setResizeFunction(@NonNull ResizeFunction resize) {
        this.resize = resize;
        return this;
    }

    public BufferType getType() {
        return currentType;
    }

}
