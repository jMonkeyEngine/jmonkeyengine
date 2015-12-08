/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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

import com.jme3.IntegrationTest;
import com.jme3.renderer.RenderManager;
import com.jme3.system.JmeContext;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class LwjglAppIT {

    private final AtomicInteger simpleInitAppInvocations = new AtomicInteger();
    private final AtomicInteger simpleUpdateInvocations = new AtomicInteger();
    private final AtomicInteger simpleRenderInvocations = new AtomicInteger();

    private class TestApp extends SimpleApplication {

        @Override
        public void simpleInitApp() {
            simpleInitAppInvocations.incrementAndGet();
        }

        @Override
        public void simpleUpdate(float tpf) {
            simpleUpdateInvocations.incrementAndGet();
        }

        @Override
        public void simpleRender(RenderManager rm) {
            simpleRenderInvocations.incrementAndGet();
        }
    }

    private void doStopStart(Application app, JmeContext.Type type) throws InterruptedException {
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);

        // start the application - simple init / update will be called once.
        app.start(type, true);
        assert app.isStarted();

        // stop the application, wait a bit, then start it again.
        app.stop(true);
        assert !app.isStarted();

        Thread.sleep(100);

        app.start(type, true);
        assert app.isStarted();
        app.stop(true);
        assert !app.isStarted();

        // make sure each method was called twice.
        assertEquals(2, simpleInitAppInvocations.get());
        assertEquals(2, simpleUpdateInvocations.get());
        assertEquals(2, simpleRenderInvocations.get());
    }

    @Before
    public void setUp() {
        Logger.getLogger("com.jme3").setLevel(Level.OFF);
    }

    @Test
    public void testDisplayAppLifeCycle() throws InterruptedException {
        assumeFalse(GraphicsEnvironment.isHeadless());
        doStopStart(new TestApp(), JmeContext.Type.Display);
    }

    @Test
    public void testOffscreenAppLifeCycle() throws InterruptedException {
        assumeFalse(GraphicsEnvironment.isHeadless());
        doStopStart(new TestApp(), JmeContext.Type.OffscreenSurface);
    }

    @Test
    public void testExceptionInvokesHandleError() throws InterruptedException {
        doStopStart(new TestApp(), JmeContext.Type.Headless);
    }
}
