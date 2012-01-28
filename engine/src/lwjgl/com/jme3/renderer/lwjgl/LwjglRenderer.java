/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.renderer.lwjgl;

import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.StencilOperation;
import com.jme3.material.RenderState.TestFunction;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.shader.Attribute;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.shader.Shader.ShaderType;
import com.jme3.shader.Uniform;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.RenderBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.ListMap;
import com.jme3.util.NativeObjectManager;
import com.jme3.util.SafeArrayList;
import java.nio.*;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3tools.converters.MipMapGenerator;
import static org.lwjgl.opengl.ARBTextureMultisample.*;
import static org.lwjgl.opengl.EXTFramebufferBlit.*;
import static org.lwjgl.opengl.EXTFramebufferMultisample.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import org.lwjgl.opengl.*;
//import static org.lwjgl.opengl.ARBDrawInstanced.*;

public class LwjglRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(LwjglRenderer.class.getName());
    private static final boolean VALIDATE_SHADER = false;
    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final IntBuffer intBuf1 = BufferUtils.createIntBuffer(1);
    private final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private final RenderContext context = new RenderContext();
    private final NativeObjectManager objManager = new NativeObjectManager();
    private final EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    // current state
    private Shader boundShader;
    private int initialDrawBuf, initialReadBuf;
    private int glslVer;
    private int vertexTextureUnits;
    private int fragTextureUnits;
    private int vertexUniforms;
    private int fragUniforms;
    private int vertexAttribs;
    private int maxFBOSamples;
    private int maxFBOAttachs;
    private int maxMRTFBOAttachs;
    private int maxRBSize;
    private int maxTexSize;
    private int maxCubeTexSize;
    private int maxVertCount;
    private int maxTriCount;
    private int maxColorTexSamples;
    private int maxDepthTexSamples;
    private boolean tdc;
    private FrameBuffer lastFb = null;
    private FrameBuffer mainFbOverride = null;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;

    public LwjglRenderer() {
    }

    protected void updateNameBuffer() {
        int len = stringBuf.length();

        nameBuf.position(0);
        nameBuf.limit(len);
        for (int i = 0; i < len; i++) {
            nameBuf.put((byte) stringBuf.charAt(i));
        }

        nameBuf.rewind();
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public EnumSet<Caps> getCaps() {
        return caps;
    }

    @SuppressWarnings("fallthrough")
    public void initialize() {
        ContextCapabilities ctxCaps = GLContext.getCapabilities();
        if (ctxCaps.OpenGL20) {
            caps.add(Caps.OpenGL20);
            if (ctxCaps.OpenGL21) {
                caps.add(Caps.OpenGL21);
                if (ctxCaps.OpenGL30) {
                    caps.add(Caps.OpenGL30);
                    if (ctxCaps.OpenGL31) {
                        caps.add(Caps.OpenGL31);
                        if (ctxCaps.OpenGL32) {
                            caps.add(Caps.OpenGL32);
                        }
                    }
                }
            }
        }

        String versionStr = null;
        if (ctxCaps.OpenGL20) {
            versionStr = glGetString(GL_SHADING_LANGUAGE_VERSION);
        }
        if (versionStr == null || versionStr.equals("")) {
            glslVer = -1;
            throw new UnsupportedOperationException("GLSL and OpenGL2 is "
                    + "required for the LWJGL "
                    + "renderer!");
        }

        // Fix issue in TestRenderToMemory when GL_FRONT is the main
        // buffer being used.
        initialDrawBuf = glGetInteger(GL_DRAW_BUFFER);
        initialReadBuf = glGetInteger(GL_READ_BUFFER);

        // XXX: This has to be GL_BACK for canvas on Mac
        // Since initialDrawBuf is GL_FRONT for pbuffer, gotta
        // change this value later on ...
//        initialDrawBuf = GL_BACK;
//        initialReadBuf = GL_BACK;

        int spaceIdx = versionStr.indexOf(" ");
        if (spaceIdx >= 1) {
            versionStr = versionStr.substring(0, spaceIdx);
        }

        float version = Float.parseFloat(versionStr);
        glslVer = (int) (version * 100);

        switch (glslVer) {
            default:
                if (glslVer < 400) {
                    break;
                }

            // so that future OpenGL revisions wont break jme3

            // fall through intentional
            case 400:
            case 330:
            case 150:
                caps.add(Caps.GLSL150);
            case 140:
                caps.add(Caps.GLSL140);
            case 130:
                caps.add(Caps.GLSL130);
            case 120:
                caps.add(Caps.GLSL120);
            case 110:
                caps.add(Caps.GLSL110);
            case 100:
                caps.add(Caps.GLSL100);
                break;
        }

        if (!caps.contains(Caps.GLSL100)) {
            logger.log(Level.WARNING, "Force-adding GLSL100 support, since OpenGL2 is supported.");
            caps.add(Caps.GLSL100);
        }

        glGetInteger(GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, intBuf16);
        vertexTextureUnits = intBuf16.get(0);
        logger.log(Level.FINER, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS, intBuf16);
        fragTextureUnits = intBuf16.get(0);
        logger.log(Level.FINER, "Texture Units: {0}", fragTextureUnits);

        glGetInteger(GL_MAX_VERTEX_UNIFORM_COMPONENTS, intBuf16);
        vertexUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);

        glGetInteger(GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, intBuf16);
        fragUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);

        glGetInteger(GL_MAX_VERTEX_ATTRIBS, intBuf16);
        vertexAttribs = intBuf16.get(0);
        logger.log(Level.FINER, "Vertex Attributes: {0}", vertexAttribs);

        glGetInteger(GL_SUBPIXEL_BITS, intBuf16);
        int subpixelBits = intBuf16.get(0);
        logger.log(Level.FINER, "Subpixel Bits: {0}", subpixelBits);

        glGetInteger(GL_MAX_ELEMENTS_VERTICES, intBuf16);
        maxVertCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);

        glGetInteger(GL_MAX_ELEMENTS_INDICES, intBuf16);
        maxTriCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);

        glGetInteger(GL_MAX_TEXTURE_SIZE, intBuf16);
        maxTexSize = intBuf16.get(0);
        logger.log(Level.FINER, "Maximum Texture Resolution: {0}", maxTexSize);

        glGetInteger(GL_MAX_CUBE_MAP_TEXTURE_SIZE, intBuf16);
        maxCubeTexSize = intBuf16.get(0);
        logger.log(Level.FINER, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);

        if (ctxCaps.GL_ARB_color_buffer_float) {
            // XXX: Require both 16 and 32 bit float support for FloatColorBuffer.
            if (ctxCaps.GL_ARB_half_float_pixel) {
                caps.add(Caps.FloatColorBuffer);
            }
        }

        if (ctxCaps.GL_ARB_depth_buffer_float) {
            caps.add(Caps.FloatDepthBuffer);
        }

        if (ctxCaps.GL_ARB_draw_instanced) {
            caps.add(Caps.MeshInstancing);
        }

        if (ctxCaps.GL_ARB_fragment_program) {
            caps.add(Caps.ARBprogram);
        }

        if (ctxCaps.GL_ARB_texture_buffer_object) {
            caps.add(Caps.TextureBuffer);
        }

        if (ctxCaps.GL_ARB_texture_float) {
            if (ctxCaps.GL_ARB_half_float_pixel) {
                caps.add(Caps.FloatTexture);
            }
        }

        if (ctxCaps.GL_ARB_vertex_array_object) {
            caps.add(Caps.VertexBufferArray);
        }

        if (ctxCaps.GL_ARB_texture_non_power_of_two) {
            caps.add(Caps.NonPowerOfTwoTextures);
        } else {
            logger.log(Level.WARNING, "Your graphics card does not "
                    + "support non-power-of-2 textures. "
                    + "Some features might not work.");
        }

        boolean latc = ctxCaps.GL_EXT_texture_compression_latc;
        boolean atdc = ctxCaps.GL_ATI_texture_compression_3dc;
        if (latc || atdc) {
            caps.add(Caps.TextureCompressionLATC);
            if (atdc && !latc) {
                tdc = true;
            }
        }

        if (ctxCaps.GL_EXT_packed_float) {
            caps.add(Caps.PackedFloatColorBuffer);
            if (ctxCaps.GL_ARB_half_float_pixel) {
                // because textures are usually uploaded as RGB16F
                // need half-float pixel
                caps.add(Caps.PackedFloatTexture);
            }
        }

        if (ctxCaps.GL_EXT_texture_array) {
            caps.add(Caps.TextureArray);
        }

        if (ctxCaps.GL_EXT_texture_shared_exponent) {
            caps.add(Caps.SharedExponentTexture);
        }

        if (ctxCaps.GL_EXT_framebuffer_object) {
            caps.add(Caps.FrameBuffer);

            glGetInteger(GL_MAX_RENDERBUFFER_SIZE_EXT, intBuf16);
            maxRBSize = intBuf16.get(0);
            logger.log(Level.FINER, "FBO RB Max Size: {0}", maxRBSize);

            glGetInteger(GL_MAX_COLOR_ATTACHMENTS_EXT, intBuf16);
            maxFBOAttachs = intBuf16.get(0);
            logger.log(Level.FINER, "FBO Max renderbuffers: {0}", maxFBOAttachs);

            if (ctxCaps.GL_EXT_framebuffer_multisample) {
                caps.add(Caps.FrameBufferMultisample);

                glGetInteger(GL_MAX_SAMPLES_EXT, intBuf16);
                maxFBOSamples = intBuf16.get(0);
                logger.log(Level.FINER, "FBO Max Samples: {0}", maxFBOSamples);
            }

            if (ctxCaps.GL_ARB_texture_multisample) {
                caps.add(Caps.TextureMultisample);

                glGetInteger(GL_MAX_COLOR_TEXTURE_SAMPLES, intBuf16);
                maxColorTexSamples = intBuf16.get(0);
                logger.log(Level.FINER, "Texture Multisample Color Samples: {0}", maxColorTexSamples);

                glGetInteger(GL_MAX_DEPTH_TEXTURE_SAMPLES, intBuf16);
                maxDepthTexSamples = intBuf16.get(0);
                logger.log(Level.FINER, "Texture Multisample Depth Samples: {0}", maxDepthTexSamples);
            }

            if (ctxCaps.GL_ARB_draw_buffers) {
                caps.add(Caps.FrameBufferMRT);
                glGetInteger(ARBDrawBuffers.GL_MAX_DRAW_BUFFERS_ARB, intBuf16);
                maxMRTFBOAttachs = intBuf16.get(0);
                logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
            }
        }

        if (ctxCaps.GL_ARB_multisample) {
            glGetInteger(ARBMultisample.GL_SAMPLE_BUFFERS_ARB, intBuf16);
            boolean available = intBuf16.get(0) != 0;
            glGetInteger(ARBMultisample.GL_SAMPLES_ARB, intBuf16);
            int samples = intBuf16.get(0);
            logger.log(Level.FINER, "Samples: {0}", samples);
            boolean enabled = glIsEnabled(ARBMultisample.GL_MULTISAMPLE_ARB);
            if (samples > 0 && available && !enabled) {
                glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
            }
        }

        logger.log(Level.INFO, "Caps: {0}", caps);
    }

    public void invalidateState() {
        context.reset();
        boundShader = null;
        lastFb = null;

        initialDrawBuf = glGetInteger(GL_DRAW_BUFFER);
        initialReadBuf = glGetInteger(GL_READ_BUFFER);
    }

    public void resetGLObjects() {
        logger.log(Level.INFO, "Reseting objects and invalidating state");
        objManager.resetObjects();
        statistics.clearMemory();
        invalidateState();
    }

    public void cleanup() {
        logger.log(Level.INFO, "Deleting objects and invalidating state");
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
        invalidateState();
    }

    private void checkCap(Caps cap) {
        if (!caps.contains(cap)) {
            throw new UnsupportedOperationException("Required capability missing: " + cap.name());
        }
    }

    /*********************************************************************\
    |* Render State                                                      *|
    \*********************************************************************/
    public void setDepthRange(float start, float end) {
        glDepthRange(start, end);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            //See explanations of the depth below, we must enable color write to be able to clear the color buffer
            if (context.colorWriteEnabled == false) {
                glColorMask(true, true, true, true);
                context.colorWriteEnabled = true;
            }
            bits = GL_COLOR_BUFFER_BIT;
        }
        if (depth) {

            //glClear(GL_DEPTH_BUFFER_BIT) seems to not work when glDepthMask is false
            //here s some link on openl board
            //http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=257223
            //if depth clear is requested, we enable the depthMask
            if (context.depthWriteEnabled == false) {
                glDepthMask(true);
                context.depthWriteEnabled = true;
            }
            bits |= GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            bits |= GL_STENCIL_BUFFER_BIT;
        }
        if (bits != 0) {
            glClear(bits);
        }
    }

    public void setBackgroundColor(ColorRGBA color) {
        glClearColor(color.r, color.g, color.b, color.a);
    }

    public void setAlphaToCoverage(boolean value) {
        if (value) {
            glEnable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
        } else {
            glDisable(ARBMultisample.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
        }
    }

    public void applyRenderState(RenderState state) {
        if (state.isWireframe() && !context.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            context.wireframe = true;
        } else if (!state.isWireframe() && context.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            context.wireframe = false;
        }

        if (state.isDepthTest() && !context.depthTestEnabled) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            glDisable(GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }

        if (state.isAlphaTest() && !context.alphaTestEnabled) {
            glEnable(GL_ALPHA_TEST);
            glAlphaFunc(GL_GREATER, state.getAlphaFallOff());
            context.alphaTestEnabled = true;
        } else if (!state.isAlphaTest() && context.alphaTestEnabled) {
            glDisable(GL_ALPHA_TEST);
            context.alphaTestEnabled = false;
        }

        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            glDepthMask(true);
            context.depthWriteEnabled = true;
        } else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            glDepthMask(false);
            context.depthWriteEnabled = false;
        }

        if (state.isColorWrite() && !context.colorWriteEnabled) {
            glColorMask(true, true, true, true);
            context.colorWriteEnabled = true;
        } else if (!state.isColorWrite() && context.colorWriteEnabled) {
            glColorMask(false, false, false, false);
            context.colorWriteEnabled = false;
        }

        if (state.isPointSprite() && !context.pointSprite) {
            // Only enable/disable sprite
            if (context.boundTextures[0] != null){
                if (context.boundTextureUnit != 0){
                    glActiveTexture(GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                glEnable(GL_POINT_SPRITE);
                glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
            }
            context.pointSprite = true;
        } else if (!state.isPointSprite() && context.pointSprite) {
            if (context.boundTextures[0] != null){
                if (context.boundTextureUnit != 0){
                    glActiveTexture(GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                glDisable(GL_POINT_SPRITE);
                glDisable(GL_VERTEX_PROGRAM_POINT_SIZE);
                context.pointSprite = false;
            }
        }

        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                glEnable(GL_POLYGON_OFFSET_FILL);
                glPolygonOffset(state.getPolyOffsetFactor(),
                        state.getPolyOffsetUnits());
                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            } else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    glPolygonOffset(state.getPolyOffsetFactor(),
                            state.getPolyOffsetUnits());
                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        } else {
            if (context.polyOffsetEnabled) {
                glDisable(GL_POLYGON_OFFSET_FILL);
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }

        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                glDisable(GL_CULL_FACE);
            } else {
                glEnable(GL_CULL_FACE);
            }

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    glCullFace(GL_BACK);
                    break;
                case Front:
                    glCullFace(GL_FRONT);
                    break;
                case FrontAndBack:
                    glCullFace(GL_FRONT_AND_BACK);
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                glDisable(GL_BLEND);
            } else {
                glEnable(GL_BLEND);
                switch (state.getBlendMode()) {
                    case Off:
                        break;
                    case Additive:
                        glBlendFunc(GL_ONE, GL_ONE);
                        break;
                    case AlphaAdditive:
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
                        break;
                    case Color:
                        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_COLOR);
                        break;
                    case Alpha:
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case PremultAlpha:
                        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case Modulate:
                        glBlendFunc(GL_DST_COLOR, GL_ZERO);
                        break;
                    case ModulateX2:
                        glBlendFunc(GL_DST_COLOR, GL_SRC_COLOR);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unrecognized blend mode: "
                                + state.getBlendMode());
                }
            }

            context.blendMode = state.getBlendMode();
        }

        if (context.stencilTest != state.isStencilTest()
                || context.frontStencilStencilFailOperation != state.getFrontStencilStencilFailOperation()
                || context.frontStencilDepthFailOperation != state.getFrontStencilDepthFailOperation()
                || context.frontStencilDepthPassOperation != state.getFrontStencilDepthPassOperation()
                || context.backStencilStencilFailOperation != state.getBackStencilStencilFailOperation()
                || context.backStencilDepthFailOperation != state.getBackStencilDepthFailOperation()
                || context.backStencilDepthPassOperation != state.getBackStencilDepthPassOperation()
                || context.frontStencilFunction != state.getFrontStencilFunction()
                || context.backStencilFunction != state.getBackStencilFunction()) {

            context.frontStencilStencilFailOperation = state.getFrontStencilStencilFailOperation();   //terrible looking, I know
            context.frontStencilDepthFailOperation = state.getFrontStencilDepthFailOperation();
            context.frontStencilDepthPassOperation = state.getFrontStencilDepthPassOperation();
            context.backStencilStencilFailOperation = state.getBackStencilStencilFailOperation();
            context.backStencilDepthFailOperation = state.getBackStencilDepthFailOperation();
            context.backStencilDepthPassOperation = state.getBackStencilDepthPassOperation();
            context.frontStencilFunction = state.getFrontStencilFunction();
            context.backStencilFunction = state.getBackStencilFunction();

            if (state.isStencilTest()) {
                glEnable(GL_STENCIL_TEST);
                glStencilOpSeparate(GL_FRONT,
                        convertStencilOperation(state.getFrontStencilStencilFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthPassOperation()));
                glStencilOpSeparate(GL_BACK,
                        convertStencilOperation(state.getBackStencilStencilFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthPassOperation()));
                glStencilFuncSeparate(GL_FRONT,
                        convertTestFunction(state.getFrontStencilFunction()),
                        0, Integer.MAX_VALUE);
                glStencilFuncSeparate(GL_BACK,
                        convertTestFunction(state.getBackStencilFunction()),
                        0, Integer.MAX_VALUE);
            } else {
                glDisable(GL_STENCIL_TEST);
            }
        }
    }

    private int convertStencilOperation(StencilOperation stencilOp) {
        switch (stencilOp) {
            case Keep:
                return GL_KEEP;
            case Zero:
                return GL_ZERO;
            case Replace:
                return GL_REPLACE;
            case Increment:
                return GL_INCR;
            case IncrementWrap:
                return GL_INCR_WRAP;
            case Decrement:
                return GL_DECR;
            case DecrementWrap:
                return GL_DECR_WRAP;
            case Invert:
                return GL_INVERT;
            default:
                throw new UnsupportedOperationException("Unrecognized stencil operation: " + stencilOp);
        }
    }

    private int convertTestFunction(TestFunction testFunc) {
        switch (testFunc) {
            case Never:
                return GL_NEVER;
            case Less:
                return GL_LESS;
            case LessOrEqual:
                return GL_LEQUAL;
            case Greater:
                return GL_GREATER;
            case GreaterOrEqual:
                return GL_GEQUAL;
            case Equal:
                return GL_EQUAL;
            case NotEqual:
                return GL_NOTEQUAL;
            case Always:
                return GL_ALWAYS;
            default:
                throw new UnsupportedOperationException("Unrecognized test function: " + testFunc);
        }
    }

    /*********************************************************************\
    |* Camera and World transforms                                       *|
    \*********************************************************************/
    public void setViewPort(int x, int y, int w, int h) {
        if (x != vpX || vpY != y || vpW != w || vpH != h) {
            glViewport(x, y, w, h);
            vpX = x;
            vpY = y;
            vpW = w;
            vpH = h;
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            glEnable(GL_SCISSOR_TEST);
            context.clipRectEnabled = true;
        }
        if (clipX != x || clipY != y || clipW != width || clipH != height) {
            glScissor(x, y, width, height);
            clipX = x;
            clipY = y;
            clipW = width;
            clipH = height;
        }
    }

    public void clearClipRect() {
        if (context.clipRectEnabled) {
            glDisable(GL_SCISSOR_TEST);
            context.clipRectEnabled = false;

            clipX = 0;
            clipY = 0;
            clipW = 0;
            clipH = 0;
        }
    }

    public void onFrame() {
        objManager.deleteUnused(this);
//        statistics.clearFrame();
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
    }

    /*********************************************************************\
    |* Shaders                                                           *|
    \*********************************************************************/
    protected void updateUniformLocation(Shader shader, Uniform uniform) {
        stringBuf.setLength(0);
        stringBuf.append(uniform.getName()).append('\0');
        updateNameBuffer();
        int loc = glGetUniformLocation(shader.getId(), nameBuf);
        if (loc < 0) {
            uniform.setLocation(-1);
            // uniform is not declared in shader
            logger.log(Level.INFO, "Uniform {0} is not declared in shader {1}.", new Object[]{uniform.getName(), shader.getSources()});
        } else {
            uniform.setLocation(loc);
        }
    }

    protected void bindProgram(Shader shader){
        int shaderId = shader.getId();
        if (context.boundShaderProgram != shaderId) {
            glUseProgram(shaderId);
            statistics.onShaderUse(shader, true);
            boundShader = shader;
            context.boundShaderProgram = shaderId;
        } else {
            statistics.onShaderUse(shader, false);
        }
    }
    
    protected void updateUniform(Shader shader, Uniform uniform) {
        int shaderId = shader.getId();

        assert uniform.getName() != null;
        assert shader.getId() > 0;

        bindProgram(shader);

        int loc = uniform.getLocation();
        if (loc == -1) {
            return;
        }

        if (loc == -2) {
            // get uniform location
            updateUniformLocation(shader, uniform);
            if (uniform.getLocation() == -1) {
                // not declared, ignore
                uniform.clearUpdateNeeded();
                return;
            }
            loc = uniform.getLocation();
        }

        if (uniform.getVarType() == null) {
            return; // value not set yet..
        }
        statistics.onUniformSet();

        uniform.clearUpdateNeeded();
        FloatBuffer fb;
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else if (val instanceof Vector4f) {
                    Vector4f c = (Vector4f) val;
                    glUniform4f(loc, c.x, c.y, c.z, c.w);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                glUniform1i(loc, b.booleanValue() ? GL_TRUE : GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                glUniformMatrix3(loc, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                glUniformMatrix4(loc, false, fb);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                glUniform1(loc, fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                glUniform2(loc, fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                glUniform3(loc, fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                glUniform4(loc, fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                glUniformMatrix4(loc, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + uniform.getVarType());
        }
    }

    protected void updateShaderUniforms(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
//        for (Uniform uniform : shader.getUniforms()){
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.getValue(i);
            if (uniform.isUpdateNeeded()) {
                updateUniform(shader, uniform);
            }
        }
    }

    protected void resetUniformLocations(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
//        for (Uniform uniform : shader.getUniforms()){
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.getValue(i);
            uniform.reset(); // e.g check location again
        }
    }

    /*
     * (Non-javadoc)
     * Only used for fixed-function. Ignored.
     */
    public void setLighting(LightList list) {
    }

    public int convertShaderType(ShaderType type) {
        switch (type) {
            case Fragment:
                return GL_FRAGMENT_SHADER;
            case Vertex:
                return GL_VERTEX_SHADER;
//            case Geometry:
//                return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new UnsupportedOperationException("Unrecognized shader type.");
        }
    }

    public void updateShaderSourceData(ShaderSource source, String language) {
        int id = source.getId();
        if (id == -1) {
            // create id
            id = glCreateShader(convertShaderType(source.getType()));
            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader.");
            }

            source.setId(id);
        }else{
            throw new RendererException("Cannot recompile shader source");
        }

        // upload shader source
        // merge the defines and source code

        stringBuf.setLength(0);
        if (language.startsWith("GLSL")) {
            int version = Integer.parseInt(language.substring(4));
            if (version > 100) {
                stringBuf.append("#version ");
                stringBuf.append(language.substring(4));
                if (version >= 150) {
                    stringBuf.append(" core");
                }
                stringBuf.append("\n");
            }
        }
        updateNameBuffer();

        byte[] definesCodeData = source.getDefines().getBytes();
        byte[] sourceCodeData = source.getSource().getBytes();
        ByteBuffer codeBuf = BufferUtils.createByteBuffer(nameBuf.limit()
                + definesCodeData.length
                + sourceCodeData.length);
        codeBuf.put(nameBuf);
        codeBuf.put(definesCodeData);
        codeBuf.put(sourceCodeData);
        codeBuf.flip();

        glShaderSource(id, codeBuf);
        glCompileShader(id);

        glGetShader(id, GL_COMPILE_STATUS, intBuf1);

        boolean compiledOK = intBuf1.get(0) == GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            glGetShader(id, GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                glGetShaderInfoLog(id, null, logBuf);
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                // convert to string, etc
                infoLog = new String(logBytes);
            }
        }

        if (compiledOK) {
            if (infoLog != null) {
                logger.log(Level.INFO, "{0} compile success\n{1}",
                        new Object[]{source.getName(), infoLog});
            } else {
                logger.log(Level.FINE, "{0} compile success", source.getName());
            }
        } else {
            logger.log(Level.WARNING, "Bad compile of:\n{0}{1}",
                    new Object[]{source.getDefines(), source.getSource()});
            if (infoLog != null) {
                throw new RendererException("compile error in:" + source + " error:" + infoLog);
            } else {
                throw new RendererException("compile error in:" + source + " error: <not provided>");
            }
        }

        source.clearUpdateNeeded();
        // only usable if compiled
        source.setUsable(compiledOK);
        if (!compiledOK) {
            // make sure to dispose id cause all program's
            // shaders will be cleared later.
            glDeleteShader(id);
        } else {
            // register for cleanup since the ID is usable
            // NOTE: From now on cleanup is handled
            // by the parent shader object so no need
            // to register.
            //objManager.registerForCleanup(source);
        }
    }

    public void updateShaderData(Shader shader) {
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program
            id = glCreateProgram();
            if (id == 0) {
                throw new RendererException("Invalid ID (" + id + ") received when trying to create shader program.");
            }

            shader.setId(id);
            needRegister = true;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.isUpdateNeeded()) {
                updateShaderSourceData(source, shader.getLanguage());
                // shader has been compiled here
            }

            if (!source.isUsable()) {
                // it's useless.. just forget about everything..
                shader.setUsable(false);
                shader.clearUpdateNeeded();
                return;
            }
            glAttachShader(id, source.getId());
        }

        if (caps.contains(Caps.OpenGL30)) {
            // Check if GLSL version is 1.5 for shader
            GL30.glBindFragDataLocation(id, 0, "outFragColor");
        }

        // link shaders to program
        glLinkProgram(id);
        glGetProgram(id, GL_LINK_STATUS, intBuf1);
        boolean linkOK = intBuf1.get(0) == GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            glGetProgram(id, GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                glGetProgramInfoLog(id, null, logBuf);

                // convert to string, etc
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                infoLog = new String(logBytes);
            }
        }

        if (linkOK) {
            if (infoLog != null) {
                logger.log(Level.INFO, "shader link success. \n{0}", infoLog);
            } else {
                logger.fine("shader link success");
            }
        } else {
            if (infoLog != null) {
                throw new RendererException("Shader link failure, shader:" + shader + " info:" + infoLog);
            } else {
                throw new RendererException("Shader link failure, shader:" + shader + " info: <not provided>");
            }
        }

        shader.clearUpdateNeeded();
        if (!linkOK) {
            // failure.. forget about everything
            shader.resetSources();
            shader.setUsable(false);
            deleteShader(shader);
        } else {
            shader.setUsable(true);
            if (needRegister) {
                objManager.registerForCleanup(shader);
                statistics.onNewShader();
            } else {
                // OpenGL spec: uniform locations may change after re-link
                resetUniformLocations(shader);
            }
        }
    }

    public void setShader(Shader shader) {
        if (shader == null) {
            throw new IllegalArgumentException("shader cannot be null");
//            if (context.boundShaderProgram > 0) {
//                glUseProgram(0);
//                statistics.onShaderUse(null, true);
//                context.boundShaderProgram = 0;
//                boundShader = null;
//            }
        } else {
            if (shader.isUpdateNeeded()) {
                updateShaderData(shader);
            }

            // NOTE: might want to check if any of the
            // sources need an update?

            if (!shader.isUsable()) {
                return;
            }

            assert shader.getId() > 0;

            updateShaderUniforms(shader);
            bindProgram(shader);
        }
    }

    public void deleteShaderSource(ShaderSource source) {
        if (source.getId() < 0) {
            logger.warning("Shader source is not uploaded to GPU, cannot delete.");
            return;
        }
        source.setUsable(false);
        source.clearUpdateNeeded();
        glDeleteShader(source.getId());
        source.resetObject();
    }

    public void deleteShader(Shader shader) {
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {
                glDetachShader(shader.getId(), source.getId());
                deleteShaderSource(source);
            }
        }

        // kill all references so sources can be collected
        // if needed.
        shader.resetSources();
        glDeleteProgram(shader.getId());

        statistics.onDeleteShader();
    }

    /*********************************************************************\
    |* Framebuffers                                                      *|
    \*********************************************************************/
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        copyFrameBuffer(src, dst, true);
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
        if (GLContext.getCapabilities().GL_EXT_framebuffer_blit) {
            int srcW = 0;
            int srcH = 0;
            int dstW = 0;
            int dstH = 0;
            int prevFBO = context.boundFBO;

            if (src != null && src.isUpdateNeeded()) {
                updateFrameBuffer(src);
            }

            if (dst != null && dst.isUpdateNeeded()) {
                updateFrameBuffer(dst);
            }

            if (src == null) {
                glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, 0);
//                srcW = viewWidth;
//                srcH = viewHeight;
            } else {
                glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, src.getId());
                srcW = src.getWidth();
                srcH = src.getHeight();
            }
            if (dst == null) {
                glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, 0);
