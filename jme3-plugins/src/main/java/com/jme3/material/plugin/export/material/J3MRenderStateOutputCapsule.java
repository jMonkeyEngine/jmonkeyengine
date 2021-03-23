package com.jme3.material.plugin.export.material;

import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

/**
 *
 * @author tsr
 */
public class J3MRenderStateOutputCapsule extends J3MOutputCapsule {
    protected final static HashMap<String, String> NAME_MAP;
    protected String offsetUnit;

    static {
        NAME_MAP = new HashMap<>();
        NAME_MAP.put( "wireframe", "Wireframe");
        NAME_MAP.put( "cullMode", "FaceCull");
        NAME_MAP.put( "depthWrite", "DepthWrite");
        NAME_MAP.put( "depthTest", "DepthTest");
        NAME_MAP.put( "blendMode", "Blend");
        NAME_MAP.put( "alphaFallOff", "AlphaTestFalloff");
        NAME_MAP.put( "offsetFactor", "PolyOffset");
        NAME_MAP.put( "colorWrite", "ColorWrite");
        NAME_MAP.put( "pointSprite", "PointSprite");
        NAME_MAP.put( "depthFunc", "DepthFunc");
        NAME_MAP.put( "alphaFunc", "AlphaFunc");
        NAME_MAP.put( "lineWidth", "LineWidth");
    }
    public J3MRenderStateOutputCapsule(J3MExporter exporter) {
        super(exporter);
    }

    public OutputCapsule getCapsule(Savable object) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        super.clear();
        offsetUnit = "";
    }

    @Override
    public void writeToStream(Writer out) throws IOException {
        out.write("    AdditionalRenderState {\n");
        super.writeToStream(out);
        out.write("    }\n");
    }

    @Override
    protected void writeParameter(Writer out, String name, String value) throws IOException {
        out.write(name);
        out.write(" ");
        out.write(value);

        if( "PolyOffset".equals(name) ) {
            out.write(" ");
            out.write(offsetUnit);
        }
    }

    @Override
    protected void putParameter(String name, String value ) {
        if( "offsetUnits".equals(name) ) {
            offsetUnit = value;
            return;
        }

        if( !NAME_MAP.containsKey(name) )
            return;

        super.putParameter(NAME_MAP.get(name), value);
    }
}
