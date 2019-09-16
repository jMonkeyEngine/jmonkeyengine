/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.niftygui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.scene.Geometry;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import de.lessvoid.nifty.render.BlendMode;
import de.lessvoid.nifty.spi.render.MouseCursor;
import de.lessvoid.nifty.spi.render.RenderDevice;
import de.lessvoid.nifty.spi.render.RenderFont;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.resourceloader.NiftyResourceLoader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

public class RenderDeviceJme implements RenderDevice {
    
    private NiftyJmeDisplay display;
    private RenderManager rm;
    private Renderer r;
    private HashMap<CachedTextKey, BitmapText> textCacheLastFrame = new HashMap<CachedTextKey, BitmapText>();
    private HashMap<CachedTextKey, BitmapText> textCacheCurrentFrame = new HashMap<CachedTextKey, BitmapText>();
    private final Quad quad = new Quad(1, -1, true);
    private final Geometry quadGeom = new Geometry("nifty-quad", quad);
    private boolean clipWasSet = false;
    private VertexBuffer quadDefaultTC = quad.getBuffer(Type.TexCoord);
    private VertexBuffer quadModTC = quadDefaultTC.clone();
    private VertexBuffer quadColor;
    private Matrix4f tempMat = new Matrix4f();
    private ColorRGBA tempColor = new ColorRGBA();
    private RenderState renderState = new RenderState();
    
    private Material colorMaterial;
    private Material textureColorMaterial;
    private Material vertexColorMaterial;
    
    private static class CachedTextKey {
        
        BitmapFont font;
        String text;
//        ColorRGBA color;
        
        public CachedTextKey(BitmapFont font, String text/*, ColorRGBA color*/) {
            this.font = font;
            this.text = text;
//            this.color = color;
        }
        
        @Override
        public boolean equals(Object other) {
            CachedTextKey otherKey = (CachedTextKey) other;
            return font.equals(otherKey.font) &&
                   text.equals(otherKey.text)/* &&
                   color.equals(otherKey.color)*/;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + font.hashCode();
            hash = 53 * hash + text.hashCode();
//            hash = 53 * hash + color.hashCode();
            return hash;
        }
    }
    
