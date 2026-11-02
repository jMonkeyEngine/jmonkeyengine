package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.shadergen.vars.StructVar;
import com.jme3.vulkan.shadergen.vars.Var;
import com.jme3.vulkan.util.Flag;

/**
 * A value declared at shader scope, shared by every stage of a {@link Shader} that uses it. Each
 * global is identified within its shader by its {@link #getKey() location}; a global declared
 * by a shader hides any global at the same location declared by a shader it includes.
 * <p>
 * Only {@link DescriptorGlobal descriptor globals} occupy descriptor set bindings and have resources
 * assigned to them. Other kinds of globals, such as {@link PushConstants} and
 * {@link Specialization specialization constants}, are supplied to the shader in other ways.
 * <p>
 * Implementations normally extend {@link BlockedGlobal}.
 *
 * @param <T> type of the value
 */
public interface Global <K, T extends Var> {

    static <T extends StructVar> Uniform<T> uniform(int set, int binding, T value) {
        return new Uniform<>(new Location(set, binding), value);
    }

    static <T extends StructVar> Storage<T> storage(int set, int binding, T value) {
        return new Storage<>(new Location(set, binding), value);
    }

    static <T extends Var> Sampler<T> sampler(int set, int binding, T value) {
        return new Sampler<>(new Location(set, binding), value);
    }

    /**
     * Creates the push constants global. On OpenGL it is declared as a uniform block at
     * {@link PushConstants#DEFAULT_GL_BINDING}.
     *
     * @see PushConstants
     */
    static <T extends StructVar> PushConstants<T> pushConstants(T value) {
        return new PushConstants<>(value);
    }

    /**
     * Creates the push constants global, declared on OpenGL as a uniform block at {@code glBinding}.
     *
     * @see PushConstants
     */
    static <T extends StructVar> PushConstants<T> pushConstants(int glBinding, T value) {
        return new PushConstants<>(glBinding, value);
    }

    static <T extends Var> Specialization<T> specialization(int id, T value) {
        return new Specialization<>(id, value);
    }

    /**
     * Declares this global in the stage being generated, if it isn't already, and references it.
     * Must be called while a stage is being generated.
     *
     * @return reference to this global's value in generated code
     */
    T use();

    /**
     * Declares this global in the stage {@code logic} is generating, if it isn't already. Called by
     * the shader that registered this global; use {@link #use()} instead.
     *
     * @param logic generator of the stage being compiled
     * @return reference to this global's value in generated code
     */
    T compile(ShaderLogic logic);

    /**
     * Registers this global with the shader that declares it. Called by {@link Shader} when compiling.
     *
     * @param shader shader the global belongs to
     */
    void setShader(Shader shader);

    /**
     * @return location identifying this global within its shader
     */
    K getKey();

    /**
     * @return value declared by this global
     */
    T getValue();

    /**
     * @return stages that use this global
     */
    Flag<ShaderStage> getStages();

}
