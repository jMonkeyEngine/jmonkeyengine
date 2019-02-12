/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.materialdef;

import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.texture.image.ColorSpace;

import java.io.*;

import static com.jme3.shader.VarType.Vector2;
import static com.jme3.shader.VarType.Vector3;
import static com.jme3.shader.VarType.Vector4;

/**
 * @author nehon
 */
public class J3mdMatParamWriter {

    public J3mdMatParamWriter() {
    }

    public void write(MatParam param, OutputStreamWriter out) throws IOException {
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


