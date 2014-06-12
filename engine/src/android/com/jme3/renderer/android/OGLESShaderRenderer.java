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
package com.jme3.renderer.android;

import android.opengl.GLES10;
import android.opengl.GLES20;
import android.os.Build;
import com.jme3.asset.AndroidImageInfo;
import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.*;
import com.jme3.renderer.android.TextureUtil.AndroidGLImageFormat;
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
import jme3tools.shader.ShaderDebug;

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
    // initalDrawBuf and initialReadBuf are not used on ES,
    // http://www.khronos.org/opengles/sdk/docs/man/xhtml/glBindFramebuffer.xml
    //private int initialDrawBuf, initialReadBuf;
    private int glslVer;
    private int vertexTextureUnits;
    private int fragTextureUnits;
    private int vertexUniforms;
    private int fragUniforms;
    private int vertexAttribs;
//    private int maxFBOSamples;
    private final int maxFBOAttachs = 1; // Only 1 color attachment on ES
    private final int maxMRTFBOAttachs = 1; // FIXME for now, not sure if > 1 is needed for ES
    private int maxRBSize;
    private int maxTexSize;
    private int maxCubeTexSize;
    private int maxVertCount;
    private int maxTriCount;
    private boolean tdc;
    private FrameBuffer lastFb = null;
    private FrameBuffer mainFbOverride = null;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;
    //private final GL10 gl;
    private boolean powerVr = false;
    private boolean useVBO = false;

    public OGLESShaderRenderer() {
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
    
    private int extractVersion(String prefixStr, String versionStr) {
        if (versionStr != null) {
            int spaceIdx = versionStr.indexOf(" ", prefixStr.length());
            if (spaceIdx >= 1) {
                versionStr = versionStr.substring(prefixStr.length(), spaceIdx).trim();
            } else {
                versionStr = versionStr.substring(prefixStr.length()).trim();
            }
            //some device have ":" at the end of the version.
            versionStr = versionStr.replaceAll("\\:", "");
            float version = Float.parseFloat(versionStr);
            return (int) (version * 100);
        } else {
            return -1;
        }
    }

    public void initialize() {
        logger.log(Level.FINE, "Vendor: {0}", GLES20.glGetString(GLES20.GL_VENDOR));
        logger.log(Level.FINE, "Renderer: {0}", GLES20.glGetString(GLES20.GL_RENDERER));
        logger.log(Level.FINE, "Version: {0}", GLES20.glGetString(GLES20.GL_VERSION));
        logger.log(Level.FINE, "Shading Language Version: {0}", GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION));

        powerVr = GLES20.glGetString(GLES20.GL_RENDERER).contains("PowerVR");

        /*
        // Fix issue in TestRenderToMemory when GL_FRONT is the main
        // buffer being used.
        initialDrawBuf = glGetInteger(GL_DRAW_BUFFER);
        initialReadBuf = glGetInteger(GL_READ_BUFFER);

        // XXX: This has to be GL_BACK for canvas on Mac
        // Since initialDrawBuf is GL_FRONT for pbuffer, gotta
        // change this value later on ...
//        initialDrawBuf = GL_BACK;
//        initialReadBuf = GL_BACK;
         */

        // Check OpenGL version
        int openGlVer = extractVersion("OpenGL ES ", GLES20.glGetString(GLES20.GL_VERSION));
        if (openGlVer == -1) {
            glslVer = -1;
            throw new UnsupportedOperationException("OpenGL ES 2.0+ is required for OGLESShaderRenderer!");
        }

        // Check shader language version
        glslVer = extractVersion("OpenGL ES GLSL ES ", GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION));
        switch (glslVer) {
            // TODO: When new versions of OpenGL ES shader language come out,
            // update this.
            default:
                caps.add(Caps.GLSL100);
                break;
        }

        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, intBuf16);
        vertexTextureUnits = intBuf16.get(0);
        logger.log(Level.FINE, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, intBuf16);
        fragTextureUnits = intBuf16.get(0);
        logger.log(Level.FINE, "Texture Units: {0}", fragTextureUnits);

        // Multiply vector count by 4 to get float count.
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, intBuf16);
        vertexUniforms = intBuf16.get(0) * 4;
        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);

        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, intBuf16);
        fragUniforms = intBuf16.get(0) * 4;
        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);

        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, intBuf16);
        int varyingFloats = intBuf16.get(0) * 4;
        logger.log(Level.FINER, "Varying Floats: {0}", varyingFloats);

        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, intBuf16);
        vertexAttribs = intBuf16.get(0);
        logger.log(Level.FINE, "Vertex Attributes: {0}", vertexAttribs);

        GLES20.glGetIntegerv(GLES20.GL_SUBPIXEL_BITS, intBuf16);
        int subpixelBits = intBuf16.get(0);
        logger.log(Level.FINE, "Subpixel Bits: {0}", subpixelBits);

