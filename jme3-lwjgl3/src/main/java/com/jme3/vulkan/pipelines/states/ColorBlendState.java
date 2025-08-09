package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;

public class ColorBlendState implements PipelineState<VkPipelineColorBlendStateCreateInfo> {

    private final List<ColorBlendAttachment> attachments = new ArrayList<>();
    private boolean logicEnabled = false;
    private int logic = VK_LOGIC_OP_COPY;

    @Override
    public VkPipelineColorBlendStateCreateInfo toStruct(MemoryStack stack) {
        VkPipelineColorBlendAttachmentState.Buffer attBuf = VkPipelineColorBlendAttachmentState.calloc(attachments.size(), stack);
        for (ColorBlendAttachment a : attachments) {
            a.writeToStruct(attBuf.get());
        }
        attBuf.flip();
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(logicEnabled)
                .logicOp(logic)
                .pAttachments(attBuf);
    }

    public void addAttachment(ColorBlendAttachment attachment) {
        this.attachments.add(attachment);
    }

    public void setLogicEnabled(boolean logicEnabled) {
        this.logicEnabled = logicEnabled;
    }

    public void setLogic(int logic) {
        this.logic = logic;
    }

}
