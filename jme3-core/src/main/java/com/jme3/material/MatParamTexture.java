/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import java.io.IOException;

/**
 * A material parameter that holds a reference to a texture and its required color space.
 * This class extends {@link MatParam} to provide texture specific functionalities.
 */
public class MatParamTexture extends MatParam {

    private ColorSpace colorSpace;

    /**
     * Constructs a new MatParamTexture instance with the specified type, name,
     * texture, and color space.
     *
     * @param type       the type of the material parameter
     * @param name       the name of the parameter
     * @param texture    the texture associated with this parameter
     * @param colorSpace the required color space for the texture
     */
    public MatParamTexture(VarType type, String name, Texture texture, ColorSpace colorSpace) {
        super(type, name, texture);
        this.colorSpace = colorSpace;
    }

    /**
     * Serialization only. Do not use.
     */
    public MatParamTexture() {
    }

    /**
     * Retrieves the texture associated with this material parameter.
     *
     * @return the texture object
     */
    public Texture getTextureValue() {
        return (Texture) getValue();
    }

    /**
     * Sets the texture associated with this material parameter.
     *
     * @param value the texture object to set
     * @throws IllegalArgumentException if the provided value is not a {@link Texture}
     */
    public void setTextureValue(Texture value) {
        setValue(value);
    }
    
    /**
     * Overrides the base class {@link MatParam#setValue(Object)} to allow null
     * values.
     *
     * @param value the object to set as the value (must be a {@link Texture} or null)
     * @throws IllegalArgumentException if the provided value is not a {@link Texture} and not null
     */
    @Override
    public void setValue(Object value) {
        if ((value == null) || value instanceof Texture) {
            this.value = value;
        } else {
            throw new IllegalArgumentException("Value must be a Texture object");
        }
    }

    /**
     * Gets the required color space for this texture parameter.
     * 
     * @return the required color space ({@link ColorSpace})
     */
    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    /**
     * Set to {@link ColorSpace#Linear} if the texture color space has to be forced
     * to linear instead of sRGB.
     * 
     * @param colorSpace the desired color space
     */
    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(colorSpace, "colorSpace", null);
        // For backwards compatibility
        oc.write(0, "texture_unit", -1);
        oc.write((Texture) value, "texture", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        colorSpace = ic.readEnum("colorSpace", ColorSpace.class, null);
    }
    
}
