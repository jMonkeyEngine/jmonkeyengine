/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.view.surfaceview;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import com.jme3.app.LegacyApplication;
import com.jme3.asset.AssetLoader;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <b>A RelativeLayout class holder that wraps a {@link GLSurfaceView} as a renderer UI component and uses {@link OGLESContext} as a renderer context to render
 * a jme game on an android view for custom xml designs.</b>
 * The main idea of {@link JmeSurfaceView} class is to start a jMonkeyEngine application in a {@link SystemListener} context on a GL_ES thread,
 * then the game is rendered and updated through a {@link GLSurfaceView} component with a delay of user's choice using a {@link Handler}, during the delay,
 * the user has the ability to handle a couple of actions asynchronously as displaying a progress bar on a SplashScreen or an image or even play a preface game music of choice.
 *
 * @author pavl_g.
 */
public class JmeSurfaceView extends RelativeLayout implements SystemListener, DialogInterface.OnClickListener, LifecycleEventObserver {

    private static final Logger jmeSurfaceViewLogger = Logger.getLogger(JmeSurfaceView.class.getName());
    /*AppSettings attributes*/
    protected String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
    /*using {@link LegacyApplication} instead of {@link SimpleApplication} to include all classes extends LegacyApplication*/
    private LegacyApplication legacyApplication;
    private AppSettings appSettings;
    private int eglBitsPerPixel = 24;
    private int eglAlphaBits = 0;
    private int eglDepthBits = 16;
    private int eglSamples = 0;
    private int eglStencilBits = 0;
    private int frameRate = -1;
    private boolean emulateKeyBoard = true;
    private boolean emulateMouse = true;
    private boolean useJoyStickEvents = true;
    private boolean isGLThreadPaused;
    /*Late-init instances -- nullable objects*/
    private GLSurfaceView glSurfaceView;
    private OGLESContext oglesContext;
    private ConfigurationInfo configurationInfo;
    private OnRendererCompleted onRendererCompleted;
    private OnRendererStarted onRendererStarted;
    private OnExceptionThrown onExceptionThrown;
    private OnLayoutDrawn onLayoutDrawn;
    /*Global Objects*/
    private Handler handler = new Handler();
    private RendererThread rendererThread = new RendererThread();
    private StringWriter crashLogWriter = new StringWriter(150);
    /*Global flags*/
    private boolean showErrorDialog = true;
    private boolean bindAppState = true;
    private boolean showEscExitPrompt = true;
    private boolean exitOnEscPressed = true;
    /*Destruction policy flag*/
    private DestructionPolicy destructionPolicy = DestructionPolicy.DESTROY_WHEN_FINISH;
    /*extra messages/data*/
    private String crashLog = "";
    private String glEsVersion = "";

    /**
     * Instantiates a default surface view holder without XML attributes.
     * On instantiating this surface view, the holder is bound directly to the
     * parent context life cycle.
     *
     * @param context the parent context.
     */
    public JmeSurfaceView(@NonNull Context context) {
        super(context);
        //binds the view component to the holder activity life cycle
        bindAppStateToActivityLifeCycle(bindAppState);
    }

