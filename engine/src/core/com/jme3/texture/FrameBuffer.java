/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.renderer.GLObject;
import com.jme3.renderer.Renderer;
import com.jme3.texture.Image.Format;
import java.util.ArrayList;

public class FrameBuffer extends GLObject {

    private int width = 0;
    private int height = 0;
    private int samples = 1;
    private ArrayList<RenderBuffer> colorBufs = new ArrayList<RenderBuffer>();
    private RenderBuffer depthBuf = null;
    private int colorBufIndex = 0;

    public class RenderBuffer {

        Texture tex;
        Image.Format format;
        int id = -1;
        int slot = -1;

        public Format getFormat() {
            return format;
        }

        public Texture getTexture(){
            return tex;
        }

        public int getId() {
            return id;
        }

        public void setId(int id){
            this.id = id;
        }

        public int getSlot() {
            return slot;
        }

        public void setSlot(int slot) {
            this.slot = slot;
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
    }

    public FrameBuffer(int width, int height, int samples){
        super(Type.FrameBuffer);
        if (width <= 0 || height <= 0)
                throw new IllegalArgumentException("FrameBuffer must have valid size.");

        this.width = width;
        this.height = height;
        this.samples = samples == 0 ? 1 : samples;
    }

    protected FrameBuffer(FrameBuffer src){
        super(Type.FrameBuffer, src.id);
        /*
        for (RenderBuffer renderBuf : src.colorBufs){
            RenderBuffer clone = renderBuf.createDestructableClone();
            if (clone != null)
                this.colorBufs.add(clone);
        }

        this.depthBuf = src.depthBuf.createDestructableClone();
         */
    }

    public void setDepthBuffer(Image.Format format){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        if (!format.isDepthFormat())
            throw new IllegalArgumentException("Depth buffer format must be depth.");
            
        depthBuf = new RenderBuffer();
        depthBuf.slot = -100; // -100 == special slot for DEPTH_BUFFER
        depthBuf.format = format;
    }

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

    public void setMultiTarget(boolean enabled){
        if (enabled) colorBufIndex = -1;
        else colorBufIndex = 0;
    }

    public void setTargetIndex(int index){
        if (index < 0 || index >= 16)
            throw new IllegalArgumentException();

        if (colorBufs.size() >= index)
            throw new IndexOutOfBoundsException("The target at " + index + " is not set!");

        colorBufIndex = index;
    }

    public boolean isMultiTarget(){
        return colorBufIndex == -1;
    }

    public int getTargetIndex(){
        return colorBufIndex;
    }

    public void setColorTexture(Texture2D tex){
        clearColorTargets();
        addColorTexture(tex);
    }

    public void clearColorTargets(){
        colorBufs.clear();
    }

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

    public void setDepthTexture(Texture2D tex){
        if (id != -1)
            throw new UnsupportedOperationException("FrameBuffer already initialized.");

        Image img = tex.getImage();
        checkSetTexture(tex, true);
        
        depthBuf = new RenderBuffer();
        depthBuf.slot = -100; // indicates GL_DEPTH_ATTACHMENT
        depthBuf.tex = tex;
        depthBuf.format = img.getFormat();
    }

    public int getNumColorBuffers(){
        return colorBufs.size();
    }

    public RenderBuffer getColorBuffer(int index){
        return colorBufs.get(index);
    }

    public RenderBuffer getColorBuffer() {
        if (colorBufs.size() == 0)
            return null;
        
        return colorBufs.get(0);
    }

    public RenderBuffer getDepthBuffer() {
        return depthBuf;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

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
    public void deleteObject(Renderer r) {
        r.deleteFrameBuffer(this);
    }

    public GLObject createDestructableClone(){
        return new FrameBuffer(this);
    }
}
