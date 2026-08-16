package com.jme3.vulkan.mesh;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.StructArray;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.DynamicBuffer;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.util.Flag;

import java.util.Iterator;

public class CongregateVertexBuffer <T extends Struct<VertexAttr>> {

    private final DynamicBuffer<StructArray<T>> array;
    private final BufferTracker usedVertices = new ExactBufferTracker();

    public CongregateVertexBuffer(MemoryAllocator alloc, int vertexCapacity, T struct, BufferType type, Flag<EngineBuffer.Role> roles) {
        array = new DynamicBuffer<>(alloc, new StructArray<>(vertexCapacity, struct), type, roles);
    }

    public Hook allocate(CommandBuffer cmd, int vertices) {
        StructArray<T> arr = array.getStructure();
        for (BufferTracker.Island i : usedVertices) {
            if (i.getAvailableAfter(arr.getLength()) >= vertices) {
                usedVertices.add(i.getEnd(), vertices);
                return new Hook(i.getEnd(), vertices);
            }
        }
        int pos = arr.getLength();
        array.update(cmd, new StructArray<>(arr.getLength() + vertices, arr.getStruct()), array.getType(), array.getRoles(), OpLocation.PreferHost);
        usedVertices.add(pos, vertices);
        return new Hook(pos, vertices);
    }

    public class Hook implements Iterable<T> {

        private final int firstVertex, length;

        private Hook(int firstVertex, int length) {
            this.firstVertex = firstVertex;
            this.length = length;
        }

        @Override
        public Iterator<T> iterator() {
            return new VertexIterator<>(this);
        }

        public T getVertex(int index) {
            return array.getStructure().index(firstVertex + index);
        }

        public int getFirstVertex() {
            return firstVertex;
        }

        public int getLength() {
            return length;
        }

    }

    private static class VertexIterator <T extends Struct<VertexAttr>> implements Iterator<T> {

        private final CongregateVertexBuffer<T>.Hook hook;
        private int index = 0;

        private VertexIterator(CongregateVertexBuffer<T>.Hook hook) {
            this.hook = hook;
        }

        @Override
        public boolean hasNext() {
            return index < hook.getLength();
        }

        @Override
        public T next() {
            return hook.getVertex(index++);
        }

    }

}