//        GLES10.glGetIntegerv(GLES10.GL_MAX_ELEMENTS_VERTICES, intBuf16);
//        maxVertCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);
//
//        GLES10.glGetIntegerv(GLES10.GL_MAX_ELEMENTS_INDICES, intBuf16);
//        maxTriCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);

        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, intBuf16);
        maxTexSize = intBuf16.get(0);
        logger.log(Level.FINE, "Maximum Texture Resolution: {0}", maxTexSize);

        GLES20.glGetIntegerv(GLES20.GL_MAX_CUBE_MAP_TEXTURE_SIZE, intBuf16);
        maxCubeTexSize = intBuf16.get(0);
        logger.log(Level.FINE, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);

        GLES20.glGetIntegerv(GLES20.GL_MAX_RENDERBUFFER_SIZE, intBuf16);
        maxRBSize = intBuf16.get(0);
        logger.log(Level.FINER, "FBO RB Max Size: {0}", maxRBSize);

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

        if (ctxCaps.GL_ARB_texture_buffer_object)
        caps.add(Caps.TextureBuffer);

        if (ctxCaps.GL_ARB_texture_float){
        if (ctxCaps.GL_ARB_half_float_pixel){
        caps.add(Caps.FloatTexture);
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
        logger.log(Level.FINE, "GL_EXTENSIONS: {0}", extensions);

        // Get number of compressed formats available.
        GLES20.glGetIntegerv(GLES20.GL_NUM_COMPRESSED_TEXTURE_FORMATS, intBuf16);
        int numCompressedFormats = intBuf16.get(0);

        // Allocate buffer for compressed formats.
        IntBuffer compressedFormats = BufferUtils.createIntBuffer(numCompressedFormats);
        GLES20.glGetIntegerv(GLES20.GL_COMPRESSED_TEXTURE_FORMATS, compressedFormats);

        // Check for errors after all glGet calls.
        RendererUtil.checkGLError();

        // Print compressed formats.
        for (int i = 0; i < numCompressedFormats; i++) {
            logger.log(Level.FINE, "Compressed Texture Formats: {0}", compressedFormats.get(i));
        }

        TextureUtil.loadTextureFeatures(extensions);

        applyRenderState(RenderState.DEFAULT);
        GLES20.glDisable(GLES20.GL_DITHER);
        RendererUtil.checkGLError();

        useVBO = false;

        // NOTE: SDK_INT is only available since 1.6,
        // but for jME3 it doesn't matter since android versions 1.5 and below
        // are not supported.
        if (Build.VERSION.SDK_INT >= 9){
            logger.log(Level.FINE, "Force-enabling VBO (Android 2.3 or higher)");
            useVBO = true;
        } else {
            useVBO = false;
        }

        logger.log(Level.FINE, "Caps: {0}", caps);
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
        GLES20.glDepthRangef(start, end);
        RendererUtil.checkGLError();
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
            GLES20.glClear(bits);
            RendererUtil.checkGLError();
        }
    }

    public void setBackgroundColor(ColorRGBA color) {
        GLES20.glClearColor(color.r, color.g, color.b, color.a);
        RendererUtil.checkGLError();
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
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);
            RendererUtil.checkGLError();
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
            RendererUtil.checkGLError();
            context.depthTestEnabled = false;
        }

        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            GLES20.glDepthMask(true);
            RendererUtil.checkGLError();
            context.depthWriteEnabled = true;
        } else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            GLES20.glDepthMask(false);
            RendererUtil.checkGLError();
            context.depthWriteEnabled = false;
        }
        if (state.isColorWrite() && !context.colorWriteEnabled) {
            GLES20.glColorMask(true, true, true, true);
            RendererUtil.checkGLError();
            context.colorWriteEnabled = true;
        } else if (!state.isColorWrite() && context.colorWriteEnabled) {
            GLES20.glColorMask(false, false, false, false);
            RendererUtil.checkGLError();
            context.colorWriteEnabled = false;
        }
