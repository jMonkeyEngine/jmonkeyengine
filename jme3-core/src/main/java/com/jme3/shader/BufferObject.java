package com.jme3.shader;

import com.jme3.math.*;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Renderer;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import com.jme3.util.SafeArrayList;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The base implementation of BO.
 *
 * @author JavaSaBr
 */
public class BufferObject extends NativeObject {

    private static final Map<Class<?>, VarType> CLASS_TO_VAR_TYPE = new HashMap<>();

    static {
        CLASS_TO_VAR_TYPE.put(Float.class, VarType.Float);
        CLASS_TO_VAR_TYPE.put(Integer.class, VarType.Int);
        CLASS_TO_VAR_TYPE.put(Boolean.class, VarType.Boolean);
        CLASS_TO_VAR_TYPE.put(Vector2f.class, VarType.Vector2);
        CLASS_TO_VAR_TYPE.put(Vector3f.class, VarType.Vector3);
        CLASS_TO_VAR_TYPE.put(ColorRGBA.class, VarType.Vector4);
        CLASS_TO_VAR_TYPE.put(Quaternion.class, VarType.Vector4);
        CLASS_TO_VAR_TYPE.put(Vector4f.class, VarType.Vector4);

        CLASS_TO_VAR_TYPE.put(Vector2f[].class, VarType.Vector2Array);
        CLASS_TO_VAR_TYPE.put(Vector3f[].class, VarType.Vector3Array);
        CLASS_TO_VAR_TYPE.put(Vector4f[].class, VarType.Vector4Array);
        CLASS_TO_VAR_TYPE.put(ColorRGBA[].class, VarType.Vector4Array);
        CLASS_TO_VAR_TYPE.put(Quaternion[].class, VarType.Vector4Array);

        CLASS_TO_VAR_TYPE.put(Matrix3f.class, VarType.Matrix3);
        CLASS_TO_VAR_TYPE.put(Matrix4f.class, VarType.Matrix4);
        CLASS_TO_VAR_TYPE.put(Matrix3f[].class, VarType.Matrix3Array);
        CLASS_TO_VAR_TYPE.put(Matrix4f[].class, VarType.Matrix4Array);
    }

    protected static VarType getVarTypeByValue(final Object value) {

        final VarType varType = CLASS_TO_VAR_TYPE.get(value.getClass());
        if (varType != null) {
            return varType;
        } else if (value instanceof Collection<?> && ((Collection) value).isEmpty()) {
            throw new IllegalArgumentException("Can't calculate a var type for the empty collection value[" + value + "].");
        } else if (value instanceof List<?>) {
            return getVarTypeByValue(((List) value).get(0));
        } else if (value instanceof Collection<?>) {
            return getVarTypeByValue(((Collection) value).iterator().next());
        }

        throw new IllegalArgumentException("Can't calculate a var type for the value " + value);
    }

    public enum Layout {
        std140,
        /** unsupported yet */
        @Deprecated
        std430,
    }

    public enum BufferType {
        ShaderStorageBufferObject(Caps.ShaderStorageBufferObject),
        UniformBufferObject(Caps.UniformBufferObject),
        ;

        private final Caps requiredCaps;

        BufferType(final Caps requiredCaps) {
            this.requiredCaps = requiredCaps;
        }

        /**
         * Get the required caps.
         *
         * @return the required caps.
         */
        public Caps getRequiredCaps() {
            return requiredCaps;
        }
    }

    /**
     * The fields of this BO.
     */
    private final Map<String, BufferObjectField> fields;

    /**
     * The field's array.
     */
    private final SafeArrayList<BufferObjectField> fieldArray;

    /**
     * The buffer's data layout.
     */
    private final Layout layout;

    /**
     * The binding number.
     */
    private final int binding;

    /**
     * The buffer's type.
     */
    private BufferType bufferType;

    /**
     * The previous data buffer.
     */
    private ByteBuffer previousData;

    public BufferObject(final int binding, final Layout layout, final BufferType bufferType) {
        this.handleRef = new Object();
        this.bufferType = bufferType;
        this.binding = binding;
        this.layout = layout;
        this.fields = new HashMap<>();
        this.fieldArray = new SafeArrayList<>(BufferObjectField.class);
    }

    public BufferObject(final int binding, final Layout layout) {
        this(binding, layout, BufferType.UniformBufferObject);
    }

    public BufferObject(final int binding, final BufferType bufferType) {
        this(binding, Layout.std140, bufferType);
    }

    public BufferObject(final BufferType bufferType) {
        this(1, Layout.std140, bufferType);
    }

    public BufferObject(final Layout layout) {
        this(1, layout, BufferType.UniformBufferObject);
    }

    public BufferObject(final int binding) {
        this(binding, Layout.std140, BufferType.UniformBufferObject);
    }

