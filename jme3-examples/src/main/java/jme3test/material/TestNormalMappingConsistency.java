/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.system.AppSettings;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * This test cycles through a model exported in different formats and with different materials with tangents
 * generated in different ways. The normal map should look correct in all cases. Refer to
 * https://github.com/KhronosGroup/glTF-Sample-Models/tree/master/2.0/NormalTangentMirrorTest for details on
 * the correct result and debugging.
 */
public class TestNormalMappingConsistency extends SimpleApplication {
    Node probeNode;
    DirectionalLight light;
    BitmapText materialTxt;
    BitmapText modelTxt;
    boolean flipTextures = false;
    float t = -1;
    int modelType = 0;
    int materialType = 0;
    Spatial loadedSpatial;

    final int maxModels = 4;
    final int maxMaterials = 3;

    public static void main(String[] args) {
        AppSettings sett = new AppSettings(true);
        sett.setWidth(1024);
        sett.setHeight(768);
        TestNormalMappingConsistency app = new TestNormalMappingConsistency();
        app.setSettings(sett);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setPauseOnLostFocus(false);
        flyCam.setMoveSpeed(20);
        viewPort.setBackgroundColor(new ColorRGBA().setAsSrgb(0.2f, 0.2f, 0.2f, 1.0f));
        probeNode = (Node) assetManager.loadModel("Scenes/defaultProbe.j3o");
        rootNode.attachChild(probeNode);

        probeNode.addLight(new AmbientLight(ColorRGBA.Gray));
        light = new DirectionalLight(new Vector3f(-1, -1, -1), ColorRGBA.White);
        rootNode.addLight(light);

        modelTxt = new BitmapText(guiFont);
        modelTxt.setSize(guiFont.getCharSet().getRenderedSize());
        modelTxt.setLocalTranslation(0, 700, 0);
        guiNode.attachChild(modelTxt);

        materialTxt = new BitmapText(guiFont);
        materialTxt.setSize(guiFont.getCharSet().getRenderedSize());
        materialTxt.setLocalTranslation(300, 700, 0);
        guiNode.attachChild(materialTxt);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (t == -1 || t > 5) {
            t = 0;
            loadModel(new Vector3f(0, 0, 0), 3, modelType, materialType);
            materialType++;
            if (materialType >= maxMaterials) {
                materialType = 0;
                modelType++;
                if (modelType >= maxModels) {
                    modelType = 0;
                }
            }
        }
        t += tpf;

    }

    private void loadModel(Vector3f offset, float scale, int modelType, int materialType) {
        if (loadedSpatial != null) {
            loadedSpatial.removeFromParent();
        }


        if (modelType == 0) loadedSpatial = loadGltf();
        else if (modelType == 1) loadedSpatial = loadGltfGen();
        else if (modelType == 2) loadedSpatial = loadOgre();
        else if (modelType == 3) loadedSpatial = loadOgreGen();

        loadedSpatial.scale(scale);
        loadedSpatial.move(offset);
        if (materialType == 0) loadedSpatial.setMaterial(createPBRLightingMat());
        else if (materialType == 1) loadedSpatial.setMaterial(createSPLightingMat());
        else if (materialType == 2) loadedSpatial.setMaterial(createLightingMat());
        probeNode.attachChild(loadedSpatial);
    }

    private Spatial loadGltf() {
        GltfModelKey k = new GltfModelKey("jme3test/normalmapCompare/NormalTangentMirrorTest.gltf");
        Spatial sp = assetManager.loadModel(k);
        modelTxt.setText("GLTF");
        return sp;
    }

    private Spatial loadGltfGen() {
        GltfModelKey k = new GltfModelKey("jme3test/normalmapCompare/NormalTangentMirrorTest.gltf");
        Spatial sp = assetManager.loadModel(k);
        MikktspaceTangentGenerator.generate(loadedSpatial);
        modelTxt.setText("GLTF  - regen tg");
        return sp;
    }

    private Spatial loadOgre() {
        GltfModelKey k = new GltfModelKey("jme3test/normalmapCompare/ogre/NormalTangentMirrorTest.scene");
        Spatial sp = assetManager.loadModel(k);
        modelTxt.setText("OGRE");
        return sp;
    }

    private Spatial loadOgreGen() {
        GltfModelKey k = new GltfModelKey("jme3test/normalmapCompare/ogre/NormalTangentMirrorTest.scene");
        Spatial sp = assetManager.loadModel(k);
        MikktspaceTangentGenerator.generate(loadedSpatial);
        modelTxt.setText("OGRE  - regen tg");
        return sp;
    }

    private Material createPBRLightingMat() {
        renderManager.setPreferredLightMode(LightMode.SinglePassAndImageBased);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setTexture("BaseColorMap", assetManager.loadTexture(new TextureKey(
                "jme3test/normalmapCompare/NormalTangentMirrorTest_BaseColor.png", flipTextures)));
        mat.setTexture("NormalMap", assetManager.loadTexture(
                new TextureKey("jme3test/normalmapCompare/NormalTangentTest_Normal.png", flipTextures)));
        mat.setFloat("NormalType", 1);
        materialTxt.setText("PBR Lighting");
        return mat;
    }

    private Material createSPLightingMat() {
        renderManager.setPreferredLightMode(LightMode.SinglePass);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", assetManager.loadTexture(new TextureKey(
                "jme3test/normalmapCompare/NormalTangentMirrorTest_BaseColor.png", flipTextures)));
        mat.setTexture("NormalMap", assetManager.loadTexture(
                new TextureKey("jme3test/normalmapCompare/NormalTangentTest_Normal.png", flipTextures)));
        mat.setFloat("NormalType", 1);
        materialTxt.setText("SP Lighting");
        return mat;
    }

    private Material createLightingMat() {
        renderManager.setPreferredLightMode(LightMode.MultiPass);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("DiffuseMap", assetManager.loadTexture(new TextureKey(
                "jme3test/normalmapCompare/NormalTangentMirrorTest_BaseColor.png", flipTextures)));
        mat.setTexture("NormalMap", assetManager.loadTexture(
                new TextureKey("jme3test/normalmapCompare/NormalTangentTest_Normal.png", flipTextures)));
        materialTxt.setText("Lighting");
        mat.setFloat("NormalType", 1);

        return mat;
    }

}
