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
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.asset.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import org.ngengine.libjglios.core.LibJGLIOSBundleBridge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Locates assets inside the iOS application bundle resource directory.
 */
public class IosBundleLocator implements AssetLocator {

    private File root;

    @Override
    public void setRootPath(String rootPath) {
        String resourcePath = LibJGLIOSBundleBridge.resourcePath();
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new AssetLoadException("Unable to resolve iOS bundle resource path");
        }

        String relativeRoot = normalizeRootPath(rootPath);
        try {
            root = new File(resourcePath, relativeRoot).getCanonicalFile();
            if (!root.isDirectory()) {
                throw new IllegalArgumentException("Given iOS bundle root path \"" + root + "\" is not a directory");
            }
        } catch (IOException ex) {
            throw new AssetLoadException("iOS bundle root path is invalid", ex);
        }
    }

    private static String normalizeRootPath(String rootPath) {
        if (rootPath == null || rootPath.isEmpty() || rootPath.equals("/") || rootPath.equals(".")) {
            return "";
        }

        String normalized = rootPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Override
    public AssetInfo locate(AssetManager manager, AssetKey key) {
        String name = key.getName();
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        File file = new File(root, name);
        if (!file.exists() || !file.isFile()) {
            return null;
        }

        try {
            File canonicalFile = file.getCanonicalFile();
            if (!isInsideRoot(canonicalFile)) {
                return null;
            }
            return new BundleAssetInfo(manager, key, canonicalFile);
        } catch (IOException ex) {
            throw new AssetLoadException("Failed to resolve iOS bundle asset path " + file, ex);
        }
    }

    private boolean isInsideRoot(File file) {
        String rootPath = root.getPath();
        String filePath = file.getPath();
        return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
    }

    private static final class BundleAssetInfo extends AssetInfo {

        private final File file;

        private BundleAssetInfo(AssetManager manager, AssetKey key, File file) {
            super(manager, key);
            this.file = file;
        }

        @Override
        public InputStream openStream() {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException ex) {
                throw new AssetLoadException("Failed to open iOS bundle asset: " + file, ex);
            }
        }
    }
}
