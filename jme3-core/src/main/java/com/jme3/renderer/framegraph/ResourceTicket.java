/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph;

import java.util.Collection;
import java.util.LinkedList;

/**
 * References a {@link RenderResource} by index.
 * <p>
 * Can reference another ticket as a source, which makes this point to the same
 * resource as the source ticket. This mechanism allows RenderPasses to share
 * resources. Also vaguely tracks the last seen render object, which is used to
 * prioritize that render object, especially for reservations.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> {
    
    public static final String RESERVED = "#";
    
    private String name;
    private int localIndex;
    private long objectId = -1;
    private ResourceTicket<T> source;
    private final LinkedList<ResourceTicket<T>> targets = new LinkedList<>();
    private int exportGroupId = -1;
    
    /**
     * 
     */
    public ResourceTicket() {
        this(null, -1);
    }
    /**
     * Creates a ticket with the name and a negative local index.
     * 
     * @param name 
     */
    public ResourceTicket(String name) {
        this(name, -1);
    }
    /**
     * Creates a ticket with the name and local index.
     * 
     * @param name
     * @param index 
     */
    public ResourceTicket(String name, int index) {
        this.name = name;
        this.localIndex = index;
    }
    
    /**
     * Clears all target tickets.
     */
    public void clearAllTargets() {
        for (ResourceTicket<T> t : targets) {
            t.source = null;
        }
        targets.clear();
    }
    
    /**
     * Copies this ticket's resource index to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyIndexTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        return target.setLocalIndex(localIndex);
    }
    /**
     * Copies this ticket's object ID to the target ticket.
     * 
     * @param target
     * @return 
     */
    public ResourceTicket<T> copyObjectTo(ResourceTicket<T> target) {
        if (target == null) {
            target = new ResourceTicket();
        }
        target.setObjectId(objectId);
        return target;
    }
    
    /**
     * Sets the source ticket.
     * 
     * @param source 
     */
    public void setSource(ResourceTicket<T> source) {
        if (this.source != null) {
            this.source.targets.remove(this);
        }
        this.source = source;
        if (this.source != null) {
            this.source.targets.add(this);
        }
    }
    /**
     * Sets the name of this ticket.
     * 
     * @param name
     * @return 
     */
    public ResourceTicket<T> setName(String name) {
        this.name = name;
        return this;
    }
    /**
     * Sets the local index.
     * <p>
     * The local index is overriden if the source ticket is not null and
     * the source's world index is not negative.
     * 
     * @param index
     * @return 
     */
    protected ResourceTicket<T> setLocalIndex(int index) {
        this.localIndex = index;
        return this;
    }
    /**
     * Sets the object ID.
     * 
     * @param objectId 
     */
    public void setObjectId(long objectId) {
        this.objectId = objectId;
    }
    /**
     * Sets the id of the group this ticket is exported with.
     * <p>
     * Called internally. Do not use.
     * 
     * @param exportGroupId 
     */
    public void setExportGroupId(int exportGroupId) {
        this.exportGroupId = exportGroupId;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the world index.
     * <p>
     * If the source ticket is null or its world index is negative, this ticket's
     * local index will be returned.
     * 
     * @return 
     */
    public int getWorldIndex() {
        if (source != null) {
            int i = source.getWorldIndex();
            if (i >= 0) return i;
        }
        return localIndex;
    }
    /**
     * 
     * @return 
     */
    public int getLocalIndex() {
        return localIndex;
    }
    /**
     * 
     * @return 
     */
    public long getObjectId() {
        return objectId;
    }
    /**
     * 
     * @return 
     */
    public ResourceTicket<T> getSource() {
        return source;
    }
    /**
     * Returns true if this source ticket is not null.
     * 
     * @return 
     */
    public boolean hasSource() {
        return source != null;
    }
    /**
     * Gets all tickets depending on this ticket.
     * 
     * @return 
     */
    public Collection<ResourceTicket<T>> getTargets() {
        return targets;
    }
    public int getExportGroupId() {
        return exportGroupId;
    }
    
    @Override
    public String toString() {
        return "Ticket[name="+name+", worldIndex="+getWorldIndex()+"]";
    }
    
    
    /**
     * Returns true if the ticket is valid for locating a resource.
     * <p>
     * A ticket is only valid if it is not null and its world index
     * is greater than or equal to zero.
     * 
     * @param ticket
     * @return true if ticket is valid
     */
    public static boolean validate(ResourceTicket ticket) {
        return ticket != null && ticket.getWorldIndex() >= 0;
    }
    
    public static void validateUserTicketName(String name) {
        if (name.startsWith(RESERVED)) {
            throw new IllegalArgumentException("Cannot start ticket name with reserved \""+RESERVED+"\".");
        }
    }
    
}
