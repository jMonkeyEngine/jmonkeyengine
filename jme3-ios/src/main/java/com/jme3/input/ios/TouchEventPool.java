/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.input.ios;

import com.jme3.input.event.TouchEvent;
import com.jme3.util.RingBuffer;
import java.util.logging.Logger;

/**
 * TouchEventPool provides a RingBuffer of jME TouchEvents to help with garbage
 * collection on iOS.  Each TouchEvent is stored in the RingBuffer and is 
 * reused if the TouchEvent has been consumed.
 * 
 * If a TouchEvent has not been consumed, it is placed back into the pool at the 
 * end for later use.  If a TouchEvent has been consumed, it is reused to avoid
 * creating lots of little objects.
 * 
 * If the pool is full of unconsumed events, then a new event is created and provided.
 * 
 * 
 * @author iwgeric
 */
public class TouchEventPool {
    private static final Logger logger = Logger.getLogger(TouchEventPool.class.getName());
    private final RingBuffer<TouchEvent> eventPool;
    private final int maxEvents;
    
    public TouchEventPool (int maxEvents) {
        eventPool = new RingBuffer<TouchEvent>(maxEvents);
        this.maxEvents = maxEvents;
    } 

    public void initialize() {
        TouchEvent newEvent;
        while (!eventPool.isEmpty()) {
            eventPool.pop();
        }
        for (int i = 0; i < maxEvents; i++) {
            newEvent = new TouchEvent();
            newEvent.setConsumed();
            eventPool.push(newEvent);
        }
    }
    
    public void destroy() {
        // Clean up queues
        while (!eventPool.isEmpty()) {
            eventPool.pop();
        }
    }

    /**
     * Fetches a touch event from the reuse pool
     *
     * @return a usable TouchEvent
     */
    public TouchEvent getNextFreeEvent() {
        TouchEvent evt = null;
        int curSize = eventPool.size();
        while (curSize > 0) {
            evt = eventPool.pop();
            if (evt.isConsumed()) {
                break;
            } else {
                eventPool.push(evt);
                evt = null;
            }
            curSize--;
        }

        if (evt == null) {
            logger.warning("eventPool full of unconsumed events");
            evt = new TouchEvent();
        }
        return evt;
    }
    
    /**
     * Stores the TouchEvent back in the pool for later reuse.  It is only reused
     * if the TouchEvent has been consumed.
     * 
     * @param event TouchEvent to store for later use if consumed.
     */
    public void storeEvent(TouchEvent event) {
        if (eventPool.size() < maxEvents) {
            eventPool.push(event);
        } else {
            logger.warning("eventPool full");
        }
    }    
    
}
