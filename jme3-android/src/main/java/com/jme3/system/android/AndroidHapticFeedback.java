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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.android;

import com.jme3.math.FastMath;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Android haptic helpers shared by device and input-device vibrators.
 */
public final class AndroidHapticFeedback {


    private AndroidHapticFeedback() {
    }

    public static boolean isSupported(Vibrator vibrator) {
        return vibrator != null && vibrator.hasVibrator();
    }

    @SuppressWarnings("deprecation")
    public static void rumble(Vibrator vibrator, float amountHigh, float amountLow, float duration) {
        if (!isSupported(vibrator)) {
            return;
        }
        float amount = Math.max(FastMath.clamp(amountHigh, 0f, 1f), FastMath.clamp(amountLow, 0f, 1f));
        if (amount <= 0f || duration <= 0f) {
            stop(vibrator);
            return;
        }

        try {
            if (duration == Float.POSITIVE_INFINITY) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
                    int amplitude = Math.max(1, Math.round(amount * 255f));
                    vibrator.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 1000},
                            new int[]{0, amplitude},
                            0));
                } else {
                    long rumbleOnDur = Math.max(1L, Math.round(amount * 1000));
                    long rumbleOffDur = Math.max(0L, 1000 - rumbleOnDur);
                    vibrator.vibrate(new long[]{0, rumbleOnDur, rumbleOffDur}, 0);
                }
            } else {
                long durationMs = Math.max(1L, Math.round(duration * 1000f));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
                    int amplitude = Math.max(1, Math.round(amount * 255f));
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(durationMs);
                }
            }
        } catch (SecurityException ignored) {
            // Applications without VIBRATE permission should degrade to no-op.
        }
    }

    public static void stop(Vibrator vibrator) {
        if (!isSupported(vibrator)) {
            return;
        }
        try {
            vibrator.cancel();
        } catch (SecurityException ignored) {
            // Applications without VIBRATE permission should degrade to no-op.
        }
    } 
}
