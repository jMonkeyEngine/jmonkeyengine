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
package com.jme3.shader;

import com.jme3.math.FastMath;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DefineListTest {
    
    private List<String> defineNames;
    private List<VarType> defineTypes;
    
    @Test
    public void testHashCollision() {
        DefineList dl1 = new DefineList(64);
        DefineList dl2 = new DefineList(64);
        
        // Try to cause a hash collision
        // (since bit #32 is aliased to bit #1 in 32-bit ints)
        dl1.set(0, 123);
        dl1.set(32, 0);
        
        dl2.set(32, 0);
        dl2.set(0, 123);
        
        assert dl1.hashCode() == dl2.hashCode();
        assert dl1.equals(dl2);
    }
    
    private String generateSource(DefineList dl) {
        StringBuilder sb = new StringBuilder();
        dl.generateSource(sb, defineNames, defineTypes);
        return sb.toString();
    }
    
    @Test
    public void testInitial() {
        DefineList dl = new DefineList(3);
        defineNames  = Arrays.asList("A", "B", "C");
        defineTypes = Arrays.asList(VarType.Boolean, VarType.Int, VarType.Float);
        
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
    }
    
    @Test
    public void testBooleanDefine() {
        DefineList dl = new DefineList(1);
        defineNames  = Arrays.asList("BOOL_VAR");
        defineTypes = Arrays.asList(VarType.Boolean);
        
        dl.set(0, true);
        assert dl.hashCode() == 1;
        assert generateSource(dl).equals("#define BOOL_VAR 1\n");
        
        dl.set(0, false);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
    }
    
    @Test
    public void testFloatDefine() {
        DefineList dl = new DefineList(1);
        defineNames  = Arrays.asList("FLOAT_VAR");
        defineTypes = Arrays.asList(VarType.Float);
        
        dl.set(0, 1f);
        assert dl.hashCode() == 1;
        assert generateSource(dl).equals("#define FLOAT_VAR 1.0\n");
        
        dl.set(0, 0f);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
        
        dl.set(0, -1f);
        assert generateSource(dl).equals("#define FLOAT_VAR -1.0\n");
        
        dl.set(0, FastMath.FLT_EPSILON);
        assert generateSource(dl).equals("#define FLOAT_VAR 1.1920929E-7\n");
        
        dl.set(0, FastMath.PI);
        assert generateSource(dl).equals("#define FLOAT_VAR 3.1415927\n");
        
        try {
            dl.set(0, Float.NaN);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) { }
        
        try {
            dl.set(0, Float.POSITIVE_INFINITY);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) { }
        
        try {
            dl.set(0, Float.NEGATIVE_INFINITY);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) { }
    }
    
    @Test
    public void testSourceGeneration() {
        DefineList dl = new DefineList(64);
        defineNames  = Arrays.asList("BOOL_VAR",      "INT_VAR",   "FLOAT_VAR");
        defineTypes = Arrays.asList(VarType.Boolean, VarType.Int, VarType.Float);
        dl.set(0, true);
        dl.set(1, -1);
        dl.set(2, Float.NaN);
    }
}
