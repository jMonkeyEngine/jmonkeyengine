package com.jme3.vulkan.data;

public class Data<T> implements DataPipe<T> {

    private T data;

    public Data() {}

    public Data(T data) {
        this.data = data;
    }

    @Override
    public T execute() {
        return data;
    }

    public void set(T data) {
        this.data = data;
    }

    public T get() {
        return data;
    }

}
