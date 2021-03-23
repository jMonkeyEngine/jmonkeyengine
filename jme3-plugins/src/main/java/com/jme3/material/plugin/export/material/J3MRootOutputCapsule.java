package com.jme3.material.plugin.export.material;

import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/**
 * @author tsr
 */
public class J3MRootOutputCapsule extends J3MOutputCapsule {

    private final HashMap<Savable, J3MOutputCapsule> outCapsules;
    private String name;
    private String materialDefinition;
    private Boolean isTransparent;

    public J3MRootOutputCapsule(J3MExporter exporter) {
        super(exporter);
        outCapsules = new HashMap<>();
    }

    @Override
    public void clear() {
        super.clear();
        isTransparent = null;
        name = "";
        materialDefinition = "";
        outCapsules.clear();

    }

    public OutputCapsule getCapsule(Savable object) {
        if (!outCapsules.containsKey(object)) {
            outCapsules.put(object, new J3MRenderStateOutputCapsule(exporter));
        }

        return outCapsules.get(object);
    }

    @Override
    public void writeToStream(Writer out) throws IOException {
        out.write("Material " + name + " : " + materialDefinition + " {\n\n");
        if (isTransparent != null)
            out.write("    Transparent " + ((isTransparent) ? "On" : "Off") + "\n\n");

        out.write("    MaterialParameters {\n");
        super.writeToStream(out);
        out.write("    }\n\n");

        for (J3MOutputCapsule c : outCapsules.values()) {
            c.writeToStream(out);
        }
        out.write("}\n");
    }

    @Override
    public void write(String value, String name, String defVal) throws IOException {
        switch (name) {
            case "material_def":
                materialDefinition = value;
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
        if( value == defVal)
            return;

        switch (name) {
            case "is_transparent":
                isTransparent = value;
                break;
            default:
                throw new UnsupportedOperationException(name + " boolean material parameter not supported yet");
        }
    }

    @Override
    public void write(Savable object, String name, Savable defVal) throws IOException {
        if(object != null && !object.equals(defVal)) {
            object.write(exporter);
        }
    }

}
