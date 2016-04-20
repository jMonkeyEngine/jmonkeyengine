/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

package jme3test.gui.opencl;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.opencl.Buffer;
import com.jme3.opencl.CommandQueue;
import com.jme3.opencl.Context;
import com.jme3.opencl.Event;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Sample 1 - how to get started with the most simple JME 3 application.
 * Display a blue 3D cube and view from all sides by
 * moving the mouse and pressing the WASD keys. */
public class HelloOpenCL extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(HelloOpenCL.class.getName());

    public static void main(String[] args){
        HelloOpenCL app = new HelloOpenCL();
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        app.setSettings(settings);
        app.start(); // start the game
    }

    @Override
    public void simpleInitApp() {
        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        Context clContext = context.getOpenCLContext();
        if (clContext == null) {
            BitmapText txt = new BitmapText(fnt);
            txt.setText("No OpenCL Context created!\nSee output log for details.");
            txt.setLocalTranslation(5, settings.getHeight() - 5, 0);
            guiNode.attachChild(txt);
            return;
        }
        CommandQueue clQueue = clContext.createQueue();
        
        StringBuilder str = new StringBuilder();
        str.append("OpenCL Context created:\n  Platform: ")
                .append(clContext.getDevices().get(0).getPlatform().getName())
                .append("\n  Devices: ").append(clContext.getDevices());
        str.append("\nTests:");
        str.append("\n  Buffers: ").append(testBuffer(clContext, clQueue));
        
        BitmapText txt1 = new BitmapText(fnt);
        txt1.setText(str.toString());
        txt1.setLocalTranslation(5, settings.getHeight() - 5, 0);
        guiNode.attachChild(txt1);
    }
    
    private boolean testBuffer(Context clContext, CommandQueue clQueue) {
        try {
            //create two buffers
            ByteBuffer h1 = BufferUtils.createByteBuffer(256);
            Buffer b1 = clContext.createBuffer(256);
            ByteBuffer h2 = BufferUtils.createByteBuffer(256);
            Buffer b2 = clContext.createBuffer(256);

            //fill buffer
            h2.rewind();
            for (int i=0; i<256; ++i) {
                h2.put((byte)i);
            }
            h2.rewind();
            b2.write(clQueue, h2);
            
            //copy b2 to b1
            b2.copyTo(clQueue, b1);
            
            //read buffer
            h1.rewind();
            b1.read(clQueue, h1);
            h1.rewind();
            for (int i=0; i<256; ++i) {
                byte b = h1.get();
                if (b != (byte)i) {
                    System.err.println("Wrong byte read: expected="+i+", actual="+b);
                    return false;
                }
            }
            
            //read buffer with offset
            int low = 26;
            int high = 184;
            h1.position(5);
            Event event = b1.readAsync(clQueue, h1, high-low, low);
            event.waitForFinished();
            h1.position(5);
            for (int i=0; i<high-low; ++i) {
                byte b = h1.get();
                if (b != (byte)(i+low)) {
                    System.err.println("Wrong byte read: expected="+(i+low)+", actual="+b);
                    return false;
                }
            }
        
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Buffer test failed with:", ex);
            return false;
        }
        return true;
    }
}