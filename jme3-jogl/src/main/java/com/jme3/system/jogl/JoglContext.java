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

package com.jme3.system.jogl;

import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.opencl.Context;
import com.jme3.opencl.DefaultPlatformChooser;
import com.jme3.opencl.Device;
import com.jme3.opencl.PlatformChooser;
import com.jme3.opencl.jocl.JoclDevice;
import com.jme3.opencl.jocl.JoclPlatform;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.jogl.JoglGL;
import com.jme3.renderer.jogl.JoglGLExt;
import com.jme3.renderer.jogl.JoglGLFbo;
import com.jme3.renderer.opengl.GL2;
import com.jme3.renderer.opengl.GL3;
import com.jme3.renderer.opengl.GL4;
import com.jme3.renderer.opengl.GLDebug;
import com.jme3.renderer.opengl.GLExt;
import com.jme3.renderer.opengl.GLFbo;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.opengl.GLTiming;
import com.jme3.renderer.opengl.GLTimingState;
import com.jme3.renderer.opengl.GLTracer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.NanoTimer;
import com.jme3.system.SystemListener;
import com.jme3.system.Timer;
import com.jogamp.opencl.CLDevice;
import com.jogamp.opencl.CLPlatform;
import com.jogamp.opencl.gl.CLGLContext;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import java.util.ArrayList;
import java.util.List;

public abstract class JoglContext implements JmeContext {

    private static final Logger logger = Logger.getLogger(JoglContext.class.getName());

    protected static final String THREAD_NAME = "jME3 Main";

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean renderable = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Renderer renderer;
    protected Timer timer;
    protected SystemListener listener;

    protected KeyInput keyInput;
    protected MouseInput mouseInput;
    protected JoyInput joyInput;

    protected com.jme3.opencl.Context clContext;
    
    @Override
	public void setSystemListener(SystemListener listener){
        this.listener = listener;
    }

    @Override
	public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    @Override
	public boolean isRenderable(){
        return renderable.get();
    }

    @Override
	public AppSettings getSettings() {
        return settings;
    }

    @Override
	public Renderer getRenderer() {
        return renderer;
    }

    @Override
	public MouseInput getMouseInput() {
        return mouseInput;
    }

    @Override
	public KeyInput getKeyInput() {
        return keyInput;
    }

    @Override
	public JoyInput getJoyInput() {
        return joyInput;
    }

    @Override
	public Timer getTimer() {
        return timer;
    }

    @Override
	public boolean isCreated() {
        return created.get();
    }

    public void create(){
        create(false);
    }

    public void destroy(){
        destroy(false);
    }

