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
package com.jme3.asset.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Handles loading of assets from a URL
 * 
 * @author Kirill Vainer
 */
public class UrlAssetInfo extends AssetInfo {
    
    private URL url;
    private InputStream in;
    
    public static UrlAssetInfo create(AssetManager assetManager, AssetKey key, URL url) throws IOException {
        // Check if URL can be reached. This will throw
        // IOException which calling code will handle.
        URLConnection conn = url.openConnection();
        conn.setUseCaches(false);
        InputStream in = conn.getInputStream();
        
        // For some reason url cannot be reached?
        if (in == null){
            return null;
        }else{
            return new UrlAssetInfo(assetManager, key, url, in);
        }
    }
    
    private UrlAssetInfo(AssetManager assetManager, AssetKey key, URL url, InputStream in) throws IOException {
        super(assetManager, key);
        this.url = url;
        this.in = in;
    }
    
    public boolean hasInitialConnection(){
        return in != null;
    }
    
    @Override
    public InputStream openStream() {
        if (in != null){
            // Reuse the already existing stream (only once)
            InputStream in2 = in;
            in = null;
            return in2;
        }else{
            // Create a new stream for subsequent invocations.
            try {
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                return conn.getInputStream();
            } catch (IOException ex) {
                throw new AssetLoadException("Failed to read URL " + url, ex);
            }
        }
    }
}
