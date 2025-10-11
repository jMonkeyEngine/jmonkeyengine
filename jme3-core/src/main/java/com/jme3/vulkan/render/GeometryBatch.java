package com.jme3.vulkan.render;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.pipelines.newstate.PipelineState;

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

    protected abstract boolean fastAdd(Geometry geometry);

    public boolean add(Geometry geometry) {
        return add(geometry, true, true);
    }

    public boolean add(Geometry geometry, boolean respectCullHints, boolean frustumCulling) {
        Spatial.CullHint h = geometry.getCullHint();
        if ((!respectCullHints || h != Spatial.CullHint.Always)
                && (!frustumCulling
                || (respectCullHints && h == Spatial.CullHint.Never)
                || frustumIntersect(geometry) != Camera.FrustumIntersect.Outside)) {
            return fastAdd(geometry);
        }
        return false;
    }

    public void addAll(Spatial spatial) {
        addAll(spatial, true, true);
    }

    public void addAll(Spatial spatial, boolean respectCullHints, boolean frustumCulling) {
        for (Spatial.GraphIterator it = spatial.iterator(); it.hasNext();) {
            Spatial child = it.next();
            if (respectCullHints) {
                if (child.getCullHint() == Spatial.CullHint.Always) {
                    it.skipChildren();
                    continue;
                } else if (child.getCullHint() == Spatial.CullHint.Never) {
                    forceAddAll(child);
                    it.skipChildren();
                    continue; // current spatial is handled by the nested addAll call
                }
            }
            if (frustumCulling && (!respectCullHints || child.getCullHint() == Spatial.CullHint.Dynamic)) {
                Camera.FrustumIntersect intersect = frustumIntersect(child);
                if (intersect == Camera.FrustumIntersect.Outside) {
                    it.skipChildren();
                    continue;
                } else if (intersect == Camera.FrustumIntersect.Inside) {
                    addAll(child, respectCullHints, false);
                    it.skipChildren();
                    continue; // current spatial is handled by the nested addAll call
                }
            }
            if (child instanceof Geometry) {
                fastAdd((Geometry)child);
            }
        }
    }

    public void forceAddAll(Spatial spatial) {
        for (Spatial.GraphIterator it = spatial.iterator(); it.hasNext();) {
            Spatial child = it.next();
            if (child instanceof Geometry) {
                fastAdd((Geometry)child);
            }
        }
    }

    public int addAll(Iterable<Geometry> geometries) {
        return addAll(geometries, true, true);
    }

    public int addAll(Iterable<Geometry> geometries, boolean respectCullHints, boolean frustumCulling) {
        int count = 0;
        for (Geometry g : geometries) {
            if (add(g, respectCullHints, frustumCulling)) {
                count++;
            }
        }
        return count;
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
