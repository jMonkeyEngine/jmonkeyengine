/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.material.plugin.export.material;

import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/**
 * The `J3MRootOutputCapsule` class extends `J3MOutputCapsule` and serves as the
 * root output capsule for exporting jME materials (`.j3m` files).
 *
 * @author tsr
 */
public class J3MRootOutputCapsule extends J3MOutputCapsule {

    /**
     * Stores a map of `Savable` objects to their corresponding `J3MOutputCapsule` instances.
     * This allows for managing and exporting different components (e.g., render states)
     * of a material.
     */
    private final HashMap<Savable, J3MOutputCapsule> outCapsules = new HashMap<>();
    // The name of the material.
    private String name;
    // The material definition string (e.g., "Common/MatDefs/Light.j3md").
    private String materialDef;
    // Indicates whether the material is transparent
    private Boolean isTransparent;
    // Indicates whether the material receives shadows
    private Boolean receivesShadows;

    /**
     * Constructs a new `J3MRootOutputCapsule`.
     *
     * @param exporter The `J3MExporter` instance used for exporting savable objects.
     */
    public J3MRootOutputCapsule(J3MExporter exporter) {
        super(exporter);
    }

    /**
     * Clears all data within this capsule and its superclass.
     * Resets material properties to their default or null values and clears
     * the map of savable capsules.
     */
    @Override
    public void clear() {
        super.clear();
        isTransparent = null;
        receivesShadows = null;
        name = "";
        materialDef = "";
        outCapsules.clear();
    }

    /**
     * Retrieves an `OutputCapsule` for a given `Savable` object.
     * If a capsule for the object does not exist, a new `J3MRenderStateOutputCapsule`
     * is created and associated with the object.
     *
     * @param object The `Savable` object for which to retrieve or create a capsule.
     * @return The `OutputCapsule` associated with the given savable object.
     */
    public OutputCapsule getCapsule(Savable object) {
        if (!outCapsules.containsKey(object)) {
            outCapsules.put(object, new J3MRenderStateOutputCapsule(exporter));
        }
        return outCapsules.get(object);
    }

    @Override
    public void writeToStream(Writer out) throws IOException {
        out.write("Material " + name + " : " + materialDef + " {\n\n");

        if (isTransparent != null)
            out.write("    Transparent " + (isTransparent ? "On" : "Off") + "\n\n");
        if (receivesShadows != null)
            out.write("    ReceivesShadows " + (receivesShadows ? "On" : "Off") + "\n\n");

        out.write("    MaterialParameters {\n");
        super.writeToStream(out); // Writes parameters from the superclass
        out.write("    }\n\n");

        // Write out encapsulated savable object data
        for (J3MOutputCapsule c : outCapsules.values()) {
            c.writeToStream(out);
        }
        out.write("}\n");
    }

    @Override
    public void write(String value, String name, String defVal) throws IOException {
        switch (name) {
            case "material_def":
                materialDef = value;
                break;
            case "name":
                this.name = value;
                break;
            default:
                throw new UnsupportedOperationException(name + " string material parameter not supported yet");
        }
    }

    @Override
    public void write(boolean value, String name, boolean defVal) throws IOException {
        // No need to write if the value is the same as the default.
        if (value == defVal) {
            return;
        }

        switch (name) {
            case "is_transparent":
                isTransparent = value;
                break;
            case "receives_shadows":
                receivesShadows = value;
                break;
            default:
                throw new UnsupportedOperationException(name + " boolean material parameter not supported yet");
        }
    }

    @Override
    public void write(Savable object, String name, Savable defVal) throws IOException {
        if (object != null && !object.equals(defVal)) {
            object.write(exporter);
        }
    }

}
