package com.jme3.vulkan.struct;

public class Std140Struct implements StdLayoutStruct {

    private int offset = 0;
    private int alignment = StdLayoutType.vec4.getAlignment();

    @Override
    public int add(StdLayoutType type) {
        int typeAlign = type.isArray() ? Math.max(type.getAlignment(), StdLayoutType.vec4.getAlignment()) : type.getAlignment();
        alignment = Math.max(alignment, typeAlign);
        int misalign = offset % typeAlign;
        if (misalign != 0) {
            offset += typeAlign - misalign;
        }
        int result = offset;
        int size = type.getSize();
        if (type.isArray()) {
            misalign = type.getSize() % typeAlign;
            if (misalign != 0) {
                size += typeAlign - misalign;
            }
        }
        offset += size * type.getCount();
        return result;
    }

    @Override
    public int getSize() {
        return offset;
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

}
