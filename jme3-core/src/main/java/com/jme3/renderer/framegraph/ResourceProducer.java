/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.Collection;

/**
 *
 * @author codex
 */
public interface ResourceProducer {
    
    public int getIndex();
    
    public boolean dereference();
    public boolean isReferenced();
    
    public Collection<ResourceTicket> getInputTickets();
    public Collection<ResourceTicket> getOutputTickets();
    
}
