/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public class TimeFrame {
    
    private int index, length;
    
    private TimeFrame() {}
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
    
    public void extendTo(int passIndex) {
        length = Math.max(length, passIndex-this.index);
    }
    public TimeFrame copyTo(TimeFrame target) {
        if (target == null) {
            target = new TimeFrame();
        }
        target.index = index;
        target.length = length;
        return target;
    }
    
    public int getStartIndex() {
        return index;
    }
    public int getLength() {
        return length;
    }
    public int getEndIndex() {
        return index+length;
    }
    
    public boolean overlaps(TimeFrame time) {
        return index <= time.index+time.length && index+length >= time.index;
    }
    public boolean contains(int index) {
        return index <= index && index+length >= index;
    }
    
}
