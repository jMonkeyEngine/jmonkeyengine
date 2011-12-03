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

package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.Collection;
import java.util.logging.Logger;

public class HDRRenderer implements SceneProcessor {

    private static final int LUMMODE_NONE = 0x1,
                             LUMMODE_ENCODE_LUM = 0x2,
                             LUMMODE_DECODE_LUM = 0x3;

    private Renderer renderer;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private static final Logger logger = Logger.getLogger(HDRRenderer.class.getName());

    private Camera fbCam = new Camera(1, 1);

    private FrameBuffer msFB;

    private FrameBuffer mainSceneFB;
    private Texture2D mainScene;
    private FrameBuffer scene64FB;
    private Texture2D scene64;
    private FrameBuffer scene8FB;
    private Texture2D scene8;
    private FrameBuffer scene1FB[] = new FrameBuffer[2];
    private Texture2D scene1[] = new Texture2D[2];

    private Material hdr64;
    private Material hdr8;
    private Material hdr1;
    private Material tone;

    private Picture fsQuad;
    private float time = 0;
    private int curSrc = -1;
    private int oppSrc = -1;
    private float blendFactor = 0;

    private int numSamples = 0;
    private float exposure = 0.18f;
    private float whiteLevel = 100f;
    private float throttle = -1;
    private int maxIterations = -1;
    private Image.Format bufFormat = Format.RGB16F;

    private MinFilter fbMinFilter = MinFilter.BilinearNoMipMaps;
    private MagFilter fbMagFilter = MagFilter.Bilinear;
    private AssetManager manager;

    private boolean enabled = true;

