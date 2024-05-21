/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * Represents a period of time starting at the start of the indexed pass, and
 * lasting for the duration of a number of following passes.
 * 
 * @author codex
 */
public class TimeFrame {
    
    private int index, length;
    
    /**
     * 
     */
    private TimeFrame() {}
    /**
     * 
     * @param passIndex
     * @param length 
     */
    public TimeFrame(int passIndex, int length) {
        this.index = passIndex;
        this.length = length;
        if (this.index < 0) {
            throw new IllegalArgumentException("Pass index cannot be negative.");
        }
        if (this.length < 0) {
            throw new IllegalArgumentException("Length cannot be negative.");
        }
    }
    
    /**
     * Extends, but does not retract, the length so that this time frame
     * includes the given index.
     * 
     * @param passIndex 
     */
    public void extendTo(int passIndex) {
        length = Math.max(length, passIndex-this.index);
    }
    /**
     * Copies this to the target time frame.
     * 
     * @param target
     * @return 
     */
    public TimeFrame copyTo(TimeFrame target) {
        if (target == null) {
            target = new TimeFrame();
        }
        target.index = index;
        target.length = length;
        return target;
    }
    
    /**
     * Gets index of the first pass this time frame includes.
     * 
     * @return 
     */
    public int getStartIndex() {
        return index;
    }
    /**
     * Gets the length.
     * 
     * @return 
     */
    public int getLength() {
        return length;
    }
    /**
     * Gets index of the last pass this time frame includes.
     * 
     * @return 
     */
    public int getEndIndex() {
        return index+length;
    }
    
    /**
     * Returns true if this time frame overlaps the given time frame.
     * 
     * @param time
     * @return 
     */
    public boolean overlaps(TimeFrame time) {
        return index <= time.index+time.length && index+length >= time.index;
    }
    /**
     * Returns true if this time frame includes the given index.
     * 
     * @param index
     * @return 
     */
    public boolean includes(int index) {
        return index <= index && index+length >= index;
    }
    
}
