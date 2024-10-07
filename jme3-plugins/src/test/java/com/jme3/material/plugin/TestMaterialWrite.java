/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.material.plugin;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.ColorRGBA;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Texture;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class TestMaterialWrite {

    private AssetManager assetManager;

    @Before
    public void init() {
        assetManager = JmeSystem.newAssetManager(
                TestMaterialWrite.class.getResource("/com/jme3/asset/Desktop.cfg"));


    }


    @Test
    public void testWriteMat() throws Exception {

        Material mat = new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");

        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", ColorRGBA.White);
        mat.setColor("Ambient", ColorRGBA.DarkGray);
        mat.setFloat("AlphaDiscardThreshold", 0.5f);

        mat.setFloat("Shininess", 2.5f);

        Texture tex = assetManager.loadTexture(new TextureKey("Common/Textures/MissingTexture.png", true));
        tex.setMagFilter(Texture.MagFilter.Nearest);
        tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        tex.setWrap(Texture.WrapAxis.S, Texture.WrapMode.Repeat);
        tex.setWrap(Texture.WrapAxis.T, Texture.WrapMode.MirroredRepeat);

        mat.setTexture("DiffuseMap", tex);
        mat.getAdditionalRenderState().setDepthWrite(false);
        mat.getAdditionalRenderState().setDepthTest(false);
        mat.getAdditionalRenderState().setLineWidth(5);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        J3MExporter exporter = new J3MExporter();
        try {
            exporter.save(mat, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.err.println(stream.toString());

        J3MLoader loader = new J3MLoader();
        AssetInfo info = new AssetInfo(assetManager, new AssetKey("test")) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(stream.toByteArray());
            }
        };
        Material mat2 = (Material)loader.load(info);

        assertTrue(mat.contentEquals(mat2));
    }

}
