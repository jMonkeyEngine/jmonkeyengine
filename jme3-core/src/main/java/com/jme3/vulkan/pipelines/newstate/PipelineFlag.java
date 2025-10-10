package com.jme3.vulkan.pipelines.newstate;

import com.jme3.vulkan.util.Flag;

public abstract class PipelineFlag <T extends Flag, P> extends PipelineProperty<T, P> {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineFlag<? extends Flag, ?> that = (PipelineFlag<? extends Flag, ?>)o;
        return value.is(that.value.bits());
    }

}
