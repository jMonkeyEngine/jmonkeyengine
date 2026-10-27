package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.StructVar;
import com.jme3.vulkan.shadergen.vars.Var;
import com.jme3.vulkan.util.Flag;

public abstract class Global <T extends Var> {

    public static <T extends StructVar> Global<T> uniform(int set, int binding, T value) {
        return new Uniform<>(new Location(set, binding), value);
    }

    public static <T extends StructVar> Global<T> storage(int set, int binding, T value) {
        return new Storage<>(new Location(set, binding), value);
    }

    public static <T extends Var> Global<T> sampler(int set, int binding, T value) {
        return new Value<>(DescriptorType.CombinedImageSampler, 1, new Location(set, binding), value);
    }

    public static <T extends Var> Global<T> specialization(int id, T value) {
        return new Specialization<>(id, value);
    }

    private DescriptorType type;
    private final int descriptorCount;
    private final Location location;
    private final T value;
    private Flag<ShaderStage> stages = Flag.empty();
    private T ref;

    public Global(DescriptorType type, int descriptorCount, Location location, T value) {
        this.type = type;
        this.descriptorCount = descriptorCount;
        this.location = location;
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public T use() {
        ShaderLogic logic = ShaderLogic.get();
        return (T)logic.getSource().useGlobalByLocation(location).compile(logic);
    }

    @SuppressWarnings("unchecked")
    protected T compile(ShaderLogic logic) {
        if (logic.startBlock(this)) {
            stages = stages.add(logic.getStage());
            ref = (T)value.create(logic.generateUniqueName());
            compile(logic, value, ref);
            logic.endBlock();
        }
        return ref;
    }

    protected abstract void compile(ShaderLogic logic, T value, T ref);

    public void makeDynamic() {
        if (type == DescriptorType.UniformBuffer) {
            type = DescriptorType.UniformBufferDynamic;
        } else if (type == DescriptorType.StorageBuffer) {
            type = DescriptorType.StorageBufferDynamic;
        }
    }

    public DescriptorType getType() {
        return type;
    }

    public int getDescriptorCount() {
        return descriptorCount;
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

    public Location getLocation() {
        return location;
    }

    public T getValue() {
        return value;
    }

    private static class Value <T extends Var> extends Global<T> {

        public Value(DescriptorType type, int descriptorCount, Location location, T value) {
            super(type, descriptorCount, location, value);
        }

        @Override
        protected void compile(ShaderLogic logic, T value, T ref) {
            logic.add("layout(set=%d,binding=%d) uniform %s %s;", getLocation().getSet(), getLocation().getBinding(), value.getType(), ref);
        }

    }

}
