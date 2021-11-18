/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.app.jmeSurfaceView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jme3.app.LegacyApplication;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.android.AndroidSensorJoyInput;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <b>A RelativeLayout Class Holder that holds a #{{@link GLSurfaceView}} using #{{@link OGLESContext}} as a renderer to render
 * a JME game on an android view for custom xmL designs.</b>
 * The main idea of #{@link JmeSurfaceView} class is to start a jMonkeyEngine application in a {@link SystemListener} context in a GL_ES thread ,
 * then the GLSurfaceView holding the GL_ES thread joins the UI thread with a delay of user's choice using a #{@link Handler} , during the delay , the game runs normally in the GL_ES thread(but without coming up on the UI)
 * and the user has the ability to handle a couple of actions asynchronously as displaying a progress bar on a SplashScreen or
 * an image or even play a preface game music of choice.
 *
 * @author pavl_g.
 */
public class JmeSurfaceView extends RelativeLayout implements SystemListener, DialogInterface.OnClickListener {

    /*using #{@link LegacyApplication} instead of #{@link SimpleApplication} to include all classes extends LegacyApplication*/
    private LegacyApplication legacyApplication;
    protected String audioRendererType = AppSettings.ANDROID_OPENAL_SOFT;
    private static final Logger jmeSurfaceViewLogger = Logger.getLogger("JmeSurfaceView");
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
    private final Handler handler = new Handler();
    private GLSurfaceView glSurfaceView;
    private OnRendererCompleted onRendererCompleted;
    private final AtomicInteger synthesizedTime = new AtomicInteger();
    private OnExceptionThrown onExceptionThrown;
    public static final int NO_DELAY = 1;
    private int delayMillis = NO_DELAY;
    private static final int TOLERANCE_TIMER = 100;

    public JmeSurfaceView(@NonNull Context context) {
        super(context);
    }

    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JmeSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * starts the jmeRenderer on a GlSurfaceView attached to a RelativeLayout.
     * @param delayMillis delay of the appearance of jme game on the screen , this doesn't delay the renderer though.
     * Use #{@link JmeSurfaceView#NO_DELAY} to disable the delay ,
     * any values less than 1ms#{@link JmeSurfaceView#NO_DELAY} would be ignored and the delay would be disabled.
     */
    public void startRenderer(int delayMillis) {
        this.delayMillis = Math.max(NO_DELAY, delayMillis);
        if (legacyApplication != null) {
            try {
                /*initialize App Settings & start the Game*/
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
                legacyApplication.setSettings(appSettings);
                /*start jme game context*/
                legacyApplication.start();
                /*attach the game to JmE OpenGL.Renderer context */
                OGLESContext oglesContext = (OGLESContext) legacyApplication.getContext();
                /*create a glSurfaceView that will hold the renderer thread*/
                glSurfaceView = oglesContext.createView(JmeSurfaceView.this.getContext());
                /*set the current view as the system engine thread view for future uses*/
                JmeAndroidSystem.setView(JmeSurfaceView.this);
                /*set JME system Listener to initialize game , update , requestClose & destroy on closure*/
                oglesContext.setSystemListener(JmeSurfaceView.this);
                /* set the glSurfaceView to fit the widget */
                glSurfaceView.setLayoutParams(new LayoutParams(JmeSurfaceView.this.getLayoutParams().width, JmeSurfaceView.this.getLayoutParams().height));
                /*post delay the renderer join into the UI thread*/
                handler.postDelayed(new RendererThread(), delayMillis);
            } catch (Exception e) {
                jmeSurfaceViewLogger.log(Level.WARNING, e.getMessage());
                showErrorDialog(e, e.getMessage());
                if (onExceptionThrown != null) {
                    onExceptionThrown.onExceptionThrown(e);
                }
            }
        }
    }

    /**
     * Custom thread that delays the appearance of the display of jme game on the screen for the sake of initial frame pacing & splash screens.
     */
    private class RendererThread implements Runnable {
        /**
         * Delays the #{@link GLSurfaceView} add on the UI thread.
         * @see JmeSurfaceView#NO_DELAY
         * @see JmeSurfaceView#startRenderer(int)
         */
        @Override
        public void run() {
            /*jme Renderer joins the UIThread at that point*/
            JmeSurfaceView.this.addView(glSurfaceView);
            jmeSurfaceViewLogger.log(Level.CONFIG, "JmeSurfaceView's joined the UI thread.......");
        }
    }

    @Override
    public void initialize() {
        if (legacyApplication != null) {
            legacyApplication.initialize();
            /*log for display*/
            jmeSurfaceViewLogger.log(Level.INFO, "JmeGame started in GLThread Asynchronously.......");
        }
    }

    @Override
    public void reshape(int width, int height) {
        if (legacyApplication != null) {
            legacyApplication.reshape(width, height);
        }
    }

