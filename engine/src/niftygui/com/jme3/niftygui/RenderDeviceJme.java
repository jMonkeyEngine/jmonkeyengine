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

package com.jme3.niftygui;

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
import de.lessvoid.nifty.elements.render.TextRenderer.RenderFontNull;
import de.lessvoid.nifty.render.BlendMode;
import de.lessvoid.nifty.spi.render.*;
import de.lessvoid.nifty.tools.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class RenderDeviceJme implements RenderDevice {

    private NiftyJmeDisplay display;
    private RenderManager rm;
    private Renderer r;

    private final Quad quad = new Quad(1, -1, true);
    private final Geometry quadGeom = new Geometry("nifty-quad", quad);
    private final Material niftyMat;

    private boolean clipWasSet = false;
    private BlendMode blendMode = null;

    private VertexBuffer quadDefaultTC = quad.getBuffer(Type.TexCoord);
    private VertexBuffer quadModTC = quadDefaultTC.clone();
    private VertexBuffer quadColor;

    private Matrix4f tempMat = new Matrix4f();
    private ColorRGBA tempColor = new ColorRGBA();

    public RenderDeviceJme(NiftyJmeDisplay display){
        this.display = display;

        quadColor = new VertexBuffer(Type.Color);
        quadColor.setNormalized(true);
        ByteBuffer bb = BufferUtils.createByteBuffer(4 * 4);
        quadColor.setupData(Usage.Stream, 4, Format.UnsignedByte, bb);
        quad.setBuffer(quadColor);

        quadModTC.setUsage(Usage.Stream);

        niftyMat = new Material(display.getAssetManager(), "Common/MatDefs/Nifty/Nifty.j3md");
        niftyMat.getAdditionalRenderState().setDepthTest(false);
    }

    public void setRenderManager(RenderManager rm){
        this.rm = rm;
        this.r = rm.getRenderer();
    }

    // TODO: Cursor support
    public MouseCursor createMouseCursor(String str, int x, int y){
        return new MouseCursor() {
            public void dispose() {
            }
        };
    }

    public void enableMouseCursor(MouseCursor cursor){
    }

    public void disableMouseCursor(){
    }

    public RenderImage createImage(String filename, boolean linear) {
        return new RenderImageJme(filename, linear, display);
    }

    public RenderFont createFont(String filename) {
        return new RenderFontJme(filename, display);
    }

    public void beginFrame() {
    }

    public void endFrame() {
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
        if (this.blendMode != blendMode){
            this.blendMode = blendMode;
        }
    }

    private RenderState.BlendMode convertBlend(){
        if (blendMode == null)
            return RenderState.BlendMode.Off;
        else if (blendMode == BlendMode.BLEND)
            return RenderState.BlendMode.Alpha;
        else if (blendMode == BlendMode.MULIPLY)
            return RenderState.BlendMode.Modulate;
        else
            throw new UnsupportedOperationException();
    }

    private int convertColor(Color color){
        int color2 = 0;
        color2 |= ((int)(255.0 * color.getAlpha())) << 24;
        color2 |= ((int)(255.0 * color.getBlue())) << 16;
        color2 |= ((int)(255.0 * color.getGreen())) << 8;
        color2 |= ((int)(255.0 * color.getRed()));
        return color2;
    }

    private ColorRGBA convertColor(Color inColor, ColorRGBA outColor){
        return outColor.set(inColor.getRed(), inColor.getGreen(), inColor.getBlue(), inColor.getAlpha());
    }

    private void setColor(Color color){
        ByteBuffer buf = (ByteBuffer) quadColor.getData();
        buf.rewind();

        int color2 = convertColor(color);
        buf.putInt(color2);
        buf.putInt(color2);
        buf.putInt(color2);
        buf.putInt(color2);

        buf.flip();
        quadColor.updateData(buf);
    }

    public void renderFont(RenderFont font, String str, int x, int y, Color color, float size){
        if (str.length() == 0)
            return;

        if (font instanceof RenderFontNull)
            return;

        RenderFontJme jmeFont = (RenderFontJme) font;
        BitmapText text = jmeFont.getText();

        // WARNING: Not compatible with OpenGL1 implementations..
        niftyMat.setColor("Color", convertColor(color, tempColor));
        
        niftyMat.setBoolean("UseTex", true);
        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        text.setMaterial(niftyMat);

        text.setText(str);
        text.updateLogicalState(0);

        float width = text.getLineWidth();
        float height = text.getLineHeight();

        float x0 = x + 0.5f * width  * (1f - size);
        float y0 = y + 0.5f * height * (1f - size);

        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(size, size, 0);

        rm.setWorldMatrix(tempMat);
        text.render(rm);
    }

    public void renderImage(RenderImage image, int x, int y, int w, int h,
                            int srcX, int srcY, int srcW, int srcH,
                            Color color, float scale,
                            int centerX, int centerY){
        RenderImageJme jmeImage = (RenderImageJme) image;
        Texture2D texture = jmeImage.getTexture();

        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setColor("Color", ColorRGBA.White);
        niftyMat.setTexture("Texture", texture);
        niftyMat.setBoolean("UseTex", true);
        setColor(color);

        float imageWidth  = jmeImage.getWidth();
        float imageHeight = jmeImage.getHeight();
        FloatBuffer texCoords = (FloatBuffer) quadModTC.getData();

        float startX = srcX / imageWidth;
        float startY = srcY / imageHeight;
        float endX   = startX + (srcW / imageWidth);
        float endY   = startY + (srcH / imageHeight);

        startY = 1f - startY;
        endY   = 1f - endY;

        texCoords.rewind();
        texCoords.put(startX).put(startY);
        texCoords.put(endX)  .put(startY);
        texCoords.put(endX)  .put(endY);
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
        niftyMat.render(quadGeom, rm);
    }

    public void renderImage(RenderImage image, int x, int y, int width, int height,
                       Color color, float imageScale){

        RenderImageJme jmeImage = (RenderImageJme) image;

        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setColor("Color", ColorRGBA.White);
        niftyMat.setTexture("Texture", jmeImage.getTexture());
        niftyMat.setBoolean("UseTex", true);
        setColor(color);

        quad.clearBuffer(Type.TexCoord);
        quad.setBuffer(quadDefaultTC);

        float x0 = x + 0.5f * width  * (1f - imageScale);
        float y0 = y + 0.5f * height * (1f - imageScale);

        tempMat.loadIdentity();
        tempMat.setTranslation(x0, getHeight() - y0, 0);
        tempMat.setScale(width * imageScale, height * imageScale, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(quadGeom, rm);
    }

    public void renderQuad(int x, int y, int width, int height, Color color){
        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setColor("Color", ColorRGBA.White);
        niftyMat.clearParam("Texture");
        niftyMat.setBoolean("UseTex", false);
        setColor(color);

        tempMat.loadIdentity();
        tempMat.setTranslation(x, getHeight() - y, 0);
        tempMat.setScale(width, height, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(quadGeom, rm);
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
        
        niftyMat.getAdditionalRenderState().setBlendMode(convertBlend());
        niftyMat.setColor("Color", ColorRGBA.White);
        niftyMat.clearParam("Texture");
        niftyMat.setBoolean("UseTex", false);

        tempMat.loadIdentity();
        tempMat.setTranslation(x, getHeight() - y, 0);
        tempMat.setScale(width, height, 0);

        rm.setWorldMatrix(tempMat);
        niftyMat.render(quadGeom, rm);
    }

    public void enableClip(int x0, int y0, int x1, int y1){
        clipWasSet = true;
        r.setClipRect(x0, getHeight() - y1, x1 - x0, y1 - y0);
    }

    public void disableClip() {
        if (clipWasSet){
            r.clearClipRect();
            clipWasSet = false;
        }
    }

}
