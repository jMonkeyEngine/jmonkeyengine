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

/**
 * <code>AssetEventListener</code> is an interface for listening to various
 * events happening inside {@link AssetManager}. For now, it is possible
 * to receive an event when an asset has been requested
 * (one of the AssetManager.load***() methods were called), or when
 * an asset has been loaded.
 * 
 * @author Kirill Vainer
 */
public interface AssetEventListener {

    /**
     * Called when an asset has been successfully loaded (e.g: loaded from
     * file system and parsed).
     *
     * @param key the AssetKey for the asset loaded.
     */
    public void assetLoaded(AssetKey key);

    /**
     * Called when an asset has been requested (e.g any of the load*** methods
     * in AssetManager are called).
     * In contrast to the assetLoaded() method, this one will be called even
     * if the asset has failed to load, or if it was retrieved from the cache.
     *
     * @param key
     */
    public void assetRequested(AssetKey key);
    
    /**
     * Called when an asset dependency cannot be found for an asset.
     * When an asset is loaded, each of its dependent assets that 
     * have failed to load due to a {@link AssetNotFoundException}, will cause 
     * an invocation of this callback. 
     * 
     * @param parentKey The key of the parent asset that is being loaded
     * from within the user application.
     * @param dependentAssetKey The asset key of the dependent asset that has 
     * failed to load.
     */
    public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey);

}
