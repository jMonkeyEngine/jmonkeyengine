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
package com.jme3.shadow;

/**
 * <code>ShadowEdgeFiltering</code> specifies how shadows are filtered
 */
public enum EdgeFilteringMode {

    /**
     * Shadows are not filtered. Nearest sample is used, causing in blocky
     * shadows.
     */
    Nearest(10),
    /**
     * Bilinear filtering is used. Has the potential of being hardware
     * accelerated on some GPUs
     */
    Bilinear(1),
    /**
     * Dither-based sampling is used, very cheap but can look bad at low
     * resolutions.
     */
    Dither(2),
    /**
     * 4x4 percentage-closer filtering is used. Shadows will be smoother at the
     * cost of performance
     */
    PCF4(3),
    /**
     * 12 samples percentage-closer filtering with a POISON disc distribution 
     * is used. 
     * http://devmag.org.za/2009/05/03/poisson-disk-sampling/
     * The principle is to eliminate the regular blurring pattern that can be 
     * seen with pcf4x4 by randomizing the samble position with a poisson disc.
     * Shadows will look smoother than 4x4 PCF but with slightly better or 
     * similar performance.
     */
    PCFPOISSON(4),
    /**
     * 8x8 percentage-closer filtering is used. Shadows will be smoother at the
     * cost of performance
     */
    PCF8(5);
    
    int materialParamValue;

    private EdgeFilteringMode(int val) {
        materialParamValue = val;
    }

    public int getMaterialParamValue() {
        return materialParamValue;
    }
    
}
