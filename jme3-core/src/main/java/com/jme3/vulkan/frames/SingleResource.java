package com.jme3.vulkan.frames;

import java.util.Iterator;

public class SingleResource <T> implements VersionedResource<T> {

    private T resource;

    public SingleResource() {}

    public SingleResource(T resource) {
        this.resource = resource;
    }

    @Override
    public void set(T resource) {
        this.resource = resource;
    }

    @Override
    public void set(int frame, T resource) {
        this.resource = resource;
    }

    @Override
    public T get() {
        return resource;
    }

    @Override
    public T get(int frame) {
        return resource;
    }

    @Override
    public int getNumResources() {
        return 1;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl<>(resource);
    }

    private static class IteratorImpl <T> implements Iterator<T> {

        private T value;

        public IteratorImpl(T value) {
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return value != null;
        }

        @Override
        public T next() {
            T v = value;
            value = null;
            return v;
        }

    }

}
