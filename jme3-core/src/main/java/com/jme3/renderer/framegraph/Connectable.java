/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * 
 * @author codex
 */
public interface Connectable {
    
    public ResourceTicket getInput(String name);
    
    public ResourceTicket getOutput(String name);
    
    public TicketGroup getGroup(String name);
    
    public ResourceTicket addListEntry(String groupName);
    
    public default ResourceTicket getInput(String name, boolean failOnMiss) {
        ResourceTicket t = getInput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Input ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    public default ResourceTicket getOutput(String name, boolean failOnMiss) {
        ResourceTicket t = getOutput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Output ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    public default TicketGroup getGroup(String name, boolean failOnMiss) {
        TicketGroup g = getGroup(name);
        if (g == null && failOnMiss) {
            throw new NullPointerException("Group \""+name+"\" does not exist.");
        }
        return g;
    }
    
    public default void makeInput(Connectable source, String sourceTicket, String targetTicket) {
        ResourceTicket out = source.getOutput(sourceTicket, true);
        if (TicketGroup.isListTicket(targetTicket)) {
            ResourceTicket t = addListEntry(TicketGroup.extractGroupName(targetTicket));
            t.setSource(out);
        } else {
            ResourceTicket target = getInput(targetTicket, true);
            target.setSource(out);
        }
    }
    
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup, int sourceStart, int targetStart, int length) {
        ResourceTicket[] sourceArray = source.getGroup(sourceGroup, true).getArray();
        ResourceTicket[] targetArray = getGroup(targetGroup, true).getArray();
        int n = Math.min(sourceStart+length, sourceArray.length);
        int m = Math.min(targetStart+length, targetArray.length);
        for (; sourceStart < n && targetStart < m; sourceStart++, targetStart++) {
            targetArray[targetStart].setSource(sourceArray[sourceStart]);
        }
    }
    
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup) {
        makeGroupInput(source, sourceGroup, targetGroup, 0, 0, Integer.MAX_VALUE);
    }
    
    public default void makeInputToList(Connectable source, String sourceTicket, String targetGroup) {
        ResourceTicket t = addListEntry(targetGroup);
        t.setSource(source.getOutput(sourceTicket));
    }
    
}
