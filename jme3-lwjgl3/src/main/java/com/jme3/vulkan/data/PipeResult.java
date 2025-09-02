package com.jme3.vulkan.data;

import com.jme3.vulkan.commands.CommandBuffer;

public class PipeResult <T> implements ThroughputDataPipe<T, T> {

    private DataPipe<? extends T> input;
    private T result;

    @Override
    public void setInput(DataPipe<? extends T> input) {
        this.input = input;
    }

    @Override
    public DataPipe<? extends T> getInput() {
        return input;
    }

    @Override
    public T execute(CommandBuffer cmd) {
        return result = input.execute(cmd);
    }

    public T getResult() {
        return result;
    }

}
