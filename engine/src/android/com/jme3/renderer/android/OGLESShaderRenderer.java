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
package com.jme3.renderer.android;

import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Build;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.light.LightList;
import com.jme3.material.RenderState;
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
import com.jme3.util.ListMap;
import com.jme3.util.NativeObjectManager;
import java.nio.*;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OGLESShaderRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(OGLESShaderRenderer.class.getName());
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
    private boolean tdc;
    private FrameBuffer lastFb = null;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;
    //private final GL10 gl;
    private boolean powerVr = false;
    private boolean powerOf2 = false;
    private boolean verboseLogging = false;
    private boolean useVBO = false;
    private boolean checkErrors = true;

    public OGLESShaderRenderer() {
    }

    public void setUseVA(boolean value) {
        logger.log(Level.INFO, "use_VBO [{0}] -> [{1}]", new Object[]{useVBO, !value});
        useVBO = !value;
    }

    public void setVerboseLogging(boolean value) {
        logger.log(Level.INFO, "verboseLogging [{0}] -> [{1}]", new Object[]{verboseLogging, value});
        verboseLogging = value;
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
    
    private void checkGLError() {
        if (!checkErrors) return;
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RendererException("OpenGL Error " + error);
        }
    }

    private boolean log(String message) {
        logger.info(message);
        return true;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public EnumSet<Caps> getCaps() {
        return caps;
    }

    public void initialize() {

        logger.log(Level.INFO, "Vendor: {0}", GLES20.glGetString(GLES20.GL_VENDOR));
        logger.log(Level.INFO, "Renderer: {0}", GLES20.glGetString(GLES20.GL_RENDERER));
        logger.log(Level.INFO, "Version: {0}", GLES20.glGetString(GLES20.GL_VERSION));

        powerVr = GLES20.glGetString(GLES20.GL_RENDERER).contains("PowerVR");
        
        String versionStr = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION);
        logger.log(Level.INFO, "GLES20.Shading Language Version: {0}", versionStr);
        if (versionStr == null || versionStr.equals("")) {
            glslVer = -1;
            throw new UnsupportedOperationException("GLSL and OpenGL2 is "
                    + "required for the OpenGL ES "
                    + "renderer!");
        }

        // Fix issue in TestRenderToMemory when GL_FRONT is the main
        // buffer being used.

//        initialDrawBuf = GLES20.glGetIntegeri(GLES20.GL_DRAW_BUFFER);
//        initialReadBuf = GLES20.glGetIntegeri(GLES20.GL_READ_BUFFER);

        String openGlEsStr = "OpenGL ES GLSL ES ";
        int spaceIdx = versionStr.indexOf(" ", openGlEsStr.length());
        if (spaceIdx >= 1) {
            versionStr = versionStr.substring(openGlEsStr.length(), spaceIdx).trim();
        }else{
            versionStr = versionStr.substring(openGlEsStr.length()).trim();
        }

        float version = Float.parseFloat(versionStr);
        glslVer = (int) (version * 100);

        switch (glslVer) {
            // TODO: When new versions of OpenGL ES shader language come out, 
            // update this.
            default:
                caps.add(Caps.GLSL100);
                break;
        }

        if (!caps.contains(Caps.GLSL100)) {
            logger.info("Force-adding GLSL100 support, since OpenGL2 is supported.");
            caps.add(Caps.GLSL100);
        }

        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, intBuf16);
        vertexTextureUnits = intBuf16.get(0);
        logger.log(Level.INFO, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, intBuf16);
        fragTextureUnits = intBuf16.get(0);
        logger.log(Level.INFO, "Texture Units: {0}", fragTextureUnits);
        /*
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_COMPONENTS, intBuf16);
        vertexUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);
        
        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, intBuf16);
        fragUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);
         */

        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, intBuf16);
        vertexAttribs = intBuf16.get(0);
        logger.log(Level.INFO, "Vertex Attributes: {0}", vertexAttribs);

        /*
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_FLOATS, intBuf16);
        int varyingFloats = intBuf16.get(0);
        logger.log(Level.FINER, "Varying Floats: {0}", varyingFloats);
         */

        GLES20.glGetIntegerv(GLES20.GL_SUBPIXEL_BITS, intBuf16);
        int subpixelBits = intBuf16.get(0);
        logger.log(Level.INFO, "Subpixel Bits: {0}", subpixelBits);
        /*
        GLES20.glGetIntegerv(GLES20.GL_MAX_ELEMENTS_VERTICES, intBuf16);
        maxVertCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);
        
        GLES20.glGetIntegerv(GLES20.GL_MAX_ELEMENTS_INDICES, intBuf16);
        maxTriCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);
         */
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, intBuf16);
        maxTexSize = intBuf16.get(0);
        logger.log(Level.INFO, "Maximum Texture Resolution: {0}", maxTexSize);

        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, intBuf16);
        maxCubeTexSize = intBuf16.get(0);
        logger.log(Level.INFO, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);


        /*
        if (ctxCaps.GL_ARB_color_buffer_float){
        // XXX: Require both 16 and 32 bit float support for FloatColorBuffer.
        if (ctxCaps.GL_ARB_half_float_pixel){
        caps.add(Caps.FloatColorBuffer);
        }
        }
        
        if (ctxCaps.GL_ARB_depth_buffer_float){
        caps.add(Caps.FloatDepthBuffer);
        }
        
        if (ctxCaps.GL_ARB_draw_instanced)
        caps.add(Caps.MeshInstancing);
        
        if (ctxCaps.GL_ARB_fragment_program)
        caps.add(Caps.ARBprogram);
        
        if (ctxCaps.GL_ARB_texture_buffer_object)
        caps.add(Caps.TextureBuffer);
        
        if (ctxCaps.GL_ARB_texture_float){
        if (ctxCaps.GL_ARB_half_float_pixel){
        caps.add(Caps.FloatTexture);
        }
        }
        
        if (ctxCaps.GL_ARB_vertex_array_object)
        caps.add(Caps.VertexBufferArray);
        
        boolean latc = ctxCaps.GL_EXT_texture_compression_latc;
        boolean atdc = ctxCaps.GL_ATI_texture_compression_3dc;
        if (latc || atdc){
        caps.add(Caps.TextureCompressionLATC);
        if (atdc && !latc){
        tdc = true;
        }
        }
        
        if (ctxCaps.GL_EXT_packed_float){
        caps.add(Caps.PackedFloatColorBuffer);
        if (ctxCaps.GL_ARB_half_float_pixel){
        // because textures are usually uploaded as RGB16F
        // need half-float pixel
        caps.add(Caps.PackedFloatTexture);
        }
        }
        
        if (ctxCaps.GL_EXT_texture_array)
        caps.add(Caps.TextureArray);
        
        if (ctxCaps.GL_EXT_texture_shared_exponent)
        caps.add(Caps.SharedExponentTexture);
        
        if (ctxCaps.GL_EXT_framebuffer_object){
        caps.add(Caps.FrameBuffer);
        
        glGetInteger(GL_MAX_RENDERBUFFER_SIZE_EXT, intBuf16);
        maxRBSize = intBuf16.get(0);
        logger.log(Level.FINER, "FBO RB Max Size: {0}", maxRBSize);
        
        glGetInteger(GL_MAX_COLOR_ATTACHMENTS_EXT, intBuf16);
        maxFBOAttachs = intBuf16.get(0);
        logger.log(Level.FINER, "FBO Max renderbuffers: {0}", maxFBOAttachs);
        
        if (ctxCaps.GL_EXT_framebuffer_multisample){
        caps.add(Caps.FrameBufferMultisample);
        
        glGetInteger(GL_MAX_SAMPLES_EXT, intBuf16);
        maxFBOSamples = intBuf16.get(0);
        logger.log(Level.FINER, "FBO Max Samples: {0}", maxFBOSamples);
        }
        
        if (ctxCaps.GL_ARB_draw_buffers){
        caps.add(Caps.FrameBufferMRT);
        glGetInteger(ARBDrawBuffers.GL_MAX_DRAW_BUFFERS_ARB, intBuf16);
        maxMRTFBOAttachs = intBuf16.get(0);
        logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
        }
        }
        
        if (ctxCaps.GL_ARB_multisample){
        glGetInteger(ARBMultisample.GL_SAMPLE_BUFFERS_ARB, intBuf16);
        boolean available = intBuf16.get(0) != 0;
        glGetInteger(ARBMultisample.GL_SAMPLES_ARB, intBuf16);
        int samples = intBuf16.get(0);
        logger.log(Level.FINER, "Samples: {0}", samples);
        boolean enabled = glIsEnabled(ARBMultisample.GL_MULTISAMPLE_ARB);
        if (samples > 0 && available && !enabled){
        glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
        }
        }
         */

        String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        logger.log(Level.INFO, "GL_EXTENSIONS: {0}", extensions);

        GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, intBuf16);
        for (int i = 0; i < intBuf16.limit(); i++) {
            logger.log(Level.INFO, "Compressed Texture Formats: {0}", intBuf16.get(i));
        }

        if (extensions.contains("GL_OES_texture_npot")) {
            powerOf2 = true;
        }

        applyRenderState(RenderState.DEFAULT);
        GLES20.glDisable(GLES20.GL_DITHER);

        checkGLError();

        useVBO = false;
        
        // NOTE: SDK_INT is only available since 1.6, 
        // but for jME3 it doesn't matter since android versions 1.5 and below
        // are not supported.
        if (Build.VERSION.SDK_INT >= 9){
            useVBO = true;
        }
        
        logger.log(Level.INFO, "Caps: {0}", caps);        
    }

    /**
     * <code>resetGLObjects</code> should be called when die GLView gets recreated to reset all GPU objects
     */
    public void resetGLObjects() {
        objManager.resetObjects();
        statistics.clearMemory();
        boundShader = null;
        lastFb = null;
        context.reset();
    }

    public void cleanup() {
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
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

        if (verboseLogging) {
            logger.log(Level.INFO, "GLES20.glDepthRangef({0}, {1})", new Object[]{start, end});
        }
        GLES20.glDepthRangef(start, end);
        checkGLError();
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            bits = GLES20.GL_COLOR_BUFFER_BIT;
        }
        if (depth) {
            bits |= GLES20.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            bits |= GLES20.GL_STENCIL_BUFFER_BIT;
        }
        if (bits != 0) {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glClear(color={0}, depth={1}, stencil={2})", new Object[]{color, depth, stencil});
            }
            GLES20.glClear(bits);
            checkGLError();
        }
    }

    public void setBackgroundColor(ColorRGBA color) {
        if (verboseLogging) {
            logger.log(Level.INFO, "GLES20.glClearColor({0}, {1}, {2}, {3})", new Object[]{color.r, color.g, color.b, color.a});
        }
        GLES20.glClearColor(color.r, color.g, color.b, color.a);
        checkGLError();
    }

    public void applyRenderState(RenderState state) {
        /*
        if (state.isWireframe() && !context.wireframe){
        GLES20.glPolygonMode(GLES20.GL_FRONT_AND_BACK, GLES20.GL_LINE);
        context.wireframe = true;
        }else if (!state.isWireframe() && context.wireframe){
        GLES20.glPolygonMode(GLES20.GL_FRONT_AND_BACK, GLES20.GL_FILL);
        context.wireframe = false;
        }
         */
        if (state.isDepthTest() && !context.depthTestEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glEnable(GLES20.GL_DEPTH_TEST)");
            }
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            checkGLError();
            if (verboseLogging) {
                logger.info("GLES20.glDepthFunc(GLES20.GL_LEQUAL)");
            }
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            checkGLError();
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glDisable(GLES20.GL_DEPTH_TEST)");
            }
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            checkGLError();
            context.depthTestEnabled = false;
        }
        if (state.isAlphaTest() && !context.alphaTestEnabled) {
//            GLES20.glEnable(GLES20.GL_ALPHA_TEST);
//           GLES20.glAlphaFunc(GLES20.GL_GREATER, state.getAlphaFallOff());
            context.alphaTestEnabled = true;
        } else if (!state.isAlphaTest() && context.alphaTestEnabled) {
//            GLES20.glDisable(GLES20.GL_ALPHA_TEST);
            context.alphaTestEnabled = false;
        }
        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glDepthMask(true)");
            }
            GLES20.glDepthMask(true);
            checkGLError();
            context.depthWriteEnabled = true;
        } else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glDepthMask(false)");
            }
            GLES20.glDepthMask(false);
            checkGLError();
            context.depthWriteEnabled = false;
        }
        if (state.isColorWrite() && !context.colorWriteEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glColorMask(true, true, true, true)");
            }
            GLES20.glColorMask(true, true, true, true);
            checkGLError();
            context.colorWriteEnabled = true;
        } else if (!state.isColorWrite() && context.colorWriteEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glColorMask(false, false, false, false)");
            }
            GLES20.glColorMask(false, false, false, false);
            checkGLError();
            context.colorWriteEnabled = false;
        }
        if (state.isPointSprite() && !context.pointSprite) {
//            GLES20.glEnable(GLES20.GL_POINT_SPRITE);
//            GLES20.glTexEnvi(GLES20.GL_POINT_SPRITE, GLES20.GL_COORD_REPLACE, GLES20.GL_TRUE);
//            GLES20.glEnable(GLES20.GL_VERTEX_PROGRAM_POINT_SIZE);
//            GLES20.glPointParameterf(GLES20.GL_POINT_SIZE_MIN, 1.0f);
        } else if (!state.isPointSprite() && context.pointSprite) {
//            GLES20.glDisable(GLES20.GL_POINT_SPRITE);
        }

        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                if (verboseLogging) {
                    logger.info("GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL)");
                }
                GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
                checkGLError();
                if (verboseLogging) {
                    logger.log(Level.INFO, "GLES20.glPolygonOffset({0}, {1})", new Object[]{state.getPolyOffsetFactor(), state.getPolyOffsetUnits()});
                }
                GLES20.glPolygonOffset(state.getPolyOffsetFactor(),
                        state.getPolyOffsetUnits());
                checkGLError();
                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            } else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    if (verboseLogging) {
                        logger.log(Level.INFO, "GLES20.glPolygonOffset({0}, {1})", new Object[]{state.getPolyOffsetFactor(), state.getPolyOffsetUnits()});
                    }
                    GLES20.glPolygonOffset(state.getPolyOffsetFactor(),
                            state.getPolyOffsetUnits());
                    checkGLError();
                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        } else {
            if (context.polyOffsetEnabled) {
                if (verboseLogging) {
                    logger.info("GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL)");
                }
                GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
                checkGLError();
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                if (verboseLogging) {
                    logger.info("GLES20.glDisable(GLES20.GL_CULL_FACE)");
                }
                GLES20.glDisable(GLES20.GL_CULL_FACE);
            } else {
                if (verboseLogging) {
                    logger.info("GLES20.glEnable(GLES20.GL_CULL_FACE)");
                }
                GLES20.glEnable(GLES20.GL_CULL_FACE);
            }

            checkGLError();

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    if (verboseLogging) {
                        logger.info("GLES20.glCullFace(GLES20.GL_BACK)");
                    }
                    GLES20.glCullFace(GLES20.GL_BACK);
                    break;
                case Front:
                    if (verboseLogging) {
                        logger.info("GLES20.glCullFace(GLES20.GL_FRONT)");
                    }
                    GLES20.glCullFace(GLES20.GL_FRONT);
                    break;
                case FrontAndBack:
                    if (verboseLogging) {
                        logger.info("GLES20.glCullFace(GLES20.GL_FRONT_AND_BACK)");
                    }
                    GLES20.glCullFace(GLES20.GL_FRONT_AND_BACK);
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            checkGLError();

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                if (verboseLogging) {
                    logger.info("GLES20.glDisable(GLES20.GL_BLEND)");
                }
                GLES20.glDisable(GLES20.GL_BLEND);
            } else {
                if (verboseLogging) {
                    logger.info("GLES20.glEnable(GLES20.GL_BLEND)");
                }
                GLES20.glEnable(GLES20.GL_BLEND);
                switch (state.getBlendMode()) {
                    case Off:
                        break;
                    case Additive:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
                        break;
                    case AlphaAdditive:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
                        break;
                    case Color:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_COLOR)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_COLOR);
                        break;
                    case Alpha:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case PremultAlpha:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case Modulate:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_ZERO)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_ZERO);
                        break;
                    case ModulateX2:
                        if (verboseLogging) {
                            logger.info("GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_SRC_COLOR)");
                        }
                        GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_SRC_COLOR);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unrecognized blend mode: "
                                + state.getBlendMode());
                }
            }

            checkGLError();

            context.blendMode = state.getBlendMode();
        }
    }

    /*********************************************************************\
    |* Camera and World transforms                                       *|
    \*********************************************************************/
    public void setViewPort(int x, int y, int w, int h) {
        if (x != vpX || vpY != y || vpW != w || vpH != h) {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glViewport({0}, {1}, {2}, {3})", new Object[]{x, y, w, h});
            }
            GLES20.glViewport(x, y, w, h);
            checkGLError();
            vpX = x;
            vpY = y;
            vpW = w;
            vpH = h;
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glEnable(GLES20.GL_SCISSOR_TEST)");
            }
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            checkGLError();
            context.clipRectEnabled = true;
        }
        if (clipX != x || clipY != y || clipW != width || clipH != height) {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glScissor({0}, {1}, {2}, {3})", new Object[]{x, y, width, height});
            }
            GLES20.glScissor(x, y, width, height);
            clipX = x;
            clipY = y;
            clipW = width;
            clipH = height;
            checkGLError();
        }
    }

    public void clearClipRect() {
        if (context.clipRectEnabled) {
            if (verboseLogging) {
                logger.info("GLES20.glDisable(GLES20.GL_SCISSOR_TEST)");
            }
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            checkGLError();
            context.clipRectEnabled = false;

            clipX = 0;
            clipY = 0;
            clipW = 0;
            clipH = 0;
        }
    }

    public void onFrame() {
        if (!checkErrors){
            int error = GLES20.glGetError();
            if (error != GLES20.GL_NO_ERROR){
                throw new RendererException("OpenGL Error " + error + ". Enable error checking for more info.");
            }
        }
        objManager.deleteUnused(this);
//      statistics.clearFrame();
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
        if (verboseLogging) {
            logger.log(Level.INFO, "GLES20.glGetUniformLocation({0}, {1})", new Object[]{shader.getId(), uniform.getName()});
        }
        int loc = GLES20.glGetUniformLocation(shader.getId(), uniform.getName());
        checkGLError();
        if (loc < 0) {
            uniform.setLocation(-1);
            // uniform is not declared in shader
            if (verboseLogging) {
                logger.log(Level.WARNING, "Uniform [{0}] is not declared in shader.", uniform.getName());
            }
        } else {
            uniform.setLocation(loc);
        }
    }

    protected void updateUniform(Shader shader, Uniform uniform) {
        int shaderId = shader.getId();

        assert uniform.getName() != null;
        assert shader.getId() > 0;

        if (context.boundShaderProgram != shaderId) {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glUseProgram({0})", shaderId);
            }
            GLES20.glUseProgram(shaderId);
            checkGLError();
            statistics.onShaderUse(shader, true);
            boundShader = shader;
            context.boundShaderProgram = shaderId;
        } else {
            statistics.onShaderUse(shader, false);
        }

        int loc = uniform.getLocation();
        if (loc == -1) {
            if (verboseLogging) {
                logger.log(Level.WARNING, "no location for uniform [{0}]", uniform.getName());
            }
            return;
        }

        if (loc == -2) {
            // get uniform location
            updateUniformLocation(shader, uniform);
            if (uniform.getLocation() == -1) {
                // not declared, ignore

                if (verboseLogging) {
                    logger.log(Level.WARNING, "not declared uniform: [{0}]", uniform.getName());
                }

                uniform.clearUpdateNeeded();
                return;
            }
            loc = uniform.getLocation();
        }

        if (uniform.getVarType() == null) {
            logger.warning("value is not set yet.");
            return; // value not set yet..
        }

        statistics.onUniformSet();

        uniform.clearUpdateNeeded();
        FloatBuffer fb;
        switch (uniform.getVarType()) {
            case Float:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform1f set Float. " + uniform.getName());
                }
                Float f = (Float) uniform.getValue();
                GLES20.glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform2f set Vector2. " + uniform.getName());
                }
                Vector2f v2 = (Vector2f) uniform.getValue();
                GLES20.glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform3f set Vector3. " + uniform.getName());
                }
                Vector3f v3 = (Vector3f) uniform.getValue();
                GLES20.glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform4f set Vector4." + uniform.getName());
                }
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    GLES20.glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    GLES20.glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform1i set Boolean." + uniform.getName());
                }
                Boolean b = (Boolean) uniform.getValue();
                GLES20.glUniform1i(loc, b.booleanValue() ? GLES20.GL_TRUE : GLES20.GL_FALSE);
                break;
            case Matrix3:
                if (verboseLogging) {
                    logger.info("GLES20.glUniformMatrix3fv set Matrix3." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                GLES20.glUniformMatrix3fv(loc, 1, false, fb);
                break;
            case Matrix4:
                if (verboseLogging) {
                    logger.info("GLES20.glUniformMatrix4fv set Matrix4." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                GLES20.glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case FloatArray:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform1fv set FloatArray." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform1fv(loc, fb.capacity(), fb);
                break;
            case Vector2Array:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform2fv set Vector2Array." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform2fv(loc, fb.capacity() / 2, fb);
                break;
            case Vector3Array:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform3fv set Vector3Array." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform3fv(loc, fb.capacity() / 3, fb);
                break;
            case Vector4Array:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform4fv set Vector4Array." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform4fv(loc, fb.capacity() / 4, fb);
                break;
            case Matrix4Array:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform4fv set Matrix4Array." + uniform.getName());
                }
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniformMatrix4fv(loc, fb.capacity() / 16, false, fb);
                break;
            case Int:
                if (verboseLogging) {
                    logger.info("GLES20.glUniform1i set Int." + uniform.getName());
                }
                Integer i = (Integer) uniform.getValue();
                GLES20.glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + uniform.getVarType());
        }
        checkGLError();
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
                return GLES20.GL_FRAGMENT_SHADER;
            case Vertex:
                return GLES20.GL_VERTEX_SHADER;
//            case Geometry:
//                return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new RuntimeException("Unrecognized shader type.");
        }
    }

    public void updateShaderSourceData(ShaderSource source, String language) {
        int id = source.getId();
        if (id == -1) {
            // create id
            if (verboseLogging) {
                logger.info("GLES20.glCreateShader(" + source.getType() + ")");
            }
            id = GLES20.glCreateShader(convertShaderType(source.getType()));
            checkGLError();
            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader.");
            }

            source.setId(id);
        }

        // upload shader source
        // merge the defines and source code
        byte[] versionData = new byte[]{};//"#version 140\n".getBytes();
