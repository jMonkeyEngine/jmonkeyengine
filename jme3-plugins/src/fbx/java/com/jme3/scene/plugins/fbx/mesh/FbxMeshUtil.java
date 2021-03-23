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
package com.jme3.scene.plugins.fbx.mesh;

import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxMeshUtil {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private FbxMeshUtil() {
    }

    public static double[] getDoubleArray(FbxElement el) {
        if (el.propertiesTypes[0] == 'd') {
            // FBX 7.x
            return (double[]) el.properties.get(0);
        } else if (el.propertiesTypes[0] == 'D') {
            // FBX 6.x
            double[] doubles = new double[el.propertiesTypes.length];
            for (int i = 0; i < doubles.length; i++) {
                doubles[i] = (Double) el.properties.get(i);
            }
            return doubles;
        } else {
            return null;
        }
    }
    
    public static int[] getIntArray(FbxElement el) {
        if (el.propertiesTypes[0] == 'i') {
            // FBX 7.x
            return (int[]) el.properties.get(0);
        } else if (el.propertiesTypes[0] == 'I') {
            // FBX 6.x
            int[] ints = new int[el.propertiesTypes.length];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = (Integer) el.properties.get(i);
            }
            return ints;
        } else {
            return null;
        }
    }
}
