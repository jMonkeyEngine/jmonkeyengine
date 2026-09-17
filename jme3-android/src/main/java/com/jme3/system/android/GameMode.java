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
 * The Android game modes reported by the platform Game Mode API of Android 12 (API 31)
 * and newer.
 *
 * <p>The constants mirror the values of {@code android.app.GameManager} so that
 * applications can react to the mode selected by the user in the system game
 * settings without depending on API 31 at compile time or on older devices.</p>
 *
 * @see AndroidGameMode
 * @see OnGameModeChanged
 */
public enum GameMode {

    /**
     * No game mode is reported by the system. This is the mode returned on devices
     * running Android 11 or older and for applications the platform does not treat
     * as games.
     */
    UNSUPPORTED(0),

    /**
     * The system reports the standard mode, a balanced trade-off between performance
     * and battery life.
     */
    STANDARD(1),

    /**
     * The system asks for maximum performance, for example when the user enabled
     * performance mode for the game.
     */
    PERFORMANCE(2),

    /**
     * The system asks for battery saving, for example when the user enabled battery
     * saver mode for the game.
     */
    BATTERY(3);

    private final int value;

    GameMode(int value) {
        this.value = value;
    }

    /**
     * Returns the raw value used by the Android platform for this game mode.
     *
     * @return the {@code android.app.GameManager} constant value
     */
    public int getValue() {
        return value;
    }

    /**
     * Tests whether the platform actually reports a game mode.
     *
     * @return false if this is {@link #UNSUPPORTED}, true otherwise
     */
    public boolean isSupported() {
        return this != UNSUPPORTED;
    }

    /**
     * Converts a raw Android game mode value into a {@link GameMode} constant.
     *
     * @param value the {@code android.app.GameManager} constant value
     * @return the matching game mode, or {@link #UNSUPPORTED} for unknown values
     */
    public static GameMode fromValue(int value) {
        for (GameMode gameMode : values()) {
            if (gameMode.value == value) {
                return gameMode;
            }
        }
        return UNSUPPORTED;
    }
}
