/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.texture;

import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL4;
import com.jme3.renderer.opengl.TextureUtil;
import java.util.Objects;

/**
 * Wraps a texture so that only a single level of the underlying image is bound
 * instead of the entire image.
 * 
 * @author codex
 */
public class TextureImage {
    
    /**
     * Enum specifying the shader access hint of the image.
     * <p>
     * Shader accesses that violate the hint may result in undefined behavior.
     */
    public enum Access {
        
        /**
         * The image can only read from in a shader.
         */
        ReadOnly(true, false, GL2.GL_READ_ONLY),
        
        /**
         * The image can written to in a shader.
         */
        WriteOnly(false, true, GL2.GL_WRITE_ONLY),
        
        /**
         * The image can both be written to and read from in a shader.
         */
        ReadWrite(true, true, GL2.GL_READ_WRITE);
        
        private final boolean read, write;
        private final int glEnum;

        private Access(boolean read, boolean write, int glEnum) {
            this.read = read;
            this.write = write;
            this.glEnum = glEnum;
        }
        
        /**
         * If true, the image can be read from in a shader.
         * 
         * @return 
         */
        public boolean isRead() {
            return read;
        }

        /**
         * If true, the image can be written to in a shader.
         * 
         * @return 
         */
        public boolean isWrite() {
            return write;
        }
        
        /**
         * Corresponding OpenGL enum.
         * 
         * @return 
         */
        public int getGlEnum() {
            return glEnum;
        }
        
    }
    
    private Texture texture;
    private int level, layer;
    private Access access;
    private boolean updateFlag = true;
    
    public TextureImage(Texture texture) {
        this(texture, 0, -1, Access.ReadWrite);
    }
    
    public TextureImage(Texture texture, Access access) {
        this(texture, 0, -1, access);
    }
    
    public TextureImage(Texture texture, int level, int layer) {
        this(texture, level, layer, Access.ReadWrite);
    }
    
    public TextureImage(Texture texture, int level, int layer, Access access) {
        this.texture = Objects.requireNonNull(texture, "Underlying texture cannot be null");
        this.level = level;
        this.layer = layer;
        this.access = access;
        if (this.level < 0) {
            throw new IllegalArgumentException("Level cannot be less than zero.");
        }
    }
    
    /**
     * Binds this texture image to the texture unit.
     * <p>
     * Calling this is not completely sufficient for totally binding an image
     * to an image unit. Additionally, the image must be bound beforehand using
     * {@link GL2#glBindTexture(int, int)}.
     * 
     * @param gl4 GL4 implementation (not null)
     * @param texUtil utility used to convert JME's image format to the corresponding GL enum (not null)
     * @param unit texture unit to bind to
     */
    public void bindImage(GL4 gl4, TextureUtil texUtil, int unit) {
        Image img = texture.getImage();
        gl4.glBindImageTexture(unit, img.getId(), level, isLayered(), Math.max(layer, 0),
                access.getGlEnum(), texUtil.getImageFormat(img.getFormat(), false).internalFormat);
    }
    
    /**
     * Sets the update flag indicating this texture image needs rebinding.
     */
    public void setUpdateNeeded() {
        updateFlag = true;
    }
    
    /**
     * Clears the update flag and returns the update flag's value before
     * it was cleared.
     * 
     * @return 
     */
    public boolean clearUpdateNeeded() {
        boolean f = updateFlag;
        updateFlag = false;
        return f;
    }
    
    /**
     * Sets the underlying texture wrapped by this TextureImage.
     * 
     * @param texture wrapped texture (not null)
     */
    public void setTexture(Texture texture) {
        Objects.requireNonNull(texture, "Wrapped texture cannot be null.");
        if (this.texture != texture) {
            this.texture = texture;
            updateFlag = true;
        }
    }
    
    /**
     * Sets the image level to bind.
     * <p>
     * The level controls which mipmap level will be bound to the texture unit,
     * where zero corresponds to the base level of the texture.
     * <p>
     * default=0
     * 
     * @param level level to bind (not negative)
     */
    public void setLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("Texture image level cannot be negative.");
        }
        if (this.level != level) {
            this.level = level;
            updateFlag = true;
        }
    }
    
    /**
     * Sets the image layer to bind.
     * <p>
     * If the underlying texture is a one/two/three demensional array,
     * cube map, cube map array, two demensional multisample array, then this
     * specifies which layer of the array to bind.
     * <p>
     * default=-1
     * 
     * @param layer layer to bind, or negative to bind all layers
     */
    public void setLayer(int layer) {
        if (this.layer != layer && (this.layer >= 0 || layer >= 0)) {
            this.layer = layer;
            updateFlag = true;
        }
    }
    
    /**
     * Sets the shader access hint with which to bind the image.
     * 
     * @param access 
     */
    public void setAccess(Access access) {
        if (this.access != access) {
            this.access = access;
            updateFlag = true;
        }
    }
    
    /**
     * 
     * @return 
     * @see #setTexture(com.jme3.texture.Texture)
     */
    public Texture getTexture() {
        return texture;
    }
    
    /**
     * Gets the image belonging to the underlying texture.
     * 
     * @return 
     */
    public Image getImage() {
        return texture.getImage();
    }
    
    /**
     * Gets the format of the image belonging to the underlying texture.
     * 
     * @return 
     */
    public Image.Format getFormat() {
        return texture.getImage().getFormat();
    }
    
    /**
     * Gets the native id of the image belonging to the underlying texture.
     * 
     * @return 
     */
    public int getImageId() {
        return texture.getImage().getId();
    }
    
    /**
     * 
     * @return 
     * @see #setLevel(int)
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 
     * @return 
     * @see #setLayer(int)
     */
    public int getLayer() {
        return layer;
    }
    
    /**
     * 
     * @return 
     * @see #setAccess(com.jme3.texture.TextureImage.Access)
     */
    public Access getAccess() {
        return access;
    }
    
    /**
     * Returns true if all layers of an image will be bound, when
     * {@code layer} is negative.
     * 
     * @return 
     * @see #setLayer(int)
     */
    public boolean isLayered() {
        return layer < 0;
    }
    
    /**
     * Returns true if the update flag has been set indicating rebinding
     * is required.
     * 
     * @return 
     */
    public boolean isUpdateNeeded() {
        return updateFlag;
    }
    
}
