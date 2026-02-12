package com.jme3.vulkan.pipeline.cache;

import com.jme3.asset.AssetManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.states.BasePipelineState;
import com.jme3.vulkan.pipeline.states.IShaderState;
import com.jme3.vulkan.material.shader.ShaderModule;

import java.util.*;
import java.util.function.Supplier;

public class PipelineCache {

    public static final long DEFAULT_TIMEOUT = 2000L;

    private final LogicalDevice<?> device;
    private final AssetManager assetManager;
    private final Map<BasePipelineState<?, ?>, CacheElement<Pipeline>> pipelines = new HashMap<>();
    private final Map<IShaderState, CacheElement<ShaderModule>> shaders = new HashMap<>();
    private long timeout = DEFAULT_TIMEOUT;

    public PipelineCache(LogicalDevice<?> device, AssetManager assetManager) {
        this.device = device;
        this.assetManager = assetManager;
    }

    public Supplier<Pipeline> acquirePipeline(BasePipelineState<?, ?> state, Pipeline parent) {
        CacheElement<Pipeline> pipeline = pipelines.get(state);
        if (pipeline == null) {
            pipeline = new CacheElement<>();
            Collection<ShaderModule> shaders = new ArrayList<>(state.getPipelineShaderStates().size());
            for (IShaderState s : state.getPipelineShaderStates()) {
                CacheElement<ShaderModule> e = acquireShader(s);
                shaders.add(e.get());
                e.addDependent(pipeline);
            }
            pipeline.set(state.createPipeline(device, parent, shaders));
            pipelines.put(pipeline.peek().getState(), pipeline);
        }
        return pipeline;
    }

    protected CacheElement<ShaderModule> acquireShader(IShaderState state) {
        CacheElement<ShaderModule> shader = shaders.get(state);
        if (shader == null) {
            shader = new CacheElement<>(new ShaderModule(device, assetManager, state));
            shaders.put(state, shader);
        }
        return shader;
    }

    public void flush() {
        long current = System.currentTimeMillis();
        pipelines.values().removeIf(p -> p.isIdle(current));
        shaders.values().removeIf(s -> s.isIdle(current));
    }

    public void clear() {
        pipelines.clear();
        shaders.clear();
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    protected class CacheElement<T> implements Supplier<T> {

        private T object;
        private long lastUsed = System.currentTimeMillis();
        private final Collection<CacheElement> dependents = new ArrayList<>();

        public CacheElement() {}

        public CacheElement(T object) {
            this.object = object;
        }

        public void set(T object) {
            this.object = object;
        }

        @Override
        public T get() {
            lastUsed = System.currentTimeMillis();
            return object;
        }

        public T peek() {
            return object;
        }

        public boolean isIdle(long current) {
            return Math.abs(current - lastUsed) > timeout
                    && dependents.stream().allMatch(d -> d.isIdle(current));
        }

        public void addDependent(CacheElement d) {
            dependents.add(d);
        }

    }

}