//                dstW = viewWidth;
//                dstH = viewHeight;
            } else {
                glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, dst.getId());
                dstW = dst.getWidth();
                dstH = dst.getHeight();
            }
            int mask = GL_COLOR_BUFFER_BIT;
            if (copyDepth) {
                mask |= GL_DEPTH_BUFFER_BIT;
            }
            glBlitFramebufferEXT(0, 0, srcW, srcH,
                    0, 0, dstW, dstH, mask,
                    GL_NEAREST);


            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, prevFBO);
            try {
                checkFrameBufferError();
            } catch (IllegalStateException ex) {
                logger.log(Level.SEVERE, "Source FBO:\n{0}", src);
                logger.log(Level.SEVERE, "Dest FBO:\n{0}", dst);
                throw ex;
            }
        } else {
            throw new RendererException("EXT_framebuffer_blit required.");
            // TODO: support non-blit copies?
        }
    }
    
    private String getTargetBufferName(int buffer){
        switch (buffer){
            case GL_NONE: return "NONE";
            case GL_FRONT: return "GL_FRONT";
            case GL_BACK: return "GL_BACK";
            default:
                if ( buffer >= GL_COLOR_ATTACHMENT0_EXT
                  && buffer <= GL_COLOR_ATTACHMENT15_EXT){
                    return "GL_COLOR_ATTACHMENT" + 
                                (buffer - GL_COLOR_ATTACHMENT0_EXT);
                }else{
                    return "UNKNOWN? " + buffer;
                }
        }
    }

    private void printRealRenderBufferInfo(FrameBuffer fb, RenderBuffer rb, String name){
        System.out.println("== Renderbuffer " + name + " ==");
        System.out.println("RB ID: " + rb.getId());
        System.out.println("Is proper? " + glIsRenderbufferEXT(rb.getId()));
        
        int attachment = convertAttachmentSlot(rb.getSlot());
        
        int type = glGetFramebufferAttachmentParameterEXT(GL_DRAW_FRAMEBUFFER_EXT,
                                                          attachment,
                                                          GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE_EXT);
        
        int rbName = glGetFramebufferAttachmentParameterEXT(GL_DRAW_FRAMEBUFFER_EXT,
                                                            attachment,
                                                            GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME_EXT);
        
        switch (type){
            case GL_NONE:
                System.out.println("Type: None");
                return; // note: return from method as other queries will be invalid
            case GL_TEXTURE:
                System.out.println("Type: Texture");
                break;
            case GL_RENDERBUFFER_EXT:
                System.out.println("Type: Buffer");
                System.out.println("RB ID: " + rbName);
                break;
        }
        
        
        
    }
    
    private void printRealFrameBufferInfo(FrameBuffer fb) {
        boolean doubleBuffer = glGetBoolean(GL_DOUBLEBUFFER);
        String drawBuf = getTargetBufferName(glGetInteger(GL_DRAW_BUFFER));
        String readBuf = getTargetBufferName(glGetInteger(GL_READ_BUFFER));
        
        int fbId = fb.getId();
        int curDrawBinding = glGetInteger(ARBFramebufferObject.GL_DRAW_FRAMEBUFFER_BINDING);
        int curReadBinding = glGetInteger(ARBFramebufferObject.GL_READ_FRAMEBUFFER_BINDING);
    
        System.out.println("=== OpenGL FBO State ===");
        System.out.println("Context doublebuffered? " + doubleBuffer);
        System.out.println("FBO ID: " + fbId);
        System.out.println("Is proper? " + glIsFramebufferEXT(fbId));
        System.out.println("Is bound to draw? " + (fbId == curDrawBinding));
        System.out.println("Is bound to read? " + (fbId == curReadBinding));
        System.out.println("Draw buffer: " + drawBuf);
        System.out.println("Read buffer: " + readBuf);
        
        if (context.boundFBO != fbId){
            glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, fbId);
            context.boundFBO = fbId;
        }
        
        if (fb.getDepthBuffer() != null){
            printRealRenderBufferInfo(fb, fb.getDepthBuffer(), "Depth");
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++){
            printRealRenderBufferInfo(fb, fb.getColorBuffer(i), "Color" + i);
        }
    }
    
    private void checkFrameBufferError() {
        int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
        switch (status) {
            case GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                //Choose different formats
                throw new IllegalStateException("Framebuffer object format is "
                        + "unsupported by the video hardware.");
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new IllegalStateException("Framebuffer has erronous attachment.");
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new IllegalStateException("Framebuffer doesn't have any renderbuffers attached.");
            case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new IllegalStateException("Framebuffer attachments must have same dimensions.");
            case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new IllegalStateException("Framebuffer attachments must have same formats.");
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new IllegalStateException("Incomplete draw buffer.");
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new IllegalStateException("Incomplete read buffer.");
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT:
                throw new IllegalStateException("Incomplete multisample buffer.");
            default:
                //Programming error; will fail on all hardware
                throw new IllegalStateException("Some video driver error "
                        + "or programming error occured. "
                        + "Framebuffer object status is invalid. ");
        }
    }

    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        int id = rb.getId();
        if (id == -1) {
            glGenRenderbuffersEXT(intBuf1);
            id = intBuf1.get(0);
            rb.setId(id);
        }

        if (context.boundRB != id) {
            glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, id);
            context.boundRB = id;
        }

        if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize) {
            throw new RendererException("Resolution " + fb.getWidth()
                    + ":" + fb.getHeight() + " is not supported.");
        }

        TextureUtil.checkFormatSupported(rb.getFormat());

        if (fb.getSamples() > 1 && GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
            int samples = fb.getSamples();
            if (maxFBOSamples < samples) {
                samples = maxFBOSamples;
            }
            glRenderbufferStorageMultisampleEXT(GL_RENDERBUFFER_EXT,
                    samples,
                    TextureUtil.convertTextureFormat(rb.getFormat()),
                    fb.getWidth(),
                    fb.getHeight());
        } else {
            glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT,
                    TextureUtil.convertTextureFormat(rb.getFormat()),
                    fb.getWidth(),
                    fb.getHeight());
        }
    }

    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == -100) {
            return GL_DEPTH_ATTACHMENT_EXT;
        } else if (attachmentSlot < 0 || attachmentSlot >= 16) {
            throw new UnsupportedOperationException("Invalid FBO attachment slot: " + attachmentSlot);
        }

        return GL_COLOR_ATTACHMENT0_EXT + attachmentSlot;
    }

    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        Texture tex = rb.getTexture();
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels(), 0);

            // NOTE: For depth textures, sets nearest/no-mips mode
            // Required to fix "framebuffer unsupported"
            // for old NVIDIA drivers!
            setupTextureParams(tex);
        }

        glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,
                convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType(), image.getMultiSamples()),
                image.getId(),
                0);
    }

    public void updateFrameBufferAttachment(FrameBuffer fb, RenderBuffer rb) {
        boolean needAttach;
        if (rb.getTexture() == null) {
            // if it hasn't been created yet, then attach is required.
            needAttach = rb.getId() == -1;
            updateRenderBuffer(fb, rb);
        } else {
            needAttach = false;
            updateRenderTexture(fb, rb);
        }
        if (needAttach) {
            glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT,
                    convertAttachmentSlot(rb.getSlot()),
                    GL_RENDERBUFFER_EXT,
                    rb.getId());
        }
    }

    public void updateFrameBuffer(FrameBuffer fb) {
        int id = fb.getId();
        if (id == -1) {
            // create FBO
            glGenFramebuffersEXT(intBuf1);
            id = intBuf1.get(0);
            fb.setId(id);
            objManager.registerForCleanup(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, id);
            // binding an FBO automatically sets draw buf to GL_COLOR_ATTACHMENT0
            context.boundDrawBuf = 0;
            context.boundFBO = id;
        }

        FrameBuffer.RenderBuffer depthBuf = fb.getDepthBuffer();
        if (depthBuf != null) {
            updateFrameBufferAttachment(fb, depthBuf);
        }

        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
            FrameBuffer.RenderBuffer colorBuf = fb.getColorBuffer(i);
            updateFrameBufferAttachment(fb, colorBuf);
        }

        fb.clearUpdateNeeded();
    }

    public Vector2f[] getFrameBufferSamplePositions(FrameBuffer fb) {
        if (fb.getSamples() <= 1) {
            throw new IllegalArgumentException("Framebuffer must be multisampled");
        }

        setFrameBuffer(fb);

        Vector2f[] samplePositions = new Vector2f[fb.getSamples()];
        FloatBuffer samplePos = BufferUtils.createFloatBuffer(2);
        for (int i = 0; i < samplePositions.length; i++) {
            glGetMultisample(GL_SAMPLE_POSITION, i, samplePos);
            samplePos.clear();
            samplePositions[i] = new Vector2f(samplePos.get(0) - 0.5f,
                    samplePos.get(1) - 0.5f);
        }
        return samplePositions;
    }
    
    public void setMainFrameBufferOverride(FrameBuffer fb){
        mainFbOverride = fb;
    }

    public void setFrameBuffer(FrameBuffer fb) {
        if (fb == null && mainFbOverride != null){
            fb = mainFbOverride;
        }
        
        if (lastFb == fb) {
            if (fb == null || !fb.isUpdateNeeded()){
                return;
            }
        }

        // generate mipmaps for last FB if needed
        if (lastFb != null) {
            for (int i = 0; i < lastFb.getNumColorBuffers(); i++) {
                RenderBuffer rb = lastFb.getColorBuffer(i);
                Texture tex = rb.getTexture();
                if (tex != null
                        && tex.getMinFilter().usesMipMapLevels()) {
                    setTexture(0, rb.getTexture());

                    int textureType = convertTextureType(tex.getType(), tex.getImage().getMultiSamples());
                    glEnable(textureType);
                    glGenerateMipmapEXT(textureType);
                    glDisable(textureType);
                }
            }
        }

        if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
                statistics.onFrameBufferUse(null, true);

                context.boundFBO = 0;
            }
            // select back buffer
            if (context.boundDrawBuf != -1) {
                glDrawBuffer(initialDrawBuf);
                context.boundDrawBuf = -1;
            }
            if (context.boundReadBuf != -1) {
                glReadBuffer(initialReadBuf);
                context.boundReadBuf = -1;
            }

            lastFb = null;
        } else {
            if (fb.getNumColorBuffers() == 0 && fb.getDepthBuffer() == null){
                throw new IllegalArgumentException("The framebuffer: " + fb + 
                                                   "\nDoesn't have any color/depth buffers");
            }
            
            if (fb.isUpdateNeeded()) {
                updateFrameBuffer(fb);
            }

            if (context.boundFBO != fb.getId()) {
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fb.getId());
                statistics.onFrameBufferUse(fb, true);

                // update viewport to reflect framebuffer's resolution
                setViewPort(0, 0, fb.getWidth(), fb.getHeight());

                context.boundFBO = fb.getId();
            } else {
                statistics.onFrameBufferUse(fb, false);
            }
            if (fb.getNumColorBuffers() == 0) {
                // make sure to select NONE as draw buf
                // no color buffer attached. select NONE
                if (context.boundDrawBuf != -2) {
                    glDrawBuffer(GL_NONE);
                    context.boundDrawBuf = -2;
                }
                if (context.boundReadBuf != -2) {
                    glReadBuffer(GL_NONE);
                    context.boundReadBuf = -2;
                }
            } else {
                if (fb.isMultiTarget()) {
                    if (fb.getNumColorBuffers() > maxMRTFBOAttachs) {
                        throw new RendererException("Framebuffer has more"
                                + " targets than are supported"
                                + " on the system!");
                    }

                    if (context.boundDrawBuf != 100 + fb.getNumColorBuffers()) {
                        intBuf16.clear();
                        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
                            intBuf16.put(GL_COLOR_ATTACHMENT0_EXT + i);
                        }

                        intBuf16.flip();
                        glDrawBuffers(intBuf16);
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                } else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (context.boundDrawBuf != rb.getSlot()) {
                        glDrawBuffer(GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
                        context.boundDrawBuf = rb.getSlot();
                    }
                }
            }

            assert fb.getId() >= 0;
            assert context.boundFBO == fb.getId();
            
            lastFb = fb;
            
            try {
                checkFrameBufferError();
            } catch (IllegalStateException ex) {
                logger.log(Level.SEVERE, "=== jMonkeyEngine FBO State ===\n{0}", fb);
                printRealFrameBufferInfo(fb);
                throw ex;
            }
        }
    }

    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        if (fb != null) {
            RenderBuffer rb = fb.getColorBuffer();
            if (rb == null) {
                throw new IllegalArgumentException("Specified framebuffer"
                        + " does not have a colorbuffer");
            }

            setFrameBuffer(fb);
            if (context.boundReadBuf != rb.getSlot()) {
                glReadBuffer(GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
                context.boundReadBuf = rb.getSlot();
            }
        } else {
            setFrameBuffer(null);
        }

        glReadPixels(vpX, vpY, vpW, vpH, /*GL_RGBA*/ GL_BGRA, GL_UNSIGNED_BYTE, byteBuf);
    }

    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1.put(0, rb.getId());
        glDeleteRenderbuffersEXT(intBuf1);
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
        if (fb.getId() != -1) {
            if (context.boundFBO == fb.getId()) {
                glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
                context.boundFBO = 0;
            }

            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1.put(0, fb.getId());
            glDeleteFramebuffersEXT(intBuf1);
            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

    /*********************************************************************\
    |* Textures                                                          *|
    \*********************************************************************/
    private int convertTextureType(Texture.Type type, int samples) {
        switch (type) {
            case TwoDimensional:
                if (samples > 1) {
                    return ARBTextureMultisample.GL_TEXTURE_2D_MULTISAMPLE;
                } else {
                    return GL_TEXTURE_2D;
                }
            case TwoDimensionalArray:
                if (samples > 1) {
                    return ARBTextureMultisample.GL_TEXTURE_2D_MULTISAMPLE_ARRAY;
                } else {
                    return EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT;
                }
            case ThreeDimensional:
                return GL_TEXTURE_3D;
            case CubeMap:
                return GL_TEXTURE_CUBE_MAP;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return GL_LINEAR;
            case Nearest:
                return GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter) {
        switch (filter) {
            case Trilinear:
                return GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GL_LINEAR_MIPMAP_NEAREST;
            case NearestLinearMipMap:
                return GL_NEAREST_MIPMAP_LINEAR;
            case NearestNearestMipMap:
                return GL_NEAREST_MIPMAP_NEAREST;
            case BilinearNoMipMaps:
                return GL_LINEAR;
            case NearestNoMipMaps:
                return GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown min filter: " + filter);
        }
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
                return GL_CLAMP_TO_BORDER;
            case Clamp:
                return GL_CLAMP;
            case EdgeClamp:
                return GL_CLAMP_TO_EDGE;
            case Repeat:
                return GL_REPEAT;
            case MirroredRepeat:
                return GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    @SuppressWarnings("fallthrough")
    private void setupTextureParams(Texture tex) {
        Image image = tex.getImage();
        int target = convertTextureType(tex.getType(), image != null ? image.getMultiSamples() : 1);

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);

        if (tex.getAnisotropicFilter() > 1) {
            if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                glTexParameterf(target,
                        EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
                        tex.getAnisotropicFilter());
            }
        }

        if (context.pointSprite) {
            return; // Attempt to fix glTexParameter crash for some ATI GPUs
        }
        // repeat modes
        switch (tex.getType()) {
            case ThreeDimensional:
            case CubeMap: // cubemaps use 3D coords
                glTexParameteri(target, GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
            case TwoDimensionalArray:
                glTexParameteri(target, GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                glTexParameteri(target, GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        // R to Texture compare mode
        if (tex.getShadowCompareMode() != Texture.ShadowCompareMode.Off) {
            glTexParameteri(target, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
            glTexParameteri(target, GL_DEPTH_TEXTURE_MODE, GL_INTENSITY);
            if (tex.getShadowCompareMode() == Texture.ShadowCompareMode.GreaterOrEqual) {
                glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, GL_GEQUAL);
            } else {
                glTexParameteri(target, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            }
        }
    }

    public void updateTexImageData(Image img, Texture.Type type, boolean mips, int unit) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            glGenTextures(intBuf1);
            texId = intBuf1.get(0);
            img.setId(texId);
            objManager.registerForCleanup(img);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type, img.getMultiSamples());
        if (context.boundTextureUnit != unit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            context.boundTextureUnit = unit;
        }
        if (context.boundTextures[unit] != img) {
            glBindTexture(target, texId);
            context.boundTextures[unit] = img;

            statistics.onTextureUse(img, true);
        }

        if (!img.hasMipmaps() && mips) {
            // No pregenerated mips available,
            // generate from base level if required
            if (!GLContext.getCapabilities().OpenGL30) {
                glTexParameteri(target, GL_GENERATE_MIPMAP, GL_TRUE);
            }
        } else {
//          glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, 0 );
            if (img.getMipMapSizes() != null) {
                glTexParameteri(target, GL_TEXTURE_MAX_LEVEL, img.getMipMapSizes().length);
            }
        }

        int imageSamples = img.getMultiSamples();
        if (imageSamples > 1) {
            if (img.getFormat().isDepthFormat()) {
                img.setMultiSamples(Math.min(maxDepthTexSamples, imageSamples));
            } else {
                img.setMultiSamples(Math.min(maxColorTexSamples, imageSamples));
            }
        }

        // Yes, some OpenGL2 cards (GeForce 5) still dont support NPOT.
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            if (img.getWidth() != 0 && img.getHeight() != 0) {
                if (!FastMath.isPowerOfTwo(img.getWidth())
                        || !FastMath.isPowerOfTwo(img.getHeight())) {
                    if (img.getData(0) == null) {
                        throw new RendererException("non-power-of-2 framebuffer textures are not supported by the video hardware");
                    } else {
                        MipMapGenerator.resizeToPowerOf2(img);
                    }
                }
            }
        }

        // Check if graphics card doesn't support multisample textures
        if (!GLContext.getCapabilities().GL_ARB_texture_multisample) {
            if (img.getMultiSamples() > 1) {
                throw new RendererException("Multisample textures not supported by graphics hardware");
            }
        }

        if (target == GL_TEXTURE_CUBE_MAP) {
            List<ByteBuffer> data = img.getData();
            if (data.size() != 6) {
                logger.log(Level.WARNING, "Invalid texture: {0}\n"
                        + "Cubemap textures must contain 6 data units.", img);
                return;
            }
            for (int i = 0; i < 6; i++) {
                TextureUtil.uploadTexture(img, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, 0, tdc);
            }
        } else if (target == EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT) {
            List<ByteBuffer> data = img.getData();
            // -1 index specifies prepare data for 2D Array
            TextureUtil.uploadTexture(img, target, -1, 0, tdc);
            for (int i = 0; i < data.size(); i++) {
                // upload each slice of 2D array in turn
                // this time with the appropriate index
                TextureUtil.uploadTexture(img, target, i, 0, tdc);
            }
        } else {
            TextureUtil.uploadTexture(img, target, 0, 0, tdc);
        }

        if (img.getMultiSamples() != imageSamples) {
            img.setMultiSamples(imageSamples);
        }

        if (GLContext.getCapabilities().OpenGL30) {
            if (!img.hasMipmaps() && mips && img.getData() != null) {
                // XXX: Required for ATI
                glEnable(target);
                glGenerateMipmapEXT(target);
                glDisable(target);
            }
        }

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels(), unit);
        }

        int texId = image.getId();
        assert texId != -1;

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType(), image.getMultiSamples());
//        if (!context.textureIndexList.moveToNew(unit)) {
//             if (context.boundTextureUnit != unit){
//                glActiveTexture(GL_TEXTURE0 + unit);
//                context.boundTextureUnit = unit;
//             }
//             glEnable(type);
//        }

        if (context.boundTextureUnit != unit) {
            glActiveTexture(GL_TEXTURE0 + unit);
            context.boundTextureUnit = unit;
        }
        if (textures[unit] != image) {
            glBindTexture(type, texId);
            textures[unit] = image;

            statistics.onTextureUse(image, true);
        } else {
            statistics.onTextureUse(image, false);
        }

        setupTextureParams(tex);
    }

    public void clearTextureUnits() {
//        IDList textureList = context.textureIndexList;
//        Image[] textures = context.boundTextures;
//        for (int i = 0; i < textureList.oldLen; i++) {
//            int idx = textureList.oldList[i];
//            if (context.boundTextureUnit != idx){
//                glActiveTexture(GL_TEXTURE0 + idx);
//                context.boundTextureUnit = idx;
//            }
//            glDisable(convertTextureType(textures[idx].getType()));
//            textures[idx] = null;
//        }
//        context.textureIndexList.copyNewToOld();
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);
            glDeleteTextures(intBuf1);
            image.resetObject();

            statistics.onDeleteTexture();
        }
    }

    /*********************************************************************\
    |* Vertex Buffers and Attributes                                     *|
    \*********************************************************************/
    private int convertUsage(Usage usage) {
        switch (usage) {
            case Static:
                return GL_STATIC_DRAW;
            case Dynamic:
                return GL_DYNAMIC_DRAW;
            case Stream:
                return GL_STREAM_DRAW;
            default:
                throw new UnsupportedOperationException("Unknown usage type.");
        }
    }

    private int convertFormat(Format format) {
        switch (format) {
            case Byte:
                return GL_BYTE;
            case UnsignedByte:
                return GL_UNSIGNED_BYTE;
            case Short:
                return GL_SHORT;
            case UnsignedShort:
                return GL_UNSIGNED_SHORT;
            case Int:
                return GL_INT;
            case UnsignedInt:
                return GL_UNSIGNED_INT;
            case Half:
                return NVHalfFloat.GL_HALF_FLOAT_NV;
//                return ARBHalfFloatVertex.GL_HALF_FLOAT;
            case Float:
                return GL_FLOAT;
            case Double:
                return GL_DOUBLE;
            default:
                throw new UnsupportedOperationException("Unknown buffer format.");

        }
    }

    public void updateBufferData(VertexBuffer vb) {
        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer
            glGenBuffers(intBuf1);
            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerForCleanup(vb);

            //statistics.onNewVertexBuffer();
            
            created = true;
        }

        // bind buffer
        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
                //statistics.onVertexBufferUse(vb, true);
            }else{
                //statistics.onVertexBufferUse(vb, false);
            }
        } else {
            target = GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
                //statistics.onVertexBufferUse(vb, true);
            }else{
                //statistics.onVertexBufferUse(vb, false);
            }
        }

        int usage = convertUsage(vb.getUsage());
        vb.getData().rewind();

        if (created || vb.hasDataSizeChanged()) {
            // upload data based on format
            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    glBufferData(target, (ByteBuffer) vb.getData(), usage);
                    break;
                //            case Half:
                case Short:
                case UnsignedShort:
                    glBufferData(target, (ShortBuffer) vb.getData(), usage);
                    break;
                case Int:
                case UnsignedInt:
                    glBufferData(target, (IntBuffer) vb.getData(), usage);
                    break;
                case Float:
                    glBufferData(target, (FloatBuffer) vb.getData(), usage);
                    break;
                case Double:
                    glBufferData(target, (DoubleBuffer) vb.getData(), usage);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown buffer format.");
            }
        } else {
            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    glBufferSubData(target, 0, (ByteBuffer) vb.getData());
                    break;
                case Short:
                case UnsignedShort:
                    glBufferSubData(target, 0, (ShortBuffer) vb.getData());
                    break;
                case Int:
                case UnsignedInt:
                    glBufferSubData(target, 0, (IntBuffer) vb.getData());
                    break;
                case Float:
                    glBufferSubData(target, 0, (FloatBuffer) vb.getData());
                    break;
                case Double:
                    glBufferSubData(target, 0, (DoubleBuffer) vb.getData());
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown buffer format.");
            }
        }
