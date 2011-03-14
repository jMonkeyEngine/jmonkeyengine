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

package com.jme3.renderer.jogl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import com.jme3.glhelper.Helper;
import com.jme3.glhelper.jogl.JoglHelper;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.AbstractRenderer;
import com.jme3.renderer.Caps;
import com.jme3.renderer.IDList;
import com.jme3.renderer.RendererException;
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

/**
 * OpenGL renderer that relies on a specific OpenGL binding. It handles operations that can hardly be 
 * done in a binding-agnostic way (detection of available extensions, capabilities, manipulation of 
 * shader sources, ...) 
 *
 */
public class JoglRenderer extends AbstractRenderer {

    public JoglRenderer() {  	
    }

    public void initialize() {
    	helper = new JoglHelper();
        GL gl = GLContext.getCurrentGL();
        logger.log(Level.INFO, "Vendor: {0}", gl.glGetString(GL.GL_VENDOR));
        logger.log(Level.INFO, "Renderer: {0}", gl.glGetString(GL.GL_RENDERER));
        logger.log(Level.INFO, "Version: {0}", gl.glGetString(GL.GL_VERSION));

        applyRenderState(RenderState.DEFAULT);

        powerOf2 = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
        hardwareMips = gl.isExtensionAvailable("GL_SGIS_generate_mipmap");
        vbo = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");

        // TODO: use gl.glGetString(GL.GL_VERSION)
        /*if (ctxCaps.OpenGL20) {
            caps.add(Caps.OpenGL20);
        }
        if (ctxCaps.OpenGL21) {
            caps.add(Caps.OpenGL21);
        }
        if (ctxCaps.OpenGL30) {
            caps.add(Caps.OpenGL30);
        }*/

        final boolean glslSupported = gl.isExtensionAvailable("GL_ARB_shader_objects")
        && gl.isExtensionAvailable("GL_ARB_fragment_shader") && gl.isExtensionAvailable("GL_ARB_vertex_shader")
        && gl.isExtensionAvailable("GL_ARB_shading_language_100");
        String versionStr = glslSupported?gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION):null;
        if (versionStr == null || versionStr.equals("")) {
            glslVer = -1;
            // no, I need the support of low end graphics cards too
        }
        else {
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
            // N.B: do NOT force GLSL100 support
        }

        gl.glGetIntegerv(GL2GL3.GL_DRAW_BUFFER, intBuf1);
        initialDrawBuf = intBuf1.get(0);
        gl.glGetIntegerv(GL2GL3.GL_READ_BUFFER, intBuf1);
        initialReadBuf = intBuf1.get(0);

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

