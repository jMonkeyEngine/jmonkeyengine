/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.system.android;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.input.*;
import com.jme3.input.android.AndroidInputHandler;
import com.jme3.input.android.AndroidInputHandler14;
import com.jme3.input.android.AndroidInputHandler24;
import com.jme3.input.android.AndroidInputHandler26;
import com.jme3.input.controls.SoftTextDialogInputListener;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.android.AndroidGL;
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
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.PrimitiveAllocator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class OGLESContext implements JmeContext, GLSurfaceView.Renderer, SoftTextDialogInput {

    private static final String BLIT_MATERIAL = "Common/MatDefs/Blit/Blit.j3md";
    private static final Logger logger = Logger.getLogger(OGLESContext.class.getName());
    private static final String SAFER_BUFFER_ALLOCATOR_CLASS = "com.jme3.util.SaferBufferAllocator";
    protected final AtomicBoolean created = new AtomicBoolean(false);
    protected final AtomicBoolean renderable = new AtomicBoolean(false);
    protected final AtomicBoolean needClose = new AtomicBoolean(false);
    protected AppSettings settings = new AppSettings(true);

    protected GLRenderer renderer;
    protected Timer timer;
    protected SystemListener listener;
    protected boolean autoFlush = true;
    protected AndroidInputHandler androidInput;
    protected long minFrameDuration = 0; // No FPS cap
    protected long lastUpdateTime = 0;
    private int logicalWidth = 1;
    private int logicalHeight = 1;
    private int framebufferWidth = 1;
    private int framebufferHeight = 1;
    private final Vector2f displayScale = new Vector2f(1f, 1f);
    private float appliedDisplayScaleMode = Float.NaN;
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
            if (isClassPresent(SAFER_BUFFER_ALLOCATOR_CLASS)) {
                System.setProperty(implementation, SAFER_BUFFER_ALLOCATOR_CLASS);
            } else {
                System.setProperty(implementation, PrimitiveAllocator.class.getName());
            }
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, OGLESContext.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public OGLESContext() {}

    @Override
    public Type getType() {
        return Type.Display;
    }

    /**
     * <code>createView</code> creates the GLSurfaceView that the renderer will
     * draw to. <p> The result GLSurfaceView will receive input events and
     * forward them to the Application. Any rendering will be done into the
     * GLSurfaceView. Only one GLSurfaceView can be created at this time. The
     * given configType specifies how to determine the display configuration.
     *
     * @param context (not null)
     * @return GLSurfaceView The newly created view
     */
    public GLSurfaceView createView(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        if (info.reqGlEsVersion < 0x30000) {
            throw new UnsupportedOperationException("OpenGL ES 3.0 or better is not supported on this device");
        }

        // Start to set up the view
        GLSurfaceView view = new GLSurfaceView(context);
        logger.log(Level.INFO, "Android Build Version: {0}", Build.VERSION.SDK_INT);
        if (androidInput == null) {
            if (Build.VERSION.SDK_INT >= 26) {
                androidInput = new AndroidInputHandler26();
            } else if (Build.VERSION.SDK_INT >= 24) {
                androidInput = new AndroidInputHandler24();
            } else if (Build.VERSION.SDK_INT >= 14) {
                androidInput = new AndroidInputHandler14();
            } else if (Build.VERSION.SDK_INT >= 9) {
                androidInput = new AndroidInputHandler();
            }
        }
        androidInput.setView(view);
        androidInput.loadSettings(settings);

        // setEGLContextClientVersion must be set before calling setRenderer.
        view.setEGLContextClientVersion(3);

        view.setFocusableInTouchMode(true);
        view.setFocusable(true);
        view.setFocusedByDefault(true);
        view.requestFocus();
        //view.setClickable(true);

        // setFormat must be set before AndroidConfigChooser is called by the surfaceview.
        // For best rendering performance and sRGB support, prefer Opaque (alpha bits = 0).
        int curAlphaBits = settings.getAlphaBits();
        logger.log(Level.FINE, "curAlphaBits: {0}", curAlphaBits);
        if (curAlphaBits >= 8) {
            logger.log(Level.FINE, "Pixel Format: TRANSLUCENT");
            view.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            view.setZOrderOnTop(true);
        } else if (curAlphaBits >= 1) {
            logger.log(Level.FINE, "Pixel Format: TRANSPARENT");
            view.getHolder().setFormat(PixelFormat.TRANSPARENT);
        } else {
            logger.log(Level.FINE, "Pixel Format: OPAQUE");
            view.getHolder().setFormat(PixelFormat.OPAQUE);
        }

        AndroidConfigChooser configChooser = new AndroidConfigChooser(settings);
        view.setEGLConfigChooser(configChooser);
        view.setEGLContextFactory(new Gles3ContextFactory());
        view.setRenderer(this);

        // Attempt to preserve the EGL Context on app pause/resume.
        // Not destroying and recreating the EGL context
        // will help with resume time by reusing the existing context to avoid
        // reloading all the OpenGL objects.
        if (Build.VERSION.SDK_INT >= 11) {
            view.setPreserveEGLContextOnPause(true);
        }

        return view;
    }

    private static final class Gles3ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        private static final int EGL_CONTEXT_PRIORITY_LEVEL_IMG = 0x3100;
        private static final int EGL_CONTEXT_PRIORITY_HIGH_IMG = 0x3101;

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
            EGLContext context = createGles3Context(egl, display, config, true);
            if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                context = createGles3Context(egl, display, config, false);
            }
            if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                throw new IllegalStateException("Unable to create an OpenGL ES 3 context");
            }
            return context;
        }

        private EGLContext createGles3Context(EGL10 egl, EGLDisplay display,
                                              EGLConfig config, boolean preferHighPriority) {
            boolean usePriority = preferHighPriority && hasExtension(egl, display, "EGL_IMG_context_priority");
            int[] attributes = usePriority
                    ? new int[]{
                        EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL_CONTEXT_PRIORITY_LEVEL_IMG, EGL_CONTEXT_PRIORITY_HIGH_IMG,
                        EGL10.EGL_NONE
                    }
                    : new int[]{
                        EGL_CONTEXT_CLIENT_VERSION, 3,
                        EGL10.EGL_NONE
                    };
            return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attributes);
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    private static boolean hasExtension(EGL10 egl, EGLDisplay display, String extension) {
        String extensions = egl.eglQueryString(display, EGL10.EGL_EXTENSIONS);
        if (extensions == null) {
            return false;
        }
        // EGL extension list is space separated. Ensure we only match full
        // extension names to avoid false positives when one name is a
        // substring of another.
        String padded = " " + extensions + " ";
        return padded.contains(" " + extension + " ");
    }

    // renderer:initialize
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
        if (created.get() && renderer != null) {
            renderer.resetGLObjects();
            destroyLinearFrameBuffer();
        } else {
            if (!created.get()) {
                logger.fine("GL Surface created, initializing JME3 renderer");
                initInThread();
            } else {
                logger.warning("GL Surface already created");
            }
        }
    }

    protected void initInThread() {
        created.set(true);

        logger.fine("OGLESContext create");
        logger.log(Level.FINE, "Running on thread: {0}", Thread.currentThread().getName());

        // Setup unhandled Exception Handler
        Thread
            .currentThread()
            .setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable thrown) {
                        listener.handleError("Exception thrown in " + thread.toString(), thrown);
                    }
                }
            );

        timer = new NanoTimer();
        GL gl = new AndroidGL();
        if (settings.getBoolean("GraphicsDebug")) {
            gl =
                (GL) GLDebug.createProxy(
                    gl,
                    gl,
                    GL.class,
                    GL2.class,
                    GLES_30.class,
                    GLFbo.class,
                    GLExt.class
                );
        }
        if (settings.getBoolean("GraphicsTrace")) {
            gl = (GL) GLTracer.createGlesTracer(gl, GL.class, GLES_30.class, GLFbo.class, GLExt.class);
        }
        renderer = new GLRenderer(gl, (GLExt) gl, (GLFbo) gl);
        renderer.initialize();

        boolean blitSrgbConversion = useBlitSrgbConversion();
        renderer.setMainFrameBufferSrgb(false);
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());
        logger.log(Level.INFO,
                "Android gamma correction: requested={0}, main framebuffer sRGB=false, blit sRGB={1}",
                new Object[]{settings.isGammaCorrection(), blitSrgbConversion});

        JmeSystem.setSoftTextDialogInput(this);

        needClose.set(false);
    }

    /**
     * De-initialize in the OpenGL thread.
     */
    protected void deinitInThread() {
        if (renderable.get()) {
            created.set(false);
            destroyLinearFrameBufferResources();
            if (renderer != null) {
                renderer.cleanup();
            }

            listener.destroy();
            // releases the view holder from the Android Input Resources
            // releasing the view enables the context instance to be
            // reclaimed by the GC.
            // if not released; it leads to a weak reference leak
            // disabling the destruction of the Context View Holder.
            androidInput.setView(null);

            // nullifying the references
            // signals their memory to be reclaimed
            listener = null;
            renderer = null;
            timer = null;
            androidInput = null;

            // do android specific cleaning here
            logger.fine("Display destroyed.");

            renderable.set(false);
        }
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
        if (androidInput != null) {
            androidInput.loadSettings(settings);
        }

        if (settings.getFrameRate() > 0) {
            minFrameDuration = (long) (1000d / settings.getFrameRate()); // ms
            logger.log(Level.FINE, "Setting min tpf: {0}ms", minFrameDuration);
        } else {
            minFrameDuration = 0;
        }
    }

    /**
     * Accesses the listener that receives events related to this context.
     *
     * @return the pre-existing instance
     */
    @Override
    public SystemListener getSystemListener() {
        return listener;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
        if (listener instanceof Application) {
            application = (Application) listener;
        }
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public com.jme3.renderer.Renderer getRenderer() {
        return renderer;
    }

    @Override
    public MouseInput getMouseInput() {
        return androidInput.getMouseInput();
    }

    @Override
    public KeyInput getKeyInput() {
        return new DummyKeyInput();
    }

    @Override
    public JoyInput getJoyInput() {
        return androidInput.getJoyInput();
    }

    @Override
    public TouchInput getTouchInput() {
        return androidInput.getTouchInput();
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public void setTitle(String title) {}

    @Override
    public boolean isCreated() {
        return created.get();
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
        this.autoFlush = enabled;
    }

    // SystemListener:reshape
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(
                Level.FINE,
                "GL Surface changed, width: {0} height: {1}",
                new Object[] { width, height }
            );
        }
        framebufferWidth = Math.max(width, 1);
        framebufferHeight = Math.max(height, 1);
        updateDisplayScaleMetrics();
        // if the application has already been initialized (ie renderable is set)
        // then call reshape so the app can adjust to the new resolution.
        if (renderable.get()) {
            logger.log(Level.FINE, "App already initialized, calling reshape");
            listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
            listener.reshape(logicalWidth, logicalHeight);
        }
    }

    private float getAndroidDisplayDensity() {
        if (androidInput != null && androidInput.getView() != null
                && androidInput.getView().getResources() != null) {
            android.util.DisplayMetrics metrics = androidInput.getView().getResources().getDisplayMetrics();
            if (metrics != null) {
                if (metrics.density > 0f) {
                    return metrics.density;
                }
                if (metrics.densityDpi > 0) {
                    return metrics.densityDpi / 160f;
                }
            }
        }
        return 1f;
    }

    private void updateDisplayScaleMetrics() {
        float density = DisplayScaleUtils.sanitizeScale(getAndroidDisplayDensity());
        displayScale.set(density, density);
        appliedDisplayScaleMode = settings.getDisplayScaleMode();
        if (DisplayScaleUtils.isNativePixelsMode(appliedDisplayScaleMode)) {
            logicalWidth = framebufferWidth;
            logicalHeight = framebufferHeight;
        } else {
            logicalWidth = Math.max(Math.round(framebufferWidth / density), 1);
            logicalHeight = Math.max(Math.round(framebufferHeight / density), 1);
        }
        settings.setResolution(logicalWidth, logicalHeight);
        // Reload settings in androidInput so the correct touch event scaling can be
        // calculated in case the surface resolution is different than the view.
        if (androidInput != null) {
            androidInput.loadSettings(settings);
        }
    }

    private void applyDisplayScaleModeIfNeeded() {
        if (Float.compare(settings.getDisplayScaleMode(), appliedDisplayScaleMode) == 0) {
            return;
        }

        updateDisplayScaleMetrics();
        if (renderable.get()) {
            listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
            listener.reshape(logicalWidth, logicalHeight);
        }
    }

    // SystemListener:update
    @Override
    public void onDrawFrame(GL10 gl) {
        if (needClose.get()) {
            deinitInThread();
            return;
        }

        if (!renderable.get()) {
            if (created.get()) {
                applyDisplayScaleModeIfNeeded();
                logger.fine("GL Surface is setup, initializing application");
                listener.initialize();
                if (framebufferWidth > 0 && framebufferHeight > 0) {
                    listener.reshape(logicalWidth, logicalHeight, getRenderFramebufferWidth(), getRenderFramebufferHeight());
                    listener.reshape(logicalWidth, logicalHeight);
                }
                renderable.set(true);
            }
        } else {
            if (!created.get()) {
                throw new IllegalStateException("onDrawFrame without create");
            }

            applyDisplayScaleModeIfNeeded();
            if (!renderFrameWithBlitSrgbConversion()) {
                listener.update();
            }
            if (autoFlush) {
                renderer.postFrame();
            }

            long updateDelta = System.currentTimeMillis() - lastUpdateTime;

            // Enforce a FPS cap
            if (updateDelta < minFrameDuration) {
                //                    logger.log(Level.INFO, "lastUpdateTime: {0}, updateDelta: {1}, minTimePerFrame: {2}",
                //                            new Object[]{lastUpdateTime, updateDelta, minTimePerFrame});
                try {
                    Thread.sleep(minFrameDuration - updateDelta);
                } catch (InterruptedException e) {}
            }

            lastUpdateTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isRenderable() {
        return renderable.get();
    }

    @Override
    public void create(boolean waitFor) {
        if (waitFor) {
            waitFor(true);
        }
    }

    public void create() {
        create(false);
    }

    @Override
    public void restart() {}

    @Override
    public void destroy(boolean waitFor) {
        needClose.set(true);
        if (waitFor) {
            waitFor(false);
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
        frameBuffer.setName("Android Linear Blit FrameBuffer");
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
                + "Falling back to RGBA8 for Android sRGB blit conversion.");
        return Format.RGBA8;
    }

    private boolean ensureBlitResources() {
        if (!useBlitFrameBuffer()) {
            return false;
        }

        AssetManager assetManager = application.getAssetManager();
        RenderManager renderManager = application.getRenderManager();
        if (assetManager == null || renderManager == null) {
            return false;
        }

        if (blitMaterial == null) {
            blitMaterial = new Material(assetManager, BLIT_MATERIAL);
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
            int blitWidth = Math.max(getFramebufferWidth(), 1);
            int blitHeight = Math.max(getFramebufferHeight(), 1);
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
    public void requestDialog(
        final int id,
        final String title,
        final String initialValue,
        final SoftTextDialogInputListener listener
    ) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(
                Level.FINE,
                "requestDialog: title: {0}, initialValue: {1}",
                new Object[] { title, initialValue }
            );
        }

        final View view = JmeAndroidSystem.getView();
        view
            .getHandler()
            .post(
                new Runnable() {
                    @Override
                    public void run() {
                        final FrameLayout layoutTextDialogInput = new FrameLayout(view.getContext());
                        final EditText editTextDialogInput = new EditText(view.getContext());
                        editTextDialogInput.setWidth(LayoutParams.MATCH_PARENT);
                        editTextDialogInput.setHeight(LayoutParams.MATCH_PARENT);
                        editTextDialogInput.setPadding(20, 20, 20, 20);
                        editTextDialogInput.setGravity(Gravity.FILL_HORIZONTAL);
                        //editTextDialogInput.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

                        editTextDialogInput.setText(initialValue);

                        switch (id) {
                            case SoftTextDialogInput.TEXT_ENTRY_DIALOG:
                                editTextDialogInput.setInputType(InputType.TYPE_CLASS_TEXT);
                                break;
                            case SoftTextDialogInput.NUMERIC_ENTRY_DIALOG:
                                editTextDialogInput.setInputType(
                                    InputType.TYPE_CLASS_NUMBER |
                                    InputType.TYPE_NUMBER_FLAG_DECIMAL |
                                    InputType.TYPE_NUMBER_FLAG_SIGNED
                                );
                                break;
                            case SoftTextDialogInput.NUMERIC_KEYPAD_DIALOG:
                                editTextDialogInput.setInputType(InputType.TYPE_CLASS_PHONE);
                                break;
                            default:
                                break;
                        }

                        layoutTextDialogInput.addView(editTextDialogInput);

                        AlertDialog dialogTextInput = new AlertDialog.Builder(view.getContext())
                            .setTitle(title)
                            .setView(layoutTextDialogInput)
                            .setPositiveButton(
                                "OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        /* User clicked OK, send COMPLETE action
                                         * and text */
                                        listener.onSoftText(
                                            SoftTextDialogInputListener.COMPLETE,
                                            editTextDialogInput.getText().toString()
                                        );
                                    }
                                }
                            )
                            .setNegativeButton(
                                "Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        /* User clicked CANCEL, send CANCEL action
                                         * and text */
                                        listener.onSoftText(
                                            SoftTextDialogInputListener.CANCEL,
                                            editTextDialogInput.getText().toString()
                                        );
                                    }
                                }
                            )
                            .create();

                        dialogTextInput.show();
                    }
                }
            );
    }

    @Override
    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    /**
     * Returns the width of the input surface.
     *
     * @return the width (in pixels)
     */
    @Override
    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    /**
     * Returns the screen X coordinate of the left edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowXPosition() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * Returns the screen Y coordinate of the top edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowYPosition() {
        throw new UnsupportedOperationException("not implemented yet");
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
