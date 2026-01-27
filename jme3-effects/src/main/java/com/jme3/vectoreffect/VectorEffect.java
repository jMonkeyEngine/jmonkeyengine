/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

package com.jme3.vectoreffect;

import com.jme3.app.state.AppStateManager;
import java.util.ArrayList;

/**
 * Base class for vector/color effects. * 
 * Supports Vector2f, Vector3f, Vector4f, and ColorRGBA.
 * 
 * @author yaRnMcDonuts
 */

public abstract class VectorEffect {
    
    protected VectorGroup vectorsToModify;    
    private final ArrayList<Runnable> onFinishedCallbacks = new ArrayList<>();    
    protected boolean isFinished = false;       
   
    public VectorEffect(){
        
    }
    
    public VectorEffect(VectorGroup vectorsToModify) {
        this.vectorsToModify = vectorsToModify;
    }        
    
    public void setIsFinished(boolean isFinished) {        
        this.isFinished = isFinished;
        if (isFinished) {
            for(Runnable r : onFinishedCallbacks) {
                r.run();
            }
            onFinishedCallbacks.clear();
        }
    }
    
    public boolean isFinished() {        
        return isFinished;  
    }    
    

    public void reset() {
        isFinished = false;
    }
    
    
    public void registerRunnableOnFinish(Runnable runnable) {
        onFinishedCallbacks.add(runnable);
    }  
    
    public void update(float tpf){
       
    }    
        
    // convenience registration method so users can avoid repeatedly writing this AppState fetching code
    public void convenienceRegister(AppStateManager stateManager) {
        if(stateManager != null){
            VectorEffectManagerState vectorEffectManagerState = stateManager.getState(VectorEffectManagerState.class);
            if(vectorEffectManagerState != null){
                vectorEffectManagerState.registerVectorEffect(this);
            }
        }
    }


}

