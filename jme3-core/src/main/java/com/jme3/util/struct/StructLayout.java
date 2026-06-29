package com.jme3.util.struct;

import com.jme3.math.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StructLayout {
    
    private static final Logger logger = Logger.getLogger(StructLayout.class.getName());
    
    private static final int VEC2_WIDTH = Float.BYTES * 2;
    private static final int VEC3_WIDTH = Float.BYTES * 3;
    private static final int VEC4_WIDTH = Float.BYTES * 4;

    public static final StructLayout std140 = new StructLayout("std140", VEC4_WIDTH);
    public static final StructLayout std430 = new StructLayout("std430", VEC4_WIDTH);
    public static final StructLayout packed = new StructLayout("packed", Byte.BYTES);
    private static final Map<String, StructLayout> layouts = new HashMap<>();

    private static void addToAllLayouts(FieldDescription desc, Class... types) {
        std140.addFieldDescription(desc, types);
        std430.addFieldDescription(desc, types);
        packed.addFieldDescription(desc, types);
    }

    private static void addToLayouts(FieldDescription desc, StructLayout[] layouts, Class... types) {
        for (StructLayout l : layouts) {
            l.addFieldDescription(desc, types);
        }
    }
    
    public static StructLayout getLayoutByIdentifier(String identifier) {
        return layouts.get(identifier);
    }

    static {
        
        final StructLayout[] stds = {std140, std430};

        // general descriptions
        addToAllLayouts(new ObjectDesc<Boolean>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Boolean value) {
                buffer.putInt(value ? Integer.MAX_VALUE : 0);
            }
            @Override
            public Boolean read(StructLayout layout, ByteBuffer buffer, Boolean store) {
                return buffer.get() != 0;
            }
        }, boolean.class, Boolean.class);
        addToAllLayouts(new ObjectDesc<Integer>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Integer value) {
                buffer.putInt(value);
            }
            @Override
            public Integer read(StructLayout layout, ByteBuffer buffer, Integer store) {
                return buffer.getInt(0);
            }
        }, int.class, Integer.class);
        addToAllLayouts(new ObjectDesc<Float>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Float value) {
                buffer.putFloat(value);
            }
            @Override
            public Float read(StructLayout layout, ByteBuffer buffer, Float store) {
                return buffer.getFloat(0);
            }
        }, float.class, Float.class);
        addToLayouts(new ObjectDesc<Vector2f>(VEC2_WIDTH, VEC2_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector2f value) {
                buffer.putFloat(value.x).putFloat(value.y);
            }
            @Override
            public Vector2f read(StructLayout layout, ByteBuffer buffer, Vector2f store) {
                buffer.position(0);
                return store.set(buffer.getFloat(), buffer.getFloat());
            }
        }, stds, Vector2f.class);
        addToLayouts(new ObjectDesc<Vector3f>(Float.BYTES * 3, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector3f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z);
            }
            @Override
            public Vector3f read(StructLayout layout, ByteBuffer buffer, Vector3f store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, stds, Vector3f.class);
        addToLayouts(new ObjectDesc<Vector4f>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector4f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
            }
            @Override
            public Vector4f read(StructLayout layout, ByteBuffer buffer, Vector4f store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, stds, Vector4f.class);
        addToLayouts(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, ColorRGBA value) {
                buffer.putFloat(value.r).putFloat(value.g).putFloat(value.b).putFloat(value.a);
            }
            @Override
            public ColorRGBA read(StructLayout layout, ByteBuffer buffer, ColorRGBA store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, stds, ColorRGBA.class);
        addToLayouts(new ObjectDesc<Matrix3f>(Float.BYTES * 12, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Matrix3f value) {
                value.writeToStdBuffer(buffer);
            }
            @Override
            public Matrix3f read(StructLayout layout, ByteBuffer buffer, Matrix3f store) {
                return store.readFromStdBuffer(buffer);
            }
        }, stds, Matrix3f.class);
        addToLayouts(new ObjectDesc<Matrix4f>(Float.BYTES << 4, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Matrix4f value) {
                value.writeToBuffer(buffer);
            }
            @Override
            public Matrix4f read(StructLayout layout, ByteBuffer buffer, Matrix4f store) {
                return store.readFromBuffer(buffer);
            }
        }, stds, Matrix4f.class);

        // std140 descriptions
        std140.addFieldDescription(new DirectArrayDesc<boolean[], Boolean>(std140.getFieldDescription(boolean.class), Float.BYTES, VEC4_WIDTH) {
            @Override
            protected void setElement(boolean[] array, int index, Boolean element) {
                array[index] = element;
            }
            @Override
            protected Boolean getElement(boolean[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(boolean[] array) {
                return array.length;
            }
        }, boolean[].class);
        std140.addFieldDescription(new DirectArrayDesc<int[], Integer>(std140.getFieldDescription(int.class), Float.BYTES, VEC4_WIDTH) {
            @Override
            protected void setElement(int[] array, int index, Integer element) {
                array[index] = element;
            }
            @Override
            protected Integer getElement(int[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(int[] array) {
                return array.length;
            }
        }, int[].class);
        std140.addFieldDescription(new DirectArrayDesc<float[], Float>(std140.getFieldDescription(float.class), Float.BYTES, VEC4_WIDTH) {
            @Override
            protected void setElement(float[] array, int index, Float element) {
                array[index] = element;
            }
            @Override
            protected Float getElement(float[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(float[] array) {
                return array.length;
            }
        }, float[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Boolean.class), VEC4_WIDTH), Boolean[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Integer.class), VEC4_WIDTH), Integer[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Float.class), VEC4_WIDTH), Float[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector2f.class), VEC4_WIDTH), Vector2f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector3f.class), VEC4_WIDTH), Vector3f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector4f.class), VEC4_WIDTH), Vector4f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(ColorRGBA.class), VEC4_WIDTH), ColorRGBA[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Matrix3f.class), VEC4_WIDTH), Matrix3f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Matrix4f.class), VEC4_WIDTH), Matrix4f[].class);
        std140.addFieldDescription(new ArrayDesc(null, VEC4_WIDTH), Object[].class);
        std140.addFieldDescription(new ListDesc(VEC4_WIDTH), List.class, ArrayList.class, LinkedList.class);

        // std340 descriptions
        std430.addFieldDescription(new DirectArrayDesc<boolean[], Boolean>(std140.getFieldDescription(boolean.class), Float.BYTES, Float.BYTES) {
            @Override
            protected void setElement(boolean[] array, int index, Boolean element) {
                array[index] = element;
            }
            @Override
            protected Boolean getElement(boolean[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(boolean[] array) {
                return array.length;
            }
        }, boolean[].class);
        std430.addFieldDescription(new DirectArrayDesc<int[], Integer>(std140.getFieldDescription(int.class), Float.BYTES, Float.BYTES) {
            @Override
            protected void setElement(int[] array, int index, Integer element) {
                array[index] = element;
            }
            @Override
            protected Integer getElement(int[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(int[] array) {
                return array.length;
            }
        }, int[].class);
        std430.addFieldDescription(new DirectArrayDesc<float[], Float>(std140.getFieldDescription(float.class), Float.BYTES, Float.BYTES) {
            @Override
            protected void setElement(float[] array, int index, Float element) {
                array[index] = element;
            }
            @Override
            protected Float getElement(float[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(float[] array) {
                return array.length;
            }
        }, float[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Boolean.class), Float.BYTES), Boolean[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Integer.class), Float.BYTES), Integer[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Float.class), Float.BYTES), Float[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector2f.class), VEC2_WIDTH), Vector2f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector3f.class), VEC4_WIDTH), Vector3f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector4f.class), VEC4_WIDTH), Vector4f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(ColorRGBA.class), VEC4_WIDTH), ColorRGBA[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Matrix3f.class), VEC4_WIDTH), Matrix3f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Matrix4f.class), VEC4_WIDTH), Matrix4f[].class);
        std430.addFieldDescription(new ArrayDesc(null, 0), Object[].class);
        std430.addFieldDescription(new ListDesc(0), List.class, ArrayList.class, LinkedList.class);

        // packed descriptions
        packed.addFieldDescription(new ObjectDesc<Vector2f>(VEC2_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector2f value) {
                buffer.putFloat(value.x).putFloat(value.y);
            }
            @Override
            public Vector2f read(StructLayout layout, ByteBuffer buffer, Vector2f store) {
                return store.set(buffer.getFloat(), buffer.getFloat());
            }
        }, Vector2f.class);
        packed.addFieldDescription(new ObjectDesc<Vector3f>(VEC3_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector3f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z);
            }
            @Override
            public Vector3f read(StructLayout layout, ByteBuffer buffer, Vector3f store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, Vector3f.class);
        packed.addFieldDescription(new ObjectDesc<Vector4f>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Vector4f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
            }
            @Override
            public Vector4f read(StructLayout layout, ByteBuffer buffer, Vector4f store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, Vector4f.class);
        packed.addFieldDescription(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, ColorRGBA value) {
                buffer.putFloat(value.r).putFloat(value.g).putFloat(value.b).putFloat(value.a);
            }
            @Override
            public ColorRGBA read(StructLayout layout, ByteBuffer buffer, ColorRGBA store) {
                return store.set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, ColorRGBA.class);
        packed.addFieldDescription(new ObjectDesc<Matrix3f>(VEC3_WIDTH * 3, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Matrix3f value) {
                value.writeToPackedBuffer(buffer);
            }
            @Override
            public Matrix3f read(StructLayout layout, ByteBuffer buffer, Matrix3f store) {
                return store.readFromPackedBuffer(buffer);
            }
        }, Matrix3f.class);
        packed.addFieldDescription(new ObjectDesc<Matrix4f>(Float.BYTES << 4, Float.BYTES) {
            @Override
            public void write(StructLayout layout, ByteBuffer buffer, Matrix4f value) {
                value.writeToBuffer(buffer);
            }
            @Override
            public Matrix4f read(StructLayout layout, ByteBuffer buffer, Matrix4f store) {
                return store.readFromBuffer(buffer);
            }
        }, Matrix4f.class);

    }
    
    private final String identifier;
    private final int minStructAlignment;
    private final Map<Class, FieldDescription> fields = new HashMap<>();
    private final Map<Class, Class> typeRemappings = new HashMap<>();

    public StructLayout(String identifier, int minStructAlignment) {
        this.identifier = identifier;
        this.minStructAlignment = minStructAlignment;
        if (layouts.put(identifier, this) != null) {
            logger.log(Level.WARNING, "Overwriting layout \"{0}\" can result in incoherent data.", identifier);
        }
    }
    
    public String getIdentifier() {
        return identifier;
    }

    public void addFieldDescription(FieldDescription desc, Class... types) {
        for (Class t : types) {
            if (fields.putIfAbsent(t, desc) != null) {
                throw new IllegalArgumentException(t + " is already described.");
            }
        }
    }

    public void remapType(Class src, Class dst) {
        typeRemappings.put(src, dst);
    }

    public int getMinStructAlignment() {
        return minStructAlignment;
    }

    public FieldDescription getFieldDescription(Class type) {
        return getFieldDescription(type, type);
    }

    protected FieldDescription getFieldDescription(Class origin, Class type) {
        if (type == null) {
            throw new NullPointerException(origin + " is not described.");
        }
        type = typeRemappings.getOrDefault(type, type);
        FieldDescription d = fields.get(type);
        if (d != null) return d;
        else return getFieldDescription(type.getSuperclass());
    }

    public static abstract class ObjectDesc <T> implements FieldDescription<T> {

        private final int size, alignment;

        public ObjectDesc(int size, int alignment) {
            this.size = size;
            this.alignment = alignment;
        }

        @Override
        public int getSize(StructLayout layout, T value) {
            return size;
        }

        @Override
        public int getAlignment(StructLayout layout, T value) {
            return alignment;
        }

    }

    public static abstract class DirectArrayDesc <T, E> implements FieldDescription<T> {

        private final FieldDescription<E> elementDesc;
        private final int alignment, stride;

        public DirectArrayDesc(FieldDescription<E> elementDesc, int size, int alignment) {
            this.elementDesc = elementDesc;
            this.alignment = alignment;
            this.stride = FastMath.toMultipleOf(size, alignment);
        }

        @Override
        public int getSize(StructLayout layout, T value) {
            return stride * getLength(value);
        }

        @Override
        public int getAlignment(StructLayout layout, T value) {
            return alignment;
        }

        @Override
        public void write(StructLayout layout, ByteBuffer buffer, T value) {
            int l = getLength(value);
            assert l > 0 : "Struct array must contain at least one element.";
            for (int i = 0; i < l; i++) {
                elementDesc.write(layout, buffer.slice(), getElement(value, i));
                buffer.position(buffer.position() + stride);
            }
        }

        @Override
        public T read(StructLayout layout, ByteBuffer buffer, T store) {
            int l = getLength(store);
            assert l > 0 : "Struct array must contain at least one element.";
            buffer.position(0);
            for (int i = 0; i < l; i++) {
                setElement(store, i, elementDesc.read(layout, buffer, getElement(store, i)));
                buffer.position(buffer.position() + stride);
            }
            return store;
        }

        protected abstract void setElement(T array, int index, E element);

        protected abstract E getElement(T array, int index);

        protected abstract int getLength(T array);

    }

    public static class ArrayDesc implements FieldDescription<Object[]> {

        private final FieldDescription elementDesc;
        private final int minAlignment;

        public ArrayDesc(FieldDescription elementDesc, int minAlignment) {
            this.elementDesc = elementDesc;
            this.minAlignment = minAlignment;
        }

        @Override
        public int getSize(StructLayout layout, Object[] value) {
            FieldDescription d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            int stride = getStride(layout, d, value[0]);
            return stride * value.length;
        }

        @Override
        public int getAlignment(StructLayout layout, Object[] value) {
            FieldDescription d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            return Math.max(minAlignment, d.getAlignment(layout, value[0]));
        }

        @Override
        public void write(StructLayout layout, ByteBuffer buffer, Object[] value) {
            assert value.length != 0 : "Struct array must contain at least one element.";
            FieldDescription d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            int stride = getStride(layout, d, value[0]);
            for (Object v : value) {
                d.write(layout, buffer, v);
                buffer.position(buffer.position() + stride);
            }
        }

        @Override
        public Object[] read(StructLayout layout, ByteBuffer buffer, Object[] store) {
            assert store.length != 0 : "Struct array must contain at least one element.";
            FieldDescription d = elementDesc != null ? elementDesc : layout.getFieldDescription(store[0].getClass());
            int stride = getStride(layout, d, store[0]);
            for (int i = 0; i < store.length; i++) {
                store[i] = d.read(layout, buffer, store[i]);
                buffer.position(buffer.position() + stride);
            }
            return store;
        }

        private int getStride(StructLayout layout, FieldDescription d, Object v) {
            return FastMath.toMultipleOf(d.getSize(layout, v), Math.max(minAlignment, d.getAlignment(layout, v)));
        }

    }

    public static class ListDesc implements FieldDescription<List> {

        private final int minAlignment;

        public ListDesc(int minAlignment) {
            this.minAlignment = minAlignment;
        }

        @Override
        public int getSize(StructLayout layout, List value) {
            Object first = value.getFirst();
            FieldDescription d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            return stride * value.size();
        }

        @Override
        public int getAlignment(StructLayout layout, List value) {
            Object first = value.getFirst();
            FieldDescription d = layout.getFieldDescription(first.getClass());
            return Math.max(minAlignment, d.getAlignment(layout, first));
        }

        @Override
        public void write(StructLayout layout, ByteBuffer buffer, List value) {
            Object first = value.getFirst();
            FieldDescription d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (Object v : value) {
                d.write(layout, buffer, v);
                buffer.position(buffer.position() + stride);
            }
        }

        @Override
        public List read(StructLayout layout, ByteBuffer buffer, List store) {
            Object first = store.getFirst();
            FieldDescription d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (ListIterator it = store.listIterator(); it.hasNext();) {
                Object oldVal = it.next();
                Object newVal = d.read(layout, buffer.slice(), oldVal);
                buffer.position(buffer.position() + stride);
                if (newVal != oldVal) {
                    it.set(newVal);
                }
            }
            return store;
        }

        private int getStride(StructLayout layout, FieldDescription d, Object v) {
            return FastMath.toMultipleOf(d.getSize(layout, v), Math.max(minAlignment, d.getAlignment(layout, v)));
        }

    }

}
