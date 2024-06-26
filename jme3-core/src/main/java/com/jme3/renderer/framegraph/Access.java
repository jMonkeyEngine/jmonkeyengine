/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public enum Access {
    
    /**
     * Indicates that the resource is accessed for reading only.
     */
    Read(true, false),
    
    /**
     * Indicates that the resource is accessed for writing only.
     */
    Write(false, true),
    
    /**
     * Indicates that the resource is accessed for both reading and writing.
     */
    ReadAndWrite(true, true);
    
    private final boolean read, write;
    
    private Access(boolean read, boolean write) {
        this.read = read;
        this.write = write;
    }

    /**
     * Returns true if the access is for reading.
     * 
     * @return 
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Returns true if the access is for writing.
     * 
     * @return 
     */
    public boolean isWrite() {
        return write;
    }
    
}
