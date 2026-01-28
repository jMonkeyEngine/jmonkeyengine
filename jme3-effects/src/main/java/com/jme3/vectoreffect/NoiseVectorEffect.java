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

import com.jme3.math.Vector4f;
import com.jme3.math.FastNoiseLite;
import com.jme3.math.FastNoiseLite.NoiseType;

/**
 *
 * @author yaRnMcDonuts
 */
public class NoiseVectorEffect extends VectorEffect {
    
    private FastNoiseLite noiseGenerator;        

    private VectorGroup noiseMagnitudes;    
    private VectorGroup originalVectorValues;
    
    private final Vector4f tempNoiseVariationVec = new Vector4f();
    private final Vector4f tempOriginalVec = new Vector4f();
    
    public float speed = 1;
    private float timeAccrued = 0;
    
    public NoiseVectorEffect(VectorGroup vectorObject, VectorGroup noiseMagnitude) {
        this(vectorObject, noiseMagnitude, NoiseType.OpenSimplex2, 0.5f);       
    }
    
    public NoiseVectorEffect(VectorGroup vectorObject, VectorGroup noiseMagnitude, NoiseType noiseType,
            float frequency) {
        super(vectorObject);
        
        this.noiseMagnitudes = noiseMagnitude;
        noiseGenerator = new FastNoiseLite();
        noiseGenerator.SetFrequency(frequency);
        noiseGenerator.SetNoiseType(noiseType);       
      
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        
        if(originalVectorValues == null){
            originalVectorValues = vectorsToModify.clone();
        }
        
        timeAccrued += tpf;             
        float noiseReturnVal = noiseGenerator.GetNoise(timeAccrued * speed, 12.671f + timeAccrued * speed * 0.92173f, 19.54f + timeAccrued * speed * 0.68913f);
        
        for(int v = 0; v < vectorsToModify.getSize(); v++){
            int magnitudeIndex = Math.min(v, noiseMagnitudes.getSize() - 1); //allows multiple vectors to share the same magnitude if desired
            noiseMagnitudes.getAsVector4(magnitudeIndex, tempNoiseVariationVec);

            tempNoiseVariationVec.multLocal(noiseReturnVal);        
            originalVectorValues.getAsVector4(v, tempOriginalVec);
        
            vectorsToModify.updateVectorObject(tempOriginalVec.add(tempNoiseVariationVec), v);
        }             
    }    
    
    public FastNoiseLite getNoiseGenerator() {
        return noiseGenerator;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
    
    @Override
    public void reset() {
        super.reset(); 
        originalVectorValues = null;       
    }
}
