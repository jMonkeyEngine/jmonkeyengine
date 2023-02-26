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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Defines default native libraries used by jMonkeyEngine.
 *
 * @author Ali-RS
 */
public enum NativeLibraries {

    // Note: LWJGL 3 handles its native library extracting & loading using
    // its own SharedLibraryLoader.

    // LWJGL 2
    Lwjgl(new LibraryInfo("lwjgl", libPath ->
            // lwjgl handle loading by itself.
            System.setProperty("org.lwjgl.librarypath",
                    Paths.get(libPath).getParent().toAbsolutePath().toString()))
            .addNativeVariant(Platform.Windows32, "lwjgl.dll")
            .addNativeVariant(Platform.Windows64, "lwjgl64.dll")
            .addNativeVariant(Platform.Linux32, "liblwjgl.so")
            .addNativeVariant(Platform.Linux64, "liblwjgl64.so")
            .addNativeVariant(Platform.MacOSX32, "liblwjgl.dylib")
            .addNativeVariant(Platform.MacOSX64, "liblwjgl.dylib")
    ),

    // OpenAL for LWJGL 2
    // For OSX: Need to add lib prefix when extracting
    Openal(new LibraryInfo("openal")
            .addNativeVariant(Platform.Windows32, "OpenAL32.dll")
            .addNativeVariant(Platform.Windows64, "OpenAL64.dll")
            .addNativeVariant(Platform.Linux32,   "libopenal.so")
            .addNativeVariant(Platform.Linux64,   "libopenal64.so")
            .addNativeVariant(Platform.MacOSX32,  "openal.dylib", "libopenal.dylib")
            .addNativeVariant(Platform.MacOSX64,  "openal.dylib", "libopenal.dylib")
    ),

    // BulletJme
    BulletJme(new LibraryInfo("bulletjme")
            .addNativeVariant(Platform.Windows32, "native/windows/x86/bulletjme.dll", "bulletjme-x86.dll")
            .addNativeVariant(Platform.Windows64, "native/windows/x86_64/bulletjme.dll", "bulletjme-x86_64.dll")
            .addNativeVariant(Platform.Windows_ARM64, "native/windows/arm64/bulletjme.dll", "bulletjme-arm64.dll")
            .addNativeVariant(Platform.Linux32, "native/linux/x86/libbulletjme.so", "libbulletjme-x86.so")
            .addNativeVariant(Platform.Linux64, "native/linux/x86_64/libbulletjme.so", "libbulletjme-x86_64.so")
            .addNativeVariant(Platform.Linux_ARM32, "native/linux/arm32/libbulletjme.so", "libbulletjme-arm32.so")
            .addNativeVariant(Platform.Linux_ARM64, "native/linux/arm64/libbulletjme.so", "libbulletjme-arm64.so")
            .addNativeVariant(Platform.MacOSX32, "native/osx/x86/libbulletjme.dylib", "libbulletjme-x86.dylib")
            .addNativeVariant(Platform.MacOSX64, "native/osx/x86_64/libbulletjme.dylib", "libbulletjme-x86_64.dylib")
            .addNativeVariant(Platform.MacOSX_ARM64, "native/osx/arm64/libbulletjme.dylib", "libbulletjme-arm64.dylib")
    ),

    // JInput
    // For OSX: Need to rename extension jnilib -> dylib when extracting
    JInput(new LibraryInfo("jinput", libPath ->
            // jinput handle loading by itself.
            System.setProperty("net.java.games.input.librarypath",
                    Paths.get(libPath).getParent().toAbsolutePath().toString()))
            .addNativeVariant(Platform.Windows32, "jinput-raw.dll")
            .addNativeVariant(Platform.Windows64, "jinput-raw_64.dll")
            .addNativeVariant(Platform.Linux32, "libjinput-linux.so")
            .addNativeVariant(Platform.Linux64, "libjinput-linux64.so")
            .addNativeVariant(Platform.MacOSX32, "libjinput-osx.jnilib", "libjinput-osx.dylib")
            .addNativeVariant(Platform.MacOSX64, "libjinput-osx.jnilib", "libjinput-osx.dylib")
    ),

    // JInput Auxiliary (only required on Windows)
    JInputDX8(new LibraryInfo("jinput-dx8")
            .addNativeVariant(Platform.Windows32, "jinput-dx8.dll", null)
            .addNativeVariant(Platform.Windows64, "jinput-dx8_64.dll", null)
            .addNativeVariant(Platform.Linux32, null)
            .addNativeVariant(Platform.Linux64, null)
            .addNativeVariant(Platform.MacOSX32, null)
            .addNativeVariant(Platform.MacOSX64, null)
    );

    private final LibraryInfo library;


    NativeLibraries(LibraryInfo library) {
        this.library = library;
    }

    public static void registerDefaultLibraries() {
        Lwjgl.registerLibrary();
        Openal.registerLibrary();
        BulletJme.registerLibrary();
        JInput.registerLibrary();
        JInputDX8.registerLibrary();
    }

    public LibraryInfo getLibrary() {
        return library;
    }

    public String getName() {
        return library.getName();
    }

    private void registerLibrary() {
        library.getNativeVariants().forEach(NativeLibraryLoader::registerNativeLibrary);
    }

    public static class LibraryInfo {

        private final String name;
        private final List<NativeLibrary> nativeVariants = new ArrayList<>();
        private final Consumer<String> loadFunction;

        public LibraryInfo(String name) {
            this(name, System::load);
        }

        public LibraryInfo(String name, Consumer<String> loadFunction) {
            this.name = name;
            this.loadFunction = loadFunction;
        }

        public String getName() {
            return name;
        }

        public List<NativeLibrary> getNativeVariants() {
            return nativeVariants;
        }

        public LibraryInfo addNativeVariant(Platform platform, String pathInNativesJar) {
            return addNativeVariant(platform, pathInNativesJar, null);
        }

        public LibraryInfo addNativeVariant(Platform platform, String pathInNativesJar, String extractedAsFileName) {
             nativeVariants.add(new NativeLibrary(name, platform, pathInNativesJar, extractedAsFileName, loadFunction));
             return this;
        }
    }
}
