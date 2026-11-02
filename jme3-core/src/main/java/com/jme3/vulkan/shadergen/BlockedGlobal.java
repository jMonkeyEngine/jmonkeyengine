package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.Var;
import com.jme3.vulkan.util.Flag;

/**
 * Base {@link Global} that is declared once per stage, in its own block of generated code.
 * Subclasses only write the declaration.
 *
 * @param <T> type of the value
 */
public abstract class BlockedGlobal <K, T extends Var> implements Global<K, T> {

    private final K key;
    private final T value;
    private Shader shader;
    private Flag<ShaderStage> stages = Flag.empty();
    private T ref;

    public BlockedGlobal(K key, T value) {
        this.key = key;
        this.value = value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T use() {
        ShaderLogic logic = ShaderLogic.get();
        return (T)logic.getSource().useGlobalByLocation(key).compile(logic);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T compile(ShaderLogic logic) {
        if (logic.startBlock(this)) {
            stages = stages.add(logic.getStage());
            ref = (T)value.create(logic.generateUniqueName());
            compile(logic, value, ref);
            logic.endBlock();
        }
        return ref;
    }

    /**
     * Writes the declaration of this global.
     *
     * @param logic generator of the stage being compiled
     * @param value value declared by this global
     * @param ref reference to the value in generated code
     */
    protected abstract void compile(ShaderLogic logic, T value, T ref);

    @Override
    public void setShader(Shader shader) {
        this.shader = shader;
    }

    /**
     * @return shader this global is registered with
     * @throws IllegalStateException if this global is not registered with a shader
     */
    protected Shader getShader() {
        if (shader == null) {
            throw new IllegalStateException("Global at " + key + " is not registered with a shader.");
        }
        return shader;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public Flag<ShaderStage> getStages() {
        return stages;
    }

}
