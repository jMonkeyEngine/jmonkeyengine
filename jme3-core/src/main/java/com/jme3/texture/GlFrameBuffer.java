/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.texture.GlImage.Format;
import com.jme3.util.NativeObject;
import com.jme3.util.natives.GlNative;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.util.IntEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * <code>FrameBuffer</code>s are rendering surfaces allowing
 * off-screen rendering and render-to-texture functionality.
 * Instead of the scene rendering to the screen, it is rendered into the
 * FrameBuffer, the result can be either a texture or a buffer.
 * <p>
 * A <code>FrameBuffer</code> supports two methods of rendering,
 * using a {@link GlTexture} or using a buffer.
 * When using a texture, the result of the rendering will be rendered
 * onto the texture, after which the texture can be placed on an object
 * and rendered as if the texture was uploaded from disk.
 * When using a buffer, the result is rendered onto
 * a buffer located on the GPU, the data of this buffer is not accessible
 * to the user. buffers are useful if one
 * wishes to retrieve only the color content of the scene, but still desires
 * depth testing (which requires a depth buffer).
 * Buffers can be copied to other framebuffers
 * including the main screen, by using
 * {@link Renderer#copyFrameBuffer(GlFrameBuffer, GlFrameBuffer, boolean, boolean)}.
 * The content of a {@link RenderBuffer} can be retrieved by using
 * {@link Renderer#readFrameBuffer(GlFrameBuffer, java.nio.ByteBuffer) }.
 * <p>
 * <code>FrameBuffer</code>s have several attachment points, there are
 * several <em>color</em> attachment points and a single <em>depth</em>
 * attachment point.
 * The color attachment points support image formats such as
 * {@link Format#RGBA8}, allowing rendering the color content of the scene.
 * The depth attachment point requires a depth image format.
 *
 * @see Renderer#setFrameBuffer(GlFrameBuffer)
 *
 * @author Kirill Vainer
 */
public class GlFrameBuffer extends GlNative implements FrameBuffer<GlFrameBuffer.RenderBuffer> {

    public static final int SLOT_UNDEF = -1;
    public static final int SLOT_DEPTH = -100;
    public static final int SLOT_DEPTH_STENCIL = -101;

    private int width = 0;
    private int height = 0;
    private int samples = 1;
    private final ArrayList<RenderBuffer> colorBufs = new ArrayList<>();
    private RenderBuffer depthBuf = null;
    private int colorBufIndex = 0;
    private boolean srgb;
    private String name;
    private Boolean mipMapsGenerationHint = null;

    /**
     * <code>RenderBuffer</code> represents either a texture or a
     * buffer that will be rendered to. <code>RenderBuffer</code>s
     * are attached to an attachment slot on a <code>FrameBuffer</code>.
     */
    public static abstract class RenderBuffer implements ImageView<GlImage> {

        GlTexture tex;
        GlImage.Format format;
        int id = -1;
        int slot = SLOT_UNDEF;
        int face = -1;
        int layer = -1;
        int level = 0;


        public int getLevel() {
            return this.level;
        }

        /**
         * @return The image format of the render buffer.
         */
        public Format getFormat() {
            return format;
        }

        /**
         * @return The texture to render to for this <code>RenderBuffer</code>
         * or null if content should be rendered into a buffer.
         */
        public GlTexture getTexture() {
            return tex;
        }

        /**
         * Do not use.
         * @return the render buffer's ID
         */
        public int getRenderBufferId() {
            return id;
        }

        /**
         * Do not use.
         *
         * @param id the desired render buffer ID
         */
        public void setRenderBufferId(int id) {
            this.id = id;
        }

        /**
         * Do not use.
         *
         * @return the slot code, such as SLOT_DEPTH_STENCIL
         */
        public int getSlot() {
            return slot;
        }

        public int getFace() {
            return face;
        }

        public void resetObject() {
            id = -1;
        }

        protected abstract void verifyTextureProperties(int width, int height, int samples, boolean depth);

        @Override
        public String toString() {
            if (tex != null) {
                return "TextureTarget[format=" + format + "]";
            } else {
                return "BufferTarget[format=" + format + "]";
            }
        }

        public int getLayer() {
            return this.layer;
        }

        @Override
        public long getId() {
            return tex.getId();
        }

        @Override
        public GlImage getImage() {
            return tex.getImage();
        }

        @Override
        public IntEnum<Type> getViewType() {
            return tex.getViewType();
        }

        @Override
        public int getBaseMipmap() {
            return tex.getBaseMipmap();
        }

        @Override
        public int getMipmapCount() {
            return tex.getMipmapCount();
        }

        @Override
        public int getBaseLayer() {
            return tex.getBaseLayer();
        }

        @Override
        public int getLayerCount() {
            return tex.getLayerCount();
        }

    }
    
    public static class FrameBufferTextureTarget extends RenderBuffer {

        private FrameBufferTextureTarget() {}

        void setTexture(GlTexture tx){
            this.tex=tx;
            this.format=tx.getImage().getGlFormat();
        }

        void setFormat(Format f) {
            this.format=f;
        }

        public FrameBufferTextureTarget layer(int i){
            this.layer=i;
            return this;
        }

        public FrameBufferTextureTarget level(int i){
            this.level=i;
            return this;
        }

        public FrameBufferTextureTarget face(TextureCubeMap.Face f){
            return face(f.ordinal());
        }

        public FrameBufferTextureTarget face(int f){
            this.face=f;
            return this;
        }

        @Override
        protected void verifyTextureProperties(int width, int height, int samples, boolean depth) {
            GlImage img = tex.getImage();
            if (img == null) {
                throw new IllegalArgumentException("Texture not initialized with RTT.");
            }
            if (depth && !img.getGlFormat().isDepthFormat()) {
                throw new IllegalArgumentException("Texture image format must be depth.");
            } else if (!depth && img.getGlFormat().isDepthFormat()) {
                throw new IllegalArgumentException("Texture image format must be color/luminance.");
            }
            if (width != img.getWidth() || height != img.getHeight()) {
                throw new IllegalArgumentException("Texture image resolution "
                        + "must match FB resolution");
            }
            if (samples != tex.getImage().getMultiSamples()) {
                throw new IllegalStateException("Texture samples must match framebuffer samples");
            }
        }

    }

    public static class FrameBufferBufferTarget extends RenderBuffer {
        private FrameBufferBufferTarget(){}
        void setFormat(Format f){
            this.format=f;
        }

        @Override
        protected void verifyTextureProperties(int width, int height, int samples, boolean depth) {
            if (!getFormat().isDepthFormat()) {
                throw new IllegalArgumentException("Depth buffer format must be depth.");
            }
        }

    }

    public static class FrameBufferTarget {
        private FrameBufferTarget(){}
        public static FrameBufferTextureTarget newTarget(GlTexture tx){
            FrameBufferTextureTarget t=new FrameBufferTextureTarget();
            t.setTexture(tx);
            return t;
        }
    
        public static FrameBufferBufferTarget newTarget(Format format){
            FrameBufferBufferTarget t=new FrameBufferBufferTarget();
            t.setFormat(format);
            return t;
        }

        /**
         * Creates a frame buffer texture and sets the face position by using the face parameter. It uses
         * {@link TextureCubeMap} ordinal number for the face position.
         *
         * @param tx texture to add to the frame buffer
         * @param face face to add to the color buffer to
         * @return FrameBufferTexture Target
         */
        public static FrameBufferTextureTarget newTarget(GlTexture tx, TextureCubeMap.Face face) {
            FrameBufferTextureTarget t = new FrameBufferTextureTarget();
            t.face = face.ordinal();
            t.setTexture(tx);
            return t;
        }
    }

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private GlFrameBuffer() {
    }

    public void addColorTarget(FrameBufferBufferTarget colorBuf){
        colorBuf.slot=colorBufs.size();
        colorBufs.add(colorBuf);
    }
    
    public void addColorTarget(FrameBufferTextureTarget colorBuf){
        // checkSetTexture(colorBuf.getTexture(), false);  // TODO: this won't work for levels.
        colorBuf.slot=colorBufs.size();
        colorBufs.add(colorBuf);
    }
    
    /**
     * Replaces the color target at the index.
     * <p>
     * A color target must already exist at the index, otherwise
     * an exception will be thrown.
     * 
     * @param i index of color target to replace
     * @param colorBuf color target to replace with
     */
    public void replaceColorTarget(int i, FrameBufferTextureTarget colorBuf) {
        if (i < 0 || i >= colorBufs.size()) {
            throw new IndexOutOfBoundsException("No color target exists to replace at index=" + i);
        }
        colorBuf.slot = i;
        colorBufs.set(i, colorBuf);
    }

    @Override
    public void addColorTarget(RenderBuffer image) {
        image.verifyTextureProperties(width, height, samples, false);
        image.slot = colorBufs.size();
        colorBufs.add(image);
        setUpdateNeeded();
    }

    @Override
    public void setColorTarget(int i, RenderBuffer image) {
        image.verifyTextureProperties(width, height, samples, false);
        image.slot = i;
        if (colorBufs.set(i, image) != image) {
            setUpdateNeeded();
        }
    }

    /**
     * Removes the color target at the index.
     * <p>
     * Color targets above the removed target will have their
     * slot indices shifted accordingly.
     * 
     * @param i index of the target to remove
     */
    @Override
    public void removeColorTarget(int i) {
        if (i < colorBufs.size()) {
            colorBufs.remove(i);
            for (; i < colorBufs.size(); i++) {
                colorBufs.get(i).slot = i;
            }
            setUpdateNeeded();
        }
    }

    @Override
    public void removeColorTarget(RenderBuffer image) {
        if (colorBufs.remove(image)) {
            for (int i = 0; i < colorBufs.size(); i++) {
                colorBufs.get(i).slot = i;
            }
            setUpdateNeeded();
        }
    }

    @Override
    public void setDepthTarget(RenderBuffer image) {
        image.verifyTextureProperties(width, height, samples, true);
        this.depthBuf = image;
        this.depthBuf.slot =  this.depthBuf.getFormat().isDepthStencilFormat() ? SLOT_DEPTH_STENCIL : SLOT_DEPTH;
    }

    @Override
    public List<RenderBuffer> getColorTargets() {
        return Collections.unmodifiableList(colorBufs);
    }

    /**
     * Adds a texture to one of the color Buffers Array. It uses {@link TextureCubeMap} ordinal number for the
     * position in the color buffer ArrayList.
     *
     * @param colorBuf texture to add to the color Buffer
     * @param face position to add to the color buffer
     * @deprecated use {@link FrameBufferTextureTarget#face(TextureCubeMap.Face)} instead
     */
    @Deprecated
    public void addColorTarget(RenderBuffer colorBuf, TextureCubeMap.Face face) {
        colorBuf.face = face.ordinal();
        addColorTarget(colorBuf);
    }

    public int getNumColorTargets(){
        return colorBufs.size();
    }

    @Override
    public RenderBuffer getColorTarget(int index){
        return colorBufs.get(index);
    }

    public RenderBuffer getColorTarget() {
        if (colorBufs.isEmpty())
            return null;
        if (colorBufIndex<0 || colorBufIndex>=colorBufs.size()) {
            return colorBufs.get(0);
        }
        return colorBufs.get(colorBufIndex);
    }

    @Override
    public RenderBuffer getDepthTarget() {
        return depthBuf;
    }



    /**
     * <p>
     * Creates a new FrameBuffer with the given width, height, and number
     * of samples. If any textures are attached to this FrameBuffer, then
     * they must have the same number of samples as given in this constructor.
     * <p>
     * Note that if the {@link Renderer} does not expose the
     * {@link Caps#NonPowerOfTwoTextures}, then an exception will be thrown
     * if the width and height arguments are not power of two.
     *
     * @param width The width to use
     * @param height The height to use
     * @param samples The number of samples to use for a multisampled
     * framebuffer, or 1 if the framebuffer should be single-sampled.
     *
     * @throws IllegalArgumentException If width or height are not positive.
     */
    public GlFrameBuffer(int width, int height, int samples) {
        super();
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("FrameBuffer must have valid size.");
        }

        this.width = width;
        this.height = height;
        this.samples = samples == 0 ? 1 : samples;
    }

    protected GlFrameBuffer(GlFrameBuffer src) {
        super(src.object);
        /*
        for (RenderBuffer renderBuf : src.colorBufs){
            RenderBuffer clone = renderBuf.createDestructableClone();
            if (clone != null)
                this.colorBufs.add(clone);
        }

        this.depthBuf = src.depthBuf.createDestructableClone();
         */
    }



    /**
     * If enabled, any shaders rendering into this <code>FrameBuffer</code>
     * will be able to write several results into the renderbuffers
     * by using the <code>gl_FragData</code> array. Every slot in that
     * array maps into a color buffer attached to this framebuffer.
     *
     * @param enabled True to enable MRT (multiple rendering targets).
     */
    public void setMultiTarget(boolean enabled) {
        if (enabled) {
            colorBufIndex = -1;
        } else {
            colorBufIndex = 0;
        }
    }

    /**
     * @return True if MRT (multiple rendering targets) is enabled.
     * @see GlFrameBuffer#setMultiTarget(boolean)
     */
    public boolean isMultiTarget() {
        return colorBufIndex == -1;
    }

    /**
     * If MRT is not enabled ({@link GlFrameBuffer#setMultiTarget(boolean) } is false)
     * then this specifies the color target to which the scene should be rendered.
     * <p>
     * By default the value is 0.
     *
     * @param index The color attachment index.
     * @throws IllegalArgumentException If index is negative or doesn't map
     * to any attachment on this framebuffer.
     */
    public void setTargetIndex(int index) {
        if (index < 0 || index >= 16) {
            throw new IllegalArgumentException("Target index must be between 0 and 16");
        }

        if (colorBufs.size() < index) {
            throw new IllegalArgumentException("The target at " + index + " is not set!");
        }

        colorBufIndex = index;
        setUpdateNeeded();
    }

    /**
     * @return The color target to which the scene should be rendered.
     *
     * @see GlFrameBuffer#setTargetIndex(int)
     */
    public int getTargetIndex() {
        return colorBufIndex;
    }

    /**
     * Clears all color targets that were set or added previously.
     */
    @Override
    public void clearColorTargets() {
        colorBufs.clear();
    }

    /**
     * @return The height in pixels of this framebuffer.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return The width in pixels of this framebuffer.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return The number of samples when using a multisample framebuffer, or
     * 1 if this is a single-sampled framebuffer.
     */
    public int getSamples() {
        return samples;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String mrtStr = colorBufIndex >= 0 ? "" + colorBufIndex : "mrt";
        sb.append("FrameBuffer[format=").append(width).append("x").append(height)
                .append("x").append(samples).append(", drawBuf=").append(mrtStr).append("]\n");
        if (depthBuf != null) {
            sb.append("Depth => ").append(depthBuf).append("\n");
        }
        for (RenderBuffer colorBuf : colorBufs) {
            sb.append("Color(").append(colorBuf.slot)
                    .append(") => ").append(colorBuf).append("\n");
        }
        return sb.toString();
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> renderer.deleteFrameBuffer(new GlFrameBuffer(this));
    }

    @Override
    public void resetObject() {
        this.object = -1;
        for (int i = 0; i < colorBufs.size(); i++) {
            colorBufs.get(i).resetObject();
        }
        if (depthBuf != null) {
            depthBuf.resetObject();
        }
        setUpdateNeeded();
    }

    /**
     * Specifies that the color values stored in this framebuffer are in SRGB
     * format.
     *
     * The FrameBuffer must have an SRGB texture attached.
     *
     * The Renderer must expose the {@link Caps#Srgb sRGB pipeline} capability
     * for this option to take any effect.
     *
     * Rendering operations performed on this framebuffer shall undergo a linear
     * -&gt; sRGB color space conversion when this flag is enabled. If
     * {@link com.jme3.material.RenderState#getBlendMode() blending} is enabled, it will be
     * performed in linear space by first decoding the stored sRGB pixel values
     * into linear, combining with the shader result, and then converted back to
     * sRGB upon being written into the framebuffer.
     *
     * @param srgb If the framebuffer color values should be stored in sRGB
     * color space.
     *
     * @throws IllegalStateException If the texture attached to this framebuffer
     * is not sRGB.
     */
    public void setSrgb(boolean srgb) {
        this.srgb = srgb;
    }

    /**
     * Determines if this framebuffer contains SRGB data.
     *
     * @return True if the framebuffer color values are in SRGB space, false if
     * in linear space.
     */
    public boolean isSrgb() {
        return srgb;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Hints the renderer to generate mipmaps for this framebuffer if necessary
     * @param v true to enable, null to use the default value for the renderer (default to null)
     */
    public void setMipMapsGenerationHint(Boolean v) {
        mipMapsGenerationHint = v;
    }

    public Boolean getMipMapsGenerationHint() {
        return mipMapsGenerationHint;
    }
}
