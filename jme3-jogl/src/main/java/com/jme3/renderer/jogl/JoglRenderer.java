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
package com.jme3.renderer.jogl;

import com.jme3.light.LightList;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.IDList;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.Statistics;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.shader.Attribute;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.shader.Uniform;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.RenderBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.util.BufferUtils;
import com.jme3.util.ListMap;
import com.jme3.util.NativeObjectManager;
import com.jme3.util.SafeArrayList;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;
import jme3tools.converters.MipMapGenerator;
import jme3tools.shader.ShaderDebug;

public class JoglRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());
    private static final boolean VALIDATE_SHADER = false;
    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final IntBuffer intBuf1 = BufferUtils.createIntBuffer(1);
    private final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    protected FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);
    private RenderContext context = new RenderContext();
    private NativeObjectManager objManager = new NativeObjectManager();
    private EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    //current state
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
    private FrameBuffer lastFb = null;
    private FrameBuffer mainFbOverride = null;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;
    private boolean linearizeSrgbImages;

    public JoglRenderer() {
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

    public void initialize() {
        GL gl = GLContext.getCurrentGL();
        //logger.log(Level.FINE, "Vendor: {0}", gl.glGetString(GL.GL_VENDOR));
        //logger.log(Level.FINE, "Renderer: {0}", gl.glGetString(GL.GL_RENDERER));
        //logger.log(Level.FINE, "Version: {0}", gl.glGetString(GL.GL_VERSION));
        if (gl.isExtensionAvailable("GL_VERSION_2_0")) {
            caps.add(Caps.OpenGL20);
            if (gl.isExtensionAvailable("GL_VERSION_2_1")) {
                caps.add(Caps.OpenGL21);
                if (gl.isExtensionAvailable("GL_VERSION_3_0")) {
                    caps.add(Caps.OpenGL30);
                    if (gl.isExtensionAvailable("GL_VERSION_3_1")) {
                        caps.add(Caps.OpenGL31);
                        if (gl.isExtensionAvailable("GL_VERSION_3_2")) {
                            caps.add(Caps.OpenGL32);
                        }
                    }
                }
            }
        }

        //workaround, always assume we support GLSL100
        //some cards just don't report this correctly
        caps.add(Caps.GLSL100);
        
        String versionStr = null;
        if (caps.contains(Caps.OpenGL20) || gl.isGL2ES2()) {
            versionStr = gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION);
        }
        if (versionStr == null || versionStr.equals("")) {
            glslVer = -1;
            throw new UnsupportedOperationException("GLSL and OpenGL2 is " +
             "required for the JOGL " +
             "renderer!");
        }
        
        // Fix issue in TestRenderToMemory when GL_FRONT is the main
        // buffer being used.
        gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, intBuf1);
        initialDrawBuf = intBuf1.get(0);
        gl.glGetIntegerv(GL2GL3.GL_READ_BUFFER, intBuf1);
        initialReadBuf = intBuf1.get(0);
        
        // XXX: This has to be GL_BACK for canvas on Mac
        // Since initialDrawBuf is GL_FRONT for pbuffer, gotta
        // change this value later on ...
