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

package jme3test.app.state;

import com.jme3.app.Application;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;

public class TestAppStates extends Application {

    public static void main(String[] args){
        TestAppStates app = new TestAppStates();
        app.start();
    }

    @Override
    public void start(JmeContext.Type contextType){
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1024, 768);
        setSettings(settings);
        
        super.start(contextType);
    }

    @Override
    public void initialize(){
        super.initialize();

        System.out.println("Initialize");

        RootNodeState state = new RootNodeState();
        viewPort.attachScene(state.getRootNode());
        stateManager.attach(state);

        Spatial model = assetManager.loadModel("Models/Teapot/Teapot.obj");
        model.scale(3);
        model.setMaterial(assetManager.loadMaterial("Interface/Logo/Logo.j3m"));
        state.getRootNode().attachChild(model);

        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                                                           inputManager,
                                                           audioRenderer,
                                                           guiViewPort);
        niftyDisplay.getNifty().fromXml("Interface/Nifty/HelloJme.xml", "start");
        guiViewPort.addProcessor(niftyDisplay);
    }

    @Override
    public void update(){
        super.update();

        // do some animation
        float tpf = timer.getTimePerFrame();

        stateManager.update(tpf);
        stateManager.render(renderManager);

        // render the viewports
        renderManager.render(tpf, context.isRenderable());
    }

    @Override
    public void destroy(){
        super.destroy();

        System.out.println("Destroy");
    }
}