//        versionData = "#define INSTANCING 1\n".getBytes();
        byte[] definesCodeData = source.getDefines().getBytes();
        byte[] sourceCodeData = source.getSource().getBytes();
        ByteBuffer codeBuf = BufferUtils.createByteBuffer(versionData.length
                + definesCodeData.length
                + sourceCodeData.length);
        codeBuf.put(versionData);
        codeBuf.put(definesCodeData);
        codeBuf.put(sourceCodeData);
        codeBuf.flip();

        if (verboseLogging) {
            logger.info("GLES20.glShaderSource(" + id + ")");
        }

        if (powerVr && source.getType() == ShaderType.Vertex) {
            // XXX: This is to fix a bug in old PowerVR, remove
            // when no longer applicable.
            GLES20.glShaderSource(
                    id, source.getDefines()
                    + source.getSource());
        } else {
            GLES20.glShaderSource(
                    id,
                    "precision mediump float;\n"
                    + source.getDefines()
                    + source.getSource());
        }

        checkGLError();

        if (verboseLogging) {
            logger.info("GLES20.glCompileShader(" + id + ")");
        }

        GLES20.glCompileShader(id);

        checkGLError();

        if (verboseLogging) {
            logger.info("GLES20.glGetShaderiv(" + id + ", GLES20.GL_COMPILE_STATUS)");
        }

        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, intBuf1);

        checkGLError();

        boolean compiledOK = intBuf1.get(0) == GLES20.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            if (verboseLogging) {
                logger.info("GLES20.glGetShaderiv()");
            }
            GLES20.glGetShaderiv(id, GLES20.GL_INFO_LOG_LENGTH, intBuf1);
            checkGLError();
            if (verboseLogging) {
                logger.info("GLES20.glGetShaderInfoLog(" + id + ")");
            }
            infoLog = GLES20.glGetShaderInfoLog(id);
            logger.severe("Errooooooooooot(" + id + ")");
        }

        if (compiledOK) {
            if (infoLog != null) {
                logger.log(Level.INFO, "compile success: " + source.getName() + ", " + infoLog);
            } else {
                logger.log(Level.FINE, "compile success: " + source.getName());
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
            if (verboseLogging) {
                logger.info("GLES20.glDeleteShader(" + id + ")");
            }
            GLES20.glDeleteShader(id);
            checkGLError();
        } else {
            // register for cleanup since the ID is usable
            objManager.registerForCleanup(source);
        }
    }

    public void updateShaderData(Shader shader) {
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program

            if (verboseLogging) {
                logger.info("GLES20.glCreateProgram()");
            }

            id = GLES20.glCreateProgram();

            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader program.");
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
            if (verboseLogging) {
                logger.info("GLES20.glAttachShader(" + id + ", " + source.getId() + ")");
            }

            GLES20.glAttachShader(id, source.getId());
        }

        // link shaders to program
        if (verboseLogging) {
            logger.info("GLES20.glLinkProgram(" + id + ")");
        }

        GLES20.glLinkProgram(id);


        if (verboseLogging) {
            logger.info("GLES20.glGetProgramiv(" + id + ")");
        }

        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, intBuf1);

        boolean linkOK = intBuf1.get(0) == GLES20.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            if (verboseLogging) {
                logger.info("GLES20.glGetProgramiv(" + id + ", GLES20.GL_INFO_LOG_LENGTH, buffer)");
            }

            GLES20.glGetProgramiv(id, GLES20.GL_INFO_LOG_LENGTH, intBuf1);

            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos

                if (verboseLogging) {
                    logger.info("GLES20.glGetProgramInfoLog(" + id + ")");
                }

                infoLog = GLES20.glGetProgramInfoLog(id);
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
        if (verboseLogging) {
            logger.info("setShader(" + shader + ")");
        }

        if (shader == null) {
            if (context.boundShaderProgram > 0) {

                if (verboseLogging) {
                    logger.info("GLES20.glUseProgram(0)");
                }

                GLES20.glUseProgram(0);

                statistics.onShaderUse(null, true);
                context.boundShaderProgram = 0;
                boundShader = null;
            }
        } else {
            if (shader.isUpdateNeeded()) {
                updateShaderData(shader);
            }

            // NOTE: might want to check if any of the 
            // sources need an update?

            if (!shader.isUsable()) {
                logger.warning("shader is not usable.");
                return;
            }

            assert shader.getId() > 0;

            updateShaderUniforms(shader);
            if (context.boundShaderProgram != shader.getId()) {
                if (VALIDATE_SHADER) {
                    // check if shader can be used
                    // with current state
                    if (verboseLogging) {
                        logger.info("GLES20.glValidateProgram(" + shader.getId() + ")");
                    }

                    GLES20.glValidateProgram(shader.getId());

                    if (verboseLogging) {
                        logger.info("GLES20.glGetProgramiv(" + shader.getId() + ", GLES20.GL_VALIDATE_STATUS, buffer)");
                    }

                    GLES20.glGetProgramiv(shader.getId(), GLES20.GL_VALIDATE_STATUS, intBuf1);

                    boolean validateOK = intBuf1.get(0) == GLES20.GL_TRUE;

                    if (validateOK) {
                        logger.fine("shader validate success");
                    } else {
                        logger.warning("shader validate failure");
                    }
                }

                if (verboseLogging) {
                    logger.info("GLES20.glUseProgram(" + shader.getId() + ")");
                }

                GLES20.glUseProgram(shader.getId());

                statistics.onShaderUse(shader, true);
                context.boundShaderProgram = shader.getId();
                boundShader = shader;
            } else {
                statistics.onShaderUse(shader, false);
            }
        }
    }

    public void deleteShaderSource(ShaderSource source) {
        if (source.getId() < 0) {
            logger.warning("Shader source is not uploaded to GPU, cannot delete.");
            return;
        }
        source.setUsable(false);
        source.clearUpdateNeeded();

        if (verboseLogging) {
            logger.info("GLES20.glDeleteShader(" + source.getId() + ")");
        }

        GLES20.glDeleteShader(source.getId());
        source.resetObject();
    }

    public void deleteShader(Shader shader) {
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }
        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {

                if (verboseLogging) {
                    logger.info("GLES20.glDetachShader(" + shader.getId() + ", " + source.getId() + ")");
                }

                GLES20.glDetachShader(shader.getId(), source.getId());
                // the next part is done by the GLObjectManager automatically
//                glDeleteShader(source.getId());
            }
        }
        // kill all references so sources can be collected
        // if needed.
        shader.resetSources();

        if (verboseLogging) {
            logger.info("GLES20.glDeleteProgram(" + shader.getId() + ")");
        }

        GLES20.glDeleteProgram(shader.getId());

        statistics.onDeleteShader();
    }

    /*********************************************************************\
    |* Framebuffers                                                      *|
    \*********************************************************************/
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        logger.warning("copyFrameBuffer is not supported.");
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
        logger.warning("copyFrameBuffer is not supported.");
    }
    /*
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst){
    if (GLContext.getCapabilities().GL_EXT_framebuffer_blit){
    int srcW = 0;
    int srcH = 0;
    int dstW = 0;
    int dstH = 0;
    int prevFBO = context.boundFBO;
    
    if (src != null && src.isUpdateNeeded())
    updateFrameBuffer(src);
    
    if (dst != null && dst.isUpdateNeeded())
    updateFrameBuffer(dst);
    
    if (src == null){
    glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, 0);
    //                srcW = viewWidth;
    //                srcH = viewHeight;
    }else{  
    glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, src.getId());
    srcW = src.getWidth();
    srcH = src.getHeight();
    }
    if (dst == null){
    glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, 0);
    //                dstW = viewWidth;
    //                dstH = viewHeight;
    }else{
    glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, dst.getId());
    dstW = dst.getWidth();
    dstH = dst.getHeight();
    }
    glBlitFramebufferEXT(0, 0, srcW, srcH,
    0, 0, dstW, dstH,
    GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT,
    GL_NEAREST);
    
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, prevFBO);
    try {
    checkFrameBufferError();
    } catch (IllegalStateException ex){
    logger.log(Level.SEVERE, "Source FBO:\n{0}", src);
    logger.log(Level.SEVERE, "Dest FBO:\n{0}", dst);
    throw ex;
    }
    }else{
    throw new UnsupportedOperationException("EXT_framebuffer_blit required.");
    // TODO: support non-blit copies?
    }
    }
     */

    private void checkFrameBufferError() {
        logger.warning("checkFrameBufferError is not supported.");
    }
    /*
    private void checkFrameBufferError() {
    int status = glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT);
    switch (status) {
    case GL_FRAMEBUFFER_COMPLETE_EXT:
    break;
    case GL_FRAMEBUFFER_UNSUPPORTED_EXT:
    //Choose different formats
    throw new IllegalStateException("Framebuffer object format is " +
    "unsupported by the video hardware.");
    case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
    throw new IllegalStateException("Framebuffer has erronous attachment.");
    case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
    throw new IllegalStateException("Framebuffer is missing required attachment.");
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
    throw new IllegalStateException("Some video driver error " +
    "or programming error occured. " +
    "Framebuffer object status is invalid. ");
    }
    }
     */

    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        logger.warning("updateRenderBuffer is not supported.");
    }
    /*
    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb){
    int id = rb.getId();
    if (id == -1){
    glGenRenderbuffersEXT(intBuf1);
    id = intBuf1.get(0);
    rb.setId(id);
    }
    
    if (context.boundRB != id){
    glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, id);
    context.boundRB = id;
    }
    
    if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize)
    throw new UnsupportedOperationException("Resolution "+fb.getWidth()+
    ":"+fb.getHeight()+" is not supported.");
    
    if (fb.getSamples() > 0 && GLContext.getCapabilities().GL_EXT_framebuffer_multisample){
    int samples = fb.getSamples();
    if (maxFBOSamples < samples){
    samples = maxFBOSamples;
    }
    glRenderbufferStorageMultisampleEXT(GL_RENDERBUFFER_EXT,
    samples,
    TextureUtil.convertTextureFormat(rb.getFormat()),
    fb.getWidth(),
    fb.getHeight());
    }else{
    glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT,
    TextureUtil.convertTextureFormat(rb.getFormat()),
    fb.getWidth(),
    fb.getHeight());
    }
    }
     */

    private int convertAttachmentSlot(int attachmentSlot) {
        logger.warning("convertAttachmentSlot is not supported.");
        return -1;
    }
    /*
    private int convertAttachmentSlot(int attachmentSlot){
    // can also add support for stencil here
    if (attachmentSlot == -100){
    return GL_DEPTH_ATTACHMENT_EXT;
    }else if (attachmentSlot < 0 || attachmentSlot >= 16){
    throw new UnsupportedOperationException("Invalid FBO attachment slot: "+attachmentSlot);
    }
    
    return GL_COLOR_ATTACHMENT0_EXT + attachmentSlot;
    }
     */

    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        logger.warning("updateRenderTexture is not supported.");
    }
    /*
    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb){
    Texture tex = rb.getTexture();
    Image image = tex.getImage();
    if (image.isUpdateNeeded())
    updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels());
    
    glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT,
    convertAttachmentSlot(rb.getSlot()),
    convertTextureType(tex.getType()),
    image.getId(),
    0);
    }
     */

    public void updateFrameBufferAttachment(FrameBuffer fb, RenderBuffer rb) {
        logger.warning("updateFrameBufferAttachment is not supported.");
    }
    /*
    public void updateFrameBufferAttachment(FrameBuffer fb, RenderBuffer rb){
    boolean needAttach;
    if (rb.getTexture() == null){
    // if it hasn't been created yet, then attach is required.
    needAttach = rb.getId() == -1;
    updateRenderBuffer(fb, rb);
    }else{
    needAttach = false;
    updateRenderTexture(fb, rb);
    }
    if (needAttach){
    glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT,
    convertAttachmentSlot(rb.getSlot()),
    GL_RENDERBUFFER_EXT,
    rb.getId());
    }
    }
     */

    public void updateFrameBuffer(FrameBuffer fb) {
        logger.warning("updateFrameBuffer is not supported.");
    }
    /*
    public void updateFrameBuffer(FrameBuffer fb) {
    int id = fb.getId();
    if (id == -1){
    // create FBO
    glGenFramebuffersEXT(intBuf1);
    id = intBuf1.get(0);
    fb.setId(id);
    objManager.registerForCleanup(fb);
    
    statistics.onNewFrameBuffer();
    }
    
    if (context.boundFBO != id){
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, id);
    // binding an FBO automatically sets draw buf to GL_COLOR_ATTACHMENT0
    context.boundDrawBuf = 0;
    context.boundFBO = id;
    }
    
    FrameBuffer.RenderBuffer depthBuf = fb.getDepthBuffer();
    if (depthBuf != null){
    updateFrameBufferAttachment(fb, depthBuf);
    }
    
    for (int i = 0; i < fb.getNumColorBuffers(); i++){
    FrameBuffer.RenderBuffer colorBuf = fb.getColorBuffer(i);
    updateFrameBufferAttachment(fb, colorBuf);
    }
    
    fb.clearUpdateNeeded();
    }
     */

    public void setMainFrameBufferOverride(FrameBuffer fb){
    }
    
    public void setFrameBuffer(FrameBuffer fb) {
        if (verboseLogging) {
            logger.warning("setFrameBuffer is not supported.");
        }
    }
    /*
    public void setFrameBuffer(FrameBuffer fb) {
    if (lastFb == fb)
    return;
    
    // generate mipmaps for last FB if needed
    if (lastFb != null){
    for (int i = 0; i < lastFb.getNumColorBuffers(); i++){
    RenderBuffer rb = lastFb.getColorBuffer(i);
    Texture tex = rb.getTexture();
    if (tex != null
    && tex.getMinFilter().usesMipMapLevels()){
    setTexture(0, rb.getTexture());
    glGenerateMipmapEXT(convertTextureType(tex.getType()));
    }
    }
    }
    
    
    if (fb == null){
    // unbind any fbos
    if (context.boundFBO != 0){
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    statistics.onFrameBufferUse(null, true);
    
    context.boundFBO = 0;
    }
    // select back buffer
    if (context.boundDrawBuf != -1){
    glDrawBuffer(initialDrawBuf);
    context.boundDrawBuf = -1;
    }
    if (context.boundReadBuf != -1){
    glReadBuffer(initialReadBuf);
    context.boundReadBuf = -1;
    }
    
    lastFb = null;
    }else{
    if (fb.isUpdateNeeded())
    updateFrameBuffer(fb);
    
    if (context.boundFBO != fb.getId()){
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fb.getId());
    statistics.onFrameBufferUse(fb, true);
    
    // update viewport to reflect framebuffer's resolution
    setViewPort(0, 0, fb.getWidth(), fb.getHeight());
    
    context.boundFBO = fb.getId();
    }else{
    statistics.onFrameBufferUse(fb, false);
    }
    if (fb.getNumColorBuffers() == 0){
    // make sure to select NONE as draw buf
    // no color buffer attached. select NONE
    if (context.boundDrawBuf != -2){
    glDrawBuffer(GL_NONE);
    context.boundDrawBuf = -2;
    }
    if (context.boundReadBuf != -2){
    glReadBuffer(GL_NONE);
    context.boundReadBuf = -2;
    }
    }else{
    if (fb.isMultiTarget()){
    if (fb.getNumColorBuffers() > maxMRTFBOAttachs)
    throw new UnsupportedOperationException("Framebuffer has more"
    + " targets than are supported"
    + " on the system!");
    
    if (context.boundDrawBuf != 100 + fb.getNumColorBuffers()){
    intBuf16.clear();
    for (int i = 0; i < fb.getNumColorBuffers(); i++)
    intBuf16.put( GL_COLOR_ATTACHMENT0_EXT + i );
    
    intBuf16.flip();
    glDrawBuffers(intBuf16);
    context.boundDrawBuf = 100 + fb.getNumColorBuffers();
    }
    }else{
    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
    // select this draw buffer
    if (context.boundDrawBuf != rb.getSlot()){
    glDrawBuffer(GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
    context.boundDrawBuf = rb.getSlot();
    }
    }
    }
    
    assert fb.getId() >= 0;
    assert context.boundFBO == fb.getId();
    lastFb = fb;
    }
    
    try {
    checkFrameBufferError();
    } catch (IllegalStateException ex){
    logger.log(Level.SEVERE, "Problem FBO:\n{0}", fb);
    throw ex;
    }
    }
     */

    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        logger.warning("readFrameBuffer is not supported.");
    }
    /*
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf){
    if (fb != null){
    RenderBuffer rb = fb.getColorBuffer();
    if (rb == null)
    throw new IllegalArgumentException("Specified framebuffer" +
    " does not have a colorbuffer");
    
    setFrameBuffer(fb);
    if (context.boundReadBuf != rb.getSlot()){
    glReadBuffer(GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
    context.boundReadBuf = rb.getSlot();
    }
    }else{
    setFrameBuffer(null);
    }
    
    glReadPixels(vpX, vpY, vpW, vpH, GL_RGBA GL_BGRA, GL_UNSIGNED_BYTE, byteBuf);
    }
     */

    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        logger.warning("deleteRenderBuffer is not supported.");
    }
    /*
    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb){
    intBuf1.put(0, rb.getId());
    glDeleteRenderbuffersEXT(intBuf1);
    }
     */

    public void deleteFrameBuffer(FrameBuffer fb) {
        logger.warning("deleteFrameBuffer is not supported.");
    }
    /*
    public void deleteFrameBuffer(FrameBuffer fb) {
    if (fb.getId() != -1){
    if (context.boundFBO == fb.getId()){
    glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
    context.boundFBO = 0;
    }
    
    if (fb.getDepthBuffer() != null){
    deleteRenderBuffer(fb, fb.getDepthBuffer());
    }
    if (fb.getColorBuffer() != null){
    deleteRenderBuffer(fb, fb.getColorBuffer());
    }
    
    intBuf1.put(0, fb.getId());
    glDeleteFramebuffersEXT(intBuf1);
    fb.resetObject();
    
    statistics.onDeleteFrameBuffer();
    }
    }
     */

    /*********************************************************************\
    |* Textures                                                          *|
    \*********************************************************************/
    private int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return GLES20.GL_TEXTURE_2D;
            //        case TwoDimensionalArray:
            //            return EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT;
