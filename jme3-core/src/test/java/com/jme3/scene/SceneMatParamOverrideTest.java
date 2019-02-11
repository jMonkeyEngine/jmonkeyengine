/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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

import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.material.MatParamOverride;
import org.junit.Test;

import static com.jme3.scene.MPOTestUtils.*;
import static org.junit.Assert.*;

import com.jme3.system.TestUtil;
import java.util.List;

/**
 * Validates how {@link MatParamOverride MPOs} work on the scene level.
 *
 * @author Kirill Vainer
 */
public class SceneMatParamOverrideTest {

    private static Node createDummyScene() {
        Node scene = new Node("Scene Node");

        Node a = new Node("A");
        Node b = new Node("B");

        Node c = new Node("C");
        Node d = new Node("D");

        Node e = new Node("E");
        Node f = new Node("F");

        Node g = new Node("G");
        Node h = new Node("H");
        Node j = new Node("J");

        scene.attachChild(a);
        scene.attachChild(b);

        a.attachChild(c);
        a.attachChild(d);

        b.attachChild(e);
        b.attachChild(f);

        c.attachChild(g);
        c.attachChild(h);
        c.attachChild(j);

        return scene;
    }

    @Test
    public void testOverrides_Empty() {
        Node n = new Node("Node");
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());

        n.updateGeometricState();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());
    }

    @Test
    public void testOverrides_AddRemove() {
        MatParamOverride override = mpoBool("Test", true);
        Node n = new Node("Node");

        n.removeMatParamOverride(override);
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());

        n.addMatParamOverride(override);

        assertSame(n.getLocalMatParamOverrides().get(0), override);
        assertTrue(n.getWorldMatParamOverrides().isEmpty());
        n.updateGeometricState();

        assertSame(n.getLocalMatParamOverrides().get(0), override);
        assertSame(n.getWorldMatParamOverrides().get(0), override);

        n.removeMatParamOverride(override);
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertSame(n.getWorldMatParamOverrides().get(0), override);

        n.updateGeometricState();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());
    }

    @Test
    public void testOverrides_Clear() {
        MatParamOverride override = mpoBool("Test", true);
        Node n = new Node("Node");

        n.clearMatParamOverrides();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());

        n.addMatParamOverride(override);
        n.clearMatParamOverrides();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());

        n.addMatParamOverride(override);
        n.updateGeometricState();
        n.clearMatParamOverrides();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertSame(n.getWorldMatParamOverrides().get(0), override);

        n.updateGeometricState();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());

        n.addMatParamOverride(override);
        n.clearMatParamOverrides();
        n.updateGeometricState();
        assertTrue(n.getLocalMatParamOverrides().isEmpty());
        assertTrue(n.getWorldMatParamOverrides().isEmpty());
    }

    @Test
    public void testOverrides_AddAfterAttach() {
        Node scene = createDummyScene();
        scene.updateGeometricState();

        Node root = new Node("Root Node");
        root.updateGeometricState();

        root.attachChild(scene);
        scene.getChild("A").addMatParamOverride(mpoInt("val", 5));

        validateScene(root);
    }

    @Test
    public void testOverrides_AddBeforeAttach() {
        Node scene = createDummyScene();
        scene.getChild("A").addMatParamOverride(mpoInt("val", 5));
        scene.updateGeometricState();

        Node root = new Node("Root Node");
        root.updateGeometricState();

        root.attachChild(scene);

        validateScene(root);
    }

    @Test
    public void testOverrides_RemoveBeforeAttach() {
        Node scene = createDummyScene();
        scene.updateGeometricState();

        Node root = new Node("Root Node");
        root.updateGeometricState();

        scene.getChild("A").addMatParamOverride(mpoInt("val", 5));
        validateScene(scene);

        scene.getChild("A").clearMatParamOverrides();
        validateScene(scene);

        root.attachChild(scene);
        validateScene(root);
    }

    @Test
    public void testOverrides_RemoveAfterAttach() {
        Node scene = createDummyScene();
        scene.updateGeometricState();

        Node root = new Node("Root Node");
        root.updateGeometricState();

        scene.getChild("A").addMatParamOverride(mpoInt("val", 5));

        root.attachChild(scene);
        validateScene(root);

        scene.getChild("A").clearMatParamOverrides();
        validateScene(root);
    }

    @Test
    public void testOverrides_IdenticalNames() {
        Node scene = createDummyScene();

        scene.getChild("A").addMatParamOverride(mpoInt("val", 5));
        scene.getChild("C").addMatParamOverride(mpoInt("val", 7));

        validateScene(scene);
    }

    @Test
    public void testOverrides_CloningScene_DoesntCloneMPO() {
        Node originalScene = createDummyScene();

        originalScene.getChild("A").addMatParamOverride(mpoInt("int", 5));
        originalScene.getChild("A").addMatParamOverride(mpoBool("bool", true));
        originalScene.getChild("A").addMatParamOverride(mpoFloat("float", 3.12f));

        Node clonedScene = originalScene.clone(false);

        validateScene(clonedScene);
        validateScene(originalScene);

        List<MatParamOverride> clonedOverrides = clonedScene.getChild("A").getLocalMatParamOverrides();
        List<MatParamOverride> originalOverrides = originalScene.getChild("A").getLocalMatParamOverrides();

        assertNotSame(clonedOverrides, originalOverrides);
        assertEquals(clonedOverrides, originalOverrides);

        for (int i = 0; i < clonedOverrides.size(); i++) {
            assertNotSame(clonedOverrides.get(i), originalOverrides.get(i));
            assertEquals(clonedOverrides.get(i), originalOverrides.get(i));
        }
    }

    @Test
    public void testOverrides_SaveAndLoad_KeepsMPOs() {
        MatParamOverride override = mpoInt("val", 5);
        Node scene = createDummyScene();
        scene.getChild("A").addMatParamOverride(override);

        AssetManager assetManager = TestUtil.createAssetManager();
        Node loadedScene = BinaryExporter.saveAndLoad(assetManager, scene);

        Node root = new Node("Root Node");
        root.attachChild(loadedScene);
        validateScene(root);
        validateScene(scene);

        assertNotSame(override, loadedScene.getChild("A").getLocalMatParamOverrides().get(0));
        assertEquals(override, loadedScene.getChild("A").getLocalMatParamOverrides().get(0));
    }

    @Test
    public void testEquals() {
        assertEquals(mpoInt("val", 5), mpoInt("val", 5));
        assertEquals(mpoBool("val", true), mpoBool("val", true));
        assertNotEquals(mpoInt("val", 5), mpoInt("val", 6));
        assertNotEquals(mpoInt("val1", 5), mpoInt("val2", 5));
        assertNotEquals(mpoBool("val", true), mpoInt("val", 1));
    }
}
