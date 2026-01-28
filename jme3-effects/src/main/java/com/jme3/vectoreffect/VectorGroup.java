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

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.ArrayList;

/**
 *
 * @author yaRnMcDonuts
 */
public class VectorGroup implements Cloneable {
    
    protected final ArrayList<VectorSupplier> vectorSupplier = new ArrayList<>();
        
    public int getSize(){
        return vectorSupplier.size();
    }
    
    public VectorGroup(VectorSupplier... vectorSuppliers) {
        for (VectorSupplier vs : vectorSuppliers) {
            vectorSupplier.add(vs);
        }
    }

    public VectorGroup(ColorRGBA... color) {
        for (ColorRGBA c : color) {
            this.vectorSupplier.add(VectorSupplier.of(c));
        }
    }

    public VectorGroup(Vector4f... vectors) {
        for (Vector4f v : vectors) {
            this.vectorSupplier.add(VectorSupplier.of(v));
        }
    }

    public VectorGroup(Vector3f... vectors) {
        for (Vector3f v : vectors) {
            this.vectorSupplier.add(VectorSupplier.of(v));
        }
    }

    public VectorGroup(Vector2f... vectors) {
        for (Vector2f v : vectors) {
            this.vectorSupplier.add(VectorSupplier.of(v));
        }
    }

    protected Vector4f getAsVector4(int index, Vector4f store) {
        return store.set(vectorSupplier.get(index).get());
    }

    protected void updateVectorObject(Vector4f newVal, int index) {
        VectorSupplier store = vectorSupplier.get(index);
        store.set(newVal);
    }

    @Override
    public VectorGroup clone() {
        VectorGroup clonedGroup = new VectorGroup(new VectorSupplier[0]);
        for (VectorSupplier supplier : this.vectorSupplier) {
            clonedGroup.vectorSupplier.add(VectorSupplier.of(supplier.get().clone()));
        }
        return clonedGroup;
    }
}

