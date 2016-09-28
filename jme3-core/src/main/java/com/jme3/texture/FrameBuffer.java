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
package com.jme3.texture;

import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.texture.Image.Format;
import com.jme3.util.NativeObject;
import java.util.ArrayList;

/**
 * <p>
 * <code>FrameBuffer</code>s are rendering surfaces allowing
 * off-screen rendering and render-to-texture functionality.
 * Instead of the scene rendering to the screen, it is rendered into the 
 * FrameBuffer, the result can be either a texture or a buffer.
 * <p>
 * A <code>FrameBuffer</code> supports two methods of rendering, 
 * using a {@link Texture} or using a buffer. 
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
 * {@link Renderer#copyFrameBuffer(com.jme3.texture.FrameBuffer, com.jme3.texture.FrameBuffer) }.
 * The content of a {@link RenderBuffer} can be retrieved by using 
 * {@link Renderer#readFrameBuffer(com.jme3.texture.FrameBuffer, java.nio.ByteBuffer) }.
 * <p>
 * <code>FrameBuffer</code>s have several attachment points, there are 
 * several <em>color</em> attachment points and a single <em>depth</em> 
 * attachment point.
 * The color attachment points support image formats such as
 * {@link Format#RGBA8}, allowing rendering the color content of the scene.
 * The depth attachment point requires a depth image format. 
 * 
 * @see Renderer#setFrameBuffer(com.jme3.texture.FrameBuffer) 
 * 
 * @author Kirill Vainer
 */
public class FrameBuffer extends NativeObject {
    
    public static final int SLOT_UNDEF = -1;
    public static final int SLOT_DEPTH = -100;
    public static final int SLOT_DEPTH_STENCIL = -101;

    private int width = 0;
    private int height = 0;
    private int samples = 1;
    private ArrayList<RenderBuffer> colorBufs = new ArrayList<RenderBuffer>();
    private RenderBuffer depthBuf = null;
    private int colorBufIndex = 0;
    private boolean srgb;

    /**
     * <code>RenderBuffer</code> represents either a texture or a 
     * buffer that will be rendered to. <code>RenderBuffer</code>s
     * are attached to an attachment slot on a <code>FrameBuffer</code>.
     */
    public class RenderBuffer {

        Texture tex;
        Image.Format format;
        int id = -1;
        int slot = SLOT_UNDEF;
        int face = -1;
        int layer = -1;
        
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
        public Texture getTexture(){
            return tex;
        }

        /**
         * Do not use.
         */
        public int getId() {
            return id;
        }

        /**
         * Do not use.
         */
        public void setId(int id){
            this.id = id;
        }

        /**
         * Do not use.
         */
        public int getSlot() {
            return slot;
        }
        
        public int getFace() {
            return face;
        }

        public void resetObject(){
            id = -1;
        }

        public RenderBuffer createDestructableClone(){
            if (tex != null){
                return null;
            }else{
                RenderBuffer destructClone =  new RenderBuffer();
                destructClone.id = id;
                return destructClone;
            }
        }

        @Override
        public String toString(){
            if (tex != null){
                return "TextureTarget[format=" + format + "]";
            }else{
                return "BufferTarget[format=" + format + "]";
            }
        }

        public int getLayer() {
            return this.layer;
        }
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
     * framebuffer, or 1 if the framebuffer should be singlesampled.
     * 
     * @throws IllegalArgumentException If width or height are not positive.
     */
    public FrameBuffer(int width, int height, int samples){
        super();
        if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("FrameBuffer must have valid size.");

        this.width = width;
        this.height = height;
        this.samples = samples == 0 ? 1 : samples;
    }

