/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.materialdef;

import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.texture.image.ColorSpace;

import java.io.*;
import java.util.*;

import static com.jme3.shader.VarType.Vector2;
import static com.jme3.shader.VarType.Vector3;
import static com.jme3.shader.VarType.Vector4;

/**
 * @author nehon
 */
public class J3mdMatParamOutputCapsule extends J3mdOutputCapsuleAdapter {

    private String name;
    private String varType;
    private String value;
    private String space;



    public J3mdMatParamOutputCapsule(J3mdExporter exporter) {
        super(exporter);
    }

    @Override
    public void clear() {
        super.clear();
        name = "";
        varType = "";
        value = "";
        space = "";
    }

    @Override
    public void writeToStream(OutputStreamWriter out) throws IOException {
        out.write("        ");
        out.write(varType);
        out.write(" ");
        out.write(name);
        if(space != null) {
            out.write(" ");
            out.write(space);
        }
        if(value != null){
            out.write(" : ");
            out.write(value);
        }
        out.write("\n");
    }

    @Override
    public void write(String value, String name, String defVal) throws IOException {
        switch (name) {
            case "name":
                this.name = value;
                break;
            default:
                throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }

    @Override
    public void write(Savable object, String name, Savable defVal) throws IOException {


        if(name.equals("texture")){
            return;
        }

        if(name.equals("value_savable")){
            if(object instanceof Vector2f) {
                Vector2f v = (Vector2f)object;
                this.value = v.getX() + " " + v.getY();
            } else if (object instanceof Vector3f) {
                Vector3f v = (Vector3f)object;
                this.value = v.getX() + " " + v.getY()+ " " + v.getZ();
            } else if (object instanceof Vector4f ) {
                Vector4f v = (Vector4f)object;
                this.value = v.getX() + " " + v.getY()+ " " + v.getZ()+ " " + v.getW();
            } else if (object instanceof ColorRGBA) {
                ColorRGBA v = (ColorRGBA)object;
                this.value = v.getRed() + " " + v.getGreen() + " " + v.getBlue() + " " + v.getAlpha();
            } else {
                throw new UnsupportedOperationException(object.getClass() + " Unsupported type");
            }

        } else {
            throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }


    @Override
    public void write(Enum value, String name, Enum defVal) throws IOException {


        switch (name) {
            case "varType":
                this.varType = value.name();
                break;
            case "colorSpace":
                space = value == ColorSpace.Linear?"-LINEAR":null;
                break;
            default:
                throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }

    }

    @Override
    public void write(boolean value, String name, boolean defVal) throws IOException {
        if(name.equals("value_bool")){
            this.value = Boolean.toString(value);
        } else {
            throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }

    @Override
    public void write(float value, String name, float defVal) throws IOException {
        if(name.equals("value_float")){
            this.value = Float.toString(value);
        } else {
            throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }

    @Override
    public void write(int value, String name, int defVal) throws IOException {
        if(name.equals("texture_unit")){
            return;
        }
        if(name.equals("value_int")){
            this.value = Integer.toString(value);
        } else {
            throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }
}


