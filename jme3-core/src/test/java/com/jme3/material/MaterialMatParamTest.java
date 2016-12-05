/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Assert;
import org.junit.Test;

import static com.jme3.scene.MPOTestUtils.*;
import com.jme3.scene.Node;
import com.jme3.shader.DefineList;
import com.jme3.system.NullRenderer;
import com.jme3.system.TestUtil;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import org.junit.Before;

/**
 * Validates {@link MatParam}s.
 *
 * @author Kirill Vainer
 */
public class MaterialMatParamTest {

    private static final HashSet<String> IGNORED_UNIFORMS = new HashSet<String>(
            Arrays.asList(new String[]{"m_ParallaxHeight", "m_Shininess", "m_BackfaceShadows"}));

    @Test
    public void testBoolMpoOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoBool("UseMaterialColors", true));
        outDefines(def("MATERIAL_COLORS", VarType.Boolean, true));
        outUniforms(uniform("UseMaterialColors", VarType.Boolean, true));
    }

    @Test
    public void testBoolMpOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoBool("UseMaterialColors", true));
        outDefines(def("MATERIAL_COLORS", VarType.Boolean, true));
        outUniforms(uniform("UseMaterialColors", VarType.Boolean, true));
    }

    @Test
    public void testBoolMpFalse() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoBool("UseMaterialColors", false));
        outDefines(def("MATERIAL_COLORS", VarType.Boolean, false));
        outUniforms(uniform("UseMaterialColors", VarType.Boolean, false));
    }
    
    @Test
    public void testBoolOverrideFalse() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoBool("UseMaterialColors", true));
        inputMpo(mpoBool("UseMaterialColors", false));
        outDefines();
        outUniforms(uniform("UseMaterialColors", VarType.Boolean, false));
    }

    @Test
    public void testBoolOverrideTrue() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoBool("UseMaterialColors", false));
        inputMpo(mpoBool("UseMaterialColors", true));
        outDefines(def("MATERIAL_COLORS", VarType.Boolean, true));
        outUniforms(uniform("UseMaterialColors", VarType.Boolean, true));
    }

    @Test
    public void testFloatMpoOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoFloat("AlphaDiscardThreshold", 3.12f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 3.12f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 3.12f));
    }

    @Test
    public void testFloatMpOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoFloat("AlphaDiscardThreshold", 3.12f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 3.12f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 3.12f));
    }

    @Test
    public void testFloatMpZero() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoFloat("AlphaDiscardThreshold", 0.0f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 0.0f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 0.0f));
    }
    
    @Test
    public void testFloatOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoFloat("AlphaDiscardThreshold", 3.12f));
        inputMpo(mpoFloat("AlphaDiscardThreshold", 2.79f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 2.79f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 2.79f));
    }

    @Test
    public void testForcedOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoFloat("AlphaDiscardThreshold", 3.12f));
        inputMpo(mpoFloat("AlphaDiscardThreshold", 2.79f));
        inputFpo(mpoFloat("AlphaDiscardThreshold", 1.23f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 1.23f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 1.23f));

        reset();
        root.clearMatParamOverrides();
        root.updateGeometricState();
        outDefines(def("DISCARD_ALPHA", VarType.Float, 2.79f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 2.79f));
    }

    @Test
    public void testChildOverridesParent() {
        material("Common/MatDefs/Light/Lighting.j3md");

        inputParentMpo(mpoFloat("AlphaDiscardThreshold", 3.12f));
        inputMpo(mpoFloat("AlphaDiscardThreshold", 2.79f));

        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 2.79f));
        outDefines(def("DISCARD_ALPHA", VarType.Float, 2.79f));
    }

    @Test
    public void testMpoDisable() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoFloat("AlphaDiscardThreshold", 3.12f));

        MatParamOverride override = mpoFloat("AlphaDiscardThreshold", 2.79f);
        inputMpo(override);
        outDefines(def("DISCARD_ALPHA", VarType.Float, 2.79f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 2.79f));

        reset();
        override.setEnabled(false);
        outDefines(def("DISCARD_ALPHA", VarType.Float, 3.12f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 3.12f));

        reset();
        override.setEnabled(true);
        outDefines(def("DISCARD_ALPHA", VarType.Float, 2.79f));
        outUniforms(uniform("AlphaDiscardThreshold", VarType.Float, 2.79f));
    }

    @Test
    public void testIntMpoOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoInt("NumberOfBones", 1234));
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));
    }

    @Test
    public void testIntMpOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoInt("NumberOfBones", 1234));
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));
    }
    
    @Test
    public void testIntMpZero() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoInt("NumberOfBones", 0));
        outDefines(def("NUM_BONES", VarType.Int, 0));
        outUniforms(uniform("NumberOfBones", VarType.Int, 0));
    }

    @Test
    public void testIntOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMp(mpoInt("NumberOfBones", 1234));
        inputMpo(mpoInt("NumberOfBones", 4321));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));
    }

    @Test
    public void testMatrixArray() {
        Matrix4f[] matrices = new Matrix4f[]{
            new Matrix4f()
        };

        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoMatrix4Array("BoneMatrices", matrices));
        outDefines();
        outUniforms(uniform("BoneMatrices", VarType.Matrix4Array, matrices));
    }

    @Test
    public void testNonExistentParameter() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoInt("NonExistent", 4321));
        outDefines();
        outUniforms();
    }

    @Test
    public void testWrongType() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoInt("UseMaterialColors", 4321));
        outDefines();
        outUniforms();
    }

    @Test
    public void testParamOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        inputMpo(mpoFloat("ShadowMapSize", 3.12f));
        outDefines();
        outUniforms(uniform("ShadowMapSize", VarType.Float, 3.12f));
    }

    @Test
    public void testRemove() {
        material("Common/MatDefs/Light/Lighting.j3md");

        reset();
        inputMp(mpoInt("NumberOfBones", 1234));
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));

        reset();
        inputMpo(mpoInt("NumberOfBones", 4321));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));

        reset();
        geometry.clearMatParamOverrides();
        root.updateGeometricState();
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));

        reset();
        geometry.getMaterial().clearParam("NumberOfBones");
        outDefines();
        outUniforms();

        reset();
        inputMpo(mpoInt("NumberOfBones", 4321));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));

        reset();
        inputMp(mpoInt("NumberOfBones", 1234));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));
    }

    public void testRemoveOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");

        reset();
        inputMp(mpoInt("NumberOfBones", 1234));
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));

        reset();
        inputMpo(mpoInt("NumberOfBones", 4321));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));

        reset();
        geometry.clearMatParamOverrides();
        outDefines(def("NUM_BONES", VarType.Int, 1234));
        outUniforms(uniform("NumberOfBones", VarType.Int, 1234));
    }

    @Test
    public void testRemoveMpoOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");

        reset();
        inputMpo(mpoInt("NumberOfBones", 4321));
        outDefines(def("NUM_BONES", VarType.Int, 4321));
        outUniforms(uniform("NumberOfBones", VarType.Int, 4321));

        reset();
        geometry.clearMatParamOverrides();
        root.updateGeometricState();
        outDefines();
        outUniforms();
    }

    @Test
    public void testTextureMpoOnly() {
        material("Common/MatDefs/Light/Lighting.j3md");
        Texture2D tex = new Texture2D(128, 128, Format.RGBA8);

        inputMpo(mpoTexture2D("DiffuseMap", tex));
        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex);
    }

    @Test
    public void testTextureOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");
        Texture2D tex1 = new Texture2D(128, 128, Format.RGBA8);
        Texture2D tex2 = new Texture2D(128, 128, Format.RGBA8);

        inputMp(mpoTexture2D("DiffuseMap", tex1));
        inputMpo(mpoTexture2D("DiffuseMap", tex2));

        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex2));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex2);
    }

    @Test
    public void testRemoveTexture() {
        material("Common/MatDefs/Light/Lighting.j3md");
        Texture2D tex = new Texture2D(128, 128, Format.RGBA8);

        reset();
        inputMpo(mpoTexture2D("DiffuseMap", tex));
        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex);

        reset();
        geometry.clearMatParamOverrides();
        root.updateGeometricState();
        outDefines();
        outUniforms();
        outTextures();
    }

    @Test
    public void testRemoveTextureOverride() {
        material("Common/MatDefs/Light/Lighting.j3md");
        Texture2D tex1 = new Texture2D(128, 128, Format.RGBA8);
        Texture2D tex2 = new Texture2D(128, 128, Format.RGBA8);

        reset();
        inputMp(mpoTexture2D("DiffuseMap", tex1));
        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex1));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex1);

        reset();
        inputMpo(mpoTexture2D("DiffuseMap", tex2));
        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex2));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex2);

        reset();
        geometry.clearMatParamOverrides();
        root.updateGeometricState();
        outDefines(def("DIFFUSEMAP", VarType.Texture2D, tex1));
        outUniforms(uniform("DiffuseMap", VarType.Int, 0));
        outTextures(tex1);
    }

    private static class Define {

        public String name;
        public VarType type;
        public Object value;

        @Override
        public String toString() {
            switch (type) {
                case Boolean:
                    if ((Boolean)value) {
                        return "#define " + name + " 1\n";
                    } else {
                        return "";
                    }
                case Int:
                case Float:
                    return "#define " + name + " " + value + "\n";
                default:
                    if (value != null) {
                        return "#define " + name + " 1\n";
                    } else {
                        return "";
                    }
            }
        }
    }

    private final Geometry geometry = new Geometry("Geometry", new Box(1, 1, 1));
    private final Node root = new Node("Root Node");
    private final LightList lightList = new LightList(geometry);

    @Before
    public void setUp() {
        root.attachChild(geometry);
    }

    private final NullRenderer renderer = new NullRenderer() {
        @Override
        public void setShader(Shader shader) {
            MaterialMatParamTest.this.usedShader = shader;
            evaluated = true;
        }

        @Override
        public void setTexture(int unit, Texture texture) {
            MaterialMatParamTest.this.usedTextures[unit] = texture;
        }
    };
    private final RenderManager renderManager = new RenderManager(renderer);

    private boolean evaluated = false;
    private Shader usedShader = null;
    private final Texture[] usedTextures = new Texture[32];

    private void inputMp(MatParam... params) {
        if (evaluated) {
            throw new IllegalStateException();
        }
        Material mat = geometry.getMaterial();
        for (MatParam param : params) {
            mat.setParam(param.getName(), param.getVarType(), param.getValue());
        }
    }

    private void inputMpo(MatParamOverride... overrides) {
        if (evaluated) {
            throw new IllegalStateException();
        }
        for (MatParamOverride override : overrides) {
            geometry.addMatParamOverride(override);
        }
        root.updateGeometricState();
    }

    private void inputParentMpo(MatParamOverride... overrides) {
        if (evaluated) {
            throw new IllegalStateException();
        }
        for (MatParamOverride override : overrides) {
            root.addMatParamOverride(override);
        }
        root.updateGeometricState();
    }

    private void inputFpo(MatParamOverride... overrides) {
        if (evaluated) {
            throw new IllegalStateException();
        }
        for (MatParamOverride override : overrides) {
            renderManager.addForcedMatParam(override);
        }
    }

    private void reset() {
        evaluated = false;
        usedShader = null;
        Arrays.fill(usedTextures, null);
        for (MatParamOverride override : new ArrayList<>(renderManager.getForcedMatParams())) {
            renderManager.removeForcedMatParam(override);
        }
    }

    private Define def(String name, VarType type, Object value) {
        Define d = new Define();
        d.name = name;
        d.type = type;
        d.value = value;
        return d;
    }

    private Uniform uniform(String name, VarType type, Object value) {
        Uniform u = new Uniform();
        u.setName("m_" + name);
        u.setValue(type, value);
        return u;
    }

    private void material(String path) {
        AssetManager assetManager = TestUtil.createAssetManager();
        geometry.setMaterial(new Material(assetManager, path));
    }

    private void evaluateTechniqueDef() {
        Assert.assertFalse(evaluated);
        Material mat = geometry.getMaterial();
        mat.render(geometry, lightList, renderManager);
        Assert.assertTrue(evaluated);
    }

    private void outTextures(Texture... textures) {
        for (int i = 0; i < usedTextures.length; i++) {
            if (i < textures.length) {
                Assert.assertSame(textures[i], usedTextures[i]);
            } else {
                Assert.assertNull(usedTextures[i]);
            }
        }
    }

    private void outDefines(Define... expectedDefinesArray) {
        StringBuilder expectedDefineSource = new StringBuilder();
        for (Define define : expectedDefinesArray) {
            expectedDefineSource.append(define.toString());
        }
        
        if (!evaluated) {
            evaluateTechniqueDef();
        }

        Material mat = geometry.getMaterial();
        Technique tech = mat.getActiveTechnique();
        TechniqueDef def = tech.getDef();
        DefineList actualDefines = tech.getDynamicDefines();
        String[] defineNames = def.getDefineNames();
        VarType[] defineTypes = def.getDefineTypes();
        String actualDefineSource = actualDefines.generateSource(Arrays.asList(defineNames), Arrays.asList(defineTypes));

        assertEquals(expectedDefineSource.toString(), actualDefineSource);
    }

    private void outUniforms(Uniform... uniforms) {
        if (!evaluated) {
            evaluateTechniqueDef();
        }

        HashSet<Uniform> actualUniforms = new HashSet<>();

        for (Uniform uniform : usedShader.getUniformMap().values()) {
            if (uniform.getName().startsWith("m_")
                    && !IGNORED_UNIFORMS.contains(uniform.getName())) {
                actualUniforms.add(uniform);
            }
        }

        HashSet<Uniform> expectedUniforms = new HashSet<>(Arrays.asList(uniforms));

        if (!expectedUniforms.equals(actualUniforms)) {
            Assert.fail("Uniform lists must match: " + expectedUniforms + " != " + actualUniforms);
        }
    }
}
