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
package com.jme3.scene.plugins.triangulator;

import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.IrPolygon;
import com.jme3.scene.plugins.IrVertex;
import junit.framework.TestCase;

public class TriangulatorTest extends TestCase {
    
    public void testTriangulator() {
        Vector3f[] dataSet = new Vector3f[]{
            new Vector3f(0.75f, 0.3f, 1.2f),
            new Vector3f(0.75f, 0.3f, 0.0f),
            new Vector3f(0.75f, 0.17f, 0.0f),
            new Vector3f(0.75000095f, 0.17f, 1.02f),
            new Vector3f(0.75f, -0.17f, 1.02f),
            new Vector3f(0.75f, -0.17f, 0.0f),
            new Vector3f(0.75f, -0.3f, 0.0f),
            new Vector3f(0.75f, -0.3f, 1.2f)
        };
        
        IrPolygon poly = new IrPolygon();
        poly.vertices = new IrVertex[dataSet.length];
        for (int i = 0; i < dataSet.length; i++) {
            poly.vertices[i] = new IrVertex();
            poly.vertices[i].pos = dataSet[i];
        }
        
        EarClippingTriangulator triangulator = new EarClippingTriangulator();
        triangulator.triangulate(poly);
    }
    
}
