/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.bullet.collision;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author normenhansen
 */
public class PhysicsCollisionEventFactory {

    private ConcurrentLinkedQueue<PhysicsCollisionEvent> eventBuffer = new ConcurrentLinkedQueue<PhysicsCollisionEvent>();

    /**
     * Obtain an unused event.
     * 
     * @return an event (not null)
     */
    public PhysicsCollisionEvent getEvent(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        PhysicsCollisionEvent event = eventBuffer.poll();
        if (event == null) {
            event = new PhysicsCollisionEvent(type, source, nodeB, manifoldPointObjectId);
        }else{
            event.refactor(type, source, nodeB, manifoldPointObjectId);
        }
        return event;
    }

    /**
     * Recycle the specified event.
     * 
     * @param event 
     */
    public void recycle(PhysicsCollisionEvent event) {
        event.clean();
        eventBuffer.add(event);
    }
}
