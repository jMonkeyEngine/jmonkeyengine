/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

import com.jme3.material.TechniqueDef;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
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
     *//**
     * Supports {@link FrameBuffer FrameBuffers}.
     * <p>
     * OpenGL: Renderer exposes the GL_EXT_framebuffer_object extension.<br>
     * OpenGL ES: Renderer supports OpenGL ES 2.0.
     *//**
     * Supports {@link FrameBuffer FrameBuffers}.
     * <p>
     * OpenGL: Renderer exposes the GL_EXT_framebuffer_object extension.<br>
     * OpenGL ES: Renderer supports OpenGL ES 2.0.
     *//**
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
     * Supports OpenGL 3.3
     */
    OpenGL33,
    /**
     * Supports OpenGL 4.0
     */
    OpenGL40,
    /**
     * Supports OpenGL 4.1
     */
    OpenGL41,
    /**
     * Supports OpenGL 4.2
     */
    OpenGL42,
    /**
     * Supports OpenGL 4.3
     */
    OpenGL43,
    /**
     * Supports OpenGL 4.4
     */
    OpenGL44,
    /**
     * Supports OpenGL 4.5
     */
    OpenGL45,
    /**
     * Do not use.
     * 
     * @deprecated do not use.
     */
    @Deprecated
    Reserved0,
    
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
     * Supports GLSL 4.0
     */
    GLSL400,
    /**
     * Supports GLSL 4.1
     */
    GLSL410,
    /**
     * Supports GLSL 4.2
     */
    GLSL420,
    /**
     * Supports GLSL 4.3
     */
    GLSL430,
    /**
     * Supports GLSL 4.4
     */
    GLSL440,
    /**
     * Supports GLSL 4.5
     */
    GLSL450,
    /**
     * Supports reading from textures inside the vertex shader.
     */
    VertexTextureFetch,

    /**
     * Supports geometry shader.
     */
    GeometryShader,
    /**
     * Supports Tessellation shader
     */
    TesselationShader,
    /**
     * Supports texture arrays
     */
    TextureArray,

    /**
     * Supports texture buffers
     */
    TextureBuffer,

    /**
     * Supports floating point and half textures (Format.RGB16F)
     */
    FloatTexture,
    
    /**
     * Supports integer textures
     */
    IntegerTexture,
    
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
     * Do not use.
     * 
     * @deprecated do not use.
     */
    @Deprecated
    Reserved1,

    /**
     * Supports Non-Power-Of-Two (NPOT) textures and framebuffers
     */
    NonPowerOfTwoTextures,

    /**
     * Supports geometry instancing.
     */
    MeshInstancing,

    /**
     * Supports VAO, or vertex buffer arrays
     */
    VertexBufferArray,

    /**
     * Supports multisampling on the screen
     */
    Multisample,
    
    /**
     * Supports FBO with Depth24Stencil8 image format
     */
    PackedDepthStencilBuffer,
    
    /**
     * Supports sRGB framebuffers and sRGB texture format
     */
    Srgb,
    
    /**
     * Supports blitting framebuffers.
     */
    FrameBufferBlit,
    
    /**
     * Supports {@link Format#DXT1} and sister formats.
     */
    TextureCompressionS3TC,
    
    /**
     * Supports anisotropic texture filtering.
     */
    TextureFilterAnisotropic,
    
    /**
     * Supports {@link Format#ETC1} texture compression.
     */
    TextureCompressionETC1,
    
    /**
     * Supports {@link Format#ETC1} texture compression by uploading
     * the texture as ETC2 (they are backwards compatible).
     */
    TextureCompressionETC2,
    
    /**
     * Supports OpenGL ES 2
     */
    OpenGLES20,
    
    /**
     * Supports RGB8 / RGBA8 textures
     */
    Rgba8,
    
    /**
     * Supports depth textures.
     */
    DepthTexture,
    
    /**
     * Supports 32-bit index buffers.
     */
    IntegerIndexBuffer,
    
    /**
     * Partial support for non-power-of-2 textures, typically found
     * on OpenGL ES 2 devices.
     * <p>
     * Use of NPOT textures is allowed iff:
     * <ul>
     * <li>The {@link com.jme3.texture.Texture.WrapMode} is set to 
     * {@link com.jme3.texture.Texture.WrapMode#EdgeClamp}.</li>
     * <li>Mip-mapping is not used, meaning 
     * {@link com.jme3.texture.Texture.MinFilter} is set to
     * {@link com.jme3.texture.Texture.MinFilter#BilinearNoMipMaps} or 
     * {@link com.jme3.texture.Texture.MinFilter#NearestNoMipMaps}</li>
     * </ul>
     */
    PartialNonPowerOfTwoTextures,
    
    /**
     * When sampling cubemap edges, interpolate between the adjecent faces
     * instead of just sampling one face.
     * <p>
     * Improves the quality of environment mapping.
     */
    SeamlessCubemap,
    
    /**
     * Running with OpenGL 3.2+ core profile.
     * 
     * Compatibility features will not be available.
     */
    CoreProfile,
    
    /**
     * GPU can provide and accept binary shaders.
     */
    BinaryShader,
    /**
     * Supporting working with UniformBufferObject.
     */
    UniformBufferObject,
    /**
     * Supporting working with ShaderStorageBufferObjects.
     */
    ShaderStorageBufferObject,
    /**
     * Supports TimerQueries
     * GL3.3 is available or (GL_ARB_timer_query) or (GL_EXT_timer_query)
     * 
     * TODO
     * GLES: add support for (EXT_disjoint_timer_query)
     */
    TimerQuery,
    /**
     * Supports for GL_ANY_SAMPLES_PASSED query.
     * GL3.3 is available or (GL_ARB_occlusion_query2)
     * 
     */
    OcclusionQuery2,
    /**
     * Supports for GL_ANY_SAMPLES_PASSED_CONSERVATIVE query.
     * GL4.3 is available or (GL_ARB_ES3_compatibility)
     * 
     */
    OcclusionQueryConservative,
    /**
     * Support for transform feedback, primitives_generated que
     * GL3.0 is available or (GL_EXT_transform_feedback)
     */
    TransformFeedback,
    /**
     * Support for additional transform feedback operations
     * GL4.0 is available or (GL_ARB_transform_feedback2)
     */
    TransformFeedback2,
     /**
     * Support for additional transform feedback operations
     * GL4.0 is available or (GL_ARB_transform_feedback3)
     */
    TransformFeedback3,
    
    /**
     * Supports OpenGL ES 3.0
     */
    OpenGLES30,

    /**
     * Supports GLSL 3.0
     */
    GLSL300,

    /**
     * Supports OpenGL ES 3.1
     */
    OpenGLES31,

    /**
     * Supports GLSL 3.1
     */
    GLSL310,

    /**
     * Supports OpenGL ES 3.2
     */
    OpenGLES32,

    /**
     * Supports GLSL 3.2
     */
    GLSL320,

    /**
     * Explicit support of depth 24 textures
     */
    Depth24,     

    
    UnpackRowLength

    ;

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
            case Depth24Stencil8:
                return caps.contains(Caps.PackedDepthStencilBuffer);
            case Depth32F:
                return caps.contains(Caps.FloatDepthBuffer);
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
    
    private static boolean supportsColorBuffer(Collection<Caps> caps, RenderBuffer colorBuf){
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

        RenderBuffer depthBuf = fb.getDepthBuffer();
        if (depthBuf != null){
            Format depthFmt = depthBuf.getFormat();
            if (!depthFmt.isDepthFormat()){
                return false;
            }else{
                if (depthFmt == Format.Depth32F
                 && !caps.contains(Caps.FloatDepthBuffer))
                    return false;
                
                if (depthFmt == Format.Depth24Stencil8
                 && !caps.contains(Caps.PackedDepthStencilBuffer))
                    return false;
            }
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++){
            if (!supportsColorBuffer(caps, fb.getColorBuffer(i))){
                return false;
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
        for (ShaderSource source : shader.getSources()) {
            if (source.getLanguage().startsWith("GLSL")) {
                int ver = Integer.parseInt(source.getLanguage().substring(4));
                switch (ver) {
                    case 100:
                        if (!caps.contains(Caps.GLSL100)) return false;
                    case 110:
                        if (!caps.contains(Caps.GLSL110)) return false;
                    case 120:
                        if (!caps.contains(Caps.GLSL120)) return false;
                    case 130:
                        if (!caps.contains(Caps.GLSL130)) return false;
                    case 140:
                        if (!caps.contains(Caps.GLSL140)) return false;
                    case 150:
                        if (!caps.contains(Caps.GLSL150)) return false;
                    case 330:
                        if (!caps.contains(Caps.GLSL330)) return false;
                    case 400:
                        if (!caps.contains(Caps.GLSL400)) return false;
                    case 410:
                        if (!caps.contains(Caps.GLSL410)) return false;
                    case 420:
                        if (!caps.contains(Caps.GLSL420)) return false;
                    case 430:
                        if (!caps.contains(Caps.GLSL430)) return false;
                    case 440:
                        if (!caps.contains(Caps.GLSL440)) return false;
                    case 450:
                        if (!caps.contains(Caps.GLSL450)) return false;
                    default:
                        return false;
                }
            }
        }
        if(shader.getBoundTransformFeedbackMode() != null &&
           shader.getBoundTransformFeedbackMode() != TechniqueDef.TransformFeedbackMode.Off &&
           !caps.contains(Caps.TransformFeedback)) return false;
            
        return true;
    }
    /**
     * Returns true if given the renderer capabilities, the query object
     * can be supported by the renderer.
     * 
     * @param caps The collection of renderer capabilities {@link Renderer#getCaps() }.
     * @param query The query object to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, QueryObject query) {
        switch(query.getType()) {
            case SamplesPassed:
                //GL1.5 is available or (GL_ARB_occlusion_query)
                return true;
            case AnySamplesPassed: 
                return caps.contains(Caps.OcclusionQuery2);
            case TimeElapsed: 
                return caps.contains(Caps.TimerQuery);
            case TransformFeedbackPrimitivesGenerated:
                return caps.contains(Caps.TransformFeedback);
            case PrimitivesGenerated: 
                return caps.contains(Caps.TransformFeedback);
            case AnySamplesPassedConservative: 
                return caps.contains(Caps.OcclusionQueryConservative);
            default:
                throw new UnsupportedOperationException("Unrecognized query object type: " + query.getType());
        }
    }
}