//        if (state.isPointSprite() && !context.pointSprite) {
////            GLES20.glEnable(GLES20.GL_POINT_SPRITE);
////            GLES20.glTexEnvi(GLES20.GL_POINT_SPRITE, GLES20.GL_COORD_REPLACE, GLES20.GL_TRUE);
////            GLES20.glEnable(GLES20.GL_VERTEX_PROGRAM_POINT_SIZE);
////            GLES20.glPointParameterf(GLES20.GL_POINT_SIZE_MIN, 1.0f);
//        } else if (!state.isPointSprite() && context.pointSprite) {
////            GLES20.glDisable(GLES20.GL_POINT_SPRITE);
//        }

        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
                GLES20.glPolygonOffset(state.getPolyOffsetFactor(),
                        state.getPolyOffsetUnits());
                RendererUtil.checkGLError();

                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            } else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    GLES20.glPolygonOffset(state.getPolyOffsetFactor(),
                            state.getPolyOffsetUnits());
                    RendererUtil.checkGLError();

                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        } else {
            if (context.polyOffsetEnabled) {
                GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
                RendererUtil.checkGLError();

                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                RendererUtil.checkGLError();
            } else {
                GLES20.glEnable(GLES20.GL_CULL_FACE);
                RendererUtil.checkGLError();
            }

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    GLES20.glCullFace(GLES20.GL_BACK);
                    RendererUtil.checkGLError();
                    break;
                case Front:
                    GLES20.glCullFace(GLES20.GL_FRONT);
                    RendererUtil.checkGLError();
                    break;
                case FrontAndBack:
                    GLES20.glCullFace(GLES20.GL_FRONT_AND_BACK);
                    RendererUtil.checkGLError();
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                GLES20.glDisable(GLES20.GL_BLEND);
                RendererUtil.checkGLError();
            } else {
                GLES20.glEnable(GLES20.GL_BLEND);
                switch (state.getBlendMode()) {
                    case Off:
                        break;
                    case Additive:
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);
                        break;
                    case AlphaAdditive:
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
                        break;
                    case Color:
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_COLOR);
                        break;
                    case Alpha:
                        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case PremultAlpha:
                        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case Modulate:
                        GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_ZERO);
                        break;
                    case ModulateX2:
                        GLES20.glBlendFunc(GLES20.GL_DST_COLOR, GLES20.GL_SRC_COLOR);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unrecognized blend mode: "
                                + state.getBlendMode());
                }
                RendererUtil.checkGLError();
            }
            context.blendMode = state.getBlendMode();
        }
    }

    /*********************************************************************\
    |* Camera and World transforms                                       *|
    \*********************************************************************/
    public void setViewPort(int x, int y, int w, int h) {
        if (x != vpX || vpY != y || vpW != w || vpH != h) {
            GLES20.glViewport(x, y, w, h);
            RendererUtil.checkGLError();

            vpX = x;
            vpY = y;
            vpW = w;
            vpH = h;
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            RendererUtil.checkGLError();
            context.clipRectEnabled = true;
        }
        if (clipX != x || clipY != y || clipW != width || clipH != height) {
            GLES20.glScissor(x, y, width, height);
            RendererUtil.checkGLError();
            clipX = x;
            clipY = y;
            clipW = width;
            clipH = height;
        }
    }

    public void clearClipRect() {
        if (context.clipRectEnabled) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            RendererUtil.checkGLError();
            context.clipRectEnabled = false;

            clipX = 0;
            clipY = 0;
            clipW = 0;
            clipH = 0;
        }
    }

    public void onFrame() {
        RendererUtil.checkGLErrorForced();

        objManager.deleteUnused(this);
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
        int loc = GLES20.glGetUniformLocation(shader.getId(), uniform.getName());
        RendererUtil.checkGLError();

        if (loc < 0) {
            uniform.setLocation(-1);
            // uniform is not declared in shader
        } else {
            uniform.setLocation(loc);
        }
    }

    protected void bindProgram(Shader shader) {
        int shaderId = shader.getId();
        if (context.boundShaderProgram != shaderId) {
            GLES20.glUseProgram(shaderId);
            RendererUtil.checkGLError();

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

        if (context.boundShaderProgram != shaderId) {
            GLES20.glUseProgram(shaderId);
            RendererUtil.checkGLError();

            statistics.onShaderUse(shader, true);
            boundShader = shader;
            context.boundShaderProgram = shaderId;
        } else {
            statistics.onShaderUse(shader, false);
        }

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
            // removed logging the warning to avoid flooding the log
            // (LWJGL also doesn't post a warning)
            //logger.log(Level.FINEST, "Uniform value is not set yet. Shader: {0}, Uniform: {1}",
            //        new Object[]{shader.toString(), uniform.toString()});
            return; // value not set yet..
        }

        statistics.onUniformSet();

        uniform.clearUpdateNeeded();
        FloatBuffer fb;
        IntBuffer ib;
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                GLES20.glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                GLES20.glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                GLES20.glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    GLES20.glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else if (val instanceof Vector4f) {
                    Vector4f c = (Vector4f) val;
                    GLES20.glUniform4f(loc, c.x, c.y, c.z, c.w);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    GLES20.glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                GLES20.glUniform1i(loc, b.booleanValue() ? GLES20.GL_TRUE : GLES20.GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                GLES20.glUniformMatrix3fv(loc, 1, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                GLES20.glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case IntArray:
                ib = (IntBuffer) uniform.getValue();
                GLES20.glUniform1iv(loc, ib.limit(), ib);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform1fv(loc, fb.limit(), fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform2fv(loc, fb.limit() / 2, fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform3fv(loc, fb.limit() / 3, fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniform4fv(loc, fb.limit() / 4, fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                GLES20.glUniformMatrix4fv(loc, fb.limit() / 16, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                GLES20.glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + uniform.getVarType());
        }
        RendererUtil.checkGLError();
    }

    protected void updateShaderUniforms(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.getValue(i);
            if (uniform.isUpdateNeeded()) {
                updateUniform(shader, uniform);
            }
        }
    }

    protected void resetUniformLocations(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
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

    public void updateShaderSourceData(ShaderSource source) {
        int id = source.getId();
        if (id == -1) {
            // Create id
            id = GLES20.glCreateShader(convertShaderType(source.getType()));
            RendererUtil.checkGLError();

            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader.");
            }
            source.setId(id);
        }

        if (!source.getLanguage().equals("GLSL100")) {
            throw new RendererException("This shader cannot run in OpenGL ES. "
                                      + "Only GLSL 1.0 shaders are supported.");
        }

        // upload shader source
        // merge the defines and source code
        byte[] definesCodeData = source.getDefines().getBytes();
        byte[] sourceCodeData = source.getSource().getBytes();
        ByteBuffer codeBuf = BufferUtils.createByteBuffer(definesCodeData.length
                                                        + sourceCodeData.length);
        codeBuf.put(definesCodeData);
        codeBuf.put(sourceCodeData);
        codeBuf.flip();

        if (powerVr && source.getType() == ShaderType.Vertex) {
            // XXX: This is to fix a bug in old PowerVR, remove
            // when no longer applicable.
            GLES20.glShaderSource(
                    id, source.getDefines()
                    + source.getSource());
        } else {
            String precision ="";
            if (source.getType() == ShaderType.Fragment) {
                precision =  "precision mediump float;\n";
            }
            GLES20.glShaderSource(
                    id,
                    precision
                    +source.getDefines()
                    + source.getSource());
        }
//        int range[] = new int[2];
//        int precision[] =  new int[1];
//        GLES20.glGetShaderPrecisionFormat(GLES20.GL_VERTEX_SHADER, GLES20.GL_HIGH_FLOAT, range, 0, precision, 0);
//        System.out.println("PRECISION HIGH FLOAT VERTEX");
//        System.out.println("range "+range[0]+"," +range[1]);
//        System.out.println("precision "+precision[0]);

        GLES20.glCompileShader(id);
        RendererUtil.checkGLError();

        GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, intBuf1);
        RendererUtil.checkGLError();

        boolean compiledOK = intBuf1.get(0) == GLES20.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            GLES20.glGetShaderiv(id, GLES20.GL_INFO_LOG_LENGTH, intBuf1);
            RendererUtil.checkGLError();
            infoLog = GLES20.glGetShaderInfoLog(id);
        }

        if (compiledOK) {
            if (infoLog != null) {
                logger.log(Level.FINE, "compile success: {0}, {1}", new Object[]{source.getName(), infoLog});
            } else {
                logger.log(Level.FINE, "compile success: {0}", source.getName());
            }
            source.clearUpdateNeeded();
        } else {
           logger.log(Level.WARNING, "Bad compile of:\n{0}",
                    new Object[]{ShaderDebug.formatShaderSource(source.getDefines(), source.getSource(),stringBuf.toString())});
            if (infoLog != null) {
                throw new RendererException("compile error in:" + source + " error:" + infoLog);
            } else {
                throw new RendererException("compile error in:" + source + " error: <not provided>");
            }
        }
    }

    public void updateShaderData(Shader shader) {
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program
            id = GLES20.glCreateProgram();
            RendererUtil.checkGLError();

            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader program.");
            }

            shader.setId(id);
            needRegister = true;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.isUpdateNeeded()) {
                updateShaderSourceData(source);
            }

            GLES20.glAttachShader(id, source.getId());
            RendererUtil.checkGLError();
        }

        // link shaders to program
        GLES20.glLinkProgram(id);
        RendererUtil.checkGLError();

        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, intBuf1);
        RendererUtil.checkGLError();

        boolean linkOK = intBuf1.get(0) == GLES20.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            GLES20.glGetProgramiv(id, GLES20.GL_INFO_LOG_LENGTH, intBuf1);
            RendererUtil.checkGLError();

            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                infoLog = GLES20.glGetProgramInfoLog(id);
                RendererUtil.checkGLError();
            }
        }

        if (linkOK) {
            if (infoLog != null) {
                logger.log(Level.FINE, "shader link success. \n{0}", infoLog);
            } else {
                logger.fine("shader link success");
            }
            shader.clearUpdateNeeded();
            if (needRegister) {
                // Register shader for clean up if it was created in this method.
                objManager.registerObject(shader);
                statistics.onNewShader();
            } else {
                // OpenGL spec: uniform locations may change after re-link
                resetUniformLocations(shader);
            }
        } else {
            if (infoLog != null) {
                throw new RendererException("Shader link failure, shader:" + shader + " info:" + infoLog);
            } else {
                throw new RendererException("Shader link failure, shader:" + shader + " info: <not provided>");
            }
        }
    }

    public void setShader(Shader shader) {
        if (shader == null) {
            throw new IllegalArgumentException("Shader cannot be null");
        } else {
            if (shader.isUpdateNeeded()) {
                updateShaderData(shader);
            }

            // NOTE: might want to check if any of the
            // sources need an update?

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

        source.clearUpdateNeeded();

        GLES20.glDeleteShader(source.getId());
        RendererUtil.checkGLError();

        source.resetObject();
    }

    public void deleteShader(Shader shader) {
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {
                GLES20.glDetachShader(shader.getId(), source.getId());
                RendererUtil.checkGLError();

                deleteShaderSource(source);
            }
        }

        GLES20.glDeleteProgram(shader.getId());
        RendererUtil.checkGLError();

        statistics.onDeleteShader();
        shader.resetObject();
    }

    /*********************************************************************\
    |* Framebuffers                                                      *|
    \*********************************************************************/
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        copyFrameBuffer(src, dst, true);
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
            throw new RendererException("Copy framebuffer not implemented yet.");