        boolean latc = gl.isExtensionAvailable("GL_EXT_texture_compression_latc");
        boolean atdc = gl.isExtensionAvailable("GL_ATI_texture_compression_3dc");
        if (latc || atdc) {
            caps.add(Caps.TextureCompressionLATC);
            if (atdc && !latc) {
                tdc = true;
            }
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

        if (gl.isExtensionAvailable("GL_ARB_draw_buffers")) {
            caps.add(Caps.FrameBufferMRT);
            gl.glGetIntegerv(GL2GL3.GL_MAX_DRAW_BUFFERS, intBuf16);
            maxMRTFBOAttachs = intBuf16.get(0);
            logger.log(Level.FINER, "FBO Max MRT renderbuffers: {0}", maxMRTFBOAttachs);
        }
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
        }
        framebufferBlit = gl.isExtensionAvailable("GL_EXT_framebuffer_blit");
        renderbufferStorageMultisample = gl.isExtensionAvailable("GL_EXT_framebuffer_multisample")
                && gl.isFunctionAvailable("glRenderbufferStorageMultisample");
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            bits = Helper.BufferBit.COLOR_BUFFER.getGLConstant();
        }
        if (depth) {
            bits |= Helper.BufferBit.DEPTH_BUFFER.getGLConstant();
        }
        if (stencil) {
            bits |= Helper.BufferBit.STENCIL_BUFFER.getGLConstant();
        }
        if (bits != 0) {
        	//TODO: use helper.clear(BufferBit bufferBit)
        	GLContext.getCurrentGL().glClear(bits);
        }
    }

    public void applyRenderState(RenderState state) {
        GL gl = GLContext.getCurrentGL();
        if (state.isWireframe() && !context.wireframe) {
            gl.getGL2().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            context.wireframe = true;
        }
        else if (!state.isWireframe() && context.wireframe) {
            gl.getGL2().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            context.wireframe = false;
        }
        if (state.isDepthTest() && !context.depthTestEnabled) {
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            context.depthTestEnabled = true;
        }
        else if (!state.isDepthTest() && context.depthTestEnabled) {
            gl.glDisable(GL.GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }
        if (state.isAlphaTest() && !context.alphaTestEnabled) {
            gl.glEnable(GL2ES1.GL_ALPHA_TEST);
            gl.getGL2().glAlphaFunc(GL.GL_GREATER, state.getAlphaFallOff());
            context.alphaTestEnabled = true;
        }
        else if (!state.isAlphaTest() && context.alphaTestEnabled) {
            gl.glDisable(GL2ES1.GL_ALPHA_TEST);
            context.alphaTestEnabled = false;
        }
        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            gl.glDepthMask(true);
            context.depthWriteEnabled = true;
        }
        else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            gl.glDepthMask(false);
            context.depthWriteEnabled = false;
        }
        if (state.isColorWrite() && !context.colorWriteEnabled) {
            gl.glColorMask(true, true, true, true);
            context.colorWriteEnabled = true;
        }
        else if (!state.isColorWrite() && context.colorWriteEnabled) {
            gl.glColorMask(false, false, false, false);
            context.colorWriteEnabled = false;
        }
        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(state.getPolyOffsetFactor(), state.getPolyOffsetUnits());
                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            }
            else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    gl.glPolygonOffset(state.getPolyOffsetFactor(), state.getPolyOffsetUnits());
                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        }
        else {
            if (context.polyOffsetEnabled) {
                disable(GL.GL_POLYGON_OFFSET_FILL);
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                disable(GL.GL_CULL_FACE);
            }
            else {
                enable(GL.GL_CULL_FACE);
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
                disable(GL.GL_BLEND);
            }
            else {
                enable(GL.GL_BLEND);
            }

            switch (state.getBlendMode()) {
                case Off:
                    break;
                case Additive:
                    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
                    break;
                case AlphaAdditive:
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
                    break;
                case Alpha:
                    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case Color:
                    gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_COLOR);
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

            context.blendMode = state.getBlendMode();
        }
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            enable(GL.GL_SCISSOR_TEST);
            context.clipRectEnabled = true;
        }
        helper.setScissor(x, y, width, height);
    }

    @Override
    protected void updateUniformVar(Shader shader, Uniform uniform) {
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
        GL gl = GLContext.getCurrentGL();
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                gl.getGL2().glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                gl.getGL2().glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                gl.getGL2().glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    gl.getGL2().glUniform4f(loc, c.r, c.g, c.b, c.a);
                }
                else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    gl.getGL2().glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                gl.getGL2().glUniform1i(loc, b.booleanValue() ? GL.GL_TRUE : GL.GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                gl.getGL2().glUniformMatrix3fv(loc, 1, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                gl.getGL2().glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2().glUniform1fv(loc, 1, fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2().glUniform2fv(loc, 1, fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2().glUniform3fv(loc, 1, fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2().glUniform4fv(loc, 1, fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                gl.getGL2().glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                gl.getGL2().glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: "
                        + uniform.getVarType());
        }
    }

    public void setLighting(LightList list) {
        if (glslVer == -1) {
            GL gl = GLContext.getCurrentGL();
            if (list == null || list.size() == 0) {
                // turn off lighting
                gl.glDisable(GLLightingFunc.GL_LIGHTING);
                return;
            }

            gl.glEnable(GLLightingFunc.GL_LIGHTING);
            gl.getGL2().glShadeModel(GLLightingFunc.GL_SMOOTH);

            float[] temp = new float[4];

            // reset model view to specify
            // light positions in world space
            // instead of model space
            // gl.glPushMatrix();
            // gl.glLoadIdentity();

            for (int i = 0; i < list.size() + 1; i++) {

                int lightId = GLLightingFunc.GL_LIGHT0 + i;

                if (list.size() <= i) {
                    // goes beyond the num lights we need
                    // disable it
                    gl.glDisable(lightId);
                    break;
                }

                Light l = list.get(i);

                if (!l.isEnabled()) {
                    gl.glDisable(lightId);
                    continue;
                }

                ColorRGBA color = l.getColor();
                color.toArray(temp);

                gl.glEnable(lightId);
                gl.getGL2().glLightfv(lightId, GLLightingFunc.GL_DIFFUSE, temp, 0);
                gl.getGL2().glLightfv(lightId, GLLightingFunc.GL_SPECULAR, temp, 0);

                ColorRGBA.Black.toArray(temp);
                gl.getGL2().glLightfv(lightId, GLLightingFunc.GL_AMBIENT, temp, 0);

                switch (l.getType()) {
                    case Directional:
                        DirectionalLight dl = (DirectionalLight) l;
                        dl.getDirection().toArray(temp);
                        temp[3] = 0f; // marks to GL its a directional light
                        gl.getGL2().glLightfv(lightId, GLLightingFunc.GL_POSITION, temp, 0);
                        break;
                    case Point:
                        PointLight pl = (PointLight) l;
                        pl.getPosition().toArray(temp);
                        temp[3] = 1f; // marks to GL its a point light
                        gl.getGL2().glLightfv(lightId, GLLightingFunc.GL_POSITION, temp, 0);
                        break;
                }

            }
            // restore modelview to original value
            // gl.glPopMatrix();
        }
    }

    public int convertShaderType(ShaderType type) {
        switch (type) {
            case Fragment:
                return GL2ES2.GL_FRAGMENT_SHADER;
            case Vertex:
                return GL2ES2.GL_VERTEX_SHADER;
                // case Geometry:
                // return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new RuntimeException("Unrecognized shader type.");
        }
    }

    @Override
    protected void deleteShader(ShaderSource source) {
        GL gl = GLContext.getCurrentGL();
        gl.getGL2().glDeleteShader(source.getId());
    }

    @Override
    public void updateShaderData(Shader shader) {
        if (glslVer != -1) {
            GL gl = GLContext.getCurrentGL();
            int id = shader.getId();
            boolean needRegister = false;
            if (id == -1) {
                // create program
                id = gl.getGL2().glCreateProgram();
                if (id <= 0) {
                    throw new RendererException(
                            "Invalid ID received when trying to create shader program.");
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
                gl.getGL2().glAttachShader(id, source.getId());
            }
            // link shaders to program
            gl.getGL2().glLinkProgram(id);

            gl.getGL2().glGetProgramiv(id, GL2ES2.GL_LINK_STATUS, intBuf1);
            boolean linkOK = intBuf1.get(0) == GL.GL_TRUE;
            String infoLog = null;

            if (VALIDATE_SHADER || !linkOK) {
                gl.getGL2().glGetProgramiv(id, GL2ES2.GL_INFO_LOG_LENGTH, intBuf1);
                int length = intBuf1.get(0);
                if (length > 3) {
                    // get infos
                    ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                    gl.getGL2().glGetProgramInfoLog(id, logBuf.limit(), intBuf1, logBuf);

                    // convert to string, etc
                    byte[] logBytes = new byte[length];
                    logBuf.get(logBytes, 0, length);
                    infoLog = new String(logBytes);
                }
            }

            if (linkOK) {
                if (infoLog != null) {
                    logger.log(Level.INFO, "shader link success. \n{0}", infoLog);
                }
                else {
                    logger.fine("shader link success");
                }
            }
            else {
                if (infoLog != null) {
                    logger.log(Level.WARNING, "shader link failure. \n{0}", infoLog);
                }
                else {
                    logger.warning("shader link failure");
                }
            }

            shader.clearUpdateNeeded();
            if (!linkOK) {
                // failure.. forget about everything
                shader.resetSources();
                shader.setUsable(false);
                deleteShader(shader);
            }
            else {
                shader.setUsable(true);
                if (needRegister) {
                    objManager.registerForCleanup(shader);
                    statistics.onNewShader();
                }
                else {
                    // OpenGL spec: uniform locations may change after re-link
                    resetUniformLocations(shader);
                }
            }
        }
    }

    public void updateShaderSourceData(ShaderSource source, String language) {
        if (glslVer != -1) {
            GL gl = GLContext.getCurrentGL();
            int id = source.getId();
            if (id == -1) {
                // create id
                id = gl.getGL2().glCreateShader(convertShaderType(source.getType()));
                if (id <= 0) {
                    throw new RendererException("Invalid ID received when trying to create shader.");
                }

                source.setId(id);
            }

            // upload shader source
            // merge the defines and source code
            byte[] versionData = new byte[] {};// "#version 140\n".getBytes();
            // versionData = "#define INSTANCING 1\n".getBytes();
            byte[] definesCodeData = source.getDefines().getBytes();
            byte[] sourceCodeData = source.getSource().getBytes();
            ByteBuffer codeBuf = BufferUtils.createByteBuffer(versionData.length
                    + definesCodeData.length + sourceCodeData.length);
            codeBuf.put(versionData);
            codeBuf.put(definesCodeData);
            codeBuf.put(sourceCodeData);
            codeBuf.flip();

            final byte[] array = new byte[codeBuf.limit()];
            codeBuf.get(array);
            gl.getGL2().glShaderSourceARB(id, 1, new String[] { new String(array) },
                    new int[] { array.length }, 0);
            gl.getGL2().glCompileShader(id);

            gl.getGL2().glGetShaderiv(id, GL2ES2.GL_COMPILE_STATUS, intBuf1);

            boolean compiledOK = intBuf1.get(0) == GL.GL_TRUE;
            String infoLog = null;

            if (VALIDATE_SHADER || !compiledOK) {
                // even if compile succeeded, check
                // log for warnings
                gl.getGL2().glGetShaderiv(id, GL2ES2.GL_INFO_LOG_LENGTH, intBuf1);
                int length = intBuf1.get(0);
                if (length > 3) {
                    // get infos
                    ByteBuffer logBuf = BufferUtils.createByteBuffer(length);
                    gl.getGL2().glGetShaderInfoLog(id, logBuf.limit(), intBuf1, logBuf);
                    byte[] logBytes = new byte[length];
                    logBuf.get(logBytes, 0, length);
                    // convert to string, etc
                    infoLog = new String(logBytes);
                }
            }

            if (compiledOK) {
                if (infoLog != null) {
                    logger.log(Level.INFO, "{0} compile success\n{1}",
                            new Object[] { source.getName(), infoLog });
                }
                else {
                    logger.log(Level.FINE, "{0} compile success", source.getName());
                }
            }
            else {
                if (infoLog != null) {
                    logger.log(Level.WARNING, "{0} compile error: {1}",
                            new Object[] { source.getName(), infoLog });
                }
                else {
                    logger.log(Level.WARNING, "{0} compile error: ?", source.getName());
                }
                logger.log(Level.WARNING, "{0}{1}",
                        new Object[] { source.getDefines(), source.getSource() });
            }

            source.clearUpdateNeeded();
            // only usable if compiled
            source.setUsable(compiledOK);
            if (!compiledOK) {
                // make sure to dispose id cause all program's
                // shaders will be cleared later.
                gl.getGL2().glDeleteShader(id);
            }
            else {
                // register for cleanup since the ID is usable
                objManager.registerForCleanup(source);
            }
        }
    }

    @Override
    protected boolean isShaderValid(Shader shader) {
        GL gl = GLContext.getCurrentGL();
        gl.getGL2().glValidateProgram(shader.getId());
        gl.getGL2().glGetProgramiv(shader.getId(), GL2ES2.GL_VALIDATE_STATUS, intBuf1);
        return intBuf1.get(0) == GL.GL_TRUE;
    }

    public void deleteShader(Shader shader) {
        if (glslVer != -1) {
            if (shader.getId() == -1) {
                logger.warning("Shader is not uploaded to GPU, cannot delete.");
                return;
            }
            GL gl = GLContext.getCurrentGL();
            for (ShaderSource source : shader.getSources()) {
                if (source.getId() != -1) {
                    gl.getGL2().glDetachShader(shader.getId(), source.getId());
                    // the next part is done by the GLObjectManager automatically
                    // glDeleteShader(source.getId());
                }
            }
            // kill all references so sources can be collected
            // if needed.
            shader.resetSources();
            gl.getGL2().glDeleteProgram(shader.getId());

            statistics.onDeleteShader();
        }
    }

    @Override
    protected void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
            int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GL gl = GLContext.getCurrentGL();
        gl.getGL2().glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask,
                filter);
    }

    public void setFrameBuffer(FrameBuffer fb) {
        if (lastFb == fb) {
            return;
        }

        GL gl = GLContext.getCurrentGL();
        if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                bindFramebuffer(0);
                statistics.onFrameBufferUse(null, true);

                context.boundFBO = 0;
            }
            // select back buffer
            if (context.boundDrawBuf != -1) {
                gl.getGL2().glDrawBuffer(initialDrawBuf);
                context.boundDrawBuf = -1;
            }
            if (context.boundReadBuf != -1) {
                gl.getGL2().glReadBuffer(initialReadBuf);
                context.boundReadBuf = -1;
            }

            lastFb = null;
        }
        else {
            if (fb.isUpdateNeeded()) {
                updateFrameBuffer(fb);
            }

            if (context.boundFBO != fb.getId()) {
                bindFramebuffer(fb.getId());
                statistics.onFrameBufferUse(fb, true);

                // update viewport to reflect framebuffer's resolution
                setViewPort(0, 0, fb.getWidth(), fb.getHeight());

                context.boundFBO = fb.getId();
            }
            else {
                statistics.onFrameBufferUse(fb, false);
            }
            if (fb.getNumColorBuffers() == 0) {
                // make sure to select NONE as draw buf
                // no color buffer attached. select NONE
                if (context.boundDrawBuf != -2) {
                    gl.getGL2().glDrawBuffer(GL.GL_NONE);
                    context.boundDrawBuf = -2;
                }
                if (context.boundReadBuf != -2) {
                    gl.getGL2().glReadBuffer(GL.GL_NONE);
                    context.boundReadBuf = -2;
                }
            }
            else {
                if (fb.isMultiTarget()) {
                    if (fb.getNumColorBuffers() > maxMRTFBOAttachs) {
                        throw new UnsupportedOperationException("Framebuffer has more"
                                + " targets than are supported" + " on the system!");
                    }

                    if (context.boundDrawBuf != 100 + fb.getNumColorBuffers()) {
                        intBuf16.clear();
                        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
                            intBuf16.put(GL.GL_COLOR_ATTACHMENT0 + i);
                        }

                        intBuf16.flip();
                        gl.getGL2().glDrawBuffers(intBuf16.limit(), intBuf16);
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                }
                else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (context.boundDrawBuf != rb.getSlot()) {
                        gl.getGL2().glDrawBuffer(GL.GL_COLOR_ATTACHMENT0 + rb.getSlot());
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
        }
        catch (IllegalStateException ex) {
            logger.log(Level.SEVERE, "Problem FBO:\n{0}", fb);
            throw ex;
        }
    }

    @Override
    protected int genFramebufferId() {
        GL gl = GLContext.getCurrentGL();
        gl.glGenFramebuffers(1, intBuf1);
        return intBuf1.get(0);
    }

    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == -100) {
            return GL.GL_DEPTH_ATTACHMENT;
        }
        else if (attachmentSlot < 0 || attachmentSlot >= 16) {
            throw new UnsupportedOperationException("Invalid FBO attachment slot: "
                    + attachmentSlot);
        }

        return GL.GL_COLOR_ATTACHMENT0 + attachmentSlot;
    }

    public void updateRenderTexture(FrameBuffer fb, RenderBuffer rb) {
        GL gl = GLContext.getCurrentGL();
        Texture tex = rb.getTexture();
        if (tex.isUpdateNeeded()) {
            updateTextureData(tex);
        }

        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType()), tex.getId(), 0);
    }

    @Override
    public void updateFrameBufferAttachment(FrameBuffer fb, RenderBuffer rb) {
        boolean needAttach;
        if (rb.getTexture() == null) {
            // if it hasn't been created yet, then attach is required.
            needAttach = rb.getId() == -1;
            updateRenderBuffer(fb, rb);
        }
        else {
            needAttach = false;
            updateRenderTexture(fb, rb);
        }
        if (needAttach) {
            GL gl = GLContext.getCurrentGL();
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, convertAttachmentSlot(rb.getSlot()),
                    GL.GL_RENDERBUFFER, rb.getId());
        }
    }

    @Override
    protected void checkFrameBufferError() {
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
            throw new UnsupportedOperationException("Resolution " + fb.getWidth() + ":"
                    + fb.getHeight() + " is not supported.");
        }

        if (fb.getSamples() > 0 && renderbufferStorageMultisample) {
            int samples = fb.getSamples();
            if (maxFBOSamples < samples) {
                samples = maxFBOSamples;
            }
            gl.getGL2()
                    .glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples,
                            TextureUtil.convertTextureFormat(rb.getFormat()), fb.getWidth(),
                            fb.getHeight());
        }
        else {
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER,
                    TextureUtil.convertTextureFormat(rb.getFormat()), fb.getWidth(), fb.getHeight());
        }
    }

    @Override
    protected void deleteFramebuffer() {
        GL gl = GLContext.getCurrentGL();
        gl.getGL2().glDeleteFramebuffers(1, intBuf1);
    }

    @Override
    protected void bindFramebuffer(int framebuffer) {
        GL gl = GLContext.getCurrentGL();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, framebuffer);
    }

    @Override
    protected void bindDrawFramebuffer(int framebuffer) {
        GL gl = GLContext.getCurrentGL();
        gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, framebuffer);
    }

    @Override
    protected void bindReadFramebuffer(int framebuffer) {
        GL gl = GLContext.getCurrentGL();
        gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, framebuffer);
    }

    @Override
    protected void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1.put(0, rb.getId());
        GL gl = GLContext.getCurrentGL();
        gl.getGL2().glDeleteRenderbuffers(1, intBuf1);
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
                gl.getGL2().glReadBuffer(GL.GL_COLOR_ATTACHMENT0 + rb.getSlot());
                context.boundReadBuf = rb.getSlot();
            }
        }
        else {
            setFrameBuffer(null);
        }
        gl.glReadPixels(vpX, vpY, vpW, vpH, GL2GL3.GL_BGRA, GL.GL_UNSIGNED_BYTE, byteBuf);
    }

    @Override
    protected int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return GL.GL_TEXTURE_2D;
            case TwoDimensionalArray:
                return GL.GL_TEXTURE_2D_ARRAY;
            case ThreeDimensional:
                return GL2GL3.GL_TEXTURE_3D;
            case CubeMap:
                return GL.GL_TEXTURE_CUBE_MAP;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
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

    public void updateTextureData(Texture tex) {
        int texId = tex.getId();
        GL gl = GLContext.getCurrentGL();
        if (texId == -1) {
            // create texture
            gl.glGenTextures(1, intBuf1);
            texId = intBuf1.get(0);
            tex.setId(texId);
            objManager.registerForCleanup(tex);
        }

        // bind texture
        int target = convertTextureType(tex.getType());
        if (context.boundTextures[0] != tex) {
            if (context.boundTextureUnit != 0) {
                setActiveTexture(GL.GL_TEXTURE0);
                context.boundTextureUnit = 0;
            }

            bindTexture(target, texId);
            context.boundTextures[0] = tex;
        }

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);

        // repeat modes
        switch (tex.getType()) {
            case ThreeDimensional:
            case CubeMap:
                gl.glTexParameteri(target, GL2GL3.GL_TEXTURE_WRAP_R,
                        convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T,
                        convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
                // case OneDimensional:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S,
                        convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        Image img = tex.getImage();
        if (img != null) {
            boolean generateMips = false;
            if (!img.hasMipmaps() && tex.getMinFilter().usesMipMapLevels()) {
                // No pregenerated mips available,
                // generate from base level if required
                if (hardwareMips) {
                    gl.glTexParameteri(target, GL2ES1.GL_GENERATE_MIPMAP, GL.GL_TRUE);
                }
                else {
                    generateMips = true;
                }
            }

            TextureUtil.uploadTexture(gl, img, tex.getImageDataIndex(), generateMips, powerOf2);
        }

        tex.clearUpdateNeeded();
    }

    private void checkTexturingUsed() {
        IDList textureList = context.textureIndexList;
        GL gl = GLContext.getCurrentGL();
        // old mesh used texturing, new mesh doesn't use it
        // should actually go through entire oldLen and
        // disable texturing for each unit.. but that's for later.
        if (textureList.oldLen > 0 && textureList.newLen == 0) {
            gl.glDisable(GL.GL_TEXTURE_2D);
        }
    }

    public void setTexture(int unit, Texture tex) {       
        if (tex.isUpdateNeeded()) {
            updateTextureData(tex);
        }

        int texId = tex.getId();
        assert texId != -1;

        Texture[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType());
        if (!context.textureIndexList.moveToNew(unit)) {
            if (context.boundTextureUnit != unit) {
            	setActiveTexture(GL.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            enable(type);
        }

        if (textures[unit] != tex) {
            if (context.boundTextureUnit != unit) {
                setActiveTexture(GL.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            bindTexture(type, texId);
            textures[unit] = tex;
        }
    }
    
    @Override
    protected void enable(int cap){
    	GLContext.getCurrentGL().glEnable(cap);
    }
    
    @Override
    protected void disable(int cap){
    	GLContext.getCurrentGL().glDisable(cap);
    }
    
    @Override
    protected void setActiveTexture(int unit){
    	GLContext.getCurrentGL().glActiveTexture(unit);
    }
    
    @Override
    protected void bindTexture(int type,int id){
    	GLContext.getCurrentGL().glBindTexture(type, id);
    } 
    
    protected void deleteTexture(){
    	GL gl = GLContext.getCurrentGL();
        gl.glDeleteTextures(1, intBuf1);
    }

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

    @Override
    protected int convertFormat(Format format) {
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
                return GL2ES2.GL_UNSIGNED_INT;
            case Half:
                return GL.GL_HALF_FLOAT;
            case Float:
                return GL.GL_FLOAT;
            case Double:
                return GL2GL3.GL_DOUBLE;
            default:
                throw new RuntimeException("Unknown buffer format.");

        }
    }

    public void updateBufferData(VertexBuffer vb) {
        GL gl = GLContext.getCurrentGL();
        int bufId = vb.getId();
        if (bufId == -1) {
            // create buffer
            gl.glGenBuffers(1, intBuf1);
            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerForCleanup(vb);
        }

        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = GL.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
            }
        }
        else {
            target = GL.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
            }
        }

        int usage = convertUsage(vb.getUsage());
        Buffer data = vb.getData();
        data.rewind();

        gl.glBufferData(target, data.capacity() * vb.getFormat().getComponentSize(), data, usage);

        vb.clearUpdateNeeded();
    }
    
    @Override
    protected void deleteBuffer(){
    	GLContext.getCurrentGL().glDeleteBuffers(1, intBuf1);
    }

    @Override
    protected int convertArrayType(VertexBuffer.Type type) {
        switch (type) {
            case Position:
                return GLPointerFunc.GL_VERTEX_ARRAY;
            case Normal:
                return GLPointerFunc.GL_NORMAL_ARRAY;
            case TexCoord:
                return GLPointerFunc.GL_TEXTURE_COORD_ARRAY;
            case Color:
                return GLPointerFunc.GL_COLOR_ARRAY;
            default:
                return -1; // unsupported
        }
    }

    @Override
    protected int convertVertexFormat(VertexBuffer.Format fmt) {
        switch (fmt) {
            case Byte:
                return GL.GL_BYTE;
            case Double:
                return GL2GL3.GL_DOUBLE;
            case Float:
                return GL.GL_FLOAT;
            case Half:
                return GL.GL_HALF_FLOAT;
            case Int:
                return GL2ES2.GL_INT;
            case Short:
                return GL.GL_SHORT;
            case UnsignedByte:
                return GL.GL_UNSIGNED_BYTE;
            case UnsignedInt:
                return GL2ES2.GL_UNSIGNED_INT;
            case UnsignedShort:
                return GL.GL_UNSIGNED_SHORT;
            default:
                throw new UnsupportedOperationException("Unrecognized vertex format: " + fmt);
        }
    }

    @Override
    protected int convertElementMode(Mesh.Mode mode) {
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

    @Override
    protected void setVertexAttribVBO(VertexBuffer vb, VertexBuffer idb) {
        GL gl = GLContext.getCurrentGL();
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1) {
            return; // unsupported
        }

        if (vb.isUpdateNeeded() && idb == null) {
            updateBufferData(vb);
        }

        int bufId = idb != null ? idb.getId() : vb.getId();
        if (context.boundArrayVBO != bufId) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId);
            context.boundArrayVBO = bufId;
        }

        gl.getGL2().glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal) {
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled) {
                gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                context.normalizeEnabled = true;
            }
            else if (!vb.isNormalized() && context.normalizeEnabled) {
                gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                context.normalizeEnabled = false;
            }
        }

        int comps = vb.getNumComponents();
        int type = convertVertexFormat(vb.getFormat());

        switch (vb.getBufferType()) {
            case Position:
                gl.getGL2().glVertexPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case Normal:
                gl.getGL2().glNormalPointer(type, vb.getStride(), vb.getOffset());
                break;
            case Color:
                gl.getGL2().glColorPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case TexCoord:
                gl.getGL2().glTexCoordPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
        }
    }

    @Override
    protected void bindElementArrayBuffer(int buffer) {
        GL gl = GLContext.getCurrentGL();
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, buffer);
    }

    @Override
    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            throw new IllegalArgumentException(
                    "Index buffers not allowed to be set to vertex attrib");
        }

        if (vb.isUpdateNeeded() && idb == null) {
            updateBufferData(vb);
        }

        int programId = context.boundShaderProgram;
        GL gl = GLContext.getCurrentGL();
        if (glslVer != -1 && programId > 0) {
            Attribute attrib = boundShader.getAttribute(vb.getBufferType().name());
            int loc = attrib.getLocation();
            if (loc == -1) {
                return; // not defined
            }

            if (loc == -2) {
                stringBuf.setLength(0);
                stringBuf.append("in").append(vb.getBufferType().name()).append('\0');
                updateNameBuffer();
                loc = gl.getGL2().glGetAttribLocation(programId, stringBuf.toString());

                // not really the name of it in the shader (inPosition\0) but
                // the internal name of the enum (Position).
                if (loc < 0) {
                    attrib.setLocation(-1);
                    return; // not available in shader.
                }
                else {
                    attrib.setLocation(loc);
                }
            }

            VertexBuffer[] attribs = context.boundAttribs;
            if (!context.attribIndexList.moveToNew(loc)) {
                gl.getGL2().glEnableVertexAttribArrayARB(loc);
                // System.out.println("Enabled ATTRIB IDX: "+loc);
            }
            if (attribs[loc] != vb) {
                // NOTE: Use id from interleaved buffer if specified
                int bufId = idb != null ? idb.getId() : vb.getId();
                assert bufId != -1;
                if (context.boundArrayVBO != bufId) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufId);
                    context.boundArrayVBO = bufId;
                }

                gl.getGL2().glVertexAttribPointer(loc, vb.getNumComponents(),
                        convertFormat(vb.getFormat()), vb.isNormalized(), vb.getStride(),
                        vb.getOffset());

                attribs[loc] = vb;
            }
        }
        else {
            int arrayType = convertArrayType(vb.getBufferType());
            if (arrayType == -1) {
                return; // unsupported
            }

            gl.getGL2().glEnableClientState(arrayType);
            context.boundAttribs[vb.getBufferType().ordinal()] = vb;

            if (vb.getBufferType() == Type.Normal) {
                // normalize if requested
                if (vb.isNormalized() && !context.normalizeEnabled) {
                    gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                    context.normalizeEnabled = true;
                }
                else if (!vb.isNormalized() && context.normalizeEnabled) {
                    gl.glDisable(GLLightingFunc.GL_NORMALIZE);
                    context.normalizeEnabled = false;
                }
            }

            // NOTE: Use data from interleaved buffer if specified
            Buffer data = idb != null ? idb.getData() : vb.getData();
            int comps = vb.getNumComponents();
            int type = convertVertexFormat(vb.getFormat());
            data.clear();
            data.position(vb.getOffset());

            switch (vb.getBufferType()) {
                case Position:
                    gl.getGL2().glVertexPointer(comps, type, vb.getStride(), data);
                    break;
                case Normal:
                    gl.getGL2().glNormalPointer(type, vb.getStride(), data);
                    break;
                case Color:
                    gl.getGL2().glColorPointer(comps, type, vb.getStride(), data);
                    break;
                case TexCoord:
                    gl.getGL2().glTexCoordPointer(comps, type, vb.getStride(), data);
                    break;
            }
        }
    }
    
    @Override
    protected void disableClientState(int cap){
    	GLContext.getCurrentGL().getGL2().glDisableClientState(cap);
    }

    @Override
    protected void drawElements(Mode mode, int count, Format format, Buffer indices) {
        GLContext.getCurrentGL().glDrawElements(convertElementMode(mode), count, convertVertexFormat(format), indices);
    }

    @Override
    protected void drawRangeElements(Mode mode, int start, int end, int count, Format format,
            long indices_buffer_offset) {
        GLContext.getCurrentGL().getGL2().glDrawRangeElements(convertElementMode(mode), start, end, count,
                convertVertexFormat(format), indices_buffer_offset);
    }

    @Override
    protected void drawElementsInstanced(Mode mode, int indices_count, Format format,
            long indices_buffer_offset, int primcount) {
        // GL gl = GLContext.getCurrentGL();
        // FIXME: not yet in JOGL
        // gl.getGL2GL3().glDrawElementsInstanced(convertElementMode(mode), indices_count,
        // convertVertexFormat(format), indices_buffer_offset, primcount);
    }

    @Override
    protected void drawArrays(Mesh mesh) {
        GLContext.getCurrentGL().glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
    }

    private void updateDisplayList(Mesh mesh) {
        GL gl = GLContext.getCurrentGL();
        if (mesh.getId() != -1) {
            // delete list first
            gl.getGL2().glDeleteLists(mesh.getId(), mesh.getId());
            mesh.setId(-1);
        }

        // create new display list
        // first set state to NULL
        applyRenderState(RenderState.NULL);

        // disable lighting
        setLighting(null);

        int id = gl.getGL2().glGenLists(1);
        mesh.setId(id);
        gl.getGL2().glNewList(id, GL2.GL_COMPILE);
        renderMeshDefault(mesh, 0, 1);
        gl.getGL2().glEndList();
    }

    private void renderMeshDisplayList(Mesh mesh) {
        if (mesh.getId() == -1) {
            updateDisplayList(mesh);
        }
        GLContext.getCurrentGL().getGL2().glCallList(mesh.getId());
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
        GL gl = GLContext.getCurrentGL();
        if (context.pointSize != mesh.getPointSize()) {
            gl.getGL2().glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            gl.glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        checkTexturingUsed();

        if (vbo) {
            renderMeshVBO(mesh, lod, count);
        }
        else {
            boolean dynamic = false;
            if (mesh.getNumLodLevels() == 0) {
                IntMap<VertexBuffer> bufs = mesh.getBuffers();
                for (Entry<VertexBuffer> entry : bufs) {
                    if (entry.getValue().getUsage() != VertexBuffer.Usage.Static) {
                        dynamic = true;
                        break;
                    }
                }
            }
            else {
                dynamic = true;
            }

            if (!dynamic) {
                // dealing with a static object, generate display list
                renderMeshDisplayList(mesh);
            }
            else {
                renderMeshDefault(mesh, lod, count);
            }
        }
    }

}
