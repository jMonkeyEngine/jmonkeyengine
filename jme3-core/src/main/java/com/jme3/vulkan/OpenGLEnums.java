package com.jme3.vulkan;

import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.pipeline.Topology;
import org.graalvm.compiler.lir.amd64.AMD64BinaryConsumer;

import static org.lwjgl.opengl.GL45.*;

public class OpenGLEnums implements EnumInterpreter {
    
    public static final OpenGLEnums instance = new OpenGLEnums();
    
    private OpenGLEnums() {}
    
    @Override
    public int getFormatEnum(Format fmt) {
        switch (fmt) {
            case RGBA32_SFloat: return GL_RGBA32F;
            case RGB32_SFloat: return GL_RGB32F;
            case RG32_SFloat: return GL_RG32F;
            case R32_SFloat: return GL_R32F;
            case RGBA8_SRGB: return GL_RGBA8;
            case R8_SRGB: return GL_R8;
            case BGR8_SRGB: return GL_BGR;
            case BGRA8_SRGB: return GL_BGRA;
            case Depth32_SFloat: return GL_DEPTH;
            case Depth32_SFloat_Stencil8_UInt: return GL_DEPTH32F_STENCIL8;
            case Depth24_UNorm_Stencil8_UInt: return GL_DEPTH24_STENCIL8;
            case Depth16_UNorm: return GL_DEPTH_COMPONENT16;
            default: throw new UnsupportedOperationException(fmt.name());
        }
    }

    @Override
    public int getTopologyEnum(Topology topology) {
        switch (topology) {
            case LineList: return GL_LINES;
            case LineStrip: return GL_LINE_STRIP;
            case LineLoop: return GL_LINE_LOOP;
            case TriangleList: return GL_TRIANGLES;
            case PatchList: return GL_PATCHES;
            case PointList: return GL_POINTS;
            case LineListAdjacency: return GL_LINES_ADJACENCY;
            case LineStripAdjacency: return GL_LINE_STRIP_ADJACENCY;
            case TriangleFan: return GL_TRIANGLE_FAN;
            case TriangleListAdjacency: return GL_TRIANGLES_ADJACENCY;
            case TriangleStrip: return GL_TRIANGLE_STRIP;
            case TriangleStripAdjacency: return GL_TRIANGLE_STRIP_ADJACENCY;
            default: throw new UnsupportedOperationException(topology.name());
        }
    }

    @Override
    public int getIndexTypeEnum(IndexType type) {
        switch (type) {
            case UInt8: return GL_UNSIGNED_BYTE;
            case UInt16: return GL_UNSIGNED_SHORT;
            case UInt32: return GL_UNSIGNED_INT;
            default: throw new UnsupportedOperationException(type.name());
        }
    }

}
