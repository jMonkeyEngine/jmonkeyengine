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
package com.jme3.material;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.system.NullRenderer;
import com.jme3.system.TestUtil;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Image.Format;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.util.Arrays;
import java.util.EnumSet;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MaterialTest {

    private Material material;
    private final Geometry geometry = new Geometry("Geometry", new Box(1, 1, 1));
    private final EnumSet<Caps> myCaps = EnumSet.noneOf(Caps.class);
    private final RenderManager renderManager = new RenderManager(new NullRenderer() {
        @Override
        public EnumSet<Caps> getCaps() {
            return MaterialTest.this.myCaps;
        }
    });

    @Test(expected = IllegalArgumentException.class)
    public void testSelectNonExistentTechnique() {
        material("Common/MatDefs/Gui/Gui.j3md");
        material.selectTechnique("Doesn't Exist", renderManager);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSelectDefaultTechnique_NoCaps() {
        material("Common/MatDefs/Gui/Gui.j3md");
        material.selectTechnique("Default", renderManager);
    }

    @Test
    public void testSelectDefaultTechnique_GLSL100Cap() {
        supportGlsl(100);
        material("Common/MatDefs/Gui/Gui.j3md");

        material.selectTechnique("Default", renderManager);

        checkRequiredCaps(Caps.GLSL100);
    }

    @Test
    public void testSelectDefaultTechnique_GLSL150Cap() {
        supportGlsl(150);
        material("Common/MatDefs/Gui/Gui.j3md");

        material.selectTechnique("Default", renderManager);

        checkRequiredCaps(Caps.GLSL150);
    }

    @Test
    public void testSelectDefaultTechnique_GLSL120Cap_MultipleLangs() {
        supportGlsl(120);
        material("Common/MatDefs/Misc/Particle.j3md");

        material.selectTechnique("Default", renderManager);

        checkRequiredCaps(Caps.GLSL100, Caps.GLSL120);
    }

    @Test
    public void testSelectDefaultTechnique_GLSL100Cap_MultipleLangs() {
        supportGlsl(100);
        material("Common/MatDefs/Misc/Particle.j3md");

        material.selectTechnique("Default", renderManager);

        checkRequiredCaps(Caps.GLSL100);
    }

    @Test
    public void testSelectNamedTechnique_GLSL150Cap() {
        supportGlsl(150);
        material("Common/MatDefs/Light/Lighting.j3md");

        material.selectTechnique("PostShadow", renderManager);

        checkRequiredCaps(Caps.GLSL150);
    }

    @Test
    public void testForcedColorSpace(){
       
        Image img=new Image(Format.RGBA8,2,2,BufferUtils.createByteBuffer(16),null,ColorSpace.sRGB);
        Image img2=new Image(Format.RGBA8,2,2,BufferUtils.createByteBuffer(16),null,ColorSpace.sRGB);
        Texture2D tx=new Texture2D(img);
        Texture2D tx2=new Texture2D(img2);

        assertTrue(tx2.getImage().getColorSpace()==ColorSpace.sRGB);
        assertTrue(tx2.getImage().getColorSpace()==ColorSpace.sRGB);

        AssetManager assetManager = TestUtil.createAssetManager();
        MaterialDef def=new MaterialDef(assetManager,"test");
        def.addMaterialParamTexture(VarType.Texture2D, "ColorMap",ColorSpace.Linear, null);
        Material mat=new Material(def);
        
        mat.setTexture("ColorMap",tx);          
        assertTrue(tx.getImage().getColorSpace()==ColorSpace.Linear);
        
        mat.setTexture("ColorMap",tx2);  
        assertTrue(tx2.getImage().getColorSpace()==ColorSpace.Linear);       
    
    }

    @Test
    public void testSelectNamedTechnique_GLSL100Cap() {
        supportGlsl(100);
        material("Common/MatDefs/Light/Lighting.j3md");

        material.selectTechnique("PostShadow", renderManager);

        checkRequiredCaps(Caps.GLSL100);
    }

    private void checkRequiredCaps(Caps... caps) {
        EnumSet<Caps> expected = EnumSet.noneOf(Caps.class);
        expected.addAll(Arrays.asList(caps));

        Technique tech = material.getActiveTechnique();

        assertEquals(expected, tech.getDef().getRequiredCaps());
    }

    private void supportGlsl(int version) {
        switch (version) {
            case 150:
                myCaps.add(Caps.GLSL150);
            case 140:
                myCaps.add(Caps.GLSL140);
            case 130:
                myCaps.add(Caps.GLSL130);
            case 120:
                myCaps.add(Caps.GLSL120);
            case 110:
                myCaps.add(Caps.GLSL110);
            case 100:
                myCaps.add(Caps.GLSL100);
                break;
        }
    }

    private void material(String path) {
        AssetManager assetManager = TestUtil.createAssetManager();
        material = new Material(assetManager, path);
        geometry.setMaterial(material);
    }

}
