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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.system.NativeLibraries.LibraryInfo;
import com.jme3.util.res.Resources;

/**
 * Utility class to register, extract, and load native libraries.
 * <br>
 * Register your own libraries via the {@link #registerNativeLibrary(String, Platform, String, String)} method, for
 * each platform.
 * You can then extract this library (depending on platform), by
 * using {@link #loadNativeLibrary(java.lang.String, boolean) }.
 * <br>
 * Example:<br>
 * <pre>
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Windows64, "native/windows/mystuff64.dll");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Linux64,   "native/linux/libmystuff64.so");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.MacOSX64,  "native/macosx/libmystuff.jnilib");
 * </pre>
 * <br>
 * This will register the library. Load it via: <br>
 * <pre>
 * NativeLibraryLoader.loadNativeLibrary("mystuff", true);
 * </pre>
 * It will load the right library automatically based on the platform.
 * 
 * @author Kirill Vainer
 */
public final class NativeLibraryLoader {
    /**
     * System property containing the filesystem directory where native libraries
     * should be extracted to or loaded from.
     */
    public static final String CUSTOM_EXTRACTION_FOLDER_PROPERTY = "com.jme3.NativeLibraryExtractionFolder";

    /**
     * System property controlling whether native libraries should be extracted
     * from the classpath before loading. Defaults to {@code true}. When false,
     * {@link #CUSTOM_EXTRACTION_FOLDER_PROPERTY} must specify existing natives.
     */
    public static final String EXTRACT_NATIVE_LIBRARIES_PROPERTY = "com.jme3.ExtractNativeLibraries";

    /** Base directory for the automatic native cache. */
    public static final String CACHE_FOLDER_PROPERTY = "com.jme3.CacheFolder";

