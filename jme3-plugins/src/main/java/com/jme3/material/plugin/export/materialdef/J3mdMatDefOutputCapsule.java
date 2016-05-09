/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.materialdef;

import com.jme3.export.*;
import com.jme3.material.*;

import java.io.*;
import java.util.*;

/**
 * @author nehon
 */
public class J3mdMatDefOutputCapsule extends J3mdOutputCapsuleAdapter {

    //private final HashMap<Savable, J3mdOutputCapsuleAdapter> outCapsules;
    private String name;
    private List<J3mdOutputCapsuleAdapter> matParams = new ArrayList<J3mdOutputCapsuleAdapter>();
    boolean textureParam= false;


    public J3mdMatDefOutputCapsule(J3mdExporter exporter) {
        super(exporter);
    }

    @Override
    public void clear() {
        super.clear();
        name = "";
        matParams.clear();
    }

    public OutputCapsule getCapsule(Savable object) {
        if(object instanceof MatParam){
            if(object instanceof MatParamTexture && textureParam){
                textureParam = false;
                return matParams.get(matParams.size() - 1);
            }
            if( object instanceof MatParamTexture){
                textureParam = true;
            }
            J3mdMatParamOutputCapsule matParamCapsule = new J3mdMatParamOutputCapsule(exporter);
            matParams.add(matParamCapsule);
            return matParamCapsule;
        }

        throw new IllegalArgumentException("Unsupported type : " + object.getClass().getName());

    }

    @Override
    public void writeToStream(OutputStreamWriter out) throws IOException {
        out.write("MaterialDef " + name + " {\n");

        out.write("    MaterialParameters {\n");
        for (J3mdOutputCapsuleAdapter matParam : matParams) {
            matParam.writeToStream(out);
        }
        out.write("    }\n\n");
//
//        for (J3mdOutputCapsule c : outCapsules.values()) {
//            c.writeToStream(out);
//        }
        out.write("}\n");
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
        object.write(exporter);
    }

    @Override
    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal) throws IOException {
        switch (name) {
            case "matParams":
                for (Savable savable : map.values()) {
                    savable.write(exporter);
                }
                break;
            case "techniques":
                //nothing for now
                break;
            default:
                throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }
}
