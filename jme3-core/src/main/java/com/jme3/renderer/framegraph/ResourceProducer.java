/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.Collection;

/**
 * Interface for objects that use and produce resources.
 * 
 * @author codex
 */
public interface ResourceProducer {
    
    /**
     * Gets the index of execution.
     * 
     * @return 
     */
    public int getExecutionIndex();
    
    /**
     * Dereferences this producer.
     * 
     * @return 
     */
    public boolean dereference();
    /**
     * Returns true if this producer is used.
     * 
     * @return 
     */
    public boolean isUsed();
    
    /**
     * Gets a collection of all input tickets.
     * 
     * @return 
     */
    public Collection<ResourceTicket> getInputTickets();
    /**
     * Gets a collection of all output tickets.
     * 
     * @return 
     */
    public Collection<ResourceTicket> getOutputTickets();
    
}
