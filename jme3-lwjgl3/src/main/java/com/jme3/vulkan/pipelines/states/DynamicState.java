package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.util.LibEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.*;

public class DynamicState implements PipelineState<VkPipelineDynamicStateCreateInfo> {

    public enum Type implements LibEnum<Type> {

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

    private final Set<LibEnum<Type>> states = new HashSet<>();

    @SafeVarargs
    public DynamicState(LibEnum<Type>... types) {
        addTypes(types);
    }

    @Override
    public VkPipelineDynamicStateCreateInfo toStruct(MemoryStack stack) {
        IntBuffer stateBuf = stack.mallocInt(states.size());
        for (LibEnum<Type> t : states) {
            stateBuf.put(t.getEnum());
        }
        stateBuf.flip();
        return VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(stateBuf);
    }

    @SafeVarargs
    public final void addTypes(LibEnum<Type>... types) {
        states.addAll(Arrays.asList(types));
    }

}
