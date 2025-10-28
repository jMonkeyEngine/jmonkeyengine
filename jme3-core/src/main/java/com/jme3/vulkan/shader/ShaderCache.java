package com.jme3.vulkan.shader;

import com.jme3.asset.AssetManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.graphics.NewShaderState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ShaderCache {

    public static final long DEFAULT_TIMEOUT = 2000;

    private final LogicalDevice<?> device;
    private final AssetManager assetManager;
    private final Map<NewShaderState, CachedShader> shaders = new HashMap<>();
    private long timeout = DEFAULT_TIMEOUT;

    public ShaderCache(LogicalDevice<?> device, AssetManager assetManager) {
        this.device = device;
        this.assetManager = assetManager;
    }

    public Supplier<ShaderModule> acquire(NewShaderState state) {
        CachedShader shader = shaders.get(state);
        if (shader == null) {
            shader = new CachedShader(state.createShader(device, assetManager));
            shaders.put(shader.peek().getState(), shader);
        }
        return shader;
    }

    public void flush() {
        long current = System.currentTimeMillis();
        shaders.values().removeIf(s -> s.isIdle(current));
    }

    public void clear() {
        shaders.clear();
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    protected class CachedShader implements Supplier<ShaderModule> {

        private final ShaderModule module;
        private long lastUsed = System.currentTimeMillis();

        private CachedShader(ShaderModule module) {
            this.module = module;
        }

        @Override
        public ShaderModule get() {
            lastUsed = System.currentTimeMillis();
            return module;
        }

        public ShaderModule peek() {
            return module;
        }

        public boolean isIdle(long current) {
            return Math.abs(current - lastUsed) > timeout;
        }

    }

}