//            case ThreeDimensional:
            //               return GLES20.GL_TEXTURE_3D;
            case CubeMap:
                return GLES20.GL_TEXTURE_CUBE_MAP;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return GLES20.GL_LINEAR;
            case Nearest:
                return GLES20.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter) {
        switch (filter) {
            case Trilinear:
                return GLES20.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GLES20.GL_LINEAR_MIPMAP_NEAREST;
            case NearestLinearMipMap:
                return GLES20.GL_NEAREST_MIPMAP_LINEAR;
            case NearestNearestMipMap:
                return GLES20.GL_NEAREST_MIPMAP_NEAREST;
            case BilinearNoMipMaps:
                return GLES20.GL_LINEAR;
            case NearestNoMipMaps:
                return GLES20.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown min filter: " + filter);
        }
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
            case Clamp:
            case EdgeClamp:
                return GLES20.GL_CLAMP_TO_EDGE;
            case Repeat:
                return GLES20.GL_REPEAT;
            case MirroredRepeat:
                return GLES20.GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    /**
     * <code>setupTextureParams</code> sets the OpenGL context texture parameters
     * @param tex the Texture to set the texture parameters from
     */
    private void setupTextureParams(Texture tex) {
        int target = convertTextureType(tex.getType());

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());

        if (verboseLogging) {
            logger.info("GLES20.glTexParameteri(" + target + ", GLES20.GL_TEXTURE_MIN_FILTER, " + minFilter + ")");
        }

        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);

        if (verboseLogging) {
            logger.info("GLES20.glTexParameteri(" + target + ", GLES20.GL_TEXTURE_MAG_FILTER, " + magFilter + ")");
        }

        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, magFilter);

        /*        
        if (tex.getAnisotropicFilter() > 1){
        
        if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic){
        glTexParameterf(target,
        EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
        tex.getAnisotropicFilter());
        }
        
        }
         */
        // repeat modes

        switch (tex.getType()) {
            case ThreeDimensional:
            case CubeMap: // cubemaps use 3D coords
            // GL_TEXTURE_WRAP_R is not available in api 8
            //GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
            case TwoDimensionalArray:

                if (verboseLogging) {
                    logger.info("GLES20.glTexParameteri(" + target + ", GLES20.GL_TEXTURE_WRAP_T, " + convertWrapMode(tex.getWrap(WrapAxis.T)));
                }

                GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));

                // fall down here is intentional..
