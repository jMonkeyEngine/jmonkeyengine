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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 * Tests for traversal order when using SceneGraphVisitor
 * with Spatial.depthFirstTraversal() and Spatial.breadthFirstTraversal().
 *
 * Related: Pull Request #1012 at GitHub
 *
 * @author 1000ml/FennelFetish (https://github.com/FennelFetish)
 */
public class SceneGraphTraversalTest {

    private static Node buildTestSceneGraph() {
        Node root       = new Node("Root");

        Node n1         = new Node("N1");
        Node n11        = new Node("N11");
        Node n111       = new Node("N111");
        Geometry g112   = new Geometry("G112");

        Node n2         = new Node("N2");
        Node n21        = new Node("N21");
        Node n211       = new Node("N211");
        Geometry g212   = new Geometry("G212");

        root.attachChild(n1);
        n1.attachChild(n11);
        n11.attachChild(n111);
        n11.attachChild(g112);

        root.attachChild(n2);
        n2.attachChild(n21);
        n21.attachChild(n211);
        n21.attachChild(g212);

        /* Graph:
                Root
                    N1
                        N11
                            N111
                            G112
                    N2
                        N21
                            N211
                            G212
        */

        return root;
    }


    private static final List<String> EXPECTED_ORDER_DFS_PRE;
    private static final List<String> EXPECTED_ORDER_DFS_POST;
    private static final List<String> EXPECTED_ORDER_BFS;

    static {
        List<String> expectedDfsPre = new ArrayList<>();
        expectedDfsPre.add("Root");
        expectedDfsPre.add("N1");
        expectedDfsPre.add("N11");
        expectedDfsPre.add("N111");
        expectedDfsPre.add("G112");
        expectedDfsPre.add("N2");
        expectedDfsPre.add("N21");
        expectedDfsPre.add("N211");
        expectedDfsPre.add("G212");
        EXPECTED_ORDER_DFS_PRE = Collections.unmodifiableList(expectedDfsPre);

        List<String> expectedDfsPost = new ArrayList<>();
        expectedDfsPost.add("N111");
        expectedDfsPost.add("G112");
        expectedDfsPost.add("N11");
        expectedDfsPost.add("N1");
        expectedDfsPost.add("N211");
        expectedDfsPost.add("G212");
        expectedDfsPost.add("N21");
        expectedDfsPost.add("N2");
        expectedDfsPost.add("Root");
        EXPECTED_ORDER_DFS_POST = Collections.unmodifiableList(expectedDfsPost);

        List<String> expectedBfs = new ArrayList<>();
        expectedBfs.add("Root");
        expectedBfs.add("N1");
        expectedBfs.add("N2");
        expectedBfs.add("N11");
        expectedBfs.add("N21");
        expectedBfs.add("N111");
        expectedBfs.add("G112");
        expectedBfs.add("N211");
        expectedBfs.add("G212");
        EXPECTED_ORDER_BFS = Collections.unmodifiableList(expectedBfs);
    }


    private static class NameListVisitor implements SceneGraphVisitor {
        private final List<String> namesInOrder = new ArrayList<>();

        @Override
        public void visit(Spatial spatial) {
            namesInOrder.add(spatial.getName());
        }
    }

    
    @Test
    public void testDFSPreOrder() {
        NameListVisitor visitor = new NameListVisitor();
        buildTestSceneGraph().depthFirstTraversal(visitor, Spatial.DFSMode.PRE_ORDER);

        assertThat(visitor.namesInOrder, is(EXPECTED_ORDER_DFS_PRE));
    }

    @Test
    public void testDFSPostOrder() {
        NameListVisitor visitor = new NameListVisitor();
        buildTestSceneGraph().depthFirstTraversal(visitor, Spatial.DFSMode.POST_ORDER);

        assertThat(visitor.namesInOrder, is(EXPECTED_ORDER_DFS_POST));
    }

    @Test
    public void testDFSDefaultOrder() {
        NameListVisitor visitor = new NameListVisitor();
        buildTestSceneGraph().depthFirstTraversal(visitor);

        assertThat(visitor.namesInOrder, is(EXPECTED_ORDER_DFS_POST));
    }

    @Test
    public void testBFS() {
        NameListVisitor visitor = new NameListVisitor();
        buildTestSceneGraph().breadthFirstTraversal(visitor);

        assertThat(visitor.namesInOrder, is(EXPECTED_ORDER_BFS));
    }
}
