/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.cinematic;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.cinematic.events.AnimationEvent;
import com.jme3.scene.Node;
import org.junit.Test;

/**
 *
 * @author davidB
 */
public class CinematicTest {
    
    /**
     * No NPE or any exception when clear() a new Cinematic
     */
    @Test
    public void clearEmpty() {
        Cinematic sut = new Cinematic();
        sut.clear();
    }
    
    /**
     * No ClassCastException when clear() a Cinematic with AnimationEvent
     */
    @Test
    public void clearAnimationEvent() {
        Cinematic sut = new Cinematic();
        Node model = new Node("model");
        AnimControl ac = new AnimControl();
        ac.addAnim(new Animation("animName", 1.0f));
        model.addControl(ac);
        sut.enqueueCinematicEvent(new AnimationEvent(model, "animName"));
        sut.initialize(null, null);
        sut.clear();
    }
}