//        if (GLContext.getCapabilities().GL_EXT_framebuffer_blit) {
//            int srcX0 = 0;
//            int srcY0 = 0;
//            int srcX1 = 0;
//            int srcY1 = 0;
//
//            int dstX0 = 0;
//            int dstY0 = 0;
//            int dstX1 = 0;
//            int dstY1 = 0;
//
//            int prevFBO = context.boundFBO;
//
//            if (mainFbOverride != null) {
//                if (src == null) {
//                    src = mainFbOverride;
//                }
//                if (dst == null) {
//                    dst = mainFbOverride;
//                }
//            }
//
//            if (src != null && src.isUpdateNeeded()) {
//                updateFrameBuffer(src);
//            }
//
//            if (dst != null && dst.isUpdateNeeded()) {
//                updateFrameBuffer(dst);
//            }
//
//            if (src == null) {
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                srcX0 = vpX;
//                srcY0 = vpY;
//                srcX1 = vpX + vpW;
//                srcY1 = vpY + vpH;
//            } else {
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, src.getId());
//                srcX1 = src.getWidth();
//                srcY1 = src.getHeight();
//            }
//            if (dst == null) {
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                dstX0 = vpX;
//                dstY0 = vpY;
//                dstX1 = vpX + vpW;
//                dstY1 = vpY + vpH;
//            } else {
//                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, dst.getId());
//                dstX1 = dst.getWidth();
//                dstY1 = dst.getHeight();
//            }
//
//
//            int mask = GL_COLOR_BUFFER_BIT;
//            if (copyDepth) {
//                mask |= GL_DEPTH_BUFFER_BIT;
//            }
//            GLES20.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1,
//                    dstX0, dstY0, dstX1, dstY1, mask,
//                    GL_NEAREST);
//
//
//            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, prevFBO);
//            try {
//                checkFrameBufferError();
//            } catch (IllegalStateException ex) {
//                logger.log(Level.SEVERE, "Source FBO:\n{0}", src);
//                logger.log(Level.SEVERE, "Dest FBO:\n{0}", dst);
//                throw ex;
//            }
//        } else {
//            throw new RendererException("EXT_framebuffer_blit required.");
//            // TODO: support non-blit copies?
//        }
    }

    private void checkFrameBufferStatus(FrameBuffer fb) {
        try {
            checkFrameBufferError();
        } catch (IllegalStateException ex) {
            logger.log(Level.SEVERE, "=== jMonkeyEngine FBO State ===\n{0}", fb);
            printRealFrameBufferInfo(fb);
            throw ex;
        }
    }

    private void checkFrameBufferError() {
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        switch (status) {
            case GLES20.GL_FRAMEBUFFER_COMPLETE:
                break;
            case GLES20.GL_FRAMEBUFFER_UNSUPPORTED:
                //Choose different formats
                throw new IllegalStateException("Framebuffer object format is "
                        + "unsupported by the video hardware.");
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                throw new IllegalStateException("Framebuffer has erronous attachment.");
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                throw new IllegalStateException("Framebuffer doesn't have any renderbuffers attached.");
            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                throw new IllegalStateException("Framebuffer attachments must have same dimensions.");
//            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
//                throw new IllegalStateException("Framebuffer attachments must have same formats.");
//            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
//                throw new IllegalStateException("Incomplete draw buffer.");
//            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
//                throw new IllegalStateException("Incomplete read buffer.");
//            case GLES20.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT:
//                throw new IllegalStateException("Incomplete multisample buffer.");
            default:
                //Programming error; will fail on all hardware
                throw new IllegalStateException("Some video driver error "
                        + "or programming error occured. "
                        + "Framebuffer object status is invalid: " + status);
        }
    }

    private void printRealRenderBufferInfo(FrameBuffer fb, RenderBuffer rb, String name) {
        System.out.println("== Renderbuffer " + name + " ==");
        System.out.println("RB ID: " + rb.getId());
        System.out.println("Is proper? " + GLES20.glIsRenderbuffer(rb.getId()));

        int attachment = convertAttachmentSlot(rb.getSlot());

        intBuf16.clear();
        GLES20.glGetFramebufferAttachmentParameteriv(GLES20.GL_FRAMEBUFFER,
                attachment, GLES20.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, intBuf16);
        int type = intBuf16.get(0);

        intBuf16.clear();
        GLES20.glGetFramebufferAttachmentParameteriv(GLES20.GL_FRAMEBUFFER,
                attachment, GLES20.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, intBuf16);
        int rbName = intBuf16.get(0);

        switch (type) {
            case GLES20.GL_NONE:
                System.out.println("Type: None");
                break;
            case GLES20.GL_TEXTURE:
                System.out.println("Type: Texture");
                break;
            case GLES20.GL_RENDERBUFFER:
                System.out.println("Type: Buffer");
                System.out.println("RB ID: " + rbName);
                break;
        }



    }

    private void printRealFrameBufferInfo(FrameBuffer fb) {
//        boolean doubleBuffer = GLES20.glGetBooleanv(GLES20.GL_DOUBLEBUFFER);
        boolean doubleBuffer = false; // FIXME
//        String drawBuf = getTargetBufferName(glGetInteger(GL_DRAW_BUFFER));
//        String readBuf = getTargetBufferName(glGetInteger(GL_READ_BUFFER));

        int fbId = fb.getId();
        intBuf16.clear();
//        int curDrawBinding = GLES20.glGetIntegerv(GLES20.GL_DRAW_FRAMEBUFFER_BINDING);
//        int curReadBinding = glGetInteger(ARBFramebufferObject.GL_READ_FRAMEBUFFER_BINDING);

        System.out.println("=== OpenGL FBO State ===");
        System.out.println("Context doublebuffered? " + doubleBuffer);
        System.out.println("FBO ID: " + fbId);
        System.out.println("Is proper? " + GLES20.glIsFramebuffer(fbId));
//        System.out.println("Is bound to draw? " + (fbId == curDrawBinding));
//        System.out.println("Is bound to read? " + (fbId == curReadBinding));
//        System.out.println("Draw buffer: " + drawBuf);
//        System.out.println("Read buffer: " + readBuf);

        if (context.boundFBO != fbId) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbId);
            context.boundFBO = fbId;
        }

        if (fb.getDepthBuffer() != null) {
            printRealRenderBufferInfo(fb, fb.getDepthBuffer(), "Depth");
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
            printRealRenderBufferInfo(fb, fb.getColorBuffer(i), "Color" + i);
        }
    }

    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        int id = rb.getId();
        if (id == -1) {
            GLES20.glGenRenderbuffers(1, intBuf1);
            RendererUtil.checkGLError();

            id = intBuf1.get(0);
            rb.setId(id);
        }

        if (context.boundRB != id) {
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, id);
            RendererUtil.checkGLError();

            context.boundRB = id;
        }

        if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize) {
            throw new RendererException("Resolution " + fb.getWidth()
                    + ":" + fb.getHeight() + " is not supported.");
        }

        AndroidGLImageFormat imageFormat = TextureUtil.getImageFormat(rb.getFormat());
        if (imageFormat.renderBufferStorageFormat == 0) {
            throw new RendererException("The format '" + rb.getFormat() + "' cannot be used for renderbuffers.");
        }

