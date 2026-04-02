package com.jme3.vulkan.mesh;

import com.jme3.vulkan.formats.EnumInterpreter;

public enum IndexType {

    UInt8(Byte.BYTES), UInt16(Short.BYTES), UInt32(Integer.BYTES);

    private final int bytes;

    IndexType(int bytes) {
        this.bytes = bytes;
    }

    public int getEnum(EnumInterpreter interpreter) {
        return interpreter.getIndexTypeEnum(this);
    }

    public int getBytes() {
        return bytes;
    }

}
