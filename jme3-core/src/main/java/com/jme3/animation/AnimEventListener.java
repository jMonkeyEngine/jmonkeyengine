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
package com.jme3.animation;

/**
 * <code>AnimEventListener</code> allows user code to receive various
 * events regarding an AnimControl. For example, when an animation cycle is done.
 * 
 * @author Kirill Vainer
 */
@Deprecated
public interface AnimEventListener {

    /**
     * Invoked when an animation "cycle" is done. For non-looping animations,
     * this event is invoked when the animation is finished playing. For
     * looping animations, this even is invoked each time the animation is restarted.
     *
     * @param control The control to which the listener is assigned.
     * @param channel The channel being altered
     * @param animName The new animation that is done.
     */
    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName);

    /**
     * Invoked when a animation is set to play by the user on the given channel.
     *
     * @param control The control to which the listener is assigned.
     * @param channel The channel being altered
     * @param animName The new animation name set.
     */
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName);

}
