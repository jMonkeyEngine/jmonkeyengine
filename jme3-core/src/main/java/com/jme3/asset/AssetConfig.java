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
package com.jme3.asset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AssetConfig</code> loads a config file to configure the asset manager.
 * <br/><br/>
 * The config file is specified with the following format:
 * <code>
 * "INCLUDE" <path>
 * "LOADER" <class> : (<extension> ",")* <extension>
 * "LOCATOR" <path> <class>
 * </code>
 *
 * @author Kirill Vainer
 */
public final class AssetConfig {

    private static final Logger logger = Logger.getLogger(AssetConfig.class.getName());
    
    private AssetConfig() { }
    
    private static Class acquireClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }
    
    public static void loadText(AssetManager assetManager, URL configUrl) throws IOException{
        InputStream in = configUrl.openStream();
        try {
            Scanner scan = new Scanner(in, "UTF-8");
            scan.useLocale(Locale.US); // Fix commas / periods ??
            while (scan.hasNext()){
                String cmd = scan.next();
                if (cmd.equals("LOADER")){
                    String loaderClass = scan.next();
                    String colon = scan.next();
                    if (!colon.equals(":")){
                        throw new IOException("Expected ':', got '"+colon+"'");
                    }
                    String extensionsList = scan.nextLine();
                    String[] extensions = extensionsList.split(",");
                    for (int i = 0; i < extensions.length; i++){
                        extensions[i] = extensions[i].trim();
                    }
                    Class clazz = acquireClass(loaderClass);
                    if (clazz != null) {
                        assetManager.registerLoader(clazz, extensions);
                    } else {
                        logger.log(Level.WARNING, "Cannot find loader {0}", loaderClass);
                    }
                } else if (cmd.equals("LOCATOR")) {
                    String rootPath = scan.next();
                    String locatorClass = scan.nextLine().trim();
                    Class clazz = acquireClass(locatorClass);
                    if (clazz != null) {
                        assetManager.registerLocator(rootPath, clazz);
                    } else {
                        logger.log(Level.WARNING, "Cannot find locator {0}", locatorClass);
                    }
                } else if (cmd.equals("INCLUDE")) {
                    String includedCfg = scan.nextLine().trim();
                    URL includedCfgUrl = Thread.currentThread().getContextClassLoader().getResource(includedCfg);
                    if (includedCfgUrl != null) {
                        loadText(assetManager, includedCfgUrl);
                    } else {
                        logger.log(Level.WARNING, "Cannot find config include {0}", includedCfg);
                    }
                } else if (cmd.trim().startsWith("#")) {
                    scan.nextLine();
                    continue;
                } else {
                    throw new IOException("Expected command, got '" + cmd + "'");
                }
            }
        } finally {
            if (in != null) in.close();
        }
    }
}