    public BufferObject() {
        this(1, Layout.std140, BufferType.UniformBufferObject);
    }

    private BufferObject(final Void unused, final int id) {
        super(id);
        this.fieldArray = null;
        this.fields = null;
        this.layout = null;
        this.binding = 0;
    }

    /**
     * Declares a filed in this BO.
     *
     * @param name    the field's name.
     * @param varType the field's type.
     */
    public void declareField(final String name, final VarType varType) {

        if (fields.containsKey(name)) {
            throw new IllegalArgumentException("The field " + name + " is already declared.");
        }

        final BufferObjectField field = new BufferObjectField(name, varType);

        fields.put(name, field);
        fieldArray.add(field);
    }

    /**
     * Gets the buffer's type.
     *
     * @return the buffer's type.
     */
    public BufferType getBufferType() {
        return bufferType;
    }

    /**
     * Sets the buffer's type.
     *
     * @param bufferType the buffer's type.
     */
    public void setBufferType(final BufferType bufferType) {

        if (getId() != -1) {
            throw new IllegalStateException("Can't change buffer's type when this buffer is already initialized.");
        }

        this.bufferType = bufferType;
    }

    /**
     * Sets the value to the filed by the field's name.
     *
     * @param name  the field's name.
     * @param value the value.
     */
    public void setFieldValue(final String name, final Object value) {

        BufferObjectField field = fields.get(name);

        if (field == null) {
            declareField(name, getVarTypeByValue(value));
            field = fields.get(name);
        }

        field.setValue(value);
        setUpdateNeeded();
    }

    /**
     * Gets the current value of the field by the name.
     *
     * @param name the field name.
     * @param <T> the value's type.
     * @return the current value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(final String name) {

        final BufferObjectField field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Unknown a field with the name " + name);
        }

        return (T) field.getValue();
    }

    /**
     * Get the binding number.
     *
     * @return the binding number.
     */
    public int getBinding() {
        return binding;
    }

    @Override
    public void resetObject() {
        this.id = -1;
        setUpdateNeeded();
    }

    /**
     * Computes the current binary data of this BO.
     *
     * @param maxSize the max data size.
     * @return the current binary data of this BO.
     */
    public ByteBuffer computeData(final int maxSize) {

        int estimateSize = 0;

        for (final BufferObjectField field : fieldArray) {
            estimateSize += estimateSize(field);
        }

        if(maxSize < estimateSize) {
            throw new IllegalStateException("The estimated size(" + estimateSize + ") of this BO is bigger than " +
                    "maximum available size " + maxSize);
        }

        if (previousData != null) {
            if (previousData.capacity() < estimateSize) {
                BufferUtils.destroyDirectBuffer(previousData);
                previousData = null;
            } else {
                previousData.clear();
            }
        }

        final ByteBuffer data = previousData == null ? BufferUtils.createByteBuffer(estimateSize) : previousData;

        for (final BufferObjectField field : fieldArray) {
            writeField(field, data);
        }

        data.flip();

        this.previousData = data;

        return data;
    }

    /**
     * Estimates size of the field.
     *
     * @param field the field.
     * @return the estimated size.
     */
    protected int estimateSize(final BufferObjectField field) {

        switch (field.getType()) {
            case Float:
            case Int: {
                if (layout == Layout.std140) {
                    return 16;
                }
                return 4;
            }
            case Boolean: {
                if (layout == Layout.std140) {
                    return 16;
                }
                return 1;
            }
            case Vector2: {
                return 4 * 2;
            }
            case Vector3: {
                final int multiplier = layout == Layout.std140 ? 4 : 3;
                return 4 * multiplier;
            }
            case Vector4:
                return 16;
            case IntArray: {
                return estimate((int[]) field.getValue());
            }
            case FloatArray: {
                return estimate((float[]) field.getValue());
            }
            case Vector2Array: {
                return estimateArray(field.getValue(), 8);
            }
            case Vector3Array: {
                final int multiplier = layout == Layout.std140 ? 16 : 12;
                return estimateArray(field.getValue(), multiplier);
            }
            case Vector4Array: {
                return estimateArray(field.getValue(), 16);
            }
            case Matrix3: {
                final int multiplier = layout == Layout.std140 ? 16 : 12;
                return 3 * 3 * multiplier;
            }
            case Matrix4: {
                return 4 * 4 * 4;
            }
            case Matrix3Array: {
                int multiplier = layout == Layout.std140 ? 16 : 12;
                multiplier = 3 * 3 * multiplier;
                return estimateArray(field.getValue(), multiplier);
            }
            case Matrix4Array: {
                final int multiplier = 4 * 4 * 16;
                return estimateArray(field.getValue(), multiplier);
            }
            default: {
                throw new IllegalArgumentException("The type of BO field " + field.getType() + " doesn't support.");
            }
        }
    }

