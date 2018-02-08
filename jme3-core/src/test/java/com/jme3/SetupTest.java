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
package com.jme3;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.shader.BufferObject;
import com.jme3.shader.BufferObject.Layout;
import com.jme3.shader.BufferObjectField;
import com.jme3.shader.UniformBufferObject;
import com.jme3.shader.VarType;
import org.junit.Test;

/**
 *
 * @author davidB
 */
public class SetupTest {
    
   @Test(expected=AssertionError.class)
   public void testAssertionEnabled() {

       Material material;

       final UniformBufferObject ubo = new UniformBufferObject(3, Layout.std140,
           new BufferObjectField("light_1", VarType.Vector4),
           new BufferObjectField("light_2", VarType.Vector4),
           new BufferObjectField("array", VarType.FloatArray)
       );
       ubo.setValue("light_1", ColorRGBA.Black);
       ubo.setValue("light_2", ColorRGBA.Gray);
       ubo.setValue("array", new float[] {1F, 2F, 3F});

       material.setBufferObject("uboTest", ubo);

       assert false;
   }
}
