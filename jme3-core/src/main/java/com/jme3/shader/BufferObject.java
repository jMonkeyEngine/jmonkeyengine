package com.jme3.shader;

import com.jme3.math.*;
import com.jme3.renderer.Renderer;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;
import com.jme3.util.SafeArrayList;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The base implementation of BO.
 *
 * @author JavaSaBr
 */
public class BufferObject extends NativeObject {

    public enum Layout {
        std140,
        std430,
    }

    /**
     * The fields of this BO.
     */
    private final Map<String, BufferObjectField> fields;

    /**
     * The buffer's data layout.
     */
    private final Layout layout;

    /**
     * The binding number.
     */
    private final int binding;

    /**
     * The previous data buffer.
     */
    private ByteBuffer previousData;

    public BufferObject(final int binding, final Layout layout, final BufferObjectField... fields) {
        this.handleRef = new Object();
        this.binding = binding;
        this.layout = layout;
        this.fields = new LinkedHashMap<>(fields.length);
        for (final BufferObjectField field : fields) {
            this.fields.put(field.getName(), field);
        }
    }

    public BufferObject(final int id) {
        super(id);
        this.binding = -2;
        this.fields = null;
        this.layout = null;
    }

    /**
     * Sets the value to the filed by the field's name.
     *
     * @param name  the field's name.
     * @param value the value.
     */
    public void setValue(final String name, final Object value) {

        final BufferObjectField field = fields.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Unknown a field with the name " + name);
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
    public <T> T getValue(final String name) {

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

        for (final Map.Entry<String, BufferObjectField> entry : fields.entrySet()) {
            final BufferObjectField field = entry.getValue();
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
            }
        }

        final ByteBuffer data = previousData == null ? BufferUtils.createByteBuffer((int) (estimateSize * 1.1F)) : previousData;

        for (final Map.Entry<String, BufferObjectField> entry : fields.entrySet()) {
            writeField(entry.getValue(), data);
        }

        data.flip();

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
            case Int:
                return 4;
            case Float:
                return 4;
            case Boolean:
                return 1;
            case Vector2:
                return 8;
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
                return estimateArray(field.getValue(), 2);
            }
            case Vector3Array: {
                final int multiplier = layout == Layout.std140? 4 : 3;
                return estimateArray(field.getValue(), multiplier);
            }
            case Vector4Array: {
                return estimateArray(field.getValue(), 4);
            }
            case Matrix3: {
                final int multiplier = layout == Layout.std140? 4 : 3;
                return 3 * multiplier;
            }
            case Matrix4: {
                return 4 * 4;
            }
            case Matrix3Array: {
                int multiplier = layout == Layout.std140? 4 : 3;
                multiplier *= 3;
                return estimateArray(field.getValue(), multiplier);
            }
            case Matrix4Array: {
                int multiplier = layout == Layout.std140? 4 : 3;
                multiplier *= 4;
                return estimateArray(field.getValue(), multiplier);
            }
            default: {
                throw new IllegalArgumentException("The type of BO field " + field.getType() + " doesn't support.");
            }
        }
    }

    /**
     * Estimates bytes count to present the value on GPU.
     *
     * @param value      the value.
     * @param multiplier the multiplier.
     * @return the estimated bytes cunt.
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
     * Estimates bytes count to present the values on GPU.
     *
     * @param values the values.
     * @return the estimated bytes cunt.
     */
    protected int estimate(final float[] values) {
        return values.length * 4;
    }

    /**
     * Estimates bytes count to present the values on GPU.
     *
     * @param values the values.
     * @return the estimated bytes cunt.
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
            case Int:
                data.putInt(((Number) value).intValue());
                break;
            case Float:
                data.putFloat(((Number) value).floatValue());
                break;
            case Boolean:
                data.putInt(((Boolean) value) ? 1 : 0);
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

        if(value instanceof Vector4f) {

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

        data.putFloat(value.getX())
            .putFloat(value.getY())
            .putFloat(value.getZ());

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
        data.putFloat(value.getX())
            .putFloat(value.getY());
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Matrix3f value) {
        write(data, value.get(0, 0), value.get(0, 1), value.get(0, 2));
        write(data, value.get(1, 0), value.get(1, 1), value.get(1, 2));
        write(data, value.get(2, 0), value.get(2, 1), value.get(2, 2));
    }

    /**
     * Writes the value to the data buffer.
     *
     * @param data  the data buffer.
     * @param value the value.
     */
    protected void write(final ByteBuffer data, final Matrix4f value) {
        write(data, value.get(0, 0), value.get(0, 1), value.get(0, 2), value.get(0, 3));
        write(data, value.get(1, 0), value.get(1, 1), value.get(1, 2), value.get(1, 3));
        write(data, value.get(2, 0), value.get(2, 1), value.get(2, 2), value.get(2, 3));
        write(data, value.get(3, 0), value.get(3, 1), value.get(3, 2), value.get(3, 3));
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
        return new BufferObject(id);
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
