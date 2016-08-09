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

package jme3test.opencl;

import com.jme3.app.SimpleApplication;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector2f;
import com.jme3.opencl.*;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.logging.Logger;

/**
 * This test class tests the capability to write to a GL texture from OpenCL.
 * Move the mouse around while pressing the left mouse key to modify the fractal.
 * 
 * In addition, this test shows how to use {@link ProgramCache}.
 * 
 * @author shaman
 */
public class TestWriteToTexture extends SimpleApplication implements AnalogListener, ActionListener {
    private static final Logger LOG = Logger.getLogger(TestWriteToTexture.class.getName());
    private static final float MOUSE_SPEED = 0.5f;
    
    private Texture2D tex;
    private int initCounter;
    private Context clContext;
    private CommandQueue clQueue;
    private ProgramCache programCache;
    private Kernel kernel;
    private Vector2f C;
    private Image texCL;
    private boolean dragging;

    public static void main(String[] args){
        TestWriteToTexture app = new TestWriteToTexture();
        AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(false);
//        settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE);
        app.setSettings(settings);
        app.start(); // start the game
    }

    @Override
    public void simpleInitApp() {
        initOpenCL1();
        
        tex = new Texture2D(settings.getWidth(), settings.getHeight(), 1, com.jme3.texture.Image.Format.RGBA8);
        Picture pic = new Picture("julia");
        pic.setTexture(assetManager, tex, true);
        pic.setPosition(0, 0);
        pic.setWidth(settings.getWidth());
        pic.setHeight(settings.getHeight());
        guiNode.attachChild(pic);
        
        initCounter = 0;
        
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        inputManager.addMapping("right", new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping("left", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("up", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("down", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("drag", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "right", "left", "up", "down", "drag");
        dragging = false;
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        if (initCounter < 2) {
        initCounter++;
        } else if (initCounter == 2) {
            //when initCounter reaches 2, the scene was drawn once and the texture was uploaded to the GPU
            //then we can bind the texture to OpenCL
            initOpenCL2();
            updateOpenCL(tpf);
            initCounter = 3;
        } else {
            updateOpenCL(tpf);
        }
        
    }
    
    private void initOpenCL1() {
        clContext = context.getOpenCLContext();
        clQueue = clContext.createQueue().register();
        programCache = new ProgramCache(clContext);
        //create kernel
        String cacheID = getClass().getName()+".Julia";
        Program program = programCache.loadFromCache(cacheID);
        if (program == null) {
            LOG.info("Program not loaded from cache, create from sources instead");
            program = clContext.createProgramFromSourceFiles(assetManager, "jme3test/opencl/JuliaSet.cl");
            program.build();
            programCache.saveToCache(cacheID, program);
        }
        program.register();
        kernel = program.createKernel("JuliaSet").register();
        C = new Vector2f(0.12f, -0.2f);
    }
    private void initOpenCL2() {
        //bind image to OpenCL
        texCL = clContext.bindImage(tex, MemoryAccess.WRITE_ONLY).register();
    }
    private void updateOpenCL(float tpf) {
        //aquire resource
        texCL.acquireImageForSharingNoEvent(clQueue);
        //no need to wait for the returned event, since the kernel implicitely waits for it (same command queue)
        
        //execute kernel
        Kernel.WorkSize ws = new Kernel.WorkSize(settings.getWidth(), settings.getHeight());
        kernel.Run1NoEvent(clQueue, ws, texCL, C, 16);
        
        //release resource
        texCL.releaseImageForSharingNoEvent(clQueue);
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!dragging) {
            return;
        }
        if ("left".equals(name)) {
            C.x -= tpf * MOUSE_SPEED;
        } else if ("right".equals(name)) {
            C.x += tpf * MOUSE_SPEED;
        } else if ("up".equals(name)) {
            C.y -= tpf * MOUSE_SPEED;
        } else if ("down".equals(name)) {
            C.y += tpf * MOUSE_SPEED;
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if ("drag".equals(name)) {
            dragging = isPressed;
            inputManager.setCursorVisible(!isPressed);
        }
    }
}