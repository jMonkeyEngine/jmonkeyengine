/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.renderer.framegraph.passes;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.SkyLightAndReflectionProbeRender;
import com.jme3.material.logic.TechniqueDefLogic;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.scene.Geometry;
import com.jme3.shader.DefineList;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Renders GBuffer information using deferred lighting to a color texture.
 * 
 * @author codex
 */
public class DeferredPass extends RenderPass implements TechniqueDefLogic {
    
    /**
     * Indicates the maximum number of directional, point, and spot lights
     * that can be handled using buffers.
     * <p>
     * Excess lights will be discarded.
     * <p>
     * <strong>Development Note:</strong> if more uniforms are added to the
     * shader, this value may need to be decreased.
     */
    public static final int MAX_BUFFER_LIGHTS = 338;
    /**
     * Indicates the maximum number of light probes that can be handled.
     * <p>
     * Excess light probes will be discarded.
     */
    public static final int MAX_PROBES = 3;
    
    private static Defines defs;
    private static final List<LightProbe> localProbeList = new LinkedList<>();
    
    private boolean tiled = false;
    private AssetManager assetManager;
    private ResourceTicket<Texture2D> outColor;
    private ResourceTicket<LightList> lights;
    private ResourceTicket<Integer> numLights;
    private ResourceTicket<ColorRGBA> ambient;
    private ResourceTicket<List<LightProbe>> probes;
    private TextureDef<Texture2D> colorDef;
    private Material material;
    private final Texture2D[] lightTextures = new Texture2D[3];
    private final Texture2D[] tileTextures = new Texture2D[2];
    private final ColorRGBA ambientColor = new ColorRGBA();
    private List<LightProbe> probeList;
    
    public DeferredPass() {}
    public DeferredPass(boolean tiled) {
        this.tiled = tiled;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        // Lighting Arguments:
        // IF LightTextures = undefined
        //     EITHER provide raw light list
        //     OR provide processed light list (without ambient or probes), ambient color, and probe list.
        // ELSE provide 3 light textures, number of lights, ambient color, and probe list.
        addInputGroup("GBufferData", 5);
        lights = addInput("Lights");
        addInputGroup("LightTextures", 3);
        addInputGroup("TileTextures", 2);
        numLights = addInput("NumLights");
        ambient = addInput("Ambient");
        probes = addInput("Probes");
        outColor = addOutput("Color");
        colorDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        colorDef.setFormatFlexible(true);
        assetManager = frameGraph.getAssetManager();
        material = new Material(assetManager, "Common/MatDefs/ShadingCommon/DeferredShading.j3md");
        if (defs == null) {
            defs = new Defines();
            for (TechniqueDef t : material.getMaterialDef().getTechniqueDefs("DeferredPass")) {
                defs.config(t);
            }
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        colorDef.setSize(context.getWidth(), context.getHeight());
        declare(colorDef, outColor);
        reserve(outColor);
        // groups are stored by hashmap, which makes it absolutely fine to fetch every frame
        reference(getGroupArray("GBufferData"));
        referenceOptional(lights, numLights, ambient, probes);
        referenceOptional(getGroupArray("LightTextures"));
        referenceOptional(getGroupArray("TileTextures"));
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTargets(fb, outColor);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        ResourceTicket<Texture2D>[] gbuffers = getGroupArray("GBufferData");
        for (int i = 0; i < gbuffers.length; i++) {
            material.setTexture("GBuffer"+i, resources.acquire(gbuffers[i]));
        }
        material.selectTechnique("DeferredPass", context.getRenderManager());
        material.getActiveTechnique().getDef().setLogic(this);
        acquireArrayOrElse("LightTextures", lightTextures, null);
        if (lightTextures[0] == null) {
            context.getScreen().render(context.getRenderManager(), material, resources.acquire(lights));
        } else {
            for (int i = 1; i <= lightTextures.length; i++) {
                material.setTexture("LightTex"+i, lightTextures[i-1]);
            }
            // get textures used for screenspace light tiling
            acquireArrayOrElse("TileTextures", tileTextures, null);
            material.setTexture("Tiles", tileTextures[0]);
            material.setTexture("LightIndex", tileTextures[1]);
            context.renderFullscreen(material);
        }
        material.getActiveTechnique().getDef().setLogic(null);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public Shader makeCurrent(AssetManager assetManager, RenderManager renderManager,
            EnumSet<Caps> rendererCaps, LightList lights, DefineList defines) {
        // defines should only be set in this method
        if (lightTextures[0] == null) {
            ColorRGBA amb = resources.acquireOrElse(ambient, null);
            if (amb == null) {
                probeList = localProbeList;
                // extract ambient and probes from light list
                SkyLightAndReflectionProbeRender.extractSkyLightAndReflectionProbes(
                        lights, ambientColor, probeList, true);
            } else {
                // lights are already processed: get ambient and probes from resources
                ambientColor.set(amb);
                probeList = resources.acquire(probes);
            }
            defines.set(defs.numLights, Math.min(lights.size(), MAX_BUFFER_LIGHTS)*3);
        } else {
            // get resources for lighting with textures
            ambientColor.set(resources.acquire(ambient));
            probeList = resources.acquire(probes);
            defines.set(defs.useTextures, true);
            defines.set(defs.numLights, resources.acquire(numLights));
            if (tileTextures[0] != null) {
                defines.set(defs.useTiles, true);
            }
        }
        // this may need to be changed to only be enabled when there is an ambient light present
        defines.set(defs.useAmbientLight, true);
        defines.set(defs.numProbes, getNumReadyProbes(probeList));
        return material.getActiveTechnique().getDef().getShader(assetManager, rendererCaps, defines);
    }
    @Override
    public void render(RenderManager rm, Shader shader, Geometry geometry,
            LightList lights, Material.BindUnits lastBindUnits) {
        Renderer renderer = rm.getRenderer();
        injectShaderGlobals(rm, shader, lastBindUnits.textureUnit);
        if (lightTextures[0] == null) {
            // pass light data by uniforms
            injectLightBuffers(shader, lights);
        } else {
            // pass light data by textures
            injectLightTextures(shader);
        }
        renderer.setShader(shader);
        TechniqueDefLogic.renderMeshFromGeometry(renderer, geometry);
        probeList = null;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(tiled, "tiled", false);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        tiled = in.readBoolean("tiled", false);
    }
    
    private int getNumReadyProbes(List<LightProbe> probes) {
        int n = 0;
        if (probes == null) {
            return n;
        }
        for (LightProbe p : probes) {
            if (p.isEnabled() && p.isReady() && ++n == MAX_PROBES) {
                break;
            }
        }
        return n;
    }
    
    private void injectShaderGlobals(RenderManager rm, Shader shader, int lastTexUnit) {
        shader.getUniform("g_AmbientLightColor").setValue(VarType.Vector4, ambientColor);
        if (probeList != null && !probeList.isEmpty()) {
            int i = 0;
            // inject light probes
            for (LightProbe p : probeList) {
                if (!p.isEnabled() || !p.isReady()) {
                    continue;
                }
                String num = (++i > 1 ? String.valueOf(i) : "");
                Uniform sky = shader.getUniform("g_SkyLightData"+num);
                Uniform coeffs = shader.getUniform("g_ShCoeffs"+num);
                Uniform env = shader.getUniform("g_ReflectionEnvMap"+num);
                lastTexUnit = SkyLightAndReflectionProbeRender.setSkyLightAndReflectionProbeData(
                        rm, lastTexUnit, sky, coeffs, env, p);
                if (i == MAX_PROBES) break;
            }
        } else {
            // disable light probes
            shader.getUniform("g_SkyLightData").setValue(VarType.Matrix4, LightProbe.FALLBACK_MATRIX);
        }
    }
    private void injectLightBuffers(Shader shader, LightList lights) {
        // ambient lights and probes should already have been extracted at this point
        Uniform data = shader.getUniform("g_LightData");
        int n = Math.min(lights.size(), MAX_BUFFER_LIGHTS)*3;
        data.setVector4Length(n);
        int i = 0, lightCount = 0;
        for (Light l : lights) {
            if (lightCount++ >= MAX_BUFFER_LIGHTS) {
                break;
            }
            Light.Type type = l.getType();
            writeColorToUniform(data, l.getColor(), type.getId(), i++);
            switch (type) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight)l;
                    writeVectorToUniform(data, dl.getDirection(), -1, i++);
                    data.setVector4InArray(0, 0, 0, 0, i++);
                    break;
                case Point:
                    PointLight pl = (PointLight)l;
                    writeVectorToUniform(data, pl.getPosition(), pl.getInvRadius(), i++);
                    data.setVector4InArray(0, 0, 0, 0, i++);
                    break;
                case Spot:
                    SpotLight sl = (SpotLight)l;
                    writeVectorToUniform(data, sl.getPosition(), sl.getInvSpotRange(), i++);
                    writeVectorToUniform(data, sl.getDirection(), sl.getPackedAngleCos(), i++);
                    break;
                default:
                    throw new UnsupportedOperationException("Light "+type+" not supported.");
            }
        }
        // just in case, fill in the remaining elements
        while (i < n) {
            data.setVector4InArray(0, 0, 0, 0, i++);
        }
    }
    private void injectLightTextures(Shader shader) {
        int w = lightTextures[0].getImage().getWidth();
        shader.getUniform("m_LightTexInv").setValue(VarType.Float, 1f/w);
        if (tileTextures[0] != null) {
            w = tileTextures[1].getImage().getWidth();
            int h = tileTextures[1].getImage().getHeight();
            shader.getUniform("m_LightIndexSize").setValue(VarType.Vector3, new Vector3f(w-0.5f, 1f/w, 1f/h));
        }
    }
    
