package com.jme3.vulkan;

import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.pipelines.Pipeline;
import com.jme3.vulkan.pipelines.PipelineBindPoint;
import com.jme3.vulkan.pipelines.PipelineCache;
import com.jme3.vulkan.pipelines.newstate.PipelineState;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * Optimally renders an ordered collection of geometries from a certain view.
 *
 * <p>The user is expected to provide the Comparator with which to sort
 * the geometries into the optimal rendering order. It is encouraged to
 * sort the geometries as to minimize pipeline, material, and material
 * parameter switches.</p>
 */
public class GeometryBatch implements Iterable<GeometryBatch.Element> {

    private final Camera camera;
    private final PipelineCache cache;
    private final PriorityQueue<Element> queue;
    private String forcedTechnique = null;
    private Material forcedMaterial = null;
    private Mesh forcedMesh = null;
    private PipelineState overrideState = null;

    public GeometryBatch(Camera camera, PipelineCache cache, Comparator<Element> comparator) {
        this.camera = camera;
        this.cache = cache;
        this.queue = new PriorityQueue<>(comparator);
    }

    public void render(RenderManager rm, CommandBuffer cmd) {
        Pipeline currentPipeline = null;
        Material currentMaterial = null;
        Mesh currentMesh = null;
        for (Element e : queue) {
            // I'm not sure if it's safe or optimal to allow controls to muck with
            // geometry/material/mesh at this point, but this would technically
            // be the location to do this.
            //e.getGeometry().runControlRender(rm, cmd, camera);

            // Provide transform matrices to the material. This requires
            // that the appropriate buffers be updated on or after this point.
            e.getMaterial().updateTransform(e.getGeometry(), e.getCamera());

            // bind the selected pipeline if necessary
            if (e.getPipeline() != currentPipeline) {
                if (!e.getPipeline().isMaterialEquivalent(currentPipeline)) {
                    currentMaterial = null; // must explicitly bind next material
                }
                (currentPipeline = e.getPipeline()).bind(cmd);
            }

            // bind the material if necessary
            if (e.getMaterial() != currentMaterial && !(currentMaterial = e.getMaterial()).bind(cmd, currentPipeline)) {
                throw new IllegalStateException("Attempted to bind material with an incompatible pipeline.");
            }

            // bind the mesh if necessary
            if (currentMesh != e.getMesh()) {
                (currentMesh = e.getMesh()).bind(rm, cmd, e.getGeometry());
            }

            // render the mesh
            currentMesh.render(rm, cmd, e.getGeometry());
        }
    }

    private boolean fastAdd(Geometry geometry) {
        return queue.add(new Element(geometry));
    }

    public boolean add(Geometry geometry) {
        return add(geometry, true, true);
    }

    public boolean add(Geometry geometry, boolean respectCullHints, boolean frustumCulling) {
        Spatial.CullHint h = geometry.getCullHint();
        if ((!respectCullHints || h != Spatial.CullHint.Always)
                && (!frustumCulling
                    || (respectCullHints && h == Spatial.CullHint.Never)
                    || camera.contains(geometry.getWorldBound()) != Camera.FrustumIntersect.Outside)) {
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
                    addAll(child, false, false);
                    it.skipChildren();
                    continue; // current spatial is handled by the nested addAll call
                }
            }
            if (frustumCulling && (!respectCullHints || child.getCullHint() == Spatial.CullHint.Dynamic)) {
                Camera.FrustumIntersect intersect = camera.contains(child.getWorldBound());
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

    public void setOverrideState(PipelineState overrideState) {
        this.overrideState = overrideState;
    }

    public Camera getCamera() {
        return camera;
    }

    public PipelineCache getCache() {
        return cache;
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

    public PipelineState getOverrideState() {
        return overrideState;
    }

    @Override
    public Iterator<Element> iterator() {
        return queue.iterator();
    }

    public class Element {

        private final Geometry geometry;
        private final Material material;
        private final Mesh mesh;
        private final Pipeline pipeline;
        private float distanceSq = Float.NaN;

        private Element(Geometry geometry) {
            this.geometry = geometry;
            this.material = forcedMaterial != null ? forcedMaterial : this.geometry.getMaterial();
            this.mesh = forcedMesh != null ? forcedMesh : this.geometry.getMesh();
            this.pipeline = this.material.selectPipeline(cache, this.geometry.getMesh().getDescription(), forcedTechnique, overrideState);
            if (!this.pipeline.getBindPoint().is(PipelineBindPoint.Graphics)) {
                throw new IllegalStateException("Cannot render geometry with a non-graphical pipeline.");
            }
        }

        public float computeDistanceSq() {
            if (!Float.isNaN(distanceSq)) return distanceSq;
            return (distanceSq = camera.getLocation().distanceSquared(geometry.getWorldTranslation()));
        }

        public Camera getCamera() {
            return camera;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public Material getMaterial() {
            return material;
        }

        public Mesh getMesh() {
            return mesh;
        }

        public Pipeline getPipeline() {
            return pipeline;
        }

    }

}
