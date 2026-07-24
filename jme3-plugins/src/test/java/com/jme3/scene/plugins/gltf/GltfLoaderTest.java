/*
 * Copyright (c) 2017-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.plugin.TestMaterialWrite;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.JmeSystem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Created by Nehon on 07/08/2017.
 */
public class GltfLoaderTest {

    private final static String indentString = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

    private AssetManager assetManager;

    @BeforeEach
    public void init() {
        assetManager = JmeSystem.newAssetManager(
                TestMaterialWrite.class.getResource("/com/jme3/asset/Desktop.cfg"));

    }

    @Test
    public void testLoad() {
        Spatial scene = assetManager.loadModel("gltf/box/box.gltf");
        dumpScene(scene, 0);
        //        scene = assetManager.loadModel("gltf/hornet/scene.gltf");
        //        dumpScene(scene, 0);
    }

    @Test
    public void testLoadEmptyScene() {
        try {
            Spatial scene = assetManager.loadModel("gltf/box/boxWithEmptyScene.gltf");
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assertions.fail("Failed to import gltf model with empty scene");
        }
    }

    @Test
    public void testLoadCubicSplineScaleAnimation() {
        try {
            Spatial scene = assetManager.loadModel("gltf/cubicSplineScale.gltf");
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assertions.fail("Failed to import gltf model with cubic spline scale animation");
        }
    }

    @Test
    public void testLightsPunctualExtension() {
        try {
            Spatial scene = assetManager.loadModel("gltf/lights/lights.gltf");
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assertions.fail("Failed to import gltf model with lights punctual extension");
        }
    }


    @Test
    public void testRequiredExtensionHandling() {

        // By default, the unsupported extension that is listed in
        // the 'extensionsRequired' will cause an AssetLoadException
        Assertions.assertThrows(AssetLoadException.class, () -> {
            GltfModelKey gltfModelKey = new GltfModelKey("gltf/TriangleUnsupportedExtensionRequired.gltf");
            Spatial scene = assetManager.loadModel(gltfModelKey);
            dumpScene(scene, 0);
        });

        // When setting the 'strict' flag to 'false', then the
        // asset will be loaded despite the unsupported extension
        try {
            GltfModelKey gltfModelKey = new GltfModelKey("gltf/TriangleUnsupportedExtensionRequired.gltf");
            gltfModelKey.setStrict(false);
            Spatial scene = assetManager.loadModel(gltfModelKey);
            dumpScene(scene, 0);
        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assertions.fail("Failed to load TriangleUnsupportedExtensionRequired");
        }

    }

    @Test
    public void testDracoExtension() {
        try {
            Spatial scene = assetManager.loadModel("gltf/unitSquare11x11_unsignedShortTexCoords-draco.glb");

            Node node0 = (Node) scene;
            Node node1 = (Node) node0.getChild(0);
            Node node2 = (Node) node1.getChild(0);
            Geometry geometry = (Geometry) node2.getChild(0);
            Mesh mesh = geometry.getMesh();

            // The geometry has 11x11 vertices arranged in a square,
            // so there are 10 x 10 * 2 triangles
            VertexBuffer indices = mesh.getBuffer(VertexBuffer.Type.Index);
            Assertions.assertEquals(10 * 10 * 2, indices.getNumElements());
            Assertions.assertEquals(VertexBuffer.Format.UnsignedShort, indices.getFormat());

            // All attributes of the 11 x 11 vertices are stored as Float
            // attributes (even the texture coordinates, which originally
            // had been normalized(!) unsigned shorts!)
            VertexBuffer positions = mesh.getBuffer(VertexBuffer.Type.Position);
            Assertions.assertEquals(11 * 11, positions.getNumElements());
            Assertions.assertEquals(VertexBuffer.Format.Float, positions.getFormat());

            VertexBuffer normal = mesh.getBuffer(VertexBuffer.Type.Normal);
            Assertions.assertEquals(11 * 11, normal.getNumElements());
            Assertions.assertEquals(VertexBuffer.Format.Float, normal.getFormat());

            VertexBuffer texCoord = mesh.getBuffer(VertexBuffer.Type.TexCoord);
            Assertions.assertEquals(11 * 11, texCoord.getNumElements());
            Assertions.assertEquals(VertexBuffer.Format.Float, texCoord.getFormat());

            dumpScene(scene, 0);

        } catch (AssetLoadException ex) {
            ex.printStackTrace();
            Assertions.fail("Failed to import unitSquare11x11_unsignedShortTexCoords");
        }
    }

