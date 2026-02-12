package com.jme3.vulkan.struct;

public class Std430Struct implements StdLayoutStruct {

    private int offset = 0;
    private int alignment = StdLayoutType.vec4.getAlignment();

    @Override
    public int add(StdLayoutType type) {
        alignment = Math.max(alignment, type.getAlignment());
        int misalign = offset % type.getAlignment();
        if (misalign != 0) {
            offset += type.getAlignment() - misalign;
        }
        int result = offset;
        int size = type.getSize();
        if (type.isArray()) {
            misalign = type.getSize() % type.getAlignment();
            if (misalign != 0) {
                size += type.getAlignment() - misalign;
            }
        }
        offset += size * type.getCount();
        return result;
    }

    @Override
    public int getSize() {
        return (int)Math.ceil((double)offset / alignment) * alignment;
    }

    @Override
    public int getAlignment() {
        return alignment;
    }

}