    public HDRRenderer(AssetManager manager, Renderer renderer){
        this.manager = manager;
        this.renderer = renderer;
        
        Collection<Caps> caps = renderer.getCaps();
        if (caps.contains(Caps.PackedFloatColorBuffer))
            bufFormat = Format.RGB111110F;
        else if (caps.contains(Caps.FloatColorBuffer))
            bufFormat = Format.RGB16F;
        else{
            enabled = false;
            return;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSamples(int samples){
        this.numSamples = samples;
    }

    public void setExposure(float exp){
        this.exposure = exp;
    }

    public void setWhiteLevel(float whiteLevel){
        this.whiteLevel = whiteLevel;
    }

    public void setMaxIterations(int maxIterations){
        this.maxIterations = maxIterations;

        // regenerate shaders if needed
        if (hdr64 != null)
            createLumShaders();
    }

    public void setThrottle(float throttle){
        this.throttle = throttle;
    }

    public void setUseFastFilter(boolean fastFilter){
        if (fastFilter){
            fbMagFilter = MagFilter.Nearest;
            fbMinFilter = MinFilter.NearestNoMipMaps;
        }else{
            fbMagFilter = MagFilter.Bilinear;
            fbMinFilter = MinFilter.BilinearNoMipMaps;
        }
    }

    public Picture createDisplayQuad(/*int mode, Texture tex*/){
        if (scene64 == null)
            return null;

        Material mat = new Material(manager, "Common/MatDefs/Hdr/LogLum.j3md");
//        if (mode == LUMMODE_ENCODE_LUM)
//            mat.setBoolean("EncodeLum", true);
//        else if (mode == LUMMODE_DECODE_LUM)
            mat.setBoolean("DecodeLum", true);
            mat.setTexture("Texture", scene64);
//        mat.setTexture("Texture", tex);
        
        Picture dispQuad = new Picture("Luminance Display");
        dispQuad.setMaterial(mat);
        return dispQuad;
    }

    private Material createLumShader(int srcW, int srcH, int bufW, int bufH, int mode,
                                int iters, Texture tex){
        Material mat = new Material(manager, "Common/MatDefs/Hdr/LogLum.j3md");
        
        Vector2f blockSize = new Vector2f(1f / bufW, 1f / bufH);
        Vector2f pixelSize = new Vector2f(1f / srcW, 1f / srcH);
        Vector2f blocks = new Vector2f();
        float numPixels = Float.POSITIVE_INFINITY;
        if (iters != -1){
            do {
                pixelSize.multLocal(2);
                blocks.set(blockSize.x / pixelSize.x,
                           blockSize.y / pixelSize.y);
                numPixels = blocks.x * blocks.y;
            } while (numPixels > iters);
        }else{
            blocks.set(blockSize.x / pixelSize.x,
                       blockSize.y / pixelSize.y);
            numPixels = blocks.x * blocks.y;
        }
        System.out.println(numPixels);

        mat.setBoolean("Blocks", true);
        if (mode == LUMMODE_ENCODE_LUM)
            mat.setBoolean("EncodeLum", true);
        else if (mode == LUMMODE_DECODE_LUM)
            mat.setBoolean("DecodeLum", true);

        mat.setTexture("Texture", tex);
        mat.setVector2("BlockSize", blockSize);
        mat.setVector2("PixelSize", pixelSize);
        mat.setFloat("NumPixels", numPixels);

        return mat;
    }

    private void createLumShaders(){
        int w = mainSceneFB.getWidth();
        int h = mainSceneFB.getHeight();
        hdr64 = createLumShader(w,  h,  64, 64, LUMMODE_ENCODE_LUM, maxIterations, mainScene);
        hdr8  = createLumShader(64, 64, 8,  8,  LUMMODE_NONE,       maxIterations, scene64);
        hdr1  = createLumShader(8,  8,  1,  1,  LUMMODE_NONE,       maxIterations, scene8);
    }

    private int opposite(int i){
        return i == 1 ? 0 : 1;
    }

    private void renderProcessing(Renderer r, FrameBuffer dst, Material mat){
        if (dst == null){
            fsQuad.setWidth(mainSceneFB.getWidth());
            fsQuad.setHeight(mainSceneFB.getHeight());
            fbCam.resize(mainSceneFB.getWidth(), mainSceneFB.getHeight(), true);
        }else{
            fsQuad.setWidth(dst.getWidth());
            fsQuad.setHeight(dst.getHeight());
            fbCam.resize(dst.getWidth(), dst.getHeight(), true);
        }
        fsQuad.setMaterial(mat);
        fsQuad.updateGeometricState();
        renderManager.setCamera(fbCam, true);

        r.setFrameBuffer(dst);
        r.clearBuffers(true, true, true);
        renderManager.renderGeometry(fsQuad);
    }

    private void renderToneMap(Renderer r, FrameBuffer out){
        tone.setFloat("A", exposure);
        tone.setFloat("White", whiteLevel);
        tone.setTexture("Lum", scene1[oppSrc]);
        tone.setTexture("Lum2", scene1[curSrc]);
        tone.setFloat("BlendFactor", blendFactor);
        renderProcessing(r, out, tone);
    }

    private void updateAverageLuminance(Renderer r){
        renderProcessing(r, scene64FB, hdr64);
        renderProcessing(r, scene8FB, hdr8);
        renderProcessing(r, scene1FB[curSrc], hdr1);
    }

    public boolean isInitialized(){
        return viewPort != null;
    }

    public void reshape(ViewPort vp, int w, int h){
        if (mainSceneFB != null){
            renderer.deleteFrameBuffer(mainSceneFB);
        }

        mainSceneFB = new FrameBuffer(w, h, 1);
        mainScene = new Texture2D(w, h, bufFormat);
        mainSceneFB.setDepthBuffer(Format.Depth);
        mainSceneFB.setColorTexture(mainScene);
        mainScene.setMagFilter(fbMagFilter);
        mainScene.setMinFilter(fbMinFilter);

        if (msFB != null){
            renderer.deleteFrameBuffer(msFB);
        }

        tone.setTexture("Texture", mainScene);
        
        Collection<Caps> caps = renderer.getCaps();
        if (numSamples > 1 && caps.contains(Caps.FrameBufferMultisample)){
            msFB = new FrameBuffer(w, h, numSamples);
            msFB.setDepthBuffer(Format.Depth);
            msFB.setColorBuffer(bufFormat);
            vp.setOutputFrameBuffer(msFB);
        }else{
            if (numSamples > 1)
                logger.warning("FBO multisampling not supported on this GPU, request ignored.");

            vp.setOutputFrameBuffer(mainSceneFB);
        }

        createLumShaders();
    }

    public void initialize(RenderManager rm, ViewPort vp){
        if (!enabled)
            return;

        renderer = rm.getRenderer();
        renderManager = rm;
        viewPort = vp;

        // loadInitial()
        fsQuad = new Picture("HDR Fullscreen Quad");

        Format lumFmt = Format.RGB8;
        scene64FB = new FrameBuffer(64, 64, 1);
        scene64 = new Texture2D(64, 64, lumFmt);
        scene64FB.setColorTexture(scene64);
        scene64.setMagFilter(fbMagFilter);
        scene64.setMinFilter(fbMinFilter);

        scene8FB = new FrameBuffer(8, 8, 1);
        scene8 = new Texture2D(8, 8, lumFmt);
        scene8FB.setColorTexture(scene8);
        scene8.setMagFilter(fbMagFilter);
        scene8.setMinFilter(fbMinFilter);

        scene1FB[0] = new FrameBuffer(1, 1, 1);
        scene1[0] = new Texture2D(1, 1, lumFmt);
        scene1FB[0].setColorTexture(scene1[0]);

        scene1FB[1] = new FrameBuffer(1, 1, 1);
        scene1[1] = new Texture2D(1, 1, lumFmt);
        scene1FB[1].setColorTexture(scene1[1]);

        // prepare tonemap shader
        tone = new Material(manager, "Common/MatDefs/Hdr/ToneMap.j3md");
        tone.setFloat("A", 0.18f);
        tone.setFloat("White", 100);

        // load();
        int w = vp.getCamera().getWidth();
        int h = vp.getCamera().getHeight();
        reshape(vp, w, h);

        
    }

    public void preFrame(float tpf) {
        if (!enabled)
            return;

        time += tpf;
        blendFactor = (time / throttle);
    }

    public void postQueue(RenderQueue rq) {
    }

    public void postFrame(FrameBuffer out) {
        if (!enabled)
            return;

        if (msFB != null){
            // first render to multisampled FB
//            renderer.setFrameBuffer(msFB);
//            renderer.clearBuffers(true,true,true);
//
//            renderManager.renderViewPortRaw(viewPort);

            // render back to non-multisampled FB
            renderer.copyFrameBuffer(msFB, mainSceneFB);
        }else{
//            renderer.setFrameBuffer(mainSceneFB);
//            renderer.clearBuffers(true,true,false);
//
//            renderManager.renderViewPortRaw(viewPort);
        }

        // should we update avg lum?
        if (throttle == -1){
            // update every frame
            curSrc = 0;
            oppSrc = 0;
            blendFactor = 0;
            time = 0;
            updateAverageLuminance(renderer);
        }else{
            if (curSrc == -1){
                curSrc = 0;
                oppSrc = 0;

                // initial update
                updateAverageLuminance(renderer);

                blendFactor = 0;
                time = 0;
            }else if (time > throttle){

                // time to switch
                oppSrc = curSrc;
                curSrc = opposite(curSrc);

                updateAverageLuminance(renderer);

                blendFactor = 0;
                time = 0;
            }
        }

        // since out == mainSceneFB, tonemap into the main screen instead
        //renderToneMap(renderer, out);
        renderToneMap(renderer, null);

        renderManager.setCamera(viewPort.getCamera(), false);
    }

    public void cleanup() {
        if (!enabled)
            return;

        if (msFB != null)
            renderer.deleteFrameBuffer(msFB);
        if (mainSceneFB != null)
            renderer.deleteFrameBuffer(mainSceneFB);
        if (scene64FB != null){
            renderer.deleteFrameBuffer(scene64FB);
            renderer.deleteFrameBuffer(scene8FB);
            renderer.deleteFrameBuffer(scene1FB[0]);
            renderer.deleteFrameBuffer(scene1FB[1]);
        }
    }

}
