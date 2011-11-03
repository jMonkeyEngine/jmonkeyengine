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
 * Implementing the asset interface allows use of smart asset management.
 * <p>
 * Smart asset management requires cooperation from the {@link AssetKey}. 
 * In particular, the AssetKey should return true in its 
 * {@link AssetKey#useSmartCache() } method. Also smart assets MUST
 * create a clone of the asset and cannot return the same reference,
 * e.g. {@link AssetKey#createClonedInstance(java.lang.Object) createCloneInstance(someAsset)} <code>!= someAsset</code>.
 * <p>
 * If the {@link AssetManager#loadAsset(com.jme3.asset.AssetKey) } method
 * is called twice with the same asset key (equals() wise, not necessarily reference wise)
 * then both assets will have the same asset key set (reference wise) via
 * {@link Asset#setKey(com.jme3.asset.AssetKey) }, then this asset key
 * is used to track all instances of that asset. Once all clones of the asset 
 * are garbage collected, the shared asset key becomes unreachable and at that 
 * point it is removed from the smart asset cache. 
 */
public interface Asset {
    
    /**
     * Set by the {@link AssetManager} to track this asset. 
     * 
     * Only clones of the asset has this set, the original copy that
     * was loaded has this key set to null so that only the clones are tracked
     * for garbage collection. 
     * 
     * @param key The AssetKey to set
     */
    public void setKey(AssetKey key);
    
    /**
     * Returns the asset key that is used to track this asset for garbage
     * collection.
     * 
     * @return the asset key that is used to track this asset for garbage
     * collection.
     */
    public AssetKey getKey();
}
