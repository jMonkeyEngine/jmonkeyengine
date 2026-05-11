package org.jmonkeyengine.gradle.nativemetadata.internal;

import org.graalvm.nativeimage.ProcessProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

final class JmeNativeRuntimeDefaults {

    private static final String JME_EXTRACTION_FOLDER = "com.jme3.NativeLibraryExtractionFolder";
    private static final String JME_EXTRACT_LIBRARIES = "com.jme3.ExtractNativeLibraries";
    private static final String LWJGL_LIBRARY_PATH = "org.lwjgl.librarypath";
    private static final String SAFERALLOC_OVERRIDE = "saferalloc.native.override";

    private JmeNativeRuntimeDefaults() {
    }

    static void apply() {
        setIfMissing(JME_EXTRACT_LIBRARIES, "false");

        String nativeLibDir = computeNativeLibDirectory();
        if (nativeLibDir == null || nativeLibDir.isEmpty()) {
            return;
        }

        setIfMissing(JME_EXTRACTION_FOLDER, nativeLibDir);
        setIfMissing(LWJGL_LIBRARY_PATH, nativeLibDir);
        setIfMissing(SAFERALLOC_OVERRIDE, nativeLibDir);
    }

    private static String computeNativeLibDirectory() {
        try {
            String executablePath = ProcessProperties.getExecutableName();
            if (executablePath == null || executablePath.isEmpty()) {
                return null;
            }
            Path executable = Paths.get(executablePath);
            Path executableDir = executable.getParent();
            if (executableDir == null) {
                return null;
            }
            return executableDir.resolve("lib").toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void setIfMissing(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