    private static final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());
    private static File extractionFolderOverride = null;
    private static File extractionFolder = null;
    private static final int EXTRACTION_ROOT_COUNT = 4; // configured, temp, cache, home
    private static int extractionRootIndex = 0; // extraction root to try next
    private static Boolean extractNativeLibrariesOverride = null;
    private static final Map<NativeLibrary, String> loadedLibraries = new HashMap<>();

    private static final HashMap<NativeLibrary.Key, NativeLibrary> nativeLibraryMap = new HashMap<>();

    static {
        NativeLibraries.registerDefaultLibraries();
    }

    /**
     * Register a new native library.
     *
     * This simply registers a known library, the actual extraction and loading
     * is performed by calling {@link #loadNativeLibrary(java.lang.String, boolean) }.
     */
    public static synchronized void registerNativeLibrary(NativeLibrary library) {
        nativeLibraryMap.put(library.getKey(), library);
    }

    /**
     * Register a new native library.
     *
     * This simply registers a known library, the actual extraction and loading is performed by calling
     * {@link #loadNativeLibrary(java.lang.String, boolean) }.
     */
    public static synchronized void registerNativeLibrary(LibraryInfo library) {
        library.getNativeVariants().forEach(NativeLibraryLoader::registerNativeLibrary);
    }

    /**
     * Register a new native library.
     * 
     * This simply registers a known library, the actual extraction and loading
     * is performed by calling {@link #loadNativeLibrary(java.lang.String, boolean) }.
     * 
     * @param name The name / ID of the library (not OS or architecture specific).
     * @param platform The platform for which the in-natives-jar path has 
     * been specified for.
     * @param path The path inside the natives-jar or classpath
     * corresponding to this library. Must be compatible with the platform 
     * argument.
     * @param extractAsName The filename that the library should be extracted as,
     * if null, use the same name as in the path.
     */
    public static synchronized void registerNativeLibrary(String name, Platform platform,
            String path, String extractAsName) {
        nativeLibraryMap.put(new NativeLibrary.Key(name, platform),
                new NativeLibrary(name, platform, path, extractAsName));
    }
    
    /**
     * Register a new native library.
     * 
     * This simply registers a known library, the actual extraction and loading
     * is performed by calling {@link #loadNativeLibrary(java.lang.String, boolean) }.
     * 
     * This method should be called several times for each library name, 
     * each time specifying a different platform + path combination.
     * 
     * @param name The name / ID of the library (not OS or architecture specific).
     * @param platform The platform for which the in-natives-jar path has 
     * been specified for.
     * @param path The path inside the natives-jar or classpath
     * corresponding to this library. Must be compatible with the platform 
     * argument.
     */
    public static synchronized void registerNativeLibrary(String name, Platform platform,
            String path) {
        registerNativeLibrary(name, platform, path, null);
    }
    
    private NativeLibraryLoader() {
    }
    
    /**
     * Determines whether native Bullet is on the classpath.
     * 
     * Currently, the context extracts the native Bullet libraries, so
     * this method is needed to determine if they are needed.
     * Ideally, native Bullet would be responsible for its own natives.
     * 
     * @return True native bullet is on the classpath, false otherwise.
     */
    public static boolean isUsingNativeBullet() {
        try {
            Class clazz = Class.forName("com.jme3.bullet.util.NativeMeshUtil");
            return clazz != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }
    
    /**
     * Specify a custom root for private native extraction directories.
     * Set to <code>null</code> to restore default
     * functionality.
     * 
     * @param path Root where private native directories are created.
     */
    public static synchronized void setCustomExtractionFolder(String path) {
        extractionFolderOverride = path == null ? null : new File(path).getAbsoluteFile();
        extractionFolder = null;
        extractionRootIndex = 0;
    }

    /**
     * Returns the configured custom extraction folder.
     *
     * @return the programmatic override if set, otherwise the
     *         {@link #CUSTOM_EXTRACTION_FOLDER_PROPERTY} system property
     */
    public static synchronized File getCustomExtractionFolder() {
        if (extractionFolderOverride != null) {
            return extractionFolderOverride;
        }

        String extractionFolderProperty = System.getProperty(CUSTOM_EXTRACTION_FOLDER_PROPERTY);
        if (extractionFolderProperty != null && !extractionFolderProperty.trim().isEmpty()) {
            return new File(extractionFolderProperty).getAbsoluteFile();
        }

        return null;
    }

    /**
     * Specify whether native libraries should be extracted from the classpath
     * before loading. When false, configure a folder containing the native
     * files with {@link #setCustomExtractionFolder(String)} or
     * {@link #CUSTOM_EXTRACTION_FOLDER_PROPERTY}.
     *
     * @param extractNativeLibraries true to extract classpath natives, false to
     *                               load existing files from the extraction folder
     */
    public static synchronized void setExtractNativeLibraries(boolean extractNativeLibraries) {
        extractNativeLibrariesOverride = extractNativeLibraries;
    }

    /**
     * Clears the programmatic extraction flag override.
     */
    public static synchronized void clearExtractNativeLibrariesOverride() {
        extractNativeLibrariesOverride = null;
    }

    /**
     * Returns whether native libraries should be extracted before loading.
     *
     * @return the programmatic override if set, otherwise the
     *         {@link #EXTRACT_NATIVE_LIBRARIES_PROPERTY} system property,
     *         defaulting to true
     */
    public static synchronized boolean isExtractNativeLibraries() {
        if (extractNativeLibrariesOverride != null) {
            return extractNativeLibrariesOverride;
        }

        String extractNativeLibrariesProperty = System.getProperty(EXTRACT_NATIVE_LIBRARIES_PROPERTY);
        return extractNativeLibrariesProperty == null
                || extractNativeLibrariesProperty.trim().isEmpty()
                || Boolean.parseBoolean(extractNativeLibrariesProperty);
    }

    /**
     * Returns the folder where native libraries will be extracted or loaded from.
     * Extraction uses a fresh private directory scheduled for deletion on JVM
     * exit; an abrupt termination may leave it behind. Without extraction, a
     * custom folder containing the native files must be configured.
     * @return the folder used to extract or load native libraries
     * @throws IllegalStateException if extraction is disabled without a custom folder
     */
    public static synchronized File getExtractionFolder() {
        if (!isExtractNativeLibraries()) {
            File custom = getCustomExtractionFolder();
            if (custom != null) return custom;
            throw new IllegalStateException("Native library extraction is disabled; configure "
                    + CUSTOM_EXTRACTION_FOLDER_PROPERTY + " with a directory containing the native files.");
        }
        if (extractionFolder != null) return extractionFolder;

        UnsatisfiedLinkError error = new UnsatisfiedLinkError(
                "Cannot find a suitable extraction folder for native libraries.");
        exit:
        while (true) {
            Path root = null;
            try {
                switch (extractionRootIndex) {
                    case 0: { // configured directory
                        File custom = getCustomExtractionFolder();
                        if (custom == null) {
                            extractionRootIndex++;
                            continue;
                        }
                        root = custom.toPath();
                        break;
                    }
                    case 1: { // temp directory
                        String tmp = System.getProperty("java.io.tmpdir", "").trim();
                        if (tmp.isEmpty()) throw new IllegalArgumentException("java.io.tmpdir is not set");
                        root = Paths.get(tmp);
                        if (!root.isAbsolute() || !Files.isDirectory(root)) throw new IllegalArgumentException("java.io.tmpdir is not a valid extraction root");
                        break;
                    }
                    case 2: { // platform cache
                        root = getJmeUserCacheFolder();
                        if (root == null) throw new IOException("No usable cache directory");
                        root = root.resolve(".jme3");
                        break;
                    }
                    case 3: { // user home directory
                        String home = System.getProperty("user.home", "").trim();
                        if (home.isEmpty()) throw new IllegalArgumentException("user.home is not set");
                        root = Paths.get(home);
                        if (!root.isAbsolute() || (root.getParent() != null
                                && !Files.isDirectory(root.getParent()))) throw new IllegalArgumentException("user.home is not a valid extraction root");
                        root = root.resolve(".jme3");
                        break;
                    }
                    default: {
                        break exit;
                    }
                }

                extractionFolder = NativeLibraryExtraction.createDirectory(root, "jme3-natives-").toFile();
                return extractionFolder;
            } catch (IOException | SecurityException | IllegalArgumentException
                    | UnsupportedOperationException failure) {
                error.addSuppressed(new IOException("Cannot use native extraction root: " + root, failure));
            }
            extractionRootIndex++; // next call tries the next root
        }
        extractionRootIndex = 0; // let a later call try every root again
        throw error;
    }

 /**
     * Returns the platform cache folder
     */
    private static Path getJmeUserCacheFolder() {
        Path base = null;
        String cacheFolder = System.getProperty(CACHE_FOLDER_PROPERTY);
        if (cacheFolder != null && !cacheFolder.trim().isEmpty()) {
            base = Paths.get(cacheFolder);
            if (!base.isAbsolute() || !Files.isDirectory(base)) {
                base = null;
                logger.warning(CACHE_FOLDER_PROPERTY
                        + " must be an absolute path and must exist. Falling back to default cache location.");
            }
        }

        if (base == null) {
            String loc = null;
            Platform.Os os = JmeSystem.getPlatform().getOs();
            if (os == Platform.Os.Windows) {
                loc = System.getenv("LOCALAPPDATA");
            } else if (os == Platform.Os.Linux) {
                loc = System.getenv("XDG_CACHE_HOME");
            }

            if (loc != null && !loc.trim().isEmpty()) {
                base = Paths.get(loc);
                if (!base.isAbsolute() || !Files.isDirectory(base)) base = null;
            }
        }

        if (base == null) {
            Platform.Os os = JmeSystem.getPlatform().getOs();
            Path home = Paths.get(System.getProperty("user.home"));
            try{
                switch (os) {
                    case Windows:
                        base = home.resolve("AppData").resolve("Local");
                        break;
                    case MacOS:
                        base = home.resolve("Library").resolve("Caches");
                        break;
                    default:
                        base = home.resolve(".cache");
                        break;
                }
            } catch (Exception e) {
                logger.warning("Failed to determine default cache location: " + e.getMessage());
            }

            if (base != null && (!base.isAbsolute() || !Files.isDirectory(base))) {
                base = null;
            }
        }

        return base;
    }

    /**
     * Checks that the value is a single name which cannot escape a directory.
     */
    private static boolean isSimpleName(String value) {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("..")
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.indexOf(':') >= 0) {
            return false;
        }
        try {
            return !Paths.get(value).isAbsolute();
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }


    public static synchronized File[] getJarsWithNatives() {
        HashSet<File> jarFiles = new HashSet<>();
        for (Map.Entry<NativeLibrary.Key, NativeLibrary> lib : nativeLibraryMap.entrySet()) {
            File jarFile = getJarForNativeLibrary(lib.getValue().getPlatform(), lib.getValue().getName());
            if (jarFile != null) {
                jarFiles.add(jarFile);
            }
        }
        return jarFiles.toArray(new File[0]);
    }
    
    public static synchronized void extractNativeLibraries(Platform platform, File targetDir) throws IOException {
        for (Map.Entry<NativeLibrary.Key, NativeLibrary> lib : nativeLibraryMap.entrySet()) {
            if (lib.getValue().getPlatform() == platform) {
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                extractNativeLibrary(platform, lib.getValue().getName(), targetDir);
            }
        }
    }
    
    /**
     * Removes platform-specific portions of a library file name so
     * that it can be accepted by {@link System#loadLibrary(java.lang.String) }.
     * <p>
     * E.g.<br>
     * <ul>
     * <li>jinput-dx8_64.dll => jinput-dx8_64</li>
     * <li>liblwjgl64.so => lwjgl64</li>
     * <li>libopenal.so => openal</li>
     * </ul>
     * 
     * @param filename The filename to strip platform-specific parts
     * @return The stripped library name
     */
    private static String unmapLibraryName(String filename) {
        StringBuilder sb = new StringBuilder(filename);
        if (sb.indexOf("lib") == 0 && !filename.toLowerCase().endsWith(".dll")) {
            sb.delete(0, 3);
        }
        int dot = sb.lastIndexOf(".");
        if (dot > 0) {
            sb.delete(dot, sb.length());
        }
        return sb.toString();
    }

    public static synchronized File getJarForNativeLibrary(Platform platform, String name) {
        NativeLibrary library = nativeLibraryMap.get(new NativeLibrary.Key(name, platform));
        if (library == null) {
            return null;
        }

        String pathInJar = library.getPathInNativesJar();
        if (pathInJar == null) {
            return null;
        }
        
        String fileNameInJar;
        if (pathInJar.contains("/")) {
            fileNameInJar = pathInJar.substring(pathInJar.lastIndexOf("/") + 1);
        } else {
            fileNameInJar = pathInJar;
        }
        
        URL url = Resources.getResource(pathInJar);
        if (url == null) {
            url = Resources.getResource(fileNameInJar);
        }
        
        if (url == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder(url.toString());
        if (sb.indexOf("jar:file:/") == 0) {
            sb.delete(0, 9);
            sb.delete(sb.indexOf("!"), sb.length());
            return new File(sb.toString());
        } else {
            return null; // not a jar
        }
    }
    
    public static synchronized void extractNativeLibrary(Platform platform, String name, File targetDir) throws IOException {
        NativeLibrary library = nativeLibraryMap.get(new NativeLibrary.Key(name, platform));
        if (library == null) {
            return;
        }

        String pathInJar = library.getPathInNativesJar();
        if (pathInJar == null) {
            return;
        }
        
        URL url = Resources.getResource(pathInJar);
        if (url == null) {
            return;
        }

        String loadedAsFileName = getLoadedAsFileName(library, pathInJar);

        URLConnection conn = url.openConnection();

        File targetFile = new File(targetDir, loadedAsFileName);

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            targetFile.setLastModified(conn.getLastModified());
        }
    }

    /**
     * First extracts the native library and then loads it.
     * 
     * @param name
     *            The name of the library to load.
     * @param isRequired
     *            If true and the library fails to load, throw exception. If false, do nothing if it fails to
     *            load.
     * 
     * @return The absolute path of the loaded library.
     */
    public static synchronized String loadNativeLibrary(String name, boolean isRequired) {
        if (JmeSystem.isLowPermissions()) {
            throw new UnsupportedOperationException("JVM is running under "
                    + "reduced permissions. Cannot load native libraries.");
        }

        Platform platform = JmeSystem.getPlatform();
        NativeLibrary library = nativeLibraryMap.get(new NativeLibrary.Key(name, platform));
        
        if (library == null) {
            // No library exists for this platform.
            if (isRequired) {
                throw new UnsatisfiedLinkError(
                        "The required native library '" + name + "'"
                        + " is not available for your OS: " + platform);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "The optional native library ''{0}''" +
                                    " is not available for your OS: {1}",
                            new Object[]{name, platform});
                }
                return null;
            }
        }
        
        final String pathInJar = library.getPathInNativesJar();

        if (pathInJar == null) {
            // This platform does not require the native library to be loaded.
            return null;
        }

        String loadedAsFileName = getLoadedAsFileName(library, pathInJar);

        if (isExtractNativeLibraries()) {
            URL url = Resources.getResource(pathInJar);

            if (url == null) {
                if (isRequired) {
                    throw new UnsatisfiedLinkError(
                            "The required native library '" + library.getName() + "'"
                                    + " was not found in the classpath via '" + pathInJar);
                } else if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "The optional native library ''{0}''" +
                                    " was not found in the classpath via ''{1}''.",
                            new Object[]{library.getName(), pathInJar});
                }
                return null;
            }

            String loaded = loadedLibraries.get(library);
            if (loaded != null) {
                return loaded;
            }
            UnsatisfiedLinkError error = new UnsatisfiedLinkError(
                    "Cannot extract/load native libraries from the configured directory, temp, user cache, or ~/.jme3.");
            while (extractionRootIndex < EXTRACTION_ROOT_COUNT) {
                Path target = null;
                boolean created = false;
                try {
                    File directory = getExtractionFolder();
                    target = directory.toPath().resolve(loadedAsFileName);
                    try (InputStream in = url.openStream();
                        OutputStream out = Files.newOutputStream(target,
                                StandardOpenOption.CREATE_NEW,
                                StandardOpenOption.WRITE)) {
                        created = true;
                        target.toFile().deleteOnExit();
                        byte[] buffer = new byte[8192];
                        for (int read; (read = in.read(buffer)) != -1;) {
                            out.write(buffer, 0, read);
                        }
                    }
                    loaded = target.toAbsolutePath().toString();
                    library.getLoadFunction().accept(loaded);
                    loadedLibraries.put(library, loaded);
                    return loaded;

                } catch (IOException | UnsatisfiedLinkError | SecurityException failure) {
                    error.addSuppressed(new IOException("Failed to extract/load native library: " + target, failure));
                    if (target == null) {
                        break; // getExtractionFolder() has exhausted every root.
                    }
                    if (created) {
                        try {
                            Files.deleteIfExists(target);
                        } catch (IOException | SecurityException cleanupFailure) {
                            failure.addSuppressed(cleanupFailure);
                        }
                    }
                    // Keep successful libraries in the previous directory intact.
                    extractionFolder = null;
                    extractionRootIndex++;
                }
            }
            extractionRootIndex = 0;
            if (isRequired) {
                throw error;
            }
            logger.log(Level.FINE, "Optional native library could not be loaded: " + library.getName(), error);
            return null;
        }

        File directory;
        try {
            directory = getExtractionFolder();
        } catch (IllegalStateException missingFolder) {
            if (isRequired) throw new UnsatisfiedLinkError(missingFolder.getMessage());
            logger.log(Level.FINE, missingFolder.getMessage());
            return null;
        }
        File targetFile = new File(directory, loadedAsFileName);
        if (!targetFile.isFile()) {
            if (isRequired) {
                throw new UnsatisfiedLinkError(
                        "The required native library '" + library.getName() + "'"
                                + " was not found at '" + targetFile
                                + "' and native library extraction is disabled");
            } else if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "The optional native library ''{0}''" +
                                " was not found at ''{1}'' and native library extraction is disabled.",
                        new Object[]{library.getName(), targetFile});
            }
            return null;
        }

        library.getLoadFunction().accept(targetFile.getAbsolutePath());

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Loaded native library {0}.", library.getName());
        }

        return targetFile.getAbsolutePath();
    }

    private static String getLoadedAsFileName(NativeLibrary library, String pathInJar) {
        String filename = library.getExtractedAsName() != null
                ? library.getExtractedAsName() : Paths.get(pathInJar).getFileName().toString();
        if (!isSimpleName(filename)) {
            throw new IllegalArgumentException("Native library extraction requires a file name: " + filename);
        }
        return filename;
    }

}
