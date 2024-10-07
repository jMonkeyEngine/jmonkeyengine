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

package jme3test.app;

import com.jme3.app.LegacyApplication;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests the functionality of LegacyApplication.restart().
 * <p>
 * If successful, the test will wait for 3 seconds, change to fullscreen, wait 3
 * more seconds, change to a 500x400 window, wait 3 more seconds, and terminate.
 * <p>
 * If successful, the reshape() method will be logged twice: once for the
 * transition to fullscreen mode and again for the transition to windowed mode.
 */
public class TestContextRestart {

    final private static Logger logger
            = Logger.getLogger(TestContextRestart.class.getName());

    public static void main(String[] args) throws InterruptedException{
        logger.setLevel(Level.INFO);
        AppSettings settings = new AppSettings(true);

        final LegacyApplication app = new LegacyApplication() {
            @Override
            public void reshape(int width, int height) {
                super.reshape(width, height);
                logger.log(Level.INFO, "reshape(width={0} height={1})",
                        new Object[]{width, height});
            }
        };
        app.setSettings(settings);
        app.start();

        Thread.sleep(3000);
        /*
         * Restart with a fullscreen graphics context.
         */
        settings.setFullscreen(true);
        settings.setResolution(-1, -1);
        app.setSettings(settings);
        app.restart();

        Thread.sleep(3000);
        /*
         * Restart with a 500x400 windowed context.
         */
        settings.setFullscreen(false);
        settings.setResolution(500, 400);
        app.setSettings(settings);
        app.restart();

        Thread.sleep(3000);
        app.stop();
    }

}
