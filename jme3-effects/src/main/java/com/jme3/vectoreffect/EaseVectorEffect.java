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

import com.jme3.math.EaseFunction;
import com.jme3.math.Easing;
import com.jme3.math.Vector4f;

/**
 *
 * @author yaRnMcDonuts
 */
public final class EaseVectorEffect extends VectorEffect {

    private VectorGroup targetVectors;
    private VectorGroup startVectors;

    private float duration = 0f;
    private float easeTimer = 0f;
    private float delay = 0f;
    private float delayTimer = 0f;

    private EaseFunction easeFunction = Easing.linear; 

    public EaseVectorEffect(VectorGroup vectorToModify) {
        super(vectorToModify);
    }
    
    public EaseVectorEffect(VectorGroup vectorToModify, VectorGroup targetVector, float duration) {
        this(vectorToModify);
        setEaseToValueOverDuration(duration, targetVector);
    }
    
    public EaseVectorEffect(VectorGroup vectorToModify, VectorGroup targetVector, float duration,
            float delay) {
        this(vectorToModify);
        setEaseToValueOverDuration(duration, targetVector);
        setDelayTime(delay);
    }
    
    public EaseVectorEffect(VectorGroup vectorToModify, VectorGroup targetVector, float duration,
            EaseFunction easeFunction) {
        this(vectorToModify, targetVector, duration);
        setEaseFunction(easeFunction);
    }
    
    public EaseVectorEffect(VectorGroup vectorToModify, VectorGroup targetVector, float duration,
            EaseFunction easeFunction, float delay) {
        this(vectorToModify, targetVector, duration);
        setEaseFunction(easeFunction);
        setDelayTime(delay);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        
        if (delayTimer <= delay) {
            delayTimer += tpf;
            return;
        }

        
        if (startVectors == null) {
            startVectors = vectorsToModify.clone();
        }

        easeTimer += tpf;
        float t = Math.min(easeTimer / duration, 1f);

        float easedT = easeFunction.apply(t);

        for(int v = 0; v < vectorsToModify.getSize(); v++){
            
                Vector4f targetVector = targetVectors.getAsVector4(v);
                Vector4f startVector = startVectors.getAsVector4(v);
                Vector4f difference = targetVector.subtract(startVector);
                
                Vector4f currentValue = startVector.add(difference.mult(easedT));
                vectorsToModify.updateVectorObject(currentValue, v);            
        }        

        if (t >= 1f) {
            super.setIsFinished(true);
        }
    }

    public EaseVectorEffect setEaseToValueOverDuration(float dur, VectorGroup targetVector) {
        this.targetVectors = targetVector;
        duration = dur;
        startVectors = null;
        return this;
    }

    public void setDelayTime(float delay) {
        this.delay = delay;
    }

    public void setEaseFunction(EaseFunction func) {
        this.easeFunction = func;
    }
    
    @Override
    public void reset() {
        delayTimer = 0;
        easeTimer = 0;
        startVectors = null;
        super.reset(); 
    }
}