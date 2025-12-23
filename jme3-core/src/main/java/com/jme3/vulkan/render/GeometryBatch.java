package com.jme3.vulkan.render;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.commands.CommandBuffer;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

public abstract class GeometryBatch <T> implements Iterable<T> {

    protected final Camera camera;
    protected final PriorityQueue<T> queue;
    protected String forcedTechnique = null;
    protected Material forcedMaterial = null;
    protected Mesh forcedMesh = null;

    public GeometryBatch(Camera camera, Comparator<T> comparator) {
        this.camera = camera;
        this.queue = new PriorityQueue<>(comparator);
    }

    public abstract void render(CommandBuffer cmd);

    public abstract boolean add(Geometry geometry);

    public void addAll(Spatial spatial) {
        for (Spatial child : spatial) {
            if (child instanceof Geometry) {
                add((Geometry)child);
            }
        }
    }

    public void addAll(Iterable<Geometry> geometries) {
        geometries.forEach(this::add);
    }

    protected Camera.FrustumIntersect frustumIntersect(Spatial spatial) {
        return camera.contains(spatial.getWorldBound());
    }

    public void clear() {
        queue.clear();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void setForcedTechnique(String forcedTechnique) {
        this.forcedTechnique = forcedTechnique;
    }

    public void setForcedMaterial(Material forcedMaterial) {
        this.forcedMaterial = forcedMaterial;
    }

    public void setForcedMesh(Mesh forcedMesh) {
        this.forcedMesh = forcedMesh;
    }

    public Camera getCamera() {
        return camera;
    }

    public String getForcedTechnique() {
        return forcedTechnique;
    }

    public Material getForcedMaterial() {
        return forcedMaterial;
    }

    public Mesh getForcedMesh() {
        return forcedMesh;
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

}
