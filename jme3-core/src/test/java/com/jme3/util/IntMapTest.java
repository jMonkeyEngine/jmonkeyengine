/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.util;

import java.util.Iterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link IntMap} class works correctly.
 */
public class IntMapTest {

    @Test
    public void testDefaultConstructor() {
        IntMap<String> map = new IntMap<>();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testPutAndGet() {
        IntMap<String> map = new IntMap<>();
        map.put(1, "one");
        map.put(2, "two");
        Assert.assertEquals("one", map.get(1));
        Assert.assertEquals("two", map.get(2));
        Assert.assertEquals(2, map.size());
    }

    @Test
    public void testGetMissingKey() {
        IntMap<String> map = new IntMap<>();
        Assert.assertNull(map.get(999));
    }

    @Test
    public void testContainsKey() {
        IntMap<String> map = new IntMap<>();
        map.put(5, "five");
        Assert.assertTrue(map.containsKey(5));
        Assert.assertFalse(map.containsKey(99));
    }

    @Test
    public void testContainsValue() {
        IntMap<String> map = new IntMap<>();
        map.put(1, "hello");
        Assert.assertTrue(map.containsValue("hello"));
        Assert.assertFalse(map.containsValue("world"));
    }

    @Test
    public void testRemove() {
        IntMap<String> map = new IntMap<>();
        map.put(1, "one");
        map.put(2, "two");
        String removed = map.remove(1);
        Assert.assertEquals("one", removed);
        Assert.assertEquals(1, map.size());
        Assert.assertNull(map.get(1));
    }

    @Test
    public void testRemoveMissingKey() {
        IntMap<String> map = new IntMap<>();
        Assert.assertNull(map.remove(42));
    }

    @Test
    public void testClear() {
        IntMap<String> map = new IntMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.clear();
        Assert.assertEquals(0, map.size());
        Assert.assertNull(map.get(1));
    }

    @Test
    public void testOverwriteExistingKey() {
        IntMap<String> map = new IntMap<>();
        map.put(1, "first");
        String old = map.put(1, "second");
        Assert.assertEquals("first", old);
        Assert.assertEquals("second", map.get(1));
        Assert.assertEquals(1, map.size());
    }

    @Test
    public void testIterator() {
        IntMap<String> map = new IntMap<>();
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");
        int count = 0;
        for (IntMap.Entry<String> entry : map) {
            Assert.assertNotNull(entry.getValue());
            count++;
        }
        Assert.assertEquals(3, count);
    }

    @Test
    public void testClone() {
        IntMap<String> original = new IntMap<>();
        original.put(1, "one");
        original.put(2, "two");
        IntMap<String> cloned = original.clone();
        Assert.assertEquals(original.size(), cloned.size());
        Assert.assertEquals("one", cloned.get(1));
        // Verify independence
        cloned.put(3, "three");
        Assert.assertEquals(2, original.size());
    }

    @Test
    public void testLargeMap() {
        IntMap<Integer> map = new IntMap<>();
        for (int i = 0; i < 100; i++) {
            map.put(i, i * 10);
        }
        Assert.assertEquals(100, map.size());
        for (int i = 0; i < 100; i++) {
            Assert.assertEquals(Integer.valueOf(i * 10), map.get(i));
        }
    }

    @Test
    public void testNegativeKeys() {
        IntMap<String> map = new IntMap<>();
        map.put(-1, "neg1");
        map.put(-100, "neg100");
        Assert.assertEquals("neg1", map.get(-1));
        Assert.assertEquals("neg100", map.get(-100));
    }
}