//        initialDrawBuf = GL_BACK;
//        initialReadBuf = GL_BACK;
        
        int spaceIdx = versionStr.indexOf(" ");
        if (spaceIdx >= 1) {
            versionStr = versionStr.substring(0, spaceIdx);
        }
        
        try {
            float version = Float.parseFloat(versionStr);
            glslVer = (int) (version * 100);
        } catch (NumberFormatException e) {
            // the parsing fails on Raspberry Pi
            if (NativeWindowFactory.getNativeWindowType(false).equals(NativeWindowFactory.TYPE_BCM_VC_IV)) {
                logger.warning("Failed parsing GLSL version assuming it's v1.00");
                glslVer = 100;
            }
        }

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

        gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, intBuf16);
        vertexTextureUnits = intBuf16.get(0);
        logger.log(Level.FINER, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, intBuf16);
        fragTextureUnits = intBuf16.get(0);
        logger.log(Level.FINER, "Texture Units: {0}", fragTextureUnits);

        gl.glGetIntegerv(GL2GL3.GL_MAX_VERTEX_UNIFORM_COMPONENTS, intBuf16);
        vertexUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);

        gl.glGetIntegerv(GL2GL3.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS, intBuf16);
        fragUniforms = intBuf16.get(0);
        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);

        gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_ATTRIBS, intBuf16);
        vertexAttribs = intBuf16.get(0);
        logger.log(Level.FINER, "Vertex Attributes: {0}", vertexAttribs);

        gl.glGetIntegerv(GL2GL3.GL_MAX_VARYING_FLOATS, intBuf16);
        int varyingFloats = intBuf16.get(0);
        logger.log(Level.FINER, "Varying Floats: {0}", varyingFloats);

        gl.glGetIntegerv(GL.GL_SUBPIXEL_BITS, intBuf16);
        int subpixelBits = intBuf16.get(0);
        logger.log(Level.FINER, "Subpixel Bits: {0}", subpixelBits);

        gl.glGetIntegerv(GL2GL3.GL_MAX_ELEMENTS_VERTICES, intBuf16);
        maxVertCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);

        gl.glGetIntegerv(GL2GL3.GL_MAX_ELEMENTS_INDICES, intBuf16);
        maxTriCount = intBuf16.get(0);
        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);

        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, intBuf16);
        maxTexSize = intBuf16.get(0);
        logger.log(Level.FINER, "Maximum Texture Resolution: {0}", maxTexSize);

        gl.glGetIntegerv(GL.GL_MAX_CUBE_MAP_TEXTURE_SIZE, intBuf16);
        maxCubeTexSize = intBuf16.get(0);
        logger.log(Level.FINER, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);

        if (gl.isExtensionAvailable("GL_ARB_color_buffer_float")) {
            // XXX: Require both 16 and 32 bit float support for FloatColorBuffer.
            if (gl.isExtensionAvailable("GL_ARB_half_float_pixel")) {
                caps.add(Caps.FloatColorBuffer);
            }
        }

        if (gl.isExtensionAvailable("GL_ARB_depth_buffer_float")) {
            caps.add(Caps.FloatDepthBuffer);
        }
        
        if (caps.contains(Caps.OpenGL30)) {
            caps.add(Caps.PackedDepthStencilBuffer);
        }

        if (gl.isExtensionAvailable("GL_ARB_draw_instanced")) {
            caps.add(Caps.MeshInstancing);
        }

        if (gl.isExtensionAvailable("GL_ARB_fragment_program")) {
            caps.add(Caps.ARBprogram);
        }

        if (gl.isExtensionAvailable("GL_ARB_texture_buffer_object")) {
            caps.add(Caps.TextureBuffer);
        }

        if (gl.isExtensionAvailable("GL_ARB_texture_float")) {
            if (gl.isExtensionAvailable("GL_ARB_half_float_pixel")) {
                caps.add(Caps.FloatTexture);
            }
        }

        if (gl.isExtensionAvailable("GL_ARB_vertex_array_object")) {
            caps.add(Caps.VertexBufferArray);
        }
        
        if (gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two")) {
            caps.add(Caps.NonPowerOfTwoTextures);
        }
        else {
            logger.log(Level.WARNING, "Your graphics card does not "
                    + "support non-power-of-2 textures. "
                    + "Some features might not work.");
        }

        boolean latc = gl.isExtensionAvailable("GL_EXT_texture_compression_latc");
        //FIXME ignore atdc?
        boolean atdc = gl.isExtensionAvailable("GL_ATI_texture_compression_3dc");
        if (latc || atdc) {
            caps.add(Caps.TextureCompressionLATC);
        }

        if (gl.isExtensionAvailable("GL_EXT_packed_float")) {
            caps.add(Caps.PackedFloatColorBuffer);
            if (gl.isExtensionAvailable("GL_ARB_half_float_pixel")) {
                // because textures are usually uploaded as RGB16F
                // need half-float pixel
                caps.add(Caps.PackedFloatTexture);
            }
        }

        if (gl.isExtensionAvailable("GL_EXT_texture_array")) {
            caps.add(Caps.TextureArray);
        }

        if (gl.isExtensionAvailable("GL_EXT_texture_shared_exponent")) {
            caps.add(Caps.SharedExponentTexture);
        }

        if (gl.isExtensionAvailable("GL_EXT_framebuffer_object")) {
            caps.add(Caps.FrameBuffer);

            gl.glGetIntegerv(GL.GL_MAX_RENDERBUFFER_SIZE, intBuf16);
            maxRBSize = intBuf16.get(0);
            logger.log(Level.FINER, "FBO RB Max Size: {0}", maxRBSize);

            gl.glGetIntegerv(GL2GL3.GL_MAX_COLOR_ATTACHMENTS, intBuf16);
            maxFBOAttachs = intBuf16.get(0);
            logger.log(Level.FINER, "FBO Max renderbuffers: {0}", maxFBOAttachs);
            
            if (gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")) {
                caps.add(Caps.FrameBufferMultisample);

                gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, intBuf16);
                maxFBOSamples = intBuf16.get(0);
                logger.log(Level.FINER, "FBO Max Samples: {0}", maxFBOSamples);
            }

            if (gl.isExtensionAvailable("GL_ARB_texture_multisample")) {
                caps.add(Caps.TextureMultisample);

                gl.glGetIntegerv(GL3.GL_MAX_COLOR_TEXTURE_SAMPLES, intBuf16);
                maxColorTexSamples = intBuf16.get(0);
                logger.log(Level.FINER, "Texture Multisample Color Samples: {0}", maxColorTexSamples);

                gl.glGetIntegerv(GL3.GL_MAX_DEPTH_TEXTURE_SAMPLES, intBuf16);
                maxDepthTexSamples = intBuf16.get(0);
                logger.log(Level.FINER, "Texture Multisample Depth Samples: {0}", maxDepthTexSamples);
            }
            
            gl.glGetIntegerv(GL2ES2.GL_MAX_DRAW_BUFFERS, intBuf16);
            maxMRTFBOAttachs = intBuf16.get(0);
            if (maxMRTFBOAttachs > 1) {
                caps.add(Caps.FrameBufferMRT);
                logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
            }
            
            //if (gl.isExtensionAvailable("GL_ARB_draw_buffers")) {
            //    caps.add(Caps.FrameBufferMRT);
            //    gl.glGetIntegerv(GL2GL3.GL_MAX_DRAW_BUFFERS, intBuf16);
            //    maxMRTFBOAttachs = intBuf16.get(0);
            //    logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
            //}
        }

        if (gl.isExtensionAvailable("GL_ARB_multisample")) {
            gl.glGetIntegerv(GL.GL_SAMPLE_BUFFERS, intBuf16);
            boolean available = intBuf16.get(0) != 0;
            gl.glGetIntegerv(GL.GL_SAMPLES, intBuf16);
            int samples = intBuf16.get(0);
            logger.log(Level.FINER, "Samples: {0}", samples);
            boolean enabled = gl.glIsEnabled(GL.GL_MULTISAMPLE);
            if (samples > 0 && available && !enabled) {
                gl.glEnable(GL.GL_MULTISAMPLE);
            }
            caps.add(Caps.Multisample);
        }
        
        //supports sRGB pipeline
        if (gl.isExtensionAvailable("GL_ARB_framebuffer_sRGB") && gl.isExtensionAvailable("GL_EXT_texture_sRGB")){
            caps.add(Caps.Srgb);
        }
        
        logger.log(Level.FINE, "Caps: {0}", caps);
    }
    
    public void invalidateState() {
        context.reset();
        boundShader = null;
        lastFb = null;

        GL gl = GLContext.getCurrentGL();
        gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, intBuf1);
        initialDrawBuf = intBuf1.get(0);
        gl.glGetIntegerv(GL2GL3.GL_READ_BUFFER, intBuf1);
        initialReadBuf = intBuf1.get(0);
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
        GL gl = GLContext.getCurrentGL();
        gl.glDepthRange(start, end);
    }
    
    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        GL gl = GLContext.getCurrentGL();
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

            //glClear(GL_DEPTH_BUFFER_BIT) seems to not work when glDepthMask is false
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
        GL gl = GLContext.getCurrentGL();
        gl.glClearColor(color.r, color.g, color.b, color.a);
    }

    public void setAlphaToCoverage(boolean value) {
        if (caps.contains(Caps.Multisample)) {
            GL gl = GLContext.getCurrentGL();
            if (value) {
                gl.glEnable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
            } else {
                gl.glDisable(GL.GL_SAMPLE_ALPHA_TO_COVERAGE);
            }
        }
    }

    public void applyRenderState(RenderState state) {
        GL gl = GLContext.getCurrentGL();
        if (state.isWireframe() && !context.wireframe) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            }
            context.wireframe = true;
        } else if (!state.isWireframe() && context.wireframe) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            }
            context.wireframe = false;
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

        if (state.isAlphaTest() && context.alphaTestFallOff == 0) {
            gl.glEnable(GL2ES1.GL_ALPHA_TEST);
            if (gl.isGL2ES1()) {
                gl.getGL2ES1().glAlphaFunc(convertTestFunction(context.alphaFunc), state.getAlphaFallOff());               
            }
            context.alphaTestFallOff = state.getAlphaFallOff();
        } else if (!state.isAlphaTest() && context.alphaTestFallOff != 0) {
            if (gl.isGL2ES1()) {
                gl.glDisable(GL2ES1.GL_ALPHA_TEST);
            }
            context.alphaTestFallOff = 0;
        }
        if (state.getAlphaFunc() != context.alphaFunc && gl.isGL2ES1()) {
            gl.getGL2ES1().glAlphaFunc(convertTestFunction(context.alphaFunc), state.getAlphaFallOff());  
            context.alphaFunc = state.getAlphaFunc();
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
                if (gl.isGL2ES1()) {
                    gl.glEnable(GL2ES1.GL_POINT_SPRITE);
                }
                if (gl.isGL2GL3()) {
                    gl.glEnable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
                }
            }
            context.pointSprite = true;
        } else if (!state.isPointSprite() && context.pointSprite) {
            if (context.boundTextures[0] != null) {
                if (context.boundTextureUnit != 0) {
                    gl.glActiveTexture(GL.GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                if (gl.isGL2ES1()) {
                    gl.glDisable(GL2ES1.GL_POINT_SPRITE);
                }
                if (gl.isGL2GL3()) {
                    gl.glDisable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
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
                gl.getGL2ES2().glStencilOpSeparate(GL.GL_FRONT,
                        convertStencilOperation(state.getFrontStencilStencilFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthFailOperation()),
                        convertStencilOperation(state.getFrontStencilDepthPassOperation()));
                gl.getGL2ES2().glStencilOpSeparate(GL.GL_BACK,
                        convertStencilOperation(state.getBackStencilStencilFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthFailOperation()),
                        convertStencilOperation(state.getBackStencilDepthPassOperation()));
                gl.getGL2ES2().glStencilFuncSeparate(GL.GL_FRONT,
                        convertTestFunction(state.getFrontStencilFunction()),
                        0, Integer.MAX_VALUE);
                gl.getGL2ES2().glStencilFuncSeparate(GL.GL_BACK,
                        convertTestFunction(state.getBackStencilFunction()),
                        0, Integer.MAX_VALUE);
            } else {
                gl.glDisable(GL.GL_STENCIL_TEST);
            }
        }
    }
    
    private int convertStencilOperation(RenderState.StencilOperation stencilOp) {
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

    private int convertTestFunction(RenderState.TestFunction testFunc) {
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
            GL gl = GLContext.getCurrentGL();
            gl.glViewport(x, y, w, h);
            vpX = x;
            vpY = y;
            vpW = w;
            vpH = h;
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        GL gl = GLContext.getCurrentGL();
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
            GL gl = GLContext.getCurrentGL();
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
    }
    
    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
    }

    /*********************************************************************\
    |* Shaders                                                           *|
    \*********************************************************************/
    protected void updateUniformLocation(Shader shader, Uniform uniform) {
        GL gl = GLContext.getCurrentGL();
        // passing a null terminated string is not necessary with JOGL 2.0
        int loc = gl.getGL2ES2().glGetUniformLocation(shader.getId(), uniform.getName());
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
            GL gl = GLContext.getCurrentGL();
            gl.getGL2ES2().glUseProgram(shaderId);
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
        assert shaderId > 0;

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
        GL gl = GLContext.getCurrentGL();
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                gl.getGL2ES2().glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                gl.getGL2ES2().glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                gl.getGL2ES2().glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    gl.getGL2ES2().glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else if (val instanceof Vector4f) {
                    Vector4f c = (Vector4f) val;
                    gl.getGL2ES2().glUniform4f(loc, c.x, c.y, c.z, c.w);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    gl.getGL2ES2().glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                gl.getGL2ES2().glUniform1i(loc, b.booleanValue() ? GL.GL_TRUE : GL.GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                gl.getGL2ES2().glUniformMatrix3fv(loc, 1, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                gl.getGL2ES2().glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case IntArray:
                ib = (IntBuffer) uniform.getValue();
                gl.getGL2ES2().glUniform1iv(loc, ib.remaining(), ib);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2ES2().glUniform1fv(loc, fb.remaining(), fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2ES2().glUniform2fv(loc, fb.remaining(), fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2ES2().glUniform3fv(loc, fb.remaining(), fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2ES2().glUniform4fv(loc, fb.remaining(), fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2ES2().glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                gl.getGL2ES2().glUniform1i(loc, i.intValue());
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
    
    public int convertShaderType(Shader.ShaderType type) {
        switch (type) {
            case Fragment:
                return GL2ES2.GL_FRAGMENT_SHADER;
            case Vertex:
                return GL2ES2.GL_VERTEX_SHADER;
//            case Geometry:
//                return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new UnsupportedOperationException("Unrecognized shader type.");
        }
    }
    
    public void updateShaderSourceData(ShaderSource source) {
        int id = source.getId();
        GL gl = GLContext.getCurrentGL();
        if (id == -1) {
            // Create id
            id = gl.getGL2ES2().glCreateShader(convertShaderType(source.getType()));
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
        
        byte[] array = new byte[codeBuf.limit()];
        codeBuf.rewind();
        codeBuf.get(array);
        codeBuf.rewind();

        gl.getGL2ES2().glShaderSource(id, 1, new String[]{new String(array)}, new int[]{array.length}, 0);
        gl.getGL2ES2().glCompileShader(id);

        gl.getGL2ES2().glGetShaderiv(id, GL2ES2.GL_COMPILE_STATUS, intBuf1);

        boolean compiledOK = intBuf1.get(0) == GL.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            gl.getGL2ES2().glGetShaderiv(id, GL2ES2.GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                gl.getGL2ES2().glGetShaderInfoLog(id, length, null, logBuf);
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                // convert to string, etc
                infoLog = new String(logBytes);
            }
        }

        if (compiledOK) {
            if (infoLog != null) {
                logger.log(Level.FINE, "{0} compile success\n{1}",
                        new Object[]{source.getName(), infoLog});
            } else {
                logger.log(Level.FINE, "{0} compile success", source.getName());
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
        GL gl = GLContext.getCurrentGL();
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program
            id = gl.getGL2ES2().glCreateProgram();
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
            gl.getGL2ES2().glAttachShader(id, source.getId());
        }

        if (caps.contains(Caps.OpenGL30) && gl.isGL2GL3()) {
            // Check if GLSL version is 1.5 for shader
            gl.getGL2GL3().glBindFragDataLocation(id, 0, "outFragColor");
            // For MRT
            for (int i = 0; i < maxMRTFBOAttachs; i++) {
                gl.getGL2GL3().glBindFragDataLocation(id, i, "outFragData[" + i + "]");
            }
        }

        // Link shaders to program
        gl.getGL2ES2().glLinkProgram(id);

        // Check link status
        gl.getGL2ES2().glGetProgramiv(id, GL2ES2.GL_LINK_STATUS, intBuf1);
        boolean linkOK = intBuf1.get(0) == GL.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            gl.getGL2ES2().glGetProgramiv(id, GL2ES2.GL_INFO_LOG_LENGTH, intBuf1);
            int length = intBuf1.get(0);
            if (length > 3) {
                // get infos
                ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                gl.getGL2ES2().glGetProgramInfoLog(id, length, null, logBuf);

                // convert to string, etc
                byte[] logBytes = new byte[length];
                logBuf.get(logBytes, 0, length);
                infoLog = new String(logBytes);
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
                throw new RendererException("Shader link failure, shader:" + shader + "\n" + infoLog);
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
        GL gl = GLContext.getCurrentGL();
        gl.getGL2ES2().glDeleteShader(source.getId());
        source.resetObject();
    }

    public void deleteShader(Shader shader) {
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }

        GL gl = GLContext.getCurrentGL();
        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {
                gl.getGL2ES2().glDetachShader(shader.getId(), source.getId());
                deleteShaderSource(source);
            }
        }

        gl.getGL2ES2().glDeleteProgram(shader.getId());
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
        GL gl = GLContext.getCurrentGL();
        if (gl.isExtensionAvailable("GL_EXT_framebuffer_blit") && gl.isGL2GL3()) {
            int srcX0 = 0;
            int srcY0 = 0;
            int srcX1/* = 0*/;
            int srcY1/* = 0*/;

            int dstX0 = 0;
            int dstY0 = 0;
            int dstX1/* = 0*/;
            int dstY1/* = 0*/;

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
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, 0);
                srcX0 = vpX;
                srcY0 = vpY;
                srcX1 = vpX + vpW;
                srcY1 = vpY + vpH;
            } else {
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, src.getId());
                srcX1 = src.getWidth();
                srcY1 = src.getHeight();
            }
            if (dst == null) {
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, 0);
                dstX0 = vpX;
                dstY0 = vpY;
                dstX1 = vpX + vpW;
                dstY1 = vpY + vpH;
            } else {
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, dst.getId());
                dstX1 = dst.getWidth();
                dstY1 = dst.getHeight();
            }
            int mask = GL.GL_COLOR_BUFFER_BIT;
            if (copyDepth) {
                mask |= GL.GL_DEPTH_BUFFER_BIT;
            }
            gl.getGL2GL3().glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1,
                    dstX0, dstY0, dstX1, dstY1, mask,
                    GL.GL_NEAREST);
            gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, prevFBO);

            
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
    
    private String getTargetBufferName(int buffer) {
        switch (buffer) {
            case GL.GL_NONE:
                return "NONE";
            case GL.GL_FRONT:
                return "GL_FRONT";
            case GL.GL_BACK:
                return "GL_BACK";
            default:
                if (buffer >= GL.GL_COLOR_ATTACHMENT0
                        && buffer <= GL2ES2.GL_COLOR_ATTACHMENT15) {
                    return "GL_COLOR_ATTACHMENT"
                            + (buffer - GL.GL_COLOR_ATTACHMENT0);
                } else {
                    return "UNKNOWN? " + buffer;
                }
        }
    }

    private void printRealRenderBufferInfo(FrameBuffer fb, RenderBuffer rb, String name) {
        GL gl = GLContext.getCurrentGL();
        System.out.println("== Renderbuffer " + name + " ==");
        System.out.println("RB ID: " + rb.getId());
        System.out.println("Is proper? " + gl.glIsRenderbuffer(rb.getId()));

        int attachment = convertAttachmentSlot(rb.getSlot());

        gl.glGetFramebufferAttachmentParameteriv(GL2GL3.GL_DRAW_FRAMEBUFFER,
                attachment,
                GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, intBuf16);
        int type = intBuf16.get(0);
        gl.glGetFramebufferAttachmentParameteriv(GL2GL3.GL_DRAW_FRAMEBUFFER,
                attachment,
                GL.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, intBuf16);
        int rbName  = intBuf16.get(0);

        switch (type) {
            case GL.GL_NONE:
                System.out.println("Type: None");
                break;
            case GL.GL_TEXTURE:
                System.out.println("Type: Texture");
                break;
            case GL.GL_RENDERBUFFER:
                System.out.println("Type: Buffer");
                System.out.println("RB ID: " + rbName);
                break;
        }



    }

    private void printRealFrameBufferInfo(FrameBuffer fb) {
        GL gl = GLContext.getCurrentGL();
        final byte[] param = new byte[1];
        gl.glGetBooleanv(GL2GL3.GL_DOUBLEBUFFER, param, 0);
        boolean doubleBuffer = param[0] != (byte) 0x00;
        gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, intBuf16);
        String drawBuf = getTargetBufferName(intBuf16.get(0));
        gl.glGetIntegerv(GL2GL3.GL_READ_BUFFER, intBuf16);
        String readBuf = getTargetBufferName(intBuf16.get(0));

        int fbId = fb.getId();
        gl.glGetIntegerv(GL2GL3.GL_DRAW_FRAMEBUFFER_BINDING, intBuf16);
        int curDrawBinding = intBuf16.get(0);
        gl.glGetIntegerv(GL2GL3.GL_READ_FRAMEBUFFER_BINDING, intBuf16);
        int curReadBinding = intBuf16.get(0);

        System.out.println("=== OpenGL FBO State ===");
        System.out.println("Context doublebuffered? " + doubleBuffer);
        System.out.println("FBO ID: " + fbId);
        System.out.println("Is proper? " + gl.glIsFramebuffer(fbId));
        System.out.println("Is bound to draw? " + (fbId == curDrawBinding));
        System.out.println("Is bound to read? " + (fbId == curReadBinding));
        System.out.println("Draw buffer: " + drawBuf);
        System.out.println("Read buffer: " + readBuf);

        if (context.boundFBO != fbId) {
            gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, fbId);
            context.boundFBO = fbId;
        }

        if (fb.getDepthBuffer() != null) {
            printRealRenderBufferInfo(fb, fb.getDepthBuffer(), "Depth");
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
            printRealRenderBufferInfo(fb, fb.getColorBuffer(i), "Color" + i);
        }
    }
    
    private void checkFrameBufferError() {
        GL gl = GLContext.getCurrentGL();
        int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        switch (status) {
            case GL.GL_FRAMEBUFFER_COMPLETE:
                break;
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
                // Choose different formats
                throw new IllegalStateException("Framebuffer object format is "
                        + "unsupported by the video hardware.");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                throw new IllegalStateException("Framebuffer has erronous attachment.");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                throw new IllegalStateException("Framebuffer is missing required attachment.");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                throw new IllegalStateException(
                        "Framebuffer attachments must have same dimensions.");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                throw new IllegalStateException("Framebuffer attachments must have same formats.");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                throw new IllegalStateException("Incomplete draw buffer.");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                throw new IllegalStateException("Incomplete read buffer.");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                throw new IllegalStateException("Incomplete multisample buffer.");
            default:
                // Programming error; will fail on all hardware
                throw new IllegalStateException("Some video driver error "
                        + "or programming error occured. "
                        + "Framebuffer object status is invalid. ");
        }
    }
    
    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        GL gl = GLContext.getCurrentGL();
        int id = rb.getId();
        if (id == -1) {
            gl.glGenRenderbuffers(1, intBuf1);
            id = intBuf1.get(0);
            rb.setId(id);
        }

        if (context.boundRB != id) {
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, id);
            context.boundRB = id;
        }

        if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize) {
            throw new RendererException("Resolution " + fb.getWidth()
                    + ":" + fb.getHeight() + " is not supported.");
        }

        TextureUtil.GLImageFormat glFmt = TextureUtil.getImageFormatWithError(rb.getFormat(), fb.isSrgb());
        
        if (fb.getSamples() > 1 && gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")
                && gl.isGL2GL3()/*&& gl.isFunctionAvailable("glRenderbufferStorageMultisample")*/) {
            int samples = fb.getSamples();
            if (maxFBOSamples < samples) {
                samples = maxFBOSamples;
            }
            gl.getGL2GL3()
                    .glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples,
                    glFmt.internalFormat, fb.getWidth(),
                    fb.getHeight());
        } else {
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER,
                    glFmt.internalFormat, fb.getWidth(), fb.getHeight());
        }
    }
    
    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == -100) {
            return GL.GL_DEPTH_ATTACHMENT;
        } else if (attachmentSlot < 0 || attachmentSlot >= 16) {
            throw new UnsupportedOperationException("Invalid FBO attachment slot: "
                    + attachmentSlot);
        }

        return GL.GL_COLOR_ATTACHMENT0 + attachmentSlot;
    }
    
    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        GL gl = GLContext.getCurrentGL();
        Texture tex = rb.getTexture();
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), 0);

            // NOTE: For depth textures, sets nearest/no-mips mode
            // Required to fix "framebuffer unsupported"
            // for old NVIDIA drivers!
            setupTextureParams(tex);
        }

        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType(), image.getMultiSamples(), rb.getFace()),
                image.getId(), 0);
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
            GL gl = GLContext.getCurrentGL();
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, convertAttachmentSlot(rb.getSlot()),
                    GL.GL_RENDERBUFFER, rb.getId());
        }
    }
    
    public void updateFrameBuffer(FrameBuffer fb) {
        GL gl = GLContext.getCurrentGL();
        int id = fb.getId();
        if (id == -1) {
            // create FBO
            gl.glGenFramebuffers(1, intBuf1);
            id = intBuf1.get(0);
            fb.setId(id);
            objManager.registerObject(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, id);
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
        GL gl = GLContext.getCurrentGL();
        if (gl.isGL2GL3()) {
            for (int i = 0; i < samplePositions.length; i++) {
                gl.getGL3().glGetMultisamplefv(GL3.GL_SAMPLE_POSITION, i, samplePos);
                samplePos.clear();
                samplePositions[i] = new Vector2f(samplePos.get(0) - 0.5f,
                        samplePos.get(1) - 0.5f);
            }
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

        if (lastFb == fb) {
            if (fb == null || !fb.isUpdateNeeded()) {
                return;
            }
        }

        GL gl = GLContext.getCurrentGL();
        // generate mipmaps for last FB if needed
        if (lastFb != null) {
            for (int i = 0; i < lastFb.getNumColorBuffers(); i++) {
                RenderBuffer rb = lastFb.getColorBuffer(i);
                Texture tex = rb.getTexture();
                if (tex != null
                        && tex.getMinFilter().usesMipMapLevels()) {
                    setTexture(0, rb.getTexture());

                    int textureType = convertTextureType(tex.getType(), tex.getImage().getMultiSamples(), rb.getFace());
                    gl.glEnable(textureType);
                    gl.glGenerateMipmap(textureType);
                    gl.glDisable(textureType);
                }
            }
        }
        
        if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
                statistics.onFrameBufferUse(null, true);

                context.boundFBO = 0;
            }
            // select back buffer
            if (context.boundDrawBuf != -1) {
                if (gl.isGL2GL3()) {
                    gl.getGL2GL3().glDrawBuffer(initialDrawBuf);
                }
                context.boundDrawBuf = -1;
            }
            if (context.boundReadBuf != -1) {
                if (gl.isGL2GL3()) {
                    gl.getGL2GL3().glReadBuffer(initialReadBuf);
                }
                context.boundReadBuf = -1;
            }

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
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fb.getId());
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
                    if (gl.isGL2GL3()) {
                        gl.getGL2GL3().glDrawBuffer(GL.GL_NONE);
                    }
                    context.boundDrawBuf = -2;
                }
                if (context.boundReadBuf != -2) {
                    if (gl.isGL2GL3()) {
                        gl.getGL2GL3().glReadBuffer(GL.GL_NONE);
                    }
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
                            intBuf16.put(GL.GL_COLOR_ATTACHMENT0 + i);
                        }

                        intBuf16.flip();
                        if (gl.isGL2GL3()) {
                            gl.getGL2GL3().glDrawBuffers(intBuf16.limit(), intBuf16);
                        }
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                } else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (context.boundDrawBuf != rb.getSlot()) {
                        if (gl.isGL2GL3()) {
                            gl.getGL2GL3().glDrawBuffer(GL.GL_COLOR_ATTACHMENT0 + rb.getSlot());
                        }
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
        GL gl = GLContext.getCurrentGL();
        if (fb != null) {
            RenderBuffer rb = fb.getColorBuffer();
            if (rb == null) {
                throw new IllegalArgumentException("Specified framebuffer"
                        + " does not have a colorbuffer");
            }

            setFrameBuffer(fb);
            if (context.boundReadBuf != rb.getSlot()) {
                if (gl.isGL2GL3()) {
                    gl.getGL2GL3().glReadBuffer(GL.GL_COLOR_ATTACHMENT0 + rb.getSlot());
                }
                context.boundReadBuf = rb.getSlot();
            }
        } else {
            setFrameBuffer(null);
        }

        gl.glReadPixels(vpX, vpY, vpW, vpH, /*GL.GL_RGBA*/ GL.GL_BGRA, GL.GL_UNSIGNED_BYTE, byteBuf);
    }
    
    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1.put(0, rb.getId());
        GL gl = GLContext.getCurrentGL();
        gl.glDeleteRenderbuffers(1, intBuf1);
    }
    
    public void deleteFrameBuffer(FrameBuffer fb) {
        if (fb.getId() != -1) {
            GL gl = GLContext.getCurrentGL();
            if (context.boundFBO == fb.getId()) {
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
                context.boundFBO = 0;
            }

            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1.put(0, fb.getId());
            gl.glDeleteFramebuffers(1, intBuf1);
            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

    /*********************************************************************\
    |* Textures                                                          *|
    \*********************************************************************/
    private int convertTextureType(Texture.Type type, int samples, int face) {
        switch (type) {
            case TwoDimensional:
                if (samples > 1) {
                    return GL3.GL_TEXTURE_2D_MULTISAMPLE;
                } else {
                    return GL.GL_TEXTURE_2D;
                }
            case TwoDimensionalArray:
                if (samples > 1) {
                    return GL3.GL_TEXTURE_2D_MULTISAMPLE_ARRAY;
                } else {
                    return GL.GL_TEXTURE_2D_ARRAY;
                }
            case ThreeDimensional:
                return GL2ES2.GL_TEXTURE_3D;
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

    private int convertMinFilter(Texture.MinFilter filter) {
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
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
                return GL2GL3.GL_CLAMP_TO_BORDER;
            case Clamp:
                return GL2.GL_CLAMP;
            case EdgeClamp:
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
        GL gl = GLContext.getCurrentGL();
        Image image = tex.getImage();
        int target = convertTextureType(tex.getType(), image != null ? image.getMultiSamples() : 1, -1);

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);

        if (tex.getAnisotropicFilter() > 1) {
            if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
                gl.glTexParameterf(target,
                        GL.GL_TEXTURE_MAX_ANISOTROPY_EXT,
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
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
            case TwoDimensionalArray:
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        if (tex.isNeedCompareModeUpdate()) {
            // R to Texture compare mode
            if (tex.getShadowCompareMode() != Texture.ShadowCompareMode.Off) {
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);
                gl.glTexParameteri(target, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY);
                if (tex.getShadowCompareMode() == Texture.ShadowCompareMode.GreaterOrEqual) {
                    gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_COMPARE_FUNC, GL.GL_GEQUAL);
                } else {
                    gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_COMPARE_FUNC, GL.GL_LEQUAL);
                }
            } else {
                //restoring default value
                gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_COMPARE_MODE, GL.GL_NONE);
            }
            tex.compareModeUpdated();
        }
    }

    /**
     * Uploads the given image to the GL driver.
     *
     * @param img The image to upload
     * @param type How the data in the image argument should be interpreted.
     * @param unit The texture slot to be used to upload the image, not
     * important
     */
    public void updateTexImageData(Image img, Texture.Type type, int unit) {
        int texId = img.getId();
        GL gl = GLContext.getCurrentGL();
        if (texId == -1) {
            // create texture
            gl.glGenTextures(1, intBuf1);
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
            // No pregenerated mips available,
            // generate from base level if required
            if (!gl.isExtensionAvailable("GL_VERSION_3_0")) {
                if (gl.isGL2ES1()) {
                    gl.glTexParameteri(target, GL2ES1.GL_GENERATE_MIPMAP, GL.GL_TRUE);
                }
                img.setMipmapsGenerated(true);
            }
        } else {
            // Image already has mipmaps or no mipmap generation desired.
//          glTexParameteri(target, GL_TEXTURE_BASE_LEVEL, 0 );
            if (img.getMipMapSizes() != null) {
                if (gl.isGL2GL3()) {
                    gl.glTexParameteri(target, GL2GL3.GL_TEXTURE_MAX_LEVEL, img.getMipMapSizes().length - 1);
                }
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
        if (!gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two") && img.isNPOT()) {
            if (img.getData(0) == null) {
                throw new RendererException("non-power-of-2 framebuffer textures are not supported by the video hardware");
            } else {
                MipMapGenerator.resizeToPowerOf2(img);
            }
        }

        // Check if graphics card doesn't support multisample textures
        if (!gl.isExtensionAvailable("GL_ARB_texture_multisample")) {
            if (img.getMultiSamples() > 1) {
                throw new RendererException("Multisample textures not supported by graphics hardware");
            }
        }
        
        if (target == GL.GL_TEXTURE_CUBE_MAP) {
            // Check max texture size before upload
            if (img.getWidth() > maxCubeTexSize || img.getHeight() > maxCubeTexSize) {
                throw new RendererException("Cannot upload cubemap " + img + ". The maximum supported cubemap resolution is " + maxCubeTexSize);
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
                TextureUtil.uploadTexture(img, GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, 0, linearizeSrgbImages);
            }
        } else if (target == GL.GL_TEXTURE_2D_ARRAY) {
            List<ByteBuffer> data = img.getData();
            // -1 index specifies prepare data for 2D Array
            TextureUtil.uploadTexture(img, target, -1, 0, linearizeSrgbImages);
            for (int i = 0; i < data.size(); i++) {
                // upload each slice of 2D array in turn
                // this time with the appropriate index
                TextureUtil.uploadTexture(img, target, i, 0, linearizeSrgbImages);
            }
        } else {
            TextureUtil.uploadTexture(img, target, 0, 0, linearizeSrgbImages);
        }

        if (img.getMultiSamples() != imageSamples) {
            img.setMultiSamples(imageSamples);
        }

        if (gl.isExtensionAvailable("GL_VERSION_3_0")) {
            if (!img.hasMipmaps() && img.isGeneratedMipmapsRequired() && img.getData() != null) {
                // XXX: Required for ATI
                gl.glEnable(target);
                gl.glGenerateMipmap(target);
                gl.glDisable(target);
                img.setMipmapsGenerated(true);
            }
        }

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        GL gl = GLContext.getCurrentGL();
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
      setTexture(0, tex);
      TextureUtil.uploadSubTexture(pixels, convertTextureType(tex.getType(), tex.getImage().getMultiSamples(), -1), 0, x, y, linearizeSrgbImages);
    }

    public void clearTextureUnits() {
        /*GL gl = GLContext.getCurrentGL();
         IDList textureList = context.textureIndexList;
         Texture[] textures = context.boundTextures;
         for (int i = 0; i < textureList.oldLen; i++) {
         int idx = textureList.oldList[i];

         if (context.boundTextureUnit != idx) {
         gl.glActiveTexture(GL.GL_TEXTURE0 + idx);
         context.boundTextureUnit = idx;
         }
         gl.glDisable(convertTextureType(textures[idx].getType()));
         textures[idx] = null;
         }
         context.textureIndexList.copyNewToOld();*/
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);
            GL gl = GLContext.getCurrentGL();
            gl.glDeleteTextures(1, intBuf1);
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
                return GL2ES2.GL_STREAM_DRAW;
            default:
                throw new RuntimeException("Unknown usage type: " + usage);
        }
    }
    
    private int convertFormat(VertexBuffer.Format format) {
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
                return GL2ES2.GL_INT;
            case UnsignedInt:
                return GL.GL_UNSIGNED_INT;
//            case Half:
//                return NVHalfFloat.GL_HALF_FLOAT_NV;
//                return ARBHalfFloatVertex.GL_HALF_FLOAT;
            case Float:
                return GL.GL_FLOAT;
            case Double:
                return GL2GL3.GL_DOUBLE;
            default:
                throw new UnsupportedOperationException("Unknown buffer format.");

        }
    }

    public void updateBufferData(VertexBuffer vb) {
        GL gl = GLContext.getCurrentGL();
        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer
            gl.glGenBuffers(1, intBuf1);
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
        Buffer data = vb.getData();
        data.rewind();

        if (created || vb.hasDataSizeChanged()) {
            // upload data based on format
            gl.glBufferData(target, data.capacity() * vb.getFormat().getComponentSize(), data, usage);
        } else {
            gl.glBufferSubData(target, 0, data.capacity() * vb.getFormat().getComponentSize(), data);
        }

        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        GL gl = GLContext.getCurrentGL();
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);
            gl.glDeleteBuffers(1, intBuf1);
            vb.resetObject();

            //statistics.onDeleteVertexBuffer();
        }
    }
    
    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];
            GL gl = GLContext.getCurrentGL();
            gl.getGL2ES2().glDisableVertexAttribArray(idx);
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
            GL gl = GLContext.getCurrentGL();
            Attribute attrib = boundShader.getAttribute(vb.getBufferType());
            int loc = attrib.getLocation();
            if (loc == -1) {
                return; // not defined
            }
            if (loc == -2) {
                stringBuf.setLength(0);
                // JOGL 2.0 doesn't need a null terminated string
                stringBuf.append("in").append(vb.getBufferType().name());
                loc = gl.getGL2ES2().glGetAttribLocation(programId, stringBuf.toString());

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
                gl.getGL2ES2().glEnableVertexAttribArray(loc);
                //System.out.println("Enabled ATTRIB IDX: "+loc);
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

                gl.getGL2ES2().glVertexAttribPointer(loc,
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
        GL gl = GLContext.getCurrentGL();
        if (count > 1) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glDrawArraysInstanced(convertElementMode(mode), 0,
                    vertCount, count);
            }
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

        GL gl = GLContext.getCurrentGL();

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
                    elMode = convertElementMode(Mode.TriangleStrip);
                }
                int elementLength = elementLengths[i];

                if (useInstancing) {
                    if (gl.isGL2()) {
                        indexBuf.getData().position(curOffset);
                        indexBuf.getData().limit(curOffset + elementLength);

                        gl.getGL2().glDrawElementsInstanced(elMode,
                                elementLength,
                                fmt,
                                indexBuf.getData(),
                                count);
                    } else {
                        throw new IllegalArgumentException(
                                "instancing is not supported.");
                    }
                } else {
                    if (gl.isGL2GL3()) {
                        gl.getGL2GL3().glDrawRangeElements(elMode,
                                0,
                                vertCount,
                                elementLength,
                                fmt,
                                curOffset);
                    } else {
                        indexBuf.getData().position(curOffset);
                        gl.getGL2().glDrawElements(elMode, elementLength, fmt,
                                indexBuf.getData());
                    }
                }

                //FIXME check whether elSize is required
                curOffset += elementLength * elSize;
            }
        } else {
            if (useInstancing) {
                if (gl.isGL2()) {
                    gl.getGL2().glDrawElementsInstanced(convertElementMode(mesh.getMode()),
                            indexBuf.getData().limit(),
                            convertFormat(indexBuf.getFormat()),
                            indexBuf.getData(),
                            count);
                } else {
                    throw new IllegalArgumentException(
                            "instancing is not supported.");
                }
            } else {
                if (gl.isGL2GL3()) {
                    gl.getGL2GL3().glDrawRangeElements(convertElementMode(mesh.getMode()),
                            0,
                            vertCount,
                            indexBuf.getData().limit(),
                            convertFormat(indexBuf.getFormat()),
                            0);
                } else {
                    indexBuf.getData().rewind();
                    gl.glDrawElements(convertElementMode(mesh.getMode()),
                            indexBuf.getData().limit(),
                            convertFormat(indexBuf.getFormat()), 0);
                }
            }
        }
    }

    /**
     * *******************************************************************\ |*
     * Render Calls *|
    \********************************************************************
     */
    private int convertElementMode(Mesh.Mode mode) {
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

    public void updateVertexArray(Mesh mesh) {
        int id = mesh.getId();
        GL gl = GLContext.getCurrentGL();

        if (id == -1) {
            IntBuffer temp = intBuf1;
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glGenVertexArrays(1, temp);
            }
            id = temp.get(0);
            mesh.setId(id);
        }

        if (context.boundVertexArray != id) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glBindVertexArray(id);
            }
            context.boundVertexArray = id;
        }

        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
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
    
    private void renderMeshVertexArray(Mesh mesh, int lod, int count) {
        if (mesh.getId() == -1) {
            updateVertexArray(mesh);
        } else {
            // TODO: Check if it was updated
        }

        if (context.boundVertexArray != mesh.getId()) {
            GL gl = GLContext.getCurrentGL();
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glBindVertexArray(mesh.getId());
            }
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
    
    private void renderMeshDefault(Mesh mesh, int lod, int count) {
        VertexBuffer indices;

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

        GL gl = GLContext.getCurrentGL();
        if (context.pointSprite && mesh.getMode() != Mode.Points) {
            // XXX: Hack, disable point sprite mode if mesh not in point mode
            if (context.boundTextures[0] != null) {
                if (context.boundTextureUnit != 0) {
                    gl.glActiveTexture(GL.GL_TEXTURE0);
                    context.boundTextureUnit = 0;
                }
                if (gl.isGL2ES1()) {
                    gl.glDisable(GL2ES1.GL_POINT_SPRITE);
                }
                if (gl.isGL2GL3()) {
                    gl.glDisable(GL2GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
                }
                context.pointSprite = false;
            }
        }

        if (context.pointSize != mesh.getPointSize()) {
            if (gl.isGL2GL3()) {
                gl.getGL2GL3().glPointSize(mesh.getPointSize());
            }
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            gl.glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        statistics.onMeshDrawn(mesh, lod);
//        if (gl.isExtensionAvailable("GL_ARB_vertex_array_object")){
//            renderMeshVertexArray(mesh, lod, count);
//        }else{
        renderMeshDefault(mesh, lod, count);
//        }
    }

    public void setMainFrameBufferSrgb(boolean srgb) {
        //Gamma correction
        if(srgb && caps.contains(Caps.Srgb)){
            GLContext.getCurrentGL().glEnable(GL3.GL_FRAMEBUFFER_SRGB);
            logger.log(Level.FINER, "SRGB FrameBuffer enabled (Gamma Correction)");
        }else{
            GLContext.getCurrentGL().glDisable(GL3.GL_FRAMEBUFFER_SRGB);
        }         
    
    }

    public void setLinearizeSrgbImages(boolean linearize) {
        linearizeSrgbImages = linearize;
    }
}