//        }else{
//            if (created || vb.hasDataSizeChanged()){
//                glBufferData(target, vb.getData().capacity() * vb.getFormat().getComponentSize(), usage);
//            }
//
//            ByteBuffer buf = glMapBuffer(target,
//                                         GL_WRITE_ONLY,
//                                         vb.getMappedData());
//
//            if (buf != vb.getMappedData()){
//                buf = buf.order(ByteOrder.nativeOrder());
//                vb.setMappedData(buf);
//            }
//
//            buf.clear();
//
//            switch (vb.getFormat()){
//                case Byte:
//                case UnsignedByte:
//                    buf.put( (ByteBuffer) vb.getData() );
//                    break;
//                case Short:
//                case UnsignedShort:
//                    buf.asShortBuffer().put( (ShortBuffer) vb.getData() );
//                    break;
//                case Int:
//                case UnsignedInt:
//                    buf.asIntBuffer().put( (IntBuffer) vb.getData() );
//                    break;
//                case Float:
//                    buf.asFloatBuffer().put( (FloatBuffer) vb.getData() );
//                    break;
//                case Double:
//                    break;
//                default:
//                    throw new RuntimeException("Unknown buffer format.");
//            }
//
//            glUnmapBuffer(target);
//        }

        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);
            glDeleteBuffers(intBuf1);
            vb.resetObject();
            
            //statistics.onDeleteVertexBuffer();
        }
    }

    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];
            glDisableVertexAttribArray(idx);
            context.boundAttribs[idx] = null;
        }
        context.attribIndexList.copyNewToOld();
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Index buffers not allowed to be set to vertex attrib");
        }

        int programId = context.boundShaderProgram;
        if (programId > 0) {
            Attribute attrib = boundShader.getAttribute(vb.getBufferType());
            int loc = attrib.getLocation();
            if (loc == -1) {
                return; // not defined
            }
            if (loc == -2) {
                stringBuf.setLength(0);
                stringBuf.append("in").append(vb.getBufferType().name()).append('\0');
                updateNameBuffer();
                loc = glGetAttribLocation(programId, nameBuf);

                // not really the name of it in the shader (inPosition\0) but
                // the internal name of the enum (Position).
                if (loc < 0) {
                    attrib.setLocation(-1);
                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }
            }
            
            if (vb.isUpdateNeeded() && idb == null) {
                updateBufferData(vb);
            }
            
            VertexBuffer[] attribs = context.boundAttribs;
            if (!context.attribIndexList.moveToNew(loc)) {
                glEnableVertexAttribArray(loc);
                //System.out.println("Enabled ATTRIB IDX: "+loc);
            }
            if (attribs[loc] != vb) {
                // NOTE: Use id from interleaved buffer if specified
                int bufId = idb != null ? idb.getId() : vb.getId();
                assert bufId != -1;
                if (context.boundArrayVBO != bufId) {
                    glBindBuffer(GL_ARRAY_BUFFER, bufId);
                    context.boundArrayVBO = bufId;
                    //statistics.onVertexBufferUse(vb, true);
                }else{
                    //statistics.onVertexBufferUse(vb, false);
                }

                glVertexAttribPointer(loc,
                        vb.getNumComponents(),
                        convertFormat(vb.getFormat()),
                        vb.isNormalized(),
                        vb.getStride(),
                        vb.getOffset());

                attribs[loc] = vb;
            }
        } else {
            throw new IllegalStateException("Cannot render mesh without shader bound");
        }
    }

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    public void drawTriangleArray(Mesh.Mode mode, int count, int vertCount) {
        if (count > 1) {
            ARBDrawInstanced.glDrawArraysInstancedARB(convertElementMode(mode), 0,
                    vertCount, count);
        } else {
            glDrawArrays(convertElementMode(mode), 0, vertCount);
        }
    }

    public void drawTriangleList(VertexBuffer indexBuf, Mesh mesh, int count) {
        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        if (indexBuf.isUpdateNeeded()) {
            updateBufferData(indexBuf);
        }

        int bufId = indexBuf.getId();
        assert bufId != -1;

        if (context.boundElementArrayVBO != bufId) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufId);
            context.boundElementArrayVBO = bufId;
            //statistics.onVertexBufferUse(indexBuf, true);
        }else{
            //statistics.onVertexBufferUse(indexBuf, true);
        }

        int vertCount = mesh.getVertexCount();
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);

        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt = convertFormat(indexBuf.getFormat());
            int elSize = indexBuf.getFormat().getComponentSize();
            int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++) {
                if (i == stripStart) {
                    elMode = convertElementMode(Mode.TriangleStrip);
                } else if (i == fanStart) {
                    elMode = convertElementMode(Mode.TriangleStrip);
                }
                int elementLength = elementLengths[i];

                if (useInstancing) {
                    ARBDrawInstanced.glDrawElementsInstancedARB(elMode,
                            elementLength,
                            fmt,
                            curOffset,
                            count);
                } else {
                    glDrawRangeElements(elMode,
                            0,
                            vertCount,
                            elementLength,
                            fmt,
                            curOffset);
                }

                curOffset += elementLength * elSize;
            }
        } else {
            if (useInstancing) {
                ARBDrawInstanced.glDrawElementsInstancedARB(convertElementMode(mesh.getMode()),
                        indexBuf.getData().limit(),
                        convertFormat(indexBuf.getFormat()),
                        0,
                        count);
            } else {
                glDrawRangeElements(convertElementMode(mesh.getMode()),
                        0,
                        vertCount,
                        indexBuf.getData().limit(),
                        convertFormat(indexBuf.getFormat()),
                        0);
            }
        }
    }

    /*********************************************************************\
    |* Render Calls                                                      *|
    \*********************************************************************/
    public int convertElementMode(Mesh.Mode mode) {
        switch (mode) {
            case Points:
                return GL_POINTS;
            case Lines:
                return GL_LINES;
            case LineLoop:
                return GL_LINE_LOOP;
            case LineStrip:
                return GL_LINE_STRIP;
            case Triangles:
                return GL_TRIANGLES;
            case TriangleFan:
                return GL_TRIANGLE_FAN;
            case TriangleStrip:
                return GL_TRIANGLE_STRIP;
            default:
                throw new UnsupportedOperationException("Unrecognized mesh mode: " + mode);
        }
    }

    public void updateVertexArray(Mesh mesh) {
        int id = mesh.getId();
        if (id == -1) {
            IntBuffer temp = intBuf1;
            ARBVertexArrayObject.glGenVertexArrays(temp);
            id = temp.get(0);
            mesh.setId(id);
        }

        if (context.boundVertexArray != id) {
            ARBVertexArrayObject.glBindVertexArray(id);
            context.boundVertexArray = id;
        }

        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }

        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        for (Entry<VertexBuffer> entry : buffers) {
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData
                    || vb.getUsage() == Usage.CpuOnly // ignore cpu-only buffers
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttrib(vb);
            } else {
                // interleaved
                setVertexAttrib(vb, interleavedData);
            }
        }
    }

    private void renderMeshVertexArray(Mesh mesh, int lod, int count) {
        if (mesh.getId() == -1){
            updateVertexArray(mesh);
        }else{
            // TODO: Check if it was updated
        }

        if (context.boundVertexArray != mesh.getId()) {
            ARBVertexArrayObject.glBindVertexArray(mesh.getId());
            context.boundVertexArray = mesh.getId();
        }

//        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        VertexBuffer indices = null;
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);
        }
        if (indices != null) {
            drawTriangleList(indices, mesh, count);
        } else {
            drawTriangleArray(mesh.getMode(), count, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    private void renderMeshDefault(Mesh mesh, int lod, int count) {
        VertexBuffer indices = null;

        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }

//        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        SafeArrayList<VertexBuffer> buffersList = mesh.getBufferList();

        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);
        }
        
