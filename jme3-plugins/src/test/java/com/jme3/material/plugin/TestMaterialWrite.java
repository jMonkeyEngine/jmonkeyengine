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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.plugin.export.material.J3MExporter;
import com.jme3.system.JmeSystem;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;

public class TestMaterialWrite {

    private AssetManager assetManager;
    private Material mat;

    @Before
    public void init() {
        assetManager = JmeSystem.newAssetManager(
                TestMaterialWrite.class.getResource("/com/jme3/asset/Desktop.cfg"));

        mat = assetManager.loadMaterial("/testMat.j3m");
    }


    @Test
    public void testWriteMat() {
        assertNotNull(mat);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        J3MExporter exporter = new J3MExporter();
        try {
            exporter.save(mat, stream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String reference = convertStreamToString(TestMaterialWrite.class.getResourceAsStream("/testMat.j3m"));
//        System.err.println(reference);
//        System.err.println(stream.toString());

        assertEquals(reference.replaceAll("[\\s\\r\\n]",""), stream.toString().replaceAll("[\\s\\r\\n]",""));
    }

    private String convertStreamToString(java.io.InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