    private void writeVectorToUniform(Uniform uniform, Vector3f vec, float w, int i) {
        uniform.setVector4InArray(vec.x, vec.y, vec.z, w, i);
    }
    private void writeColorToUniform(Uniform uniform, ColorRGBA color, float a, int i) {
        uniform.setVector4InArray(color.r, color.g, color.b, a, i);
    }
    
    /**
     * Registers and tracks necessary define IDs for all deferred passes.
     */
    private static class Defines {
        
        private static final String DEFINE_NB_LIGHTS = "NB_LIGHTS";
        private static final String DEFINE_NB_PROBES = "NB_PROBES";
        private static final String DEFINE_USE_LIGHT_TEXTURES = "USE_LIGHT_TEXTURES";
        private static final String DEFINE_USE_AMBIENT_LIGHT = "USE_AMBIENT_LIGHT";
        private static final String DEFINE_TILED_LIGHTS = "TILED_LIGHTS";
        
        public int numLights, useTextures, numProbes, useAmbientLight, useTiles;
        
        public Defines() {}
        
        public void config(TechniqueDef def) {
            numLights = def.addShaderUnmappedDefine(DEFINE_NB_LIGHTS, VarType.Int);
            numProbes = def.addShaderUnmappedDefine(DEFINE_NB_PROBES, VarType.Int);
            useTextures = def.addShaderUnmappedDefine(DEFINE_USE_LIGHT_TEXTURES, VarType.Boolean);
            useAmbientLight = def.addShaderUnmappedDefine(DEFINE_USE_AMBIENT_LIGHT, VarType.Boolean);
            useTiles = def.addShaderUnmappedDefine(DEFINE_TILED_LIGHTS, VarType.Boolean);
        }
        
    }
    
}
