/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.system.ios;

import com.jme3.app.Application;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.ios.IosInputHandler;
import com.jme3.input.ios.IosJoyInput;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ios.IosGL;
import com.jme3.renderer.opengl.*;
import com.jme3.system.*;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.LibJGLIOSNativeBufferAllocator;
import com.jme3.util.BufferAllocatorFactory;
import org.ngengine.libjglios.core.LibJGLIOSEglBridge;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IGLESContext implements JmeContext {

    private static final String BLIT_MATERIAL = "Common/MatDefs/Blit/Blit.j3md";
    private static final Logger logger = Logger.getLogger(IGLESContext.class.getName());
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected AppSettings settings = new AppSettings(true);
    protected boolean autoFlush = true;
    protected int framebufferWidth;
    protected int framebufferHeight;
    protected int logicalWidth = 1;
    protected int logicalHeight = 1;
    private final Vector2f displayScale = new Vector2f(1f, 1f);
    private float appliedDisplayScaleMode = Float.NaN;

    /*
     * >= OpenGL ES 2.0 (iOS)
     */
    protected GLRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected IosInputHandler input;
    protected IosJoyInput joyInput;
    protected int minFrameDuration = 0; // No FPS cap
    private Application application;
    private Material blitMaterial;
    private Picture blitGeometry;
    private final Camera blitCamera = new Camera(1, 1);
    private FrameBuffer linearFrameBuffer;
    private Texture2D linearFrameBufferColorTexture;
    private boolean linearFrameBufferDirty;
    private boolean multisampleTextureWarningIssued;

    static {
        final String implementation = BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION;

        if (System.getProperty(implementation) == null) {
            System.setProperty(implementation, LibJGLIOSNativeBufferAllocator.class.getName());
        }
    }

    public IGLESContext() {
        logger.log(Level.FINE, "IGLESContext constructor");
    }

    

    @Override
    public Type getType() {
        return Type.Display;
    }

    @Override
    public void setSettings(AppSettings settings) {
        logger.log(Level.FINE, "IGLESContext setSettings");
        this.settings.copyFrom(settings);
        if (input != null) {
            input.loadSettings(settings);
        }
        if (joyInput != null) {
            joyInput.loadSettings(settings);
        }
    }

    /**
     * Accesses the listener that receives events related to this context.
     *
     * @return the pre-existing instance
     */
    @Override
    public SystemListener getSystemListener() {
        logger.log(Level.FINE, "IGLESContext getSystemListener");
        return listener;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        logger.log(Level.FINE, "IGLESContext setSystemListener");
        this.listener = listener;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public com.jme3.renderer.Renderer getRenderer() {
        logger.log(Level.FINE, "IGLESContext getRenderer");
        return renderer;
    }

    @Override
    public MouseInput getMouseInput() {
        return new DummyMouseInput();
    }

    @Override
    public KeyInput getKeyInput() {
        return new DummyKeyInput();
    }

    @Override
    public JoyInput getJoyInput() {
        if (joyInput == null) {
            joyInput = new IosJoyInput();
            joyInput.loadSettings(settings);
        }
        return joyInput;
    }

    @Override
    public TouchInput getTouchInput() {
        return input;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public void setTitle(String title) {}

    @Override
    public boolean isCreated() {
        logger.log(Level.FINE, "IGLESContext isCreated");
        return created.get();
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    @Override
    public boolean isRenderable() {
        logger.log(Level.FINE, "IGLESContext isRenderable");
        return true; // renderable.get();
    }

    @Override
    public void create(boolean waitFor) {
        logger.log(Level.FINE, "IGLESContext create");
        if (!LibJGLIOSEglBridge.makeCurrent()) {
            throw new IllegalStateException("Unable to make iOS EGL context current: " + LibJGLIOSEglBridge.lastError());
        }
        if (listener instanceof Application) {
            application = (Application) listener;
        }
        IosGL gl = new IosGL();

        if (settings.getBoolean("GraphicsDebug")) {
            gl = (IosGL) GLDebug.createProxy(gl, gl, GL.class, GLExt.class, GLFbo.class);
        }

        renderer = new GLRenderer(gl, gl, gl);
        renderer.initialize();
        // ANGLE/Metal currently reports GLES3 but cannot reliably use hardware
        // shadow comparison through jME's shadow pipeline.
        renderer.getCaps().remove(Caps.TextureShadowCompare);
        renderer.setMainFrameBufferSrgb(false);
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());
        logger.log(Level.INFO,
                "iOS gamma correction: requested={0}, main framebuffer sRGB=false, blit sRGB={1}",
                new Object[]{settings.isGammaCorrection(), useBlitSrgbConversion()});

        input = new IosInputHandler();
        input.loadSettings(settings);
        timer = new NanoTimer();
        updateFramebufferSizeFromLibJGLIOS();

        //synchronized (createdLock){
        created.set(true);
        //createdLock.notifyAll();
        //}

        listener.initialize();
        renderable.set(true);
        if (framebufferWidth > 0 && framebufferHeight > 0) {
            listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
        }

        if (waitFor) {
            //waitFor(true);
        }
        logger.log(Level.FINE, "IGLESContext created");
    }

    /**
     * Advances one jME frame when driven by the libJGLIOS SDL callback loop.
     */
    public void runFrame() {
        if (!created.get() || listener == null) {
            return;
        }
        if (!LibJGLIOSEglBridge.makeCurrent()) {
            throw new IllegalStateException("Unable to make iOS EGL context current: " + LibJGLIOSEglBridge.lastError());
        }
        updateFramebufferSizeFromLibJGLIOS();
        applyDisplayScaleModeIfNeeded();
        if (!renderFrameWithBlitSrgbConversion()) {
            listener.update();
        }
        if (autoFlush) {
            LibJGLIOSEglBridge.swapBuffers();
        }
        if (renderer != null) {
            renderer.postFrame();
        }
    }

    public void resizeFramebuffer(int width, int height) {
        int oldWidth = framebufferWidth;
        int oldHeight = framebufferHeight;
        int oldLogicalWidth = logicalWidth;
        int oldLogicalHeight = logicalHeight;
        boolean displayScaleModeChanged = Float.compare(settings.getDisplayScaleMode(), appliedDisplayScaleMode) != 0;
        if (width > 0) {
            framebufferWidth = width;
        }
        if (height > 0) {
            framebufferHeight = height;
        }
        if (framebufferWidth > 0 && framebufferHeight > 0) {
            updateLogicalSize();
            settings.setResolution(logicalWidth, logicalHeight);
            settings.setWindowSize(logicalWidth, logicalHeight);
        }
        if (input != null) {
            input.setFramebufferSize(logicalWidth, logicalHeight);
        }
        if (framebufferWidth != oldWidth || framebufferHeight != oldHeight
                || logicalWidth != oldLogicalWidth || logicalHeight != oldLogicalHeight
                || displayScaleModeChanged) {
            linearFrameBufferDirty = true;
            if (renderable.get() && listener != null) {
                logger.log(Level.FINE, "iOS framebuffer resized, width: {0} height: {1}",
                        new Object[]{framebufferWidth, framebufferHeight});
                listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
                listener.rescale(displayScale.x, displayScale.y);
            }
        }
    }

    private void updateLogicalSize() {
        float scale = DisplayScaleUtils.sanitizeScale(LibJGLIOSEglBridge.displayScale());
        displayScale.set(scale, scale);

        appliedDisplayScaleMode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isNativePixelsMode(appliedDisplayScaleMode)) {
            logicalWidth = Math.max(framebufferWidth, 1);
            logicalHeight = Math.max(framebufferHeight, 1);
            return;
        }

        int windowWidth = LibJGLIOSEglBridge.windowWidth();
        int windowHeight = LibJGLIOSEglBridge.windowHeight();
        if (windowWidth <= 0) {
            windowWidth = Math.max(Math.round(framebufferWidth / scale), 1);
        }
        if (windowHeight <= 0) {
            windowHeight = Math.max(Math.round(framebufferHeight / scale), 1);
        }

        int[] logicalSize = DisplayScaleUtils.resolveLogicalSize(AppSettings.DISPLAY_SCALE_DPI_AWARE,
                windowWidth, windowHeight,
                framebufferWidth, framebufferHeight, displayScale.x, displayScale.y);
        logicalWidth = logicalSize[0];
        logicalHeight = logicalSize[1];
    }

    private void applyDisplayScaleModeIfNeeded() {
        if (Float.compare(settings.getDisplayScaleMode(), appliedDisplayScaleMode) == 0) {
            return;
        }

        updateLogicalSize();
        settings.setResolution(logicalWidth, logicalHeight);
        settings.setWindowSize(logicalWidth, logicalHeight);
        if (input != null) {
            input.setFramebufferSize(logicalWidth, logicalHeight);
        }
        linearFrameBufferDirty = true;
        if (renderable.get() && listener != null) {
            listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
            listener.rescale(displayScale.x, displayScale.y);
        }
    }

    private void updateFramebufferSizeFromLibJGLIOS() {
        int width = LibJGLIOSEglBridge.framebufferWidth();
        int height = LibJGLIOSEglBridge.framebufferHeight();
        if (width > 0 && height > 0) {
            resizeFramebuffer(width, height);
        } else if (framebufferWidth <= 0 || framebufferHeight <= 0) {
            framebufferWidth = Math.max(settings.getWidth(), 1);
            framebufferHeight = Math.max(settings.getHeight(), 1);
            updateLogicalSize();
        }
    }

    public void create() {
        create(false);
    }

    @Override
    public void restart() {}

    @Override
    public void destroy(boolean waitFor) {
        logger.log(Level.FINE, "IGLESContext destroy");
        renderable.set(false);
        destroyLinearFrameBufferResources();
        if (listener != null) {
            listener.destroy();
        }
        created.set(false);
        listener = null;
        renderer = null;
        input = null;
        joyInput = null;
        timer = null;
        application = null;
        needClose.set(true);
        if (waitFor) {
            //waitFor(false);
        }
    }

    public void destroy() {
        destroy(true);
    }

    protected void waitFor(boolean createdVal) {
        while (renderable.get() != createdVal) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {}
        }
    }

    private boolean useBlitSrgbConversion() {
        return settings.isGammaCorrection() && application != null;
    }

    private boolean useBlitFrameBuffer() {
        float mode = settings.getDisplayScaleMode();
        return application != null && (useBlitSrgbConversion()
                || DisplayScaleUtils.isDisabledMode(mode) || DisplayScaleUtils.isEmulatedScaleMode(mode));
    }

    private int getRenderFramebufferWidth() {
        float mode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isDisabledMode(mode)) {
            return Math.max(logicalWidth, 1);
        }
        if (DisplayScaleUtils.isEmulatedScaleMode(mode)) {
            return Math.max(Math.round(framebufferWidth * mode), 1);
        }
        return Math.max(framebufferWidth, 1);
    }

    private int getRenderFramebufferHeight() {
        float mode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isDisabledMode(mode)) {
            return Math.max(logicalHeight, 1);
        }
        if (DisplayScaleUtils.isEmulatedScaleMode(mode)) {
            return Math.max(Math.round(framebufferHeight * mode), 1);
        }
        return Math.max(framebufferHeight, 1);
    }

    private int getLinearFrameBufferSampleCount() {
        int samples = Math.max(settings.getSamples(), 1);
        if (samples > 1 && renderer != null
                && (!renderer.getCaps().contains(Caps.TextureMultisample)
                || !renderer.getCaps().contains(Caps.OpenGL32))) {
            if (!multisampleTextureWarningIssued) {
                logger.log(Level.WARNING,
                        "Display scale blit requested {0}x MSAA, but this backend cannot sample multisample textures for the blit path. Falling back to a single-sample linear framebuffer.",
                        samples);
                multisampleTextureWarningIssued = true;
            }
            return 1;
        }
        return samples;
    }

    private void rebuildLinearFrameBufferIfNeeded() {
        if (!useBlitFrameBuffer()) {
            destroyLinearFrameBufferResources();
            return;
        }

        int width = getRenderFramebufferWidth();
        int height = getRenderFramebufferHeight();
        int samples = getLinearFrameBufferSampleCount();

        if (linearFrameBuffer != null && linearFrameBuffer.getWidth() == width
                && linearFrameBuffer.getHeight() == height && linearFrameBuffer.getSamples() == samples) {
            return;
        }

        destroyLinearFrameBuffer();

        FrameBuffer frameBuffer = new FrameBuffer(width, height, samples);
        frameBuffer.setName("iOS Linear Blit FrameBuffer");
        frameBuffer.setSrgb(false);

        Texture2D colorTexture = new Texture2D(
                new Image(getLinearFrameBufferColorFormat(), width, height, null, ColorSpace.Linear));
        colorTexture.setMagFilter(Texture.MagFilter.Bilinear);
        colorTexture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        if (samples > 1) {
            colorTexture.getImage().setMultiSamples(samples);
        }
        frameBuffer.addColorTarget(FrameBufferTarget.newTarget(colorTexture));

        if (settings.getDepthBits() > 0 || settings.getStencilBits() > 0) {
            frameBuffer.setDepthTarget(FrameBufferTarget
                    .newTarget(renderer.getBestDepthTargetFormat(false, false, settings.getStencilBits() > 0)));
        }

        linearFrameBufferColorTexture = colorTexture;
        linearFrameBuffer = frameBuffer;
        linearFrameBufferDirty = true;
    }

    private Format getLinearFrameBufferColorFormat() {
        if (renderer != null && renderer.getCaps().contains(Caps.HalfFloatColorBufferRGBA)) {
            return Format.RGBA16F;
        }
        logger.warning("RGBA16F color framebuffer is not supported. "
                + "Falling back to RGBA8 for iOS sRGB blit conversion.");
        return Format.RGBA8;
    }

    private boolean ensureBlitResources() {
        if (!useBlitFrameBuffer()) {
            return false;
        }

        RenderManager renderManager = application.getRenderManager();
        if (application.getAssetManager() == null || renderManager == null) {
            return false;
        }

        if (blitMaterial == null) {
            blitMaterial = new Material(application.getAssetManager(), BLIT_MATERIAL);
            blitMaterial.getAdditionalRenderState().setDepthTest(false);
            blitMaterial.getAdditionalRenderState().setDepthWrite(false);
        }
        blitMaterial.setBoolean("Srgb", useBlitSrgbConversion());

        if (blitGeometry == null) {
            blitGeometry = new Picture("Linear to sRGB Blit");
            blitGeometry.setWidth(1f);
            blitGeometry.setHeight(1f);
            blitGeometry.setMaterial(blitMaterial);
        }

        if (linearFrameBufferDirty && linearFrameBufferColorTexture != null) {
            blitMaterial.setTexture("Texture", linearFrameBufferColorTexture);
            if (linearFrameBuffer != null && linearFrameBuffer.getSamples() > 1) {
                blitMaterial.setInt("NumSamples", linearFrameBuffer.getSamples());
            } else {
                blitMaterial.clearParam("NumSamples");
            }
            linearFrameBufferDirty = false;
        }

        return true;
    }

    private void destroyLinearFrameBuffer() {
        if (linearFrameBuffer != null) {
            linearFrameBuffer.dispose();
            linearFrameBuffer = null;
        }
        if (linearFrameBufferColorTexture != null && linearFrameBufferColorTexture.getImage() != null) {
            linearFrameBufferColorTexture.getImage().dispose();
        }
        linearFrameBufferColorTexture = null;
        linearFrameBufferDirty = true;
    }

    private void destroyLinearFrameBufferResources() {
        destroyLinearFrameBuffer();
        blitMaterial = null;
        blitGeometry = null;
        multisampleTextureWarningIssued = false;
    }

    private boolean renderFrameWithBlitSrgbConversion() {
        if (!useBlitFrameBuffer()) {
            return false;
        }

        FrameBuffer previousMainFramebuffer = renderer.getCurrentFrameBuffer();
        if (previousMainFramebuffer != null) {
            return false;
        }

        rebuildLinearFrameBufferIfNeeded();
        if (linearFrameBuffer == null || !ensureBlitResources()) {
            return false;
        }

        FrameBuffer restoreMainFramebuffer = previousMainFramebuffer;

        renderer.setMainFrameBufferOverride(linearFrameBuffer);
        try {
            listener.update();
            FrameBuffer currentMainFramebuffer = renderer.getCurrentFrameBuffer();
            if (currentMainFramebuffer != linearFrameBuffer) {
                restoreMainFramebuffer = currentMainFramebuffer;
            }
        } finally {
            renderer.setMainFrameBufferOverride(restoreMainFramebuffer);
        }

        renderer.setMainFrameBufferOverride(null);
        RenderManager renderManager = application.getRenderManager();
        Camera previousCamera = renderManager.getCurrentCamera();
        try {
            renderer.setFrameBuffer(null);
            int blitWidth = Math.max(1, framebufferWidth);
            int blitHeight = Math.max(1, framebufferHeight);
            if (blitCamera.getWidth() != blitWidth || blitCamera.getHeight() != blitHeight) {
                blitCamera.resize(blitWidth, blitHeight, true);
            }
            renderManager.setCamera(blitCamera, true);
            if (blitGeometry.getWidth() != blitWidth || blitGeometry.getHeight() != blitHeight) {
                blitGeometry.setWidth(blitWidth);
                blitGeometry.setHeight(blitHeight);
            }
            blitGeometry.updateGeometricState();
            renderManager.renderGeometry(blitGeometry);
        } finally {
            renderer.setMainFrameBufferOverride(restoreMainFramebuffer);
            if (previousCamera != null) {
                renderManager.setCamera(previousCamera, false);
            }
        }
        return true;
    }

    @Override
    public int getFramebufferHeight() {
        updateFramebufferSizeFromLibJGLIOS();
        return framebufferHeight;
    }

    /**
     * Returns the width of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferWidth() {
        updateFramebufferSizeFromLibJGLIOS();
        return framebufferWidth;
    }

    /**
     * Returns the screen X coordinate of the left edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowXPosition() {
        return 0;
    }

    /**
     * Returns the screen Y coordinate of the top edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowYPosition() {
        return 0;
    }

    @Override
    public Displays getDisplays() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getPrimaryDisplay() {
        // TODO Auto-generated method stub
        return 0;
    }
}