//        if (fb.getSamples() > 1 && GLContext.getCapabilities().GL_EXT_framebuffer_multisample) {
        if (fb.getSamples() > 1) {
//            // FIXME
            throw new RendererException("Multisample FrameBuffer is not supported yet.");
//            int samples = fb.getSamples();
//            if (maxFBOSamples < samples) {
//                samples = maxFBOSamples;
//            }
//            glRenderbufferStorageMultisampleEXT(GL_RENDERBUFFER_EXT,
//                    samples,
//                    glFmt.internalFormat,
//                    fb.getWidth(),
//                    fb.getHeight());
        } else {
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                    imageFormat.renderBufferStorageFormat,
                    fb.getWidth(),
                    fb.getHeight());

            RendererUtil.checkGLError();
        }
    }

    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == -100) {
            return GLES20.GL_DEPTH_ATTACHMENT;
        } else if (attachmentSlot == 0) {
            return GLES20.GL_COLOR_ATTACHMENT0;
        } else {
            throw new UnsupportedOperationException("Android does not support multiple color attachments to an FBO");
        }
    }

    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        Texture tex = rb.getTexture();
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType());

            // NOTE: For depth textures, sets nearest/no-mips mode
            // Required to fix "framebuffer unsupported"
            // for old NVIDIA drivers!
            setupTextureParams(tex);
        }

        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType()),
                image.getId(),
                0);

        RendererUtil.checkGLError();
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
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                    convertAttachmentSlot(rb.getSlot()),
                    GLES20.GL_RENDERBUFFER,
                    rb.getId());

            RendererUtil.checkGLError();
        }
    }

    public void updateFrameBuffer(FrameBuffer fb) {
        int id = fb.getId();
        if (id == -1) {
            intBuf1.clear();
            // create FBO
            GLES20.glGenFramebuffers(1, intBuf1);
            RendererUtil.checkGLError();

            id = intBuf1.get(0);
            fb.setId(id);
            objManager.registerObject(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id);
            RendererUtil.checkGLError();

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

    public void setMainFrameBufferOverride(FrameBuffer fb){
        mainFbOverride = fb;
    }

    public void setFrameBuffer(FrameBuffer fb) {
        if (fb == null && mainFbOverride != null) {
            fb = mainFbOverride;
        }

        if (lastFb == fb) {
            if (fb == null || !fb.isUpdateNeeded()) {
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

//                    int textureType = convertTextureType(tex.getType(), tex.getImage().getMultiSamples(), rb.getFace());
                    int textureType = convertTextureType(tex.getType());
                    GLES20.glGenerateMipmap(textureType);
                    RendererUtil.checkGLError();
                }
            }
        }

        if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                RendererUtil.checkGLError();

                statistics.onFrameBufferUse(null, true);

                context.boundFBO = 0;
            }

            /*
            // select back buffer
            if (context.boundDrawBuf != -1) {
                glDrawBuffer(initialDrawBuf);
                context.boundDrawBuf = -1;
            }
            if (context.boundReadBuf != -1) {
                glReadBuffer(initialReadBuf);
                context.boundReadBuf = -1;
            }
             */

            lastFb = null;
        } else {
            if (fb.getNumColorBuffers() == 0 && fb.getDepthBuffer() == null) {
                throw new IllegalArgumentException("The framebuffer: " + fb
                        + "\nDoesn't have any color/depth buffers");
            }

            if (fb.isUpdateNeeded()) {
                updateFrameBuffer(fb);
            }

            if (context.boundFBO != fb.getId()) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb.getId());
                RendererUtil.checkGLError();

                statistics.onFrameBufferUse(fb, true);

                // update viewport to reflect framebuffer's resolution
                setViewPort(0, 0, fb.getWidth(), fb.getHeight());

                context.boundFBO = fb.getId();
            } else {
                statistics.onFrameBufferUse(fb, false);
            }
            if (fb.getNumColorBuffers() == 0) {
//                // make sure to select NONE as draw buf
//                // no color buffer attached. select NONE
                if (context.boundDrawBuf != -2) {
//                    glDrawBuffer(GL_NONE);
                    context.boundDrawBuf = -2;
                }
                if (context.boundReadBuf != -2) {
//                    glReadBuffer(GL_NONE);
                    context.boundReadBuf = -2;
                }
            } else {
                if (fb.getNumColorBuffers() > maxFBOAttachs) {
                    throw new RendererException("Framebuffer has more color "
                            + "attachments than are supported"
                            + " by the video hardware!");
                }
                if (fb.isMultiTarget()) {
                    if (fb.getNumColorBuffers() > maxMRTFBOAttachs) {
                        throw new RendererException("Framebuffer has more"
                                + " multi targets than are supported"
                                + " by the video hardware!");
                    }

                    if (context.boundDrawBuf != 100 + fb.getNumColorBuffers()) {
                        intBuf16.clear();
                        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
                            intBuf16.put(GLES20.GL_COLOR_ATTACHMENT0 + i);
                        }

                        intBuf16.flip();
//                        glDrawBuffers(intBuf16);
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                } else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (context.boundDrawBuf != rb.getSlot()) {
                        GLES20.glActiveTexture(convertAttachmentSlot(rb.getSlot()));
                        RendererUtil.checkGLError();

                        context.boundDrawBuf = rb.getSlot();
                    }
                }
            }

            assert fb.getId() >= 0;
            assert context.boundFBO == fb.getId();

            lastFb = fb;

            checkFrameBufferStatus(fb);
        }
    }

    /**
     * Reads the Color Buffer from OpenGL and stores into the ByteBuffer.
     * Make sure to call setViewPort with the appropriate viewport size before
     * calling readFrameBuffer.
     * @param fb FrameBuffer
     * @param byteBuf ByteBuffer to store the Color Buffer from OpenGL
     */
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        if (fb != null) {
            RenderBuffer rb = fb.getColorBuffer();
            if (rb == null) {
                throw new IllegalArgumentException("Specified framebuffer"
                        + " does not have a colorbuffer");
            }

            setFrameBuffer(fb);
        } else {
            setFrameBuffer(null);
        }

        GLES20.glReadPixels(vpX, vpY, vpW, vpH, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuf);
        RendererUtil.checkGLError();
    }

    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1.put(0, rb.getId());
        GLES20.glDeleteRenderbuffers(1, intBuf1);
        RendererUtil.checkGLError();
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
        if (fb.getId() != -1) {
            if (context.boundFBO == fb.getId()) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                RendererUtil.checkGLError();

                context.boundFBO = 0;
            }

            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1.put(0, fb.getId());
            GLES20.glDeleteFramebuffers(1, intBuf1);
            RendererUtil.checkGLError();

            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

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

        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, magFilter);
        RendererUtil.checkGLError();

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
                GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));

                // fall down here is intentional..
