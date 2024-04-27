/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public enum ResourceAction {
    
    /**
     * The resource will only be read from.
     */
    Read(true, false),
    
    /**
     * The resource will only be written to.
     */
    Write(false, true),
    
    /**
     * The resource will be read from and written to.
     */
    ReadAndWrite(true, true);
    
    private final boolean read, write;

    private ResourceAction(boolean read, boolean write) {
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
