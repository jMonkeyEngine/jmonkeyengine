package com.jme3.shader;

import com.jme3.renderer.Renderer;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObject;

import java.nio.ByteBuffer;

/**
 * The implementation of SSBO.
 *
 * @author JavaSaBr
 */
public class ShaderStorageBufferObject extends NativeObject {

    /**
     * The buffer's data.
     */
    private ByteBuffer data;

    /**
     * The binding number.
     */
    private int binding;

    public ShaderStorageBufferObject() {
        this.handleRef = new Object();
        this.binding = 1;
    }

    public ShaderStorageBufferObject(final int id) {
        super(id);
    }

    /**
     * Set the binding number.
     *
     * @param binding the binding number.
     */
    public void setBinding(final int binding) {
        this.binding = binding;
    }

    /**
     * Get the binding number.
     *
     * @return the binding number.
     */
    public int getBinding() {
        return binding;
    }

    /**
     * Called to initialize the data in the {@link ShaderStorageBufferObject}. Must only
     * be called once.
     *
     * @param data the native byte buffer.
     */
    public void setupData(final ByteBuffer data) {

        if (id != -1) {
            throw new UnsupportedOperationException("Data has already been sent. Cannot setupData again.");
        } else if (data.isReadOnly()) {
            throw new IllegalArgumentException("VertexBuffer data cannot be read-only.");
        }

        this.data = data;

        setUpdateNeeded();
    }

    /**
     * Called to update the data in the buffer with new data. Can only
     * be called after {@link #setupData(java.nio.ByteBuffer) }
     * has been called. Note that it is fine to call this method on the
     * data already set, e.g. ssbo.updateData(ssbo.getData()), this will just
     * set the proper update flag indicating the data should be sent to the GPU
     * again.
     * <p>
     * It is allowed to specify a buffer with different capacity than the
     * originally set buffer.
     *
     * @param data the native data buffer to set.
     */
    public void updateData(final ByteBuffer data) {

        if (data == null) {
            throw new IllegalArgumentException("SSBO can't be without data buffer.");
        }

        // Check if the data buffer is read-only which is a sign
        // of a bug on the part of the caller
        if (data.isReadOnly()) {
            throw new IllegalArgumentException("SSBO's data cannot be read-only.");
        }

        this.data = data;

        setUpdateNeeded();
    }

    /**
     * Get the buffer's data.
     *
     * @return the buffer's data.
     */
    public ByteBuffer getData() {
        return data;
    }

    @Override
    public void resetObject() {
        this.id = -1;
        setUpdateNeeded();
    }

    @Override
    public void deleteObject(final Object rendererObject) {

        if (!(rendererObject instanceof Renderer)) {
            throw new IllegalArgumentException("This ssbo can't be deleted from " + rendererObject);
        }

        ((Renderer) rendererObject).deleteBuffer(this);
    }

    @Override
    protected void deleteNativeBuffers() {
        super.deleteNativeBuffers();
        if (data != null) {
            BufferUtils.destroyDirectBuffer(data);
        }
    }

    @Override
    public NativeObject createDestructableClone() {
        return new ShaderStorageBufferObject(id);
    }

    @Override
    public long getUniqueId() {
        return ((long) OBJTYPE_SSBO << 32) | ((long) id);
    }
}
