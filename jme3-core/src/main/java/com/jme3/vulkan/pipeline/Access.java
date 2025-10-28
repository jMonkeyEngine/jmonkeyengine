package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public enum Access implements Flag<Access> {

    ColorAttachmentWrite(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT),
    ColorAttachmentRead(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT),
    HostWrite(VK_ACCESS_HOST_WRITE_BIT),
    HostRead(VK_ACCESS_HOST_READ_BIT),
    IndexRead(VK_ACCESS_INDEX_READ_BIT),
    DepthStencilAttachmentWrite(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT),
    DepthStencilAttachmentRead(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT),
    IndirectCommandRead(VK_ACCESS_INDIRECT_COMMAND_READ_BIT),
    InputAttachmentRead(VK_ACCESS_INPUT_ATTACHMENT_READ_BIT),
    MemoryWrite(VK_ACCESS_MEMORY_WRITE_BIT),
    MemoryRead(VK_ACCESS_MEMORY_READ_BIT),
    ShaderWrite(VK_ACCESS_SHADER_WRITE_BIT),
    ShaderRead(VK_ACCESS_SHADER_READ_BIT),
    TransferWrite(VK_ACCESS_TRANSFER_WRITE_BIT),
    TransferRead(VK_ACCESS_TRANSFER_READ_BIT),
    UniformRead(VK_ACCESS_UNIFORM_READ_BIT),
    VertexAttributeRead(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT);

    private final int vkEnum;

    Access(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int bits() {
        return vkEnum;
    }

}
