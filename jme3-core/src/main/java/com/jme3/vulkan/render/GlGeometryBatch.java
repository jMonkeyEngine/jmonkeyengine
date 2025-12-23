package com.jme3.vulkan.render;

import com.jme3.light.LightList;
import com.jme3.material.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.util.ListMap;
import com.jme3.util.SafeArrayList;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.pipeline.Pipeline;

import java.util.Comparator;

public class GlGeometryBatch extends GeometryBatch<GlGeometryBatch.Element> {

    private final RenderManager renderManager;
    private final boolean ortho;
    private final RenderState mergedState = new RenderState();
    private final LightList filteredLights = new LightList(null);
    private RenderState forcedState = null;
    private LightList forcedLights = null;

    public GlGeometryBatch(RenderManager renderManager, Camera camera, boolean ortho, Comparator<Element> comparator) {
        super(camera, comparator);
        this.renderManager = renderManager;
        this.ortho = ortho;
    }

    @Override
    public void render(CommandBuffer cmd) {
        renderManager.setCamera(camera, ortho);
        Renderer renderer = renderManager.getRenderer();
        for (Element e : queue) {
            if (e.getPipeline().getDef().isNoRender()) {
                continue;
            }
            updateRenderState(e.getGeometry(), e.getState(),
                    e.getAdditionalState(), renderer, e.getPipeline().getDef());
            SafeArrayList<MatParamOverride> overrides = e.getGeometry().getWorldMatParamOverrides();
            LightList lights = renderManager.filterLights(e.getGeometry(), e.getLights(), filteredLights);
            Shader shader = e.getPipeline().makeCurrent(renderManager, overrides,
                    renderManager.getForcedMatParams(), lights, renderer.getCaps());
            clearUniformsSetByCurrent(shader);
            renderManager.updateUniformBindings(shader);
            GlMaterial.BindUnits units = e.getMaterial().updateShaderMaterialParameters(
                    renderer, shader, overrides, renderManager.getForcedMatParams());
            resetUniformsNotSetByCurrent(shader);
            e.getPipeline().render(renderManager, shader, e.getGeometry(), e.getMesh(), lights, units);
        }
    }

    private void updateRenderState(Geometry geometry, RenderState forcedState, RenderState additionalState,
                                   Renderer renderer, TechniqueDef techniqueDef) {
        RenderState finalRenderState;
        if (forcedState != null) {
            finalRenderState = mergedState.copyFrom(forcedState);
        } else if (techniqueDef.getRenderState() != null) {
            finalRenderState = mergedState.copyFrom(RenderState.DEFAULT);
            finalRenderState = techniqueDef.getRenderState().copyMergedTo(additionalState, finalRenderState);
        } else {
            finalRenderState = mergedState.copyFrom(RenderState.DEFAULT);
            finalRenderState = RenderState.DEFAULT.copyMergedTo(additionalState, finalRenderState);
        }
        // test if the face cull mode should be flipped before render
        if (finalRenderState.isFaceCullFlippable() && isNormalsBackward(geometry.getWorldScale())) {
            finalRenderState.flipFaceCull();
        }
        renderer.applyRenderState(finalRenderState);
    }

    private boolean isNormalsBackward(Vector3f scalar) {
        // count number of negative scalar vector components
        int n = 0;
        if (scalar.x < 0) n++;
        if (scalar.y < 0) n++;
        if (scalar.z < 0) n++;
        // An odd number of negative components means the normal vectors
        // are backward to what they should be.
        return n == 1 || n == 3;
    }

    private void clearUniformsSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            u.clearSetByCurrentMaterial();
        }
    }

    private void resetUniformsNotSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            if (!u.isSetByCurrentMaterial()) {
                if (u.getName().charAt(0) != 'g') {
                    // Don't reset world globals!
                    // The benefits gained from this are very minimal
                    // and cause lots of matrix -> FloatBuffer conversions.
                    u.clearValue();
                }
            }
        }
    }

    @Override
    protected boolean add(Geometry geometry) {
        return queue.add(new Element(geometry));
    }

    @Override
    protected Camera.FrustumIntersect frustumIntersect(Spatial spatial) {
        if (ortho) {
            return camera.containsGui(spatial.getWorldBound()) ?
                    Camera.FrustumIntersect.Intersects : Camera.FrustumIntersect.Outside;
        } else return camera.contains(spatial.getWorldBound());
    }

    public void setForcedState(RenderState forcedState) {
        this.forcedState = forcedState;
    }

    public void setForcedLights(LightList forcedLights) {
        this.forcedLights = forcedLights;
    }

    public RenderState getForcedState() {
        return forcedState;
    }

    public LightList getForcedLights() {
        return forcedLights;
    }

    public class Element implements BatchElement {

        private final Geometry geometry;
        private final GlMaterial material;
        private final GlMesh mesh;
        private final Technique technique;
        private final RenderState state;
        private final RenderState additionalState;
        private final LightList lights;
        private float distanceSq = Float.NaN;
        private float distance = Float.NaN;

        public Element(Geometry geometry) {
            this.geometry = geometry;
            Material mat = forcedMaterial != null ? forcedMaterial : geometry.getMaterial();
            if (!(mat instanceof GlMaterial)) {
                throw new ClassCastException("Cannot render " + mat.getClass() + " in an OpenGL context.");
            }
            this.material = (GlMaterial)mat;
            Mesh m = forcedMesh != null ? forcedMesh : geometry.getMesh();
            if (!(m instanceof GlMesh)) {
                throw new ClassCastException("Cannot render " + m.getClass() + " in an OpenGL context.");
            }
            this.mesh = (GlMesh)m;
            this.technique = this.material.selectTechnique(forcedTechnique, renderManager);
            this.state = forcedState != null ? forcedState : renderManager.getForcedRenderState();
            this.additionalState = this.material.getAdditionalRenderState();
            this.lights = forcedLights != null ? forcedLights : this.geometry.getWorldLightList();
        }

        @Override
        public float computeDistanceSq() {
            if (!Float.isNaN(distanceSq)) return distanceSq;
            else return (distanceSq = geometry.getWorldTranslation().distanceSquared(camera.getLocation()));
        }

        @Override
        public float computeDistance() {
            if (!Float.isNaN(distance)) return distance;
            else return (distance = FastMath.sqrt(computeDistanceSq()));
        }

        @Override
        public Camera getCamera() {
            return camera;
        }

        @Override
        public Geometry getGeometry() {
            return geometry;
        }

        @Override
        public GlMaterial getMaterial() {
            return material;
        }

        @Override
        public GlMesh getMesh() {
            return mesh;
        }

        @Override
        public Technique getPipeline() {
            return technique;
        }

        public RenderState getState() {
            return state;
        }

        public RenderState getAdditionalState() {
            return additionalState;
        }

        public LightList getLights() {
            return lights;
        }

    }

}
