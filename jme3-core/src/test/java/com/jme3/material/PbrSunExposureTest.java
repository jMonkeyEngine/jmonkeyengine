/*
 * Copyright (c) 2026 jMonkeyEngine
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
import com.jme3.system.TestUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the material-to-shader contracts for PBR sunlight exposure inputs. */
public class PbrSunExposureTest {

    private final AssetManager assets = TestUtil.createAssetManager();
    private final Material material = new Material(assets, "Common/MatDefs/Light/PBRLighting.j3md");
    private final String fragment = (String) assets.loadAsset("Common/MatDefs/Light/PBRLighting.frag");

    @Test
    public void staticIntensityUsesTheMaterialDefine() {
        for (TechniqueDef technique : material.getMaterialDef().getTechniqueDefs("Default")) {
            String define = technique.getShaderParamDefine("StaticSunIntensity");
            assertGuarded(fragment, define, "uniform float m_StaticSunIntensity;");
            assertGuarded(fragment, define, "surface.exposure *= m_StaticSunIntensity;");
        }
    }

    @Test
    public void vertexIntensityUsesTheSameDefineInBothStages() {
        String vertex = (String) assets.loadAsset("Common/MatDefs/Light/PBRLighting.vert");
        for (TechniqueDef technique : material.getMaterialDef().getTechniqueDefs("Default")) {
            String define = technique.getShaderParamDefine("UseVertexColorsAsSunIntensity");
            assertGuarded(vertex, define, "varying vec4 vertColors;");
            assertGuarded(vertex, define, "vertColors = inColor;");
            assertGuarded(fragment, define, "varying vec4 vertColors;");
            assertGuarded(fragment, define, "surface.exposure *= vertColors.r;");
        }
    }

    @Test
    public void adjustedTextureCoordinatesAreDeclaredBeforeExposureSampling() {
        int declaration = fragment.indexOf("vec2 newTexCoord;");
        int sampling = fragment.indexOf("texture2D(m_SunLightExposureMap, newTexCoord)");
        assertTrue(declaration >= 0, "The adjusted texture coordinates must be declared");
        assertTrue(sampling > declaration, "The exposure helper must see the global declaration");
    }

    @Test
    public void existingExposureDefinesRemainSupported() {
        assertGuarded(fragment, "STATIC_SUN_EXPOSURE", "uniform float m_StaticSunIntensity;");
        assertGuarded(fragment, "STATIC_SUN_EXPOSURE", "surface.exposure *= m_StaticSunIntensity;");
        assertGuarded(fragment, "USE_VERTEX_COLORS_AS_SUN_EXPOSURE", "varying vec4 vertColors;");
        assertGuarded(fragment, "USE_VERTEX_COLORS_AS_SUN_EXPOSURE", "surface.exposure *= vertColors.r;");
    }

    private static void assertGuarded(String source, String define, String statement) {
        Pattern name = Pattern.compile("\\b" + Pattern.quote(define) + "\\b");
        Deque<String> conditions = new ArrayDeque<>();
        int found = 0;
        for (String line : source.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#if")) {
                conditions.push(trimmed);
            } else if (trimmed.startsWith("#elif")) {
                conditions.pop();
                conditions.push(trimmed);
            } else if (trimmed.startsWith("#endif")) {
                conditions.pop();
            }
            if (trimmed.contains(statement)) {
                found++;
                boolean guarded = false;
                for (String condition : conditions) {
                    guarded |= name.matcher(condition).find();
                }
                assertTrue(guarded, statement + " must be guarded by " + define);
            }
        }
        assertEquals(1, found, "Expected one shader statement: " + statement);
    }
}
