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
package org.jmonkeyengine.screenshottests.gui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.Test;

/**
 * @author Richard Tingle (aka richtea)
 */
public class TestBitmapText3D extends ScreenshotTestBase{

    /**
     * This tests both that bitmap text is rendered correctly and that it is
     * wrapped correctly.
     */
    @Test
    public void testBitmapText3D(){

        screenshotTest(
                new BaseAppState(){
                    @Override
                    protected void initialize(Application app){
                        String txtB = "ABCDEFGHIKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-=_+[]\\;',./{}|:<>?";

                        AssetManager assetManager = app.getAssetManager();
                        Node rootNode = ((SimpleApplication)app).getRootNode();

                        Quad q = new Quad(6, 3);
                        Geometry g = new Geometry("quad", q);
                        g.setLocalTranslation(-1.5f, -3, -0.0001f);
                        g.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
                        rootNode.attachChild(g);

                        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
                        BitmapText txt = new BitmapText(fnt);
                        txt.setBox(new Rectangle(0, 0, 6, 3));
                        txt.setQueueBucket(RenderQueue.Bucket.Transparent);
                        txt.setSize( 0.5f );
                        txt.setText(txtB);
                        txt.setLocalTranslation(-1.5f,0,0);
                        rootNode.attachChild(txt);
                    }

                    @Override
                    protected void cleanup(Application app){}

                    @Override
                    protected void onEnable(){}

                    @Override
                    protected void onDisable(){}
                }
        ).run();


    }
}
