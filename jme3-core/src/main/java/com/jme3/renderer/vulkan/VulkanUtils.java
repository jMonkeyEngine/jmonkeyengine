package com.jme3.renderer.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkInstance;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.*;
import java.util.stream.Stream;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanUtils {

    public static void check(int vulkanCode, String message) {
        if (vulkanCode != VK_SUCCESS) {
            throw new RuntimeException(message);
        }
    }

    public static <T> T enumerateBuffer(MemoryStack stack, IntFunction<T> factory, BiConsumer<IntBuffer, T> fetch) {
        IntBuffer count = stack.callocInt(1);
        fetch.accept(count, null);
        T buffer = factory.apply(count.get(0));
        fetch.accept(count, buffer);
        return buffer;
    }

    public static <T> PointerBuffer toPointers(MemoryStack stack, Stream<T> stream, int count, Function<T, ByteBuffer> toBytes) {
        PointerBuffer ptrs = stack.mallocPointer(count);
        stream.map(toBytes).forEach(ptrs::put);
        return ptrs.rewind();
    }

    public static long getPointer(MemoryStack stack, Consumer<PointerBuffer> action) {
        PointerBuffer ptr = stack.mallocPointer(1);
        action.accept(ptr);
        return ptr.get(0);
    }

    public static long getLong(MemoryStack stack, Consumer<LongBuffer> action) {
        LongBuffer l = stack.mallocLong(1);
        action.accept(l);
        return l.get(0);
    }

    public static PointerBuffer gatherPointers(MemoryStack stack, Collection<PointerBuffer> pointers) {
        int size = 0;
        for (PointerBuffer p : pointers) {
            size += p.limit();
        }
        PointerBuffer gather = stack.mallocPointer(size);
        for (PointerBuffer p : pointers) {
            gather.put(p);
        }
        return gather.rewind();
    }

    public static <T> void printListed(String label, Iterable<T> list, Function<T, String> toString) {
        System.out.println(label);
        for (T o : list) {
            System.out.println("  " + toString.apply(o));
        }
    }

    public static void verifyExtensionMethod(VkInstance instance, String name) {
        if (vkGetInstanceProcAddr(instance, name) == MemoryUtil.NULL) {
            throw new NullPointerException("Extension method " + name + " does not exist.");
        }
    }

    public static <T> NativeIterator<T> iteratePointers(PointerBuffer buffer, LongFunction<T> func) {
        return new NativeIterator<>(buffer, func);
    }

    public static boolean isBitSet(int n, int bit) {
        return (n & bit) > 0;
    }

    public static class NativeIterator <T> implements Iterable<T>, Iterator<T> {

        private final PointerBuffer pointers;
        private final LongFunction<T> func;
        private int index = 0;

        public NativeIterator(PointerBuffer pointers, LongFunction<T> func) {
            this.pointers = pointers;
            this.func = func;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return index < pointers.limit();
        }

        @Override
        public T next() {
            return func.apply(pointers.get(index++));
        }

    }

}