    @Override
    public void update() {
        if (legacyApplication == null) {
            return;
        }
        if (glSurfaceView != null) {
            legacyApplication.update();
        }
        int timeToPlay = synthesizedTime.addAndGet(1);
        if (timeToPlay == (delayMillis > 100 ? (delayMillis - TOLERANCE_TIMER) : delayMillis)) {
            ((Activity) getContext()).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    jmeSurfaceViewLogger.log(Level.INFO, "SplashScreen Dismissed, User Delay completed with 0 errors.......");
                    if (onRendererCompleted != null) {
                        onRendererCompleted.onRenderCompletion(legacyApplication, legacyApplication.getContext().getSettings());
                    }
                }
            });
        }
    }

    @Override
    public void requestClose(boolean esc) {
        if (legacyApplication != null) {
            legacyApplication.requestClose(esc);
        }
    }

    @Override
    public void gainFocus() {
        if (legacyApplication != null) {
            /*resume the audio*/
            AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.resumeAll();
            }
            /*resume the sensors (aka joysticks)*/
            if (legacyApplication.getContext() != null) {
                JoyInput joyInput = legacyApplication.getContext().getJoyInput();
                if (joyInput != null) {
                    if (joyInput instanceof AndroidSensorJoyInput) {
                        AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                        androidJoyInput.resumeSensors();
                    }
                }
                legacyApplication.gainFocus();
            }
        }
        setGLThreadPaused(false);
    }

    @Override
    public void loseFocus() {
        if (legacyApplication != null) {
            /*pause the audio*/
            legacyApplication.loseFocus();
            AudioRenderer audioRenderer = legacyApplication.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.pauseAll();
            }
            /*pause the sensors (aka joysticks)*/
            if (legacyApplication.getContext() != null) {
                JoyInput joyInput = legacyApplication.getContext().getJoyInput();
                if (joyInput != null) {
                    if (joyInput instanceof AndroidSensorJoyInput) {
                        AndroidSensorJoyInput androidJoyInput = (AndroidSensorJoyInput) joyInput;
                        androidJoyInput.pauseSensors();
                    }
                }
            }
        }
        setGLThreadPaused(true);
    }

    @Override
    public void handleError(String errorMsg, Throwable throwable) {
        jmeSurfaceViewLogger.log(Level.WARNING, throwable.getMessage());
        showErrorDialog(throwable, throwable.getMessage());
        if (onExceptionThrown != null) {
            onExceptionThrown.onExceptionThrown(throwable);
        }
    }

    @Override
    public void destroy() {
        if (legacyApplication != null) {
            legacyApplication.stop(isGLThreadPaused());
            legacyApplication.destroy();
        }
    }

    /**
     * Displays an error dialog with a throwable message(error/exception) and 2 buttons.
     * @param throwable the throwable stack.
     * @param message the string message.
     */
    protected void showErrorDialog(final Throwable throwable, final String message) {
        ((Activity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                alertDialog.setTitle(new StringBuffer(String.valueOf(throwable)));
                alertDialog.setMessage(message);
                alertDialog.setCancelable(true);
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "EXIT", JmeSurfaceView.this);
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "DISMISS", JmeSurfaceView.this);
                alertDialog.show();
            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                dialog.dismiss();
                ((Activity)getContext()).finish();
                break;
            case DialogInterface.BUTTON_POSITIVE:
                dialog.dismiss();
                break;
        }
    }

    /**
     * sets the jme game instance that will be engaged into the {@link SystemListener}.
     * @param legacyApplication your jme game instance.
     */
    public void setLegacyApplication(@NonNull LegacyApplication legacyApplication) {
        this.legacyApplication = legacyApplication;
    }

    /**
     * gets the jme app instance
     * @return legacyApplication instance representing your game enclosure.
     */
    public LegacyApplication getLegacyApplication() {
        return legacyApplication;
    }

    /**
     * sets the appSettings instance.
     * @param appSettings the custom appSettings instance
     */
    public void setAppSettings(@NonNull AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    /**
     * gets the game window settings.
     * @return app settings instance.
     */
    public AppSettings getAppSettings() {
        return appSettings;
    }

    /**
     * sets the memory representing each pixel in bits.
     * @param eglBitsPerPixel the bits for each pixel.
     */
    public void setEglBitsPerPixel(int eglBitsPerPixel) {
        this.eglBitsPerPixel = eglBitsPerPixel;
    }

    /**
     * gets the bits/pixel for Embedded gL
     * @return integer representing it.
     */
    public int getEglBitsPerPixel() {
        return eglBitsPerPixel;
    }

    /**
     * sets the memory representing the alpha of embedded gl in bits.
     * @param eglAlphaBits the alpha bits.
     */
    public void setEglAlphaBits(int eglAlphaBits) {
        this.eglAlphaBits = eglAlphaBits;
    }

    /**
     * gets the Embedded gL alpha(opacity) bits.
     * @return integer representing it.
     */
    public int getEglAlphaBits() {
        return eglAlphaBits;
    }

    /**
     * sets the EGL depth in bits.
     * the depth buffer or Z-buffer is basically coupled with stencil buffer ,
     * usually 8bits stencilBuffer + 24bits depthBuffer = 32bits shared memory.
     * @param eglDepthBits the depth bits.
     * @see JmeSurfaceView#setEglStencilBits(int)
     */
    public void setEglDepthBits(int eglDepthBits) {
        this.eglDepthBits = eglDepthBits;
    }

    /**
     * gets the memory representing the EGL depth in bits.
     * @return the depth bits.
     */
    public int getEglDepthBits() {
        return eglDepthBits;
    }

    /**
     * Sets the number of samples to use for multi-sampling.
     * Leave 0 (default) to disable multi-sampling.
     * Set to 2 or 4 to enable multi-sampling.
     * @param eglSamples embedded gl samples bits to set.
     */
    public void setEglSamples(int eglSamples) {
        this.eglSamples = eglSamples;
    }

    /**
     * get the number of samples to use for multi-sampling.
     * @return number of samples to use for multi-sampling.
     */
    public int getEglSamples() {
        return eglSamples;
    }

    /**
     * Set the number of stencil buffer bits.
     * Stencil buffer is used in depth-based shadow maps and shadow rendering as it limits rendering,
     * it's coupled with Z-buffer or depth buffer,usually 8bits stencilBuffer + 24bits depthBuffer = 32bits shared memory.
     * (default = 0)
     * @param eglStencilBits the desired number of stencil bits.
     * @see JmeSurfaceView#setEglDepthBits(int)
     */
    public void setEglStencilBits(int eglStencilBits) {
        this.eglStencilBits = eglStencilBits;
    }

    /**
     * gets the number of stencil buffer bits.
     * @return the stencil buffer bits.
     */
    public int getEglStencilBits() {
        return eglStencilBits;
    }

    /**
     * limits the frame rate (fps) in the second.
     * @param frameRate the limitation in integers.
     */
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    /**
     * gets the limited FrameRate level for egl config.
     *
     * @return the limit frameRate in integers.
     */
    public int getFrameRate() {
        return frameRate;
    }

    /**
     * sets the audioRenderer type.
     * {@link AppSettings#ANDROID_OPENAL_SOFT}.
     * @param audioRendererType string representing audioRenderer type.
     */
    public void setAudioRendererType(String audioRendererType) {
        this.audioRendererType = audioRendererType;
    }

    /**
     * gets the audio renderer in String.
     * @return string representing audio renderer framework.
     */
    public String getAudioRendererType() {
        return audioRendererType;
    }

    /**
     * enables keyboard interfacing.
     * @param emulateKeyBoard true to enable keyboard interfacing.
     */
    public void setEmulateKeyBoard(boolean emulateKeyBoard) {
        this.emulateKeyBoard = emulateKeyBoard;
    }

    /**
     * checks if the keyboard interfacing is enabled.
     * @return true if the keyboard interfacing is enabled.
     */
    public boolean isEmulateKeyBoard() {
        return emulateKeyBoard;
    }

    /**
     * enables mouse interfacing.
     * @param emulateMouse true to enable the mouse interfacing.
     */
    public void setEmulateMouse(boolean emulateMouse) {
        this.emulateMouse = emulateMouse;
    }

    /**
     * checks whether the mouse interfacing is enabled or not.
     * @return true if the mouse interfacing is enabled.
     */
    public boolean isEmulateMouse() {
        return emulateMouse;
    }

    /**
     * enable joystick interfacing for a jme-game
     * @param useJoyStickEvents true to enable the joystick interfacing.
     */
    public void setUseJoyStickEvents(boolean useJoyStickEvents) {
        this.useJoyStickEvents = useJoyStickEvents;
    }

    /**
     * checks whether joystick interfacing is enabled or not.
     * @return true if the joystick interfacing is enabled.
     */
    public boolean isUseJoyStickEvents() {
        return useJoyStickEvents;
    }

    /**
     * sets GL Thread paused.
     * @param GLThreadPaused true if you want to pause the GLThread.
     */
    protected void setGLThreadPaused(boolean GLThreadPaused) {
        isGLThreadPaused = GLThreadPaused;
    }

    /**
     * checks whether the GLThread is paused or not.
     * @return true/false
     */
    public boolean isGLThreadPaused() {
        return isGLThreadPaused;
    }

    /**
     * sets the listener for the completion of rendering , ie : when the GL thread holding the #{@link JmeSurfaceView}
     * joins the UI thread , after asynchronous rendering.
     * @param onRendererCompleted an instance of the interface #{@link OnRendererCompleted}.
     */
    public void setOnRendererCompleted(OnRendererCompleted onRendererCompleted) {
        this.onRendererCompleted = onRendererCompleted;
    }

    /**
     * sets the listener that will fire when an exception is thrown.
     * @param onExceptionThrown an instance of the interface #{@link OnExceptionThrown}.
     */
    public void setOnExceptionThrown(OnExceptionThrown onExceptionThrown) {
        this.onExceptionThrown = onExceptionThrown;
    }

}
