/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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
package com.jme3.renderer.opengl;

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
import com.jme3.util.ListMap;
import com.jme3.util.NativeObjectManager;
import java.nio.*;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3tools.shader.ShaderDebug;

public class GLRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(GLRenderer.class.getName());
    private static final boolean VALIDATE_SHADER = false;
    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final IntBuffer intBuf1 = BufferUtils.createIntBuffer(1);
    private final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private final FloatBuffer floatBuf16 = BufferUtils.createFloatBuffer(16);
    private final RenderContext context = new RenderContext();
    private final NativeObjectManager objManager = new NativeObjectManager();
    private final EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);

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
    private FrameBuffer mainFbOverride = null;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;
    private boolean linearizeSrgbImages;
    private HashSet<String> extensions;

    private final GL gl;
    private final GL2 gl2;
    private final GL3 gl3;
    private final GLExt glext;
    private final GLFbo glfbo;
    private final TextureUtil texUtil;
    
    public GLRenderer(GL gl, GLFbo glfbo) {
        this.gl = gl;
        this.gl2 = gl instanceof GL2 ? (GL2)gl : null;
        this.gl3 = gl instanceof GL3 ? (GL3)gl : null;
        this.glfbo = glfbo;
        this.glext = glfbo instanceof GLExt ? (GLExt)glfbo : null;
        this.texUtil = new TextureUtil(gl, gl2, glext);
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

    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public EnumSet<Caps> getCaps() {
        return caps;
    }

    private static HashSet<String> loadExtensions(String extensions) {
        HashSet<String> extensionSet = new HashSet<String>(64);
        for (String extension : extensions.split(" ")) {
            extensionSet.add(extension);
        }
        return extensionSet;
    }
    
    private static int extractVersion(String prefixStr, String versionStr) {
        if (versionStr != null) {
            int spaceIdx = versionStr.indexOf(" ", prefixStr.length());
            if (spaceIdx >= 1) {
                versionStr = versionStr.substring(prefixStr.length(), spaceIdx).trim();
            } else {
                versionStr = versionStr.substring(prefixStr.length()).trim();
            }
            
            // Find a character which is not a period or digit.
            for (int i = 0; i < versionStr.length(); i++) {
                char c = versionStr.charAt(i);
                if (c != '.' && (c < '0' || c > '9')) {
                    versionStr = versionStr.substring(0, i);
                    break;
                }
            }
            
            // Pivot on first point.
            int firstPoint = versionStr.indexOf(".");
            
            // Remove everything after second point.
            int secondPoint = versionStr.indexOf(".", firstPoint + 1);
            
            if (secondPoint != -1) {
                versionStr = versionStr.substring(0, secondPoint);
            }
            
            String majorVerStr = versionStr.substring(0, firstPoint);
            String minorVerStr = versionStr.substring(firstPoint + 1);
            
            if (minorVerStr.endsWith("0") && minorVerStr.length() > 1) {
                minorVerStr = minorVerStr.substring(0, minorVerStr.length() - 1);
            }
            
            int majorVer = Integer.parseInt(majorVerStr);
            int minorVer = Integer.parseInt(minorVerStr);
            
            return majorVer * 100 + minorVer * 10;
        } else {
            return -1;
        }
    }
    
    private boolean hasExtension(String extensionName) {
        return extensions.contains(extensionName);
    }
    
    private void loadCapabilitiesES() {
        caps.add(Caps.GLSL100);
        caps.add(Caps.OpenGLES20);
        
        // Important: Do not add OpenGL20 - that's the desktop capability!
    }
    
    private void loadCapabilitiesGL2() {
        int oglVer = extractVersion("", gl.glGetString(GL.GL_VERSION));
        
        if (oglVer >= 200) {
            caps.add(Caps.OpenGL20);
            if (oglVer >= 210) {
                caps.add(Caps.OpenGL21);
                if (oglVer >= 300) {
                    caps.add(Caps.OpenGL30);
                    if (oglVer >= 310) {
                        caps.add(Caps.OpenGL31);
                        if (oglVer >= 320) {
                            caps.add(Caps.OpenGL32);
                        }
                    }
                }
            }
        }
        
        int glslVer = extractVersion("", gl.glGetString(GL.GL_SHADING_LANGUAGE_VERSION));
        
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
        
        // Workaround, always assume we support GLSL100 & GLSL110
        // Supporting OpenGL 2.0 means supporting GLSL 1.10.
        caps.add(Caps.GLSL110);
        caps.add(Caps.GLSL100);
        
        // Fix issue in TestRenderToMemory when GL.GL_FRONT is the main
        // buffer being used.
        context.initialDrawBuf = getInteger(GL2.GL_DRAW_BUFFER);
        context.initialReadBuf = getInteger(GL2.GL_READ_BUFFER);
        
        // XXX: This has to be GL.GL_BACK for canvas on Mac
        // Since initialDrawBuf is GL.GL_FRONT for pbuffer, gotta
        // change this value later on ...
//        initialDrawBuf = GL.GL_BACK;
//        initialReadBuf = GL.GL_BACK;
    }
    
    private void loadCapabilitiesCommon() {
        extensions = loadExtensions(gl.glGetString(GL.GL_EXTENSIONS));
        
        vertexTextureUnits = getInteger(GL.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS);
        logger.log(Level.FINER, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        fragTextureUnits = getInteger(GL.GL_MAX_TEXTURE_IMAGE_UNITS);
        logger.log(Level.FINER, "Texture Units: {0}", fragTextureUnits);

//        gl.glGetInteger(GL.GL_MAX_VERTEX_UNIFORM_COMPONENTS, intBuf16);
//        vertexUniforms = intBuf16.get(0);
//        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);
//
//        gl.glGetInteger(GL.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, intBuf16);
//        fragUniforms = intBuf16.get(0);
//        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);

        vertexAttribs = getInteger(GL.GL_MAX_VERTEX_ATTRIBS);
        logger.log(Level.FINER, "Vertex Attributes: {0}", vertexAttribs);

//        gl.glGetInteger(GL.GL_MAX_ELEMENTS_VERTICES, intBuf16);
//        maxVertCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);
//
//        gl.glGetInteger(GL.GL_MAX_ELEMENTS_INDICES, intBuf16);
//        maxTriCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);

        maxTexSize = getInteger(GL.GL_MAX_TEXTURE_SIZE);
        logger.log(Level.FINER, "Maximum Texture Resolution: {0}", maxTexSize);

        maxCubeTexSize = getInteger(GL.GL_MAX_CUBE_MAP_TEXTURE_SIZE);
        logger.log(Level.FINER, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);

        if (hasExtension("GL_ARB_draw_instanced") && 
            hasExtension("GL_ARB_instanced_arrays")) {
            caps.add(Caps.MeshInstancing);
        }

        if (hasExtension("GL_OES_element_index_uint") || gl2 != null) {
            caps.add(Caps.IntegerIndexBuffer);
        }
        
        if (hasExtension("GL_ARB_texture_buffer_object")) {
            caps.add(Caps.TextureBuffer);
        }
        
        // == texture format extensions ==
        
        boolean hasFloatTexture = false;

        hasFloatTexture = hasExtension("GL_OES_texture_half_float") && 
                          hasExtension("GL_OES_texture_float");
        
        if (!hasFloatTexture) {
            hasFloatTexture = hasExtension("GL_ARB_texture_float") &&
                              hasExtension("GL_ARB_half_float_pixel");
            
            if (!hasFloatTexture) {
                hasFloatTexture = caps.contains(Caps.OpenGL30);
            }
        }
        
        if (hasFloatTexture) {
            caps.add(Caps.FloatTexture);
        }
        
        if (hasExtension("GL_OES_depth_texture") || gl2 != null) {
            caps.add(Caps.DepthTexture);
            
            // TODO: GL_OES_depth24
        }
        
        if (hasExtension("GL_OES_rgb8_rgba8") || 
            hasExtension("GL_ARM_rgba8") || 
            hasExtension("GL_EXT_texture_format_BGRA8888")) {
            caps.add(Caps.Rgba8);
        }
        
        if (caps.contains(Caps.OpenGL30) || hasExtension("GL_OES_packed_depth_stencil")) {
            caps.add(Caps.PackedDepthStencilBuffer);
        }

        if (hasExtension("GL_ARB_color_buffer_float") &&
            hasExtension("GL_ARB_half_float_pixel")) {
            // XXX: Require both 16 and 32 bit float support for FloatColorBuffer.
            caps.add(Caps.FloatColorBuffer);
        }

        if (hasExtension("GL_ARB_depth_buffer_float")) {
            caps.add(Caps.FloatDepthBuffer);
        }

        if ((hasExtension("GL_EXT_packed_float") && hasFloatTexture) || 
             caps.contains(Caps.OpenGL30)) {
            // Either OpenGL3 is available or both packed_float & half_float_pixel.
            caps.add(Caps.PackedFloatColorBuffer);
            caps.add(Caps.PackedFloatTexture);
        }
        
        if (hasExtension("GL_EXT_texture_shared_exponent") || caps.contains(Caps.OpenGL30)) {
            caps.add(Caps.SharedExponentTexture);
        }
        
        if (hasExtension("GL_EXT_texture_compression_s3tc")) {
            caps.add(Caps.TextureCompressionS3TC);
        }
        
        if (hasExtension("GL_ARB_ES3_compatibility")) {
            caps.add(Caps.TextureCompressionETC2);
            caps.add(Caps.TextureCompressionETC1);
        } else if (hasExtension("GL_OES_compressed_ETC1_RGB8_texture")) {
            caps.add(Caps.TextureCompressionETC1);
        }
        
        // == end texture format extensions ==
        
        if (hasExtension("GL_ARB_vertex_array_object") || caps.contains(Caps.OpenGL30)) {
            caps.add(Caps.VertexBufferArray);
        }

        if (hasExtension("GL_ARB_texture_non_power_of_two") || 
            hasExtension("GL_OES_texture_npot") ||
            caps.contains(Caps.OpenGL30)) {
            caps.add(Caps.NonPowerOfTwoTextures);
        } else {
            logger.log(Level.WARNING, "Your graphics card does not "
                                    + "support non-power-of-2 textures. "
                                    + "Some features might not work.");
        }

        if (hasExtension("GL_EXT_texture_array") || caps.contains(Caps.OpenGL30)) {
            caps.add(Caps.TextureArray);
        }

        if (hasExtension("GL_EXT_texture_filter_anisotropic")) {
            caps.add(Caps.TextureFilterAnisotropic);
        }

        if (hasExtension("GL_EXT_framebuffer_object")) {
            caps.add(Caps.FrameBuffer);

            maxRBSize = getInteger(GLExt.GL_MAX_RENDERBUFFER_SIZE_EXT);
            logger.log(Level.FINER, "FBO RB Max Size: {0}", maxRBSize);

            maxFBOAttachs = getInteger(GLExt.GL_MAX_COLOR_ATTACHMENTS_EXT);
            logger.log(Level.FINER, "FBO Max renderbuffers: {0}", maxFBOAttachs);

            if (hasExtension("GL_EXT_framebuffer_blit")) {
                caps.add(Caps.FrameBufferBlit);
            }
            
            if (hasExtension("GL_EXT_framebuffer_multisample")) {
                caps.add(Caps.FrameBufferMultisample);

                maxFBOSamples = getInteger(GLExt.GL_MAX_SAMPLES_EXT);
                logger.log(Level.FINER, "FBO Max Samples: {0}", maxFBOSamples);
            }

            if (hasExtension("GL_ARB_texture_multisample")) {
                caps.add(Caps.TextureMultisample);

                maxColorTexSamples = getInteger(GLExt.GL_MAX_COLOR_TEXTURE_SAMPLES);
                logger.log(Level.FINER, "Texture Multisample Color Samples: {0}", maxColorTexSamples);

                maxDepthTexSamples = getInteger(GLExt.GL_MAX_DEPTH_TEXTURE_SAMPLES);
                logger.log(Level.FINER, "Texture Multisample Depth Samples: {0}", maxDepthTexSamples);
            }

            if (hasExtension("GL_ARB_draw_buffers")) {
                maxMRTFBOAttachs = getInteger(GLExt.GL_MAX_DRAW_BUFFERS_ARB);
                if (maxMRTFBOAttachs > 1) {
                    caps.add(Caps.FrameBufferMRT);
                    logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
                }
            } else {
                maxMRTFBOAttachs = 1;
            }
        }

        if (hasExtension("GL_ARB_multisample")) {
            boolean available = getInteger(GLExt.GL_SAMPLE_BUFFERS_ARB) != 0;
            int samples = getInteger(GLExt.GL_SAMPLES_ARB);
            logger.log(Level.FINER, "Samples: {0}", samples);
            boolean enabled = gl.glIsEnabled(GLExt.GL_MULTISAMPLE_ARB);
            if (samples > 0 && available && !enabled) {
                gl.glEnable(GLExt.GL_MULTISAMPLE_ARB);
            }
            caps.add(Caps.Multisample);
        }
        
        // Supports sRGB pipeline.
        if ( (hasExtension("GL_ARB_framebuffer_sRGB") && hasExtension("GL_EXT_texture_sRGB")) 
           || caps.contains(Caps.OpenGL30) ) {
            caps.add(Caps.Srgb);
        }

        logger.log(Level.FINE, "Caps: {0}", caps);
        
        texUtil.initialize(caps);
    }
    
    private void loadCapabilities() {
        if (gl2 != null) {
            loadCapabilitiesGL2();
        } else {
            loadCapabilitiesES();
        }
        loadCapabilitiesCommon();
    }
    
    private int getInteger(int en) {
        intBuf16.clear();
        gl.glGetInteger(en, intBuf16);
        return intBuf16.get(0);
    }
    
    private boolean getBoolean(int en) {
        gl.glGetBoolean(en, nameBuf);
        return nameBuf.get(0) != (byte)0;
    }
    
    @SuppressWarnings("fallthrough")
    public void initialize() {
        loadCapabilities();
    }

    public void invalidateState() {
        context.reset();
        if (gl2 != null) {
            context.initialDrawBuf = getInteger(GL2.GL_DRAW_BUFFER);
            context.initialReadBuf = getInteger(GL2.GL_READ_BUFFER);
        }
    }

    public void resetGLObjects() {
        logger.log(Level.FINE, "Reseting objects and invalidating state");
        objManager.resetObjects();
        statistics.clearMemory();
        invalidateState();
    }

    public void cleanup() {
        logger.log(Level.FINE, "Deleting objects and invalidating state");
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
        gl.glDepthRange(start, end);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            //See explanations of the depth below, we must enable color write to be able to clear the color buffer
            if (context.colorWriteEnabled == false) {
                gl.glColorMask(true, true, true, true);
                context.colorWriteEnabled = true;
            }
            bits = GL.GL_COLOR_BUFFER_BIT;
        }
        if (depth) {

            //glClear(GL.GL_DEPTH_BUFFER_BIT) seems to not work when glDepthMask is false
            //here s some link on openl board
            //http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=257223
            //if depth clear is requested, we enable the depthMask
            if (context.depthWriteEnabled == false) {
                gl.glDepthMask(true);
                context.depthWriteEnabled = true;
            }
            bits |= GL.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            bits |= GL.GL_STENCIL_BUFFER_BIT;
        }
        if (bits != 0) {
            gl.glClear(bits);
        }
    }

    public void setBackgroundColor(ColorRGBA color) {
        gl.glClearColor(color.r, color.g, color.b, color.a);
    }

    public void setAlphaToCoverage(boolean value) {
        if (caps.contains(Caps.Multisample)) {
            if (value) {
                gl.glEnable(GLExt.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
            } else {
                gl.glDisable(GLExt.GL_SAMPLE_ALPHA_TO_COVERAGE_ARB);
            }
        }
    }

    public void applyRenderState(RenderState state) {
        if (gl2 != null) {
            if (state.isWireframe() && !context.wireframe) {
                gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
                context.wireframe = true;
            } else if (!state.isWireframe() && context.wireframe) {
                gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
                context.wireframe = false;
            }
        }

        if (state.isDepthTest() && !context.depthTestEnabled) {
            gl.glEnable(GL.GL_DEPTH_TEST);      
            gl.glDepthFunc(convertTestFunction(context.depthFunc));
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            gl.glDisable(GL.GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }
        if (state.getDepthFunc() != context.depthFunc) {
            gl.glDepthFunc(convertTestFunction(state.getDepthFunc()));
            context.depthFunc = state.getDepthFunc();
        }

        if (gl2 != null) {
            if (state.isAlphaTest() && !context.alphaTestEnabled) {
                gl2.glEnable(GL2.GL_ALPHA_TEST);
                gl2.glAlphaFunc(convertTestFunction(context.alphaFunc), context.alphaTestFallOff);
                context.alphaTestEnabled = true;
            } else if (!state.isAlphaTest() && context.alphaTestEnabled) {
                gl2.glDisable(GL2.GL_ALPHA_TEST);
                context.alphaTestEnabled = false;
            }
            if (state.getAlphaFallOff() != context.alphaTestFallOff) {
                gl2.glAlphaFunc(convertTestFunction(context.alphaFunc), context.alphaTestFallOff);   
                context.alphaTestFallOff = state.getAlphaFallOff();
            }         
            if (state.getAlphaFunc() != context.alphaFunc) {
                gl2.glAlphaFunc(convertTestFunction(state.getAlphaFunc()), context.alphaTestFallOff);
                context.alphaFunc = state.getAlphaFunc();
            }
        }

        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            gl.glDepthMask(true);
            context.depthWriteEnabled = true;
        } else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            gl.glDepthMask(false);
            context.depthWriteEnabled = false;
        }

        if (state.isColorWrite() && !context.colorWriteEnabled) {
            gl.glColorMask(true, true, true, true);
            context.colorWriteEnabled = true;
        } else if (!state.isColorWrite() && context.colorWriteEnabled) {
            gl.glColorMask(false, false, false, false);
            context.colorWriteEnabled = false;
        }

        if (state.isPointSprite() && !context.pointSprite) {
            // Only enable/disable sprite
            if (context.boundTextures[0] != null) {
                if (context.boundTextureUnit != 0) {
                    gl.glActiveTexture(GL.GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                if (gl2 != null) {
                    gl2.glEnable(GL2.GL_POINT_SPRITE);
                    gl2.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
                }
            }
            context.pointSprite = true;
        } else if (!state.isPointSprite() && context.pointSprite) {
            if (context.boundTextures[0] != null) {
                if (context.boundTextureUnit != 0) {
                    gl.glActiveTexture(GL.GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                if (gl2 != null) {
                    gl2.glDisable(GL2.GL_POINT_SPRITE);
                    gl2.glDisable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
                }
                context.pointSprite = false;
            }
        }

        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(state.getPolyOffsetFactor(),
                        state.getPolyOffsetUnits());
                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            } else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    gl.glPolygonOffset(state.getPolyOffsetFactor(),
                            state.getPolyOffsetUnits());
                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        } else {
            if (context.polyOffsetEnabled) {
                gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }

        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                gl.glDisable(GL.GL_CULL_FACE);
            } else {
                gl.glEnable(GL.GL_CULL_FACE);
            }

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    gl.glCullFace(GL.GL_BACK);
                    break;
                case Front:
                    gl.glCullFace(GL.GL_FRONT);
                    break;
                case FrontAndBack:
                    gl.glCullFace(GL.GL_FRONT_AND_BACK);
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                gl.glDisable(GL.GL_BLEND);
            } else {
                gl.glEnable(GL.GL_BLEND);
                switch (state.getBlendMode()) {
                    case Off:
                        break;
                    case Additive:
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
                        break;
                    case AlphaAdditive:
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
                        break;
                    case Color:
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_COLOR);
                        break;
                    case Alpha:
                        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case PremultAlpha:
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case Modulate:
                        gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_ZERO);
                        break;
                    case ModulateX2:
                        gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_SRC_COLOR);
                        break;
                    case Screen:
                        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_COLOR);
                        break;       
                    case Exclusion:
                        gl.glBlendFunc(GL.GL_ONE_MINUS_DST_COLOR, GL.GL_ONE_MINUS_SRC_COLOR);
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
                gl.glEnable(GL.GL_STENCIL_TEST);
                gl.glStencilOpSeparate(GL.GL_FRONT,
                        convertStencilOperation(state.getFrontStencilStencilFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthPassOperation()));
                gl.glStencilOpSeparate(GL.GL_BACK,
                        convertStencilOperation(state.getBackStencilStencilFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthPassOperation()));
                gl.glStencilFuncSeparate(GL.GL_FRONT,
                        convertTestFunction(state.getFrontStencilFunction()),
                        0, Integer.MAX_VALUE);
                gl.glStencilFuncSeparate(GL.GL_BACK,
                        convertTestFunction(state.getBackStencilFunction()),
                        0, Integer.MAX_VALUE);
            } else {
                gl.glDisable(GL.GL_STENCIL_TEST);
            }
        }
    }

    private int convertStencilOperation(StencilOperation stencilOp) {
        switch (stencilOp) {
            case Keep:
                return GL.GL_KEEP;
            case Zero:
                return GL.GL_ZERO;
            case Replace:
                return GL.GL_REPLACE;
            case Increment:
                return GL.GL_INCR;
            case IncrementWrap:
                return GL.GL_INCR_WRAP;
            case Decrement:
                return GL.GL_DECR;
            case DecrementWrap:
                return GL.GL_DECR_WRAP;
            case Invert:
                return GL.GL_INVERT;
            default:
                throw new UnsupportedOperationException("Unrecognized stencil operation: " + stencilOp);
        }
    }

    private int convertTestFunction(TestFunction testFunc) {
        switch (testFunc) {
            case Never:
                return GL.GL_NEVER;
            case Less:
                return GL.GL_LESS;
            case LessOrEqual:
                return GL.GL_LEQUAL;
            case Greater:
                return GL.GL_GREATER;
            case GreaterOrEqual:
                return GL.GL_GEQUAL;
            case Equal:
                return GL.GL_EQUAL;
            case NotEqual:
                return GL.GL_NOTEQUAL;
            case Always:
                return GL.GL_ALWAYS;
            default:
                throw new UnsupportedOperationException("Unrecognized test function: " + testFunc);
        }
    }

    /*********************************************************************\
    |* Camera and World transforms                                       *|
    \*********************************************************************/
    public void setViewPort(int x, int y, int w, int h) {
        if (x != vpX || vpY != y || vpW != w || vpH != h) {
            gl.glViewport(x, y, w, h);
            vpX = x;
            vpY = y;
            vpW = w;
            vpH = h;
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            gl.glEnable(GL.GL_SCISSOR_TEST);
            context.clipRectEnabled = true;
        }
        if (clipX != x || clipY != y || clipW != width || clipH != height) {
            gl.glScissor(x, y, width, height);
            clipX = x;
            clipY = y;
            clipW = width;
            clipH = height;
        }
    }

    public void clearClipRect() {
        if (context.clipRectEnabled) {
            gl.glDisable(GL.GL_SCISSOR_TEST);
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
        int loc = gl.glGetUniformLocation(shader.getId(), stringBuf.toString());
        if (loc < 0) {
            uniform.setLocation(-1);
            // uniform is not declared in shader
            logger.log(Level.FINE, "Uniform {0} is not declared in shader {1}.", new Object[]{uniform.getName(), shader.getSources()});
        } else {
            uniform.setLocation(loc);
        }
    }

    protected void bindProgram(Shader shader) {
        int shaderId = shader.getId();
        if (context.boundShaderProgram != shaderId) {
            gl.glUseProgram(shaderId);
            statistics.onShaderUse(shader, true);
            context.boundShader = shader;
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
        IntBuffer ib;
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                gl.glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                gl.glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                gl.glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    gl.glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else if (val instanceof Vector4f) {
                    Vector4f c = (Vector4f) val;
                    gl.glUniform4f(loc, c.x, c.y, c.z, c.w);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    gl.glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                gl.glUniform1i(loc, b.booleanValue() ? GL.GL_TRUE : GL.GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                gl.glUniformMatrix3(loc, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                gl.glUniformMatrix4(loc, false, fb);
                break;
            case IntArray:
                ib = (IntBuffer) uniform.getValue();
                gl.glUniform1(loc, ib);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                gl.glUniform1(loc, fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.glUniform2(loc, fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.glUniform3(loc, fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.glUniform4(loc, fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.glUniformMatrix4(loc, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                gl.glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + uniform.getVarType());
        }
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
                return GL.GL_FRAGMENT_SHADER;
            case Vertex:
                return GL.GL_VERTEX_SHADER;
            default:
                throw new UnsupportedOperationException("Unrecognized shader type.");
        }
    }

    public void updateShaderSourceData(ShaderSource source) {
        int id = source.getId();
        if (id == -1) {
            // Create id
            id = gl.glCreateShader(convertShaderType(source.getType()));
            if (id <= 0) {
                throw new RendererException("Invalid ID received when trying to create shader.");
            }

            source.setId(id);
        } else {
            throw new RendererException("Cannot recompile shader source");
        }

        // Upload shader source.
        // Merge the defines and source code.
        String language = source.getLanguage();
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
            } else {
                // version 100 does not exist in desktop GLSL.
                // put version 110 in that case to enable strict checking
                stringBuf.append("#version 110\n");
            }
        }
        stringBuf.append(source.getDefines());
        stringBuf.append(source.getSource());
        
        intBuf1.clear();
        intBuf1.put(0, stringBuf.length());
        gl.glShaderSource(id, new String[]{ stringBuf.toString() }, intBuf1);
        gl.glCompileShader(id);

        gl.glGetShader(id, GL.GL_COMPILE_STATUS, intBuf1);

        boolean compiledOK = intBuf1.get(0) == GL.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            gl.glGetShader(id, GL.GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                gl.glGetShaderInfoLog(id, null, logBuf);
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                // convert to string, etc
                infoLog = new String(logBytes);
            }
        }

        if (compiledOK) {
            if (infoLog != null) {
                logger.log(Level.WARNING, "{0} compiled successfully, compiler warnings: \n{1}",
                        new Object[]{source.getName(), infoLog});
            } else {
                logger.log(Level.FINE, "{0} compiled successfully.", source.getName());
            }
            source.clearUpdateNeeded();
        } else {
            logger.log(Level.WARNING, "Bad compile of:\n{0}",
                    new Object[]{ShaderDebug.formatShaderSource(source.getDefines(), source.getSource(), stringBuf.toString())});
            if (infoLog != null) {
                throw new RendererException("compile error in: " + source + "\n" + infoLog);
            } else {
                throw new RendererException("compile error in: " + source + "\nerror: <not provided>");
            }
        }
    }

    public void updateShaderData(Shader shader) {
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program
            id = gl.glCreateProgram();
            if (id == 0) {
                throw new RendererException("Invalid ID (" + id + ") received when trying to create shader program.");
            }

            shader.setId(id);
            needRegister = true;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.isUpdateNeeded()) {
                updateShaderSourceData(source);
            }
            gl.glAttachShader(id, source.getId());
        }

        if (caps.contains(Caps.OpenGL30) && gl3 != null) {
            // Check if GLSL version is 1.5 for shader
            gl3.glBindFragDataLocation(id, 0, "outFragColor");
            // For MRT
            for (int i = 0; i < maxMRTFBOAttachs; i++) {
                gl3.glBindFragDataLocation(id, i, "outFragData[" + i + "]");
            }
        }

        // Link shaders to program
        gl.glLinkProgram(id);

        // Check link status
        gl.glGetProgram(id, GL.GL_LINK_STATUS, intBuf1);
        boolean linkOK = intBuf1.get(0) == GL.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            gl.glGetProgram(id, GL.GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                gl.glGetProgramInfoLog(id, null, logBuf);

                // convert to string, etc
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                infoLog = new String(logBytes);
            }
        }

        if (linkOK) {
            if (infoLog != null) {
                logger.log(Level.WARNING, "Shader linked successfully. Linker warnings: \n{0}", infoLog);
            } else {
                logger.fine("Shader linked successfully.");
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
                throw new RendererException("Shader failed to link, shader:" + shader + "\n" + infoLog);
            } else {
                throw new RendererException("Shader failed to link, shader:" + shader + "\ninfo: <not provided>");
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
        gl.glDeleteShader(source.getId());
        source.resetObject();
    }

    public void deleteShader(Shader shader) {
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {
                gl.glDetachShader(shader.getId(), source.getId());
                deleteShaderSource(source);
            }
        }

        gl.glDeleteProgram(shader.getId());
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
        if (caps.contains(Caps.FrameBufferBlit)) {
            int srcX0 = 0;
            int srcY0 = 0;
            int srcX1;
            int srcY1;

            int dstX0 = 0;
            int dstY0 = 0;
            int dstX1;
            int dstY1;

            int prevFBO = context.boundFBO;

            if (mainFbOverride != null) {
                if (src == null) {
                    src = mainFbOverride;
                }
                if (dst == null) {
                    dst = mainFbOverride;
                }
            }

            if (src != null && src.isUpdateNeeded()) {
                updateFrameBuffer(src);
            }

            if (dst != null && dst.isUpdateNeeded()) {
                updateFrameBuffer(dst);
            }

            if (src == null) {
                glfbo.glBindFramebufferEXT(GLExt.GL_READ_FRAMEBUFFER_EXT, 0);
                srcX0 = vpX;
                srcY0 = vpY;
                srcX1 = vpX + vpW;
                srcY1 = vpY + vpH;
            } else {
                glfbo.glBindFramebufferEXT(GLExt.GL_READ_FRAMEBUFFER_EXT, src.getId());
                srcX1 = src.getWidth();
                srcY1 = src.getHeight();
            }
            if (dst == null) {
                glfbo.glBindFramebufferEXT(GLExt.GL_DRAW_FRAMEBUFFER_EXT, 0);
                dstX0 = vpX;
                dstY0 = vpY;
                dstX1 = vpX + vpW;
                dstY1 = vpY + vpH;
            } else {
                glfbo.glBindFramebufferEXT(GLExt.GL_DRAW_FRAMEBUFFER_EXT, dst.getId());
                dstX1 = dst.getWidth();
                dstY1 = dst.getHeight();
            }
            int mask = GL.GL_COLOR_BUFFER_BIT;
            if (copyDepth) {
                mask |= GL.GL_DEPTH_BUFFER_BIT;
            }
            glext.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1,
                    dstX0, dstY0, dstX1, dstY1, mask,
                    GL.GL_NEAREST);


            glfbo.glBindFramebufferEXT(GLExt.GL_FRAMEBUFFER_EXT, prevFBO);
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

    private void checkFrameBufferError() {
        int status = glfbo.glCheckFramebufferStatusEXT(GLFbo.GL_FRAMEBUFFER_EXT);
        switch (status) {
            case GLFbo.GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case GLFbo.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                //Choose different formats
                throw new IllegalStateException("Framebuffer object format is "
                        + "unsupported by the video hardware.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new IllegalStateException("Framebuffer has erronous attachment.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new IllegalStateException("Framebuffer doesn't have any renderbuffers attached.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new IllegalStateException("Framebuffer attachments must have same dimensions.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new IllegalStateException("Framebuffer attachments must have same formats.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new IllegalStateException("Incomplete draw buffer.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new IllegalStateException("Incomplete read buffer.");
            case GLFbo.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT:
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
            glfbo.glGenRenderbuffersEXT(intBuf1);
            id = intBuf1.get(0);
            rb.setId(id);
        }

        if (context.boundRB != id) {
            glfbo.glBindRenderbufferEXT(GLExt.GL_RENDERBUFFER_EXT, id);
            context.boundRB = id;
        }

        if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize) {
            throw new RendererException("Resolution " + fb.getWidth()
                    + ":" + fb.getHeight() + " is not supported.");
        }

        GLImageFormat glFmt = texUtil.getImageFormatWithError(rb.getFormat(), fb.isSrgb());

        if (fb.getSamples() > 1 && caps.contains(Caps.FrameBufferMultisample)) {
            int samples = fb.getSamples();
            if (maxFBOSamples < samples) {
                samples = maxFBOSamples;
            }
            glext.glRenderbufferStorageMultisampleEXT(GLExt.GL_RENDERBUFFER_EXT,
                    samples,
                    glFmt.internalFormat,
                    fb.getWidth(),
                    fb.getHeight());
        } else {
            glfbo.glRenderbufferStorageEXT(GLExt.GL_RENDERBUFFER_EXT,
                    glFmt.internalFormat,
                    fb.getWidth(),
                    fb.getHeight());
        }
    }

    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == FrameBuffer.SLOT_DEPTH) {
            return GLExt.GL_DEPTH_ATTACHMENT_EXT;
        } else if (attachmentSlot == FrameBuffer.SLOT_DEPTH_STENCIL) {
            // NOTE: Using depth stencil format requires GL3, this is already
            // checked via render caps.
            return GL3.GL_DEPTH_STENCIL_ATTACHMENT;
        } else if (attachmentSlot < 0 || attachmentSlot >= 16) {
            throw new UnsupportedOperationException("Invalid FBO attachment slot: " + attachmentSlot);
        }
        
        return GLExt.GL_COLOR_ATTACHMENT0_EXT + attachmentSlot;
    }

    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        Texture tex = rb.getTexture();
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), 0);

            // NOTE: For depth textures, sets nearest/no-mips mode
            // Required to fix "framebuffer unsupported"
            // for old NVIDIA drivers!
            setupTextureParams(tex);
        }

        glfbo.glFramebufferTexture2DEXT(GLExt.GL_FRAMEBUFFER_EXT,
                convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType(), image.getMultiSamples(), rb.getFace()),
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
            glfbo.glFramebufferRenderbufferEXT(GLExt.GL_FRAMEBUFFER_EXT,
                    convertAttachmentSlot(rb.getSlot()),
                    GLExt.GL_RENDERBUFFER_EXT,
                    rb.getId());
        }
    }

    public void updateFrameBuffer(FrameBuffer fb) {
        int id = fb.getId();
        if (id == -1) {
            // create FBO
            glfbo.glGenFramebuffersEXT(intBuf1);
            id = intBuf1.get(0);
            fb.setId(id);
            objManager.registerObject(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            glfbo.glBindFramebufferEXT(GLExt.GL_FRAMEBUFFER_EXT, id);
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
        if (!caps.contains(Caps.TextureMultisample)) {
            throw new RendererException("Multisampled textures are not supported");
        }

        setFrameBuffer(fb);

        Vector2f[] samplePositions = new Vector2f[fb.getSamples()];
        FloatBuffer samplePos = BufferUtils.createFloatBuffer(2);
        for (int i = 0; i < samplePositions.length; i++) {
            glext.glGetMultisample(GLExt.GL_SAMPLE_POSITION, i, samplePos);
            samplePos.clear();
            samplePositions[i] = new Vector2f(samplePos.get(0) - 0.5f,
                    samplePos.get(1) - 0.5f);
        }
        return samplePositions;
    }

    public void setMainFrameBufferOverride(FrameBuffer fb) {
        mainFbOverride = fb;
    }

    public void setFrameBuffer(FrameBuffer fb) {
        if (fb == null && mainFbOverride != null) {
            fb = mainFbOverride;
        }

        if (context.boundFB == fb) {
            if (fb == null || !fb.isUpdateNeeded()) {
                return;
            }
        }

        if (!caps.contains(Caps.FrameBuffer)) {
            throw new RendererException("Framebuffer objects are not supported" +
                                        " by the video hardware");
        }
        
        // generate mipmaps for last FB if needed
        if (context.boundFB != null) {
            for (int i = 0; i < context.boundFB.getNumColorBuffers(); i++) {
                RenderBuffer rb = context.boundFB.getColorBuffer(i);
                Texture tex = rb.getTexture();
                if (tex != null
                        && tex.getMinFilter().usesMipMapLevels()) {
                    setTexture(0, rb.getTexture());

                    int textureType = convertTextureType(tex.getType(), tex.getImage().getMultiSamples(), rb.getFace());
                    if (gl2 != null) gl2.glEnable(textureType);
                    glfbo.glGenerateMipmapEXT(textureType);
                    if (gl2 != null) gl2.glDisable(textureType);
                }
            }
        }

    if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                glfbo.glBindFramebufferEXT(GLExt.GL_FRAMEBUFFER_EXT, 0);
                statistics.onFrameBufferUse(null, true);

                context.boundFBO = 0;
            }
            // select back buffer
            if (gl2 != null) {
                if (context.boundDrawBuf != -1) {
                    gl2.glDrawBuffer(context.initialDrawBuf);
                    context.boundDrawBuf = -1;
                }
                if (context.boundReadBuf != -1) {
                    gl2.glReadBuffer(context.initialReadBuf);
                    context.boundReadBuf = -1;
                }
            }

            context.boundFB = null;
        } else {
            if (fb.getNumColorBuffers() == 0 && fb.getDepthBuffer() == null) {
                throw new IllegalArgumentException("The framebuffer: " + fb
                        + "\nDoesn't have any color/depth buffers");
            }

            if (fb.isUpdateNeeded()) {
                updateFrameBuffer(fb);
            }

            if (context.boundFBO != fb.getId()) {
                glfbo.glBindFramebufferEXT(GLExt.GL_FRAMEBUFFER_EXT, fb.getId());
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
                if (gl2 != null) {
                    if (context.boundDrawBuf != -2) {
                        gl2.glDrawBuffer(GL.GL_NONE);
                        context.boundDrawBuf = -2;
                    }
                    if (context.boundReadBuf != -2) {
                        gl2.glReadBuffer(GL.GL_NONE);
                        context.boundReadBuf = -2;
                    }
                    }
            } else {
                if (fb.getNumColorBuffers() > maxFBOAttachs) {
                    throw new RendererException("Framebuffer has more color "
                            + "attachments than are supported"
                            + " by the video hardware!");
                }
                if (fb.isMultiTarget()) {
                    if (!caps.contains(Caps.FrameBufferMRT)) {
                        throw new RendererException("Multiple render targets " +
                                " are not supported by the video hardware");
                    }
                    if (fb.getNumColorBuffers() > maxMRTFBOAttachs) {
                        throw new RendererException("Framebuffer has more"
                                + " multi targets than are supported"
                                + " by the video hardware!");
                    }

                    if (context.boundDrawBuf != 100 + fb.getNumColorBuffers()) {
                        intBuf16.clear();
                        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
                            intBuf16.put(GLExt.GL_COLOR_ATTACHMENT0_EXT + i);
                        }

                        intBuf16.flip();
                        glext.glDrawBuffers(intBuf16);
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                } else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (gl2 != null) {
                        if (context.boundDrawBuf != rb.getSlot()) {
                            gl2.glDrawBuffer(GLExt.GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
                            context.boundDrawBuf = rb.getSlot();
                        }
                    }
                }
            }

            assert fb.getId() >= 0;
            assert context.boundFBO == fb.getId();

            context.boundFB = fb;

            checkFrameBufferError();
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
            if (gl2 != null) {
                if (context.boundReadBuf != rb.getSlot()) {
                    gl2.glReadBuffer(GLExt.GL_COLOR_ATTACHMENT0_EXT + rb.getSlot());
                    context.boundReadBuf = rb.getSlot();
                }
            }
        } else {
            setFrameBuffer(null);
        }

        gl.glReadPixels(vpX, vpY, vpW, vpH, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, byteBuf);
    }

    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1.put(0, rb.getId());
        glfbo.glDeleteRenderbuffersEXT(intBuf1);
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
        if (fb.getId() != -1) {
            if (context.boundFBO == fb.getId()) {
                glfbo.glBindFramebufferEXT(GLExt.GL_FRAMEBUFFER_EXT, 0);
                context.boundFBO = 0;
            }

            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1.put(0, fb.getId());
            glfbo.glDeleteFramebuffersEXT(intBuf1);
            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

    /*********************************************************************\
    |* Textures                                                          *|
    \*********************************************************************/
    private int convertTextureType(Texture.Type type, int samples, int face) {
        if (samples > 1 && !caps.contains(Caps.TextureMultisample)) {
            throw new RendererException("Multisample textures are not supported" + 
                                        " by the video hardware.");
        }
        if (type == Texture.Type.ThreeDimensional && gl2 == null) {
            throw new RendererException("3D textures are not supported" + 
                                        " by the video hardware.");
        } else if (type == Texture.Type.TwoDimensionalArray && !caps.contains(Caps.TextureArray)) {
            throw new RendererException("Array textures are not supported" + 
                                        " by the video hardware.");
        }
        
        
        switch (type) {
            case TwoDimensional:
                if (samples > 1) {
                    return GLExt.GL_TEXTURE_2D_MULTISAMPLE;
                } else {
                    return GL.GL_TEXTURE_2D;
                }
            case TwoDimensionalArray:
                if (samples > 1) {
                    return GLExt.GL_TEXTURE_2D_MULTISAMPLE_ARRAY;
                } else {
                    return GLExt.GL_TEXTURE_2D_ARRAY_EXT;
                }
            case ThreeDimensional:
                return GL3.GL_TEXTURE_3D;
            case CubeMap:
                if (face < 0) {
                    return GL.GL_TEXTURE_CUBE_MAP;
                } else if (face < 6) {
                    return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face;
                } else {
                    throw new UnsupportedOperationException("Invalid cube map face index: " + face);
                }
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return GL.GL_LINEAR;
            case Nearest:
                return GL.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter, boolean haveMips) {
        if (haveMips){
            switch (filter) {
                case Trilinear:
                    return GL.GL_LINEAR_MIPMAP_LINEAR;
                case BilinearNearestMipMap:
                    return GL.GL_LINEAR_MIPMAP_NEAREST;
                case NearestLinearMipMap:
                    return GL.GL_NEAREST_MIPMAP_LINEAR;
                case NearestNearestMipMap:
                    return GL.GL_NEAREST_MIPMAP_NEAREST;
                case BilinearNoMipMaps:
                    return GL.GL_LINEAR;
                case NearestNoMipMaps:
                    return GL.GL_NEAREST;
                default:
                    throw new UnsupportedOperationException("Unknown min filter: " + filter);
            }
        } else {
            switch (filter) {
                case Trilinear:
                case BilinearNearestMipMap:
                case BilinearNoMipMaps:
                    return GL.GL_LINEAR;
                case NearestLinearMipMap:
                case NearestNearestMipMap:
                case NearestNoMipMaps:
                    return GL.GL_NEAREST;
                default:
                    throw new UnsupportedOperationException("Unknown min filter: " + filter);
            }
        }
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
            case Clamp:
            case EdgeClamp:
                // Falldown intentional.
                return GL.GL_CLAMP_TO_EDGE;
            case Repeat:
                return GL.GL_REPEAT;
            case MirroredRepeat:
                return GL.GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    @SuppressWarnings("fallthrough")
    private void setupTextureParams(Texture tex) {
        Image image = tex.getImage();
        int target = convertTextureType(tex.getType(), image != null ? image.getMultiSamples() : 1, -1);

        boolean haveMips = true;
        
        if (image != null) {
            haveMips = image.isGeneratedMipmapsRequired() || image.hasMipmaps();
        }
        
        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter(), haveMips);
        int magFilter = convertMagFilter(tex.getMagFilter());
        gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);

        if (tex.getAnisotropicFilter() > 1) {
            if (caps.contains(Caps.TextureFilterAnisotropic)) {
                gl.glTexParameterf(target,
                        GLExt.GL_TEXTURE_MAX_ANISOTROPY_EXT,
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
                if (gl2 != null) {
                    gl2.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
                }
                //There is no break statement on purpose here
            case TwoDimensional:
            case TwoDimensionalArray:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        if(tex.isNeedCompareModeUpdate() && gl2 != null){
            // R to Texture compare mode
            if (tex.getShadowCompareMode() != Texture.ShadowCompareMode.Off) {
                gl2.glTexParameteri(target, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);
                gl2.glTexParameteri(target, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY);            
                if (tex.getShadowCompareMode() == Texture.ShadowCompareMode.GreaterOrEqual) {
                    gl2.glTexParameteri(target, GL2.GL_TEXTURE_COMPARE_FUNC, GL.GL_GEQUAL);
                } else {
                    gl2.glTexParameteri(target, GL2.GL_TEXTURE_COMPARE_FUNC, GL.GL_LEQUAL);
                }
            }else{
                 //restoring default value
                 gl2.glTexParameteri(target, GL2.GL_TEXTURE_COMPARE_MODE, GL.GL_NONE);          
            }
            tex.compareModeUpdated();
        }
    }

    /**
     * Uploads the given image to the GL driver.
     * 
     * @param img The image to upload
     * @param type How the data in the image argument should be interpreted.
     * @param unit The texture slot to be used to upload the image, not important
     */
    public void updateTexImageData(Image img, Texture.Type type, int unit) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            gl.glGenTextures(intBuf1);
            texId = intBuf1.get(0);
            img.setId(texId);
            objManager.registerObject(img);

            statistics.onNewTexture();
        }

        // bind texture       
        int target = convertTextureType(type, img.getMultiSamples(), -1);
        if (context.boundTextureUnit != unit) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
            context.boundTextureUnit = unit;
        }
        if (context.boundTextures[unit] != img) {
            gl.glBindTexture(target, texId);
            context.boundTextures[unit] = img;

            statistics.onTextureUse(img, true);
        }

        if (!img.hasMipmaps() && img.isGeneratedMipmapsRequired()) {
            // Image does not have mipmaps, but they are required.
            // Generate from base level.

            if (!caps.contains(Caps.OpenGL30) && gl2 != null) {
                gl2.glTexParameteri(target, GL2.GL_GENERATE_MIPMAP, GL.GL_TRUE);
                img.setMipmapsGenerated(true);
            } else {
                // For OpenGL3 and up.
                // We'll generate mipmaps via glGenerateMipmapEXT (see below)
            }
        } else if (img.hasMipmaps()) {
            // Image already has mipmaps, set the max level based on the 
            // number of mipmaps we have.
            gl.glTexParameteri(target, GL.GL_TEXTURE_MAX_LEVEL, img.getMipMapSizes().length - 1);
        } else {
            // Image does not have mipmaps and they are not required.
            // Specify that that the texture has no mipmaps.
            gl.glTexParameteri(target, GL.GL_TEXTURE_MAX_LEVEL, 0);
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
        if (!caps.contains(Caps.NonPowerOfTwoTextures) && img.isNPOT()) {
            throw new RendererException("non-power-of-2 framebuffer textures are not supported by the video hardware");
        }

        // Check if graphics card doesn't support multisample textures
        if (!caps.contains(Caps.TextureMultisample)) {
            if (img.getMultiSamples() > 1) {
                throw new RendererException("Multisample textures are not supported by the video hardware");
            }
        }
        
        // Check if graphics card doesn't support depth textures
        if (img.getFormat().isDepthFormat() && !caps.contains(Caps.DepthTexture)) {
            throw new RendererException("Depth textures are not supported by the video hardware");
        }
        
        if (target == GL.GL_TEXTURE_CUBE_MAP) {
            // Check max texture size before upload
            if (img.getWidth() > maxCubeTexSize || img.getHeight() > maxCubeTexSize) {
                throw new RendererException("Cannot upload cubemap " + img + ". The maximum supported cubemap resolution is " + maxCubeTexSize);
            }
            if (img.getWidth() != img.getHeight()) {
                throw new RendererException("Cubemaps must have square dimensions");
            }
        } else {
            if (img.getWidth() > maxTexSize || img.getHeight() > maxTexSize) {
                throw new RendererException("Cannot upload texture " + img + ". The maximum supported texture resolution is " + maxTexSize);
            }
        }

        if (target == GL.GL_TEXTURE_CUBE_MAP) {
            List<ByteBuffer> data = img.getData();
            if (data.size() != 6) {
                logger.log(Level.WARNING, "Invalid texture: {0}\n"
                        + "Cubemap textures must contain 6 data units.", img);
                return;
            }
            for (int i = 0; i < 6; i++) {
                texUtil.uploadTexture(img, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, linearizeSrgbImages);
            }
        } else if (target == GLExt.GL_TEXTURE_2D_ARRAY_EXT) {
            if (!caps.contains(Caps.TextureArray)) {
                throw new RendererException("Texture arrays not supported by graphics hardware");
            }
            
            List<ByteBuffer> data = img.getData();
            
            // -1 index specifies prepare data for 2D Array
            texUtil.uploadTexture(img, target, -1, linearizeSrgbImages);
            
            for (int i = 0; i < data.size(); i++) {
                // upload each slice of 2D array in turn
                // this time with the appropriate index
                texUtil.uploadTexture(img, target, i, linearizeSrgbImages);
            }
        } else {
            texUtil.uploadTexture(img, target, 0, linearizeSrgbImages);
        }

        if (img.getMultiSamples() != imageSamples) {
            img.setMultiSamples(imageSamples);
        }

        if (caps.contains(Caps.OpenGL30) || gl2 == null) {
            if (!img.hasMipmaps() && img.isGeneratedMipmapsRequired() && img.getData(0) != null) {
                if (gl2 != null) gl2.glEnable(target); // XXX: Required for ATI
                glfbo.glGenerateMipmapEXT(target);
                if (gl2 != null) gl2.glDisable(target);
                img.setMipmapsGenerated(true);
            }
        }

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        Image image = tex.getImage();
        if (image.isUpdateNeeded() || (image.isGeneratedMipmapsRequired() && !image.isMipmapsGenerated())) {
            updateTexImageData(image, tex.getType(), unit);
        }

        int texId = image.getId();
        assert texId != -1;

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType(), image.getMultiSamples(), -1);
//        if (!context.textureIndexList.moveToNew(unit)) {
//             if (context.boundTextureUnit != unit){
//                gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
//                context.boundTextureUnit = unit;
//             }
//             gl.glEnable(type);
//        }

        if (context.boundTextureUnit != unit) {
            gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
            context.boundTextureUnit = unit;
        }
        if (textures[unit] != image) {
            gl.glBindTexture(type, texId);
            textures[unit] = image;

            statistics.onTextureUse(image, true);
        } else {
            statistics.onTextureUse(image, false);
        }

        setupTextureParams(tex);
    }

    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
//        setTexture(0, tex);
//        texUtil.uploadSubTexture(caps, pixels, convertTextureType(tex.getType(), pixels.getMultiSamples(), -1), 0, x, y, linearizeSrgbImages);
    }

    public void clearTextureUnits() {
//        IDList textureList = context.textureIndexList;
//        Image[] textures = context.boundTextures;
//        for (int i = 0; i < textureList.oldLen; i++) {
//            int idx = textureList.oldList[i];
//            if (context.boundTextureUnit != idx){
//                gl.glActiveTexture(GL.GL_TEXTURE0 + idx);
//                context.boundTextureUnit = idx;
//            }
//            gl.glDisable(convertTextureType(textures[idx].getType()));
//            textures[idx] = null;
//        }
//        context.textureIndexList.copyNewToOld();
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);
            gl.glDeleteTextures(intBuf1);
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
                return GL.GL_STATIC_DRAW;
            case Dynamic:
                return GL.GL_DYNAMIC_DRAW;
            case Stream:
                return GL.GL_STREAM_DRAW;
            default:
                throw new UnsupportedOperationException("Unknown usage type.");
        }
    }

    private int convertFormat(Format format) {
        switch (format) {
            case Byte:
                return GL.GL_BYTE;
            case UnsignedByte:
                return GL.GL_UNSIGNED_BYTE;
            case Short:
                return GL.GL_SHORT;
            case UnsignedShort:
                return GL.GL_UNSIGNED_SHORT;
            case Int:
                return GL.GL_INT;
            case UnsignedInt:
                return GL.GL_UNSIGNED_INT;
            case Float:
                return GL.GL_FLOAT;
            case Double:
                return GL.GL_DOUBLE;
            default:
                throw new UnsupportedOperationException("Unknown buffer format.");

        }
    }

    public void updateBufferData(VertexBuffer vb) {
        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer
            gl.glGenBuffers(intBuf1);
            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerObject(vb);

            //statistics.onNewVertexBuffer();

            created = true;
        }

        // bind buffer
        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = GL.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
                //statistics.onVertexBufferUse(vb, true);
            } else {
                //statistics.onVertexBufferUse(vb, false);
            }
        } else {
            target = GL.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
                //statistics.onVertexBufferUse(vb, true);
            } else {
                //statistics.onVertexBufferUse(vb, false);
            }
        }

        int usage = convertUsage(vb.getUsage());
        vb.getData().rewind();

