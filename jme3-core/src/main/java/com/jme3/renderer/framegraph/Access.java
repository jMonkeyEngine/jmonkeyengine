/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * Defines when a resource can be accessed for reading or writing.
 * 
 * @author codex
 */
public enum Access {
    
    /**
     * Indicates no concurrency.
     */
    NoConcurrency(false, false),
    
    /**
     * Indicates concurrency for reading only.
     */
    ConcurrentRead(true, false),
    
    /**
     * Indicates concurrency for writing only.
     */
    ConcurrentWrite(false, true),
    
    /**
     * Indicates concurrency for reading and writing.
     */
    Concurrent(true, true);
    
    private final boolean read, write;

    private Access(boolean read, boolean write) {
        this.read = read;
        this.write = write;
    }

    public boolean isReadConcurrent() {
        return read;
    }

    public boolean isWriteConcurrent() {
        return write;
    }
    
}
