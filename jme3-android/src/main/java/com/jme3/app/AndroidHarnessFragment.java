/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.app;

import android.app.AlertDialog;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.JoyInput;
import com.jme3.input.android.AndroidJoyInput;
import com.jme3.system.AppSettings;
import com.jme3.system.SystemListener;
import com.jme3.system.android.JmeAndroidSystem;
import com.jme3.system.android.OGLESContext;
import com.jme3.util.AndroidLogHandler;
import com.jme3.util.AndroidNativeBufferAllocator;
import com.jme3.util.BufferAllocatorFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * AndroidX Fragment that hosts a jME application on Android 11+.
 *
 * <p>The fragment is intentionally small: subclasses create the application and
 * optionally customize settings; this class owns the Android view lifecycle and
 * forwards render callbacks to {@link LegacyApplication}.</p>
 */
public abstract class AndroidHarnessFragment extends Fragment implements SystemListener {
    private static final Logger logger = Logger.getLogger(AndroidHarnessFragment.class.getName());

    protected GLSurfaceView view;
    protected LegacyApplication app;
    protected boolean finishOnAppStop = true;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public Application getJmeApplication() {
        return app;
    }

    public void setFinishOnAppStop(boolean finishOnAppStop) {
        this.finishOnAppStop = finishOnAppStop;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        initializeLogHandler();
        logger.fine("onCreate");
        super.onCreate(savedInstanceState);

        System.setProperty(
                BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION,
                AndroidNativeBufferAllocator.class.getName());

        try {
            app = createApplication();
            app.start();
            OGLESContext context = (OGLESContext) app.getContext();
            context.setSystemListener(this);
        } catch (Exception exception) {
            handleError("jME application initialization failed", exception);
        }
    }

    /**
     * Creates the jME application hosted by this fragment.
     *
     * @return the application instance
     * @throws Exception if the application cannot be created
     */
    protected abstract LegacyApplication createApplication() throws Exception;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logger.fine("onCreateView");
        if (app == null) {
            return new View(requireContext());
        }

        view = ((OGLESContext) app.getContext()).createView(requireActivity());
        JmeAndroidSystem.setView(view);
        return view;
    }

    @Override
    public void onResume() {
        logger.fine("onResume");
        super.onResume();
        gainFocus();
    }

    @Override
    public void onPause() {
        logger.fine("onPause");
        loseFocus();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        logger.fine("onDestroyView");
        if (view != null && view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        view = null;
        JmeAndroidSystem.setView(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        logger.fine("onDestroy");
        if (app != null) {
            app.stop(false);
        }
        app = null;
        super.onDestroy();
    }

    @Override
    public void handleError(final String errorMsg, final Throwable throwable) {
        String stackTrace = "";
        String title = "Error";

        if (throwable != null) {
            StringWriter writer = new StringWriter(100);
            throwable.printStackTrace(new PrintWriter(writer));
            stackTrace = writer.toString();
            title = throwable.toString();
        }

        final String finalTitle = title;
        final String finalMessage = (errorMsg != null ? errorMsg : "Uncaught Exception")
                + "\n" + stackTrace;

        logger.log(Level.SEVERE, finalMessage);

        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = new AlertDialog.Builder(requireActivity())
                        .setTitle(finalTitle)
                        .setMessage(finalMessage)
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            if (app != null) {
                                app.stop(true);
                            }
                            requireActivity().finish();
                        })
                        .setNegativeButton("Kill", (d, w) -> {
                            android.os.Process.killProcess(android.os.Process.myPid());
                        })
                        .create();
                dialog.show();
            }
        });
    }

    protected void initializeLogHandler() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            if (rootLogger.getLevel() != null
                    && rootLogger.getLevel().intValue() <= Level.FINE.intValue()) {
                Log.v("AndroidHarness", "Removing Handler class: " + handler.getClass().getName());
            }
            rootLogger.removeHandler(handler);
        }

        Handler handler = new AndroidLogHandler();
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
    }

    @Override
    public void initialize() {
        app.initialize();
    }

    @Override
    public void reshape(int width, int height) {
        app.reshape(width, height);
    }

    @Override
    public void reshape(int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
        app.reshape(logicalWidth, logicalHeight, framebufferWidth, framebufferHeight);
    }

    @Override
    public void rescale(float x, float y) {
        app.rescale(x, y);
    }

    @Override
    public void update() {
        app.update();
    }

    @Override
    public void requestClose(boolean esc) {
        app.requestClose(esc);
    }

    @Override
    public void destroy() {
        if (app != null) {
            app.destroy();
        }
        if (finishOnAppStop) {
            requireActivity().finish();
        }
    }

    @Override
    public void gainFocus() {
        logger.fine("gainFocus");
        if (view != null) {
            view.onResume();
        }

        if (app != null) {
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.resumeAll();
            }

            JoyInput joyInput = app.getContext() != null ? app.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidJoyInput) {
                ((AndroidJoyInput) joyInput).resumeJoysticks();
            }

            app.gainFocus();
        }
    }

    @Override
    public void loseFocus() {
        logger.fine("loseFocus");
        if (app != null) {
            app.loseFocus();
        }

        if (view != null) {
            view.onPause();
        }

        if (app != null) {
            AudioRenderer audioRenderer = app.getAudioRenderer();
            if (audioRenderer != null) {
                audioRenderer.pauseAll();
            }

            JoyInput joyInput = app.getContext() != null ? app.getContext().getJoyInput() : null;
            if (joyInput instanceof AndroidJoyInput) {
                ((AndroidJoyInput) joyInput).pauseJoysticks();
            }
        }
    }
}