//        if (created || vb.hasDataSizeChanged()) {
            // upload data based on format
            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    gl.glBufferData(target, (ByteBuffer) vb.getData(), usage);
                    break;
                //            case Half:
                case Short:
                case UnsignedShort:
                    gl.glBufferData(target, (ShortBuffer) vb.getData(), usage);
                    break;
                case Int:
                case UnsignedInt:
                    if (!caps.contains(Caps.IntegerIndexBuffer)) {
                        throw new RendererException("32-bit index buffers are not supported by the video hardware");
                    }
                    
                    glext.glBufferData(target, (IntBuffer) vb.getData(), usage);
                    break;
                case Float:
                    gl.glBufferData(target, (FloatBuffer) vb.getData(), usage);
                    break;
                case Double:
                    gl.glBufferData(target, (DoubleBuffer) vb.getData(), usage);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown buffer format.");
            }
//        } else {
//            // Invalidate buffer data (orphan) before uploading new data.
//            switch (vb.getFormat()) {
//                case Byte:
//                case UnsignedByte:
//                    gl.glBufferSubData(target, 0, (ByteBuffer) vb.getData());
//                    break;
//                case Short:
//                case UnsignedShort:
//                    gl.glBufferSubData(target, 0, (ShortBuffer) vb.getData());
//                    break;
//                case Int:
//                case UnsignedInt:
//                    gl.glBufferSubData(target, 0, (IntBuffer) vb.getData());
//                    break;
//                case Float:
//                    gl.glBufferSubData(target, 0, (FloatBuffer) vb.getData());
//                    break;
//                case Double:
//                    gl.glBufferSubData(target, 0, (DoubleBuffer) vb.getData());
//                    break;
//                default:
//                    throw new UnsupportedOperationException("Unknown buffer format.");
//            }
//        }

        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);
            gl.glDeleteBuffers(intBuf1);
            vb.resetObject();

            //statistics.onDeleteVertexBuffer();
        }
    }

    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];
            gl.glDisableVertexAttribArray(idx);
            if (context.boundAttribs[idx].isInstanced()) {
                glext.glVertexAttribDivisorARB(idx, 0);
            }
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
            Attribute attrib = context.boundShader.getAttribute(vb.getBufferType());
            int loc = attrib.getLocation();
            if (loc == -1) {
                return; // not defined
            }
            if (loc == -2) {
                stringBuf.setLength(0);
                stringBuf.append("in").append(vb.getBufferType().name()).append('\0');
                loc = gl.glGetAttribLocation(programId, stringBuf.toString());

                // not really the name of it in the shader (inPosition\0) but
                // the internal name of the enum (Position).
                if (loc < 0) {
                    attrib.setLocation(-1);
                    return; // not available in shader.
                } else {
                    attrib.setLocation(loc);
                }
            }
            
            if (vb.isInstanced()) {
                if (!caps.contains(Caps.MeshInstancing)) {
                    throw new RendererException("Instancing is required, "
                            + "but not supported by the "
                            + "graphics hardware");
                }
            }
            int slotsRequired = 1;
            if (vb.getNumComponents() > 4) {
                if (vb.getNumComponents() % 4 != 0) {
                    throw new RendererException("Number of components in multi-slot "
                            + "buffers must be divisible by 4");
                }
                slotsRequired = vb.getNumComponents() / 4;
            }

            if (vb.isUpdateNeeded() && idb == null) {
                updateBufferData(vb);
            }

            VertexBuffer[] attribs = context.boundAttribs;
            for (int i = 0; i < slotsRequired; i++) {
                if (!context.attribIndexList.moveToNew(loc + i)) {
                    gl.glEnableVertexAttribArray(loc + i);
                    //System.out.println("Enabled ATTRIB IDX: "+loc);
                }
            }
            if (attribs[loc] != vb) {
                // NOTE: Use id from interleaved buffer if specified
                int bufId = idb != null ? idb.getId() : vb.getId();
                assert bufId != -1;
                if (context.boundArrayVBO != bufId) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId);
                    context.boundArrayVBO = bufId;
                    //statistics.onVertexBufferUse(vb, true);
                } else {
                    //statistics.onVertexBufferUse(vb, false);
                }

                if (slotsRequired == 1) {
                    gl.glVertexAttribPointer(loc,
                            vb.getNumComponents(),
                            convertFormat(vb.getFormat()),
                            vb.isNormalized(),
                            vb.getStride(),
                            vb.getOffset());
                } else {
                    for (int i = 0; i < slotsRequired; i++) {
                        // The pointer maps the next 4 floats in the slot.
                        // E.g.
                        // P1: XXXX____________XXXX____________
                        // P2: ____XXXX____________XXXX________
                        // P3: ________XXXX____________XXXX____
                        // P4: ____________XXXX____________XXXX
                        // stride = 4 bytes in float * 4 floats in slot * num slots
                        // offset = 4 bytes in float * 4 floats in slot * slot index
                        gl.glVertexAttribPointer(loc + i,
                                4,
                                convertFormat(vb.getFormat()),
                                vb.isNormalized(),
                                4 * 4 * slotsRequired,
                                4 * 4 * i);
                    }
                }

                for (int i = 0; i < slotsRequired; i++) {
                    int slot = loc + i;
                    if (vb.isInstanced() && (attribs[slot] == null || !attribs[slot].isInstanced())) {
                        // non-instanced -> instanced
                        glext.glVertexAttribDivisorARB(slot, vb.getInstanceSpan());
                    } else if (!vb.isInstanced() && attribs[slot] != null && attribs[slot].isInstanced()) {
                        // instanced -> non-instanced
                        glext.glVertexAttribDivisorARB(slot, 0);
                    }
                    attribs[slot] = vb;
                }
            }
        } else {
            throw new IllegalStateException("Cannot render mesh without shader bound");
        }
    }

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    public void drawTriangleArray(Mesh.Mode mode, int count, int vertCount) {
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);
        if (useInstancing) {
            glext.glDrawArraysInstancedARB(convertElementMode(mode), 0,
                    vertCount, count);
        } else {
            gl.glDrawArrays(convertElementMode(mode), 0, vertCount);
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
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufId);
            context.boundElementArrayVBO = bufId;
            //statistics.onVertexBufferUse(indexBuf, true);
        } else {
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
                    elMode = convertElementMode(Mode.TriangleFan);
                }
                int elementLength = elementLengths[i];

                if (useInstancing) {
                    glext.glDrawElementsInstancedARB(elMode,
                            elementLength,
                            fmt,
                            curOffset,
                            count);
                } else {
                    gl.glDrawRangeElements(elMode,
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
                glext.glDrawElementsInstancedARB(convertElementMode(mesh.getMode()),
                        indexBuf.getData().limit(),
                        convertFormat(indexBuf.getFormat()),
                        0,
                        count);
            } else {
                gl.glDrawRangeElements(convertElementMode(mesh.getMode()),
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
                return GL.GL_POINTS;
            case Lines:
                return GL.GL_LINES;
            case LineLoop:
                return GL.GL_LINE_LOOP;
            case LineStrip:
                return GL.GL_LINE_STRIP;
            case Triangles:
                return GL.GL_TRIANGLES;
            case TriangleFan:
                return GL.GL_TRIANGLE_FAN;
            case TriangleStrip:
                return GL.GL_TRIANGLE_STRIP;
            default:
                throw new UnsupportedOperationException("Unrecognized mesh mode: " + mode);
        }
    }

    public void updateVertexArray(Mesh mesh, VertexBuffer instanceData) {
        int id = mesh.getId();
        if (id == -1) {
            IntBuffer temp = intBuf1;
            gl3.glGenVertexArrays(temp);
            id = temp.get(0);
            mesh.setId(id);
        }

        if (context.boundVertexArray != id) {
            gl3.glBindVertexArray(id);
            context.boundVertexArray = id;
        }

        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }
        
        if (instanceData != null) {
            setVertexAttrib(instanceData, null);
        }

        for (VertexBuffer vb : mesh.getBufferList().getArray()) {
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

    private void renderMeshVertexArray(Mesh mesh, int lod, int count, VertexBuffer instanceData) {
        if (mesh.getId() == -1) {
            updateVertexArray(mesh, instanceData);
        } else {
            // TODO: Check if it was updated
        }

        if (context.boundVertexArray != mesh.getId()) {
            gl3.glBindVertexArray(mesh.getId());
            context.boundVertexArray = mesh.getId();
        }

//        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        VertexBuffer indices;
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

    private void renderMeshDefault(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
 
        // Here while count is still passed in.  Can be removed when/if
        // the method is collapsed again.  -pspeed        
        count = Math.max(mesh.getInstanceCount(), count);
    
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }

        VertexBuffer indices;
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);
        }

        if (instanceData != null) {
            for (VertexBuffer vb : instanceData) {
                setVertexAttrib(vb, null);
            }
        }
        
        for (VertexBuffer vb : mesh.getBufferList().getArray()) {
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

    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
        if (mesh.getVertexCount() == 0) {
            return;
        }

        if (context.pointSprite && mesh.getMode() != Mode.Points) {
            // XXX: Hack, disable point sprite mode if mesh not in point mode
            if (context.boundTextures[0] != null) {
                if (context.boundTextureUnit != 0) {
                    gl.glActiveTexture(GL.GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                if (gl2 != null) {
                    gl2.glDisable(GL2.GL_POINT_SPRITE);
                    gl2.glDisable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
                }
                context.pointSprite = false;
            }
        }

        if (gl2 != null) {
            if (context.pointSize != mesh.getPointSize()) {
                gl2.glPointSize(mesh.getPointSize());
                context.pointSize = mesh.getPointSize();
            }
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            gl.glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        statistics.onMeshDrawn(mesh, lod, count);
//        if (ctxCaps.GL_ARB_vertex_array_object){
//            renderMeshVertexArray(mesh, lod, count);
//        }else{
        renderMeshDefault(mesh, lod, count, instanceData);
//        }
    }

    public void setMainFrameBufferSrgb(boolean enableSrgb) {
        // Gamma correction
        if (!caps.contains(Caps.Srgb)) {
            // Not supported, sorry.
            logger.warning("sRGB framebuffer is not supported " + 
                           "by video hardware, but was requested."); 
            
            return;
        }
        
        setFrameBuffer(null);

        if (enableSrgb) {
            if (!getBoolean(GLExt.GL_FRAMEBUFFER_SRGB_CAPABLE_EXT)) {
                logger.warning("Driver claims that default framebuffer "
                        + "is not sRGB capable. Enabling anyway.");
            }

            gl.glEnable(GLExt.GL_FRAMEBUFFER_SRGB_EXT);

            logger.log(Level.FINER, "SRGB FrameBuffer enabled (Gamma Correction)");
        } else {
            gl.glDisable(GLExt.GL_FRAMEBUFFER_SRGB_EXT);
        }
    }

    public void setLinearizeSrgbImages(boolean linearize) {
        if (caps.contains(Caps.Srgb)) {
            linearizeSrgbImages = linearize;
        }
    }
}
