/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.asset.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * <code>ZipLocator</code> is a locator that looks up resources in a .ZIP file.
 * @author Kirill Vainer
 */
public class ZipLocator implements AssetLocator {

    private ZipFile zipfile;
    private static final Logger logger = Logger.getLogger(ZipLocator.class.getName());

    private class JarAssetInfo extends AssetInfo {

        private final ZipEntry entry;

        public JarAssetInfo(AssetManager manager, AssetKey key, ZipEntry entry){
            super(manager, key);
            this.entry = entry;
        }

        public InputStream openStream(){
            try{
                return zipfile.getInputStream(entry);
            }catch (IOException ex){
                throw new AssetLoadException("Failed to load zip entry: "+entry, ex);
            }
        }
    }

    public void setRootPath(String rootPath) {
        try{
            zipfile = new ZipFile(new File(rootPath), ZipFile.OPEN_READ);
        }catch (IOException ex){
            throw new AssetLoadException("Failed to open zip file: " + rootPath, ex);
        }
    }

    public AssetInfo locate(AssetManager manager, AssetKey key) {
        String name = key.getName();
        ZipEntry entry = zipfile.getEntry(name);
        if (entry == null)
            return null;
        
        return new JarAssetInfo(manager, key, entry);
    }

}
