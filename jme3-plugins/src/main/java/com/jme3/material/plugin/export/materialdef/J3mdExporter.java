package com.jme3.material.plugin.export.materialdef;

import com.jme3.material.*;
import java.io.*;
import java.util.List;

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
public class J3mdExporter {


    /**
     * Create a J3mdExporter
     */
    public J3mdExporter() {
    }


    public void save(MaterialDef matDef, OutputStream f) throws IOException {

        OutputStreamWriter out = new OutputStreamWriter(f);
        J3mdMatParamWriter paramWriter = new J3mdMatParamWriter();
        J3mdTechniqueDefWriter techniqueWriter = new J3mdTechniqueDefWriter();

//        for (MatParam matParam : matDef.getMaterialParams()) {
//            System.err.println(matParam.toString());
//        }
//
//        for (String key : matDef.getTechniqueDefsNames()) {
//            System.err.println(matDef.getTechniqueDefs(key).toString());
//        }

        out.write("MaterialDef " + matDef.getName() + " {\n");
        out.write("    MaterialParameters {\n");
        for (MatParam matParam : matDef.getMaterialParams()) {
            paramWriter.write(matParam, out);
        }
        out.write("    }\n\n");

        for (String key : matDef.getTechniqueDefsNames()) {
            List<TechniqueDef> defs = matDef.getTechniqueDefs(key);
            for (TechniqueDef def : defs) {
                techniqueWriter.write(def, matDef.getMaterialParams(), out);
            }
        }

        out.write("}\n");
        out.flush();
    }


    public void save(MaterialDef matDef, File f) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(f);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            save(matDef, bos);
        }
    }

}