    protected FrameBuffer(FrameBuffer src){
        super(src.id);
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
     * Enables the use of a depth buffer for this <code>FrameBuffer</code>.
     * 
     * @param format The format to use for the depth buffer.
     * @throws IllegalArgumentException If <code>format</code> is not a depth format.
     */
    public void setDepthBuffer(Image.Format format){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        if (!format.isDepthFormat())
            throw new IllegalArgumentException("Depth buffer format must be depth.");
            
        depthBuf = new RenderBuffer();
        depthBuf.slot = format.isDepthStencilFormat() ?  SLOT_DEPTH_STENCIL : SLOT_DEPTH;
        depthBuf.format = format;
    }

    /**
     * Enables the use of a color buffer for this <code>FrameBuffer</code>.
     * 
     * @param format The format to use for the color buffer.
     * @throws IllegalArgumentException If <code>format</code> is not a color format.
     */
    public void setColorBuffer(Image.Format format){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        if (format.isDepthFormat())
            throw new IllegalArgumentException("Color buffer format must be color/luminance.");
        
        RenderBuffer colorBuf = new RenderBuffer();
        colorBuf.slot = 0;
        colorBuf.format = format;
        
        colorBufs.clear();
        colorBufs.add(colorBuf);
    }

    private void checkSetTexture(Texture tex, boolean depth){
        Image img = tex.getImage();
        if (img == null)
            throw new IllegalArgumentException("Texture not initialized with RTT.");

        if (depth && !img.getFormat().isDepthFormat())
            throw new IllegalArgumentException("Texture image format must be depth.");
        else if (!depth && img.getFormat().isDepthFormat())
            throw new IllegalArgumentException("Texture image format must be color/luminance.");

        // check that resolution matches texture resolution
        if (width != img.getWidth() || height != img.getHeight())
            throw new IllegalArgumentException("Texture image resolution " +
                                               "must match FB resolution");

        if (samples != tex.getImage().getMultiSamples())
            throw new IllegalStateException("Texture samples must match framebuffer samples");
    }

    /**
     * If enabled, any shaders rendering into this <code>FrameBuffer</code>
     * will be able to write several results into the renderbuffers
     * by using the <code>gl_FragData</code> array. Every slot in that
     * array maps into a color buffer attached to this framebuffer.
     * 
     * @param enabled True to enable MRT (multiple rendering targets).
     */
    public void setMultiTarget(boolean enabled){
        if (enabled) colorBufIndex = -1;
        else colorBufIndex = 0;
    }

    /**
     * @return True if MRT (multiple rendering targets) is enabled.
     * @see FrameBuffer#setMultiTarget(boolean)
     */
    public boolean isMultiTarget(){
        return colorBufIndex == -1;
    }
    
    /**
     * If MRT is not enabled ({@link FrameBuffer#setMultiTarget(boolean) } is false)
     * then this specifies the color target to which the scene should be rendered.
     * <p>
     * By default the value is 0.
     * 
     * @param index The color attachment index.
     * @throws IllegalArgumentException If index is negative or doesn't map
     * to any attachment on this framebuffer.
     */
    public void setTargetIndex(int index){
        if (index < 0 || index >= 16)
            throw new IllegalArgumentException("Target index must be between 0 and 16");

        if (colorBufs.size() < index)
            throw new IllegalArgumentException("The target at " + index + " is not set!");

        colorBufIndex = index;
        setUpdateNeeded();
    }

    /**
     * @return The color target to which the scene should be rendered.
     * 
     * @see FrameBuffer#setTargetIndex(int) 
     */
    public int getTargetIndex(){
        return colorBufIndex;
    }

    /**
     * Set the color texture to use for this framebuffer.
     * This automatically clears all existing textures added previously
     * with {@link FrameBuffer#addColorTexture } and adds this texture as the
     * only target.
     * 
     * @param tex The color texture to set.
     */
    public void setColorTexture(Texture2D tex){
        clearColorTargets();
        addColorTexture(tex);
    }
    
    /**
     * Set the color texture array to use for this framebuffer.
     * This automatically clears all existing textures added previously
     * with {@link FrameBuffer#addColorTexture } and adds this texture as the
     * only target.
     * 
     * @param tex The color texture array to set.
     */
    public void setColorTexture(TextureArray tex, int layer){
        clearColorTargets();
        addColorTexture(tex, layer);
    }
    
    /**
     * Set the color texture to use for this framebuffer.
     * This automatically clears all existing textures added previously
     * with {@link FrameBuffer#addColorTexture } and adds this texture as the
     * only target.
     *
     * @param tex The cube-map texture to set.
     * @param face The face of the cube-map to render to.
     */
    public void setColorTexture(TextureCubeMap tex, TextureCubeMap.Face face) {
        clearColorTargets();
        addColorTexture(tex, face);
    }

    /**
     * Clears all color targets that were set or added previously.
     */
    public void clearColorTargets(){
        colorBufs.clear();
    }

	/**
     * Add a color buffer without a texture bound to it.
     * If MRT is enabled, then each subsequently added texture or buffer can be
     * rendered to through a shader that writes to the array <code>gl_FragData</code>.
     * If MRT is not enabled, then the index set with {@link FrameBuffer#setTargetIndex(int) }
     * is rendered to by the shader.
     * 
     * @param format the format of the color buffer
	 * @see #addColorTexture(com.jme3.texture.Texture2D) 
     */
	public void addColorBuffer(Image.Format format){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        if (format.isDepthFormat())
            throw new IllegalArgumentException("Color buffer format must be color/luminance.");
        
        RenderBuffer colorBuf = new RenderBuffer();
        colorBuf.slot = colorBufs.size();
        colorBuf.format = format;
        
        colorBufs.add(colorBuf);
    }
	
    /**
     * Add a color texture to use for this framebuffer.
     * If MRT is enabled, then each subsequently added texture can be
     * rendered to through a shader that writes to the array <code>gl_FragData</code>.
     * If MRT is not enabled, then the index set with {@link FrameBuffer#setTargetIndex(int) }
     * is rendered to by the shader.
     * 
     * @param tex The texture to add.
	 * @see #addColorBuffer(com.jme3.texture.Image.Format) 
     */
    public void addColorTexture(Texture2D tex) {
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, false);

        RenderBuffer colorBuf = new RenderBuffer();
        colorBuf.slot = colorBufs.size();
        colorBuf.tex = tex;
        colorBuf.format = img.getFormat();

        colorBufs.add(colorBuf);
    }
    
