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
import java.util.HashMap;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefineListTest {

    private static final List<String> DEFINE_NAMES = Arrays.asList("BOOL_VAR", "INT_VAR", "FLOAT_VAR");
    private static final List<VarType> DEFINE_TYPES = Arrays.asList(VarType.Boolean, VarType.Int, VarType.Float);
    private static final int NUM_DEFINES = DEFINE_NAMES.size();
    private static final int BOOL_VAR = 0;
    private static final int INT_VAR = 1;
    private static final int FLOAT_VAR = 2;
    private static final DefineList EMPTY = new DefineList(NUM_DEFINES);

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

    @Test
    public void testGetSet() {
        DefineList dl = new DefineList(NUM_DEFINES);

        assertFalse(dl.getBoolean(BOOL_VAR));
        assertEquals(dl.getInt(INT_VAR), 0);
        assertEquals(dl.getFloat(FLOAT_VAR), 0f, 0f);

        dl.set(BOOL_VAR, true);
        dl.set(INT_VAR, -1);
        dl.set(FLOAT_VAR, Float.NaN);

        assertTrue(dl.getBoolean(BOOL_VAR));
        assertEquals(dl.getInt(INT_VAR), -1);
        assertTrue(Float.isNaN(dl.getFloat(FLOAT_VAR)));
    }

    private String generateSource(DefineList dl) {
        StringBuilder sb = new StringBuilder();
        dl.generateSource(sb, DEFINE_NAMES, DEFINE_TYPES);
        return sb.toString();
    }

    @Test
    public void testSourceInitial() {
        DefineList dl = new DefineList(NUM_DEFINES);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
    }

    @Test
    public void testSourceBooleanDefine() {
        DefineList dl = new DefineList(NUM_DEFINES);

        dl.set(BOOL_VAR, true);
        assert dl.hashCode() == 1;
        assert generateSource(dl).equals("#define BOOL_VAR 1\n");

        dl.set(BOOL_VAR, false);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");

        dl.set(BOOL_VAR, true);
        assert dl.hashCode() == 1;
        assert generateSource(dl).equals("#define BOOL_VAR 1\n");

        dl.unset(BOOL_VAR);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
    }

    @Test
    public void testSourceIntDefine() {
        DefineList dl = new DefineList(NUM_DEFINES);

        int hashCodeWithInt = 1 << INT_VAR;

        dl.set(INT_VAR, 123);
        assert dl.hashCode() == hashCodeWithInt;
        assert generateSource(dl).equals("#define INT_VAR 123\n");

        dl.set(INT_VAR, 0);
        assert dl.hashCode() == hashCodeWithInt;
        assert generateSource(dl).equals("#define INT_VAR 0\n");

        dl.set(INT_VAR, -99);
        assert dl.hashCode() == hashCodeWithInt;
        assert generateSource(dl).equals("#define INT_VAR -99\n");

        dl.set(INT_VAR, Integer.MAX_VALUE);
        assert dl.hashCode() == hashCodeWithInt;
        assert generateSource(dl).equals("#define INT_VAR 2147483647\n");

        dl.unset(INT_VAR);
        assert dl.hashCode() == 0;
        assert generateSource(dl).equals("");
    }

    @Test
    public void testSourceFloatDefine() {
        DefineList dl = new DefineList(NUM_DEFINES);

        dl.set(FLOAT_VAR, 1f);
        assert dl.hashCode() == (1 << FLOAT_VAR);
        assert generateSource(dl).equals("#define FLOAT_VAR 1.0\n");

        dl.set(FLOAT_VAR, 0f);
        assert dl.hashCode() == (1 << FLOAT_VAR);
        assert generateSource(dl).equals("#define FLOAT_VAR 0.0\n");

        dl.set(FLOAT_VAR, -1f);
        assert generateSource(dl).equals("#define FLOAT_VAR -1.0\n");

        dl.set(FLOAT_VAR, FastMath.FLT_EPSILON);
        assert generateSource(dl).equals("#define FLOAT_VAR 1.1920929E-7\n");

        dl.set(FLOAT_VAR, FastMath.PI);
        assert generateSource(dl).equals("#define FLOAT_VAR 3.1415927\n");

        try {
            dl.set(FLOAT_VAR, Float.NaN);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) {
        }

        try {
            dl.set(FLOAT_VAR, Float.POSITIVE_INFINITY);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) {
        }

        try {
            dl.set(FLOAT_VAR, Float.NEGATIVE_INFINITY);
            generateSource(dl);
            assert false;
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        DefineList dl1 = new DefineList(NUM_DEFINES);
        DefineList dl2 = new DefineList(NUM_DEFINES);

        assertEquals(0, dl1.hashCode());
        assertEquals(0, dl2.hashCode());
        assertEquals(dl1, dl2);

        dl1.set(BOOL_VAR, true);

        assertEquals(1, dl1.hashCode());
        assertEquals(0, dl2.hashCode());
        assertNotEquals(dl1, dl2);

        dl2.set(BOOL_VAR, true);

        assertEquals(1, dl1.hashCode());
        assertEquals(1, dl2.hashCode());
        assertEquals(dl1, dl2);

        dl1.set(INT_VAR, 2);

        assertEquals(1 | 2, dl1.hashCode());
        assertEquals(1, dl2.hashCode());
        assertNotEquals(dl1, dl2);

        dl2.set(INT_VAR, 2);

        assertEquals(1 | 2, dl1.hashCode());
        assertEquals(1 | 2, dl2.hashCode());
        assertEquals(dl1, dl2);

        dl1.set(BOOL_VAR, false);

        assertEquals(2, dl1.hashCode());
        assertEquals(1 | 2, dl2.hashCode());
        assertNotEquals(dl1, dl2);

        dl2.unset(BOOL_VAR);

        assertEquals(2, dl1.hashCode());
        assertEquals(2, dl2.hashCode());
        assertEquals(dl1, dl2); // unset is the same as false

        dl1.unset(BOOL_VAR);
        assertEquals(2, dl1.hashCode());
        assertEquals(2, dl2.hashCode());
        assertEquals(dl1, dl2);
    }

    @Test
    public void testDeepClone() {
        DefineList dl1 = new DefineList(NUM_DEFINES);
        DefineList dl2 = dl1.deepClone();

        assertNotSame(dl1, dl2);
        assertEquals(dl1, dl2);
        assertEquals(dl1.hashCode(), dl2.hashCode());

        dl1.set(BOOL_VAR, true);
        dl2 = dl1.deepClone();

        assertEquals(dl1, dl2);
        assertEquals(dl1.hashCode(), dl2.hashCode());

        dl1.set(BOOL_VAR, false);
        dl2 = dl1.deepClone();

        assertEquals(dl1, dl2);
        assertEquals(dl1.hashCode(), dl2.hashCode());

        dl1.set(INT_VAR, 123);

        assertNotEquals(dl1, dl2);
        assertNotEquals(dl1.hashCode(), dl2.hashCode());

        dl2 = dl1.deepClone();

        assertEquals(dl1, dl2);
        assertEquals(dl1.hashCode(), dl2.hashCode());
    }

    @Test
    public void testGenerateSource() {
        DefineList dl = new DefineList(NUM_DEFINES);

        assertEquals("", generateSource(dl));

        dl.set(BOOL_VAR, true);

        assertEquals("#define BOOL_VAR 1\n", generateSource(dl));

        dl.set(INT_VAR, 123);

        assertEquals("#define BOOL_VAR 1\n"
                + "#define INT_VAR 123\n", generateSource(dl));

        dl.set(BOOL_VAR, false);

        assertEquals("#define INT_VAR 123\n", generateSource(dl));

        dl.set(BOOL_VAR, true);

        // should have predictable ordering based on defineId
        assertEquals("#define BOOL_VAR 1\n"
                + "#define INT_VAR 123\n", generateSource(dl));

        dl.unset(BOOL_VAR);
        assertEquals("#define INT_VAR 123\n", generateSource(dl));
    }

    private static String doLookup(HashMap<DefineList, String> map, Boolean boolVal, Integer intVal, Float floatVal) {
        DefineList dl = new DefineList(NUM_DEFINES);
        dl.set(BOOL_VAR, VarType.Boolean, boolVal);
        dl.set(INT_VAR, VarType.Int, intVal);
        dl.set(FLOAT_VAR, VarType.Float, floatVal);
        return map.get(dl);
    }

    @Test
    public void testHashLookup() {
        String STR_EMPTY = "This is an empty define list";
        String STR_INT = "This define list has an int value";
        String STR_BOOL = "This define list just has boolean value set";
        String STR_BOOL_INT = "This define list has both a boolean and int value";
        String STR_BOOL_INT_FLOAT = "This define list has a boolean, int, and float value";

        HashMap<DefineList, String> map = new HashMap<>();

        DefineList lookup = new DefineList(NUM_DEFINES);

        map.put(lookup.deepClone(), STR_EMPTY);

        lookup.set(BOOL_VAR, true);
        map.put(lookup.deepClone(), STR_BOOL);

        lookup.set(BOOL_VAR, false);
        lookup.set(INT_VAR, 123);
        map.put(lookup.deepClone(), STR_INT);

        lookup.set(BOOL_VAR, true);
        map.put(lookup.deepClone(), STR_BOOL_INT);

        lookup.set(FLOAT_VAR, FastMath.PI);
        map.put(lookup.deepClone(), STR_BOOL_INT_FLOAT);

        assertEquals(STR_EMPTY, doLookup(map, null, null, null));
        assertEquals(STR_INT, doLookup(map, false, 123, null));
        assertEquals(STR_BOOL, doLookup(map, true, null, null));
        assertEquals(STR_BOOL_INT, doLookup(map, true, 123, null));
        assertEquals(STR_BOOL_INT_FLOAT, doLookup(map, true, 123, FastMath.PI));
    }
}