//        for (Entry<VertexBuffer> entry : buffers) {
//             VertexBuffer vb = entry.getValue();
        for (VertexBuffer vb : mesh.getBufferList().getArray()){
            if (vb.getBufferType() == Type.InterleavedData
                    || vb.getUsage() == Usage.CpuOnly // ignore cpu-only buffers
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttrib(vb);
            } else {
                // interleaved
                setVertexAttrib(vb, interleavedData);
            }
        }

        if (indices != null) {
            drawTriangleList(indices, mesh, count);
        } else {
            drawTriangleArray(mesh.getMode(), count, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
        if (mesh.getVertexCount() == 0) {
            return;
        }

        if (context.pointSprite && mesh.getMode() != Mode.Points){
            // XXX: Hack, disable point sprite mode if mesh not in point mode
            if (context.boundTextures[0] != null){
                if (context.boundTextureUnit != 0){
                    glActiveTexture(GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                glDisable(GL_POINT_SPRITE);
                glDisable(GL_VERTEX_PROGRAM_POINT_SIZE);
                context.pointSprite = false;
            }
        }

        if (context.pointSize != mesh.getPointSize()) {
            glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        statistics.onMeshDrawn(mesh, lod);
//        if (GLContext.getCapabilities().GL_ARB_vertex_array_object){
//            renderMeshVertexArray(mesh, lod, count);
//        }else{
            renderMeshDefault(mesh, lod, count);
//        }
    }
}
