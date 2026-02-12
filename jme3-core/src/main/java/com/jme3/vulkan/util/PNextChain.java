package com.jme3.vulkan.util;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class PNextChain {

    private final Queue<Function<Long, ? extends Struct>> contructors = new LinkedList<>();
    private final Map<Class, Struct> chain = new HashMap<>();
    private Struct head;

    public PNextChain copyStructure() {
        PNextChain copy = new PNextChain();
        for (Function<Long, ? extends Struct> c : contructors) {
            copy.add(c);
        }
        return copy;
    }

    public <T extends Struct> T add(Function<Long, T> constructor) {
        contructors.add(constructor);
        T s;
        head = s = constructor.apply(head != null ? head.address() : MemoryUtil.NULL);
        if (chain.put(head.getClass(), head) != null) {
            throw new IllegalArgumentException("Duplicate struct type in pnext chain.");
        }
        return s;
    }

    public <T extends Struct> T get(Class<T> type) {
        return (T)chain.get(type);
    }

    public <T, S extends Struct> T get(Class<S> type, T def, Function<S, T> onExists) {
        Struct s = chain.get(type);
        return s != null ? onExists.apply((S)s) : def;
    }

    public <T extends Struct> void get(Class<T> type, Consumer<T> onExists) {
        Struct s = chain.get(type);
        if (s != null) onExists.accept((T)s);
    }

    public Struct getHead() {
        return head;
    }

    public int size() {
        return chain.size();
    }

    public boolean isEmpty() {
        return chain.isEmpty();
    }

    public void free() {
        for (Struct s : chain.values()) {
            s.free();
        }
        clear();
    }

    public void clear() {
        chain.clear();
        contructors.clear();
        head = null;
    }

    public PNextChain getReadOnly() {
        return new ReadOnlyChain(this);
    }

    private static class ReadOnlyChain extends PNextChain {

        private final PNextChain delegate;

        private ReadOnlyChain(PNextChain delegate) {
            this.delegate = delegate;
        }

        @Override
        public PNextChain copyStructure() {
            return delegate.copyStructure();
        }

        @Override
        public <T extends Struct> T add(Function<Long, T> constructor) {
            throw new UnsupportedOperationException("Cannot add to read-only pnext chain.");
        }

        @Override
        public <T extends Struct> T get(Class<T> type) {
            return delegate.get(type);
        }

        @Override
        public Struct getHead() {
            return delegate.getHead();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public void free() {
            throw new UnsupportedOperationException("Cannot free read-only pnext chain.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Cannot clear read-only pnext chain.");
        }

    }

}
