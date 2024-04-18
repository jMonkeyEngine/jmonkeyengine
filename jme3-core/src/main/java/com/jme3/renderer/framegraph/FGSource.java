/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public interface FGSource {
    
    public String getName();
    
    public abstract void postLinkValidate();
    
    public abstract FGBindable yieldBindable();
    
}
