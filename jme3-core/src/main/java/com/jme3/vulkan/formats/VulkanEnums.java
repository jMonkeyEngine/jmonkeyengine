package com.jme3.vulkan.formats;

import com.jme3.vulkan.pipeline.Topology;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanEnums {

    public static void setup() {

        // formats
        Format.RGBA32_SFloat.set(VK_FORMAT_R32G32B32A32_SFLOAT);
        Format.RGB32_SFloat.set(VK_FORMAT_R32G32B32_SFLOAT);
        Format.RG32_SFloat.set(VK_FORMAT_R32G32_SFLOAT);
        Format.R32_SFloat.set(VK_FORMAT_R32_SFLOAT);
        Format.RGBA8_SRGB.set(VK_FORMAT_R8G8B8A8_SRGB);
        Format.R8_SRGB.set(VK_FORMAT_R8_SRGB);
        Format.BGR8_SRGB.set(VK_FORMAT_B8G8R8_SRGB);
        Format.ABGR8_SRGB_Pack32.set(VK_FORMAT_A8B8G8R8_SRGB_PACK32);
        Format.BGRA8_SRGB.set(VK_FORMAT_B8G8R8A8_SRGB);
        Format.Depth32_SFloat.set(VK_FORMAT_D32_SFLOAT);
        Format.Depth32_SFloat_Stencil8_UInt.set(VK_FORMAT_D32_SFLOAT_S8_UINT);
        Format.Depth24_UNorm_Stencil8_UInt.set(VK_FORMAT_D24_UNORM_S8_UINT);
        Format.Depth16_UNorm.set(VK_FORMAT_D16_UNORM);
        Format.Depth16_UNorm_Stencil8_UInt.set(VK_FORMAT_D16_UNORM_S8_UINT);

        // topology
        Topology.LineList.set(VK_PRIMITIVE_TOPOLOGY_LINE_LIST);
        Topology.LineStrip.set(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP);
        Topology.TriangleList.set(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
        Topology.PatchList.set(VK_PRIMITIVE_TOPOLOGY_PATCH_LIST);
        Topology.PointList.set(VK_PRIMITIVE_TOPOLOGY_POINT_LIST);
        Topology.LineListAdjacency.set(VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY);
        Topology.LineStripAdjacency.set(VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY);
        Topology.TriangleFan.set(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN);
        Topology.TriangleListAdjacency.set(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY);
        Topology.TriangleStrip.set(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP);
        Topology.TriangleStripAdjacency.set(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY);

    }

}
