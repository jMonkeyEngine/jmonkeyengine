package com.jme3.vulkan;

import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.shaderc.ShaderType;

import static org.lwjgl.vulkan.VK14.*;

public class VulkanEnums implements EnumInterpreter {

    public static VulkanEnums instance = new VulkanEnums();

    @Override
    public int getShaderTypeEnum(ShaderType type) {
        switch (type) {
            case Vertex: return VK_SHADER_STAGE_VERTEX_BIT;
            case Geometry: return VK_SHADER_STAGE_GEOMETRY_BIT;
            case TessellationEval: return VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
            case TessellationControl: return VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
            case Fragment: return VK_SHADER_STAGE_FRAGMENT_BIT;
            case Compute: return VK_SHADER_STAGE_COMPUTE_BIT;
            default: throw new UnsupportedOperationException(type.name());
        }
    }

    @Override
    public int getFormatEnum(Format fmt) {
        switch (fmt) {
            case RGBA32_SFloat: return VK_FORMAT_R32G32B32A32_SFLOAT;
            case RGB32_SFloat: return VK_FORMAT_R32G32B32_SFLOAT;
            case RG32_SFloat: return VK_FORMAT_R32G32_SFLOAT;
            case R32_SFloat: return VK_FORMAT_R32_SFLOAT;
            case RGBA8_SRGB: return VK_FORMAT_R8G8B8A8_SRGB;
            case R8_SRGB: return VK_FORMAT_R8_SRGB;
            case BGR8_SRGB: return VK_FORMAT_B8G8R8_SRGB;
            case ABGR8_SRGB_Pack32: return VK_FORMAT_A8B8G8R8_SRGB_PACK32;
            case BGRA8_SRGB: return VK_FORMAT_B8G8R8A8_SRGB;
            case Depth32_SFloat: return VK_FORMAT_D32_SFLOAT;
            case Depth32_SFloat_Stencil8_UInt: return VK_FORMAT_D32_SFLOAT_S8_UINT;
            case Depth24_UNorm_Stencil8_UInt: return VK_FORMAT_D24_UNORM_S8_UINT;
            case Depth16_UNorm: return VK_FORMAT_D16_UNORM;
            case Depth16_UNorm_Stencil8_UInt: return VK_FORMAT_D16_UNORM_S8_UINT;
            default: throw new UnsupportedOperationException(fmt.name());
        }
    }

    @Override
    public int getTopologyEnum(Topology topology) {
        switch (topology) {
            case LineList: return VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case LineStrip: return VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            case TriangleList: return VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case PatchList: return VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
            case PointList: return VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            case LineListAdjacency: return VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY;
            case LineStripAdjacency: return VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY;
            case TriangleFan: return VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
            case TriangleListAdjacency: return VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY;
            case TriangleStrip: return VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case TriangleStripAdjacency: return VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY;
            default: throw new UnsupportedOperationException(topology.name());
        }
    }

    @Override
    public int getIndexTypeEnum(IndexType type) {
        switch (type) {
            case UInt8: return VK_INDEX_TYPE_UINT8;
            case UInt16: return VK_INDEX_TYPE_UINT16;
            case UInt32: return VK_INDEX_TYPE_UINT32;
            default: throw new UnsupportedOperationException(type.name());
        }
    }

    @Override
    public int getImageLoadEnum(VulkanImage.Load load) {
        switch (load) {
            case Clear: return VK_ATTACHMENT_LOAD_OP_CLEAR;
            case Load: return VK_ATTACHMENT_LOAD_OP_LOAD;
            case DontCare: return VK_ATTACHMENT_LOAD_OP_DONT_CARE;
            default: throw new UnsupportedOperationException(load.name());
        }
    }

    @Override
    public int getImageStoreEnum(VulkanImage.Store store) {
        switch (store) {
            case Store: return VK_ATTACHMENT_STORE_OP_STORE;
            case DontCare: return VK_ATTACHMENT_STORE_OP_DONT_CARE;
            default: throw new UnsupportedOperationException(store.name());
        }
    }

}
