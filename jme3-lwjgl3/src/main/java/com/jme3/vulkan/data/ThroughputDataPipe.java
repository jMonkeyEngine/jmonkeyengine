package com.jme3.vulkan.data;

public interface ThroughputDataPipe <In, Out> extends DataPipe<Out> {

    void setInput(DataPipe<? extends In> input);

    DataPipe<? extends In> getInput();

}