    /**
     * Add a color texture array to use for this framebuffer.
     * If MRT is enabled, then each subsequently added texture can be
     * rendered to through a shader that writes to the array <code>gl_FragData</code>.
     * If MRT is not enabled, then the index set with {@link FrameBuffer#setTargetIndex(int) }
     * is rendered to by the shader.
     * 
     * @param tex The texture array to add.
     */
    public void addColorTexture(TextureArray tex, int layer) {
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, false);

        RenderBuffer colorBuf = new RenderBuffer();
        colorBuf.slot = colorBufs.size();
        colorBuf.tex = tex;
        colorBuf.format = img.getFormat();
        colorBuf.layer = layer;

        colorBufs.add(colorBuf);
    }
    
     /**
     * Add a color texture to use for this framebuffer.
     * If MRT is enabled, then each subsequently added texture can be
     * rendered to through a shader that writes to the array <code>gl_FragData</code>.
     * If MRT is not enabled, then the index set with {@link FrameBuffer#setTargetIndex(int) }
     * is rendered to by the shader.
     *
     * @param tex The cube-map texture to add.
     * @param face The face of the cube-map to render to.
     */
    public void addColorTexture(TextureCubeMap tex, TextureCubeMap.Face face) {
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, false);

        RenderBuffer colorBuf = new RenderBuffer();
        colorBuf.slot = colorBufs.size();
        colorBuf.tex = tex;
        colorBuf.format = img.getFormat();
        colorBuf.face = face.ordinal();

