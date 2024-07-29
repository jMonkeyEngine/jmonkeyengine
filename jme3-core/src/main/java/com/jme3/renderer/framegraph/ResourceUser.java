/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public interface ResourceUser {
    
    public LinkedList<ResourceTicket> getInputTickets();
    
    public LinkedList<ResourceTicket> getOutputTickets();
    
    public PassIndex getIndex();
    
    public void countReferences();
    
    public void dereference();
    
    public boolean isUsed();
    
}
