package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.lwjgl.vulkan.VK10.*;

public class DynamicState implements PipelineState<VkPipelineDynamicStateCreateInfo> {

    public enum Type implements IntEnum<Type> {

        ViewPort(VK_DYNAMIC_STATE_VIEWPORT),
        Scissor(VK_DYNAMIC_STATE_SCISSOR),
        BlendConstants(VK_DYNAMIC_STATE_BLEND_CONSTANTS),
        DepthBias(VK_DYNAMIC_STATE_DEPTH_BIAS),
        DepthBounds(VK_DYNAMIC_STATE_DEPTH_BOUNDS),
        LineWidth(VK_DYNAMIC_STATE_LINE_WIDTH),
        StencilCompareMask(VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK),
        StencilReference(VK_DYNAMIC_STATE_STENCIL_REFERENCE),
        StencilWriteMask(VK_DYNAMIC_STATE_STENCIL_WRITE_MASK);

        private final int vkEnum;

        Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    private final Set<Integer> states = new HashSet<>();
    protected long version = 0L;

    @SafeVarargs
    public DynamicState(IntEnum<Type>... types) {
        addTypes(types);
    }

    @Override
    public VkPipelineDynamicStateCreateInfo create(MemoryStack stack) {
        return VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineDynamicStateCreateInfo fill(MemoryStack stack, VkPipelineDynamicStateCreateInfo struct) {
        IntBuffer stateBuf = stack.mallocInt(states.size());
        for (Integer t : states) {
            stateBuf.put(t);
        }
        stateBuf.flip();
        return struct.pDynamicStates(stateBuf);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DynamicState that = (DynamicState) o;
        return Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(states);
    }

    @SafeVarargs
    public final void addTypes(IntEnum<Type>... types) {
        if (types.length > 0) {
            Arrays.stream(types).map(IntEnum::getEnum).collect(Collectors.toCollection(() -> states));
            version++;
        }
    }

}
