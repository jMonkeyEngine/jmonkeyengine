package com.jme3.vulkan.formats;

import com.jme3.vulkan.pipeline.Topology;

import static org.lwjgl.opengl.GL45.*;

public class OpenGLEnums {

    public static void setup() {

        // format
        Format.RGBA32_SFloat.set(GL_RGBA32F);
        Format.RGB32_SFloat.set(GL_RGB32F);
        Format.RG32_SFloat.set(GL_RG32F);
        Format.R32_SFloat.set(GL_R32F);
        Format.RGBA8_SRGB.set(GL_RGBA8);
        Format.R8_SRGB.set(GL_R8);
        Format.BGR8_SRGB.set(GL_BGR);
        Format.BGRA8_SRGB.set(GL_BGRA);
        Format.Depth32_SFloat.set(GL_DEPTH);
        Format.Depth32_SFloat_Stencil8_UInt.set(GL_DEPTH32F_STENCIL8);
        Format.Depth24_UNorm_Stencil8_UInt.set(GL_DEPTH24_STENCIL8);
        Format.Depth16_UNorm.set(GL_DEPTH_COMPONENT16);

        // topology
        Topology.LineList.set(GL_LINES);
        Topology.LineStrip.set(GL_LINE_STRIP);
        Topology.LineLoop.set(GL_LINE_LOOP);
        Topology.TriangleList.set(GL_TRIANGLES);
        Topology.PatchList.set(GL_PATCHES);
        Topology.PointList.set(GL_POINTS);
        Topology.LineListAdjacency.set(GL_LINES_ADJACENCY);
        Topology.LineStripAdjacency.set(GL_LINE_STRIP_ADJACENCY);
        Topology.TriangleFan.set(GL_TRIANGLE_FAN);
        Topology.TriangleListAdjacency.set(GL_TRIANGLES_ADJACENCY);
        Topology.TriangleStrip.set(GL_TRIANGLE_STRIP);
        Topology.TriangleStripAdjacency.set(GL_TRIANGLE_STRIP_ADJACENCY);

    }

}
