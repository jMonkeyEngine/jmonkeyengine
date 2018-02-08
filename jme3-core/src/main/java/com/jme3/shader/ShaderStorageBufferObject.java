package com.jme3.shader;

/**
 * The implementation of SSBO.
 *
 * @author JavaSaBr
 */
public class ShaderStorageBufferObject extends BufferObject {

    public ShaderStorageBufferObject(final int binding, final Layout layout, final BufferObjectField... fields) {
        super(binding, layout, fields);
    }

    public ShaderStorageBufferObject(final int id) {
        super(id);
    }
}
