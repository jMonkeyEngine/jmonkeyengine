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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>ImplHandler</code> manages the asset loader and asset locator
 * implementations in a thread safe way. This allows implementations
 * which store local persistent data to operate with a multi-threaded system.
 * This is done by keeping an instance of each asset loader and asset
 * locator object in a thread local.
 */
public class ImplHandler {

    private static final Logger logger = Logger.getLogger(ImplHandler.class.getName());

    private final AssetManager owner;
    
    private final ThreadLocal<AssetKey> parentAssetKey 
            = new ThreadLocal<AssetKey>();
    
    private final ArrayList<ImplThreadLocal> genericLocators =
                new ArrayList<ImplThreadLocal>();

    private final HashMap<String, ImplThreadLocal> loaders =
                new HashMap<String, ImplThreadLocal>();

    public ImplHandler(AssetManager owner){
        this.owner = owner;
    }

    protected class ImplThreadLocal extends ThreadLocal {

        private final Class<?> type;
        private final String path;

        public ImplThreadLocal(Class<?> type){
            this.type = type;
            path = null;
        }

        public ImplThreadLocal(Class<?> type, String path){
            this.type = type;
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public Class<?> getTypeClass(){
            return type;
        }

        @Override
        protected Object initialValue(){
            try {
                return type.newInstance();
            } catch (InstantiationException ex) {
                logger.log(Level.SEVERE,"Cannot create locator of type {0}, does"
                            + " the class have an empty and publically accessible"+
                              " constructor?", type.getName());
                logger.throwing(type.getName(), "<init>", ex);
            } catch (IllegalAccessException ex) {
                logger.log(Level.SEVERE,"Cannot create locator of type {0}, "
                            + "does the class have an empty and publically "
                            + "accessible constructor?", type.getName());
                logger.throwing(type.getName(), "<init>", ex);
            }
            return null;
        }
    }

    /**
     * Establishes the asset key that is used for tracking dependent assets
     * that have failed to load. When set, the {@link DesktopAssetManager}
     * gets a hint that it should suppress {@link AssetNotFoundException}s
     * and instead call the listener callback (if set).
     * 
     * @param parentKey The parent key  
     */
    public void establishParentKey(AssetKey parentKey){
        if (parentAssetKey.get() == null){
            parentAssetKey.set(parentKey);
        }
    }
    
    public void releaseParentKey(AssetKey parentKey){
        if (parentAssetKey.get() == parentKey){
            parentAssetKey.set(null);
        }
    }
    
    public AssetKey getParentKey(){
        return parentAssetKey.get();
    }
    
    /**
     * Attempts to locate the given resource name.
     * @param key The full name of the resource.
     * @return The AssetInfo containing resource information required for
     * access, or null if not found.
     */
    public AssetInfo tryLocate(AssetKey key){
        synchronized (genericLocators){
            if (genericLocators.isEmpty())
                return null;

            for (ImplThreadLocal local : genericLocators){
                AssetLocator locator = (AssetLocator) local.get();
                if (local.getPath() != null){
                    locator.setRootPath((String) local.getPath());
                }
                AssetInfo info = locator.locate(owner, key);
                if (info != null)
                    return info;
            }
        }
        return null;
    }

    public int getLocatorCount(){
        synchronized (genericLocators){
            return genericLocators.size();
        }
    }

    /**
     * Returns the AssetLoader registered for the given extension
     * of the current thread.
     * @return AssetLoader registered with addLoader.
     */
    public AssetLoader aquireLoader(AssetKey key){
        synchronized (loaders){
            ImplThreadLocal local = loaders.get(key.getExtension());
            if (local != null){
                AssetLoader loader = (AssetLoader) local.get();
                return loader;
            }
            return null;
        }
    }

    public void addLoader(final Class<?> loaderType, String ... extensions){
        ImplThreadLocal local = new ImplThreadLocal(loaderType);
        for (String extension : extensions){
            extension = extension.toLowerCase();
            synchronized (loaders){
                loaders.put(extension, local);
            }
        }
    }

    public void addLocator(final Class<?> locatorType, String rootPath){
        ImplThreadLocal local = new ImplThreadLocal(locatorType, rootPath);
        synchronized (genericLocators){
            genericLocators.add(local);
        }
    }

    public void removeLocator(final Class<?> locatorType, String rootPath){
        synchronized (genericLocators){
            Iterator<ImplThreadLocal> it = genericLocators.iterator();
            while (it.hasNext()){
                ImplThreadLocal locator = it.next();
                if (locator.getPath().equals(rootPath) &&
                    locator.getTypeClass().equals(locatorType)){
                    it.remove();
                }
            }
        }
    }

}
