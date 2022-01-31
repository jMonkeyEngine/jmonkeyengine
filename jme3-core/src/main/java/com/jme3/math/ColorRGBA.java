/*
 * Copyright (c) 2009-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.math;

import com.jme3.export.*;
import java.io.IOException;

/**
 * <code>ColorRGBA</code> defines a color made from a collection of red, green
 * and blue values stored in Linear color space. An alpha value determines is
 * transparency.
 *
 * @author Mark Powell
 * @version $Id: ColorRGBA.java,v 1.29 2007/09/09 18:25:14 irrisor Exp $
 */
public final class ColorRGBA implements Savable, Cloneable, java.io.Serializable {
    static final float GAMMA = 2.2f;

    static final long serialVersionUID = 1;
    /**
     * The color black (0,0,0).
     */
    public static final ColorRGBA Black = new ColorRGBA(0f, 0f, 0f, 1f);
    /**
     * The color white (1,1,1).
     */
    public static final ColorRGBA White = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * The color gray (.2,.2,.2).
     */
    public static final ColorRGBA DarkGray = new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f);
    /**
     * The color gray (.5,.5,.5).
     */
    public static final ColorRGBA Gray = new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f);
    /**
     * The color gray (.8,.8,.8).
     */
    public static final ColorRGBA LightGray = new ColorRGBA(0.8f, 0.8f, 0.8f, 1.0f);
    /**
     * The color red (1,0,0).
     */
    public static final ColorRGBA Red = new ColorRGBA(1f, 0f, 0f, 1f);
    /**
     * The color green (0,1,0).
     */
    public static final ColorRGBA Green = new ColorRGBA(0f, 1f, 0f, 1f);
    /**
     * The color blue (0,0,1).
     */
    public static final ColorRGBA Blue = new ColorRGBA(0f, 0f, 1f, 1f);
    /**
     * The color yellow (1,1,0).
     */
    public static final ColorRGBA Yellow = new ColorRGBA(1f, 1f, 0f, 1f);
    /**
     * The color magenta (1,0,1).
     */
    public static final ColorRGBA Magenta = new ColorRGBA(1f, 0f, 1f, 1f);
    /**
     * The color cyan (0,1,1).
     */
    public static final ColorRGBA Cyan = new ColorRGBA(0f, 1f, 1f, 1f);
    /**
     * The color orange (251/255, 130/255,0).
     */
    public static final ColorRGBA Orange = new ColorRGBA(251f / 255f, 130f / 255f, 0f, 1f);
    /**
     * The color brown (65/255, 40/255, 25/255).
     */
    public static final ColorRGBA Brown = new ColorRGBA(65f / 255f, 40f / 255f, 25f / 255f, 1f);
    /**
     * The color pink (1, 0.68, 0.68).
     */
    public static final ColorRGBA Pink = new ColorRGBA(1f, 0.68f, 0.68f, 1f);
    /**
     * The black color with no alpha (0, 0, 0, 0).
     */
    public static final ColorRGBA BlackNoAlpha = new ColorRGBA(0f, 0f, 0f, 0f);
    /**
     * The red component of the color. 0 is none and 1 is maximum red.
     */
    public float r;
    /**
     * The green component of the color. 0 is none and 1 is maximum green.
     */
    public float g;
    /**
     * The blue component of the color. 0 is none and 1 is maximum blue.
     */
    public float b;
    /**
     * The alpha component of the color. 0 is transparent and 1 is opaque.
     */
    public float a;

    /**
     * Constructor instantiates a new <code>ColorRGBA</code> object. This
     * color is the default "white" with all values 1.
     */
    public ColorRGBA() {
        r = g = b = a = 1.0f;
    }

    /**
     * Constructor instantiates a new <code>ColorRGBA</code> object. The
     * values are defined as passed parameters.
     * these values are assumed to be in linear space and stored as is.
     * If you want to assign sRGB values use
     * {@link ColorRGBA#setAsSrgb(float, float, float, float) }
     *
     * @param r The red component of this color.
     * @param g The green component of this <code>ColorRGBA</code>.
     * @param b The blue component of this <code>ColorRGBA</code>.
     * @param a The alpha component of this <code>ColorRGBA</code>.
     */
    public ColorRGBA(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Copy constructor creates a new <code>ColorRGBA</code> object, based on
     * a provided color.
     *
     * @param rgba The <code>ColorRGBA</code> object to copy.
     */
    public ColorRGBA(ColorRGBA rgba) {
        this.a = rgba.a;
        this.r = rgba.r;
        this.g = rgba.g;
        this.b = rgba.b;
    }
    
    /**
     * Constructor creates a new <code>ColorRGBA</code> object, based on
     * a provided Vector4f.
     *
     * @param vec4 The <code>Vector4f</code> object that will have its x, y, z, and w
     * values copied to this color's r, g, b, and a values respectively.
     */
    public ColorRGBA(Vector4f vec4) {
        this.a = vec4.w;
        this.r = vec4.x;
        this.g = vec4.y;
        this.b = vec4.z;
    }    
    
    /**
     * Constructor creates a new <code>ColorRGBA</code> object, based on
     * a provided Vector3f, at full opacity with a 1.0 alpha value by default
     *
     * @param vec3 The <code>Vector3f</code> object that will have its x, y, and z
     * values copied to this color's r, g, and b values respectively.
     */
    public ColorRGBA(Vector3f vec3) {
        this.a = 1.0f;
        this.r = vec3.x;
        this.g = vec3.y;
        this.b = vec3.z;
    }    

    /**
     * <code>set</code> sets the RGBA values of this <code>ColorRGBA</code>.
     * these values are assumed to be in linear space and stored as is.
     * If you want to assign sRGB values use
     * {@link ColorRGBA#setAsSrgb(float, float, float, float) }
     *
     * @param r The red component of this color.
     * @param g The green component of this color.
     * @param b The blue component of this color.
     * @param a The alpha component of this color.
     * @return this
     */
    public ColorRGBA set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    /**
     * <code>set</code> sets the values of this <code>ColorRGBA</code> to those
     * set by a parameter color.
     *
     * @param rgba The color to set this <code>ColorRGBA</code> to.
     * @return this
     */
    public ColorRGBA set(ColorRGBA rgba) {
        if (rgba == null) {
            r = 0;
            g = 0;
            b = 0;
            a = 0;
        } else {
            r = rgba.r;
            g = rgba.g;
            b = rgba.b;
            a = rgba.a;
        }
        return this;
    }
    
    /**
     * <code>set</code> sets the values of this <code>ColorRGBA</code> to those
     * set by a parameter Vector4f.
     *
     * @param vec4 The 4 component vector that will have its x, y, z, and w values copied to
     * this <code>ColorRGBA</code>'s r, g, b, and a values respectively.
     *
     * @return this
     */
    public ColorRGBA set(Vector4f vec4) {
        if (vec4 == null) {
            r = 0;
            g = 0;
            b = 0;
            a = 0;
        } else {
            r = vec4.x;
            g = vec4.y;
            b = vec4.z;
            a = vec4.w;
        }
        return this;
    }    
    
    /**
     * <code>set</code> sets the values of this <code>ColorRGBA</code> to those
     * set by a parameter Vector3f.
     *
     * @param vec3 The 3 component vector that will have its x, y, and z values copied to
     * this <code>ColorRGBA</code>'s r, g, and b values respectively.
     *
     * @return this
     */
    public ColorRGBA set(Vector3f vec3) {
        if (vec3 == null) {
            r = 0;
            g = 0;
            b = 0;
        } else {
            r = vec3.x;
            g = vec3.y;
            b = vec3.z;
        }
        return this;
    }       

    /**
     * Sets the red color to the specified value.
     * @param value the value to set the red channel.
     * @return the ColorRGBA object with the modified value.
     */
    public ColorRGBA setRed(float value) {
        r = value;
        return this;
    }

    /**
     * Sets the green color to the specified value.
     * @param value the value to set the green channel.
     * @return the ColorRGBA object with the modified value.
     */
    public ColorRGBA setGreen(float value) {
        g = value;
        return this;
    }

    /**
     * Sets the blue color to the specified value.
     * @param value the value to set the blue channel.
     * @return the ColorRGBA object with the modified value.
     */
    public ColorRGBA setBlue(float value) {
        b = value;
        return this;
    }

    /**
     * Sets the alpha color to the specified value.
     * @param value the value to set the alpha channel.
     * @return the ColorRGBA object with the modified value.
     */
    public ColorRGBA setAlpha(float value) {
        a = value;
        return this;
    }

    /**
     * Saturate that color ensuring all channels have a value between 0 and 1
     */
    public void clamp() {
        r = FastMath.clamp(r, 0f, 1f);
        g = FastMath.clamp(g, 0f, 1f);
        b = FastMath.clamp(b, 0f, 1f);
        a = FastMath.clamp(a, 0f, 1f);
    }

    /**
     * <code>getColorArray</code> retrieves the color values of this
     * <code>ColorRGBA</code> as a four element <code>float</code> array in the
     * order: r,g,b,a.
     *
     * @return The <code>float</code> array that contains the color components.
     */
    public float[] getColorArray() {
        return new float[]{r, g, b, a};
    }

    /**
     * Stores the current r,g,b,a values into the given array.  The given array must have a
     * length of 4 or greater, or an array index out of bounds exception will be thrown.
     *
     * @param store The <code>float</code> array to store the values into.
     * @return The <code>float</code> array after storage.
     */
    public float[] getColorArray(float[] store) {
        store[0] = r;
        store[1] = g;
        store[2] = b;
        store[3] = a;
        return store;
    }

    /**
     * Retrieves the alpha component value of this <code>ColorRGBA</code>.
     *
     * @return The alpha component value.
     */
    public float getAlpha() {
        return a;
    }

    /**
     * Retrieves the red component value of this <code>ColorRGBA</code>.
     *
     * @return The red component value.
     */
    public float getRed() {
        return r;
    }

    /**
     * Retrieves the blue component value of this <code>ColorRGBA</code>.
     *
     * @return The blue component value.
     */
    public float getBlue() {
        return b;
    }

    /**
     * Retrieves the green component value of this <code>ColorRGBA</code>.
     *
     * @return The green component value.
     */
    public float getGreen() {
        return g;
    }

    /**
     * Sets this <code>ColorRGBA</code> to the interpolation by changeAmount from
     * this to the finalColor:
     * this=(1-changeAmount)*this + changeAmount * finalColor
     *
     * @param finalColor The final color to interpolate towards.
     * @param changeAmount An amount between 0.0 - 1.0 representing a percentage
     * change from this towards finalColor.
     * @return this ColorRGBA
     */
    public ColorRGBA interpolateLocal(ColorRGBA finalColor, float changeAmount) {
        this.r = (1 - changeAmount) * this.r + changeAmount * finalColor.r;
        this.g = (1 - changeAmount) * this.g + changeAmount * finalColor.g;
        this.b = (1 - changeAmount) * this.b + changeAmount * finalColor.b;
        this.a = (1 - changeAmount) * this.a + changeAmount * finalColor.a;
        return this;
    }

    /**
     * Sets this <code>ColorRGBA</code> to the interpolation by changeAmount from
     * beginColor to finalColor:
     * this=(1-changeAmount)*beginColor + changeAmount * finalColor
     *
     * @param beginColor The beginning color (changeAmount=0).
     * @param finalColor The final color to interpolate towards (changeAmount=1).
     * @param changeAmount An amount between 0.0 - 1.0 representing a percentage
     *  change from beginColor towards finalColor.
     * @return this ColorRGBA
     */
    public ColorRGBA interpolateLocal(ColorRGBA beginColor, ColorRGBA finalColor, float changeAmount) {
        this.r = (1 - changeAmount) * beginColor.r + changeAmount * finalColor.r;
        this.g = (1 - changeAmount) * beginColor.g + changeAmount * finalColor.g;
        this.b = (1 - changeAmount) * beginColor.b + changeAmount * finalColor.b;
        this.a = (1 - changeAmount) * beginColor.a + changeAmount * finalColor.a;
        return this;
    }

    /**
     * <code>randomColor</code> is a utility method that generates a random
     * opaque color.
     *
     * @return a random <code>ColorRGBA</code> with an alpha set to 1.
     */
    public static ColorRGBA randomColor() {
        ColorRGBA rVal = new ColorRGBA(0, 0, 0, 1);
        rVal.r = FastMath.nextRandomFloat();
        rVal.g = FastMath.nextRandomFloat();
        rVal.b = FastMath.nextRandomFloat();
        return rVal;
    }

    /**
     * Multiplies each r,g,b,a of this <code>ColorRGBA</code> by the corresponding
     * r,g,b,a of the given color and returns the result as a new <code>ColorRGBA</code>.
     * Used as a way of combining colors and lights.
     *
     * @param c The color to multiply by.
     * @return The new <code>ColorRGBA</code>.  this*c
     */
    public ColorRGBA mult(ColorRGBA c) {
        return new ColorRGBA(c.r * r, c.g * g, c.b * b, c.a * a);
    }

    /**
     * Multiplies each r,g,b,a of this <code>ColorRGBA</code> by the given scalar and
     * returns the result as a new <code>ColorRGBA</code>.
     * Used as a way of making colors dimmer or brighter.
     *
     * @param scalar The scalar to multiply by.
     * @return The new <code>ColorRGBA</code>.  this*scalar
     */
    public ColorRGBA mult(float scalar) {
        return new ColorRGBA(scalar * r, scalar * g, scalar * b, scalar * a);
    }

    /**
     * Multiplies each r,g,b,a of this <code>ColorRGBA</code> by the given scalar and
     * returns the result (this).
     * Used as a way of making colors dimmer or brighter.
     *
     * @param scalar The scalar to multiply by.
     * @return this*c
     */
    public ColorRGBA multLocal(float scalar) {
        this.r *= scalar;
        this.g *= scalar;
        this.b *= scalar;
        this.a *= scalar;
        return this;
    }

    /**
     * Adds each r,g,b,a of this <code>ColorRGBA</code> by the corresponding
     * r,g,b,a of the given color and returns the result as a new <code>ColorRGBA</code>.
     * Used as a way of combining colors and lights.
     *
     * @param c The color to add.
     * @return The new <code>ColorRGBA</code>.  this+c
     */
    public ColorRGBA add(ColorRGBA c) {
        return new ColorRGBA(c.r + r, c.g + g, c.b + b, c.a + a);
    }

    /**
     * Adds each component to the corresponding component of the argument
     * and returns the result (this).
     * Used as a way of combining colors and lights.
     *
     * @param c The color to add.
     * @return this+c
     */
    public ColorRGBA addLocal(ColorRGBA c) {
        set(c.r + r, c.g + g, c.b + b, c.a + a);
        return this;
    }

    /**
     * <code>toString</code> returns the string representation of this <code>ColorRGBA</code>.
     * The format of the string is:<br>
     * Color[R.RRRR, G.GGGG, B.BBBB, A.AAAA]
     *
     * @return The string representation of this <code>ColorRGBA</code>.
     */
    @Override
    public String toString() {
        return "Color[" + r + ", " + g + ", " + b + ", " + a + "]";
    }

    /**
     * Create a copy of this color.
     *
     * @return a new instance, equivalent to this one
     */
    @Override
    public ColorRGBA clone() {
        try {
            return (ColorRGBA) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }

    /**
     * Saves this <code>ColorRGBA</code> into the given <code>float</code> array.
     *
     * @param floats The <code>float</code> array to take this <code>ColorRGBA</code>.
     * If null, a new <code>float[4]</code> is created.
     * @return The array, with r,g,b,a float values in that order.
     */
    public float[] toArray(float[] floats) {
        if (floats == null) {
            floats = new float[4];
        }
        floats[0] = r;
        floats[1] = g;
        floats[2] = b;
        floats[3] = a;
        return floats;
    }

    /**
     * <code>equals</code> returns true if this <code>ColorRGBA</code> is logically equivalent
     * to a given color. That is, if all the components of the two colors are the same.
     * False is returned otherwise.
     *
     * @param o The object to compare against.
     * @return true if the colors are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorRGBA)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        ColorRGBA comp = (ColorRGBA) o;
        if (Float.compare(r, comp.r) != 0) {
            return false;
        }
        if (Float.compare(g, comp.g) != 0) {
            return false;
        }
        if (Float.compare(b, comp.b) != 0) {
            return false;
        }
        if (Float.compare(a, comp.a) != 0) {
            return false;
        }
        return true;
    }

    /**
     * <code>hashCode</code> returns a unique code for this <code>ColorRGBA</code> based
     * on its values. If two colors are logically equivalent, they will return
     * the same hash code value.
     *
     * @return The hash code value of this <code>ColorRGBA</code>.
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash += 37 * hash + Float.floatToIntBits(r);
        hash += 37 * hash + Float.floatToIntBits(g);
        hash += 37 * hash + Float.floatToIntBits(b);
        hash += 37 * hash + Float.floatToIntBits(a);
        return hash;
    }

    /**
     * Serialize this color to the specified exporter, for example when
     * saving to a J3O file.
     *
     * @param e (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(r, "r", 0);
        capsule.write(g, "g", 0);
        capsule.write(b, "b", 0);
        capsule.write(a, "a", 0);
    }

    /**
     * De-serialize this color from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);
        r = capsule.readFloat("r", 0);
        g = capsule.readFloat("g", 0);
        b = capsule.readFloat("b", 0);
        a = capsule.readFloat("a", 0);
    }

    /**
     * Retrieves the component values of this <code>ColorRGBA</code> as
     * a four element <code>byte</code> array in the order: r,g,b,a.
     *
     * @return the <code>byte</code> array that contains the color components.
     */
    public byte[] asBytesRGBA() {
        byte[] store = new byte[4];
        store[0] = (byte) ((int) (r * 255) & 0xFF);
        store[1] = (byte) ((int) (g * 255) & 0xFF);
        store[2] = (byte) ((int) (b * 255) & 0xFF);
        store[3] = (byte) ((int) (a * 255) & 0xFF);
        return store;
    }

    /**
     * Retrieves the component values of this <code>ColorRGBA</code> as an
     * <code>int</code> in a,r,g,b order.
     * Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue.
     *
     * @return The integer representation of this <code>ColorRGBA</code> in a,r,g,b order.
     */
    public int asIntARGB() {
        int argb = (((int) (a * 255) & 0xFF) << 24)
                | (((int) (r * 255) & 0xFF) << 16)
                | (((int) (g * 255) & 0xFF) << 8)
                | (((int) (b * 255) & 0xFF));
        return argb;
    }

    /**
     * Retrieves the component values of this <code>ColorRGBA</code> as an
     * <code>int</code> in r,g,b,a order.
     * Bits 24-31 are red, 16-23 are green, 8-15 are blue, 0-7 are alpha.
     *
     * @return The integer representation of this <code>ColorRGBA</code> in r,g,b,a order.
     */
    public int asIntRGBA() {
        int rgba = (((int) (r * 255) & 0xFF) << 24)
                | (((int) (g * 255) & 0xFF) << 16)
                | (((int) (b * 255) & 0xFF) << 8)
                | (((int) (a * 255) & 0xFF));
        return rgba;
    }

    /**
     * Retrieves the component values of this <code>ColorRGBA</code> as an
     * <code>int</code> in a,b,g,r order.
     * Bits 24-31 are alpha, 16-23 are blue, 8-15 are green, 0-7 are red.
     *
     * @return The integer representation of this <code>ColorRGBA</code> in a,b,g,r order.
     */
    public int asIntABGR() {
        int abgr = (((int) (a * 255) & 0xFF) << 24)
                | (((int) (b * 255) & 0xFF) << 16)
                | (((int) (g * 255) & 0xFF) << 8)
                | (((int) (r * 255) & 0xFF));
        return abgr;
    }

    /**
     * Sets the component values of this <code>ColorRGBA</code> with the given
     * combined ARGB <code>int</code>.
     * Bits 24-31 are alpha, bits 16-23 are red, bits 8-15 are green, bits 0-7 are blue.
     *
     * @param color The integer ARGB value used to set this <code>ColorRGBA</code>.
     * @return this
     */
    public ColorRGBA fromIntARGB(int color) {
        a = ((byte) (color >> 24) & 0xFF) / 255f;
        r = ((byte) (color >> 16) & 0xFF) / 255f;
        g = ((byte) (color >> 8) & 0xFF) / 255f;
        b = ((byte) (color) & 0xFF) / 255f;
        return this;
    }

    /**
     * Sets the RGBA values of this <code>ColorRGBA</code> with the given combined RGBA value
     * Bits 24-31 are red, bits 16-23 are green, bits 8-15 are blue, bits 0-7 are alpha.
     *
     * @param color The integer RGBA value used to set this object.
     * @return this
     */
    public ColorRGBA fromIntRGBA(int color) {
        r = ((byte) (color >> 24) & 0xFF) / 255f;
        g = ((byte) (color >> 16) & 0xFF) / 255f;
        b = ((byte) (color >> 8) & 0xFF) / 255f;
        a = ((byte) (color) & 0xFF) / 255f;
        return this;
    }

    /**
     * Sets the RGBA values of this <code>ColorRGBA</code> with the given combined ABGR value
     * Bits 24-31 are alpha, bits 16-23 are blue, bits 8-15 are green, bits 0-7 are red.
     *
     * @param color The integer ABGR value used to set this object.
     * @return this
     */
    public ColorRGBA fromIntABGR(int color) {
        a = ((byte) (color >> 24) & 0xFF) / 255f;
        b = ((byte) (color >> 16) & 0xFF) / 255f;
        g = ((byte) (color >> 8) & 0xFF) / 255f;
        r = ((byte) (color) & 0xFF) / 255f;
        return this;
    }

    /**
     * Converts a color from RGBA 255 values.
     * @param r the red value in 0-255 range.
     * @param g the green value in 0-255 range.
     * @param b the blue value in 0-255 range.
     * @param a the alpha value in 0-255 range.
     * @return the ColorRGBA equivalent of the RGBA 255 color.
     */
    public static ColorRGBA fromRGBA255(int r, int g, int b, int a) {
        return new ColorRGBA(r / 255.0F, g / 255.0F, b / 255.0F, a / 255.0F);
    }

    /**
     * Transform this <code>ColorRGBA</code> to a <code>Vector3f</code> using
     * x = r, y = g, z = b. The Alpha value is not used.
     * This method is useful for shader assignments.
     *
     * @return A <code>Vector3f</code> containing the RGB value of this <code>ColorRGBA</code>.
     */
    public Vector3f toVector3f() {
        return new Vector3f(r, g, b);
    }

    /**
     * Transform this <code>ColorRGBA</code> to a <code>Vector4f</code> using
     * x = r, y = g, z = b, w = a.
     * This method is useful for shader assignments.
     *
     * @return A <code>Vector4f</code> containing the RGBA value of this <code>ColorRGBA</code>.
     */
    public Vector4f toVector4f() {
        return new Vector4f(r, g, b, a);
    }

    /**
     * Sets the rgba channels of this color in sRGB color space.
     * You probably want to use this method if the color is picked by the use
     * in a color picker from a GUI.
     *
     * Note that the values will be gamma corrected to be stored in linear space
     * GAMMA value is 2.2
     *
     * Note that no correction will be performed on the alpha channel as it
     * conventionally doesn't represent a color itself
     *
     * @param r the red value in sRGB color space
     * @param g the green value in sRGB color space
     * @param b the blue value in sRGB color space
     * @param a the alpha value
     *
     * @return this ColorRGBA with updated values.
     */
    public ColorRGBA setAsSrgb(float r, float g, float b, float a) {
        this.r = (float) Math.pow(r, GAMMA);
        this.b = (float) Math.pow(b, GAMMA);
        this.g = (float) Math.pow(g, GAMMA);
        this.a = a;

        return this;
    }

    /**
     * Get the color in sRGB color space as a <code>ColorRGBA</code>.
     *
     * Note that linear values stored in the ColorRGBA will be gamma corrected
     * and returned as a ColorRGBA.
     *
     * The x attribute will be fed with the r channel in sRGB space.
     * The y attribute will be fed with the g channel in sRGB space.
     * The z attribute will be fed with the b channel in sRGB space.
     * The w attribute will be fed with the a channel.
     *
     * Note that no correction will be performed on the alpha channel as it
     * conventionally doesn't represent a color itself.
     *
     * @return the color in sRGB color space as a ColorRGBA.
     */
    public ColorRGBA getAsSrgb() {
        ColorRGBA srgb = new ColorRGBA();
        float invGamma = 1f / GAMMA;
        srgb.r = (float) Math.pow(r, invGamma);
        srgb.g = (float) Math.pow(g, invGamma);
        srgb.b = (float) Math.pow(b, invGamma);
        srgb.a = a;
        return srgb;
    }
}
