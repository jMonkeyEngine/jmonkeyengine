package com.jme3.vulkan.buffer;

import com.jme3.vulkan.alloc.BufferDescription;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AutoBuffer <T extends BufferDescription> implements EngineBuffer {

    public interface ResizeFunction {
        int nextSize(int requested);
    }

    public static final ResizeFunction EXACT = r -> r;
    public static final ResizeFunction DOUBLE = r -> r << 1;
    public static final ResizeFunction POWER_OF_TWO = r -> Integer.highestOneBit(r - 1) << 1;

    private final MemoryAllocator alloc;
    private T structure;
    private EngineBuffer buffer;
    private BufferType currentType, targetType;
    private Flag<Role> roles;
    private ResizeFunction resize;

    public AutoBuffer(MemoryAllocator alloc, T structure, BufferType type, Flag<Role> roles) {
        this(alloc, structure, type, roles, DOUBLE);
    }

    public AutoBuffer(MemoryAllocator alloc, T structure, BufferType type, Flag<Role> roles, @NonNull ResizeFunction resize) {
        this.alloc = alloc;
        this.structure = structure;
        this.targetType = type;
        this.roles = roles;
        this.resize = resize;
        structure.bind(this, 0);
    }

    @Override
    public void flushCache() {
        if (buffer != null) {
            buffer.flushCache();
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
    public void invalidateCache() {
        if (buffer != null) {
            buffer.invalidateCache();
        }
    }

    @Override
    public int capacity() {
        return buffer != null ? buffer.capacity() : structure.size();
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

    private void initBuffer() {
        buffer = alloc.createBuffer(targetType, resize.nextSize(structure.size()), roles);
        currentType = targetType;
    }

    public OpLocation update(CommandBuffer cmd, OpLocation copyLocation) {
        if (buffer != null && (buffer.capacity() < structure.size() || currentType != targetType || !buffer.getRoles().contains(roles))) {
            EngineBuffer temp = alloc.createBuffer(targetType, Math.max(Math.max(resize.nextSize(structure.size()), buffer.capacity()), structure.size()), roles);
            copyLocation = cmd.cmdCopy(buffer, temp, new BufferCopy().add(0, 0, buffer.capacity()), copyLocation);
            buffer = temp;
        } else {
            copyLocation = OpLocation.DontCare;
        }
        currentType = targetType;
        return copyLocation;
    }

    public AutoBuffer<T> setStructure(@NonNull T structure) {
        this.structure.unbind();
        this.structure = structure;
        this.structure.bind(this, 0);
        return this;
    }

    public AutoBuffer<T> setType(BufferType type) {
        targetType = type;
        return this;
    }

    public AutoBuffer<T> addRoles(Flag<EngineBuffer.Role> roles) {
        this.roles = this.roles.add(roles);
        return this;
    }

    public AutoBuffer<T> setResizeFunction(@NonNull ResizeFunction resize) {
        this.resize = resize;
        return this;
    }

    public T getStructure() {
        return structure;
    }

    public BufferType getType() {
        return currentType;
    }

}
