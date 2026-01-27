package com.jme3.vectoreffect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

public interface VectorSupplier extends Cloneable {
    Vector4f get();
    void set(Vector4f newVal);



    static class Vector4fSupplier implements VectorSupplier {
        private final Vector4f vector;

        Vector4fSupplier(Vector4f vector) {
            this.vector = vector;
        }

        public Vector4f get() {
            return vector;
        }

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

        public Vector4f get() {
            store.set(vector.x, vector.y, vector.z, 0);
            return store;
        }

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

        public Vector4f get() {
            store.set(color.r, color.g, color.b, color.a);
            return store;
        }

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

        public Vector4f get() {
            store.set(vector.x, vector.y, 0, 0);
            return store;
        }

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
