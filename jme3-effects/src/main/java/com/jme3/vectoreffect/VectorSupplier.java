/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

public interface VectorSupplier extends Cloneable {
    Vector4f get();
    void set(Vector4f newVal);
    VectorSupplier clone();

    static class Vector4fSupplier implements VectorSupplier {
        private final Vector4f vector;

        Vector4fSupplier(Vector4f vector) {
            this.vector = vector;
        }

        @Override
        public Vector4f get() {
            return vector;
        }

        @Override
        public void set(Vector4f newVal) {
            this.vector.set(newVal);
        }

        @Override
        public Vector4fSupplier clone() {
            return new Vector4fSupplier(vector.clone());
        }      
    }

    static class Vector3fSupplier implements VectorSupplier {
        private final Vector3f vector;
        private final Vector4f store = new Vector4f();
        Vector3fSupplier(Vector3f vector) {
            this.vector = vector;
        }

        @Override
        public Vector4f get() {
            store.set(vector.x, vector.y, vector.z, 0);
            return store;
        }

        @Override
        public void set(Vector4f newVal) {
            this.vector.set(newVal.x, newVal.y, newVal.z);
            get(); //update store
        }

        @Override
        public Vector3fSupplier clone() {
            return new Vector3fSupplier(vector.clone());
        }
    }

    static class ColorRGBASupplier implements VectorSupplier {
        private ColorRGBA color;
        private final Vector4f store = new Vector4f();

        ColorRGBASupplier(ColorRGBA color) {
            this.color = color;
        }

        @Override
        public Vector4f get() {
            store.set(color.r, color.g, color.b, color.a);
            return store;
        }

        @Override
        public void set(Vector4f newVal) {
            this.color.set(newVal.x, newVal.y, newVal.z, newVal.w);
            get(); //update store
        }

        @Override
        public ColorRGBASupplier clone() {
            return new ColorRGBASupplier(color.clone());
        }
    }

    static class Vector2fSupplier implements VectorSupplier {
        private final Vector2f vector;
        private final Vector4f store = new Vector4f();

        Vector2fSupplier(Vector2f vector) {
            this.vector = vector;
        }

        @Override
        public Vector4f get() {
            store.set(vector.x, vector.y, 0, 0);
            return store;
        }

        @Override
        public void set(Vector4f newVal) {
            this.vector.set(newVal.x, newVal.y);
            get(); //update store
        }

        @Override
        public Vector2fSupplier clone() {
            return new Vector2fSupplier(vector.clone());
        }
    }   


    public static VectorSupplier of(Vector4f vector) {
        return new Vector4fSupplier(vector);
    }

    public static VectorSupplier of(Vector3f vector) {
        return new Vector3fSupplier(vector);
    }

    public static VectorSupplier of(Vector2f vector) {
        return new Vector2fSupplier(vector);
    }

    public static VectorSupplier of(ColorRGBA color) {
        return new ColorRGBASupplier(color);
    }


}
