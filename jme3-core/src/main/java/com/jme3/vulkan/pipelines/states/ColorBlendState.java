package com.jme3.vulkan.pipelines.states;

import com.jme3.vulkan.pipelines.LogicOp;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;

public class ColorBlendState implements PipelineState<VkPipelineColorBlendStateCreateInfo> {

    private final List<ColorBlendAttachment> attachments = new ArrayList<>();
    private boolean logicEnabled = false;
    private IntEnum<LogicOp> logic = LogicOp.Copy;
    protected long version = 0L;

    @Override
    public VkPipelineColorBlendStateCreateInfo create(MemoryStack stack) {
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
    }

    @Override
    public VkPipelineColorBlendStateCreateInfo fill(MemoryStack stack, VkPipelineColorBlendStateCreateInfo struct) {
        VkPipelineColorBlendAttachmentState.Buffer attBuf = VkPipelineColorBlendAttachmentState.calloc(attachments.size(), stack);
        for (ColorBlendAttachment a : attachments) {
            a.writeToStruct(attBuf.get());
        }
        attBuf.flip();
        return struct.logicOpEnable(logicEnabled)
                .logicOp(logic.getEnum())
                .pAttachments(attBuf);
    }

    @Override
    public long getCurrentVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColorBlendState that = (ColorBlendState) o;
        return logicEnabled == that.logicEnabled
                && IntEnum.is(logic, that.logic)
                && Objects.equals(attachments, that.attachments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachments, logicEnabled, logic);
    }

    public void addAttachment(ColorBlendAttachment attachment) {
        this.attachments.add(attachment);
        version++;
    }

    public void setLogicEnabled(boolean logicEnabled) {
        if (this.logicEnabled != logicEnabled) {
            this.logicEnabled = logicEnabled;
            version++;
        }
    }

    public void setLogic(IntEnum<LogicOp> logic) {
        if (!IntEnum.is(this.logic, logic)) {
            this.logic = logic;
            version++;
        }
    }

}
