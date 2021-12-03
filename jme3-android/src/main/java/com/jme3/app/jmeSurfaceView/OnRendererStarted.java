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

import android.view.View;
import com.jme3.app.LegacyApplication;

/**
 * An interface used for invoking an event when the application is started explicitly from {@link JmeSurfaceView#startRenderer(int)}.
 * NB : This listener must be utilized before using {@link JmeSurfaceView#startRenderer(int)}, ie : it would be ignored if you try to use {@link JmeSurfaceView#setOnRendererStarted(OnRendererStarted)} after
 * {@link JmeSurfaceView#startRenderer(int)}.
 * @see JmeSurfaceView#setOnRendererStarted(OnRendererStarted)
 *
 * @author pavl_g.
 */
public interface OnRendererStarted {
    /**
     * Invoked when the game application is started by the {@link LegacyApplication#start()}, the event is dispatched on the
     * holder Activity context thread.
     * @see JmeSurfaceView#startRenderer(int)
     * @param application the game instance.
     * @param layout the enclosing layout.
     */
    void onRenderStart(LegacyApplication application, View layout);
}
