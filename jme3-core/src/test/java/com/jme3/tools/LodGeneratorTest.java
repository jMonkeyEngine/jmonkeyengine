package com.jme3.tools;

import static org.junit.Assert.assertArrayEquals;

import java.net.URL;
import java.util.ArrayList;

import org.junit.Test;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.JmeSystem;

import java.util.List;
import jme3tools.optimize.LodGenerator;

public class LodGeneratorTest {
    URL assetCfgUrl = JmeSystem.getPlatformAssetConfigURL();
    AssetManager assetManager = JmeSystem.newAssetManager(assetCfgUrl);

    float[] REDUCTION_VALUES = { 0.5f, 0.55f, 0.6f, 0.65f, 0.7f, 0.75f, 0.80f };

    @Test
    public void testInit() {
        LodGenerator lod = new LodGenerator(sphere());
        assert true;
    }

    private int[] getBufferSizes(VertexBuffer[] buffers) {
        int[] result = new int[buffers.length];
        
        for (int i = 0; i < buffers.length; i++) {
            result[i] = buffers[i].getNumElements();
        }

        return result;
    }

    @Test
    public void testSphereReductionProportional() {
        LodGenerator lod = new LodGenerator(sphere());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL,
                REDUCTION_VALUES);

        int[] expected = { 240, 120, 108, 96, 84, 72, 60, 48 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSphereReductionCollapsCost() {
        LodGenerator lod = new LodGenerator(sphere());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST,
                REDUCTION_VALUES);

        int[] expected = { 240, 1 };
        //int[] actual = getBufferSizes(buffer);
        assert buffer != null;
        //assertArrayEquals(expected, {});

    }

    private Mesh getMesh(Node node) {
        Mesh m = null;
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                m = ((Geometry) spatial).getMesh();
                if (m.getVertexCount() == 5108) {

                }

            }
        }
        return m;
    }

    private Mesh monkey() {
        Node model = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        return getMesh(model);
    }

    private Mesh sphere() {
        return new Sphere(12, 12, 1, false, false);
    }

    @Test
    public void testMonkeyReductionConstant() {

        LodGenerator lod = new LodGenerator(monkey());
        //for(int i =)
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.CONSTANT,
                REDUCTION_VALUES);

        int[] expected = { 5108 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testMonkeyReductionProportional() {

        LodGenerator lod = new LodGenerator(monkey());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.PROPORTIONAL,
                REDUCTION_VALUES);

        int[] expected = { 5108, 2553, 2298, 2043, 1787, 1531, 1276, 1021 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testMonkeyReductionCollapsCost() {
        LodGenerator lod = new LodGenerator(monkey());
        VertexBuffer[] buffer = lod.computeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST,
                REDUCTION_VALUES);

        int[] expected = { 5108, 16 };
        int[] actual = getBufferSizes(buffer);

        assertArrayEquals(expected, actual);
    }
}