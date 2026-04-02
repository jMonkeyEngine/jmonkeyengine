package com.jme3.vulkan.mesh;

import com.jme3.util.struct.Struct;
import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.VulkanEnums;
import com.jme3.vulkan.pipeline.VertexPipeline;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;

public class VertexData extends ArrayList<VertexBuffer> {

    public void bind(MemoryStack stack, CommandBuffer cmd, VertexPipeline pipeline) {
        LongBuffer verts = stack.mallocLong(size());
        LongBuffer offsets = stack.mallocLong(size());
        Set<Integer> filledAttrLoc = new HashSet<>();
        for (VertexBuffer<Struct<VertexAttr>> vb : this) {
            for (VertexAttr a : vb.getStruct().getFields()) {
                Integer loc = pipeline.getAttributeLocation(a.getName());
                if (loc != null && filledAttrLoc.add(loc)) {
                    VulkanBuffer buffer = (VulkanBuffer)vb.getBuffer();
                    verts.put(buffer.getBufferId(cmd.getPool().getDevice()));
                    offsets.put(buffer.size().getOffset());
                    break;
                }
            }
        }
        vkCmdBindVertexBuffers(cmd.getBuffer(), 0, verts.flip(), offsets.flip());
    }

    public VertexInput declareInput(MemoryStack stack, VertexPipeline pipeline) {
        int possibleBindings = 0;
        int possibleAttrSlots = 0;
        for (VertexBuffer<Struct<VertexAttr>> vb : this) {
            possibleBindings++;
            for (VertexAttr a : vb.getStruct().getFields()) {
                possibleAttrSlots += a.getFormats().length;
            }
        }
        VkVertexInputBindingDescription.Buffer bindingBuffer = VkVertexInputBindingDescription.malloc(possibleBindings, stack);
        VkVertexInputAttributeDescription.Buffer attrBuffer = VkVertexInputAttributeDescription.malloc(possibleAttrSlots, stack);
        Set<Integer> filledAttrLoc = new HashSet<>();
        int bindingIndex = 0;
        for (VertexBuffer<Struct<VertexAttr>> vb : this) {
            boolean anyValid = false;
            for (VertexAttr attr : vb.getStruct().getFields()) {
                Integer loc = pipeline.getAttributeLocation(attr.getName());
                if (loc == null || !filledAttrLoc.add(loc)) {
                    continue;
                }
                anyValid = true;
                int offset = attr.getOffset();
                Format[] formats = attr.getFormats();
                assert formats.length > 0;
                for (int i = 0; i < formats.length; i++) {
                    attrBuffer.get().set(loc + i, bindingIndex, formats[i].getEnum(VulkanEnums.instance), offset);
                    offset += formats[i].getBytes();
                }
            }
            if (anyValid) {
                bindingBuffer.get().set(bindingIndex++, vb.getStride(), vb.getRate().getEnum());
            }
        }
        return new VertexInput(bindingBuffer.flip(), attrBuffer.flip());
    }

}
