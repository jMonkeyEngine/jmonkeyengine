/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.simple;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.jmonkeyengine.screenshottests.testframework.TestResolution;
import org.junit.jupiter.api.Test;

/**
 * @author Richard Tingle (aka richtea)
 */
public class SimpleGreyCube extends ScreenshotTestBase{

    /**
     * This allows the tests themselves to be baselines to ensure they are getting gamma correction
     * right. This is a 50% grey cube so the output image should also be 50% grey.
     */
    @Test
    public void simpleGreyCube(){

        screenshotTest(
                new BaseAppState(){
                    @Override
                    protected void initialize(Application app){
                        AssetManager assetManager = app.getAssetManager();
                        Node rootNode = ((SimpleApplication)app).getRootNode();

                        Box box = new Box(1.5f,1.5f,1.5f);
                        Geometry g = new Geometry("box", box);
                        Material mat = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
                        mat.setColor("Color", new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
                        g.setMaterial(mat);
                        rootNode.attachChild(g);
                    }

                    @Override
                    protected void cleanup(Application app){}

                    @Override
                    protected void onEnable(){}

                    @Override
                    protected void onDisable(){}
                }
        ).setTestResolution(new TestResolution(100,100)).run();

    }
}