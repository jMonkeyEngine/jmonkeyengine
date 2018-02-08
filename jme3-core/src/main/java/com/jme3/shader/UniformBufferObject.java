package com.jme3.shader;

/**
 * The implementation of UBO.
 *
 * @author JavaSaBr
 */
public class UniformBufferObject extends BufferObject {

    public UniformBufferObject(final int binding, final Layout layout, final BufferObjectField... fields) {
        super(binding, layout, fields);
    }

    public UniformBufferObject(final int id) {
        super(id);
    }
}
