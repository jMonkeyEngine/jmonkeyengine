/*
 * Copyright (c) 2011, Novyon Events
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * @author Anthyon
 */
package com.jme3.terrain.noise;

import com.jme3.terrain.noise.basis.ImprovedNoise;
import com.jme3.terrain.noise.modulator.Modulator;
import java.nio.FloatBuffer;

/**
 * Interface for - basically 3D - noise generation algorithms, based on the
 * book: Texturing &amp; Modeling - A Procedural Approach
 * 
 * The main concept is to look at noise as a basis for generating fractals.
 * Basis can be anything, like a simple:
 * 
 * <code>
 * float value(float x, float y, float z) {
 *     return 0; // a flat noise with 0 value everywhere
 * }
 * </code>
 * 
 * or a more complex perlin noise ({@link ImprovedNoise}
 * 
 * Fractals use these functions to generate a more complex result based on some
 * frequency, roughness, etcetera values.
 * 
 * Fractals themselves are implementing the Basis interface as well, opening
 * an infinite range of results.
 * 
 * @author Anthyon
 * 
 * @since 2011
 * 
 */
public interface Basis {

    public void init();

    public Basis setScale(float scale);

    public float getScale();

    public Basis addModulator(Modulator modulator);

    public float value(float x, float y, float z);

    public FloatBuffer getBuffer(float sx, float sy, float base, int size);

}
