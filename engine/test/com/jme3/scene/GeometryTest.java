package com.jme3.scene;

import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.junit.Test;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class GeometryTest {

    @Test
    public void testConstructorNameNull() {
        Geometry geom = new Geometry(null);
        assertNull(geom.getName());
    }

    @Test
    public void testConstructorName() {
        Geometry geom = new Geometry("TestGeometry");
        assertEquals("TestGeometry", geom.getName());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNameMeshNullMesh() {
        Geometry geom = new Geometry("TestGeometry", null);
    }

    @Test
    public void testConstructorNameMesh() {
        Mesh m = new Mesh();
        Geometry geom = new Geometry("TestGeometry", m);
        assertEquals("TestGeometry", geom.getName());
        assertEquals(m, geom.getMesh());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetLodLevelMeshLodZero() {
        Mesh m = new Mesh();
        Geometry geom = new Geometry("TestGeometry", m);
        geom.setLodLevel(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetLodLevelLodLessZero() {
        Mesh m = new Mesh();
        VertexBuffer lodLevels = new VertexBuffer(VertexBuffer.Type.Size);
        m.setLodLevels(new VertexBuffer[]{lodLevels});
        Geometry geom = new Geometry("TestGeometry", m);
        geom.setLodLevel(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetLodLevelLodGreaterMeshLod() {
        Mesh m = new Mesh();
        VertexBuffer lodLevel = new VertexBuffer(VertexBuffer.Type.Size);
        m.setLodLevels(new VertexBuffer[]{lodLevel});
        Geometry geom = new Geometry("TestGeometry", m);
        geom.setLodLevel(5);
    }

    @Test
    public void testSetLodLevel() {
        Mesh m = new Mesh();
        VertexBuffer lodLevel = new VertexBuffer(VertexBuffer.Type.Size);
        m.setLodLevels(new VertexBuffer[]{lodLevel, lodLevel, lodLevel});
        Geometry geom = new Geometry("TestGeometry", m);
        geom.setLodLevel(2);
        assertEquals(2, geom.getLodLevel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMeshNull() {
        Geometry geom = new Geometry();
        geom.setMesh(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetMeshBatched() {
        Mesh m = new Mesh();
        Geometry geom = new Geometry();
        BatchNode bn = new BatchNode();
        geom.batch(bn, 1);
        geom.setMesh(m);
    }

    @Test
    public void testSetMesh() {
        Mesh m = new Mesh();
        Geometry geom = new Geometry();
        geom.setMesh(m);
        assertEquals(m, geom.getMesh());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetMaterialBatched() {
        Material m = new Material();
        Geometry geom = new Geometry();
        BatchNode bn = new BatchNode();
        geom.batch(bn, 1);
        geom.setMaterial(m);
    }

    @Test
    public void testSetMaterial() {
        Material m = new Material();
        Geometry geom = new Geometry();
        geom.setMaterial(m);
        assertEquals(m, geom.getMaterial());
    }

    @Test
    public void testUpdateModelBound() {
        Mesh mockedMesh = createMock(Mesh.class);
        mockedMesh.updateBound();
        expectLastCall();

        replay(mockedMesh);
        Geometry geom = new Geometry();
        geom.setMesh(mockedMesh);
        geom.updateModelBound();

        verify(mockedMesh);
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateWorldBoundNoMesh() {
        Geometry geom = new Geometry();
        geom.updateWorldBound();
    }

    @Test
    public void testUpdateWorlBoundNoBoundingVolume() {
        Mesh mockedMesh = createMock(Mesh.class);
        expect(mockedMesh.getBound()).andReturn(null);
        replay(mockedMesh);

        Geometry geom = new Geometry();
        geom.setMesh(mockedMesh);
        geom.updateWorldBound();

        verify(mockedMesh);
    }

    @Test
    public void testUpdateWorlBoundIgnoreTransform() {
        Mesh mockedMesh = createMock(Mesh.class);
        BoundingVolume mockedBoundingVolume = createMock(BoundingVolume.class);
        expect(mockedMesh.getBound()).andReturn(mockedBoundingVolume).times(2);
        expect(mockedBoundingVolume.clone(null)).andReturn(null);
        replay(mockedMesh, mockedBoundingVolume);

        Geometry geom = new Geometry();
        geom.setMesh(mockedMesh);
        geom.setIgnoreTransform(true);
        geom.updateWorldBound();

        verify(mockedMesh, mockedBoundingVolume);
    }

    @Test
    public void testUpdateWorlBoundTransform() {
        Mesh mockedMesh = createMock(Mesh.class);
        BoundingVolume mockedBoundingVolume = createMock(BoundingVolume.class);
        expect(mockedMesh.getBound()).andReturn(mockedBoundingVolume).times(2);
        expect(mockedBoundingVolume.transform(anyObject(Transform.class), same((BoundingVolume) null))).andReturn(null);
        replay(mockedMesh, mockedBoundingVolume);

        Geometry geom = new Geometry();
        geom.setMesh(mockedMesh);
        geom.setIgnoreTransform(false);
        geom.updateWorldBound();

        verify(mockedMesh, mockedBoundingVolume);
    }
}