    /**
     * Instantiates a surface view holder with XML attributes from an XML document.
     * On instantiating this surface view, the holder is bound directly to the
     * parent context life cycle.
     *
     * @param context the parent context.
     * @param attrs   a collection of attributes describes the tags in an XML document.
     * @see android.content.res.Resources.Theme#obtainAttributes(AttributeSet, int[])
     */
    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        //binds the view component to the holder activity life cycle
        bindAppStateToActivityLifeCycle(bindAppState);
    }

    /**
     * Instantiates a surface view holder with XML attributes and a default style attribute.
     * On instantiating this surface view, the holder is bound directly to the
     * parent context life cycle.
     *
     * @param context      the parent context.
     * @param attrs        a collection of attributes describes the tags in an XML document.
     * @param defStyleAttr an attribute in the current theme that contains a
     *                     reference to a style resource that supplies
     *                     defaults values. Can be 0 to not look for defaults.
     * @see android.content.res.Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)
     */
    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //binds the view component to the holder activity life cycle
        bindAppStateToActivityLifeCycle(bindAppState);
    }

    /**
     * Instantiates a surface view holder with XML attributes, default style attribute and a default style resource.
     * On instantiating this surface view, the holder is bound directly to the
     * parent context life cycle.
     *
     * @param context      the parent context.
     * @param attrs        a collection of attributes describes the tags in an XML document.
     * @param defStyleAttr an attribute in the current theme that contains defaults. Can be 0 to not look for defaults.
     * @param defStyleRes  a resource identifier of a style resource that
     *                     supplies default values, used only if defStyleAttr is 0 or can not be found in the theme.
     *                     Can be 0 to not look for defaults.
     * @see android.content.res.Resources.Theme#obtainStyledAttributes(AttributeSet, int[], int, int)
     */
    public JmeSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        //binds the view component to the holder activity life cycle
        bindAppStateToActivityLifeCycle(bindAppState);
    }

    /**
     * Starts the jmeRenderer on a GlSurfaceView attached to a RelativeLayout.
     *
     * @param delayMillis delays the attachment of the surface view to the UI (RelativeLayout).
     */
    public void startRenderer(int delayMillis) {
        delayMillis = Math.max(0, delayMillis);
        /*gets the device configuration attributes from the activity manager*/
        configurationInfo = ((ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE)).getDeviceConfigurationInfo();
        glEsVersion = "GL_ES Version : " + configurationInfo.getGlEsVersion();
        /*sanity check the app instance*/
        if (legacyApplication == null) {
            throw new IllegalStateException("Cannot build a SurfaceView for a null app, make sure to use setLegacyApplication() to pass in your app !");
        }
        /*initialize App Settings and start the Game*/
        appSettings = new AppSettings(true);
        appSettings.setAudioRenderer(audioRendererType);
        appSettings.setResolution(JmeSurfaceView.this.getLayoutParams().width, JmeSurfaceView.this.getLayoutParams().height);
        appSettings.setAlphaBits(eglAlphaBits);
        appSettings.setDepthBits(eglDepthBits);
        appSettings.setSamples(eglSamples);
        appSettings.setStencilBits(eglStencilBits);
        appSettings.setBitsPerPixel(eglBitsPerPixel);
        appSettings.setEmulateKeyboard(emulateKeyBoard);
        appSettings.setEmulateMouse(emulateMouse);
        appSettings.setUseJoysticks(useJoyStickEvents);
        /*fetch and sanity check the static memory*/
        if (GameState.getLegacyApplication() != null) {
            this.legacyApplication = GameState.getLegacyApplication();
            jmeSurfaceViewLogger.log(Level.INFO, "Old game state has been assigned as the current game state, skipping the first update");
        } else {
            legacyApplication.setSettings(appSettings);
            jmeSurfaceViewLogger.log(Level.INFO, "Starting a new Game State");
            /*start jme game context*/
            legacyApplication.start();
            /*fire the onStart() listener*/
            if (onRendererStarted != null) {
                onRendererStarted.onRenderStart(legacyApplication, this);
            }
        }
        /*attach the game to JmE OpenGL.Renderer context*/
        oglesContext = (OGLESContext) legacyApplication.getContext();
        /*create a glSurfaceView that will hold the renderer thread*/
        glSurfaceView = oglesContext.createView(JmeSurfaceView.this.getContext());
        /*set the current view as the system engine thread view for future uses*/
        JmeAndroidSystem.setView(JmeSurfaceView.this);
        /*set JME system Listener to initialize game, update, requestClose and destroy on closure*/
        oglesContext.setSystemListener(JmeSurfaceView.this);
        /*set the glSurfaceView to fit the widget*/
        glSurfaceView.setLayoutParams(new LayoutParams(JmeSurfaceView.this.getLayoutParams().width, JmeSurfaceView.this.getLayoutParams().height));
        if (GameState.getLegacyApplication() != null) {
            addGlSurfaceView();
        } else {
            /*post delay the attachment of the surface view on the UI*/
            handler.postDelayed(rendererThread, delayMillis);
        }
    }

    private void removeGLSurfaceView() {
        ((Activity) getContext()).runOnUiThread(() -> {
            if (glSurfaceView != null) {
                JmeSurfaceView.this.removeView(glSurfaceView);
            }
        });
    }

    @Override
    public void handleError(String errorMsg, Throwable throwable) {
        throwable.printStackTrace();
        showErrorDialog(throwable, throwable.getClass().getName());
        if (onExceptionThrown != null) {
            onExceptionThrown.onExceptionThrown(throwable);
        }
    }

    /**
     * A state change observer to the holder Activity life cycle, used to keep this android view up-to-date with the holder activity life cycle.
     *
     * @param source the life cycle source, aka the observable object.
     * @param event  the fired event by the observable object, which is dispatched and sent to the observers.
     */
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_DESTROY:
                /*destroy only if the policy flag is enabled*/
                if (destructionPolicy == DestructionPolicy.DESTROY_WHEN_FINISH) {
                    legacyApplication.stop(!isGLThreadPaused());
                }
                break;
            case ON_PAUSE:
                loseFocus();
                break;
            case ON_RESUME:
                gainFocus();
                break;
            case ON_STOP:
                jmeSurfaceViewLogger.log(Level.INFO, "Context stops, but game is still running");
                break;
        }
    }

    @Override
    public void initialize() {
        /*Invoking can be delayed by delaying the draw of GlSurfaceView component on the screen*/
        if (legacyApplication == null) {
            return;
        }
        legacyApplication.initialize();
        /*log for display*/
        jmeSurfaceViewLogger.log(Level.INFO, "JmeGame started in GLThread Asynchronously.......");
    }

    @Override
    public void reshape(int width, int height) {
        if (legacyApplication == null) {
            return;
        }
        legacyApplication.reshape(width, height);
        jmeSurfaceViewLogger.log(Level.INFO, "Requested reshaping from the system listener");
    }

    @Override
    public void rescale(float x, float y) {
        if (legacyApplication == null) {
            return;
        }
        legacyApplication.rescale(x, y);
        jmeSurfaceViewLogger.log(Level.INFO, "Requested rescaling from the system listener");
    }

    @Override
    public void update() {
        /*Invoking can be delayed by delaying the draw of GlSurfaceView component on the screen*/
        if (legacyApplication == null || glSurfaceView == null) {
            return;
        }
        legacyApplication.update();
        if (!GameState.isFirstUpdatePassed()) {
            ((Activity) getContext()).runOnUiThread(() -> {
                jmeSurfaceViewLogger.log(Level.INFO, "User delay finishes with 0 errors");
                if (onRendererCompleted != null) {
                    onRendererCompleted.onRenderCompletion(legacyApplication, legacyApplication.getContext().getSettings());
                }
            });
            GameState.setFirstUpdatePassed(true);
        }
    }

    @Override
    public void requestClose(boolean esc) {
        /*skip if it's not enabled or the input is null*/
        if (legacyApplication == null || (!isExitOnEscPressed())) {
            return;
        }
        if (isShowEscExitPrompt()) {
            final AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
            alertDialog.setTitle("Exit Prompt");
            alertDialog.setMessage("Are you sure you want to quit ?");
            alertDialog.setCancelable(false);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", (dialogInterface, i) -> alertDialog.dismiss());
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", (dialogInterface, i) -> legacyApplication.requestClose(esc));
            alertDialog.show();
        } else {
            legacyApplication.requestClose(esc);
        }
    }

    @Override
    public void gainFocus() {
        /*skip the block if the instances are nullptr*/
        if (legacyApplication == null || glSurfaceView == null) {
            return;
        }
        glSurfaceView.onResume();
        /*resume the audio*/
        final AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
        if (audioRenderer != null) {
            audioRenderer.resumeAll();
        }
        /*resume the sensors (aka joysticks)*/
        if (legacyApplication.getContext() != null) {
            final JoyInput joyInput = legacyApplication.getContext().getJoyInput();
            if (joyInput != null) {
                if (joyInput instanceof AndroidSensorJoyInput) {
                    final AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                    androidJoyInput.resumeSensors();
                }
            }
            legacyApplication.gainFocus();
        }
        setGLThreadPaused(false);
        jmeSurfaceViewLogger.log(Level.INFO, "Game returns from the idle mode");
    }

    @Override
    public void loseFocus() {
        /*skip the block if the invoking instances are nullptr*/
        if (legacyApplication == null || glSurfaceView == null) {
            return;
        }
        glSurfaceView.onPause();
        /*pause the audio*/
        legacyApplication.loseFocus();
        final AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
        if (audioRenderer != null) {
            audioRenderer.pauseAll();
        }
        /*pause the sensors (aka joysticks)*/
        if (legacyApplication.getContext() != null) {
            final JoyInput joyInput = legacyApplication.getContext().getJoyInput();
            if (joyInput != null) {
                if (joyInput instanceof AndroidSensorJoyInput) {
                    final AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                    androidJoyInput.pauseSensors();
                }
            }
        }
        setGLThreadPaused(true);
        jmeSurfaceViewLogger.log(Level.INFO, "Game goes idle");
    }

    @Override
    public void destroy() {
        /*skip the destroy block if the invoking instance is null*/
        if (legacyApplication == null) {
            return;
        }
        removeGLSurfaceView();
        legacyApplication.destroy();
        /*help the Dalvik Garbage collector to destruct the pointers, by making them nullptr*/
        /*context instances*/
        legacyApplication = null;
        appSettings = null;
        oglesContext = null;
        configurationInfo = null;
        /*extra data instances*/
        crashLogWriter = null;
        crashLog = null;
        /*nullifying helper instances and flags*/
        rendererThread = null;
        destructionPolicy = null;
        audioRendererType = null;
        handler = null;
        glEsVersion = null;
        /*nullifying the event handlers*/
        onRendererStarted = null;
        onRendererCompleted = null;
        onExceptionThrown = null;
        onLayoutDrawn = null;
        /*nullifying the static memory (pushing zero to registers to prepare for a clean use)*/
        GameState.setLegacyApplication(null);
        GameState.setFirstUpdatePassed(false);
        jmeSurfaceViewLogger.log(Level.INFO, "Context and Game have been destructed");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                ((Activity) getContext()).finish();
                break;
            case DialogInterface.BUTTON_POSITIVE:
                dialog.dismiss();
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                /*copy crash log button*/
                final ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                final ClipData clipData = ClipData.newPlainText("Crash Log", crashLog);
                clipboardManager.setPrimaryClip(clipData);
                Toast.makeText(getContext(), "Crash Log copied to clipboard", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Adds the glSurfaceView to the screen immediately, saving the current app instance.
     */
    protected void addGlSurfaceView() {
        /*jme Renderer joins the UIThread at that point*/
        JmeSurfaceView.this.addView(glSurfaceView);
        /*dispatch the layout drawn event*/
        if (onLayoutDrawn != null) {
            onLayoutDrawn.onLayoutDrawn(legacyApplication, this);
        }
        /*set the static memory to hold the game state, only if the destruction policy uses KEEP_WHEN_FINISHED policy*/
        if (destructionPolicy == DestructionPolicy.KEEP_WHEN_FINISH) {
            GameState.setLegacyApplication(legacyApplication);
        } else {
            GameState.setLegacyApplication(null);
        }
    }

    /**
     * Displays an error dialog with a throwable title(error/exception), message and 3 buttons.
     * 1st button is : EXIT to exit the activity and terminates the app.
     * 2nd button is : DISMISS to dismiss the dialog and ignore the exception.
     * 3rd button is : CopyCrashLog to copy the crash log to the clipboard.
     *
     * @param throwable the throwable stack.
     * @param title     the message title.
     */
    protected void showErrorDialog(Throwable throwable, String title) {
        if (!isShowErrorDialog()) {
            return;
        }
        ((Activity) getContext()).runOnUiThread(() -> {
            throwable.printStackTrace(new PrintWriter(crashLogWriter));
            crashLog = glEsVersion + "\n" + crashLogWriter.toString();

            final AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
            alertDialog.setTitle(glEsVersion + ", " + title);
            alertDialog.setMessage(crashLog);
            alertDialog.setCancelable(false);
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Exit", this);
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Dismiss", this);
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Copy crash log", this);
            alertDialog.show();
        });
    }

    /**
     * Binds/Unbinds the game life cycle to the holder activity life cycle.
     * Unbinding the game life cycle, would disable {@link JmeSurfaceView#gainFocus()}, {@link JmeSurfaceView#loseFocus()}
     * and {@link JmeSurfaceView#destroy()} from being invoked by the System Listener.
     * The Default value is : true, and the view component is pre-bounded to its activity lifeCycle when initialized.
     *
     * @param condition true if you want to bind them, false otherwise.
     */
    public void bindAppStateToActivityLifeCycle(final boolean condition) {
        this.bindAppState = condition;
        if (condition) {
            /*register this Ui Component as an observer to the context of jmeSurfaceView only if this context is a LifeCycleOwner*/
            if (getContext() instanceof LifecycleOwner) {
                ((LifecycleOwner) getContext()).getLifecycle().addObserver(JmeSurfaceView.this);
            }
        } else {
            /*un-register this Ui Component as an observer to the context of jmeSurfaceView only if this context is a LifeCycleOwner*/
            if (getContext() instanceof LifecycleOwner) {
                ((LifecycleOwner) getContext()).getLifecycle().removeObserver(JmeSurfaceView.this);
            }
        }
    }

    /**
     * Gets the current destruction policy.
     * Default value is : {@link DestructionPolicy#DESTROY_WHEN_FINISH}.
     *
     * @return the destruction policy, either {@link DestructionPolicy#DESTROY_WHEN_FINISH} or {@link DestructionPolicy#KEEP_WHEN_FINISH}.
     * @see DestructionPolicy
     * @see GameState
     */
    public DestructionPolicy getDestructionPolicy() {
        return destructionPolicy;
    }

    /**
     * Sets the current destruction policy, destruction policy {@link DestructionPolicy#KEEP_WHEN_FINISH} ensures that we protect the app state
     * using {@link GameState#legacyApplication} static memory when the activity finishes, while
     * {@link DestructionPolicy#DESTROY_WHEN_FINISH} destroys the game context with the activity onDestroy().
     * Default value is : {@link DestructionPolicy#DESTROY_WHEN_FINISH}.
     *
     * @param destructionPolicy a destruction policy to set.
     * @see DestructionPolicy
     * @see GameState
     */
    public void setDestructionPolicy(DestructionPolicy destructionPolicy) {
        this.destructionPolicy = destructionPolicy;
    }

    /**
     * Checks whether the current game application life cycle is bound to the activity life cycle.
     *
     * @return true it matches the condition, false otherwise.
     */
    public boolean isAppStateBoundToActivityLifeCycle() {
        return bindAppState;
    }

    /**
     * Checks whether the system would show an exit prompt dialog when the esc keyboard input is invoked.
     *
     * @return ture if the exit prompt dialog is activated on exit, false otherwise.
     */
    public boolean isShowEscExitPrompt() {
        return showEscExitPrompt;
    }

    /**
     * Determines whether to show an exit prompt dialog when the esc keyboard button is invoked.
     *
     * @param showEscExitPrompt true to show the exit prompt dialog before exiting, false otherwise.
     */
    public void setShowEscExitPrompt(boolean showEscExitPrompt) {
        this.showEscExitPrompt = showEscExitPrompt;
    }

    /**
     * Checks whether the exit on esc press is activated.
     *
     * @return true if the exit on escape is activated, false otherwise.
     */
    public boolean isExitOnEscPressed() {
        return exitOnEscPressed;
    }

    /**
     * Determines whether the system would exit on pressing the keyboard esc button.
     *
     * @param exitOnEscPressed true to activate exiting on Esc button press, false otherwise.
     */
    public void setExitOnEscPressed(boolean exitOnEscPressed) {
        this.exitOnEscPressed = exitOnEscPressed;
    }

    /**
     * Gets the jme app instance.
     *
     * @return legacyApplication instance representing your game enclosure.
     */
    public LegacyApplication getLegacyApplication() {
        return legacyApplication;
    }

    /**
     * Sets the jme game instance that will be engaged into the {@link SystemListener}.
     *
     * @param legacyApplication your jme game instance.
     */
    public void setLegacyApplication(@NonNull LegacyApplication legacyApplication) {
        this.legacyApplication = legacyApplication;
    }

    /**
     * Gets the game window settings.
     *
     * @return app settings instance.
     */
    public AppSettings getAppSettings() {
        return appSettings;
    }

    /**
     * Sets the appSettings instance.
     *
     * @param appSettings the custom appSettings instance
     */
    public void setAppSettings(@NonNull AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    /**
     * Gets the bits/pixel for Embedded gL
     *
     * @return integer representing it.
     */
    public int getEglBitsPerPixel() {
        return eglBitsPerPixel;
    }

    /**
     * Sets the memory representing each pixel in bits.
     *
     * @param eglBitsPerPixel the bits for each pixel.
     */
    public void setEglBitsPerPixel(int eglBitsPerPixel) {
        this.eglBitsPerPixel = eglBitsPerPixel;
    }

    /**
     * Gets the Embedded gL alpha(opacity) bits.
     *
     * @return integer representing it.
     */
    public int getEglAlphaBits() {
        return eglAlphaBits;
    }

    /**
     * Sets the memory representing the alpha of embedded gl in bits.
     *
     * @param eglAlphaBits the alpha bits.
     */
    public void setEglAlphaBits(int eglAlphaBits) {
        this.eglAlphaBits = eglAlphaBits;
    }

    /**
     * Gets the memory representing the EGL depth in bits.
     *
     * @return the depth bits.
     */
    public int getEglDepthBits() {
        return eglDepthBits;
    }

    /**
     * Sets the EGL depth in bits.
     * The depth buffer or Z-buffer is basically coupled with stencil buffer,
     * usually 8bits stencilBuffer + 24bits depthBuffer = 32bits shared memory.
     *
     * @param eglDepthBits the depth bits.
     * @see JmeSurfaceView#setEglStencilBits(int)
     */
    public void setEglDepthBits(int eglDepthBits) {
        this.eglDepthBits = eglDepthBits;
    }

    /**
     * Gets the number of samples to use for multi-sampling.
     *
     * @return number of samples to use for multi-sampling.
     */
    public int getEglSamples() {
        return eglSamples;
    }

    /**
     * Sets the number of samples to use for multi-sampling.
     * Leave 0 (default) to disable multi-sampling.
     * Set to 2 or 4 to enable multi-sampling.
     *
     * @param eglSamples embedded gl samples bits to set.
     */
    public void setEglSamples(int eglSamples) {
        this.eglSamples = eglSamples;
    }

    /**
     * Gets the number of stencil buffer bits.
     * Default is : 0.
     *
     * @return the stencil buffer bits.
     */
    public int getEglStencilBits() {
        return eglStencilBits;
    }

    /**
     * Sets the number of stencil buffer bits.
     * Stencil buffer is used in depth-based shadow maps and shadow rendering as it limits rendering,
     * it's coupled with Z-buffer or depth buffer, usually 8bits stencilBuffer + 24bits depthBuffer = 32bits shared memory.
     * (default = 0)
     *
     * @param eglStencilBits the desired number of stencil bits.
     * @see JmeSurfaceView#setEglDepthBits(int)
     */
    public void setEglStencilBits(int eglStencilBits) {
        this.eglStencilBits = eglStencilBits;
    }

    /**
     * Gets the limited FrameRate level for egl INFO.
     * Default is : -1, for a device based limited value (determined by hardware).
     *
     * @return the limit frameRate in integers.
     */
    public int getFrameRate() {
        return frameRate;
    }

    /**
     * Limits the frame rate (fps) in the second.
     * Default is : -1, for a device based limited value (determined by hardware).
     *
     * @param frameRate the limitation in integers.
     */
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    /**
     * Gets the audio renderer in String.
     * Default is : {@link AppSettings#ANDROID_OPENAL_SOFT}.
     *
     * @return string representing audio renderer framework.
     */
    public String getAudioRendererType() {
        return audioRendererType;
    }

    /**
     * Sets the audioRenderer type.
     * Default is : {@link AppSettings#ANDROID_OPENAL_SOFT}.
     *
     * @param audioRendererType string representing audioRenderer type.
     */
    public void setAudioRendererType(String audioRendererType) {
        this.audioRendererType = audioRendererType;
    }

    /**
     * Checks if the keyboard interfacing is enabled.
     * Default is : true.
     *
     * @return true if the keyboard interfacing is enabled.
     */
    public boolean isEmulateKeyBoard() {
        return emulateKeyBoard;
    }

    /**
     * Enables keyboard interfacing.
     * Default is : true.
     *
     * @param emulateKeyBoard true to enable keyboard interfacing.
     */
    public void setEmulateKeyBoard(boolean emulateKeyBoard) {
        this.emulateKeyBoard = emulateKeyBoard;
    }

    /**
     * Checks whether the mouse interfacing is enabled or not.
     * Default is : true.
     *
     * @return true if the mouse interfacing is enabled.
     */
    public boolean isEmulateMouse() {
        return emulateMouse;
    }

    /**
     * Enables mouse interfacing.
     * Default is : true.
     *
     * @param emulateMouse true to enable the mouse interfacing.
     */
    public void setEmulateMouse(boolean emulateMouse) {
        this.emulateMouse = emulateMouse;
    }

    /**
     * Checks whether joystick interfacing is enabled or not.
     * Default is : true.
     *
     * @return true if the joystick interfacing is enabled.
     */
    public boolean isUseJoyStickEvents() {
        return useJoyStickEvents;
    }

    /**
     * Enables joystick interfacing for a jme-game
     *
     * @param useJoyStickEvents true to enable the joystick interfacing.
     */
    public void setUseJoyStickEvents(boolean useJoyStickEvents) {
        this.useJoyStickEvents = useJoyStickEvents;
    }

    /**
     * Checks whether the GLThread is paused or not.
     *
     * @return true/false
     */
    public boolean isGLThreadPaused() {
        return isGLThreadPaused;
    }

    /**
     * Sets GL Thread paused.
     *
     * @param GLThreadPaused true if you want to pause the GLThread.
     */
    protected void setGLThreadPaused(boolean GLThreadPaused) {
        isGLThreadPaused = GLThreadPaused;
    }

    /**
     * Sets the listener for the completion of rendering, ie : when the GL thread holding the {@link JmeSurfaceView}
     * joins the UI thread, after asynchronous rendering.
     *
     * @param onRendererCompleted an instance of the interface {@link OnRendererCompleted}.
     */
    public void setOnRendererCompleted(OnRendererCompleted onRendererCompleted) {
        this.onRendererCompleted = onRendererCompleted;
    }

    /**
     * Sets the listener that will fire when an exception is thrown.
     *
     * @param onExceptionThrown an instance of the interface {@link OnExceptionThrown}.
     */
    public void setOnExceptionThrown(OnExceptionThrown onExceptionThrown) {
        this.onExceptionThrown = onExceptionThrown;
    }

    /**
     * Sets the listener that will fire after initializing the game.
     *
     * @param onRendererStarted an instance of the interface {@link OnRendererStarted}.
     */
    public void setOnRendererStarted(OnRendererStarted onRendererStarted) {
        this.onRendererStarted = onRendererStarted;
    }

    /**
     * Sets the listener that will dispatch an event when the layout is drawn by {@link JmeSurfaceView#addGlSurfaceView()}.
     *
     * @param onLayoutDrawn the event to be dispatched.
     * @see JmeSurfaceView#addGlSurfaceView()
     */
    public void setOnLayoutDrawn(OnLayoutDrawn onLayoutDrawn) {
        this.onLayoutDrawn = onLayoutDrawn;
    }

    /**
     * Gets the current device GL_ES version.
     *
     * @return the current gl_es version in a string format.
     */
    public String getGlEsVersion() {
        return configurationInfo.getGlEsVersion();
    }

    /**
     * Checks whether the error dialog is enabled upon encountering exceptions/errors.
     * Default is : true.
     *
     * @return true if the error dialog is activated, false otherwise.
     */
    public boolean isShowErrorDialog() {
        return showErrorDialog;
    }

    /**
     * Determines whether the error dialog would be shown on encountering exceptions.
     * Default is : true.
     *
     * @param showErrorDialog true to activate the error dialog, false otherwise.
     */
    public void setShowErrorDialog(boolean showErrorDialog) {
        this.showErrorDialog = showErrorDialog;
    }

    /**
     * Determines whether the app context would be destructed
     * with the holder activity context in case of {@link DestructionPolicy#DESTROY_WHEN_FINISH} or be
     * spared for a second use in case of {@link DestructionPolicy#KEEP_WHEN_FINISH}.
     * Default value is : {@link DestructionPolicy#DESTROY_WHEN_FINISH}.
     *
     * @see JmeSurfaceView#setDestructionPolicy(DestructionPolicy)
     */
    public enum DestructionPolicy {
        /**
         * Finishes the game context with the activity context (ignores the static memory {@link GameState#legacyApplication}).
         */
        DESTROY_WHEN_FINISH,
        /**
         * Spares the game context inside a static memory {@link GameState#legacyApplication}
         * when the activity context is destroyed, but the app stills in the background.
         */
        KEEP_WHEN_FINISH
    }

    /**
     * Used as a static memory to protect the game context from destruction by Activity#onDestroy().
     *
     * @see DestructionPolicy
     * @see JmeSurfaceView#setDestructionPolicy(DestructionPolicy)
     */
    protected static final class GameState {

        private static LegacyApplication legacyApplication;
        private static boolean firstUpdatePassed = false;

        /**
         * Private constructor to inhibit instantiation of this class.
         */
        private GameState() {
        }

        /**
         * Returns the current application state.
         *
         * @return game state instance, holding jME3 states (JmeContext, AssetManager, StateManager, Graphics, Sound, Input, Spatial/Nodes in place, etcetera).
         */
        protected static LegacyApplication getLegacyApplication() {
            return legacyApplication;
        }

        /**
         * Replaces the current application state.
         *
         * @param legacyApplication the new app instance holding the game state (including {@link AssetLoader}s, {@link AudioNode}s, {@link Spatial}s, etcetera).
         */
        protected static void setLegacyApplication(LegacyApplication legacyApplication) {
            GameState.legacyApplication = legacyApplication;
        }

        /**
         * Tests the first update flag.
         *
         * @return true if the firstUpdate has passed, false otherwise.
         */
        protected static boolean isFirstUpdatePassed() {
            return firstUpdatePassed;
        }

        /**
         * Adjusts the first update flag.
         *
         * @param firstUpdatePassed set to true to determine whether the firstUpdate has passed, false otherwise.
         */
        protected static void setFirstUpdatePassed(boolean firstUpdatePassed) {
            GameState.firstUpdatePassed = firstUpdatePassed;
        }
    }

    /**
     * Delays the attachment surface view on the UI for the sake of initial frame pacing and splash screens,
     * delaying the display of the game (GlSurfaceView) would lead to a substantial delay in the
     * {@link android.opengl.GLSurfaceView.Renderer#onDrawFrame(javax.microedition.khronos.opengles.GL10)} which would
     * delay invoking both {@link LegacyApplication#initialize()} and {@link LegacyApplication#update()}.
     *
     * @see JmeSurfaceView#startRenderer(int)
     * @see com.jme3.system.android.OGLESContext#onDrawFrame(javax.microedition.khronos.opengles.GL10)
     */
    private class RendererThread implements Runnable {
        /**
         * Delays the {@link GLSurfaceView} attachment on the UI thread.
         *
         * @see JmeSurfaceView#startRenderer(int)
         */
        @Override
        public void run() {
            addGlSurfaceView();
            jmeSurfaceViewLogger.log(Level.INFO, "JmeSurfaceView's joined the UI thread");
        }
    }
}