    protected void waitFor(boolean createdVal){
        synchronized (createdLock){
            while (created.get() != createdVal){
                try {
                    createdLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    protected void initContextFirstTime(){
        if (GLContext.getCurrent().getGLVersionNumber().getMajor() < 2) {
            throw new RendererException("OpenGL 2.0 or higher is " +
                                        "required for jMonkeyEngine");
        }

        if (settings.getRenderer().startsWith("JOGL")) {
        	com.jme3.renderer.opengl.GL gl = new JoglGL();
        	GLExt glext = new JoglGLExt();
        	GLFbo glfbo = new JoglGLFbo();

            if (settings.getBoolean("GraphicsDebug")) {
                gl = (com.jme3.renderer.opengl.GL) GLDebug.createProxy(gl, gl, com.jme3.renderer.opengl.GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLDebug.createProxy(gl, glext, GLExt.class);
                glfbo = (GLFbo) GLDebug.createProxy(gl, glfbo, GLFbo.class);
            }

            if (settings.getBoolean("GraphicsTiming")) {
                GLTimingState timingState = new GLTimingState();
                gl    = (com.jme3.renderer.opengl.GL) GLTiming.createGLTiming(gl, timingState, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTiming.createGLTiming(glext, timingState, GLExt.class);
                glfbo = (GLFbo) GLTiming.createGLTiming(glfbo, timingState, GLFbo.class);
            }

            if (settings.getBoolean("GraphicsTrace")) {
                gl    = (com.jme3.renderer.opengl.GL) GLTracer.createDesktopGlTracer(gl, GL.class, GL2.class, GL3.class, GL4.class);
                glext = (GLExt) GLTracer.createDesktopGlTracer(glext, GLExt.class);
                glfbo = (GLFbo) GLTracer.createDesktopGlTracer(glfbo, GLFbo.class);
            }

            renderer = new GLRenderer(gl, glext, glfbo);
            renderer.initialize();
        } else {
            throw new UnsupportedOperationException("Unsupported renderer: " + settings.getRenderer());
        }

        if (GLContext.getCurrentGL().isExtensionAvailable("GL_ARB_debug_output") && settings.getBoolean("GraphicsDebug")) {
        	GLContext.getCurrent().enableGLDebugMessage(true);
        	GLContext.getCurrent().addGLDebugListener(new JoglGLDebugOutputHandler());
        }

        renderer.setMainFrameBufferSrgb(settings.isGammaCorrection());
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());

        // Init input
        if (keyInput != null) {
            keyInput.initialize();
        }

        if (mouseInput != null) {
            mouseInput.initialize();
        }

        if (joyInput != null) {
            joyInput.initialize();
        }
        
        if (settings.isOpenCLSupport()) {
            initOpenCL();
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void initOpenCL() {
        logger.info("Initialize OpenCL with JOGL");
        
        //load platforms and devices
        StringBuilder platformInfos = new StringBuilder();
        ArrayList<JoclPlatform> platforms = new ArrayList<JoclPlatform>();
        for (CLPlatform p : CLPlatform.listCLPlatforms()) {
            platforms.add(new JoclPlatform(p));
        }
        platformInfos.append("Available OpenCL platforms:");
        for (int i=0; i<platforms.size(); ++i) {
            JoclPlatform platform = platforms.get(i);
            platformInfos.append("\n * Platform ").append(i+1);
            platformInfos.append("\n *   Name: ").append(platform.getName());
            platformInfos.append("\n *   Vendor: ").append(platform.getVendor());
            platformInfos.append("\n *   Version: ").append(platform.getVersion());
            platformInfos.append("\n *   Profile: ").append(platform.getProfile());
            platformInfos.append("\n *   Supports interop: ").append(platform.hasOpenGLInterop());
            List<JoclDevice> devices = platform.getDevices();
            platformInfos.append("\n *   Available devices:");
            for (int j=0; j<devices.size(); ++j) {
                JoclDevice device = devices.get(j);
                platformInfos.append("\n *    * Device ").append(j+1);
                platformInfos.append("\n *    *   Name: ").append(device.getName());
                platformInfos.append("\n *    *   Vendor: ").append(device.getVendor());
                platformInfos.append("\n *    *   Version: ").append(device.getVersion());
                platformInfos.append("\n *    *   Profile: ").append(device.getProfile());
                platformInfos.append("\n *    *   Compiler version: ").append(device.getCompilerVersion());
                platformInfos.append("\n *    *   Device type: ").append(device.getDeviceType());
                platformInfos.append("\n *    *   Compute units: ").append(device.getComputeUnits());
                platformInfos.append("\n *    *   Work group size: ").append(device.getMaxiumWorkItemsPerGroup());
                platformInfos.append("\n *    *   Global memory: ").append(device.getGlobalMemorySize()).append("B");
                platformInfos.append("\n *    *   Local memory: ").append(device.getLocalMemorySize()).append("B");
                platformInfos.append("\n *    *   Constant memory: ").append(device.getMaximumConstantBufferSize()).append("B");
                platformInfos.append("\n *    *   Supports double: ").append(device.hasDouble());
                platformInfos.append("\n *    *   Supports half floats: ").append(device.hasHalfFloat());
                platformInfos.append("\n *    *   Supports writable 3d images: ").append(device.hasWritableImage3D());
                platformInfos.append("\n *    *   Supports interop: ").append(device.hasOpenGLInterop());
            }
        }
        logger.info(platformInfos.toString());
        
        //choose devices
        PlatformChooser chooser = null;
        if (settings.getOpenCLPlatformChooser() != null) {
            try {
                chooser = (PlatformChooser) Class.forName(settings.getOpenCLPlatformChooser()).newInstance();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "unable to instantiate custom PlatformChooser", ex);
            }
        }
        if (chooser == null) {
            chooser = new DefaultPlatformChooser();
        }
        List<? extends Device> choosenDevices = chooser.chooseDevices(platforms);
        List<CLDevice> devices = new ArrayList<>(choosenDevices.size());
        JoclPlatform platform = null;
        for (Device d : choosenDevices) {
            if (!(d instanceof JoclDevice)) {
                logger.log(Level.SEVERE, "attempt to return a custom Device implementation from PlatformChooser: {0}", d);
                return;
            }
            JoclDevice ld = (JoclDevice) d;
            if (platform == null) {
                platform = ld.getPlatform();
            } else if (platform != ld.getPlatform()) {
                logger.severe("attempt to use devices from different platforms");
                return;
            }
            devices.add(ld.getDevice());
        }
        if (devices.isEmpty()) {
            logger.warning("no devices specified, no OpenCL context created");
            return;
        }
        logger.log(Level.INFO, "chosen platform: {0}", platform.getName());
        logger.log(Level.INFO, "chosen devices: {0}", choosenDevices);
        
        //create context
        try {
            CLGLContext c = CLGLContext.create(GLContext.getCurrent(), devices.toArray(new CLDevice[devices.size()]));
            clContext = new com.jme3.opencl.jocl.JoclContext(c, (List<JoclDevice>) choosenDevices);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unable to create OpenCL context", ex);
            return;
        }
        
        logger.info("OpenCL context created");
    }

    public void internalCreate() {
        timer = new NanoTimer();
        synchronized (createdLock){
            created.set(true);
            createdLock.notifyAll();
        }
        if (renderable.get()){
            initContextFirstTime();
        } else {
            assert getType() == Type.Canvas;
        }
    }

    protected void internalDestroy() {
        renderer = null;
        timer = null;
        renderable.set(false);
        synchronized (createdLock){
            created.set(false);
            createdLock.notifyAll();
        }
    }

    protected int determineMaxSamples(int requestedSamples) {
        GL gl = GLContext.getCurrentGL();
        if (gl.hasFullFBOSupport()) {
            return gl.getMaxRenderbufferSamples();
        } else {
            if (gl.isExtensionAvailable("GL_ARB_framebuffer_object")
                    || gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")) {
                IntBuffer intBuf1 = IntBuffer.allocate(1);
                gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, intBuf1);
                return intBuf1.get(0);
            } else {
                return Integer.MAX_VALUE;
            }
        }
    }

    protected int getNumSamplesToUse() {
        int samples = 0;
        if (settings.getSamples() > 1){
            samples = settings.getSamples();
            int supportedSamples = determineMaxSamples(samples);
            if (supportedSamples < samples) {
                logger.log(Level.WARNING,
                        "Couldn''t satisfy antialiasing samples requirement: x{0}. "
                        + "Video hardware only supports: x{1}",
                        new Object[]{samples, supportedSamples});

                samples = supportedSamples;
            }
        }
        return samples;
    }

    @Override
    public Context getOpenCLContext() {
        return clContext;
    }
    
}
