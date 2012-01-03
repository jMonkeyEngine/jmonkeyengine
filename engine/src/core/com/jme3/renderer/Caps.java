/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme3.renderer;

import com.jme3.shader.Shader;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.RenderBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import java.util.Collection;

/**
 * <code>Caps</code> is an enum specifying a capability that the {@link Renderer}
 * supports.
 * 
 * @author Kirill Vainer
 */
public enum Caps {

    /**
     * Supports {@link FrameBuffer FrameBuffers}.
     * <p>
     * OpenGL: Renderer exposes the GL_EXT_framebuffer_object extension.<br>
     * OpenGL ES: Renderer supports OpenGL ES 2.0.
     */
    FrameBuffer,

    /**
     * Supports framebuffer Multiple Render Targets (MRT)
     * <p>
     * OpenGL: Renderer exposes the GL_ARB_draw_buffers extension
     */
    FrameBufferMRT,

    /**
     * Supports framebuffer multi-sampling
     * <p>
     * OpenGL: Renderer exposes the GL EXT framebuffer multisample extension<br>
     * OpenGL ES: Renderer exposes GL_APPLE_framebuffer_multisample or
     * GL_ANGLE_framebuffer_multisample.
     */
    FrameBufferMultisample,

    /**
     * Supports texture multi-sampling
     * <p>
     * OpenGL: Renderer exposes the GL_ARB_texture_multisample extension<br>
     * OpenGL ES: Renderer exposes the GL_IMG_multisampled_render_to_texture
     * extension.
     */
    TextureMultisample,

    /**
     * Supports OpenGL 2.0 or OpenGL ES 2.0.
     */
    OpenGL20,
    
    /**
     * Supports OpenGL 2.1
     */
    OpenGL21,
    
    /**
     * Supports OpenGL 3.0
     */
    OpenGL30,
    
    /**
     * Supports OpenGL 3.1
     */
    OpenGL31,
    
    /**
     * Supports OpenGL 3.2
     */
    OpenGL32,

    /**
     * Supports OpenGL ARB program.
     * <p>
     * OpenGL: Renderer exposes ARB_vertex_program and ARB_fragment_program
     * extensions.
     */
    ARBprogram,
    
    /**
     * Supports GLSL 1.0
     */
    GLSL100,
    
    /**
     * Supports GLSL 1.1
     */
    GLSL110,
    
    /**
     * Supports GLSL 1.2
     */
    GLSL120,
    
    /**
     * Supports GLSL 1.3
     */
    GLSL130,
    
    /**
     * Supports GLSL 1.4
     */
    GLSL140,
    
    /**
     * Supports GLSL 1.5
     */
    GLSL150,
    
    /**
     * Supports GLSL 3.3
     */
    GLSL330,

    /**
     * Supports reading from textures inside the vertex shader.
     */
    VertexTextureFetch,

    /**
     * Supports geometry shader.
     */
    GeometryShader,

    /**
     * Supports texture arrays
     */
    TextureArray,

    /**
     * Supports texture buffers
     */
    TextureBuffer,

    /**
     * Supports floating point textures (Format.RGB16F)
     */
    FloatTexture,

    /**
     * Supports floating point FBO color buffers (Format.RGB16F)
     */
    FloatColorBuffer,

    /**
     * Supports floating point depth buffer
     */
    FloatDepthBuffer,

    /**
     * Supports Format.RGB111110F for textures
     */
    PackedFloatTexture,

    /**
     * Supports Format.RGB9E5 for textures
     */
    SharedExponentTexture,

    /**
     * Supports Format.RGB111110F for FBO color buffers
     */
    PackedFloatColorBuffer,

    /**
     * Supports Format.RGB9E5 for FBO color buffers
     */
    SharedExponentColorBuffer,

    /**
     * Supports Format.LATC for textures, this includes
     * support for ATI's 3Dc texture compression.
     */
    TextureCompressionLATC,

