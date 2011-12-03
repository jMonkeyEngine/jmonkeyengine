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
import com.jme3.system.JmeSystem;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * The <code>ClasspathLocator</code> looks up an asset in the classpath.
 * @author Kirill Vainer
 */
public class ClasspathLocator implements AssetLocator {

    private static final Logger logger = Logger.getLogger(ClasspathLocator.class.getName());
    private String root = "";

    public ClasspathLocator(){
    }

    public void setRootPath(String rootPath) {
        this.root = rootPath;
        if (root.equals("/"))
            root = "";
        else if (root.length() > 1){
            if (root.startsWith("/")){
                root = root.substring(1);
            }
            if (!root.endsWith("/"))
                root += "/";
        }
    }
    
    public AssetInfo locate(AssetManager manager, AssetKey key) {
        URL url;
        String name = key.getName();
        if (name.startsWith("/"))
            name = name.substring(1);

        name = root + name;
//        if (!name.startsWith(root)){
//            name = root + name;
//        }

        if (JmeSystem.isLowPermissions()){
            url = ClasspathLocator.class.getResource("/" + name);
        }else{
            url = Thread.currentThread().getContextClassLoader().getResource(name);
        }
        if (url == null)
            return null;
        
        if (url.getProtocol().equals("file")){
            try {
                String path = new File(url.toURI()).getCanonicalPath();
                
                // convert to / for windows
                if (File.separatorChar == '\\'){
                    path = path.replace('\\', '/');
                }
                
                // compare path
                if (!path.endsWith(name)){
                    throw new AssetNotFoundException("Asset name doesn't match requirements.\n"+
                                                     "\"" + path + "\" doesn't match \"" + name + "\"");
                }
            } catch (URISyntaxException ex) {
                throw new AssetLoadException("Error converting URL to URI", ex);
            } catch (IOException ex){
                throw new AssetLoadException("Failed to get canonical path for " + url, ex);
            }
        }
        
        try{
            return UrlAssetInfo.create(manager, key, url);
        }catch (IOException ex){
            // This is different handling than URL locator
            // since classpath locating would return null at the getResource() 
            // call, otherwise there's a more critical error...
            throw new AssetLoadException("Failed to read URL " + url, ex);
        }
    }
}
