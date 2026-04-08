package com.jme3.util.struct;

import com.jme3.math.*;
import com.jme3.vulkan.buffers.BufferMapping;

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

    private static void addToAllLayouts(FieldDesc desc, Class... types) {
        std140.addFieldDescription(desc, types);
        std430.addFieldDescription(desc, types);
        packed.addFieldDescription(desc, types);
    }

    private static void addToLayouts(FieldDesc desc, StructLayout[] layouts, Class... types) {
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
        addToLayouts(new StructDesc(), stds, Struct.class);
        addToAllLayouts(new ObjectDesc<Boolean>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Boolean value) {
                mapping.getBytes().putInt(position, value ? Integer.MAX_VALUE : 0);
                mapping.stage(position, Integer.BYTES);
            }
            @Override
            public Boolean read(StructLayout layout, BufferMapping mapping, int position, Boolean store) {
                return mapping.getBytes().get(position) != 0;
            }
        }, boolean.class, Boolean.class);
        addToAllLayouts(new ObjectDesc<Integer>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Integer value) {
                mapping.getBytes().putInt(position, value);
                mapping.stage(position, Integer.BYTES);
            }
            @Override
            public Integer read(StructLayout layout, BufferMapping mapping, int position, Integer store) {
                return mapping.getBytes().getInt(position);
            }
        }, int.class, Integer.class);
        addToAllLayouts(new ObjectDesc<Float>(Float.BYTES, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Float value) {
                mapping.getBytes().putFloat(position, value);
                mapping.stage(position, Float.BYTES);
            }
            @Override
            public Float read(StructLayout layout, BufferMapping mapping, int position, Float store) {
                return mapping.getBytes().getFloat(position);
            }
        }, float.class, Float.class);
        addToLayouts(new ObjectDesc<Vector2f>(VEC2_WIDTH, VEC2_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector2f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y);
                mapping.stage(position, VEC2_WIDTH);
            }
            @Override
            public Vector2f read(StructLayout layout, BufferMapping mapping, int position, Vector2f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat());
            }
        }, stds, Vector2f.class);
        addToLayouts(new ObjectDesc<Vector3f>(Float.BYTES * 3, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector3f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y).putFloat(value.z);
                mapping.stage(position, VEC3_WIDTH);
            }
            @Override
            public Vector3f read(StructLayout layout, BufferMapping mapping, int position, Vector3f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, stds, Vector3f.class);
        addToLayouts(new ObjectDesc<Vector4f>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector4f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
                mapping.stage(position, VEC4_WIDTH);
            }
            @Override
            public Vector4f read(StructLayout layout, BufferMapping mapping, int position, Vector4f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, stds, Vector4f.class);
        addToLayouts(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, ColorRGBA value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.r).putFloat(value.g).putFloat(value.b).putFloat(value.a);
                mapping.stage(position, VEC4_WIDTH);
            }
            @Override
            public ColorRGBA read(StructLayout layout, BufferMapping mapping, int position, ColorRGBA store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, stds, ColorRGBA.class);
        addToLayouts(new ObjectDesc<Matrix3f>(Float.BYTES * 12, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Matrix3f value) {
                value.writeToStdBuffer(mapping.getBytes().position(position));
                mapping.stage(position, Float.BYTES * 12);
            }
            @Override
            public Matrix3f read(StructLayout layout, BufferMapping mapping, int position, Matrix3f store) {
                return store.readFromStdBuffer(mapping.getBytes().position(position));
            }
        }, stds, Matrix3f.class);
        addToLayouts(new ObjectDesc<Matrix4f>(Float.BYTES << 4, VEC4_WIDTH) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Matrix4f value) {
                value.writeToBuffer(mapping.getBytes().position(position));
                mapping.stage(position, Float.BYTES << 4);
            }
            @Override
            public Matrix4f read(StructLayout layout, BufferMapping mapping, int position, Matrix4f store) {
                return store.readFromBuffer(mapping.getBytes().position(position));
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
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector2f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y);
                mapping.stage(position, VEC2_WIDTH);
            }
            @Override
            public Vector2f read(StructLayout layout, BufferMapping mapping, int position, Vector2f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat());
            }
        }, Vector2f.class);
        packed.addFieldDescription(new ObjectDesc<Vector3f>(VEC3_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector3f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y).putFloat(value.z);
                mapping.stage(position, VEC3_WIDTH);
            }
            @Override
            public Vector3f read(StructLayout layout, BufferMapping mapping, int position, Vector3f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, Vector3f.class);
        packed.addFieldDescription(new ObjectDesc<Vector4f>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Vector4f value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
                mapping.stage(position, VEC4_WIDTH);
            }
            @Override
            public Vector4f read(StructLayout layout, BufferMapping mapping, int position, Vector4f store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, Vector4f.class);
        packed.addFieldDescription(new ObjectDesc<ColorRGBA>(VEC4_WIDTH, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, ColorRGBA value) {
                ByteBuffer buf = mapping.getBytes();
                buf.position(position).putFloat(value.r).putFloat(value.g).putFloat(value.b).putFloat(value.a);
                mapping.stage(position, VEC4_WIDTH);
            }
            @Override
            public ColorRGBA read(StructLayout layout, BufferMapping mapping, int position, ColorRGBA store) {
                ByteBuffer buf = mapping.getBytes().position(position);
                return store.set(buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat());
            }
        }, ColorRGBA.class);
        packed.addFieldDescription(new ObjectDesc<Matrix3f>(VEC3_WIDTH * 3, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Matrix3f value) {
                value.writeToPackedBuffer(mapping.getBytes().position(position));
            }
            @Override
            public Matrix3f read(StructLayout layout, BufferMapping mapping, int position, Matrix3f store) {
                return store.readFromPackedBuffer(mapping.getBytes().position(position));
            }
        }, Matrix3f.class);
        packed.addFieldDescription(new ObjectDesc<Matrix4f>(Float.BYTES << 4, Float.BYTES) {
            @Override
            public void write(StructLayout layout, BufferMapping mapping, int position, Matrix4f value) {
                value.writeToBuffer(mapping.getBytes().position(position));
            }
            @Override
            public Matrix4f read(StructLayout layout, BufferMapping mapping, int position, Matrix4f store) {
                return store.readFromBuffer(mapping.getBytes().position(position));
            }
        }, Matrix4f.class);

    }
    
    private final String identifier;
    private final int minStructAlignment;
    private final Map<Class, FieldDesc> fields = new HashMap<>();
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

    public int getMinStructAlignment() {
        return minStructAlignment;
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
            value.bind(layout);
            return value.getSize();
        }

        @Override
        public int getAlignment(StructLayout layout, Struct value) {
            value.bind(layout);
            return value.getAlignment();
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, int position, Struct value) {
            value.bind(layout, mapping, position);
        }

        @Override
        public Struct read(StructLayout layout, BufferMapping mapping, int position, Struct store) {
            store.bind(layout, mapping, position);
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
        public void write(StructLayout layout, BufferMapping mapping, int position, T value) {
            int l = getLength(value);
            assert l > 0 : "Struct array must contain at least one element.";
            for (int i = 0; i < l; i++) {
                elementDesc.write(layout, mapping, position, getElement(value, i));
                position += stride;
            }
        }

        @Override
        public T read(StructLayout layout, BufferMapping mapping, int position, T store) {
            int l = getLength(store);
            assert l > 0 : "Struct array must contain at least one element.";
            for (int i = 0; i < l; i++) {
                setElement(store, i, elementDesc.read(layout, mapping, position, getElement(store, i)));
                position += stride;
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
        public void write(StructLayout layout, BufferMapping mapping, int position, Object[] value) {
            assert value.length != 0 : "Struct array must contain at least one element.";
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(value[0].getClass());
            int stride = getStride(layout, d, value[0]);
            for (Object v : value) {
                d.write(layout, mapping, position, v);
                position += stride;
            }
        }

        @Override
        public Object[] read(StructLayout layout, BufferMapping mapping, int position, Object[] store) {
            assert store.length != 0 : "Struct array must contain at least one element.";
            FieldDesc d = elementDesc != null ? elementDesc : layout.getFieldDescription(store[0].getClass());
            int stride = getStride(layout, d, store[0]);
            for (int i = 0; i < store.length; i++) {
                store[i] = d.read(layout, mapping, position, store[i]);
                position += stride;
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
        public void write(StructLayout layout, BufferMapping mapping, int position, List value) {
            Object first = value.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (Object v : value) {
                d.write(layout, mapping, position, v);
                position += stride;
            }
        }

        @Override
        public List read(StructLayout layout, BufferMapping mapping, int position, List store) {
            Object first = store.getFirst();
            FieldDesc d = layout.getFieldDescription(first.getClass());
            int stride = getStride(layout, d, first);
            for (ListIterator it = store.listIterator(); it.hasNext();) {
                Object oldVal = it.next();
                Object newVal = d.read(layout, mapping, position, oldVal);
                if (newVal != oldVal) {
                    it.set(newVal);
                }
                position += stride;
            }
            return store;
        }

        private int getStride(StructLayout layout, FieldDesc d, Object v) {
            return FastMath.toMultipleOf(d.getSize(layout, v), Math.max(minAlignment, d.getAlignment(layout, v)));
        }

    }

}
