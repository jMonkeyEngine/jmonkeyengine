package com.jme3.vulkan.concurrent;

public enum Access {

    Read(true, false), Write(false, true), ReadWrite(true, true);

    private final boolean read, write;

    Access(boolean read, boolean write) {
        this.read = read;
        this.write = write;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isWrite() {
        return write;
    }

}
