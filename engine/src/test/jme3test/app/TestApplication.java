/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package jme3test.app;

import com.jme3.app.Application;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext.Type;

/**
 * Test Application functionality, such as create, restart, destroy, etc.
 * @author Kirill
 */
public class TestApplication {

    public static void main(String[] args) throws InterruptedException{
        System.out.println("Creating application..");
        Application app = new Application();
        System.out.println("Starting application in LWJGL mode..");
        app.start();
        System.out.println("Waiting 5 seconds");
        Thread.sleep(5000);
        System.out.println("Closing application..");
        app.stop();

        Thread.sleep(2000);
        System.out.println("Starting in fullscreen mode");
        app = new Application();
        AppSettings settings = new AppSettings(true);
        settings.setFullscreen(true);
        settings.setResolution(-1,-1); // current width/height
        app.setSettings(settings);
        app.start();
        Thread.sleep(5000);
        app.stop();

        Thread.sleep(2000);
        System.out.println("Creating offscreen buffer application");
        app = new Application();
        app.start(Type.OffscreenSurface);
        Thread.sleep(3000);
        System.out.println("Destroying offscreen buffer");
        app.stop();
    }

}
