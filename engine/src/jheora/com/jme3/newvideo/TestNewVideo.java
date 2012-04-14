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

package com.jme3.newvideo;

import com.fluendo.jst.BusHandler;
import com.fluendo.jst.Message;
import com.fluendo.jst.Pipeline;
import com.fluendo.utils.Debug;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TestNewVideo extends SimpleApplication implements BusHandler {

    private Picture picture;
    private JmeVideoPipeline p;
    private int frame = 0;

    public static void main(String[] args){
        TestNewVideo app = new TestNewVideo();
        AppSettings settings = new AppSettings(true);
//        settings.setFrameRate(24);
        app.setSettings(settings);
        app.start();
    }

    private void createVideo(){
        Debug.level = Debug.INFO;
        p = new JmeVideoPipeline(this);
        p.getBus().addHandler(this);
        try {
            p.inputStream = new FileInputStream("E:\\VideoTest.ogv");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        p.setState(Pipeline.PLAY);
    }

    @Override
    public void simpleUpdate(float tpf){
//        if (p == null)
//            return;

        Texture2D tex = p.getTexture();
        if (tex == null)
            return;

        if (picture != null){
            synchronized (tex){
                try {
                    tex.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
                tex.getImage().setUpdateNeeded();
                renderer.setTexture(0, tex);
                ((VideoTexture)tex).free();
                System.out.println("PLAY  : " + (frame++));
            }
            return;
        }
        
        picture = new Picture("VideoPicture", true);
        picture.setPosition(0, 0);
        picture.setWidth(settings.getWidth());
        picture.setHeight(settings.getHeight());
        picture.setTexture(assetManager, tex, false);
        rootNode.attachChild(picture);
    }

    public void simpleInitApp() {
        // start video playback
        createVideo();
    }

    @Override
    public void destroy(){
        if (p != null){
            p.setState(Pipeline.STOP);
            p.shutDown();
        }
        super.destroy();
    }

    public void handleMessage(Message msg) {
        switch (msg.getType()){
            case Message.EOS:
                Debug.log(Debug.INFO, "EOS: playback ended");
                /*
                enqueue(new Callable<Void>(){
                    public Void call() throws Exception {
                        rootNode.detachChild(picture);
                        p.setState(Element.STOP);
                        p.shutDown();
                        p = null;
                        return null;
                    }
                });
                
                Texture2D tex = p.getTexture();
                synchronized (tex){
                    tex.notifyAll();
                }
                 */
                break;
            case Message.STREAM_STATUS:
                Debug.info(msg.toString());
                break;
        }
    }
}
