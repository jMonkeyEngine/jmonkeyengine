/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.renderer;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.TestUtil;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class OpaqueComparatorTest {
    
    private final Mesh mesh = new Box(1,1,1);
    private Camera cam = new Camera(1, 1);
    private RenderManager renderManager;
    private AssetManager assetManager;
    private OpaqueComparator comparator = new OpaqueComparator();
    
    @Before
    public void setUp() {
        assetManager = TestUtil.createAssetManager();
        renderManager = TestUtil.createRenderManager();
        comparator.setCamera(cam);
    }
    
    /**
     * Given a correctly sorted list of materials, check if the 
     * opaque comparator can sort a reversed list of them.
     * 
     * Each material will be cloned so that none of them will be equal to each other
     * in reference, forcing the comparator to compare the material sort ID.
     * 
     * E.g. for a list of materials A, B, C, the following list will be generated:
     * <pre>C, B, A, C, B, A, C, B, A</pre>, it should result in
     * <pre>A, A, A, B, B, B, C, C, C</pre>.
     * 
     * @param materials The pre-sorted list of materials to check sorting for.
     */
    private void testSort(Material ... materials) {
        GeometryList gl = new GeometryList(comparator);
        for (int g = 0; g < 5; g++) {
            for (int i = materials.length - 1; i >= 0; i--) {
                Geometry geom = new Geometry("geom", mesh);
                Material clonedMaterial = materials[i].clone();
                
                if (materials[i].getActiveTechnique() != null) {
                    String techniqueName = materials[i].getActiveTechnique().getDef().getName();
                    clonedMaterial.selectTechnique(techniqueName, renderManager);
                } else {
                    clonedMaterial.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
                }
                
                geom.setMaterial(clonedMaterial);
                gl.add(geom);
            }
        }
        gl.sort();
        
        for (int i = 0; i < gl.size(); i++) {
            Material mat = gl.get(i).getMaterial();
            String sortId = Integer.toHexString(mat.getSortId()).toUpperCase();
            System.out.print(sortId + "\t");
            System.out.println(mat);
        }
        
        Set<String> alreadySeen = new HashSet<String>();
        Material current = null;
        for (int i = 0; i < gl.size(); i++) {
            Material mat = gl.get(i).getMaterial();
            if (current == null) {
                current = mat;
            } else if (!current.getName().equals(mat.getName())) {
                assert !alreadySeen.contains(mat.getName());
                alreadySeen.add(current.getName());
                current = mat;
            }
        }
        
        for (int i = 0; i < materials.length; i++) {
            for (int g = 0; g < 5; g++) {
                int index = i * 5 + g;
                Material mat1 = gl.get(index).getMaterial();
                Material mat2 = materials[i];
                assert mat1.getName().equals(mat2.getName()) : 
                       mat1.getName() + " != " + mat2.getName() + " for index " + index;
            }
        }
    }
    
    @Test
    public void testSortByMaterialDef() {
        Material lightingMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material particleMat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        Material unshadedMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Material skyMat      = new Material(assetManager, "Common/MatDefs/Misc/Sky.j3md");
        
        lightingMat.setName("MatLight");
        particleMat.setName("MatParticle");
        unshadedMat.setName("MatUnshaded");
        skyMat.setName("MatSky");
        testSort(skyMat, lightingMat, particleMat, unshadedMat);
    }
    
    @Test
    public void testSortByTechnique() {
        Material lightingMatDefault = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingPreShadow = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingPostShadow = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatPreNormalPass = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatGBuf = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatGlow = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        
        lightingMatDefault.setName("TechDefault");
        lightingMatDefault.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
        
        lightingPostShadow.setName("TechPostShad");
        lightingPostShadow.selectTechnique("PostShadow", renderManager);
        
        lightingPreShadow.setName("TechPreShad");
        lightingPreShadow.selectTechnique("PreShadow", renderManager);
        
        lightingMatPreNormalPass.setName("TechNorm");
        lightingMatPreNormalPass.selectTechnique("PreNormalPass", renderManager);
        
        lightingMatGlow.setName("TechGlow");
        lightingMatGlow.selectTechnique("Glow", renderManager);
        
        testSort(lightingMatGlow, lightingPreShadow, lightingMatPreNormalPass,
                lightingMatDefault, lightingPostShadow);
    }
    
    @Test(expected = AssertionError.class)
    public void testNoSortByParam() {
        Material sameMat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Material sameMat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        sameMat1.setName("MatRed");
        sameMat1.setColor("Color", ColorRGBA.Red);
        
        sameMat2.setName("MatBlue");
        sameMat2.setColor("Color", ColorRGBA.Blue);
        
        testSort(sameMat1, sameMat2);
    }
    
    private Texture createTexture(String name) {
        ByteBuffer bb = BufferUtils.createByteBuffer(3);
        Image image = new Image(Format.RGB8, 1, 1, bb, ColorSpace.sRGB);
        Texture2D texture = new Texture2D(image);
        texture.setName(name);
        return texture;
    }
    
    @Test
    public void testSortByTexture() {
        Material texture1Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Material texture2Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Material texture3Mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        Texture tex1 = createTexture("A");
        tex1.getImage().setId(1);
        
        Texture tex2 = createTexture("B");
        tex2.getImage().setId(2);
        
        Texture tex3 = createTexture("C");
        tex3.getImage().setId(3);
        
        texture1Mat.setName("TexA");
        texture1Mat.setTexture("ColorMap", tex1);
        
        texture2Mat.setName("TexB");
        texture2Mat.setTexture("ColorMap", tex2);
        
        texture3Mat.setName("TexC");
        texture3Mat.setTexture("ColorMap", tex3);
        
        testSort(texture1Mat, texture2Mat, texture3Mat);
    }
    
    @Test
    public void testSortByShaderDefines() {
        Material lightingMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatVColor = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatVLight = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatTC = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatVColorLight = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material lightingMatTCVColorLight = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        
        lightingMat.setName("DefNone");
        
        lightingMatVColor.setName("DefVC");
        lightingMatVColor.setBoolean("UseVertexColor", true);
        
        lightingMatVLight.setName("DefVL");
        lightingMatVLight.setBoolean("VertexLighting", true);
        
        lightingMatTC.setName("DefTC");
        lightingMatTC.setBoolean("SeparateTexCoord", true);
        
        lightingMatVColorLight.setName("DefVCVL");
        lightingMatVColorLight.setBoolean("UseVertexColor", true);
        lightingMatVColorLight.setBoolean("VertexLighting", true);
        
        lightingMatTCVColorLight.setName("DefVCVLTC");
        lightingMatTCVColorLight.setBoolean("UseVertexColor", true);
        lightingMatTCVColorLight.setBoolean("VertexLighting", true);
        lightingMatTCVColorLight.setBoolean("SeparateTexCoord", true);
        
        testSort(lightingMat, lightingMatVColor, lightingMatVLight,
                 lightingMatVColorLight, lightingMatTC, lightingMatTCVColorLight);
    }
    
    @Test
    public void testSortByAll() {
        Material matBase1 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Material matBase2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
        Texture texBase = createTexture("BASE");
        texBase.getImage().setId(1);
        Texture tex1 = createTexture("1");
        tex1.getImage().setId(2);
        Texture tex2 = createTexture("2");
        tex2.getImage().setId(3);
        
        matBase1.setName("BASE");
        matBase1.selectTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, renderManager);
        matBase1.setBoolean("UseVertexColor", true);
        matBase1.setTexture("DiffuseMap", texBase);
        
        Material mat1100 = matBase1.clone();
        mat1100.setName("1100");
        mat1100.selectTechnique("PreShadow", renderManager);
        
        Material mat1101 = matBase1.clone();
        mat1101.setName("1101");
        mat1101.selectTechnique("PreShadow", renderManager);
        mat1101.setTexture("DiffuseMap", tex1);
        
        Material mat1102 = matBase1.clone();
        mat1102.setName("1102");
        mat1102.selectTechnique("PreShadow", renderManager);
        mat1102.setTexture("DiffuseMap", tex2);
        
        Material mat1110 = matBase1.clone();
        mat1110.setName("1110");
        mat1110.selectTechnique("PreShadow", renderManager);
        mat1110.setFloat("AlphaDiscardThreshold", 2f);
        
        Material mat1120 = matBase1.clone();
        mat1120.setName("1120");
        mat1120.selectTechnique("PreShadow", renderManager);
        mat1120.setBoolean("UseInstancing", true);
        
        Material mat1121 = matBase1.clone();
        mat1121.setName("1121");
        mat1121.selectTechnique("PreShadow", renderManager);
        mat1121.setBoolean("UseInstancing", true);
        mat1121.setTexture("DiffuseMap", tex1);
        
        Material mat1122 = matBase1.clone();
        mat1122.setName("1122");
        mat1122.selectTechnique("PreShadow", renderManager);
        mat1122.setBoolean("UseInstancing", true);
        mat1122.setTexture("DiffuseMap", tex2);
        
        Material mat1140 = matBase1.clone();
        mat1140.setName("1140");
        mat1140.selectTechnique("PreShadow", renderManager);
        mat1140.setFloat("AlphaDiscardThreshold", 2f);
        mat1140.setBoolean("UseInstancing", true);
        
        Material mat1200 = matBase1.clone();
        mat1200.setName("1200");
        mat1200.selectTechnique("PostShadow", renderManager);
        
        Material mat1210 = matBase1.clone();
        mat1210.setName("1210");
        mat1210.selectTechnique("PostShadow", renderManager);
        mat1210.setFloat("AlphaDiscardThreshold", 2f);
        
        Material mat1220 = matBase1.clone();
        mat1220.setName("1220");
        mat1220.selectTechnique("PostShadow", renderManager);
        mat1220.setBoolean("UseInstancing", true);
        
        Material mat2000 = matBase2.clone();
        mat2000.setName("2000");
        
        testSort(mat1100, mat1101, mat1102, mat1110, 
                 mat1120, mat1121, mat1122, mat1140, 
                 mat1200, mat1210, mat1220, mat2000);
    }
}