    /**
     * Supports Non-Power-Of-Two (NPOT) textures and framebuffers
     */
    NonPowerOfTwoTextures,

    /// Vertex Buffer features
    MeshInstancing,

    /**
     * Supports VAO, or vertex buffer arrays
     */
    VertexBufferArray,

    /**
     * Supports multisampling on the screen
     */
    Multisample;

    /**
     * Returns true if given the renderer capabilities, the texture
     * can be supported by the renderer.
     * <p>
     * This only checks the format of the texture, non-power-of-2
     * textures are scaled automatically inside the renderer 
     * if are not supported natively.
     * 
     * @param caps The collection of renderer capabilities {@link Renderer#getCaps() }.
     * @param tex The texture to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, Texture tex){
        if (tex.getType() == Texture.Type.TwoDimensionalArray
         && !caps.contains(Caps.TextureArray))
            return false;

        Image img = tex.getImage();
        if (img == null)
            return true;

        Format fmt = img.getFormat();
        switch (fmt){
            case Depth32F:
                return caps.contains(Caps.FloatDepthBuffer);
            case LATC:
                return caps.contains(Caps.TextureCompressionLATC);
            case RGB16F_to_RGB111110F:
            case RGB111110F:
                return caps.contains(Caps.PackedFloatTexture);
            case RGB16F_to_RGB9E5:
            case RGB9E5:
                return caps.contains(Caps.SharedExponentTexture);
            default:
                if (fmt.isFloatingPont())
                    return caps.contains(Caps.FloatTexture);
                        
                return true;
        }
    }

    /**
     * Returns true if given the renderer capabilities, the framebuffer
     * can be supported by the renderer.
     * 
     * @param caps The collection of renderer capabilities {@link Renderer#getCaps() }.
     * @param fb The framebuffer to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, FrameBuffer fb){
        if (!caps.contains(Caps.FrameBuffer))
            return false;

        if (fb.getSamples() > 1
         && !caps.contains(Caps.FrameBufferMultisample))
            return false;

        RenderBuffer colorBuf = fb.getColorBuffer();
        RenderBuffer depthBuf = fb.getDepthBuffer();

        if (depthBuf != null){
            Format depthFmt = depthBuf.getFormat();
            if (!depthFmt.isDepthFormat()){
                return false;
            }else{
                if (depthFmt == Format.Depth32F
                 && !caps.contains(Caps.FloatDepthBuffer))
                    return false;
            }
        }
        if (colorBuf != null){
            Format colorFmt = colorBuf.getFormat();
            if (colorFmt.isDepthFormat())
                return false;

            if (colorFmt.isCompressed())
                return false;

            switch (colorFmt){
                case RGB111110F:
                    return caps.contains(Caps.PackedFloatColorBuffer);
                case RGB16F_to_RGB111110F:
                case RGB16F_to_RGB9E5:
                case RGB9E5:
                    return false;
                default:
                    if (colorFmt.isFloatingPont())
                        return caps.contains(Caps.FloatColorBuffer);

                    return true;
            }
        }
        return true;
    }

    /**
     * Returns true if given the renderer capabilities, the shader
     * can be supported by the renderer.
     * 
     * @param caps The collection of renderer capabilities {@link Renderer#getCaps() }.
     * @param shader The shader to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, Shader shader){
        String lang = shader.getLanguage();
        if (lang.startsWith("GLSL")){
            int ver = Integer.parseInt(lang.substring(4));
            switch (ver){
                case 100:
                    return caps.contains(Caps.GLSL100);
                case 110:
                    return caps.contains(Caps.GLSL110);
                case 120:
                    return caps.contains(Caps.GLSL120);
                case 130:
                    return caps.contains(Caps.GLSL130);
                case 140:
                    return caps.contains(Caps.GLSL140);
                case 150:
                    return caps.contains(Caps.GLSL150);
                case 330:
                    return caps.contains(Caps.GLSL330);
                default:
                    return false;
            }
        }
        return false;
    }

}