//          case OneDimensional:

                if (verboseLogging) {
                    logger.info("GLES20.glTexParameteri(" + target + ", GLES20.GL_TEXTURE_WRAP_S, " + convertWrapMode(tex.getWrap(WrapAxis.S)));
                }

                GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        // R to Texture compare mode
/*
        if (tex.getShadowCompareMode() != Texture.ShadowCompareMode.Off){
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_COMPARE_MODE, GLES20.GL_COMPARE_R_TO_TEXTURE);
        GLES20.glTexParameteri(target, GLES20.GL_DEPTH_TEXTURE_MODE, GLES20.GL_INTENSITY);
        if (tex.getShadowCompareMode() == Texture.ShadowCompareMode.GreaterOrEqual){
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_COMPARE_FUNC, GLES20.GL_GEQUAL);
        }else{
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_COMPARE_FUNC, GLES20.GL_LEQUAL);
        }
        }
         */
    }

    /**
     * <code>updateTexImageData</code> activates and binds the texture
     * @param img
     * @param type
     * @param mips
     */
    public void updateTexImageData(Image img, Texture.Type type, boolean mips) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            if (verboseLogging) {
                logger.info("GLES20.glGenTexture(1, buffer)");
            }

            GLES20.glGenTextures(1, intBuf1);
            texId = intBuf1.get(0);
            img.setId(texId);
            objManager.registerForCleanup(img);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type);
        if (context.boundTextures[0] != img) {
            if (context.boundTextureUnit != 0) {
                if (verboseLogging) {
                    logger.info("GLES20.glActiveTexture(GLES20.GL_TEXTURE0)");
                }

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                context.boundTextureUnit = 0;
            }

            if (verboseLogging) {
                logger.info("GLES20.glBindTexture(" + target + ", " + texId + ")");
            }

            GLES20.glBindTexture(target, texId);
            context.boundTextures[0] = img;
        }


        if (target == GLES20.GL_TEXTURE_CUBE_MAP) {
            // Upload a cube map / sky box
            @SuppressWarnings("unchecked")
            List<AndroidImageInfo> bmps = (List<AndroidImageInfo>) img.getEfficentData();
            if (bmps != null) {
                // Native android bitmap                                       
                if (bmps.size() != 6) {
                    throw new UnsupportedOperationException("Invalid texture: " + img
                            + "Cubemap textures must contain 6 data units.");
                }
                for (int i = 0; i < 6; i++) {
                    TextureUtil.uploadTextureBitmap(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, bmps.get(i).getBitmap(), false, powerOf2);
                }
            } else {
                // Standard jme3 image data
                List<ByteBuffer> data = img.getData();
                if (data.size() != 6) {
                    logger.log(Level.WARNING, "Invalid texture: {0}\n"
                            + "Cubemap textures must contain 6 data units.", img);
                    return;
                }
                for (int i = 0; i < 6; i++) {
                    TextureUtil.uploadTexture(img, GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, 0, tdc, false, powerOf2);
                }
            }
        } else {
            TextureUtil.uploadTexture(img, target, 0, 0, tdc, false, powerOf2);

            if (verboseLogging) {
                logger.info("GLES20.glTexParameteri(" + target + "GLES11.GL_GENERATE_MIMAP, GLES20.GL_TRUE)");
            }

            if (!img.hasMipmaps() && mips) {
                // No pregenerated mips available,
                // generate from base level if required
                if (verboseLogging) {
                    logger.info("GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)");
                }
                GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
            }
        }

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            /*
            Bitmap bmp = (Bitmap)image.getEfficentData();
            if (bmp != null)
            {
            // Check if the bitmap got recycled, can happen after wakeup/restart
            if ( bmp.isRecycled() )
            {
            // We need to reload the bitmap
            Texture textureReloaded = JmeSystem.newAssetManager().loadTexture((TextureKey)tex.getKey());
            image.setEfficentData( textureReloaded.getImage().getEfficentData());
            }
            }
             */
            updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels());
        }

        int texId = image.getId();
        assert texId != -1;

        if (texId == -1) {
            logger.warning("error: texture image has -1 id");
        }

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType());
        if (!context.textureIndexList.moveToNew(unit)) {
//             if (context.boundTextureUnit != unit){
//                glActiveTexture(GL_TEXTURE0 + unit);
//                context.boundTextureUnit = unit;
//             }
//             glEnable(type);
        }

        if (textures[unit] != image) {
            if (context.boundTextureUnit != unit) {
                if (verboseLogging) {
                    logger.info("GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + " + unit + ")");
                }
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            if (verboseLogging) {
                logger.info("GLES20.glBindTexture(" + type + ", " + texId + ")");
            }

            GLES20.glBindTexture(type, texId);
            textures[unit] = image;

            statistics.onTextureUse(tex.getImage(), true);
        } else {
            statistics.onTextureUse(tex.getImage(), false);
        }

        setupTextureParams(tex);
    }

    public void clearTextureUnits() {
        IDList textureList = context.textureIndexList;
        Image[] textures = context.boundTextures;
        for (int i = 0; i < textureList.oldLen; i++) {
            int idx = textureList.oldList[i];
//            if (context.boundTextureUnit != idx){
//                glActiveTexture(GL_TEXTURE0 + idx);
//                context.boundTextureUnit = idx;
//            }
//            glDisable(convertTextureType(textures[idx].getType()));
            textures[idx] = null;
        }
        context.textureIndexList.copyNewToOld();
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);

            if (verboseLogging) {
                logger.info("GLES20.glDeleteTexture(1, buffer)");
            }

            GLES20.glDeleteTextures(1, intBuf1);
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
                return GLES20.GL_STATIC_DRAW;
            case Dynamic:
                return GLES20.GL_DYNAMIC_DRAW;
            case Stream:
                return GLES20.GL_STREAM_DRAW;
            default:
                throw new RuntimeException("Unknown usage type.");
        }
    }

    private int convertFormat(Format format) {
        switch (format) {
            case Byte:
                return GLES20.GL_BYTE;
            case UnsignedByte:
                return GLES20.GL_UNSIGNED_BYTE;
            case Short:
                return GLES20.GL_SHORT;
            case UnsignedShort:
                return GLES20.GL_UNSIGNED_SHORT;
            case Int:
                return GLES20.GL_INT;
            case UnsignedInt:
                return GLES20.GL_UNSIGNED_INT;
            /*
            case Half:
            return NVHalfFloat.GL_HALF_FLOAT_NV;
            //                return ARBHalfFloatVertex.GL_HALF_FLOAT;
             */
            case Float:
                return GLES20.GL_FLOAT;
//            case Double:
//                return GLES20.GL_DOUBLE;
            default:
                throw new RuntimeException("Unknown buffer format.");

        }
    }

    public void updateBufferData(VertexBuffer vb) {

        if (verboseLogging) {
            logger.info("updateBufferData(" + vb + ")");
        }

        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer

            if (verboseLogging) {
                logger.info("GLES20.glGenBuffers(" + 1 + ", buffer)");
            }

            GLES20.glGenBuffers(1, intBuf1);
            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerForCleanup(vb);

            created = true;
        }

        // bind buffer
        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = GLES20.GL_ELEMENT_ARRAY_BUFFER;

            if (verboseLogging) {
                logger.info("vb.getBufferType() == VertexBuffer.Type.Index");
            }

            if (context.boundElementArrayVBO != bufId) {

                if (verboseLogging) {
                    logger.info("GLES20.glBindBuffer(" + target + ", " + bufId + ")");
                }

                GLES20.glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
            }
        } else {
            if (verboseLogging) {
                logger.info("vb.getBufferType() != VertexBuffer.Type.Index");
            }

            target = GLES20.GL_ARRAY_BUFFER;

            if (context.boundArrayVBO != bufId) {

                if (verboseLogging) {
                    logger.info("GLES20.glBindBuffer(" + target + ", " + bufId + ")");
                }

                GLES20.glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
            }
        }

        int usage = convertUsage(vb.getUsage());
        vb.getData().clear();

        if (created || vb.hasDataSizeChanged()) {
            // upload data based on format
            int size = vb.getData().capacity() * vb.getFormat().getComponentSize();

            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:

                    if (verboseLogging) {
                        logger.info("GLES20.glBufferData(" + target + ", " + size + ", (data), " + usage + ")");
                    }

                    GLES20.glBufferData(target, size, (ByteBuffer) vb.getData(), usage);
                    break;
                //            case Half:
                case Short:
                case UnsignedShort:

                    if (verboseLogging) {
                        logger.info("GLES20.glBufferData(" + target + ", " + size + ", (data), " + usage + ")");
                    }

                    GLES20.glBufferData(target, size, (ShortBuffer) vb.getData(), usage);
                    break;
                case Int:
                case UnsignedInt:

                    if (verboseLogging) {
                        logger.info("GLES20.glBufferData(" + target + ", " + size + ", (data), " + usage + ")");
                    }

                    GLES20.glBufferData(target, size, (IntBuffer) vb.getData(), usage);
                    break;
                case Float:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferData(" + target + ", " + size + ", (data), " + usage + ")");
                    }

                    GLES20.glBufferData(target, size, (FloatBuffer) vb.getData(), usage);
                    break;
                case Double:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferData(" + target + ", " + size + ", (data), " + usage + ")");
                    }

                    GLES20.glBufferData(target, size, (DoubleBuffer) vb.getData(), usage);
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
            }
        } else {
            int size = vb.getData().capacity() * vb.getFormat().getComponentSize();

            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferSubData(" + target + ", 0, " + size + ", (data))");
                    }

                    GLES20.glBufferSubData(target, 0, size, (ByteBuffer) vb.getData());
                    break;
                case Short:
                case UnsignedShort:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferSubData(" + target + ", 0, " + size + ", (data))");
                    }

                    GLES20.glBufferSubData(target, 0, size, (ShortBuffer) vb.getData());
                    break;
                case Int:
                case UnsignedInt:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferSubData(" + target + ", 0, " + size + ", (data))");
                    }

                    GLES20.glBufferSubData(target, 0, size, (IntBuffer) vb.getData());
                    break;
                case Float:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferSubData(" + target + ", 0, " + size + ", (data))");
                    }

                    GLES20.glBufferSubData(target, 0, size, (FloatBuffer) vb.getData());
                    break;
                case Double:
                    if (verboseLogging) {
                        logger.info("GLES20.glBufferSubData(" + target + ", 0, " + size + ", (data))");
                    }

                    GLES20.glBufferSubData(target, 0, size, (DoubleBuffer) vb.getData());
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
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
            if (verboseLogging) {
                logger.info("GLES20.glDeleteBuffers(1, buffer)");
            }

            GLES20.glDeleteBuffers(1, intBuf1);
            vb.resetObject();
        }
    }

    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];

            if (verboseLogging) {
                logger.info("GLES20.glDisableVertexAttribArray(" + idx + ")");
            }

            GLES20.glDisableVertexAttribArray(idx);
            context.boundAttribs[idx] = null;
        }
        context.attribIndexList.copyNewToOld();
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        if (verboseLogging) {
            logger.info("setVertexAttrib(" + vb + ", " + idb + ")");
        }

        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Index buffers not allowed to be set to vertex attrib");
        }

        if (vb.isUpdateNeeded() && idb == null) {
            updateBufferData(vb);
        }

        int programId = context.boundShaderProgram;
        if (programId > 0) {
            Attribute attrib = boundShader.getAttribute(vb.getBufferType());
            int loc = attrib.getLocation();
            if (loc == -1) {

                if (verboseLogging) {
                    logger.warning("location is invalid for attrib: [" + vb.getBufferType().name() + "]");
                }

                return; // not defined
            }

            if (loc == -2) {
//                stringBuf.setLength(0);
//                stringBuf.append("in").append(vb.getBufferType().name()).append('\0');
//                updateNameBuffer();

                String attributeName = "in" + vb.getBufferType().name();

                if (verboseLogging) {
                    logger.info("GLES20.glGetAttribLocation(" + programId + ", " + attributeName + ")");
                }

                loc = GLES20.glGetAttribLocation(programId, attributeName);

                // not really the name of it in the shader (inPosition\0) but
                // the internal name of the enum (Position).
                if (loc < 0) {
                    attrib.setLocation(-1);

                    if (verboseLogging) {
                        logger.warning("attribute is invalid in shader: [" + vb.getBufferType().name() + "]");
                    }

                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }
            }

            VertexBuffer[] attribs = context.boundAttribs;
            if (!context.attribIndexList.moveToNew(loc)) {
                if (verboseLogging) {
                    logger.info("GLES20.glEnableVertexAttribArray(" + loc + ")");
                }

                GLES20.glEnableVertexAttribArray(loc);
                //System.out.println("Enabled ATTRIB IDX: "+loc);
            }
            if (attribs[loc] != vb) {
                // NOTE: Use id from interleaved buffer if specified
                int bufId = idb != null ? idb.getId() : vb.getId();
                assert bufId != -1;

                if (bufId == -1) {
                    logger.warning("invalid buffer id");
                }

                if (context.boundArrayVBO != bufId) {
                    if (verboseLogging) {
                        logger.info("GLES20.glBindBuffer(" + GLES20.GL_ARRAY_BUFFER + ", " + bufId + ")");
                    }
                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufId);
                    context.boundArrayVBO = bufId;
                }

                vb.getData().clear();

                if (verboseLogging) {
                    logger.info("GLES20.glVertexAttribPointer("
                            + "location=" + loc + ", "
                            + "numComponents=" + vb.getNumComponents() + ", "
                            + "format=" + vb.getFormat() + ", "
                            + "isNormalized=" + vb.isNormalized() + ", "
                            + "stride=" + vb.getStride() + ", "
                            + "data.capacity=" + vb.getData().capacity() + ")");
                }

                Android22Workaround.glVertexAttribPointer(loc,
                                    vb.getNumComponents(),
                                    convertFormat(vb.getFormat()),
                                    vb.isNormalized(),
                                    vb.getStride(),
                                    0);

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
        /*        if (count > 1){
        ARBDrawInstanced.glDrawArraysInstancedARB(convertElementMode(mode), 0,
        vertCount, count);
        }else{*/
        if (verboseLogging) {
            logger.info("GLES20.glDrawArrays(" + vertCount + ")");
        }

        GLES20.glDrawArrays(convertElementMode(mode), 0, vertCount);
        /*
        }*/
    }

    public void drawTriangleList(VertexBuffer indexBuf, Mesh mesh, int count) {

        if (verboseLogging) {
            logger.info("drawTriangleList(" + count + ")");
        }

        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        if (indexBuf.isUpdateNeeded()) {
            if (verboseLogging) {
                logger.info("updateBufferData for indexBuf.");
            }
            updateBufferData(indexBuf);
        }

        int bufId = indexBuf.getId();
        assert bufId != -1;

        if (bufId == -1) {
            logger.info("invalid buffer id!");
        }

        if (context.boundElementArrayVBO != bufId) {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, {0})", bufId);
            }

            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufId);
            context.boundElementArrayVBO = bufId;
        }

        int vertCount = mesh.getVertexCount();
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);

        Buffer indexData = indexBuf.getData();

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
                    //ARBDrawInstanced.
                    throw new IllegalArgumentException("instancing is not supported.");
                    /*
                    GLES20.glDrawElementsInstancedARB(elMode,
                    elementLength,
                    fmt,
                    curOffset,
                    count);
                     */
                } else {
                    indexBuf.getData().position(curOffset);
                    if (verboseLogging) {
                        logger.log(Level.INFO, "glDrawElements(): {0}, {1}", new Object[]{elementLength, curOffset});
                    }

                    GLES20.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());
                    /*
                    glDrawRangeElements(elMode,
                    0,
                    vertCount,
                    elementLength,
                    fmt,
                    curOffset);
                     */
                }

                curOffset += elementLength * elSize;
            }
        } else {
            if (useInstancing) {
                throw new IllegalArgumentException("instancing is not supported.");
                //ARBDrawInstanced.
/*
                GLES20.glDrawElementsInstancedARB(convertElementMode(mesh.getMode()),
                indexBuf.getData().capacity(),
                convertFormat(indexBuf.getFormat()),
                0,
                count);
                 */
            } else {
                indexData.clear();

                if (verboseLogging) {
                    logger.log(Level.INFO, "glDrawElements(), indexBuf.capacity ({0}), vertCount ({1})", new Object[]{indexBuf.getData().capacity(), vertCount});
                }

                GLES20.glDrawElements(
                        convertElementMode(mesh.getMode()),
                        indexBuf.getData().capacity(),
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
                return GLES20.GL_POINTS;
            case Lines:
                return GLES20.GL_LINES;
            case LineLoop:
                return GLES20.GL_LINE_LOOP;
            case LineStrip:
                return GLES20.GL_LINE_STRIP;
            case Triangles:
                return GLES20.GL_TRIANGLES;
            case TriangleFan:
                return GLES20.GL_TRIANGLE_FAN;
            case TriangleStrip:
                return GLES20.GL_TRIANGLE_STRIP;
            default:
                throw new UnsupportedOperationException("Unrecognized mesh mode: " + mode);
        }
    }

    public void updateVertexArray(Mesh mesh) {
        logger.log(Level.INFO, "updateVertexArray({0})", mesh);
        int id = mesh.getId();
        /*
        if (id == -1){
        IntBuffer temp = intBuf1;
        //      ARBVertexArrayObject.glGenVertexArrays(temp);
        GLES20.glGenVertexArrays(temp);
        id = temp.get(0);
        mesh.setId(id);
        }
        
        if (context.boundVertexArray != id){
        //     ARBVertexArrayObject.glBindVertexArray(id);
        GLES20.glBindVertexArray(id);
        context.boundVertexArray = id;
        }
         */
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }

      
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
    }

    /**
     * renderMeshVertexArray renders a mesh using vertex arrays
     * @param mesh
     * @param lod
     * @param count
     */
    private void renderMeshVertexArray(Mesh mesh, int lod, int count) {
        if (verboseLogging) {
            logger.info("renderMeshVertexArray");
        }

      //  IntMap<VertexBuffer> buffers = mesh.getBuffers();
         for (VertexBuffer vb : mesh.getBufferList().getArray()){         

            if (vb.getBufferType() == Type.InterleavedData
                    || vb.getUsage() == Usage.CpuOnly // ignore cpu-only buffers
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttrib_Array(vb);
            } else {
                // interleaved
                VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
                setVertexAttrib_Array(vb, interleavedData);
            }
        }

        VertexBuffer indices = null;
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);//buffers.get(Type.Index.ordinal());
        }
        if (indices != null) {
            drawTriangleList_Array(indices, mesh, count);
        } else {
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glDrawArrays({0}, {1}, {2})", 
                        new Object[]{mesh.getMode(), 0, mesh.getVertexCount()});
            }

            GLES20.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    private void renderMeshDefault(Mesh mesh, int lod, int count) {
        if (verboseLogging) {
            logger.log(Level.INFO, "renderMeshDefault({0}, {1}, {2})", 
                    new Object[]{mesh, lod, count});
        }
        VertexBuffer indices = null;

        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }

        //IntMap<VertexBuffer> buffers = mesh.getBuffers();     ;
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);// buffers.get(Type.Index.ordinal());
        }
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
//            throw new UnsupportedOperationException("Cannot render without index buffer");
            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glDrawArrays({0}, 0, {1})", 
                        new Object[]{convertElementMode(mesh.getMode()), mesh.getVertexCount()});
            }

            GLES20.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
        if (context.pointSize != mesh.getPointSize()) {

            if (verboseLogging) {
                logger.log(Level.INFO, "GLES10.glPointSize({0})", mesh.getPointSize());
            }

            GLES10.glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {

            if (verboseLogging) {
                logger.log(Level.INFO, "GLES20.glLineWidth({0})", mesh.getLineWidth());
            }

            GLES20.glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        statistics.onMeshDrawn(mesh, lod);
//        if (GLContext.getCapabilities().GL_ARB_vertex_array_object){
//            renderMeshVertexArray(mesh, lod, count);
//        }else{

        if (useVBO) {
            if (verboseLogging) {
                logger.info("RENDERING A MESH USING VertexBufferObject");
            }

            renderMeshDefault(mesh, lod, count);
        } else {
            if (verboseLogging) {
                logger.info("RENDERING A MESH USING VertexArray");
            }

            renderMeshVertexArray(mesh, lod, count);
        }

//        }
    }

    /**
     * drawTriangleList_Array uses Vertex Array
     * @param indexBuf
     * @param mesh
     * @param count
     */
    public void drawTriangleList_Array(VertexBuffer indexBuf, Mesh mesh, int count) {
        if (verboseLogging) {
            logger.log(Level.INFO, "drawTriangleList_Array(Count = {0})", count);
        }

        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);
        if (useInstancing) {
            throw new IllegalArgumentException("Caps.MeshInstancing is not supported.");
        }

        int vertCount = mesh.getVertexCount();
        Buffer indexData = indexBuf.getData();
        indexData.clear();

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

                indexBuf.getData().position(curOffset);
                if (verboseLogging) {
                    logger.log(Level.INFO, "glDrawElements(): {0}, {1}", new Object[]{elementLength, curOffset});
                }

                GLES20.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());

                curOffset += elementLength * elSize;
            }
        } else {
            if (verboseLogging) {
                logger.log(Level.INFO, "glDrawElements(), indexBuf.capacity ({0}), vertCount ({1})", new Object[]{indexBuf.getData().capacity(), vertCount});
            }

            GLES20.glDrawElements(
                    convertElementMode(mesh.getMode()),
                    indexBuf.getData().capacity(),
                    convertFormat(indexBuf.getFormat()),
                    indexBuf.getData());
        }
    }

    /**
     * setVertexAttrib_Array uses Vertex Array
     * @param vb
     * @param idb
     */
    public void setVertexAttrib_Array(VertexBuffer vb, VertexBuffer idb) {
        if (verboseLogging) {
            logger.log(Level.INFO, "setVertexAttrib_Array({0}, {1})", new Object[]{vb, idb});
        }

        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Index buffers not allowed to be set to vertex attrib");
        }

        // Get shader
        int programId = context.boundShaderProgram;
        if (programId > 0) {
            VertexBuffer[] attribs = context.boundAttribs;

            Attribute attrib = boundShader.getAttribute(vb.getBufferType());
            int loc = attrib.getLocation();
            if (loc == -1) {
                //throw new IllegalArgumentException("Location is invalid for attrib: [" + vb.getBufferType().name() + "]");
                if (verboseLogging) {
                    logger.log(Level.WARNING, "attribute is invalid in shader: [{0}]", vb.getBufferType().name());
                }
                return;
            } else if (loc == -2) {
                String attributeName = "in" + vb.getBufferType().name();

                if (verboseLogging) {
                    logger.log(Level.INFO, "GLES20.glGetAttribLocation({0}, {1})", new Object[]{programId, attributeName});
                }

                loc = GLES20.glGetAttribLocation(programId, attributeName);
                if (loc < 0) {
                    attrib.setLocation(-1);
                    if (verboseLogging) {
                        logger.log(Level.WARNING, "attribute is invalid in shader: [{0}]", vb.getBufferType().name());
                    }
                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }

            }  // if (loc == -2)

            if ((attribs[loc] != vb) || vb.isUpdateNeeded()) {
                // NOTE: Use data from interleaved buffer if specified
                VertexBuffer avb = idb != null ? idb : vb;
                avb.getData().clear();
                avb.getData().position(vb.getOffset());

                if (verboseLogging) {
                    logger.log(Level.INFO,
                            "GLES20.glVertexAttribPointer(" + 
                            "location={0}, " +
                            "numComponents={1}, " +
                            "format={2}, " + 
                            "isNormalized={3}, " + 
                            "stride={4}, " + 
                            "data.capacity={5})", 
                            new Object[]{loc, vb.getNumComponents(), 
                                         vb.getFormat(), 
                                         vb.isNormalized(), 
                                         vb.getStride(), 
                                         avb.getData().capacity()});
                }


                // Upload attribute data
                GLES20.glVertexAttribPointer(loc,
                        vb.getNumComponents(),
                        convertFormat(vb.getFormat()),
                        vb.isNormalized(),
                        vb.getStride(),
                        avb.getData());
                checkGLError();

                GLES20.glEnableVertexAttribArray(loc);

                attribs[loc] = vb;
            } // if (attribs[loc] != vb)
        } else {
            throw new IllegalStateException("Cannot render mesh without shader bound");
        }
    }

    /**
     * setVertexAttrib_Array uses Vertex Array
     * @param vb
     */
    public void setVertexAttrib_Array(VertexBuffer vb) {
        setVertexAttrib_Array(vb, null);
    }

    public void setAlphaToCoverage(boolean value) {
        if (value) {
            GLES20.glEnable(GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE);
        } else {
            GLES20.glDisable(GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE);
        }
    }

    @Override
    public void invalidateState() {
        context.reset();
        boundShader = null;
        lastFb = null;
    }
}
