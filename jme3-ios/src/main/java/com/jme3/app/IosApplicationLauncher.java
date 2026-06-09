/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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

import com.jme3.system.JmeContext;
import com.jme3.system.ios.IGLESContext;

/**
 * Base iOS launcher for jME
 * Extend this class and implement {@link #createApplication()} to return the jME application to run.
 * 
 */
public abstract class IosApplicationLauncher {

    protected Application app;

    public void start() {
        try {
            startApplication(createApplication());
        } catch (Exception exception) {
            throw new IllegalStateException("jME application initialization failed", exception);
        }
    }

    protected void startApplication(Application application) throws Exception {
        app = application;
        if (app instanceof SimpleApplication) {
            ((SimpleApplication) app).setShowSettings(false);
        }
        app.start();
    }

    /**
     * Creates the jME application hosted by this launcher.
     *
     * @return the application instance
     * @throws Exception if the application cannot be created
     */
    protected abstract Application createApplication() throws Exception;

    public void update() {
        if (app == null)  return;
        JmeContext context = app.getContext();
        if (context == null) return;
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).runFrame();
        } else {
            throw new IllegalStateException("Application context is not an IGLESContext");
        }
    }

    public void resize(int width, int height) {
        if (app == null)  return;
        JmeContext context = app.getContext();
        if (context == null) return;
        if (context instanceof IGLESContext) {
            ((IGLESContext) context).resizeFramebuffer(width, height);
        } else {
            throw new IllegalStateException("Application context is not an IGLESContext");
        }
    }

    public void stop(boolean waitFor) {
        if (app != null) {
            app.stop(waitFor);
            app = null;
        }
    }
}
