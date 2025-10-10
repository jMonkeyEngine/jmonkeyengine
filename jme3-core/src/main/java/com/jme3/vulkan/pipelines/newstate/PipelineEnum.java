package com.jme3.vulkan.pipelines.newstate;

import com.jme3.vulkan.util.IntEnum;

public abstract class PipelineEnum <T extends IntEnum, P> extends PipelineProperty<T, P> {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineEnum<? extends IntEnum, ?> that = (PipelineEnum<? extends IntEnum, ?>)o;
        return value.is(that.value.getEnum());
    }

}
