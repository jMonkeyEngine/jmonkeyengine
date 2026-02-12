package com.jme3.vulkan.render;

import com.jme3.light.LightList;
import com.jme3.material.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.*;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlMesh;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.shader.ShaderProgram;
import com.jme3.shader.Uniform;
import com.jme3.util.ListMap;
import com.jme3.util.SafeArrayList;
import com.jme3.vulkan.commands.CommandBuffer;

import java.util.Comparator;

public class GlGeometryBatch extends GeometryBatch<GlGeometryBatch.Element> {

    private final GLRenderer renderer;
    private final RenderState mergedState = new RenderState();
    private final LightList filteredLights = new LightList(null);
    private RenderState forcedState = null;
    private LightList forcedLights = null;

    public GlGeometryBatch(GLRenderer renderer, Comparator<Element> comparator) {
        super(comparator);
        this.renderer = renderer;
    }

    public void render() {
        render(null);
    }

    public void render(Runnable onRender) {
        if (onRender != null && !queue.isEmpty()) {
            onRender.run();
        }
        //renderManager.setCamera(camera, ortho);
        for (Element e : queue) {
            if (e.technique.getDef().isNoRender()) {
                continue;
            }
            renderer.applyRenderState(mergedState.integrateGeometryStates(e.getGeometry(), forcedRenderState,
                    e.getMaterial().getAdditionalRenderState(), e.technique.getDef().getRenderState()));
            SafeArrayList<MatParamOverride> overrides = e.getGeometry().getWorldMatParamOverrides();
            LightList lights = renderManager.filterLights(e.getGeometry(), e.getLights(), filteredLights);
            ShaderProgram shader = e.technique.getShader(renderManager, overrides,
                    renderManager.getForcedMatParams(), lights, renderer.getCaps());
            clearUniformsSetByCurrent(shader);
            renderManager.updateUniformBindings(shader);
            GlMaterial.BindUnits units = e.getMaterial().updateShaderMaterialParameters(
                    renderer, shader, overrides, renderManager.getForcedMatParams());
            resetUniformsNotSetByCurrent(shader);
            e.technique.render(renderer, shader, e.getGeometry(), e.getMesh(), lights, units);
        }
    }

    private void clearUniformsSetByCurrent(ShaderProgram shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            u.clearSetByCurrentMaterial();
        }
    }

    private void resetUniformsNotSetByCurrent(ShaderProgram shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            if (!u.isSetByCurrentMaterial() && u.getName().charAt(0) != 'g') {
                // Don't reset world globals!
                // The benefits gained from this are very minimal
                // and cause lots of matrix -> FloatBuffer conversions.
                u.clearValue();
            }
        }
    }

    @Override
    public boolean add(Geometry geometry) {
        return queue.add(new Element(geometry));
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
        public long getPipelineSortId() {
            return technique.getSortId();
        }

        @Override
        public long getMaterialSortId() {
            return material.getSortId();
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
