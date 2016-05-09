/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.material.plugin.export.materialdef;

import com.jme3.export.JmeExporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.*;

import java.io.*;

/**
 * Saves a Material to a j3m file with proper formatting.
 *
 * usage is :
 * <pre>
 *     J3mdExporter exporter = new J3mdExporter();
 *     exporter.save(material, myFile);
 *     //or
 *     exporter.save(material, myOutputStream);
 * </pre>
 *
 * @author tsr
 * @author nehon (documentation and safety check)
 */
public class J3mdExporter implements JmeExporter {

    private final J3mdMatDefOutputCapsule rootCapsule;

    /**
     * Create a J3mdExporter
     */
    public J3mdExporter() {
        rootCapsule = new J3mdMatDefOutputCapsule(this);
    }

    @Override
    public void save(Savable object, OutputStream f) throws IOException {

        if (!(object instanceof MaterialDef)) {
            throw new IllegalArgumentException("J3mdExporter can only save com.jme3.material.MaterialDef class");
        }

        OutputStreamWriter out = new OutputStreamWriter(f);

        rootCapsule.clear();
        object.write(this);
        rootCapsule.writeToStream(out);

        out.flush();
    }

    @Override
    public void save(Savable object, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f)) {
            save(object, fos);
        }
    }

    @Override
    public OutputCapsule getCapsule(Savable object) {
        if (object instanceof MaterialDef) {
            return rootCapsule;
        }

        return rootCapsule.getCapsule(object);
    }

}
