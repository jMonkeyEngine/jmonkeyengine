package com.jme3.vulkan.shadergen;

import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.shadergen.vars.StructVar;

import java.nio.ByteBuffer;

/**
 * A shader's push constants: a small block of values recorded directly into the command buffer
 * instead of being read through a descriptor, suited to values that change every draw, such as
 * per-node transforms. It occupies no descriptor binding. A shader has at most one, at
 * {@link Location#pushConstants()}.
 * <p>
 * Vulkan declares it as a {@code push_constant} block in the layout of its struct, which may be
 * std140 or std430. Vulkan only guarantees 128 bytes of push constants, so larger blocks need the
 * device's {@code maxPushConstantsSize} to allow them.
 * <p>
 * OpenGL has no push constants, so there it is declared as a std140 uniform block at
 * {@link #getGlBinding() its GL binding}, which must not be used by any other uniform block, and
 * the values are uploaded to a uniform buffer bound there. OpenGL uniform blocks cannot be std430,
 * so a shader compiled for OpenGL needs push constants laid out as std140.
 *
 * @param <T> struct of the push constant values
 */
public class PushConstants <T extends StructVar> extends BlockedGlobal<Class<PushConstants>, T> {

    /**
     * OpenGL uniform buffer binding used when none is given.
     */
    public static final int DEFAULT_GL_BINDING = 0;

    private final int glBinding;

    public PushConstants(T value) {
        this(DEFAULT_GL_BINDING, value);
    }

    /**
     * @param glBinding uniform buffer binding to declare the block at on OpenGL
     * @param value struct of the push constant values
     */
    public PushConstants(int glBinding, T value) {
        super(PushConstants.class, value);
        if (glBinding < 0) {
            throw new IllegalArgumentException("GL binding must be at least 0: " + glBinding);
        }
        this.glBinding = glBinding;
    }

    @Override
    protected void compile(ShaderLogic logic, T value, T ref) {
        StructLayout layout = value.getLayout();
        if (layout == null) {
            throw new IllegalStateException("Push constant struct " + value.getClass().getName() + " has no layout.");
        }
        String block = logic.generateUniqueName();
        switch (logic.getTarget()) {
            case Vulkan:
                logic.add("layout(push_constant,%s) uniform %s_t{", layout.getIdentifier(), block);
                break;
            case OpenGL:
                if (!StructLayout.std140.getIdentifier().equals(layout.getIdentifier())) {
                    throw new IllegalStateException("Push constants are a uniform block on OpenGL, so they must be laid out as std140, not "
                            + layout.getIdentifier() + ".");
                }
                logic.add("layout(std140,binding=%d) uniform %s_t{", glBinding, block);
                break;
            default:
                throw new UnsupportedOperationException("Push constants are not supported for " + logic.getTarget() + ".");
        }
        for (StructVar.Member f : value.getFields()) {
            logic.add("%s %s;", f.getType(), f.getName());
        }
        logic.add("}%s;", ref);
    }

    public void set(CommandBuffer cmd, ByteBuffer buffer) {
        cmd.cmdPushConstants(getStages(), 0, buffer);
    }

    /**
     * @return size of the push constant values in bytes
     */
    public int getSize() {
        return getValue().size();
    }

    /**
     * @return uniform buffer binding the block is declared at on OpenGL
     */
    public int getGlBinding() {
        return glBinding;
    }

}
