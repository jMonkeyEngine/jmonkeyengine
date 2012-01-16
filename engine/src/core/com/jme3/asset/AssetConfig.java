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

package com.jme3.asset;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>AssetConfig</code> loads a config file to configure the asset manager.
 * <br/><br/>
 * The config file is specified with the following format:
 * <code>
 * "LOADER" <class> : (<extension> ",")* <extension>
 * "LOCATOR" <path> <class> : (<extension> ",")* <extension>
 * </code>
 *
 * @author Kirill Vainer
 */
public class AssetConfig {

    private AssetManager manager;

    public AssetConfig(AssetManager manager){
        this.manager = manager;
    }

    public void loadText(InputStream in) throws IOException{
        Scanner scan = new Scanner(in);
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
                if (hasClass(loaderClass)) {
                    manager.registerLoader(loaderClass, extensions);
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot find loader {0}", loaderClass);
                }
            } else if (cmd.equals("LOCATOR")) {
                String rootPath = scan.next();
                String locatorClass = scan.nextLine().trim();
                if (hasClass(locatorClass)) {
                    manager.registerLocator(rootPath, locatorClass);
                } else {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Cannot find locator {0}", locatorClass);
                }
            } else {
                throw new IOException("Expected command, got '" + cmd + "'");
            }
        }
    }
    
    private boolean hasClass(String name) {
        try {
            Class clazz = Class.forName(name);
            return clazz != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static String readString(DataInput dataIn) throws IOException{
        int length = dataIn.readUnsignedShort();
        char[] chrs = new char[length];
        for (int i = 0; i < length; i++){
            chrs[i] = (char) dataIn.readUnsignedByte();
        }
        return String.valueOf(chrs);
    }

    /*
    public void loadBinary(DataInput dataIn) throws IOException{
        // read signature and version

        // how many locator entries?
        int locatorEntries = dataIn.readUnsignedShort();
        for (int i = 0; i < locatorEntries; i++){
            String locatorClazz = readString(dataIn);
            String rootPath = readString(dataIn);
            manager.registerLocator(rootPath, locatorClazz);
        }

        int loaderEntries = dataIn.readUnsignedShort();
        for (int i = 0; i < loaderEntries; i++){
            String loaderClazz = readString(dataIn);
            int numExtensions = dataIn.readUnsignedByte();
            String[] extensions = new String[numExtensions];
            for (int j = 0; j < numExtensions; j++){
                extensions[j] = readString(dataIn);
            }

            manager.registerLoader(loaderClazz, extensions);
        }
    }
    */
}
