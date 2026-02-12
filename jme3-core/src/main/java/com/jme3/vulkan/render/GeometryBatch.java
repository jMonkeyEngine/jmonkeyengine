package com.jme3.vulkan.render;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;

public class GeometryBatch <T extends BatchElement> implements Iterable<T> {

    protected final PriorityQueue<T> queue;
    protected Camera camera;
    protected String forcedTechnique;
    protected Material forcedMaterial;
    protected Mesh forcedMesh;
    protected RenderState forcedRenderState;

    public GeometryBatch(Comparator<? super T> comparator) {
        this.queue = new PriorityQueue<>(comparator);
    }

    public void add(T element) {
        queue.add(element);
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

    public void setCamera(Camera camera) {
        this.camera = Objects.requireNonNull(camera, "Camera cannot be null.");
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

    public void setForcedRenderState(RenderState forcedRenderState) {
        this.forcedRenderState = forcedRenderState;
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

    public RenderState getForcedRenderState() {
        return forcedRenderState;
    }

    public PriorityQueue<T> getQueue() {
        return queue;
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

}
