/*
 * Copyright (c) 2026 jMonkeyEngine
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 * Complex behavioral tests for {@link SceneGraphIterator}.
 */
public class SceneGraphIteratorTest {

    private static Node buildTree() {
        Node root = new Node("root");
        Node a = new Node("a");
        Node a1 = new Node("a1");
        Geometry a1g = new Geometry("a1g", new Mesh());
        Node b = new Node("b");
        Geometry bg = new Geometry("bg", new Mesh());

        root.attachChild(a);
        root.attachChild(b);
        a.attachChild(a1);
        a1.attachChild(a1g);
        b.attachChild(bg);

        return root;
    }

    @Test
    public void testDepthFirstIterationOrder() {
        Node root = buildTree();
        SceneGraphIterator it = new SceneGraphIterator(root);

        List<String> names = new ArrayList<>();
        while (it.hasNext()) {
            names.add(it.next().getName());
        }

        Assert.assertEquals(
                Arrays.asList("root", "a", "a1", "a1g", "b", "bg"),
                names
        );
    }

    @Test
    public void testDepthValuesDuringTraversal() {
        Node root = buildTree();
        SceneGraphIterator it = new SceneGraphIterator(root);

        List<String> names = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        while (it.hasNext()) {
            Spatial s = it.next();
            names.add(s.getName());
            depths.add(it.getDepth());
        }

        Assert.assertEquals(Arrays.asList("root", "a", "a1", "a1g", "b", "bg"), names);
        Assert.assertEquals(Arrays.asList(0, 1, 2, 3, 1, 2), depths);
    }

    @Test
    public void testIgnoreChildrenSkipsSubtree() {
        Node root = buildTree();
        SceneGraphIterator it = new SceneGraphIterator(root);

        List<String> names = new ArrayList<>();
        while (it.hasNext()) {
            Spatial s = it.next();
            names.add(s.getName());
            if ("a".equals(s.getName())) {
                it.ignoreChildren(); // skip a1 and a1g
            }
        }

        Assert.assertEquals(Arrays.asList("root", "a", "b", "bg"), names);
    }

    @Test
    public void testCurrentTracksLastReturnedSpatial() {
        Node root = buildTree();
        SceneGraphIterator it = new SceneGraphIterator(root);

        Assert.assertNull(it.current());
        Spatial first = it.next();
        Assert.assertSame(first, it.current());

        Spatial second = it.next();
        Assert.assertSame(second, it.current());
    }

    @Test
    public void testLeafMainSpatialIteratesExactlyOnce() {
        Geometry leaf = new Geometry("leaf", new Mesh());
        SceneGraphIterator it = new SceneGraphIterator(leaf);

        Assert.assertTrue(it.hasNext());
        Assert.assertEquals("leaf", it.next().getName());
        Assert.assertFalse(it.hasNext());
    }
}
