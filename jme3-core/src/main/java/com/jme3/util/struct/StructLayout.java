package com.jme3.util.struct;

import com.jme3.math.*;
import com.jme3.vulkan.buffers.BufferMapping;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/**
 *
 * @author codex
 */
public class StructLayout {

    private final Map<Class, StructInfo> structs = new HashMap<>();
    private final Map<Class, FieldDesc> fields = new HashMap<>();
    private final Map<Class, Class> typeMappings = new HashMap<>();

    public static final StructLayout std140 = new StructLayout(false);
    public static final StructLayout std430 = new StructLayout(true);

    private static void describeFieldAll(FieldDesc description, Class... types) {
        std140.describeField(description, types);
        std430.describeField(description, types);
    }

    private static void mapTypeAll(Class src, Class dst) {
        std140.mapType(src, dst);
        std430.mapType(src, dst);
    }

    static {

        describeFieldAll(new StructDesc(), Struct.class);

        describeFieldAll(new ConsistentDesc<Boolean>(Float.BYTES, Float.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Boolean value) {
                buffer.putInt(value ? 1 : 0);
            }
            @Override
            protected Boolean read(ByteBuffer buffer, Boolean store) {
                return buffer.getInt() != 0;
            }
        }, boolean.class, Boolean.class);
        describeFieldAll(new ConsistentDesc<Byte>(Integer.BYTES, Integer.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Byte value) {
                buffer.putInt(value);
            }
            @Override
            protected Byte read(ByteBuffer buffer, Byte store) {
                return (byte)buffer.getInt();
            }
        }, byte.class, Byte.class);
        describeFieldAll(new ConsistentDesc<Short>(Integer.BYTES, Integer.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Short value) {
                buffer.putInt(value);
            }
            @Override
            protected Short read(ByteBuffer buffer, Short store) {
                return (short)buffer.getInt();
            }
        }, short.class, Short.class);
        describeFieldAll(new ConsistentDesc<Integer>(Integer.BYTES, Integer.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Integer value) {
                buffer.putInt(value);
            }
            @Override
            protected Integer read(ByteBuffer buffer, Integer store) {
                return buffer.getInt();
            }
        }, int.class, Integer.class);
        describeFieldAll(new ConsistentDesc<Float>(Float.BYTES, Float.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Float value) {
                buffer.putFloat(value);
            }
            @Override
            protected Float read(ByteBuffer buffer, Float store) {
                return buffer.getFloat();
            }
        }, float.class, Float.class);
        describeFieldAll(new ConsistentDesc<Double>(Float.BYTES, Float.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Double value) {
                buffer.putFloat(value.floatValue());
            }
            @Override
            protected Double read(ByteBuffer buffer, Double store) {
                return (double)buffer.getFloat();
            }
        }, double.class, Double.class);
        describeFieldAll(new ConsistentDesc<Long>(Long.BYTES, Long.BYTES) {
            @Override
            protected void write(ByteBuffer buffer, Long value) {
                buffer.putInt(value.intValue());
            }
            @Override
            protected Long read(ByteBuffer buffer, Long store) {
                return (long)buffer.getInt();
            }
        }, long.class, Long.class);
        describeFieldAll(new ConsistentDesc<Vector2f>(Float.BYTES * 2, Float.BYTES * 2) {
            @Override
            protected void write(ByteBuffer buffer, Vector2f value) {
                buffer.putFloat(value.x).putFloat(value.y);
            }
            @Override
            protected Vector2f read(ByteBuffer buffer, Vector2f store) {
                return Vector2f.storage(store).set(buffer.getFloat(), buffer.getFloat());
            }
        }, Vector2f.class);
        describeFieldAll(new ConsistentDesc<Vector3f>(Float.BYTES * 3, Float.BYTES * 4) {
            @Override
            protected void write(ByteBuffer buffer, Vector3f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z);
                buffer.position(buffer.position() + Float.BYTES);
            }
            @Override
            protected Vector3f read(ByteBuffer buffer, Vector3f store) {
                store = Vector3f.storage(store).set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                buffer.position(buffer.position() + Float.BYTES);
                return store;
            }
        }, Vector3f.class);
        describeFieldAll(new ConsistentDesc<Vector4f>(Float.BYTES * 4, Float.BYTES * 4) {
            @Override
            protected void write(ByteBuffer buffer, Vector4f value) {
                buffer.putFloat(value.x).putFloat(value.y).putFloat(value.z).putFloat(value.w);
            }
            @Override
            protected Vector4f read(ByteBuffer buffer, Vector4f store) {
                return Vector4f.storage(store).set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, Vector4f.class);
        describeFieldAll(new ConsistentDesc<ColorRGBA>(Float.BYTES * 4, Float.BYTES * 4) {
            @Override
            protected void write(ByteBuffer buffer, ColorRGBA value) {
                buffer.putFloat(value.r).putFloat(value.g).putFloat(value.b).putFloat(value.a);
            }
            @Override
            protected ColorRGBA read(ByteBuffer buffer, ColorRGBA store) {
                return ColorRGBA.storage(store).set(buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
            }
        }, ColorRGBA.class);
        describeFieldAll(new ConsistentDesc<Matrix3f>(Float.BYTES * 4 * 3, Float.BYTES * 4) {
            @Override
            protected void write(ByteBuffer buffer, Matrix3f value) {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        buffer.putFloat(value.get(i, j));
                    }
                    buffer.position(buffer.position() + Float.BYTES);
                }
            }
            @Override
            protected Matrix3f read(ByteBuffer buffer, Matrix3f store) {
                store = Matrix3f.storage(store);
                for (int i = 0; i < 3; i++) {
                    store.setColumn(i, buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                    buffer.position(buffer.position() + Float.BYTES);
                }
                return store;
            }
        }, Matrix3f.class);
        describeFieldAll(new ConsistentDesc<Matrix4f>(Float.BYTES * 4 * 4, Float.BYTES * 4) {
            @Override
            protected void write(ByteBuffer buffer, Matrix4f value) {
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        buffer.putFloat(value.get(i, j));
                    }
                }
            }
            @Override
            protected Matrix4f read(ByteBuffer buffer, Matrix4f store) {
                store = Matrix4f.storage(store);
                for (int i = 0; i < 4; i++) {
                    store.setColumn(i, buffer.getFloat(), buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
                }
                return store;
            }
        }, Matrix4f.class);

        describeFieldAll(new PrimitiveArrayDesc<boolean[]>() {
            @Override
            protected void setElement(boolean[] array, int index, Object element) {
                array[index] = (boolean)element;
            }
            @Override
            protected Object getElement(boolean[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(boolean[] array) {
                return array.length;
            }
        }, boolean[].class);
        describeFieldAll(new PrimitiveArrayDesc<short[]>() {
            @Override
            protected void setElement(short[] array, int index, Object element) {
                array[index] = (short)element;
            }
            @Override
            protected Object getElement(short[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(short[] array) {
                return array.length;
            }
        }, short[].class);
        describeFieldAll(new PrimitiveArrayDesc<int[]>() {
            @Override
            protected void setElement(int[] array, int index, Object element) {
                array[index] = (int)element;
            }
            @Override
            protected Object getElement(int[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(int[] array) {
                return array.length;
            }
        }, int[].class);
        describeFieldAll(new PrimitiveArrayDesc<float[]>() {
            @Override
            protected void setElement(float[] array, int index, Object element) {
                array[index] = (float)element;
            }
            @Override
            protected Object getElement(float[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(float[] array) {
                return array.length;
            }
        }, float[].class);
        describeFieldAll(new PrimitiveArrayDesc<double[]>() {
            @Override
            protected void setElement(double[] array, int index, Object element) {
                array[index] = (double)element;
            }
            @Override
            protected Object getElement(double[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(double[] array) {
                return array.length;
            }
        }, double[].class);
        describeFieldAll(new PrimitiveArrayDesc<long[]>() {
            @Override
            protected void setElement(long[] array, int index, Object element) {
                array[index] = (long)element;
            }
            @Override
            protected Object getElement(long[] array, int index) {
                return array[index];
            }
            @Override
            protected int getLength(long[] array) {
                return array.length;
            }
        }, long[].class);
        describeFieldAll(new ObjectArrayDesc(), Object[].class);
        describeFieldAll(new ObjectListDesc(), List.class);

        mapTypeAll(ArrayList.class, List.class);
        mapTypeAll(LinkedList.class, List.class);

    }

    private final boolean packedArrays;

    public StructLayout(boolean packedArrays) {
        this.packedArrays = packedArrays;
    }

    public void describeField(FieldDesc description, Class... types) {
        for (Class t : types) {
            fields.put(t, description);
        }
    }

    public void mapType(Class src, Class dst) {
        if (!dst.isAssignableFrom(src)) {
            throw new ClassCastException(src + " cannot be cast to " + dst);
        }
        typeMappings.put(src, dst);
    }

    private StructInfo getStructInfo(Class type) {
        return structs.computeIfAbsent(type, k -> new StructInfo(this, k));
    }

    private <T> FieldDesc<T> getFieldDescription(Class origin) {
        if (origin == null) return null;
        FieldDesc d = fields.get(typeMappings.getOrDefault(origin, origin));
        if (d != null) return d;
        else return getFieldDescription(origin.getSuperclass());
    }

    public void updateBuffer(Struct src, BufferMapping dst) {
        updateBuffer(src, dst, false);
    }

    public void updateBuffer(Struct src, BufferMapping dst, boolean force) {
        try {
            int p = dst.getBytes().position();
            getFieldDescription(src.getClass()).write(this, dst, src, force);
            dst.getBytes().position(p);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to update buffer.", e);
        }
    }

    public void updateStruct(ByteBuffer src, Struct dst) {
        try {
            getFieldDescription(dst.getClass()).read(this, src, dst);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to update struct.", e);
        }
    }

    public int structSize(Struct struct) {
        try {
            return getFieldDescription(struct.getClass()).size(this, struct, 0);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get struct size.", e);
        }
    }

    public boolean isPackedArrays() {
        return packedArrays;
    }

    private static ByteBuffer alignBuffer(ByteBuffer buffer, int alignment) {
        return buffer.position(FastMath.toMultipleOf(buffer.position(), alignment));
    }

    public static class StructInfo {

        private final List<Field> fields = new ArrayList<>();
        private final int alignment;

        public StructInfo(StructLayout layout, Class type) {
            if (Struct.class.isAssignableFrom(type.getSuperclass())) {
                fields.addAll(layout.getStructInfo(type.getSuperclass()).fields);
            }
            List<Field> localFields = new ArrayList<>();
            int alignment = Float.BYTES * 4;
            for (Field f : type.getDeclaredFields()) {
                if (f.getAnnotation(Member.class) != null) {
                    localFields.add(f);
                    alignment = Math.max(alignment, layout.getFieldDescription(f.getType()).getAlignment(layout, f.getType()));
                }
            }
            localFields.sort(Comparator.comparingInt(o -> o.getAnnotation(Member.class).value()));
            fields.addAll(localFields);
            this.alignment = alignment;
        }

        public List<Field> getFields() {
            return fields;
        }

        public int getAlignment() {
            return alignment;
        }

    }

    public interface FieldDesc <T> {

        int size(StructLayout layout, T value, int size) throws IllegalAccessException;

        int getAlignment(StructLayout layout, Class type);

        void write(StructLayout layout, BufferMapping mapping, T value, boolean force) throws IllegalAccessException;

        T read(StructLayout layout, ByteBuffer buffer, T value) throws IllegalAccessException;

    }

    public static class StructDesc implements FieldDesc<Struct> {

        @Override
        public int size(StructLayout layout, Struct value, int size) throws IllegalAccessException {
            StructInfo info = layout.getStructInfo(value.getClass());
            size = FastMath.toMultipleOf(size, info.getAlignment());
            for (Field f : info.getFields()) {
                size = layout.getFieldDescription(f.getType()).size(layout, f.get(value), size);
            }
            return FastMath.toMultipleOf(size, info.getAlignment());
        }

        @Override
        public int getAlignment(StructLayout layout, Class type) {
            return layout.getStructInfo(type).getAlignment();
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, Struct value, boolean force) throws IllegalAccessException {
            StructInfo info = layout.getStructInfo(value.getClass());
            alignBuffer(mapping.getBytes(), info.getAlignment());
            for (Field f : info.getFields()) {
                layout.getFieldDescription(f.getType()).write(layout, mapping, f.get(value), force);
            }
            alignBuffer(mapping.getBytes(), info.getAlignment());
        }

        @Override
        public Struct read(StructLayout layout, ByteBuffer buffer, Struct value) throws IllegalAccessException {
            StructInfo info = layout.getStructInfo(value.getClass());
            alignBuffer(buffer, info.getAlignment());
            for (Field f : info.getFields()) {
                Object v = f.get(value);
                Object n = layout.getFieldDescription(f.getType()).read(layout, buffer, v);
                if (v != n) {
                    f.set(value, n);
                }
            }
            alignBuffer(buffer, info.getAlignment());
            return value;
        }

    }

    public static abstract class ConsistentDesc<T> implements FieldDesc<T> {

        private final int size, alignment;

        public ConsistentDesc(int size, int alignment) {
            this.size = size;
            this.alignment = alignment;
        }

        @Override
        public int size(StructLayout layout, T value, int size) throws IllegalAccessException {
            return FastMath.toMultipleOf(size, alignment) + this.size;
        }

        @Override
        public int getAlignment(StructLayout layout, Class type) {
            return alignment;
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, T value, boolean force) throws IllegalAccessException {
            ByteBuffer bytes = alignBuffer(mapping.getBytes(), alignment);
            if (!force) {
                int p = bytes.position();
                T stored = read(bytes, null);
                if (Objects.equals(value, stored)) {
                    return;
                }
                bytes.position(p);
                mapping.push(p, size);
            }
            write(bytes, value);
        }

        @Override
        public T read(StructLayout layout, ByteBuffer buffer, T value) throws IllegalAccessException {
            return read(alignBuffer(buffer, alignment), value);
        }

        protected abstract void write(ByteBuffer buffer, T value);

        protected abstract T read(ByteBuffer buffer, T store);

    }

    public static class ObjectArrayDesc implements FieldDesc<Object[]> {

        @Override
        public int size(StructLayout layout, Object[] value, int size) throws IllegalAccessException {
            int a = getAlignment(layout, value.getClass());
            FieldDesc d = layout.getFieldDescription(value.getClass().getComponentType());
            return FastMath.toMultipleOf(d.size(layout, value[0], FastMath.toMultipleOf(size, a)), a) * value.length;
        }

        @Override
        public int getAlignment(StructLayout layout, Class type) {
            type = type.getComponentType();
            int a = layout.getFieldDescription(type).getAlignment(layout, type);
            return layout.isPackedArrays() ? a : Math.max(a, Float.BYTES * 4);
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, Object[] value, boolean force) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(mapping.getBytes(), alignment);
            for (Object v : value) {
                layout.getFieldDescription(v.getClass()).write(layout, mapping, v, force);
                alignBuffer(mapping.getBytes(), alignment);
            }
        }

        @Override
        public Object[] read(StructLayout layout, ByteBuffer buffer, Object[] value) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(buffer, alignment);
            for (int i = 0; i < value.length; i++) {
                FieldDesc d = layout.getFieldDescription(value[i].getClass());
                value[i] = d.read(layout, buffer, value[i]);
                alignBuffer(buffer, alignment);
            }
            return value;
        }

    }

    public static abstract class PrimitiveArrayDesc <T> implements FieldDesc<T> {

        @Override
        public int size(StructLayout layout, T value, int size) throws IllegalAccessException {
            int a = getAlignment(layout, value.getClass());
            FieldDesc d = layout.getFieldDescription(value.getClass().getComponentType());
            return FastMath.toMultipleOf(d.size(layout, getElement(value, 0), FastMath.toMultipleOf(size, a)), a) * getLength(value);
        }

        @Override
        public int getAlignment(StructLayout layout, Class type) {
            type = type.getComponentType();
            int a = layout.getFieldDescription(type).getAlignment(layout, type);
            return layout.isPackedArrays() ? a : Math.max(a, Float.BYTES * 4);
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, T value, boolean force) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(mapping.getBytes(), alignment);
            for (int i = 0; i < getLength(value); i++) {
                Object v = getElement(value, i);
                layout.getFieldDescription(v.getClass()).write(layout, mapping, v, force);
                alignBuffer(mapping.getBytes(), alignment);
            }
        }

        @Override
        public T read(StructLayout layout, ByteBuffer buffer, T value) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(buffer, alignment);
            for (int i = 0; i < getLength(value); i++) {
                FieldDesc d = layout.getFieldDescription(getElement(value, i).getClass());
                setElement(value, i, d.read(layout, buffer, getElement(value, i)));
                alignBuffer(buffer, alignment);
            }
            return value;
        }

        protected abstract void setElement(T array, int index, Object element);

        protected abstract Object getElement(T array, int index);

        protected abstract int getLength(T array);

    }

    public static class ObjectListDesc implements FieldDesc<List> {

        @Override
        public int size(StructLayout layout, List value, int size) throws IllegalAccessException {
            int a = getAlignment(layout, value.getClass());
            FieldDesc d = layout.getFieldDescription(value.getClass().getComponentType());
            return FastMath.toMultipleOf(d.size(layout, value.getFirst(), FastMath.toMultipleOf(size, a)), a) * value.size();
        }

        @Override
        public int getAlignment(StructLayout layout, Class type) {
            type = type.getTypeParameters()[0].getClass();
            int a = layout.getFieldDescription(type).getAlignment(layout, type);
            return layout.isPackedArrays() ? a : Math.max(a, Float.BYTES * 4);
        }

        @Override
        public void write(StructLayout layout, BufferMapping mapping, List value, boolean force) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(mapping.getBytes(), alignment);
            for (Object v : value) {
                layout.getFieldDescription(v.getClass()).write(layout, mapping, v, force);
                alignBuffer(mapping.getBytes(), alignment);
            }
        }

        @Override
        public List read(StructLayout layout, ByteBuffer buffer, List value) throws IllegalAccessException {
            int alignment = getAlignment(layout, value.getClass());
            alignBuffer(buffer, alignment);
            for (ListIterator it = value.listIterator(); it.hasNext();) {
                Object v = it.next();
                FieldDesc d = layout.getFieldDescription(v.getClass());
                Object n = d.read(layout, buffer, v);
                if (n != v) {
                    it.set(n);
                }
                alignBuffer(buffer, alignment);
            }
            return value;
        }

    }

}
