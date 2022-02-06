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
package com.jme3.shader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.system.TestUtil;

import org.junit.Test;

import jme3tools.shader.Preprocessor;


public class GLSLPreprocessorTest {

    String readAllAsString(InputStream is) throws Exception{
        String output = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (true) {
            String l = reader.readLine();
            if (l == null) break;
            if (output != "") output += "\n";
            output += l;
        }
        reader.close();
        return output;
    }
    
    @Test
    public void testFOR() throws Exception{
        String source = "#for i=0..2 (#ifdef IS_SET$i $0 #endif)\n" +
                "  uniform float m_Something$i;\n" +
                "#endfor";
        String processedSource= readAllAsString(Preprocessor.apply(new ByteArrayInputStream(source.getBytes("UTF-8"))));

        AssetInfo testData = TestUtil.createAssetManager().locateAsset(new AssetKey("GLSLPreprocessorTest.testFOR.validOutput"));
        assertNotNull(testData);
        String sourceCheck=readAllAsString(testData.openStream());
        assertEquals(sourceCheck, processedSource);                  
    }
}
