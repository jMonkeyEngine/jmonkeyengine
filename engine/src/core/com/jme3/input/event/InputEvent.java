/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.input.event;

import com.jme3.input.Input;

/**
 * An abstract input event.
 */
public abstract class InputEvent {

    protected long time;

    
    protected boolean consumed = false;

    /**
     * The time when the event occurred. This is relative to
     * {@link Input#getInputTimeNanos() }.
     * 
     * @return time when the event occured
     */
    public long getTime(){
        return time;
    }

    /**
     * Set the time when the event occurred.
     * 
     * @param time time when the event occurred.
     */
    public void setTime(long time){
        this.time = time;
    }

    /**
     * Returns true if the input event has been consumed, meaning it is no longer valid
     * and should not be forwarded to input listeners.
     * 
     * @return true if the input event has been consumed
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Call to mark this input event as consumed, meaning it is no longer valid
     * and should not be forwarded to input listeners.
     */
    public void setConsumed() {
        this.consumed = true;
    }
    
}
