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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.tools.TimeProvider;
import de.lessvoid.nifty.tools.resourceloader.ResourceLocation;
import java.io.InputStream;
import java.net.URL;

public class NiftyJmeDisplay implements SceneProcessor {

    protected boolean inited = false;
    protected Nifty nifty;
    protected AssetManager assetManager;
    protected RenderManager renderManager;
    protected RenderDeviceJme renderDev;
    protected InputSystemJme inputSys;
    protected SoundDeviceJme soundDev;
    protected Renderer renderer;
    protected ViewPort vp;
    
    protected ResourceLocationJme resourceLocation;

    protected int w, h;

    protected class ResourceLocationJme implements ResourceLocation {

        public InputStream getResourceAsStream(String path) {
            AssetKey<Object> key = new AssetKey<Object>(path);
            AssetInfo info = assetManager.locateAsset(key);
            if (info != null){
                return info.openStream();
            }else{
                throw new AssetNotFoundException(path);
            }
        }

        public URL getResource(String path) {
            throw new UnsupportedOperationException();
        }
    }

    //Empty constructor needed for jMP to create replacement input system
    public NiftyJmeDisplay() {
    }
    
    public NiftyJmeDisplay(AssetManager assetManager, 
                           InputManager inputManager,
                           AudioRenderer audioRenderer,
                           ViewPort vp){
        this.assetManager = assetManager;

        w = vp.getCamera().getWidth();
        h = vp.getCamera().getHeight();

        soundDev = new SoundDeviceJme(assetManager, audioRenderer);
        renderDev = new RenderDeviceJme(this);
        inputSys = new InputSystemJme(inputManager);
        if (inputManager != null)
            inputManager.addRawInputListener(inputSys);
        
        nifty = new Nifty(renderDev, soundDev, inputSys, new TimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme();
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
        renderDev.setRenderManager(rm);
        inited = true;
        this.vp = vp;
        this.renderer = rm.getRenderer();
        
        inputSys.setHeight(vp.getCamera().getHeight());
    }

    public Nifty getNifty() {
        return nifty;
    }

    RenderDeviceJme getRenderDevice() {
        return renderDev;
    }

    AssetManager getAssetManager() {
        return assetManager;
    }

    RenderManager getRenderManager() {
        return renderManager;
    }

    int getHeight() {
        return h;
    }

    int getWidth() {
        return w;
    }

    Renderer getRenderer(){
        return renderer;
    }

    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
        inputSys.setHeight(h);
        nifty.resolutionChanged();
    }

    public boolean isInitialized() {
        return inited;
    }

    public void preFrame(float tpf) {
    }

    public void postQueue(RenderQueue rq) {
        // render nifty before anything else
        renderManager.setCamera(vp.getCamera(), true);
        //nifty.update();
        nifty.render(false);
        renderManager.setCamera(vp.getCamera(), false);
    }

    public void postFrame(FrameBuffer out) {
    }

    public void cleanup() {
        inited = false;
//        nifty.exit();
    }

}
