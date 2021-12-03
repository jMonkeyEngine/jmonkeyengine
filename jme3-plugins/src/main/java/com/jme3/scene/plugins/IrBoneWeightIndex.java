/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins;

public class IrBoneWeightIndex implements Cloneable, Comparable<IrBoneWeightIndex> {
    
    int boneIndex;
    float boneWeight;

    public IrBoneWeightIndex(int boneIndex, float boneWeight) {
        this.boneIndex = boneIndex;
        this.boneWeight = boneWeight;
    }

    @Override
    public IrBoneWeightIndex clone() {
        try {
            return (IrBoneWeightIndex)super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.boneIndex;
        hash = 23 * hash + Float.floatToIntBits(this.boneWeight);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IrBoneWeightIndex other = (IrBoneWeightIndex) obj;
        if (this.boneIndex != other.boneIndex) {
            return false;
        }
        if (Float.floatToIntBits(this.boneWeight) != Float.floatToIntBits(other.boneWeight)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(IrBoneWeightIndex o) {
        if (boneWeight < o.boneWeight) {
            return 1;
        } else if (boneWeight > o.boneWeight) {
            return -1;
        } else {
            return 0;
        }
    }
}
