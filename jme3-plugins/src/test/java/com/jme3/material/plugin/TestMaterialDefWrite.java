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
package com.jme3.material.plugin;

import com.jme3.asset.*;
import com.jme3.material.*;
import com.jme3.material.plugin.export.materialdef.J3mdExporter;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.shader.UniformBinding;
import com.jme3.system.JmeSystem;
import org.junit.*;

import java.io.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestMaterialDefWrite {

    private AssetManager assetManager;

    @Before
    public void init() {
        assetManager = JmeSystem.newAssetManager(
                TestMaterialDefWrite.class.getResource("/com/jme3/asset/Desktop.cfg"));


    }


    @Test
    public void testWriteMat() throws Exception {

        Material mat = new Material(assetManager,"example.j3md");

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        J3mdExporter exporter = new J3mdExporter();
        try {
            exporter.save(mat.getMaterialDef(), stream);
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
        MaterialDef matDef = (MaterialDef)loader.load(info);
        MaterialDef ref = mat.getMaterialDef();

        for (MatParam refParam : ref.getMaterialParams()) {
            MatParam matParam = matDef.getMaterialParam(refParam.getName());
            assertTrue(refParam != null);
            assertEquals(refParam,matParam);
        }

        for (String key : ref.getTechniqueDefsNames()) {
            List<TechniqueDef> refDefs = ref.getTechniqueDefs(key);
            List<TechniqueDef> defs = matDef.getTechniqueDefs(key);

            assertNotNull(defs);
            assertTrue(refDefs.size() == defs.size());
            for (int i = 0; i < refDefs.size(); i++) {
                assertEqualTechniqueDefs(refDefs.get(i), defs.get(i));
            }
        }
    }

    private void assertEqualTechniqueDefs(TechniqueDef def1, TechniqueDef def2){
        assertEquals(def1.getName(), def2.getName());
        assertEquals(def1.getLightMode(), def2.getLightMode());
        assertEquals(def1.getShadowMode(), def2.getShadowMode());
        assertEquals(def1.getShaderProgramNames().size(), def2.getShaderProgramNames().size());

        //World params
        assertEquals(def1.getWorldBindings().size(), def2.getWorldBindings().size());
        for (UniformBinding uniformBinding : def1.getWorldBindings()) {
            assertTrue(def2.getWorldBindings().contains(uniformBinding));
        }

        //defines
        assertEquals(def1.getDefineNames().length, def2.getDefineNames().length);

        //renderState
        assertEquals(def1.getRenderState(), def2.getRenderState());

        //forced renderState
        assertEquals(def1.getForcedRenderState(), def2.getForcedRenderState());

        //no render
        assertEquals(def1.isNoRender(), def2.isNoRender());
    }

}
