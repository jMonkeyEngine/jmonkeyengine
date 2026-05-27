/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.system;

/**
 * Shared calculations for the split logical/physical display scale model.
 */
public final class DisplayScaleUtils {

    private DisplayScaleUtils() {
    }

    /**
     * Resolves the logical application size for a window-backed context.
     *
     * @param mode the active display scale mode
     * @param windowWidth the platform window coordinate width
     * @param windowHeight the platform window coordinate height
     * @param framebufferWidth the physical framebuffer width
     * @param framebufferHeight the physical framebuffer height
     * @param displayScaleX the platform content/display scale on X
     * @param displayScaleY the platform content/display scale on Y
     * @return an array containing logical width and logical height
     */
    public static int[] resolveLogicalSize(float mode, int windowWidth, int windowHeight,
            int framebufferWidth, int framebufferHeight, float displayScaleX, float displayScaleY) {
        if (!isDpiAwareMode(mode)) {
            return new int[] { Math.max(framebufferWidth, 1), Math.max(framebufferHeight, 1) };
        }

        int safeWindowWidth = Math.max(windowWidth, 1);
        int safeWindowHeight = Math.max(windowHeight, 1);
        int safeFramebufferWidth = Math.max(framebufferWidth, 1);
        int safeFramebufferHeight = Math.max(framebufferHeight, 1);
        float scaleX = sanitizeScale(displayScaleX);
        float scaleY = sanitizeScale(displayScaleY);

        int scaledWidth = Math.max(Math.round(safeFramebufferWidth / scaleX), 1);
        int scaledHeight = Math.max(Math.round(safeFramebufferHeight / scaleY), 1);

        if (approximatelyEqual(scaledWidth, safeWindowWidth) && approximatelyEqual(scaledHeight, safeWindowHeight)) {
            return new int[] { safeWindowWidth, safeWindowHeight };
        }

        return new int[] { scaledWidth, scaledHeight };
    }

    public static float sanitizeScale(float value) {
        return Float.isFinite(value) && value > 0f ? value : 1f;
    }

    public static float normalizeDisplayScaleMode(float mode) {
        if (!Float.isFinite(mode)) {
            return AppSettings.DISPLAY_SCALE_DISABLED;
        }
        if (mode == AppSettings.DISPLAY_SCALE_NATIVE_PIXELS) {
            return AppSettings.DISPLAY_SCALE_NATIVE_PIXELS;
        }
        if (mode < AppSettings.DISPLAY_SCALE_DPI_AWARE) {
            return AppSettings.DISPLAY_SCALE_DISABLED;
        }
        return mode;
    }

    public static boolean isNativePixelsMode(float mode) {
        return mode == AppSettings.DISPLAY_SCALE_NATIVE_PIXELS;
    }

    public static boolean isDpiAwareMode(float mode) {
        return mode >= AppSettings.DISPLAY_SCALE_DPI_AWARE;
    }

    public static boolean isEmulatedScaleMode(float mode) {
        return mode > AppSettings.DISPLAY_SCALE_DPI_AWARE;
    }

    public static boolean isDisabledMode(float mode) {
        return !isNativePixelsMode(mode) && !isDpiAwareMode(mode);
    }

    public static boolean requestsHighDensityFramebuffer(float mode) {
        return isNativePixelsMode(mode) || isDpiAwareMode(mode);
    }

    /**
     * Converts a native window-coordinate X value to jME input coordinates.
     *
     * @param nativeX the platform coordinate
     * @param targetWidth the target jME coordinate width
     * @param nativeWidth the platform coordinate width
     * @return the jME X coordinate
     */
    public static int toInputX(float nativeX, int targetWidth, int nativeWidth) {
        return Math.round(nativeX * Math.max(targetWidth, 1) / Math.max(nativeWidth, 1));
    }

    /**
     * Converts a native top-origin Y value to jME bottom-origin input
     * coordinates.
     *
     * @param nativeY the platform coordinate
     * @param targetHeight the target jME coordinate height
     * @param nativeHeight the platform coordinate height
     * @return the jME Y coordinate
     */
    public static int toInputY(float nativeY, int targetHeight, int nativeHeight) {
        int safeTargetHeight = Math.max(targetHeight, 1);
        return Math.round(safeTargetHeight - (nativeY * safeTargetHeight / Math.max(nativeHeight, 1)));
    }

    private static boolean approximatelyEqual(int a, int b) {
        return Math.abs(a - b) <= 1;
    }
}
