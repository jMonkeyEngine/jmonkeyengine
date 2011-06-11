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

import com.jme3.material.RenderState;
import com.jme3.math.Matrix3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;

/**
 * The <code>ParticleMesh</code> is the underlying visual implementation of a 
 * {@link ParticleEmitter particle emitter}.
 * 
 * @author Kirill Vainer
 */
public abstract class ParticleMesh extends Mesh {

    /**
     * Type of particle mesh
     */
    public enum Type {
        /**
         * The particle mesh is composed of points. Each particle is a point.
         * This can be used in conjuction with {@link RenderState#setPointSprite(boolean) point sprites}
         * to render particles the usual way.
         */
        Point,
        
        /**
         * The particle mesh is composed of triangles. Each particle is 
         * two triangles making a single quad.
         */
        Triangle;
    }

    /**
     * Initialize mesh data.
     * 
     * @param emitter The emitter which will use this <code>ParticleMesh</code>.
     * @param numParticles The maxmimum number of particles to simulate
     */
    public abstract void initParticleData(ParticleEmitter emitter, int numParticles);
    
    /**
     * Set the images on the X and Y coordinates
     * @param imagesX Images on the X coordinate
     * @param imagesY Images on the Y coordinate
     */
    public abstract void setImagesXY(int imagesX, int imagesY);
    
    /**
     * Update the particle visual data. Typically called every frame.
     */
    public abstract void updateParticleData(Particle[] particles, Camera cam, Matrix3f inverseRotation);

}
