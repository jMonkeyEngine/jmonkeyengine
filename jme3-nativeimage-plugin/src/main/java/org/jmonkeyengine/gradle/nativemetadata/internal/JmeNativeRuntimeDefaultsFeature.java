package org.jmonkeyengine.gradle.nativemetadata.internal;

import org.graalvm.nativeimage.ProcessProperties;
import org.graalvm.nativeimage.hosted.Feature;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configures runtime defaults for jME and LWJGL native library locations in native images.
 */
public final class JmeNativeRuntimeDefaultsFeature implements Feature {

    private static final String JME_EXTRACTION_FOLDER = "com.jme3.NativeLibraryExtractionFolder";
    private static final String JME_EXTRACT_LIBRARIES = "com.jme3.ExtractNativeLibraries";
    private static final String LWJGL_LIBRARY_PATH = "org.lwjgl.librarypath";
    private static final String SAFERALLOC_OVERRIDE = "saferalloc.native.override";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerStartupHook();
    }

    private static void registerStartupHook() {
        try {
            Class<?> runtimeSupportClass = Class.forName("com.oracle.svm.core.jdk.RuntimeSupport");
            Object runtimeSupport = runtimeSupportClass.getMethod("getRuntimeSupport").invoke(null);
            Method addStartupHook = runtimeSupportClass.getMethod("addStartupHook", Runnable.class);
            addStartupHook.invoke(runtimeSupport, (Runnable) JmeNativeRuntimeDefaultsFeature::applyDefaultsAtRuntime);
        } catch (Throwable ignored) {
            // No-op: if startup hooks are unavailable we do not fail native-image builds.
        }
    }

    private static void applyDefaultsAtRuntime() {
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
