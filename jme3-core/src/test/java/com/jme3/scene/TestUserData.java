/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.scene;

import junit.framework.TestCase;
import org.junit.Test;

public class TestUserData extends TestCase {

    public static void userDataTest(Spatial sp, Object v) {
        sp.setUserData("test", v);
        assertTrue("UserData is null", sp.getUserData("test") != null);
        assertEquals("UserData value is different than input value", sp.getUserData("test"), v);
        sp.setUserData("test", null);
    }

    @Test
    public void testLong() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Long) (long) (Math.random() * Long.MAX_VALUE));
    }

    @Test
    public void testInt() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Integer) (int) (Math.random() * Integer.MAX_VALUE));
    }

    @Test
    public void testShort() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Short) (short) (Math.random() * Short.MAX_VALUE));
    }

    @Test
    public void testByte() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Byte) (byte) (Math.random() * Byte.MAX_VALUE));
    }

    @Test
    public void testDouble() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Double) (double) (Math.random() * Double.MAX_VALUE));
    }

    @Test
    public void testFloat() throws Exception {
        Spatial sp = new Node("TestSpatial");
        userDataTest(sp, (Float) (float) (Math.random() * Float.MAX_VALUE));
    }
}
