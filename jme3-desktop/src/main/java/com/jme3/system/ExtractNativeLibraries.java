/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows extraction of platform specific binaries from classpath via build
 * systems.
 *
 * @author normenhansen
 */
public class ExtractNativeLibraries {

    public static void main(String[] args) {
        if (args.length == 1) {
            if ("getjarexcludes".equals(args[0])) {
                File[] jarFiles = NativeLibraryLoader.getJarsWithNatives();
                for (int i = 0; i < jarFiles.length; i++) {
                    File jarFile = jarFiles[i];
                    System.out.print("**/*" + jarFile.getName());
                    if (i != jarFiles.length - 1) {
                        System.out.print(",");
                    }
                }
                System.exit(0);
            }
        }
        if (args.length < 2) {
            System.err.println("Usage: ExtractNativeLibraries Platform ExtractionPath");
            System.err.println("Where 'Platform' is one of: " + Arrays.toString(Platform.values()));
            System.err.println("'ExtractionPath' is a folder to extract the binaries to.");
            System.err.println("You can also use ExtractNativeLibraries getjarexcludes to get a list of excludes for the jar files that contain binaries.");
            System.exit(1);
        }
        String path = args[1].replace('/', File.separatorChar);
        File folder = new File(path);
        try {
            Platform platform = Platform.valueOf(args[0]);
            NativeLibraryLoader.extractNativeLibraries(platform, folder);
        } catch (IllegalArgumentException ex) {
            System.err.println("Unknown platform: " + args[0]);
            System.err.println("Supported platforms: " + Arrays.toString(Platform.values()));
            System.exit(3);
        } catch (IOException ex) {
            Logger.getLogger(ExtractNativeLibraries.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
