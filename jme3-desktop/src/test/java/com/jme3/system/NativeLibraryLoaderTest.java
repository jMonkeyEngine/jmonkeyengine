/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeLibraryLoaderTest {

    @TempDir
    private Path tempDir;

    private String previousExtractionFolder;
    private String previousExtractNativeLibraries;

    @BeforeEach
    void rememberNativeLibrarySettings() {
        previousExtractionFolder = System.getProperty(NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY);
        previousExtractNativeLibraries = System.getProperty(NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY);
        NativeLibraryLoader.setCustomExtractionFolder(null);
        NativeLibraryLoader.clearExtractNativeLibrariesOverride();
    }

    @AfterEach
    void restoreNativeLibrarySettings() {
        NativeLibraryLoader.setCustomExtractionFolder(null);
        NativeLibraryLoader.clearExtractNativeLibrariesOverride();
        if (previousExtractionFolder == null) {
            System.clearProperty(NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY);
        } else {
            System.setProperty(NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY, previousExtractionFolder);
        }
        if (previousExtractNativeLibraries == null) {
            System.clearProperty(NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY);
        } else {
            System.setProperty(NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY, previousExtractNativeLibraries);
        }
    }

    @Test
    void loadNativeLibraryUsesCustomExtractionFolderWhenExtractionIsDisabled() throws Exception {
        Path nativeLibraryFile = tempDir.resolve("libcustom-native-test.so");
        Files.createFile(nativeLibraryFile);
        AtomicReference<String> loadedPath = new AtomicReference<>();
        String libraryName = "customExtractionFolderTest" + System.nanoTime();

        NativeLibraryLoader.registerNativeLibrary(new NativeLibrary(
                libraryName,
                JmeSystem.getPlatform(),
                "native/missing/libcustom-native-test.so",
                "libcustom-native-test.so",
                loadedPath::set));
        NativeLibraryLoader.setCustomExtractionFolder(tempDir.toString());
        NativeLibraryLoader.setExtractNativeLibraries(false);

        String result = NativeLibraryLoader.loadNativeLibrary(libraryName, true);

        assertEquals(nativeLibraryFile.toAbsolutePath().toString(), result);
        assertEquals(result, loadedPath.get());
    }

    @Test
    void loadNativeLibraryUsesExtractionFolderSystemProperties() throws Exception {
        Path nativeLibraryFile = tempDir.resolve("libproperty-native-test.so");
        Files.createFile(nativeLibraryFile);
        AtomicReference<String> loadedPath = new AtomicReference<>();
        String libraryName = "propertyExtractionFolderTest" + System.nanoTime();

        NativeLibraryLoader.registerNativeLibrary(new NativeLibrary(
                libraryName,
                JmeSystem.getPlatform(),
                "native/missing/libproperty-native-test.so",
                "libproperty-native-test.so",
                loadedPath::set));
        System.setProperty(NativeLibraryLoader.CUSTOM_EXTRACTION_FOLDER_PROPERTY, tempDir.toString());
        System.setProperty(NativeLibraryLoader.EXTRACT_NATIVE_LIBRARIES_PROPERTY, "false");

        String result = NativeLibraryLoader.loadNativeLibrary(libraryName, true);

        assertEquals(nativeLibraryFile.toAbsolutePath().toString(), result);
        assertEquals(result, loadedPath.get());
    }
}