        colorBufs.add(colorBuf);
    }

    /**
     * Set the depth texture to use for this framebuffer.
     * 
     * @param tex The color texture to set.
     */
    public void setDepthTexture(Texture2D tex){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, true);
        
        depthBuf = new RenderBuffer();
        depthBuf.slot = img.getFormat().isDepthStencilFormat() ?  SLOT_DEPTH_STENCIL : SLOT_DEPTH;
        depthBuf.tex = tex;
        depthBuf.format = img.getFormat();
    }
    public void setDepthTexture(TextureArray tex, int layer){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, true);
        
        depthBuf = new RenderBuffer();
        depthBuf.slot = img.getFormat().isDepthStencilFormat() ?  SLOT_DEPTH_STENCIL : SLOT_DEPTH;
        depthBuf.tex = tex;
        depthBuf.format = img.getFormat();
        depthBuf.layer = layer;
    }
    
    /**
     * @return The number of color buffers attached to this texture. 
     */
    public int getNumColorBuffers(){
        return colorBufs.size();
    }

    /**
     * @param index
     * @return The color buffer at the given index.
     */
    public RenderBuffer getColorBuffer(int index){
        return colorBufs.get(index);
    }

    /**
     * @return The color buffer with the index set by {@link #setTargetIndex(int), or null
     * if no color buffers are attached.
	 * If MRT is disabled, the first color buffer is returned.
     */
    public RenderBuffer getColorBuffer() {
        if (colorBufs.isEmpty())
            return null;
        if (colorBufIndex<0 || colorBufIndex>=colorBufs.size()) {
			return colorBufs.get(0);
		}
        return colorBufs.get(colorBufIndex);
    }

    /**
     * @return The depth buffer attached to this FrameBuffer, or null
     * if no depth buffer is attached
     */
    public RenderBuffer getDepthBuffer() {
        return depthBuf;
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
     * 1 if this is a singlesampled framebuffer.
     */
    public int getSamples() {
        return samples;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        String mrtStr = colorBufIndex >= 0 ? "" + colorBufIndex : "mrt";
        sb.append("FrameBuffer[format=").append(width).append("x").append(height)
          .append("x").append(samples).append(", drawBuf=").append(mrtStr).append("]\n");
        if (depthBuf != null)
            sb.append("Depth => ").append(depthBuf).append("\n");
        for (RenderBuffer colorBuf : colorBufs){
            sb.append("Color(").append(colorBuf.slot)
              .append(") => ").append(colorBuf).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void resetObject() {
        this.id = -1;
        
        for (int i = 0; i < colorBufs.size(); i++) {
            colorBufs.get(i).resetObject();
        }
        
        if (depthBuf != null)
            depthBuf.resetObject();

        setUpdateNeeded();
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((Renderer)rendererObject).deleteFrameBuffer(this);
    }

    public NativeObject createDestructableClone(){
        return new FrameBuffer(this);
    }
    
    @Override
    public long getUniqueId() {
        return ((long)OBJTYPE_FRAMEBUFFER << 32) | ((long)id);
    }
    
    /**
     * Specifies that the color values stored in this framebuffer are in SRGB
     * format.
     *
     * The FrameBuffer must have a texture attached with the flag
     * {@link Image#isSrgb()} set to true.
     *
     * The Renderer must expose the {@link Caps#Srgb sRGB pipeline} capability
     * for this option to take any effect.
     *
     * Rendering operations performed on this framebuffer shall undergo a linear
     * -&gt; sRGB color space conversion when this flag is enabled. If
     * {@link RenderState#getBlendMode() blending} is enabled, it will be
     * performed in linear space by first decoding the stored sRGB pixel values
     * into linear, combining with the shader result, and then converted back to
     * sRGB upon being written into the framebuffer.
     *
     * @param srgb If the framebuffer color values should be stored in sRGB
     * color space.
     *
     * @throws InvalidStateException If the texture attached to this framebuffer
     * is not sRGB.
     */
    public void setSrgb(boolean srgb) {
        this.srgb = srgb;
    }

    /**
     * Determines if this framebuffer contains SRGB data.
     *
     * @returns True if the framebuffer color values are in SRGB space, false if
     * in linear space.
     */
    public boolean isSrgb() {
        return srgb;
    }

}
