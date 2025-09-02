package com.jme3.vulkan.data;

public interface TerminalDataPipe <In, Out> extends DataPipe<Out> {

    void setInput(In input);

    In getInput();

}
