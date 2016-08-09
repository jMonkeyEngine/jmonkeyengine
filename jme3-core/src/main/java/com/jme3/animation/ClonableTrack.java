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
package com.jme3.animation;

import com.jme3.scene.Spatial;
import com.jme3.util.clone.JmeCloneable;

/**
 * An interface that allow to clone a Track for a given Spatial.
 * The spatial fed to the method is the Spatial holding the AnimControl controlling the Animation using this track.
 * 
 * Implement this interface only if you make your own Savable Track and that the track has a direct reference to a Spatial in the scene graph.
 * This Spatial is assumed to be a child of the spatial holding the AnimControl.
 *  
 *
 * @author Nehon
 */
public interface ClonableTrack extends Track, JmeCloneable {

    /**
     * Allows to clone the track for a given Spatial.
     * The spatial fed to the method is the Spatial holding the AnimControl controlling the Animation using this track.
     * This method will be called during the loading process of a j3o model by the assetManager.
     * The assetManager keeps the original model in cache and returns a clone of the model.
     * 
     * This method purpose is to find the cloned reference of the original spatial which it refers to in the cloned model.
     * 
     * See EffectTrack for a proper implementation.
     * 
     * @param spatial the spatial holding the AnimControl
     * @return  the cloned Track
     */
    public Track cloneForSpatial(Spatial spatial);
    
    /**
     * Method responsible of cleaning UserData on referenced Spatials when the Track is deleted
     */
    public void cleanUp();
}
