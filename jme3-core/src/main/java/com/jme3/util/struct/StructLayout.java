package com.jme3.util.struct;

import com.jme3.math.*;
import com.jme3.vulkan.buffers.BufferMapping;
import org.lwjgl.system.MemoryUtil;

import java.util.*;

public class StructLayout {

    private static final int FLOAT_WIDTH = Float.BYTES;
    private static final int VEC2_WIDTH = Float.BYTES * 2;
    private static final int VEC3_WIDTH = Float.BYTES * 3;
    private static final int VEC4_WIDTH = Float.BYTES * 4;

    public static final StructLayout std140 = new StructLayout();
    public static final StructLayout std430 = new StructLayout();

    private static void addToAllLayouts(FieldDesc desc, Class... types) {
        std140.addFieldDescription(desc, types);
        std430.addFieldDescription(desc, types);
    }

    static {

        addToAllLayouts(new StructDesc(), Struct.class);
        addToAllLayouts(new ObjectDesc<Boolean>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, long address, Boolean value) {
                MemoryUtil.memPutInt(address, value ? Integer.MAX_VALUE : 0);
            }
            @Override
            public Boolean read(StructLayout layout, long address, Boolean store) {
                return MemoryUtil.memGetByte(address) != 0;
            }
        }, boolean.class, Boolean.class);
        addToAllLayouts(new ObjectDesc<Integer>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, long address, Integer value) {
                MemoryUtil.memPutInt(address, value);
            }
            @Override
            public Integer read(StructLayout layout, long address, Integer store) {
                return MemoryUtil.memGetInt(address);
            }
        }, int.class, Integer.class);
        addToAllLayouts(new ObjectDesc<Float>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, long address, Float value) {
                MemoryUtil.memPutFloat(address, value);
            }
            @Override
            public Float read(StructLayout layout, long address, Float store) {
                return MemoryUtil.memGetFloat(address);
            }
        }, float.class, Float.class);
        addToAllLayouts(new ObjectDesc<Vector2f>(Float.BYTES << 1, Float.BYTES << 1) {
            @Override
            public void write(StructLayout layout, long address, Vector2f value) {
                MemoryUtil.memPutFloat(address, value.x);
                MemoryUtil.memPutFloat(address + FLOAT_WIDTH, value.y);
            }
            @Override
            public Vector2f read(StructLayout layout, long address, Vector2f store) {
                return store.set(MemoryUtil.memGetFloat(address), MemoryUtil.memGetFloat(address + FLOAT_WIDTH));
            }
        }, Vector2f.class);
        addToAllLayouts(new ObjectDesc<Vector3f>(Float.BYTES * 3, Float.BYTES << 2) {
            @Override
            public void write(StructLayout layout, long address, Vector3f value) {
                MemoryUtil.memPutFloat(address, value.x);
                MemoryUtil.memPutFloat(address + FLOAT_WIDTH, value.y);
                MemoryUtil.memPutFloat(address + VEC2_WIDTH, value.z);
            }
            @Override
            public Vector3f read(StructLayout layout, long address, Vector3f store) {
                return store.set(MemoryUtil.memGetFloat(address),
                    MemoryUtil.memGetFloat(address + FLOAT_WIDTH),
                    MemoryUtil.memGetFloat(address + VEC2_WIDTH));
            }
        }, Vector3f.class);
        addToAllLayouts(new ObjectDesc<Vector4f>(Float.BYTES << 2, Float.BYTES << 2) {
            @Override
            public void write(StructLayout layout, long address, Vector4f value) {
                MemoryUtil.memPutFloat(address, value.x);
                MemoryUtil.memPutFloat(address + FLOAT_WIDTH, value.y);
                MemoryUtil.memPutFloat(address + VEC2_WIDTH, value.z);
                MemoryUtil.memPutFloat(address + VEC3_WIDTH, value.w);
            }
            @Override
            public Vector4f read(StructLayout layout, long address, Vector4f store) {
                return store.set(MemoryUtil.memGetFloat(address),
                        MemoryUtil.memGetFloat(address + FLOAT_WIDTH),
                        MemoryUtil.memGetFloat(address + VEC2_WIDTH),
                        MemoryUtil.memGetFloat(address + VEC3_WIDTH));
            }
        }, Vector4f.class);
        addToAllLayouts(new ObjectDesc<ColorRGBA>(Float.BYTES << 2, Float.BYTES << 2) {
            @Override
            public void write(StructLayout layout, long address, ColorRGBA value) {
                MemoryUtil.memPutFloat(address, value.r);
                MemoryUtil.memPutFloat(address + FLOAT_WIDTH, value.g);
                MemoryUtil.memPutFloat(address + VEC2_WIDTH, value.b);
                MemoryUtil.memPutFloat(address + VEC3_WIDTH, value.a);
            }
            @Override
            public ColorRGBA read(StructLayout layout, long address, ColorRGBA store) {
                return store.set(MemoryUtil.memGetFloat(address),
                        MemoryUtil.memGetFloat(address + FLOAT_WIDTH),
                        MemoryUtil.memGetFloat(address + VEC2_WIDTH),
                        MemoryUtil.memGetFloat(address + VEC3_WIDTH));
            }
        }, ColorRGBA.class);
        addToAllLayouts(new ObjectDesc<Matrix3f>(Float.BYTES * 12, Float.BYTES << 2) {
            @Override
            public void write(StructLayout layout, long address, Matrix3f value) {
                value.writeToStdMemory(address);
            }
            @Override
            public Matrix3f read(StructLayout layout, long address, Matrix3f store) {
                return store.readFromStdMemory(address);
            }
        }, Matrix3f.class);
        addToAllLayouts(new ObjectDesc<Matrix4f>(Float.BYTES << 4, Float.BYTES << 2) {
            @Override
            public void write(StructLayout layout, long address, Matrix4f value) {
                value.writeToStdMemory(address);
            }
            @Override
            public Matrix4f read(StructLayout layout, long address, Matrix4f store) {
                return store.readFromStdMemory(address);
            }
        }, Matrix4f.class);

        std140.addFieldDescription(new DirectArrayDesc<boolean[], Boolean>(std140.getFieldDescription(boolean.class), Float.BYTES, Float.BYTES << 2) {
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
        std140.addFieldDescription(new DirectArrayDesc<int[], Integer>(std140.getFieldDescription(int.class), Float.BYTES, Float.BYTES << 2) {
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
        std140.addFieldDescription(new DirectArrayDesc<float[], Float>(std140.getFieldDescription(float.class), Float.BYTES, Float.BYTES << 2) {
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
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Boolean.class), Float.BYTES << 2), Boolean[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Integer.class), Float.BYTES << 2), Integer[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Float.class), Float.BYTES << 2), Float[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector2f.class), Float.BYTES << 2), Vector2f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector3f.class), Float.BYTES << 2), Vector3f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Vector4f.class), Float.BYTES << 2), Vector4f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(ColorRGBA.class), Float.BYTES << 2), ColorRGBA[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Matrix3f.class), Float.BYTES << 2), Matrix3f[].class);
        std140.addFieldDescription(new ArrayDesc(std140.getFieldDescription(Matrix4f.class), Float.BYTES << 2), Matrix4f[].class);
        std140.addFieldDescription(new ArrayDesc(null, Float.BYTES << 2), Object[].class);
        std140.addFieldDescription(new ListDesc(Float.BYTES << 2), List.class, ArrayList.class, LinkedList.class);

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
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector2f.class), Float.BYTES << 1), Vector2f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector3f.class), Float.BYTES << 2), Vector3f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Vector4f.class), Float.BYTES << 2), Vector4f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(ColorRGBA.class), Float.BYTES << 2), ColorRGBA[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Matrix3f.class), Float.BYTES << 2), Matrix3f[].class);
        std430.addFieldDescription(new ArrayDesc(std430.getFieldDescription(Matrix4f.class), Float.BYTES << 2), Matrix4f[].class);
        std430.addFieldDescription(new ArrayDesc(null, 0), Object[].class);
        std430.addFieldDescription(new ListDesc(0), List.class, ArrayList.class, LinkedList.class);

    }

    private final Map<Class, FieldDesc> fields = new HashMap<>();
    private final Map<Class, Class> typeRemappings = new HashMap<>();

    public void addFieldDescription(FieldDesc desc, Class... types) {
        for (Class t : types) {
            if (fields.putIfAbsent(t, desc) != null) {
                throw new IllegalArgumentException(t + " is already described.");
            }
        }
    }

    public void remapType(Class src, Class dst) {
        typeRemappings.put(src, dst);
    }

    public FieldDesc getFieldDescription(Class type) {
        return getFieldDescription(type, type);
    }

    protected FieldDesc getFieldDescription(Class origin, Class type) {
        if (type == null) {
            throw new NullPointerException(origin + " is not described.");
        }
        type = typeRemappings.getOrDefault(type, type);
        FieldDesc d = fields.get(type);
        if (d != null) return d;
        else return getFieldDescription(type.getSuperclass());
    }

    public static class StructDesc implements FieldDesc<Struct> {

        @Override
        public int getSize(StructLayout layout, Struct value) {
            return value.getSize();
        }

        @Override
        public int getAlignment(StructLayout layout, Struct value) {
            return value.getAlignment();
        }

        @Override
        public void write(StructLayout layout, long address, Struct value) {
            value.write(layout, address);
        }

        @Override
        public Struct read(StructLayout layout, long address, Struct store) {
            store.read(layout, address);
            return store;
        }

    }

    public static abstract class ObjectDesc <T> implements FieldDesc<T> {

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

    public static abstract class DirectArrayDesc <T, E> implements FieldDesc<T> {

        private final FieldDesc<E> elementDesc;
        private final int alignment, stride;

        public DirectArrayDesc(FieldDesc<E> elementDesc, int size, int alignment) {
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
        public void write(StructLayout layout, long address, T value) {
            int l = getLength(value);
            assert l > 0 : "Struct array must contain at least one element.";
            for (int i = 0; i < l; i++) {
                elementDesc.write(layout, address, getElement(value, i));
                address += stride;
            }
        }

        @Override
        public T read(StructLayout layout, long address, T store) {
            int l = getLength(store);
            assert l > 0 : "Struct array must contain at least one element.";
            for (int i = 0; i < l; i++) {
                setElement(store, i, elementDesc.read(layout, address, getElement(store, i)));
                address += stride;
            }
            return store;
        }

        protected abstract void setElement(T array, int index, E element);

        protected abstract E getElement(T array, int index);

        protected abstract int getLength(T array);

    }

    public static class ArrayDesc implements FieldDesc<Object[]> {

        private final FieldDesc elementDesc;
        private final int minAlignment;

        public ArrayDesc(FieldDesc elementDesc, int minAlignment) {
            this.elementDesc = elementDesc;
            this.minAlignment = minAlignment;
        }

        @Override
        public int getSize(StructLayout layout, Object[] value) {
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            int stride = getStride(layout, d, value[0]);
            return stride * value.length;
        }

        @Override
        public int getAlignment(StructLayout layout, Object[] value) {
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            return Math.max(minAlignment, d.getAlignment(layout, value[0]));
        }

        @Override
        public void write(StructLayout layout, long address, Object[] value) {
            assert value.length != 0 : "Struct array must contain at least one element.";
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            int stride = getStride(layout, d, value[0]);
            for (Object v : value) {
                d.write(layout, address, v);
                address += stride;
            }
        }

        @Override
        public Object[] read(StructLayout layout, long address, Object[] store) {
            assert store.length != 0 : "Struct array must contain at least one element.";
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(store[0].getClass());
            int stride = getStride(layout, d, store[0]);
            for (int i = 0; i < store.length; i++) {
                store[i] = d.read(layout, address, store[i]);
                address += stride;
            }
            return store;
        }

        private int getStride(StructLayout layout, FieldDesc d, Object v) {
            return FastMath.toMultipleOf(d.getSize(layout, v), Math.max(minAlignment, d.getAlignment(layout, v)));
        }

    }

    public static class ListDesc implements FieldDesc<List> {

        private final int minAlignment;

        public ListDesc(int minAlignment) {
            this.minAlignment = minAlignment;
        }

        @Override
        public int getSize(StructLayout layout, List value) {
            Object first = value.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            return stride * value.size();
        }

        @Override
        public int getAlignment(StructLayout layout, List value) {
            Object first = value.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            return Math.max(minAlignment, d.getAlignment(layout, first));
        }

        @Override
        public void write(StructLayout layout, long address, List value) {
            Object first = value.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (Object v : value) {
                d.write(layout, address, v);
                address += stride;
            }
        }

        @Override
        public List read(StructLayout layout, long address, List store) {
            Object first = store.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (ListIterator it = store.listIterator(); it.hasNext();) {
                Object oldVal = it.next();
                Object newVal = d.read(layout, address, oldVal);
                if (newVal != oldVal) {
                    it.set(newVal);
                }
                address += stride;
            }
            return store;
        }

        private int getStride(StructLayout layout, FieldDesc d, Object v) {
            return FastMath.toMultipleOf(d.getSize(layout, v), Math.max(minAlignment, d.getAlignment(layout, v)));
        }

    }

}
