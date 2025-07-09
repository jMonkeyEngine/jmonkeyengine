/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.effect.shapes;

import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.JmeCloneable;

/**
 * This interface declares methods used by all shapes that represent particle emitters.
 * @author Kirill
 */
public interface EmitterShape extends Savable, JmeCloneable {

    /**
     * This method fills in the initial position of the particle.
     * @param store
     *        store variable for initial position
     */
    void getRandomPoint(Vector3f store);

    /**
     * This method fills in the initial position of the particle and its normal vector.
     * @param store
     *        store variable for initial position
     * @param normal
     *        store variable for initial normal
     */
    void getRandomPointAndNormal(Vector3f store, Vector3f normal);

    /**
     * This method creates a deep clone of the current instance of the emitter shape.
     * @return deep clone of the current instance of the emitter shape
     */
    EmitterShape deepClone();
}
