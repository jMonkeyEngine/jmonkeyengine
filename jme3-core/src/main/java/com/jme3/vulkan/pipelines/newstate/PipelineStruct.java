package com.jme3.vulkan.pipelines.newstate;

import org.lwjgl.system.MemoryStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class PipelineStruct <T, P> implements PipelineState<P> {

    protected final Map<String, PipelineState<T>> states = new HashMap<>();

    @Override
    public void apply(MemoryStack stack, P parent) {
        T struct = createStruct(stack);
        for (PipelineState<T> s : states.values()) {
            s.apply(stack, struct);
        }
        applyToParent(parent, struct);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PipelineStruct<?, ?> that = (PipelineStruct<?, ?>) o;
        return Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(states);
    }

    protected abstract T createStruct(MemoryStack stack);

    protected abstract void applyToParent(P parent, T struct);

    @SuppressWarnings("unchecked")
    public <S extends PipelineState> S getState(String name, Supplier<S> generator) {
        S state = (S)states.get(name);
        if (state == null && generator != null) {
            state = generator.get();
            states.put(name, state);
        }
        return state;
    }

}
