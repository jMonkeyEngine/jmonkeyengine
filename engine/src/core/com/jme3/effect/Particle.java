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

package com.jme3.effect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/**
 * Represents a single particle in a {@link ParticleEmitter}.
 * 
 * @author Kirill Vainer
 */
public class Particle {
    
    /**
     * Particle velocity.
     */
    public final Vector3f velocity = new Vector3f();
    
    /**
     * Current particle position
     */
    public final Vector3f position = new Vector3f();
    
    /**
     * Particle color
     */
    public final ColorRGBA color = new ColorRGBA(0,0,0,0);
    
    /**
     * Particle size or radius.
     */
    public float size;
    
    /**
     * Particle remaining life, in seconds.
     */
    public float life;
    
    /**
     * The initial particle life
     */
    public float startlife;
    
    /**
     * Particle rotation angle (in radians).
     */
    public float angle;
    
    /**
     * Particle rotation angle speed (in radians).
     */
    public float rotateSpeed;
    
    /**
     * Particle image index. 
     */
    public int imageIndex = 0;
    
    /**
     * Distance to camera. Only used for sorted particles.
     */
    //public float distToCam;
}
