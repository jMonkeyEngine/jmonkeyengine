/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.client;

import com.jme3.renderer.ViewPort;

/**
 *
 * @author codex
 */
public interface ViewPortFilter {
    
    public boolean confirm(ViewPort vp);
    
}
