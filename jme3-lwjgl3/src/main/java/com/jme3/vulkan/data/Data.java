package com.jme3.vulkan.data;

import com.jme3.vulkan.commands.CommandBuffer;

public class Data<T> implements TerminalDataPipe<T, T> {

    private T input;

    public Data() {}

    public Data(T input) {
        this.input = input;
    }

    @Override
    public T execute(CommandBuffer cmd) {
        return input;
    }

    @Override
    public void setInput(T input) {
        this.input = input;
    }

    @Override
    public T getInput() {
        return input;
    }

}