    private void dumpScene(Spatial s, int indent) {
        System.err.print(indentString.substring(0, indent) + s.getName() + " (" + s.getClass().getSimpleName() + ") / " +
                s.getLocalTransform().getTranslation().toString() + ", " +
                s.getLocalTransform().getRotation().toString() + ", " +
                s.getLocalTransform().getScale().toString());
        if (s instanceof Geometry) {
            System.err.print(" / " + ((Geometry) s).getMaterial());
        }
        System.err.println();
        for (Light light : s.getLocalLightList()) {
            System.err.print(indentString.substring(0, indent + 1) + " (" + light.getClass().getSimpleName() + ")");
            if (light instanceof SpotLight) {
                Vector3f pos = ((SpotLight) light).getPosition();
                Vector3f dir = ((SpotLight) light).getDirection();
                System.err.println(" " + pos.toString() + ", " + dir.toString());
            } else if (light instanceof PointLight) {
                Vector3f pos = ((PointLight) light).getPosition();
                System.err.println(" " + pos.toString());
            } else if (light instanceof DirectionalLight) {
                Vector3f dir = ((DirectionalLight) light).getDirection();
                System.err.println(" " + dir.toString());
            } else {
                System.err.println();
            }
        }

        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                dumpScene(spatial, indent + 1);
            }
        }
    }

    @Test
    public void testPBRMaterialNoTextures() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry pbrNoTexturesCube = (Geometry) sceneNode.getChild("pbrNoTexturesCube_0");
        Material pbrMaterial = pbrNoTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(pbrMaterial, "PBR Lighting", "pbrNoTexturesMaterial");

        assertMaterialParam(pbrMaterial, "BaseColor", new ColorRGBA(0.9f, 0.6f, 0.3f, 0f));
        assertMaterialParam(pbrMaterial, "BaseColorMap", null);
        assertMaterialParam(pbrMaterial, "Metallic", 0.4f);
        assertMaterialParam(pbrMaterial, "Roughness", 0.6f);
        assertMaterialParam(pbrMaterial, "MetallicRoughnessMap", null);

        assertMaterialParam(pbrMaterial, "NormalMap", null);
        assertMaterialParam(pbrMaterial, "NormalScale", null);
        assertMaterialParam(pbrMaterial, "NormalType", -1f);

        assertMaterialParam(pbrMaterial, "LightMapAsAOMap", null);
        assertMaterialParam(pbrMaterial, "LightMap", null);
        assertMaterialParam(pbrMaterial, "AoStrength", null);
        assertMaterialParam(pbrMaterial, "AoPackedInMRMap", null);

        assertMaterialParam(pbrMaterial, "EmissiveMap", null);
        assertMaterialParam(pbrMaterial, "Emissive", new ColorRGBA(0.2f, 0.6f, 1.f, 1f));
        assertMaterialParam(pbrMaterial, "EmissiveIntensity", 2.7f);

        assertMaterialParam(pbrMaterial, "AlphaDiscardThreshold", 0.5f);
        Assertions.assertEquals(RenderState.BlendMode.Off, pbrMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Off, pbrMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(pbrMaterial, "UseVertexColor", true);
    }

    @Test
    public void testPBRMaterialWithTextures() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry pbrWithTexturesCube = (Geometry) sceneNode.getChild("pbrWithTexturesCube_0");
        Material pbrMaterial = pbrWithTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(pbrMaterial, "PBR Lighting", "pbrWithTexturesMaterial");

        assertMaterialParam(pbrMaterial, "BaseColor", ColorRGBA.White);
        assertMaterialParam(pbrMaterial, "BaseColorMap", "gltf/ColorTexture.png");
        assertMaterialParam(pbrMaterial, "Metallic", 1f);
        assertMaterialParam(pbrMaterial, "Roughness", 1f);
        assertMaterialParam(pbrMaterial, "MetallicRoughnessMap", "gltf/MetallicRoughnessOcclusionTexture.png");

        assertMaterialParam(pbrMaterial, "NormalMap", "gltf/NormalTexture.png");
        assertMaterialParam(pbrMaterial, "NormalScale", 1.8f);
        assertMaterialParam(pbrMaterial, "NormalType", 1f);

        assertMaterialParam(pbrMaterial, "LightMapAsAOMap", true);
        assertMaterialParam(pbrMaterial, "LightMap", "gltf/MetallicRoughnessOcclusionTexture.png");
        assertMaterialParam(pbrMaterial, "AoStrength", null);
        assertMaterialParam(pbrMaterial, "AoPackedInMRMap", true);

        assertMaterialParam(pbrMaterial, "EmissiveMap", "gltf/EmissiveTexture.png");
        assertMaterialParam(pbrMaterial, "Emissive", ColorRGBA.White);
        assertMaterialParam(pbrMaterial, "EmissiveIntensity", 2.7f);

        assertMaterialParam(pbrMaterial, "AlphaDiscardThreshold", null);
        Assertions.assertEquals(RenderState.BlendMode.Alpha, pbrMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Back, pbrMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(pbrMaterial, "UseVertexColor", false);
    }

    @Test
    public void testUnlitMaterialNoTextures() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry unlitNoTexturesCube = (Geometry) sceneNode.getChild("unlitNoTexturesCube_0");
        Material unlitMaterial = unlitNoTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(unlitMaterial, "Unshaded", "unlitNoTexturesMaterial");

        assertMaterialParam(unlitMaterial, "Color", new ColorRGBA(0.2f, 0.4f, 0.6f, 0f));
        assertMaterialParam(unlitMaterial, "ColorMap", null);

        assertMaterialParam(unlitMaterial, "GlowMap", null);
        assertMaterialParam(unlitMaterial, "GlowColor", ColorRGBA.Black);

        assertMaterialParam(unlitMaterial, "AlphaDiscardThreshold", 0.5f);
        Assertions.assertEquals(RenderState.BlendMode.Off, unlitMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Off, unlitMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(unlitMaterial, "VertexColor", true);
    }

    @Test
    public void testUnlitMaterialWithTextures() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry unlitWithTexturesCube = (Geometry) sceneNode.getChild("unlitWithTexturesCube_0");
        Material unlitMaterial = unlitWithTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(unlitMaterial, "Unshaded", "unlitWithTexturesMaterial");

        assertMaterialParam(unlitMaterial, "Color", ColorRGBA.White);
        assertMaterialParam(unlitMaterial, "ColorMap", "gltf/ColorTexture.png");

        assertMaterialParam(unlitMaterial, "GlowMap", null);
        assertMaterialParam(unlitMaterial, "GlowColor", ColorRGBA.Black);

        assertMaterialParam(unlitMaterial, "AlphaDiscardThreshold", null);
        Assertions.assertEquals(RenderState.BlendMode.Alpha, unlitMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Back, unlitMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(unlitMaterial, "VertexColor", false);
    }

    @Test
    public void testPBRMaterialNoTextures_LegacyMechanism() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        gltfModelKey.setMaterialAdaptersEnabled(true);
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry pbrNoTexturesCube = (Geometry) sceneNode.getChild("pbrNoTexturesCube_0");
        Material pbrMaterial = pbrNoTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(pbrMaterial, "PBR Lighting", "pbrNoTexturesMaterial");

        assertMaterialParam(pbrMaterial, "BaseColor", new ColorRGBA(0.9f, 0.6f, 0.3f, 0f));
        assertMaterialParam(pbrMaterial, "BaseColorMap", null);
        assertMaterialParam(pbrMaterial, "Metallic", 0.4f);
        assertMaterialParam(pbrMaterial, "Roughness", 0.6f);
        assertMaterialParam(pbrMaterial, "MetallicRoughnessMap", null);

        assertMaterialParam(pbrMaterial, "NormalMap", null);
        assertMaterialParam(pbrMaterial, "NormalScale", null);
        assertMaterialParam(pbrMaterial, "NormalType", -1f);

        assertMaterialParam(pbrMaterial, "LightMapAsAOMap", null);
        assertMaterialParam(pbrMaterial, "LightMap", null);
        assertMaterialParam(pbrMaterial, "AoStrength", null);
        assertMaterialParam(pbrMaterial, "AoPackedInMRMap", null);

        assertMaterialParam(pbrMaterial, "EmissiveMap", null);
        assertMaterialParam(pbrMaterial, "Emissive", new ColorRGBA(0.2f, 0.6f, 1.f, 1f));
        assertMaterialParam(pbrMaterial, "EmissiveIntensity", 2.7f);

        assertMaterialParam(pbrMaterial, "AlphaDiscardThreshold", 0.5f);
        Assertions.assertEquals(RenderState.BlendMode.Off, pbrMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Off, pbrMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(pbrMaterial, "UseVertexColor", true);
    }

    @Test
    public void testPBRMaterialWithTextures_LegacyMechanism() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        gltfModelKey.setMaterialAdaptersEnabled(true);
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry pbrWithTexturesCube = (Geometry) sceneNode.getChild("pbrWithTexturesCube_0");
        Material pbrMaterial = pbrWithTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(pbrMaterial, "PBR Lighting", "pbrWithTexturesMaterial");

        assertMaterialParam(pbrMaterial, "BaseColor", ColorRGBA.White);
        assertMaterialParam(pbrMaterial, "BaseColorMap", "gltf/ColorTexture.png");
        assertMaterialParam(pbrMaterial, "Metallic", 1f);
        assertMaterialParam(pbrMaterial, "Roughness", 1f);
        assertMaterialParam(pbrMaterial, "MetallicRoughnessMap", "gltf/MetallicRoughnessOcclusionTexture.png");

        assertMaterialParam(pbrMaterial, "NormalMap", "gltf/NormalTexture.png");
        assertMaterialParam(pbrMaterial, "NormalScale", 1.8f);
        assertMaterialParam(pbrMaterial, "NormalType", 1f);

        // Differences to new material system:
        //   - LightMap is not set to occlusion texture
        //   - LightMapAsAOMap is not set
        assertMaterialParam(pbrMaterial, "LightMapAsAOMap", null);
        assertMaterialParam(pbrMaterial, "LightMap", null);
        assertMaterialParam(pbrMaterial, "AoStrength", null);
        assertMaterialParam(pbrMaterial, "AoPackedInMRMap", true);

        assertMaterialParam(pbrMaterial, "EmissiveMap", "gltf/EmissiveTexture.png");
        assertMaterialParam(pbrMaterial, "Emissive", ColorRGBA.White);
        assertMaterialParam(pbrMaterial, "EmissiveIntensity", 2.7f);

        assertMaterialParam(pbrMaterial, "AlphaDiscardThreshold", null);
        Assertions.assertEquals(RenderState.BlendMode.Alpha, pbrMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Back, pbrMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(pbrMaterial, "UseVertexColor", false);
    }

    @Test
    public void testUnlitMaterialNoTextures_LegacyMechanism() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        gltfModelKey.setMaterialAdaptersEnabled(true);
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry unlitNoTexturesCube = (Geometry) sceneNode.getChild("unlitNoTexturesCube_0");
        Material unlitMaterial = unlitNoTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(unlitMaterial, "Unshaded", "unlitNoTexturesMaterial");

        assertMaterialParam(unlitMaterial, "Color", new ColorRGBA(0.2f, 0.4f, 0.6f, 0f));
        assertMaterialParam(unlitMaterial, "ColorMap", null);

        assertMaterialParam(unlitMaterial, "GlowMap", null);
        assertMaterialParam(unlitMaterial, "GlowColor", ColorRGBA.Black);

        // Differences to new material system:
        //   - UnlitMaterialAdapter translates alphaMode=MASK to BlendMode.Alpha
        //   - AlphaDiscardThreshold is also not set
        assertMaterialParam(unlitMaterial, "AlphaDiscardThreshold", null);
        Assertions.assertEquals(RenderState.BlendMode.Alpha, unlitMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Off, unlitMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(unlitMaterial, "VertexColor", true);
    }

    @Test
    public void testUnlitMaterialWithTextures_LegacyMechanism() {
        GltfModelKey gltfModelKey = new GltfModelKey("gltf/MaterialTestCubes.gltf");
        gltfModelKey.setMaterialAdaptersEnabled(true);
        Node sceneNode = (Node) assetManager.loadModel(gltfModelKey);

        Geometry unlitWithTexturesCube = (Geometry) sceneNode.getChild("unlitWithTexturesCube_0");
        Material unlitMaterial = unlitWithTexturesCube.getMaterial();

        assertMaterialNameAndDefinition(unlitMaterial, "Unshaded", "unlitWithTexturesMaterial");

        assertMaterialParam(unlitMaterial, "Color", ColorRGBA.White);
        assertMaterialParam(unlitMaterial, "ColorMap", "gltf/ColorTexture.png");

        assertMaterialParam(unlitMaterial, "GlowMap", null);
        assertMaterialParam(unlitMaterial, "GlowColor", ColorRGBA.Black);

        assertMaterialParam(unlitMaterial, "AlphaDiscardThreshold", null);
        Assertions.assertEquals(RenderState.BlendMode.Alpha, unlitMaterial.getAdditionalRenderState().getBlendMode());
        Assertions.assertEquals(RenderState.FaceCullMode.Back, unlitMaterial.getAdditionalRenderState().getFaceCullMode());

        assertMaterialParam(unlitMaterial, "VertexColor", false);
    }

    private void assertMaterialNameAndDefinition(Material material, String expectedDefinitionName, String expectedMaterialName) {
        Assertions.assertEquals(expectedMaterialName, material.getName(), "Wrong material name.");
        Assertions.assertEquals(expectedDefinitionName, material.getMaterialDef().getName(), "Wrong material definition.");
    }

    private void assertMaterialParam(Material material, String paramName, Object expectedValue) {
        MatParam matParam = material.getParam(paramName);
        if (expectedValue == null) {
            Assertions.assertNull(matParam, () -> "Material parameter '" + paramName + "' should not be set.");
            return;

        } else {
            Assertions.assertNotNull(matParam, () -> "Missing material parameter '" + paramName + "'.");
        }

        if (matParam instanceof MatParamTexture) {
            String imageName = ((MatParamTexture) matParam).getTextureValue().getName();
            Assertions.assertEquals(expectedValue, imageName, () -> "Wrong value of texture parameter '" + paramName + "'.");

        } else {
            Object value = matParam.getValue();
            Assertions.assertEquals(expectedValue, value, () -> "Wrong value of material parameter '" + paramName + "'.");
        }
    }

}
