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

package com.jme3.input;

/**
 * Abstract interface for an input device.
 * 
 * @see MouseInput
 * @see KeyInput
 * @see JoyInput
 */
public interface Input {

    /**
     * Initializes the native side to listen into events from the device.
     */
    public void initialize();

    /**
     * Queries the device for input. All events should be sent to the
     * RawInputListener set with setInputListener.
     *
     * @see #setInputListener(com.jme3.input.RawInputListener)
     */
    public void update();

    /**
     * Ceases listening to events from the device.
     */
    public void destroy();

    /**
     * @return True if the device has been initialized and not destroyed.
     * @see #initialize()
     * @see #destroy() 
     */
    public boolean isInitialized();

    /**
     * Sets the input listener to receive events from this device. The
     * appropriate events should be dispatched through the callbacks
     * in RawInputListener.
     * @param listener
     */
    public void setInputListener(RawInputListener listener);

    /**
     * @return The current absolute time as nanoseconds. This time is expected
     * to be relative to the time given in InputEvents time property.
     */
    public long getInputTimeNanos();
}
