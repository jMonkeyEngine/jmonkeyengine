package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.shadergen.vars.Var;

/**
 * A combined image sampler descriptor.
 *
 * @param <T> sampler type
 */
public class Sampler <T extends Var> extends DescriptorGlobal<T> {

    public Sampler(Location location, T value) {
        super(DescriptorType.CombinedImageSampler, 1, location, value);
    }

    public Sampler(int set, int binding, T value) {
        this(new Location(set, binding), value);
    }

    @Override
    protected void compile(ShaderLogic logic, T value, T ref) {
        logic.add("layout(set=%d,binding=%d) uniform %s %s;", getSet(), getBinding(), value.getType(), ref);
    }

}
