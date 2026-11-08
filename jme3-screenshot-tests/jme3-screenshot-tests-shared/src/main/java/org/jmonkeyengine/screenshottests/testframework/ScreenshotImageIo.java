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
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.StbImageLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes the PNG files the screenshot tests compare.
 *
 * <p>
 * Every image a screenshot test looks at - the screenshot it just took, and the reference image it
 * is compared against - must be decoded the same way, or the comparison measures the decoder rather
 * than the renderer. Both platforms therefore funnel through {@link #readImage(File)} here rather
 * than decoding images themselves.
 * </p>
 *
 * @author Richard Tingle (aka richtea)
 */
public final class ScreenshotImageIo {

    private ScreenshotImageIo() {
    }

    /**
     * Reads a PNG file.
     */
    public static Image readImage(File file) {
        File absoluteFile = file.getAbsoluteFile();
        AssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLocator(absoluteFile.getParent(), FileLocator.class);
        assetManager.registerLoader(StbImageLoader.class, "png", "jpg", "jpeg");
        return assetManager.loadTexture(absoluteFile.getName()).getImage();
    }

    /**
     * Reads a PNG from a stream that is not backed by a file the asset system can reach - on
     * Android the reference images arrive through the test storage content provider rather than as
     * files. The stream is spooled to a temporary file so that it is decoded by exactly the same
     * path as every other image, then the temporary file is deleted.
     *
     * @param stream the stream to read; the caller retains responsibility for closing it
     * @return the decoded image
     * @throws IOException if the stream cannot be read
     */
    public static Image readImage(InputStream stream) throws IOException {
        File temporaryFile = File.createTempFile("jme-screenshot-reference", ".png");
        try {
            Files.copy(stream, temporaryFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return readImage(temporaryFile);
        } finally {
            Files.deleteIfExists(temporaryFile.toPath());
        }
    }

    /**
     * Writes an image as a PNG, converting it to RGBA8 first because that is the only format
     * {@link JmeSystem#writeImageFile} accepts. The caller remains responsible for closing the
     * stream.
     */
    public static void writePng(Image image, OutputStream outputStream) throws IOException {
        Image rgbaImage = TestReportCaptureBase.convertToRGBA8(image);
        JmeSystem.writeImageFile(outputStream, "png", rgbaImage.getData(0), rgbaImage.getWidth(),
                rgbaImage.getHeight());
    }

    /**
     * Writes an image as a PNG to a file, creating the directories above it if they do not exist.
     */
    public static void writePng(Image image, Path file) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(file)) {
            writePng(image, outputStream);
        }
    }
}
