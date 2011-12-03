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

import com.jme3.export.*;
import java.io.IOException;
import java.util.LinkedList;

/**
 * <code>AssetKey</code> is a key that is used to
 * look up a resource from a cache. 
 * This class should be immutable.
 */
public class AssetKey<T> implements Savable {

    protected String name;
    protected transient String folder;
    protected transient String extension;
    
    public AssetKey(String name){
        this.name = reducePath(name);
        this.extension = getExtension(this.name);
    }

    public AssetKey(){
    }

    protected static String getExtension(String name){
        int idx = name.lastIndexOf('.');
        //workaround for filenames ending with xml and another dot ending before that (my.mesh.xml)
        if (name.toLowerCase().endsWith(".xml")) {
            idx = name.substring(0, idx).lastIndexOf('.');
            if (idx == -1) {
                idx = name.lastIndexOf('.');
            }
        }
        if (idx <= 0 || idx == name.length() - 1)
            return "";
        else
            return name.substring(idx+1).toLowerCase();
    }

    protected static String getFolder(String name){
        int idx = name.lastIndexOf('/');
        if (idx <= 0 || idx == name.length() - 1)
            return "";
        else
            return name.substring(0, idx+1);
    }

    public String getName() {
        return name;
    }

    /**
     * @return The extension of the <code>AssetKey</code>'s name. For example,
     * the name "Interface/Logo/Monkey.png" has an extension of "png".
     */
    public String getExtension() {
        return extension;
    }

    public String getFolder(){
        if (folder == null)
            folder = getFolder(name);
        
        return folder;
    }

    /**
     * Do any post-processing on the resource after it has been loaded.
     * @param asset
     */
    public Object postProcess(Object asset){
        return asset;
    }

    /**
     * Create a new instance of the asset, based on a prototype that is stored
     * in the cache. Implementations are allowed to return the given parameter
     * as-is if it is considered that cloning is not necessary for that particular
     * asset type.
     * 
     * @param asset The asset to be cloned.
     * @return The asset, possibly cloned.
     */
    public Object createClonedInstance(Object asset){
        return asset;
    }

    /**
     * @return True if the asset for this key should be cached. Subclasses
     * should override this method if they want to override caching behavior.
     */
    public boolean shouldCache(){
        return true;
    }

    /**
     * @return Should return true, if the asset objects implement the "Asset"
     * interface and want to be removed from the cache when no longer
     * referenced in user-code.
     */
    public boolean useSmartCache(){
        return false;
    }
    
    /**
     * Removes all relative elements of a path (A/B/../C.png and A/./C.png).
     * @param path The path containing relative elements
     * @return A path without relative elements
     */
    public static String reducePath(String path) {
        if (path == null || path.indexOf("./") == -1) {
            return path;
        }
        String[] parts = path.split("/");
        LinkedList<String> list = new LinkedList<String>();
        for (int i = 0; i < parts.length; i++) {
            String string = parts[i];
            if (string.length() == 0 || string.equals(".")) {
                //do nothing
            } else if (string.equals("..")) {
                if (list.size() > 0) {
                    list.removeLast();
                } else {
                    throw new IllegalStateException("Relative path is outside assetmanager root!");
                }
            } else {
                list.add(string);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String string = list.get(i);
            if (i != 0) {
                builder.append("/");
            }
            builder.append(string);
        }
        return builder.toString();
    }
    
    @Override
    public boolean equals(Object other){
        if (!(other instanceof AssetKey)){
            return false;
        }
        return name.equals(((AssetKey)other).name);
    }

    @Override
    public int hashCode(){
        return name.hashCode();
    }

    @Override
    public String toString(){
        return name;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        name = reducePath(ic.readString("name", null));
        extension = getExtension(name);
    }

}