    /**
     * Estimates byte count to present the value on the GPU.
     *
     * @param value      the value.
     * @param multiplier the multiplier.
     * @return the estimated byte count.
     */
    protected int estimateArray(final Object value, final int multiplier) {

        if (value instanceof Object[]) {
            return ((Object[]) value).length * multiplier;
        } else if (value instanceof Collection) {
            return ((Collection) value).size() * multiplier;
        }

        throw new IllegalArgumentException("Unexpected value " + value);
    }

    /**
     * Estimates byte count to present the values on the GPU.
     *
     * @param values the values.
     * @return the estimated byte count.
     */
    protected int estimate(final float[] values) {
        return values.length * 4;
    }

    /**
     * Estimates byte count to present the values on the GPU.
     *
     * @param values the values.
     * @return the estimated byte count.
     */
    protected int estimate(final int[] values) {
        return values.length * 4;
    }

    /**
     * Writes the field to the data buffer.
     *
     * @param field the field.
     * @param data  the data buffer.
     */
    protected void writeField(final BufferObjectField field, final ByteBuffer data) {

        final Object value = field.getValue();

        switch (field.getType()) {
            case Int: {
                data.putInt(((Number) value).intValue());
                if (layout == Layout.std140) {
                    data.putInt(0);
                    data.putLong(0);
                }
                break;
            }
            case Float: {
                data.putFloat(((Number) value).floatValue());
                if (layout == Layout.std140) {
                    data.putInt(0);
                    data.putLong(0);
                }
                break;
            }
            case Boolean:
                data.put((byte) (((Boolean) value) ? 1 : 0));
                if (layout == Layout.std140) {
                    data.putInt(0);
                    data.putLong(0);
                    data.putShort((short) 0);
                    data.put((byte) 0);
                }
                break;
            case Vector2:
                write(data, (Vector2f) value);
                break;
            case Vector3:
                write(data, (Vector3f) value);
                break;
            case Vector4:
                writeVec4(data, value);
                break;
            case IntArray: {
                write(data, (int[]) value);
                break;
            }
            case FloatArray: {
                write(data, (float[]) value);
                break;
            }
            case Vector2Array: {
                writeVec2Array(data, value);
                break;
            }
            case Vector3Array: {
                writeVec3Array(data, value);
                break;
            }
            case Vector4Array: {
                writeVec4Array(data, value);
                break;
            }
            case Matrix3: {
                write(data, (Matrix3f) value);
                break;
            }
            case Matrix4: {
                write(data, (Matrix4f) value);
                break;
            }
            case Matrix3Array: {
                writeMat3Array(data, value);
                break;
            }
            case Matrix4Array: {
                writeMat4Array(data, value);
                break;
            }
            default: {
                throw new IllegalArgumentException("The type of BO field " + field.getType() + " doesn't support.");
            }
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    protected void writeMat3Array(final ByteBuffer data, final Object value) {

        if (value instanceof Matrix3f[]) {

            final Matrix3f[] values = (Matrix3f[]) value;
            for (final Matrix3f mat : values) {
                write(data, mat);
            }

        } else if(value instanceof SafeArrayList) {

            final SafeArrayList<Matrix3f> values = (SafeArrayList<Matrix3f>) value;
            for (final Matrix3f mat : values.getArray()) {
                write(data, mat);
            }

        } else if(value instanceof Collection) {

            final Collection<Matrix3f> values = (Collection<Matrix3f>) value;
            for (final Matrix3f mat : values) {
                write(data, mat);
            }
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    protected void writeMat4Array(final ByteBuffer data, final Object value) {

        if (value instanceof Matrix4f[]) {

            final Matrix4f[] values = (Matrix4f[]) value;
            for (final Matrix4f mat : values) {
                write(data, mat);
            }

        } else if(value instanceof SafeArrayList) {

            final SafeArrayList<Matrix4f> values = (SafeArrayList<Matrix4f>) value;
            for (final Matrix4f mat : values.getArray()) {
                write(data, mat);
            }

        } else if(value instanceof Collection) {

            final Collection<Matrix4f> values = (Collection<Matrix4f>) value;
            for (final Matrix4f mat : values) {
                write(data, mat);
            }
        }
    }


    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    protected void writeVec4Array(final ByteBuffer data, final Object value) {

        if (value instanceof Object[]) {

            final Object[] values = (Object[]) value;
            for (final Object vec : values) {
                writeVec4(data, vec);
            }

        } else if(value instanceof SafeArrayList) {

            final SafeArrayList<Object> values = (SafeArrayList<Object>) value;
            for (final Object vec : values.getArray()) {
                writeVec4(data, vec);
            }

        } else if(value instanceof Collection) {

            final Collection<Object> values = (Collection<Object>) value;
            for (final Object vec : values) {
                writeVec4(data, vec);
            }
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    protected void writeVec3Array(final ByteBuffer data, final Object value) {

        if (value instanceof Vector3f[]) {

            final Vector3f[] values = (Vector3f[]) value;
            for (final Vector3f vec : values) {
                write(data, vec);
            }

        } else if(value instanceof SafeArrayList) {

            final SafeArrayList<Vector3f> values = (SafeArrayList<Vector3f>) value;
            for (final Vector3f vec : values.getArray()) {
                write(data, vec);
            }

        } else if(value instanceof Collection) {

            final Collection<Vector3f> values = (Collection<Vector3f>) value;
            for (final Vector3f vec : values) {
                write(data, vec);
            }
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    @SuppressWarnings("unchecked")
    protected void writeVec2Array(final ByteBuffer data, final Object value) {

        if (value instanceof Vector2f[]) {

            final Vector2f[] values = (Vector2f[]) value;
            for (final Vector2f vec : values) {
                write(data, vec);
            }

        } else if(value instanceof SafeArrayList) {

            final SafeArrayList<Vector2f> values = (SafeArrayList<Vector2f>) value;
            for (final Vector2f vec : values.getArray()) {
                write(data, vec);
            }

        } else if(value instanceof Collection) {

            final Collection<Vector2f> values = (Collection<Vector2f>) value;
            for (final Vector2f vec : values) {
                write(data, vec);
            }
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final float[] value) {
        for (float val : value) {
            data.putFloat(val);
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final int[] value) {
        for (int val : value) {
            data.putInt(val);
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void writeVec4(final ByteBuffer data, final Object value) {

        if (value == null) {
            data.putLong(0).putLong(0);
        } else if (value instanceof Vector4f) {

            final Vector4f vec4 = (Vector4f) value;
            data.putFloat(vec4.getX())
                    .putFloat(vec4.getY())
                    .putFloat(vec4.getZ())
                    .putFloat(vec4.getW());

        } else if(value instanceof Quaternion) {

            final Quaternion vec4 = (Quaternion) value;
            data.putFloat(vec4.getX())
                    .putFloat(vec4.getY())
                    .putFloat(vec4.getZ())
                    .putFloat(vec4.getW());

        } else if(value instanceof ColorRGBA) {

            final ColorRGBA vec4 = (ColorRGBA) value;
            data.putFloat(vec4.getRed())
                    .putFloat(vec4.getGreen())
                    .putFloat(vec4.getBlue())
                    .putFloat(vec4.getAlpha());
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Vector3f value) {

        if (value == null) {
            data.putLong(0).putInt(0);
        } else {
            data.putFloat(value.getX())
                    .putFloat(value.getY())
                    .putFloat(value.getZ());
        }

        if (layout == Layout.std140) {
            data.putInt(0);
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param x the x value.
     * @param y the y value.
     * @param z the z value.
     */
    protected void write(final ByteBuffer data, final float x, final float y, final float z) {

        data.putFloat(x)
                .putFloat(y)
                .putFloat(z);

        if (layout == Layout.std140) {
            data.putInt(0);
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param x the x value.
     * @param y the y value.
     * @param z the z value.
     * @param w the w value.
     */
    protected void write(final ByteBuffer data, final float x, final float y, final float z, final float w) {
        data.putFloat(x)
                .putFloat(y)
                .putFloat(z)
                .putFloat(w);
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Vector2f value) {
        if (value == null) {
            data.putLong(0);
        } else {
            data.putFloat(value.getX()).putFloat(value.getY());
        }
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Matrix3f value) {
        write(data, value.get(0, 0), value.get(1, 0), value.get(2, 0));
        write(data, value.get(0, 1), value.get(1, 1), value.get(2, 1));
        write(data, value.get(0, 2), value.get(1, 2), value.get(2, 2));
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Matrix4f value) {
        write(data, value.get(0, 0), value.get(1, 0), value.get(2, 0), value.get(3, 0));
        write(data, value.get(0, 1), value.get(1, 1), value.get(2, 1), value.get(3, 1));
        write(data, value.get(0, 2), value.get(1, 2), value.get(2, 2), value.get(3, 2));
        write(data, value.get(0, 3), value.get(1, 3), value.get(2, 3), value.get(3, 3));
    }

    @Override
    public void deleteObject(final Object rendererObject) {

        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This bo can't be deleted from " + rendererObject);
        }

        ((Renderer) rendererObject).deleteBuffer(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new BufferObject(null, getId());
    }

    @Override
    protected void deleteNativeBuffers() {
        super.deleteNativeBuffers();
        if (previousData != null) {
            BufferUtils.destroyDirectBuffer(previousData);
            previousData = null;
        }
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_BO << 32) | ((long) id);
    }
}