//          case OneDimensional:
                GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));

                RendererUtil.checkGLError();
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
     * activates and binds the texture
     * @param img
     * @param type
     */
    public void updateTexImageData(Image img, Texture.Type type) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            GLES20.glGenTextures(1, intBuf1);
            RendererUtil.checkGLError();

            texId = intBuf1.get(0);
            img.setId(texId);
            objManager.registerObject(img);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type);
        if (context.boundTextures[0] != img) {
            if (context.boundTextureUnit != 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                RendererUtil.checkGLError();

                context.boundTextureUnit = 0;
            }

            GLES20.glBindTexture(target, texId);
            RendererUtil.checkGLError();

            context.boundTextures[0] = img;
        }

        boolean needMips = false;
        if (img.isGeneratedMipmapsRequired()) {
            needMips = true;
            img.setMipmapsGenerated(true);
        }

        if (target == GLES20.GL_TEXTURE_CUBE_MAP) {
            // Check max texture size before upload
            if (img.getWidth() > maxCubeTexSize || img.getHeight() > maxCubeTexSize) {
                throw new RendererException("Cannot upload cubemap " + img + ". The maximum supported cubemap resolution is " + maxCubeTexSize);
            }
        } else {
            if (img.getWidth() > maxTexSize || img.getHeight() > maxTexSize) {
                throw new RendererException("Cannot upload texture " + img + ". The maximum supported texture resolution is " + maxTexSize);
            }
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
                    TextureUtil.uploadTextureBitmap(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, bmps.get(i).getBitmap(), needMips);
                    bmps.get(i).notifyBitmapUploaded();
                }
            } else {
                // Standard jme3 image data
                List<ByteBuffer> data = img.getData();
                if (data.size() != 6) {
                    throw new UnsupportedOperationException("Invalid texture: " + img
                            + "Cubemap textures must contain 6 data units.");
                }
                for (int i = 0; i < 6; i++) {
                    TextureUtil.uploadTextureAny(img, GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, needMips);
                }
            }
        } else {
            TextureUtil.uploadTextureAny(img, target, 0, needMips);
            if (img.getEfficentData() instanceof AndroidImageInfo) {
                AndroidImageInfo info = (AndroidImageInfo) img.getEfficentData();
                info.notifyBitmapUploaded();
            }
        }

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        Image image = tex.getImage();
        if (image.isUpdateNeeded() || (image.isGeneratedMipmapsRequired() && !image.isMipmapsGenerated()) ) {
            updateTexImageData(image, tex.getType());
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
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            GLES20.glBindTexture(type, texId);
            RendererUtil.checkGLError();

            textures[unit] = image;

            statistics.onTextureUse(tex.getImage(), true);
        } else {
            statistics.onTextureUse(tex.getImage(), false);
        }

        setupTextureParams(tex);
    }

    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
      setTexture(0, tex);
      TextureUtil.uploadSubTexture(pixels, convertTextureType(tex.getType()), 0, x, y);
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

            GLES20.glDeleteTextures(1, intBuf1);
            RendererUtil.checkGLError();

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

    private int convertVertexBufferFormat(Format format) {
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
        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer
            GLES20.glGenBuffers(1, intBuf1);
            RendererUtil.checkGLError();

            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerObject(vb);

            created = true;
        }

        // bind buffer
        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = GLES20.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                GLES20.glBindBuffer(target, bufId);
                RendererUtil.checkGLError();

                context.boundElementArrayVBO = bufId;
            }
        } else {
            target = GLES20.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                GLES20.glBindBuffer(target, bufId);
                RendererUtil.checkGLError();

                context.boundArrayVBO = bufId;
            }
        }

        int usage = convertUsage(vb.getUsage());
        vb.getData().rewind();

        if (created || vb.hasDataSizeChanged()) {
            // upload data based on format
            int size = vb.getData().limit() * vb.getFormat().getComponentSize();

            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    GLES20.glBufferData(target, size, (ByteBuffer) vb.getData(), usage);
                    RendererUtil.checkGLError();
                    break;
                case Short:
                case UnsignedShort:
                    GLES20.glBufferData(target, size, (ShortBuffer) vb.getData(), usage);
                    RendererUtil.checkGLError();
                    break;
                case Int:
                case UnsignedInt:
                    GLES20.glBufferData(target, size, (IntBuffer) vb.getData(), usage);
                    RendererUtil.checkGLError();
                    break;
                case Float:
                    GLES20.glBufferData(target, size, (FloatBuffer) vb.getData(), usage);
                    RendererUtil.checkGLError();
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
            }
        } else {
            int size = vb.getData().limit() * vb.getFormat().getComponentSize();

            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    GLES20.glBufferSubData(target, 0, size, (ByteBuffer) vb.getData());
                    RendererUtil.checkGLError();
                    break;
                case Short:
                case UnsignedShort:
                    GLES20.glBufferSubData(target, 0, size, (ShortBuffer) vb.getData());
                    RendererUtil.checkGLError();
                    break;
                case Int:
                case UnsignedInt:
                    GLES20.glBufferSubData(target, 0, size, (IntBuffer) vb.getData());
                    RendererUtil.checkGLError();
                    break;
                case Float:
                    GLES20.glBufferSubData(target, 0, size, (FloatBuffer) vb.getData());
                    RendererUtil.checkGLError();
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
            }
        }
        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);

            GLES20.glDeleteBuffers(1, intBuf1);
            RendererUtil.checkGLError();

            vb.resetObject();
        }
    }

    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];

            GLES20.glDisableVertexAttribArray(idx);
            RendererUtil.checkGLError();

            context.boundAttribs[idx] = null;
        }
        context.attribIndexList.copyNewToOld();
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
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
                return; // not defined
            }

            if (loc == -2) {
//                stringBuf.setLength(0);
//                stringBuf.append("in").append(vb.getBufferType().name()).append('\0');
//                updateNameBuffer();

                String attributeName = "in" + vb.getBufferType().name();
                loc = GLES20.glGetAttribLocation(programId, attributeName);
                RendererUtil.checkGLError();

                // not really the name of it in the shader (inPosition\0) but
                // the internal name of the enum (Position).
                if (loc < 0) {
                    attrib.setLocation(-1);
                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }
            }

            VertexBuffer[] attribs = context.boundAttribs;
            if (!context.attribIndexList.moveToNew(loc)) {
                GLES20.glEnableVertexAttribArray(loc);
                RendererUtil.checkGLError();
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
                    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufId);
                    RendererUtil.checkGLError();

                    context.boundArrayVBO = bufId;
                }

                vb.getData().rewind();

                Android22Workaround.glVertexAttribPointer(loc,
                                    vb.getNumComponents(),
                                    convertVertexBufferFormat(vb.getFormat()),
                                    vb.isNormalized(),
                                    vb.getStride(),
                                    0);

                RendererUtil.checkGLError();

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
        GLES20.glDrawArrays(convertElementMode(mode), 0, vertCount);
        RendererUtil.checkGLError();
        /*
        }*/
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

        if (bufId == -1) {
            throw new RendererException("Invalid buffer ID");
        }

        if (context.boundElementArrayVBO != bufId) {
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufId);
            RendererUtil.checkGLError();

            context.boundElementArrayVBO = bufId;
        }

        int vertCount = mesh.getVertexCount();
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);

        Buffer indexData = indexBuf.getData();

        if (indexBuf.getFormat() == Format.UnsignedInt) {
            throw new RendererException("OpenGL ES does not support 32-bit index buffers." +
                                        "Split your models to avoid going over 65536 vertices.");
        }

        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt = convertVertexBufferFormat(indexBuf.getFormat());
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
                    GLES20.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());
                    RendererUtil.checkGLError();
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
                indexBuf.getData().limit(),
                convertVertexBufferFormat(indexBuf.getFormat()),
                0,
                count);
                 */
            } else {
                indexData.rewind();
                GLES20.glDrawElements(
                        convertElementMode(mesh.getMode()),
                        indexBuf.getData().limit(),
                        convertVertexBufferFormat(indexBuf.getFormat()),
                        0);
                RendererUtil.checkGLError();
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
        logger.log(Level.FINE, "updateVertexArray({0})", mesh);
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
     */
    private void renderMeshVertexArray(Mesh mesh, int lod, int count) {
         for (VertexBuffer vb : mesh.getBufferList().getArray()) {
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
            GLES20.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
            RendererUtil.checkGLError();
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
            GLES20.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
            RendererUtil.checkGLError();
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
        if (mesh.getVertexCount() == 0) {
            return;
        }

        /*
         * NOTE: not supported in OpenGL ES 2.0.
        if (context.pointSize != mesh.getPointSize()) {
            GLES10.glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        */
        if (context.lineWidth != mesh.getLineWidth()) {
            GLES20.glLineWidth(mesh.getLineWidth());
            RendererUtil.checkGLError();
            context.lineWidth = mesh.getLineWidth();
        }

        statistics.onMeshDrawn(mesh, lod);
//        if (GLContext.getCapabilities().GL_ARB_vertex_array_object){
//            renderMeshVertexArray(mesh, lod, count);
//        }else{

        if (useVBO) {
            renderMeshDefault(mesh, lod, count);
        } else {
            renderMeshVertexArray(mesh, lod, count);
        }
    }

    /**
     * drawTriangleList_Array uses Vertex Array
     * @param indexBuf
     * @param mesh
     * @param count
     */
    public void drawTriangleList_Array(VertexBuffer indexBuf, Mesh mesh, int count) {
        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);
        if (useInstancing) {
            throw new IllegalArgumentException("Caps.MeshInstancing is not supported.");
        }

        int vertCount = mesh.getVertexCount();
        Buffer indexData = indexBuf.getData();
        indexData.rewind();

        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt = convertVertexBufferFormat(indexBuf.getFormat());
            int elSize = indexBuf.getFormat().getComponentSize();
            int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++) {
                if (i == stripStart) {
                    elMode = convertElementMode(Mode.TriangleStrip);
                } else if (i == fanStart) {
                    elMode = convertElementMode(Mode.TriangleFan);
                }
                int elementLength = elementLengths[i];

                indexBuf.getData().position(curOffset);
                GLES20.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());
                RendererUtil.checkGLError();

                curOffset += elementLength * elSize;
            }
        } else {
            GLES20.glDrawElements(
                    convertElementMode(mesh.getMode()),
                    indexBuf.getData().limit(),
                    convertVertexBufferFormat(indexBuf.getFormat()),
                    indexBuf.getData());
            RendererUtil.checkGLError();
        }
    }

    /**
     * setVertexAttrib_Array uses Vertex Array
     * @param vb
     * @param idb
     */
    public void setVertexAttrib_Array(VertexBuffer vb, VertexBuffer idb) {
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
                return;
            } else if (loc == -2) {
                String attributeName = "in" + vb.getBufferType().name();

                loc = GLES20.glGetAttribLocation(programId, attributeName);
                RendererUtil.checkGLError();

                if (loc < 0) {
                    attrib.setLocation(-1);
                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }

            }  // if (loc == -2)

            if ((attribs[loc] != vb) || vb.isUpdateNeeded()) {
                // NOTE: Use data from interleaved buffer if specified
                VertexBuffer avb = idb != null ? idb : vb;
                avb.getData().rewind();
                avb.getData().position(vb.getOffset());

                // Upload attribute data
                GLES20.glVertexAttribPointer(loc,
                        vb.getNumComponents(),
                        convertVertexBufferFormat(vb.getFormat()),
                        vb.isNormalized(),
                        vb.getStride(),
                        avb.getData());

                RendererUtil.checkGLError();

                GLES20.glEnableVertexAttribArray(loc);
                RendererUtil.checkGLError();

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
            RendererUtil.checkGLError();
        } else {
            GLES20.glDisable(GLES20.GL_SAMPLE_ALPHA_TO_COVERAGE);
            RendererUtil.checkGLError();
        }
    }

    @Override
    public void invalidateState() {
        context.reset();
        boundShader = null;
        lastFb = null;
    }
}
