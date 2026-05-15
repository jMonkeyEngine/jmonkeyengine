/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.math.ColorRGBA;

import java.util.function.Consumer;

/**
 * The app used for the tests. AppState(s) are used to inject the actual test code.
 * @author Richard Tingle (aka richtea)
 */
public class App extends SimpleApplication {

    public App(AppState... initialStates){
        super(initialStates);
    }

    Consumer<Throwable> onError = (onError) -> {};

    @Override
    public void simpleInitApp(){
        getViewPort().setBackgroundColor(ColorRGBA.Black);
        setTimer(new IsoTimer(60));
    }

    @Override
    public void handleError(String errMsg, Throwable t) {
        super.handleError(errMsg, t);
        onError.accept(t);
    }

    public static final class IsoTimer extends com.jme3.system.Timer {

        private final float framerate;
        private int ticks;
        private long lastTime = 0;

        public IsoTimer(float framerate) {
            this.framerate = framerate;
            this.ticks = 0;
        }

        @Override
        public long getTime() {
            return (long) (this.ticks * (1.0f / this.framerate) * 1000f);
        }

        @Override
        public long getResolution() {
            return 1000L;
        }

        @Override
        public float getFrameRate() {
            return this.framerate;
        }

        @Override
        public float getTimePerFrame() {
            return 1.0f / this.framerate;
        }

        @Override
        public void update() {
            long time = System.currentTimeMillis();
            long difference = time - lastTime;
            lastTime = time;
            if (difference < (1.0f / this.framerate) * 1000.0f) {
                try {
                    Thread.sleep(difference);
                } catch (InterruptedException ex) {
                }
            }
            this.ticks++;
        }

        @Override
        public void reset() {
            this.ticks = 0;
        }
    }
}