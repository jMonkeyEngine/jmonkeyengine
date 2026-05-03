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
package org.jmonkeyengine.screenshottests.export;

import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Richard Tingle (aka richtea)
 */
public class TestOgreConvert extends ScreenshotTestBase{

    /**
     * This tests loads an Ogre model, converts it to binary, and then reloads it.
     * <p>
     * Note that the model is animated and the animation is played back. That is why
     * two screenshots are taken
     * </p>
     */
    @Test
    public void testOgreConvert(){

        screenshotTest(
                new BaseAppState(){
                    @Override
                    protected void initialize(Application app){
                        AssetManager assetManager = app.getAssetManager();
                        Node rootNode = ((SimpleApplication)app).getRootNode();
                        Camera cam = app.getCamera();
                        Spatial ogreModel = assetManager.loadModel("Models/Oto/Oto.mesh.xml");

                        DirectionalLight dl = new DirectionalLight();
                        dl.setColor(ColorRGBA.White);
                        dl.setDirection(new Vector3f(0,-1,-1).normalizeLocal());
                        rootNode.addLight(dl);

                        cam.setLocation(new Vector3f(0, 0, 15));

                        try {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            BinaryExporter exp = new BinaryExporter();
                            exp.save(ogreModel, baos);

                            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                            BinaryImporter imp = new BinaryImporter();
                            imp.setAssetManager(assetManager);
                            Node ogreModelReloaded = (Node) imp.load(bais, null, null);

                            AnimComposer composer = ogreModelReloaded.getControl(AnimComposer.class);
                            composer.setCurrentAction("Walk");

                            rootNode.attachChild(ogreModelReloaded);
                        } catch (IOException ex){
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    protected void cleanup(Application app){}

                    @Override
                    protected void onEnable(){}

                    @Override
                    protected void onDisable(){}
                }
        )
        .setFramesToTakeScreenshotsOn(1, 5)
        .run();

    }
}
