/*
 * Copyright (c) 2021 jMonkeyEngine
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
package com.jme3.scene.debug;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cloning/saving/loading debug meshes of various types.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestCloneMesh {
    // *************************************************************************
    // new methods exposed

    /**
     * Test cloning/saving/loading an Arrow.
     */
    @Test
    public void testCloneArrow() {
        Arrow arrow = new Arrow(new Vector3f(1f, 1f, 1f));

        Arrow deepClone = Cloner.deepClone(arrow);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, arrow);

        AssetManager assetManager = new DesktopAssetManager();
        Arrow saveAndLoad = BinaryExporter.saveAndLoad(assetManager, arrow);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a Grid.
     */
    @Test
    public void testCloneGrid() {
        Grid grid = new Grid(5, 5, 1f);

        Grid deepClone = Cloner.deepClone(grid);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, grid);

        AssetManager assetManager = new DesktopAssetManager();
        Grid saveAndLoad = BinaryExporter.saveAndLoad(assetManager, grid);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a SkeletonDebugger.
     */
    @Test
    public void testCloneSkeletonDebugger() {
        Bone[] boneArray = new Bone[2];
        boneArray[0] = new Bone("rootBone");
        boneArray[1] = new Bone("leafBone");
        boneArray[0].addChild(boneArray[1]);
        Skeleton skeleton = new Skeleton(boneArray);
        skeleton.setBindingPose();
        SkeletonDebugger skeletonDebugger
                = new SkeletonDebugger("sd", skeleton);

        SkeletonDebugger deepClone = Cloner.deepClone(skeletonDebugger);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, skeletonDebugger);

        AssetManager assetManager = new DesktopAssetManager();
        SkeletonDebugger saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, skeletonDebugger);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a SkeletonInterBoneWire.  See JME issue #1705.
     */
    @Test
    public void testCloneSkeletonInterBoneWire() {
        Bone[] boneArray = new Bone[2];
        boneArray[0] = new Bone("rootBone");
        boneArray[1] = new Bone("leafBone");
        boneArray[0].addChild(boneArray[1]);
        Skeleton skeleton = new Skeleton(boneArray);
        skeleton.setBindingPose();
        Map<Integer, Float> boneLengths = new HashMap<>();
        boneLengths.put(0, 2f);
        boneLengths.put(1, 1f);
        SkeletonInterBoneWire sibw
                = new SkeletonInterBoneWire(skeleton, boneLengths);

        SkeletonInterBoneWire deepClone = Cloner.deepClone(sibw);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, sibw);

        AssetManager assetManager = new DesktopAssetManager();
        SkeletonInterBoneWire saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, sibw);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a SkeletonPoints.  See JME issue #1705.
     */
    @Test
    public void testCloneSkeletonPoints() {
        Bone[] boneArray = new Bone[2];
        boneArray[0] = new Bone("rootBone");
        boneArray[1] = new Bone("leafBone");
        boneArray[0].addChild(boneArray[1]);
        Skeleton skeleton = new Skeleton(boneArray);
        skeleton.setBindingPose();
        SkeletonPoints skeletonPoints = new SkeletonPoints(skeleton);

        SkeletonPoints deepClone = Cloner.deepClone(skeletonPoints);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, skeletonPoints);

        AssetManager assetManager = new DesktopAssetManager();
        SkeletonPoints saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, skeletonPoints);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a SkeletonWire.  See JME issue #1705.
     */
    @Test
    public void testCloneSkeletonWire() {
        Bone[] boneArray = new Bone[2];
        boneArray[0] = new Bone("rootBone");
        boneArray[1] = new Bone("leafBone");
        boneArray[0].addChild(boneArray[1]);
        Skeleton skeleton = new Skeleton(boneArray);
        SkeletonWire skeletonWire = new SkeletonWire(skeleton);

        SkeletonWire deepClone = Cloner.deepClone(skeletonWire);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, skeletonWire);

        AssetManager assetManager = new DesktopAssetManager();
        SkeletonWire saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, skeletonWire);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a WireBox.
     */
    @Test
    public void testCloneWireBox() {
        WireBox box = new WireBox(0.5f, 0.5f, 0.5f);

        WireBox deepClone = Cloner.deepClone(box);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, box);

        AssetManager assetManager = new DesktopAssetManager();
        WireBox saveAndLoad = BinaryExporter.saveAndLoad(assetManager, box);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a WireSphere.
     */
    @Test
    public void testCloneWireSphere() {
        WireSphere sphere = new WireSphere(1f);

        WireSphere deepClone = Cloner.deepClone(sphere);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, sphere);

        AssetManager assetManager = new DesktopAssetManager();
        WireSphere saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, sphere);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }

    /**
     * Test cloning/saving/loading a WireFrustum.
     */
    @Test
    public void testIssue1462() {
        Vector3f[] vertices = new Vector3f[8];
        for (int vertexIndex = 0; vertexIndex < 8; vertexIndex++) {
            vertices[vertexIndex] = new Vector3f();
        }
        WireFrustum wireFrustum = new WireFrustum(vertices);
        WireFrustum deepClone = Cloner.deepClone(wireFrustum);
        Assert.assertNotNull(deepClone);
        Assert.assertNotEquals(deepClone, wireFrustum);

        AssetManager assetManager = new DesktopAssetManager();
        WireFrustum saveAndLoad
                = BinaryExporter.saveAndLoad(assetManager, wireFrustum);
        Assert.assertNotNull(saveAndLoad);
        Assert.assertNotEquals(deepClone, saveAndLoad);
    }
}
