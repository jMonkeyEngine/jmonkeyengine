package com.jme3.shader;

/**
 * The implementation of UBO.
 *
 * @author JavaSaBr
 */
public class UniformBufferObject extends BufferObject {

    public UniformBufferObject(final int binding, final BufferObjectField... fields) {
        super(binding, Layout.std140, fields);
    }

    public UniformBufferObject(final int id) {
        super(id);
    }
}
