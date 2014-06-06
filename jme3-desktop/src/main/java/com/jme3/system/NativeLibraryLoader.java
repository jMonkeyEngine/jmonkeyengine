/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to register, extract, and load native libraries.
 * <br>
 * Register your own libraries via the 
 * {@link #registerNativeLibrary(java.lang.String, com.jme3.system.Platform, java.lang.String, boolean) }
 * method, for each platform. 
 * You can then extract this library (depending on platform), by
 * using {@link #loadNativeLibrary(java.lang.String, boolean) }.
 * <br>
 * Example:<br>
 * <code><pre>
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Windows32, "native/windows/mystuff.dll");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Windows64, "native/windows/mystuff64.dll");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Linux32,   "native/linux/libmystuff.so");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.Linux64,   "native/linux/libmystuff64.so");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.MacOSX32,  "native/macosx/libmystuff.jnilib");
 * NativeLibraryLoader.registerNativeLibrary("mystuff", Platform.MacOSX64,  "native/macosx/libmystuff.jnilib");
 * </pre></code>
 * <br>
 * This will register the library. Load it via: <br>
 * <code><pre>
 * NativeLibraryLoader.loadNativeLibrary("mystuff", true);
 * </pre></code>
 * It will load the right library automatically based on the platform.
 * 
 * @author Kirill Vainer
 */
public final class NativeLibraryLoader {
    
