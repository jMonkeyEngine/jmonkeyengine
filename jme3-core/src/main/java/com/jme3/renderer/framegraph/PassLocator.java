/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.passes.RenderPass;

/**
 * Locates a pass.
 * 
 * @author codex
 * @param <T>
 */
public interface PassLocator <T extends RenderPass> {
    
    /**
     * Determines if the pass qualifies for this locator.
     * 
     * @param pass
     * @return pass, or null if not accepted
     */
    public T accept(RenderPass pass);
    
    /**
     * Locates a pass by its type.
     * 
     * @param <R>
     * @param type
     * @return 
     */
    public static <R extends RenderPass> PassLocator<R> by(Class<R> type) {
        return pass -> {
            if (type.isAssignableFrom(pass.getClass())) {
                return (R)pass;
            } else {
                return null;
            }
        };
    }
    
    /**
     * Locates a pass by its type and name.
     * 
     * @param <R>
     * @param type
     * @param name
     * @return 
     */
    public static <R extends RenderPass> PassLocator<R> by(Class<R> type, String name) {
        return pass -> {
            if (name.equals(pass.getName()) && type.isAssignableFrom(pass.getClass())) {
                return (R)pass;
            } else {
                return null;
            }
        };
    }
    
}
