package com.jme3.vulkan.pipelines.newstate;

import java.util.Objects;

public abstract class PipelineProperty <T, P> implements PipelineState<P> {

    protected T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PipelineProperty<?, ?> that = (PipelineProperty<?, ?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

}
