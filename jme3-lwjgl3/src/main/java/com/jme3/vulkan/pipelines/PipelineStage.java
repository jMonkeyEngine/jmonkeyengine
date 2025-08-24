package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum PipelineStage implements Flag<PipelineStage> {

    TopOfPipe(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT),
    ColorAttachmentOutput(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
    AllCommands(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT),
    AllGraphics(VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT),
    EarlyFragmentTests(VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT),
    BottomOfPipe(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT),
    ComputeShader(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT),
    DrawIndirect(VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT),
    FragmentShader(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT),
    GeometryShader(VK_PIPELINE_STAGE_GEOMETRY_SHADER_BIT),
    Host(VK_PIPELINE_STAGE_HOST_BIT),
    LateFragmentTests(VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT),
    TessellationControlShader(VK_PIPELINE_STAGE_TESSELLATION_CONTROL_SHADER_BIT),
    TessellationEvalShader(VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT),
    Transfer(VK_PIPELINE_STAGE_TRANSFER_BIT),
    VertexInput(VK_PIPELINE_STAGE_VERTEX_INPUT_BIT),
    VertexShader(VK_PIPELINE_STAGE_VERTEX_SHADER_BIT),
    None(0);

    private final int vkEnum;

    PipelineStage(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int bits() {
        return vkEnum;
    }

}
