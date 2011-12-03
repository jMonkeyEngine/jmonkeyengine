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

package jme3test.asset;

import com.jme3.asset.*;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.asset.plugins.ZipLocator;

public class TestManyLocators {
    public static void main(String[] args){
        AssetManager am = new DesktopAssetManager();

        am.registerLocator("http://www.jmonkeyengine.com/wp-content/uploads/2010/09/",
                           UrlLocator.class);

        am.registerLocator("town.zip", ZipLocator.class);
        am.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip",
                           HttpZipLocator.class);
        
        
        am.registerLocator("/", ClasspathLocator.class);
        
        

        // Try loading from Core-Data source package
        AssetInfo a = am.locateAsset(new AssetKey<Object>("Interface/Fonts/Default.fnt"));

        // Try loading from town scene zip file
        AssetInfo b = am.locateAsset(new ModelKey("casaamarela.jpg"));

        // Try loading from wildhouse online scene zip file
        AssetInfo c = am.locateAsset(new ModelKey("glasstile2.png"));

        // Try loading directly from HTTP
        AssetInfo d = am.locateAsset(new TextureKey("planet-2.jpg"));

        if (a == null)
            System.out.println("Failed to load from classpath");
        else
            System.out.println("Found classpath font: " + a.toString());

        if (b == null)
            System.out.println("Failed to load from town.zip");
        else
            System.out.println("Found zip image: " + b.toString());

        if (c == null)
            System.out.println("Failed to load from wildhouse.zip on googlecode.com");
        else
            System.out.println("Found online zip image: " + c.toString());

        if (d == null)
            System.out.println("Failed to load from HTTP");
        else
            System.out.println("Found HTTP showcase image: " + d.toString());
    }
}
