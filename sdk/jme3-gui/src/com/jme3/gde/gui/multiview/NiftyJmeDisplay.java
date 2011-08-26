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

package com.jme3.gde.gui.multiview;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.niftygui.RenderDeviceJme;
import com.jme3.niftygui.SoundDeviceJme;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.spi.input.InputSystem;
import de.lessvoid.nifty.tools.TimeProvider;

public class NiftyJmeDisplay extends com.jme3.niftygui.NiftyJmeDisplay implements SceneProcessor {

    public NiftyJmeDisplay(AssetManager assetManager, 
                           InputSystem inputManager,
                           AudioRenderer audioRenderer,
                           ViewPort vp){
        this.assetManager = assetManager;
        //TODO: move
        ((DesktopAssetManager)assetManager).clearCache();
        w = vp.getCamera().getWidth();
        h = vp.getCamera().getHeight();

        soundDev = new SoundDeviceJme(assetManager, audioRenderer);
        renderDev = new RenderDeviceJme(this);
        nifty = new Nifty(renderDev, soundDev, inputManager, new TimeProvider());
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
        renderDev.setRenderManager(rm);
        inited = true;
        this.vp = vp;
        this.renderer = rm.getRenderer();
        
    }

    @Override
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

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
        nifty.resolutionChanged();
    }

    @Override
    public boolean isInitialized() {
        return inited;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // render nifty before anything else
        renderManager.setCamera(vp.getCamera(), true);
        nifty.render(false);
        renderManager.setCamera(vp.getCamera(), false);
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    @Override
    public void cleanup() {
        inited = false;
//        nifty.exit();
    }

}
