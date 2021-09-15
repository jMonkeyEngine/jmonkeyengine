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
package com.jme3.scene;

import com.jme3.material.MatParamOverride;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture2D;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class MPOTestUtils {

    private static final Camera DUMMY_CAM = new Camera(640, 480);

    private static final SceneGraphVisitor VISITOR = new SceneGraphVisitor() {
        @Override
        public void visit(Spatial spatial) {
            validateSubScene(spatial);
        }
    };

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MPOTestUtils() {
    }

    private static void validateSubScene(Spatial scene) {
        scene.checkCulling(DUMMY_CAM);

        Set<MatParamOverride> actualOverrides = new HashSet<>();
        for (MatParamOverride override : scene.getWorldMatParamOverrides()) {
            actualOverrides.add(override);
        }

        Set<MatParamOverride> expectedOverrides = new HashSet<>();
        Spatial current = scene;
        while (current != null) {
            for (MatParamOverride override : current.getLocalMatParamOverrides()) {
                expectedOverrides.add(override);
            }
            current = current.getParent();
        }

        assertEquals("For " + scene, expectedOverrides, actualOverrides);
    }
    
    public static void validateScene(Spatial scene) {
        scene.updateGeometricState();
        scene.depthFirstTraversal(VISITOR);
    }

    public static MatParamOverride mpoInt(String name, int value) {
        return new MatParamOverride(VarType.Int, name, value);
    }

    public static MatParamOverride mpoBool(String name, boolean value) {
        return new MatParamOverride(VarType.Boolean, name, value);
    }

    public static MatParamOverride mpoFloat(String name, float value) {
        return new MatParamOverride(VarType.Float, name, value);
    }

    public static MatParamOverride mpoMatrix4Array(String name, Matrix4f[] value) {
        return new MatParamOverride(VarType.Matrix4Array, name, value);
    }

    public static MatParamOverride mpoTexture2D(String name, Texture2D texture) {
        return new MatParamOverride(VarType.Texture2D, name, texture);
    }

    private static int getRefreshFlags(Spatial scene) {
        try {
            Field refreshFlagsField = Spatial.class.getDeclaredField("refreshFlags");
            refreshFlagsField.setAccessible(true);
            return (Integer) refreshFlagsField.get(scene);
        } catch (NoSuchFieldException
                | SecurityException
                | IllegalArgumentException
                | IllegalAccessException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void dumpSceneRF(Spatial scene, String indent, boolean last, int refreshFlagsMask) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent);
        if (last) {
            if (!indent.isEmpty()) {
                sb.append("└─");
            } else {
                sb.append("  ");
            }
            indent += "  ";
        } else {
            sb.append("├─");
            indent += "│ ";
        }
        sb.append(scene.getName());
        int rf = getRefreshFlags(scene) & refreshFlagsMask;
        if (rf != 0) {
            sb.append("(");
            if ((rf & 0x1) != 0) {
                sb.append("T");
            }
            if ((rf & 0x2) != 0) {
                sb.append("B");
            }
            if ((rf & 0x4) != 0) {
                sb.append("L");
            }
            if ((rf & 0x8) != 0) {
                sb.append("l");
            }
            if ((rf & 0x10) != 0) {
                sb.append("O");
            }
            sb.append(")");
        }

        if (!scene.getLocalMatParamOverrides().isEmpty()) {
            sb.append(" [MPO]");
        }

        System.out.println(sb);

        if (scene instanceof Node) {
            Node node = (Node) scene;
            int childIndex = 0;
            for (Spatial child : node.getChildren()) {
                boolean childLast = childIndex == node.getQuantity() - 1;
                dumpSceneRF(child, indent, childLast, refreshFlagsMask);
                childIndex++;
            }
        }
    }

    public static void dumpSceneRF(Spatial scene, int refreshFlagsMask) {
        dumpSceneRF(scene, "", true, refreshFlagsMask);
    }
}