    public RenderDeviceJme(NiftyJmeDisplay display) {
        this.display = display;
        
        quadColor = new VertexBuffer(Type.Color);
        quadColor.setNormalized(true);
        ByteBuffer bb = BufferUtils.createByteBuffer(4 * 4);
        quadColor.setupData(Usage.Stream, 4, Format.UnsignedByte, bb);
        quad.setBuffer(quadColor);
        
        quadModTC.setUsage(Usage.Stream);
        
        // Load the 3 material types separately to avoid
        // reloading the shader when the defines change.
        
        // Material with a single color (no texture or vertex color)
        colorMaterial = new Material(display.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        
        // Material with a texture and a color (no vertex color)
        textureColorMaterial = new Material(display.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        
        // Material with vertex color, used for gradients (no texture)
        vertexColorMaterial = new Material(display.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        vertexColorMaterial.setBoolean("VertexColor", true);
        
        // Shared render state for all materials
        renderState.setDepthTest(false);
        renderState.setDepthWrite(false);
    }
    
    public void setResourceLoader(NiftyResourceLoader niftyResourceLoader) {
    }
    
    public void setRenderManager(RenderManager rm) {
        this.rm = rm;
        this.r = rm.getRenderer();
    }

    // TODO: Cursor support
    public MouseCursor createMouseCursor(String str, int x, int y) {
        return new MouseCursor() {
            public void dispose() {
            }

            @Override
            public void enable() {
            }

            @Override
            public void disable() {
            }
        };
    }
    
    public void enableMouseCursor(MouseCursor cursor) {
    }
    
    public void disableMouseCursor() {
    }
    
    public RenderImage createImage(String filename, boolean linear) {
        //System.out.println("createImage(" + filename + ", " + linear + ")");
        return new RenderImageJme(filename, linear, display);
    }
    
    public RenderFont createFont(String filename) {
        return new RenderFontJme(filename, display);
    }
    
    public void beginFrame() {
    }
    
    public void endFrame() {
        HashMap<CachedTextKey, BitmapText> temp = textCacheLastFrame;
        textCacheLastFrame = textCacheCurrentFrame;
        textCacheCurrentFrame = temp;
        textCacheCurrentFrame.clear();
        rm.setForcedRenderState(null);
    }
    
    public int getWidth() {
        return display.getWidth();
    }
    
    public int getHeight() {
        return display.getHeight();
    }
    
    public void clear() {
    }
    
    public void setBlendMode(BlendMode blendMode) {
        renderState.setBlendMode(convertBlend(blendMode));
    }
    
    private RenderState.BlendMode convertBlend(BlendMode blendMode) {
        if (blendMode == null) {
            return RenderState.BlendMode.Off;
        } else if (blendMode == BlendMode.BLEND) {
            return RenderState.BlendMode.Alpha;
        } else if (blendMode == BlendMode.MULIPLY) {
            return RenderState.BlendMode.Alpha;
        } else {
            throw new UnsupportedOperationException();
        }
    }
    
    private int convertColor(Color color) {
        int color2 = 0;
        color2 |= ((int) (255.0 * color.getAlpha())) << 24;
        color2 |= ((int) (255.0 * color.getBlue())) << 16;
        color2 |= ((int) (255.0 * color.getGreen())) << 8;
        color2 |= ((int) (255.0 * color.getRed()));
        return color2;
    }
    
    private ColorRGBA convertColor(Color inColor, ColorRGBA outColor) {
        return outColor.set(inColor.getRed(), inColor.getGreen(), inColor.getBlue(), inColor.getAlpha());
    }

    @Override
    public void renderFont(RenderFont font, String str, int x, int y, Color color, float sizeX, float sizeY) {        
        if (str.length() == 0) {
            return;
        }
        
        RenderFontJme jmeFont = (RenderFontJme) font;
        
        ColorRGBA colorRgba = convertColor(color, tempColor);
        CachedTextKey key = new CachedTextKey(jmeFont.getFont(), str/*, colorRgba*/);
        BitmapText text = textCacheLastFrame.get(key);
        if (text == null) {
            text = jmeFont.createText();
            text.setText(str);
            text.updateLogicalState(0);
        }
        textCacheCurrentFrame.put(key, text);
        
//        float width = text.getLineWidth();
//        float height = text.getLineHeight();
        float x0 = x; //+ 0.5f * width * (1f - sizeX);
        float y0 = y; // + 0.5f * height * (1f - sizeY);
        
        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(sizeX, sizeY, 0);

        rm.setWorldMatrix(tempMat);
        rm.setForcedRenderState(renderState);
        text.setColor(colorRgba);
        text.updateLogicalState(0);
        text.render(rm, colorRgba);
        
//        System.out.format("renderFont(%s, %s, %d, %d, %s, %f, %f)\n", jmeFont.getFont(), str, x, y, color.toString(), sizeX, sizeY);
    }
    
    public void renderImage(RenderImage image, int x, int y, int w, int h,
            int srcX, int srcY, int srcW, int srcH,
            Color color, float scale,
            int centerX, int centerY) {
        
        RenderImageJme jmeImage = (RenderImageJme) image;
        Texture2D texture = jmeImage.getTexture();
        
        textureColorMaterial.setColor("Color", convertColor(color, tempColor));
        textureColorMaterial.setTexture("ColorMap", texture);        
        
        float imageWidth = jmeImage.getWidth();
        float imageHeight = jmeImage.getHeight();
        FloatBuffer texCoords = (FloatBuffer) quadModTC.getData();
        
        float startX = srcX / imageWidth;
        float startY = srcY / imageHeight;
        float endX = startX + (srcW / imageWidth);
        float endY = startY + (srcH / imageHeight);
        
        startY = 1f - startY;
        endY = 1f - endY;
        
        texCoords.rewind();
        texCoords.put(startX).put(startY);
        texCoords.put(endX).put(startY);
        texCoords.put(endX).put(endY);
        texCoords.put(startX).put(endY);
        texCoords.flip();
        quadModTC.updateData(texCoords);
        
        quad.clearBuffer(Type.TexCoord);
        quad.setBuffer(quadModTC);
        
        float x0 = centerX + (x - centerX) * scale;
        float y0 = centerY + (y - centerY) * scale;
        
        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(w * scale, h * scale, 0);
        
        rm.setWorldMatrix(tempMat);
        rm.setForcedRenderState(renderState);
        textureColorMaterial.render(quadGeom, rm);
        
        //System.out.format("renderImage2(%s, %d, %d, %d, %d, %d, %d, %d, %d, %s, %f, %d, %d)\n", texture.getKey().toString(),
        //                                                                                       x, y, w, h, srcX, srcY, srcW, srcH,
        //                                                                                       color.toString(), scale, centerX, centerY);
    }
    
    public void renderImage(RenderImage image, int x, int y, int width, int height,
            Color color, float imageScale) {
        
        RenderImageJme jmeImage = (RenderImageJme) image;
        
        textureColorMaterial.setColor("Color", convertColor(color, tempColor));
        textureColorMaterial.setTexture("ColorMap", jmeImage.getTexture());
        
        quad.clearBuffer(Type.TexCoord);
        quad.setBuffer(quadDefaultTC);
        
        float x0 = x + 0.5f * width * (1f - imageScale);
        float y0 = y + 0.5f * height * (1f - imageScale);
        
        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(width * imageScale, height * imageScale, 0);
        
        rm.setWorldMatrix(tempMat);
        rm.setForcedRenderState(renderState);
        textureColorMaterial.render(quadGeom, rm);
        
        //System.out.format("renderImage1(%s, %d, %d, %d, %d, %s, %f)\n", jmeImage.getTexture().getKey().toString(), x, y, width, height, color.toString(), imageScale);
    }
    
    public void renderQuad(int x, int y, int width, int height, Color color) {
        //We test for alpha >0 as an optimization to prevent the render of completely transparent quads.
        //Nifty use layers that are often used for logical positionning and not rendering.
        //each layer is rendered as a quad, but that can bump up the number of geometry rendered by a lot.
        //Since we disable depth write, there is absolutely no point in rendering those quads
        //This optimization can result in a huge increase of perfs on complex Nifty UIs.
        if(color.getAlpha() >0){
            colorMaterial.setColor("Color", convertColor(color, tempColor));                        

            tempMat.loadIdentity();
            tempMat.setTranslation(x, getHeight() - y, 0);
            tempMat.setScale(width, height, 0);

            rm.setWorldMatrix(tempMat);
            rm.setForcedRenderState(renderState);
            colorMaterial.render(quadGeom, rm);
        }
        
        //System.out.format("renderQuad1(%d, %d, %d, %d, %s)\n", x, y, width, height, color.toString());
    }
    
    public void renderQuad(int x, int y, int width, int height,
            Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        
        ByteBuffer buf = (ByteBuffer) quadColor.getData();
        buf.rewind();
        
        buf.putInt(convertColor(topRight));
        buf.putInt(convertColor(topLeft));
        
        buf.putInt(convertColor(bottomLeft));
        buf.putInt(convertColor(bottomRight));
        
        buf.flip();
        quadColor.updateData(buf);
                                
        tempMat.loadIdentity();
        tempMat.setTranslation(x, getHeight() - y, 0);
        tempMat.setScale(width, height, 0);
        
        rm.setWorldMatrix(tempMat);
        rm.setForcedRenderState(renderState);
        vertexColorMaterial.render(quadGeom, rm);
        
        //System.out.format("renderQuad2(%d, %d, %d, %d, %s, %s, %s, %s)\n", x, y, width, height, topLeft.toString(),
        //                                                                                        topRight.toString(),
        //                                                                                        bottomRight.toString(),
        //                                                                                        bottomLeft.toString());
    }
    
    public void enableClip(int x0, int y0, int x1, int y1) {
        clipWasSet = true;
        r.setClipRect(x0, getHeight() - y1, x1 - x0, y1 - y0);
    }
    
    public void disableClip() {
        if (clipWasSet) {
            r.clearClipRect();
            clipWasSet = false;
        }
    }
}
