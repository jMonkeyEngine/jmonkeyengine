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

package jme3tools.converters;

import com.jme3.asset.AssetManager;
import com.jme3.system.JmeSystem;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FolderConverter {

    private static AssetManager assetManager;
    private static File sourceRoot;
    private static JarOutputStream jarOut;
    private static long time;

    private static void process(File file) throws IOException{
        String name = file.getName().replaceAll("[\\/\\.]", "_");
        JarEntry entry = new JarEntry(name);
        entry.setTime(time);

        jarOut.putNextEntry(entry);
    }

    public static void main(String[] args) throws IOException{
        if (args.length == 0){
            System.out.println("Usage: java -jar FolderConverter <input folder>");
            System.out.println();
            System.out.println("  Converts files from input to output");
            System.exit(1);
        }

        sourceRoot = new File(args[0]);
        
        File jarFile = new File(sourceRoot.getParent(), sourceRoot.getName()+".jar");
        FileOutputStream out = new FileOutputStream(jarFile);
        jarOut = new JarOutputStream(out);

        assetManager = JmeSystem.newAssetManager();
        assetManager.registerLocator(sourceRoot.toString(), 
                                     "com.jme3.asset.plugins.FileSystemLocator");
        for (File f : sourceRoot.listFiles()){
             process(f);
        }
    }

}
