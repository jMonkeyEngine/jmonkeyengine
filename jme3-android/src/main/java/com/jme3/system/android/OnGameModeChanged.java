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
package com.jme3.system.android;

/**
 * Listener notified when the Android platform game mode changes.
 *
 * <p>The listener is registered with
 * {@link AndroidGameMode#setListener(OnGameModeChanged)} and is usually exposed by the
 * Android harnesses, for example
 * {@code com.jme3.view.surfaceview.JmeSurfaceView#setOnGameModeChanged(OnGameModeChanged)}
 * and
 * {@code com.jme3.app.AndroidHarnessFragment#setOnGameModeChanged(OnGameModeChanged)}.</p>
 *
 * <p>It maps the per-mode callbacks of the Game Mode API to a single game mode
 * value: {@link GameMode#PERFORMANCE} corresponds to the performance callback,
 * {@link GameMode#BATTERY} to the battery saver callback,
 * {@link GameMode#STANDARD} to the standard callback and
 * {@link GameMode#UNSUPPORTED} to the disabled callback.</p>
 *
 * <p>Callbacks are delivered on the Android main thread. The current mode is reported
 * to the listener as soon as it is registered, including once with
 * {@link GameMode#UNSUPPORTED} when the Game Mode API is unavailable.</p>
 *
 * @see GameMode
 * @see AndroidGameMode
 */
public interface OnGameModeChanged {

    /**
     * Invoked when the platform game mode changes, and once with the current mode when
     * the listener is registered.
     *
     * @param gameMode the current game mode, never null
     */
    void onGameModeChanged(GameMode gameMode);
}
