package com.jme3.vulkan.material.shader;

import com.jme3.util.Version;
import com.jme3.util.Versionable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Stores the defines that are to be used in a shader. If the defines
 * change, the shader will need to be re-compiled, or another existing
 * shader used.
 */
public class ShaderDefines implements Versionable {

    private final Map<String, Version<Define>> defines = new HashMap<>();
    private long version = 0L;

    @Override
    public long getVersionNumber() {
        for (Version<Define> d : defines.values()) {
            if (d.update()) version++;
        }
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ShaderDefines that = (ShaderDefines) o;
        return Objects.equals(defines, that.defines);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(defines);
    }

    public ShaderDefines snapshot() {
        ShaderDefines copy = new ShaderDefines();
        for (Map.Entry<String, Version<Define>> d : defines.entrySet()) {
            copy.defines.put(d.getKey(), new Version<>(new ConstantDefine(
                    d.getValue().get().getDefineName(), d.getValue().get().getDefineValue())));
        }
        return copy;
    }

    public void snapshot(Map<String, String> store) {
        for (Map.Entry<String, Version<Define>> d : defines.entrySet()) {
            store.put(d.getKey(), d.getValue().get().getDefineValue());
        }
    }

}
