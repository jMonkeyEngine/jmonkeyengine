/*
 * Copyright (c) 2023 jMonkeyEngine
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
package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

/**
 * Test suite for {@link SceneGraphIterator}.
 * <p>
 * The test succeeds if the rootNode and all its children,
 * except all spatials named "XXX", are printed on the console
 * with indents precisely indicating each spatial's distance
 * from the rootNode.
 * <p>
 * The test fails if
 * <ul>
 *   <li>Not all expected children are printed on the console.
 *   <li>An XXX is printed on the console (indicating faulty {@code ignoreChildren}).
 *   <li>Indents do not accurately indicate distance from the rootNode.
 * </ul>
 * 
 * @author codex
 */
public class TestSceneIteration extends SimpleApplication {
    
    /**
     * Launches the test application.
     * 
     * @param args no argument required
     */
    public static void main(String[] args) {
        new TestSceneIteration().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        // setup scene graph
        Node n1 = new Node("town");
        rootNode.attachChild(n1);
            n1.attachChild(new Node("car"));
            n1.attachChild(new Node("tree"));
            Node n2 = new Node("house");
            n1.attachChild(n2);
                n2.attachChild(new Node("chairs"));
                n2.attachChild(new Node("tables"));
                n2.attachChild(createGeometry("house-geometry"));
        Node n3 = new Node("sky");
        rootNode.attachChild(n3);
            n3.attachChild(new Node("airplane"));
            Node ignore = new Node("cloud");
            n3.attachChild(ignore);
                ignore.attachChild(new Node("XXX"));
                ignore.attachChild(new Node("XXX"));
                ignore.attachChild(new Node("XXX"));
            n3.attachChild(new Node("bird"));
        
        // iterate
        SceneGraphIterator iterator = new SceneGraphIterator(rootNode);
        for (Spatial spatial : iterator) {
            // create a hierarchy in the console
            System.out.println(constructTabs(iterator.getDepth()) + spatial.getName());
            // see if the children of this spatial should be ignored
            if (spatial == ignore) {
                // ignore all children of this spatial
                iterator.ignoreChildren();
            }
        }
        
        // exit the application
        stop();
        
    }
    
    private Geometry createGeometry(String name) {
        Geometry g = new Geometry(name, new Box(1, 1, 1));
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", ColorRGBA.Blue);
        g.setMaterial(m);
        return g;
    }
    
    private String constructTabs(int n) {
        StringBuilder render = new StringBuilder();
        for (; n > 0; n--) {
            render.append(" | ");
        }
        return render.toString();
    }
    
}
