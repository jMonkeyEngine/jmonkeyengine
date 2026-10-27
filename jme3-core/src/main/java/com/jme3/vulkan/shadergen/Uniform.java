package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.shadergen.vars.StructVar;

public class Uniform <T extends StructVar> extends Global<T> {

    public Uniform(Location location, T value) {
        super(DescriptorType.UniformBuffer, 1, location, value);
    }

    public Uniform(int set, int binding, T value) {
        this(new Location(set, binding), value);
    }

    @Override
    protected void compile(ShaderLogic logic, T value, T ref) {
        logic.add("layout(%s,set=%d,binding=%d) uniform %s_t{", getValue().getLayout().getIdentifier(),
                getLocation().getSet(), getLocation().getBinding(), logic.generateUniqueName());
        for (StructVar.Member f : value.getFields()) {
            logic.add("%s %s;", f.getType(), f.getName());
        }
        logic.add("}%s;", ref);
    }

}
