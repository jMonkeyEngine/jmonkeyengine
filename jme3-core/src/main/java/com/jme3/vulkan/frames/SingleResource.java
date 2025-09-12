package com.jme3.vulkan.frames;

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

}
