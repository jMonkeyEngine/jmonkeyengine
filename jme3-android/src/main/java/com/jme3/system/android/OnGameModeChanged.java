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
 * jME listener that receives the game mode the Android platform currently reports.
 *
 * <p>Android does not offer a game mode change callback: its documentation asks games
 * to read {@code GameManager.getGameMode()} every time they are resumed. This listener
 * is therefore notified when jME refreshes that value, not by a platform callback for
 * every settings change. It is usually registered through the Android harnesses, for
 * example
 * {@code com.jme3.view.surfaceview.JmeSurfaceView#setOnGameModeChanged(OnGameModeChanged)}
 * and
 * {@code com.jme3.app.AndroidHarnessFragment#setOnGameModeChanged(OnGameModeChanged)},
 * which refresh it when they are resumed.</p>
 *
 * <p>The listener receives a single game mode value: {@link GameMode#PERFORMANCE},
 * {@link GameMode#BATTERY}, {@link GameMode#STANDARD} or {@link GameMode#CUSTOM} when
 * the platform reports one, and {@link GameMode#UNSUPPORTED} when the Game Mode API is
 * unavailable or the platform has no mode for the application.</p>
 *
 * <p>The listener is notified on the Android main thread, once with the current mode
 * when it is registered and again whenever jME refreshes that mode.</p>
 *
 * @see GameMode
 * @see AndroidGameMode
 */
public interface OnGameModeChanged {

    /**
     * Invoked with the game mode the platform currently reports, when the listener is
     * registered and whenever jME refreshes that mode.
     *
     * @param gameMode the current game mode, never null
     */
    void onGameModeChanged(GameMode gameMode);
}
