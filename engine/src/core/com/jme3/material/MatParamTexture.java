package com.jme3.material;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.Renderer;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import java.io.IOException;

public class MatParamTexture extends MatParam {

    private Texture texture;
    private int unit;

    public MatParamTexture(VarType type, String name, Texture texture, int unit) {
        super(type, name, texture, null);
        this.texture = texture;
        this.unit = unit;
    }

    public MatParamTexture() {
    }

    public Texture getTextureValue() {
        return texture;
    }

    public void setTextureValue(Texture value) {
        this.value = value;
        this.texture = value;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public int getUnit() {
        return unit;
    }

    @Override
    public void apply(Renderer r, Technique technique) {
        TechniqueDef techDef = technique.getDef();
        r.setTexture(getUnit(), getTextureValue());
        if (techDef.isUsingShaders()) {
            technique.updateUniformParam(getPrefixedName(), getVarType(), getUnit(), true);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(unit, "texture_unit", -1);
        oc.write(texture, "texture", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        unit = ic.readInt("texture_unit", -1);
        texture = (Texture) ic.readSavable("texture", null);
    }
}