    private static final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());
    private static final byte[] buf = new byte[1024 * 100];
    private static File extractionFolderOverride = null;
    private static File extractionFolder = null;
    
    private static final HashMap<NativeLibrary.Key, NativeLibrary> nativeLibraryMap
            = new HashMap<NativeLibrary.Key, NativeLibrary>();
    
    /**
     * Register a new known library.
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
     * @param isJNI True if this is a JNI library, false if this is a regular
     * native (C/C++) library.
     */
    public static void registerNativeLibrary(String name, Platform platform,
            String path, boolean isJNI) {
        nativeLibraryMap.put(new NativeLibrary.Key(name, platform),
                new NativeLibrary(name, platform, path, isJNI));
    }
    
    /**
     * Register a new known JNI library.
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
    public static void registerNativeLibrary(String name, Platform platform,
            String path) {
        registerNativeLibrary(name, platform, path, true);
    }
    
    static {
        // LWJGL
        registerNativeLibrary("lwjgl", Platform.Windows32, "native/windows/lwjgl.dll");
        registerNativeLibrary("lwjgl", Platform.Windows64, "native/windows/lwjgl64.dll");
        registerNativeLibrary("lwjgl", Platform.Linux32,   "native/linux/liblwjgl.so");
        registerNativeLibrary("lwjgl", Platform.Linux64,   "native/linux/liblwjgl64.so");
        registerNativeLibrary("lwjgl", Platform.MacOSX32,  "native/macosx/liblwjgl.jnilib");
        registerNativeLibrary("lwjgl", Platform.MacOSX64,  "native/macosx/liblwjgl.jnilib");
        
        // OpenAL
        registerNativeLibrary("openal", Platform.Windows32, "native/windows/OpenAL32.dll", false);
        registerNativeLibrary("openal", Platform.Windows64, "native/windows/OpenAL64.dll", false);
        registerNativeLibrary("openal", Platform.Linux32,   "native/linux/libopenal.so", false);
        registerNativeLibrary("openal", Platform.Linux64,   "native/linux/libopenal64.so", false);
        registerNativeLibrary("openal", Platform.MacOSX32,  "native/macosx/openal.dylib", false);
        registerNativeLibrary("openal", Platform.MacOSX64,  "native/macosx/openal.dylib", false);
        
        // BulletJme
        registerNativeLibrary("bulletjme", Platform.Windows32, "native/windows/x86/bulletjme.dll", false);
        registerNativeLibrary("bulletjme", Platform.Windows64, "native/windows/x86_64/bulletjme.dll", false);
        registerNativeLibrary("bulletjme", Platform.Linux32,   "native/linux/x86/libbulletjme.so", false);
        registerNativeLibrary("bulletjme", Platform.Linux64,   "native/linux/x86_64/libbulletjme.so", false);
        registerNativeLibrary("bulletjme", Platform.MacOSX32,  "native/macosx/x86/libbulletjme.jnilib", false);
        registerNativeLibrary("bulletjme", Platform.MacOSX64,  "native/macosx/x86_64/libbulletjme.jnilib", false);
        
        // JInput
        registerNativeLibrary("jinput", Platform.Windows32, "native/windows/jinput-raw.dll");
        registerNativeLibrary("jinput", Platform.Windows64, "native/windows/jinput-raw_64.dll");
        registerNativeLibrary("jinput", Platform.Linux32,   "native/windows/libjinput-linux.so");
        registerNativeLibrary("jinput", Platform.Linux64,   "native/windows/libjinput-linux64.so");
        registerNativeLibrary("jinput", Platform.MacOSX32,  "native/macosx/libjinput-osx.jnilib");
        registerNativeLibrary("jinput", Platform.MacOSX64,  "native/macosx/libjinput-osx.jnilib");
        
        // JInput Auxiliary (only required on Windows)
        registerNativeLibrary("jinput-dx8", Platform.Windows32, "native/windows/jinput-dx8.dll");
        registerNativeLibrary("jinput-dx8", Platform.Windows64, "native/windows/jinput-dx8_64.dll");
        registerNativeLibrary("jinput-dx8", Platform.Linux32,   null);
        registerNativeLibrary("jinput-dx8", Platform.Linux64,   null);
        registerNativeLibrary("jinput-dx8", Platform.MacOSX32,  null);
        registerNativeLibrary("jinput-dx8", Platform.MacOSX64,  null);
    }
    
    private NativeLibraryLoader() {
    }
    
    /**
     * Specify a custom location where native libraries should
     * be extracted to. Ensure this is a unique path not used
     * by other applications to extract their libraries.
     * Set to <code>null</code> to restore default
     * functionality.
     * 
     * @param path Path where to extract native libraries.
     */
    public static void setCustomExtractionFolder(String path) {
        extractionFolderOverride = new File(path).getAbsoluteFile();
    }

    /**
     * Returns the folder where native libraries will be extracted.
     * This is automatically determined at run-time based on the 
     * following criteria:<br>
     * <ul>
     * <li>If a {@link #setCustomExtractionFolder(java.lang.String) custom
     * extraction folder} has been specified, it is returned.
     * <li>If the user can write to the working folder, then it 
     * is returned.</li>
     * <li>Otherwise, the {@link JmeSystem#getStorageFolder() storage folder}
     * is used, to prevent collisions, a special subfolder is used
     * called <code>natives_&lt;hash&gt;</code> where &lt;hash&gt;
     * is computed automatically as the XOR of the classpath hash code
     * and the last modified date of this class.
     * 
     * @return Path where natives will be extracted to.
     */
    public static File getExtractionFolder() {
        if (extractionFolderOverride != null) {
            return extractionFolderOverride;
        }
        if (extractionFolder == null) {
            File workingFolder = new File("").getAbsoluteFile();
            if (!workingFolder.canWrite()) {
                setExtractionDirToStorageDir();
            } else {
                try {
                    File file = new File(workingFolder + File.separator + ".jmetestwrite");
                    file.createNewFile();
                    file.delete();
                    extractionFolder = workingFolder;
                } catch (Exception e) {
                    setExtractionDirToStorageDir();
                }
            }
        }
        return extractionFolder;
    }

    private static void setExtractionDirToStorageDir() {
        logger.log(Level.WARNING, "Working directory is not writable. Using home directory instead.");
        extractionFolder = new File(JmeSystem.getStorageFolder(),
                "natives_" + Integer.toHexString(computeNativesHash()));
        if (!extractionFolder.exists()) {
            extractionFolder.mkdir();
        }
    }

    private static int computeNativesHash() {
        URLConnection conn = null;
        try {
            String classpath = System.getProperty("java.class.path");
            URL url = Thread.currentThread().getContextClassLoader().getResource("com/jme3/system/Natives.class");

            StringBuilder sb = new StringBuilder(url.toString());
            if (sb.indexOf("jar:") == 0) {
                sb.delete(0, 4);
                sb.delete(sb.indexOf("!"), sb.length());
                sb.delete(sb.lastIndexOf("/") + 1, sb.length());
            }
            try {
                url = new URL(sb.toString());
            } catch (MalformedURLException ex) {
                throw new UnsupportedOperationException(ex);
            }

            conn = url.openConnection();
            int hash = classpath.hashCode() ^ (int) conn.getLastModified();
            return hash;
        } catch (IOException ex) {
            throw new UnsupportedOperationException(ex);
        } finally {
            if (conn != null) {
                try {
                    conn.getInputStream().close();
                    conn.getOutputStream().close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * First extracts the native library and then loads it.
     * 
     * @param name The name of the library to load.
     * @param isRequired If true and the library fails to load, throw exception. If
     * false, do nothing if it fails to load.
     */
    public static void loadNativeLibrary(String name, boolean isRequired) {
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
                logger.log(Level.FINE, "The optional native library ''{0}''" +
                                       " is not available for your OS: {1}", 
                                       new Object[]{name, platform});
                return;
            }
        }
        
        String pathInJar = library.getPathInNativesJar();
        
        if (pathInJar == null) {
            // This platform does not require the native library to be loaded.
            return;
        }
        
        String fileNameInJar;
        if (pathInJar.contains("/")) {
            fileNameInJar = pathInJar.substring(pathInJar.lastIndexOf("/") + 1);
        } else {
            fileNameInJar = pathInJar;
        }
        
        URL url = Thread.currentThread().getContextClassLoader().getResource(pathInJar);
        
        if (url == null) {
            // Try the root of the classpath as well.
            url = Thread.currentThread().getContextClassLoader().getResource(fileNameInJar);
        }
        
        if (url == null) {
            // Attempt to load it as a system library.
            try {
                System.loadLibrary(name);
                logger.log(Level.FINE, "Loaded system installed " + 
                                       "version of native library: {0}", name);
            } catch (UnsatisfiedLinkError e) {
                if (isRequired) {
                    throw new UnsatisfiedLinkError(
                            "The required native library '" + name + "'"
                            + " was not found in the classpath via '" + pathInJar + "'");
                } else {
                    logger.log(Level.FINE, "The optional native library ''{0}''" + 
                                           " was not found in the classpath via ''{1}''", 
                                           new Object[]{name, pathInJar});
                }
            }
            
            return;
        }
        
        // The library has been found and is ready to be extracted.
        // Determine what filename it should be extracted as.
        String loadedAsFileName;
        if (library.isJNI()) {
            String nameWithArch;
            
            // Append "64" to path 
            // so that we don't overwrite the 32-bit version.
            if (platform.is64Bit()) {
                nameWithArch = name + "64";
            } else {
                nameWithArch = name;
            }
            
            // JNI libraries on Mac / JDK6 use jnilib extension.
            // JNI libraries on Mac / JDK7 use dylib extension.
            loadedAsFileName = System.mapLibraryName(nameWithArch);
        } else {
            // Not a JNI library.
            // Just use the original filename as it is in the JAR.
            loadedAsFileName = fileNameInJar;
        }
        
        File extactionDirectory = getExtractionFolder();
        URLConnection conn;
        InputStream in;
        
        try {
            conn = url.openConnection();
            in = conn.getInputStream();
        } catch (IOException ex) {
            // Maybe put more detail here? Not sure..
            throw new UnsatisfiedLinkError("Failed to open file: '" + url + 
                                           "'. Error: " + ex);
        }
        
        File targetFile = new File(extactionDirectory, loadedAsFileName);
        OutputStream out = null;
        try {
            if (targetFile.exists()) {
                // OK, compare last modified date of this file to 
                // file in jar
                long targetLastModified = targetFile.lastModified();
                long sourceLastModified = conn.getLastModified();

                // Allow ~1 second range for OSes that only support low precision
                if (targetLastModified + 1000 > sourceLastModified) {
                    logger.log(Level.FINE, "Not copying library {0}. " + 
                                           "Latest already extracted.", 
                                           loadedAsFileName);
                    return;
                }
            }

            out = new FileOutputStream(targetFile);
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            
            in.close();
            in = null;
            out.close();
            out = null;

            // NOTE: On OSes that support "Date Created" property, 
            // this will cause the last modified date to be lower than
            // date created which makes no sense
            targetFile.setLastModified(conn.getLastModified());
        } catch (IOException ex) {
            if (ex.getMessage().contains("used by another process")) {
                return;
            } else {
                throw new UnsatisfiedLinkError("Failed to extract native "
                        + "library to: " + targetFile);
            }
        } finally {
            // Not sure if we always want to load it.
            // Maybe specify this as a per library setting.
            System.load(targetFile.getAbsolutePath());
            
            if(in != null){
                try { in.close(); } catch (IOException ex) { }
            }
            if(out != null){
                try { out.close(); } catch (IOException ex) { }
            }
        }
        
        logger.log(Level.FINE, "Loaded native library from ''{0}'' into ''{1}''", 
                   new Object[]{url, targetFile});
    }
    
}
