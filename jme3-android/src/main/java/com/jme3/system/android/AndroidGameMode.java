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

import android.app.GameManager;
import android.app.GameState;
import android.content.Context;
import android.os.Build;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bridge to the Android Game Mode API of Android 12 (API 31) and newer.
 *
 * <p>{@link GameManager} is used directly, with the API level guarded per member:
 * looking the service up and calling {@code getGameMode()} require API 31
 * ({@link Build.VERSION_CODES#S}), {@link GameManager#setGameState(GameState)}
 * requires API 33 ({@link Build.VERSION_CODES#TIRAMISU}) and the custom game mode
 * requires API 34 ({@link Build.VERSION_CODES#UPSIDE_DOWN_CAKE}). On older devices
 * {@link #isSupported()} returns false, {@link #getGameMode()} returns
 * {@link GameMode#UNSUPPORTED} and there is nothing to unregister, so applications
 * keep working unchanged.</p>
 *
 * <p>The platform has no game mode change callback: its documentation asks
 * applications to read {@code GameManager.getGameMode()} every time they are
 * resumed. On this API that means storing an {@link OnGameModeChanged} listener and
 * calling {@link #refresh()} when the application is resumed, which is what the jME
 * harnesses do. The listener receives the mode currently reported by the system, or
 * {@link GameMode#UNSUPPORTED} when there is none.</p>
 *
 * <p>Instances are normally created and managed by the Android harnesses, for example
 * {@code com.jme3.view.surfaceview.JmeSurfaceView} and
 * {@code com.jme3.app.AndroidHarnessFragment}. Applications that need the raw API can
 * create their own instance from any {@link Context}.</p>
 *
 * @see GameMode
 * @see OnGameModeChanged
 */
public class AndroidGameMode {

    private static final Logger logger = Logger.getLogger(AndroidGameMode.class.getName());

    private final GameManager gameManager;
    private OnGameModeChanged listener;

    /**
     * Creates a bridge to the Game Mode API of the given context.
     *
     * <p>The game service is only looked up on Android 12 and newer; on any other
     * device, and when the platform does not publish a {@link GameManager}, the bridge
     * simply reports {@link GameMode#UNSUPPORTED}.</p>
     *
     * @param context the Android context used to look up the game service
     */
    public AndroidGameMode(Context context) {
        GameManager manager = null;
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                manager = context.getSystemService(GameManager.class);
            } catch (Throwable throwable) {
                logger.log(Level.FINE,
                        "The Android Game Mode API is not available on this device", throwable);
            }
        }
        this.gameManager = manager;
    }

    /**
     * Tests whether the platform Game Mode API is available, which requires Android 12
     * (API 31) or newer and a device that publishes the game service.
     *
     * @return true if the game mode can be read and reported, false otherwise
     */
    public boolean isSupported() {
        return gameManager != null;
    }

    /**
     * Reads the game mode currently selected for this application.
     *
     * @return the current game mode, or {@link GameMode#UNSUPPORTED} if the API is
     *     unavailable or the platform does not report a game mode
     */
    public GameMode getGameMode() {
        if (gameManager == null) {
            return GameMode.UNSUPPORTED;
        }
        try {
            int gameMode = gameManager.getGameMode();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    && gameMode == GameManager.GAME_MODE_CUSTOM) {
                return GameMode.CUSTOM;
            }
            return GameMode.fromValue(gameMode);
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Unable to read the Android game mode", throwable);
            return GameMode.UNSUPPORTED;
        }
    }

    /**
     * Reports how much of the current game is actually content versus an interruption,
     * which lets the platform withhold game mode interventions while gameplay must not
     * be disturbed. See the Android documentation of
     * {@link GameManager#setGameState(GameState)} for the available states.
     *
     * <p>This is a no-op that returns false before Android 13 (API 33) and on devices
     * without the Game Mode API.</p>
     *
     * @param gameState the state built with a {@code GameState.Builder}
     * @return true if the state was reported to the platform, false otherwise
     */
    public boolean setGameState(GameState gameState) {
        if (gameManager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        try {
            gameManager.setGameState(gameState);
            return true;
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Unable to report the Android game state", throwable);
            return false;
        }
    }

    /**
     * Sets the listener notified when this bridge refreshes the current game mode,
     * replacing any previously registered listener.
     *
     * <p>The current game mode is reported to a new listener immediately, including
     * once with {@link GameMode#UNSUPPORTED} when the Game Mode API is unavailable.
     * Pass null to unregister the previous listener.</p>
     *
     * @param listener the listener to notify, or null to unregister
     * @see OnGameModeChanged
     * @see #refresh()
     */
    public void setListener(OnGameModeChanged listener) {
        this.listener = listener;
        if (listener != null) {
            refresh();
        }
    }

    /**
     * Reads the current game mode and pushes it to the registered listener, if any.
     *
     * <p>Because the platform does not notify applications of game mode changes, this
     * is meant to be called whenever the application comes back to the foreground, for
     * example from {@code JmeSurfaceView} on {@code ON_RESUME} and from
     * {@code AndroidHarnessFragment#onResume()}.</p>
     *
     * @see #getGameMode()
     */
    public void refresh() {
        OnGameModeChanged target = listener;
        if (target != null) {
            target.onGameModeChanged(getGameMode());
        }
    }
}
