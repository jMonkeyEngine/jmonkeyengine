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

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author yaRnMcDonuts
 */
public class SequencedVectorEffect extends AbstractVectorEffect implements VectorEffect {
    private final ArrayList<VectorEffect> effects = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isRepeatingInfinitely = false;
    private float numTimesToRepeat = -1;
    private float currentCycle = 0;

    public void setLooping(boolean repeat) {        this.isRepeatingInfinitely = repeat;    }
    public void setRepeatNumberOfTimes(float repititionCount){ this.numTimesToRepeat = repititionCount; }
    public void addEffect(VectorEffect effect) {        effects.add(effect);    }

    public SequencedVectorEffect(VectorEffect... effects) {
        super();
        Collections.addAll(this.effects, effects);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (effects.isEmpty()) {
            setIsFinished(true);
            return;
        }

        VectorEffect current = effects.get(currentIndex);
        current.update(tpf);

        if (current.isFinished()) {
            currentIndex++;

            if (currentIndex >= effects.size()) {
                currentCycle++;
                reset();
                
                if (!isRepeatingInfinitely && currentCycle >= numTimesToRepeat) {
                    setIsFinished(true);
                    currentCycle = 0;
                }               
            }
        }
    }    
    
    @Override
    public void reset() {
        super.reset(); 
        isFinished = false;
        currentIndex = 0;
        for (VectorEffect e : effects) {
            e.reset();
        }
    }
}
