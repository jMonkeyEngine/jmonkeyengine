/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
    
    @Override
    public void setValue(Object value) {
        if (!(value instanceof Texture)) {
            throw new IllegalArgumentException("value must be a texture object");
        }
        this.value = value;
        this.texture = (Texture) value;
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
            technique.updateUniformParam(getPrefixedName(), getVarType(), getUnit());
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(unit, "texture_unit", -1);
        
        // For backwards compat
        oc.write(texture, "texture", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        unit = ic.readInt("texture_unit", -1);
        texture = (Texture) value;
        //texture = (Texture) ic.readSavable("texture", null);
    }
}