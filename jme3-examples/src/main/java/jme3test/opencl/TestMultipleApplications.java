/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.opencl.*;
import com.jme3.system.AppSettings;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class creates multiple instances of {@link TestVertexBufferSharing}.
 * This is used to test if multiple opencl instances can run in parallel.
 * @author Sebastian Weiss
 */
public class TestMultipleApplications extends SimpleApplication {
    private static final Logger LOG = Logger.getLogger(TestMultipleApplications.class.getName());
    
    private static final Object sync = new Object();
    private static Platform selectedPlatform;
    private static List<? extends Device> availableDevices;
    private static int currentDeviceIndex;
    
    private Context clContext;
    private CommandQueue clQueue;
    private Kernel kernel;
    private Buffer buffer;
    private boolean failed;
    
    private BitmapText infoText;
    private BitmapText statusText;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final AppSettings settings = new AppSettings(true);
        settings.setOpenCLSupport(true);
        settings.setVSync(true);
        settings.setOpenCLPlatformChooser(CustomPlatformChooser.class);
        settings.setRenderer(AppSettings.JOGL_OPENGL_FORWARD_COMPATIBLE);
        for (int i=0; i<2; ++i) {
            new Thread() {
                public void run() {
                    if (currentDeviceIndex == -1) {
                        return;
                    }
                    TestMultipleApplications app = new TestMultipleApplications();
                    app.setSettings(settings);
                    app.setShowSettings(false);
                    app.start();
                }
            }.start();
        }
    }
    
    public static class CustomPlatformChooser implements PlatformChooser {

        public CustomPlatformChooser() {}
        
        @Override
        public List<? extends Device> chooseDevices(List<? extends Platform> platforms) {
            synchronized(sync) {
            if (currentDeviceIndex == -1) {
                return Collections.emptyList();
            }

            Platform platform = platforms.get(0);
            availableDevices = platform.getDevices();
            selectedPlatform = platform;
            
            Device device = platform.getDevices().get(currentDeviceIndex);
            currentDeviceIndex ++;
            if (currentDeviceIndex >= availableDevices.size()) {
                currentDeviceIndex = -1;
            }
            
            return Collections.singletonList(device);
            }
        }
        
    }
    
    @Override
    public void simpleInitApp() {
        clContext = context.getOpenCLContext();
        if (clContext == null) {
            LOG.severe("No OpenCL context found");
            stop();
            return;
        }
        Device device = clContext.getDevices().get(0);
        clQueue = clContext.createQueue(device);
        clQueue.register();
        
        String source = ""
                + "__kernel void Fill(__global float* vb, float v)\n"
                + "{\n"
                + "  int idx = get_global_id(0);\n"
                + "  vb[idx] = v;\n"
                + "}\n";
        Program program = clContext.createProgramFromSourceCode(source);
        program.build();
        program.register();
        kernel = program.createKernel("Fill");
        kernel.register();
        
        buffer = clContext.createBuffer(4);
        buffer.register();
        
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);
        
        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        infoText = new BitmapText(fnt, false);
        //infoText.setBox(new Rectangle(0, 0, settings.getWidth(), settings.getHeight()));
        infoText.setText("Device: "+clContext.getDevices());
        infoText.setLocalTranslation(0, settings.getHeight(), 0);
        guiNode.attachChild(infoText);
        statusText = new BitmapText(fnt, false);
        //statusText.setBox(new Rectangle(0, 0, settings.getWidth(), settings.getHeight()));
        statusText.setText("Running");
        statusText.setLocalTranslation(0, settings.getHeight() - infoText.getHeight() - 2, 0);
        guiNode.attachChild(statusText);
    }

    @Override
    public void simpleUpdate(float tpf) {
        //call kernel to test if it is still working
        if (!failed) {
            try {
                kernel.Run1NoEvent(clQueue, new Kernel.WorkSize(1), buffer, 1.0f);
            } catch (OpenCLException ex) {
                LOG.log(Level.SEVERE, "Kernel call not working anymore", ex);
                failed = true;
                statusText.setText("Failed");
            }
        }
    }
}
