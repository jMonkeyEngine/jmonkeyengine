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
package com.jme3.material.plugin.export.materialdef;

import com.jme3.material.*;
import com.jme3.math.*;
import static com.jme3.shader.VarType.Vector2;
import static com.jme3.shader.VarType.Vector3;
import static com.jme3.shader.VarType.Vector4;
import com.jme3.texture.image.ColorSpace;
import java.io.*;

/**
 * @author nehon
 */
public class J3mdMatParamWriter {

    public J3mdMatParamWriter() {
    }

    public void write(MatParam param, Writer out) throws IOException {
        out.write("        ");
        out.write(param.getVarType().name());
        out.write(" ");
        out.write(param.getName());
        if (param instanceof MatParamTexture) {
            MatParamTexture paramTex = (MatParamTexture) param;
            String space = paramTex.getColorSpace() == ColorSpace.Linear ? "-LINEAR" : null;
            if (space != null) {
                out.write(" ");
                out.write(space);
            }
        }
        String value = formatValue(param);
        if (value != null) {
            out.write(" : ");
            out.write(value);
        }
        out.write("\n");
    }

    private String formatValue(MatParam param) {
        Object value = param.getValue();
        if (value == null) {
            return null;
        }

        switch (param.getVarType()) {
            case Vector2:
                Vector2f v2 = (Vector2f) value;
                return v2.getX() + " " + v2.getY();
            case Vector3:
                Vector3f v3 = (Vector3f) value;
                return v3.getX() + " " + v3.getY() + " " + v3.getZ();
            case Vector4:
                if (value instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) value;
                    return c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " " + c.getAlpha();
                } else {
                    Vector4f v4 = (Vector4f) value;
                    return v4.getX() + " " + v4.getY() + " " + v4.getZ() + " " + v4.getW();
                }
            default:
                return value.toString();

        }
    }
}


