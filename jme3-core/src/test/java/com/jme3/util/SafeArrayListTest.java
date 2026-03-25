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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link SafeArrayList} class works correctly.
 */
public class SafeArrayListTest {

    @Test
    public void testDefaultConstructor() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        Assert.assertTrue(list.isEmpty());
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void testAddAndGet() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        list.add("c");
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("a", list.get(0));
        Assert.assertEquals("b", list.get(1));
        Assert.assertEquals("c", list.get(2));
    }

    @Test
    public void testAddAtIndex() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("c");
        list.add(1, "b");
        Assert.assertEquals("b", list.get(1));
        Assert.assertEquals("c", list.get(2));
    }

    @Test
    public void testContains() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("hello");
        Assert.assertTrue(list.contains("hello"));
        Assert.assertFalse(list.contains("world"));
    }

    @Test
    public void testRemoveByObject() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        boolean removed = list.remove("a");
        Assert.assertTrue(removed);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("b", list.get(0));
    }

    @Test
    public void testRemoveByIndex() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        String removed = list.remove(0);
        Assert.assertEquals("a", removed);
        Assert.assertEquals(1, list.size());
    }

    @Test
    public void testSet() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        String old = list.set(0, "z");
        Assert.assertEquals("a", old);
        Assert.assertEquals("z", list.get(0));
    }

    @Test
    public void testClear() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        list.clear();
        Assert.assertTrue(list.isEmpty());
        Assert.assertEquals(0, list.size());
    }

    @Test
    public void testIndexOf() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        list.add("a");
        Assert.assertEquals(0, list.indexOf("a"));
        Assert.assertEquals(2, list.lastIndexOf("a"));
        Assert.assertEquals(-1, list.indexOf("z"));
    }

    @Test
    public void testAddAll() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("x", "y", "z"));
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("x", list.get(0));
    }

    @Test
    public void testAddAllAtIndex() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("d");
        list.addAll(1, Arrays.asList("b", "c"));
        Assert.assertEquals(4, list.size());
        Assert.assertEquals("b", list.get(1));
        Assert.assertEquals("c", list.get(2));
        Assert.assertEquals("d", list.get(3));
    }

    @Test
    public void testRemoveAll() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("a", "b", "c", "b"));
        list.removeAll(Arrays.asList("b"));
        Assert.assertEquals(2, list.size());
        Assert.assertFalse(list.contains("b"));
    }

    @Test
    public void testRetainAll() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("a", "b", "c"));
        list.retainAll(Arrays.asList("a", "c"));
        Assert.assertEquals(2, list.size());
        Assert.assertFalse(list.contains("b"));
    }

    @Test
    public void testContainsAll() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("a", "b", "c"));
        Assert.assertTrue(list.containsAll(Arrays.asList("a", "b")));
        Assert.assertFalse(list.containsAll(Arrays.asList("a", "z")));
    }

    @Test
    public void testGetArray() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        String[] arr = list.getArray();
        Assert.assertNotNull(arr);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals("a", arr[0]);
        Assert.assertEquals("b", arr[1]);
    }

    @Test
    public void testToArray() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        Object[] arr = list.toArray();
        Assert.assertEquals(2, arr.length);
    }

    @Test
    public void testToArrayTyped() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        String[] arr = list.toArray(new String[0]);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals("a", arr[0]);
    }

    @Test
    public void testIterator() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("x");
        list.add("y");
        Iterator<String> it = list.iterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals("x", it.next());
        Assert.assertEquals("y", it.next());
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void testSubList() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        List<String> sub = list.subList(1, 3);
        Assert.assertEquals(2, sub.size());
        Assert.assertEquals("b", sub.get(0));
        Assert.assertEquals("c", sub.get(1));
    }

    @Test
    public void testEqualsAndHashCode() {
        SafeArrayList<String> list1 = new SafeArrayList<>(String.class);
        SafeArrayList<String> list2 = new SafeArrayList<>(String.class);
        list1.addAll(Arrays.asList("a", "b", "c"));
        list2.addAll(Arrays.asList("a", "b", "c"));
        Assert.assertEquals(list1, list2);
        Assert.assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testClone() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.add("a");
        list.add("b");
        SafeArrayList<String> cloned = list.clone();
        Assert.assertNotSame(list, cloned);
        Assert.assertEquals(list, cloned);
        // Verify independence
        cloned.add("c");
        Assert.assertEquals(2, list.size());
    }

    @Test
    public void testListIterator() {
        SafeArrayList<String> list = new SafeArrayList<>(String.class);
        list.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = list.listIterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertEquals("a", it.next());
        Assert.assertTrue(it.hasPrevious());
        Assert.assertEquals("a", it.previous());
        Assert.assertEquals(0, it.nextIndex());
        Assert.assertEquals(-1, it.previousIndex());
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> source = Arrays.asList("x", "y", "z");
        SafeArrayList<String> list = new SafeArrayList<>(String.class, source);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals("x", list.get(0));
    }
}
