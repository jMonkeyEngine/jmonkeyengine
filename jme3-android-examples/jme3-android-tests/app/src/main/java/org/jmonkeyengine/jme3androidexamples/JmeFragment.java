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
package org.jmonkeyengine.jme3androidexamples;

import android.os.Bundle;
import com.jme3.app.AndroidHarnessFragment;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * A placeholder fragment containing a jME GLSurfaceView.
 */
public class JmeFragment extends AndroidHarnessFragment {

    public JmeFragment() {
        // Set the desired EGL configuration
        eglBitsPerPixel = 24;
        eglAlphaBits = 0;
        eglDepthBits = 16;
        eglSamples = 0;
        eglStencilBits = 0;

        // Set the maximum framerate
        // (default = -1 for unlimited)
        frameRate = -1;

        // Set the maximum resolution dimension
        // (the smaller side, height or width, is set automatically
        // to maintain the original device screen aspect ratio)
        // (default = -1 to match device screen resolution)
        maxResolutionDimension = -1;

        /*
        Skip these settings and use the settings stored in the Bundle retrieved during onCreate.

        // Set main project class (fully qualified path)
        appClass = "";

        // Set input configuration settings
        joystickEventsEnabled = false;
        keyEventsEnabled = true;
        mouseEventsEnabled = true;
        */

        // Set application exit settings
        finishOnAppStop = true;
        handleExitHook = true;
        exitDialogTitle = "Do you want to exit?";
        exitDialogMessage = "Use your home key to bring this app into the background or exit to terminate it.";

        // Set splash screen resource id, if used
        // (default = 0, no splash screen)
        // For example, if the image file name is "splash"...
        //     splashPicID = R.drawable.splash;
        splashPicID = 0;
//        splashPicID = R.drawable.android_splash;

        // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
        LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle bundle=getArguments();

        appClass = bundle.getString(MainActivity.SELECTED_APP_CLASS);
//        Log.d(this.getClass().getSimpleName(), "AppClass: " + appClass);
        joystickEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_JOYSTICK_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "JoystickEventsEnabled: " + joystickEventsEnabled);
        keyEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_KEY_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "KeyEventsEnabled: " + keyEventsEnabled);
        mouseEventsEnabled = bundle.getBoolean(MainActivity.ENABLE_MOUSE_EVENTS);
//        Log.d(this.getClass().getSimpleName(), "MouseEventsEnabled: " + mouseEventsEnabled);
        boolean verboseLogging = bundle.getBoolean(MainActivity.VERBOSE_LOGGING);
//        Log.d(this.getClass().getSimpleName(), "VerboseLogging: " + verboseLogging);
        if (verboseLogging) {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.ALL);
        } else {
            // Set the default logging level (default=Level.INFO, Level.ALL=All Debug Info)
            LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
        }

        super.onCreate(savedInstanceState);
    }
}
