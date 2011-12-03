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
import java.io.*;

/**
 * <code>FileLocator</code> allows you to specify a folder where to
 * look for assets. 
 * @author Kirill Vainer
 */
public class FileLocator implements AssetLocator {

    private File root;

    public void setRootPath(String rootPath) {
        if (rootPath == null)
            throw new NullPointerException();
        
        try {
            root = new File(rootPath).getCanonicalFile();
            if (!root.isDirectory()){
                throw new IllegalArgumentException("Given root path \"" + root + "\" not a directory");
            }
        } catch (IOException ex) {
            throw new AssetLoadException("Root path is invalid", ex);
        }
    }

    private static class AssetInfoFile extends AssetInfo {

        private File file;

        public AssetInfoFile(AssetManager manager, AssetKey key, File file){
            super(manager, key);
            this.file = file;
        }

        @Override
        public InputStream openStream() {
            try{
                return new FileInputStream(file);
            }catch (FileNotFoundException ex){
                // NOTE: Can still happen even if file.exists() is true, e.g.
                // permissions issue and similar
                throw new AssetLoadException("Failed to open file: " + file, ex);
            }
        }
    }

    public AssetInfo locate(AssetManager manager, AssetKey key) {
        String name = key.getName();
        File file = new File(root, name);
        if (file.exists() && file.isFile()){
            try {
                // Now, check asset name requirements
                String canonical = file.getCanonicalPath();
                String absolute = file.getAbsolutePath();
                if (!canonical.endsWith(absolute)){
                    throw new AssetNotFoundException("Asset name doesn't match requirements.\n"+
                                                     "\"" + canonical + "\" doesn't match \"" + absolute + "\"");
                }
            } catch (IOException ex) {
                throw new AssetLoadException("Failed to get file canonical path " + file, ex);
            }
            
            return new AssetInfoFile(manager, key, file);
        }else{
            return null;
        }
    }

}
