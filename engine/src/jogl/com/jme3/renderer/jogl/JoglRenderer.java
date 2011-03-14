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

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.GLObjectManager;
import com.jme3.renderer.IDList;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.Statistics;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;

public class JoglRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());
    protected Statistics statistics = new Statistics();
    protected Matrix4f worldMatrix = new Matrix4f();
    protected Matrix4f viewMatrix = new Matrix4f();
    protected Matrix4f projMatrix = new Matrix4f();
    protected FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);
    protected IntBuffer ib1 = BufferUtils.createIntBuffer(1);
    protected GL gl;
    private RenderContext context = new RenderContext();
    private GLObjectManager objManager = new GLObjectManager();
    private EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    private boolean powerOf2 = false;
    private boolean hardwareMips = false;
    private boolean vbo = false;
    private int vpX, vpY, vpW, vpH;

    public JoglRenderer(GL gl) {
        this.gl = gl;
    }

    public void setGL(GL gl) {
        this.gl = gl;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void initialize() {
        logger.log(Level.INFO, "Vendor: {0}", gl.glGetString(gl.GL_VENDOR));
        logger.log(Level.INFO, "Renderer: {0}", gl.glGetString(gl.GL_RENDERER));
        logger.log(Level.INFO, "Version: {0}", gl.glGetString(gl.GL_VERSION));

        applyRenderState(RenderState.DEFAULT);

        powerOf2 = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
        hardwareMips = gl.isExtensionAvailable("GL_SGIS_generate_mipmap");
        vbo = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
    }

    public EnumSet<Caps> getCaps() {
        return caps;
    }

    public void setBackgroundColor(ColorRGBA color) {
        gl.glClearColor(color.r, color.g, color.b, color.a);
    }

    public void cleanup() {
        objManager.deleteAllObjects(this);
    }

    public void resetGLObjects() {
        objManager.resetObjects();
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            bits = gl.GL_COLOR_BUFFER_BIT;
        }
        if (depth) {
            bits |= gl.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            bits |= gl.GL_STENCIL_BUFFER_BIT;
        }
        if (bits != 0) {
            gl.glClear(bits);
        }
    }

    public void applyRenderState(RenderState state) {

        if (state.isWireframe() && !context.wireframe) {
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
            context.wireframe = true;
        } else if (!state.isWireframe() && context.wireframe) {
            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
            context.wireframe = false;
        }


        if (state.isDepthTest() && !context.depthTestEnabled) {
            gl.glEnable(gl.GL_DEPTH_TEST);
            gl.glDepthFunc(gl.GL_LEQUAL);
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            gl.glDisable(gl.GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }
        if (state.isAlphaTest() && !context.alphaTestEnabled) {
            gl.glEnable(gl.GL_ALPHA_TEST);
            gl.glAlphaFunc(gl.GL_GREATER, state.getAlphaFallOff());
            context.alphaTestEnabled = true;
        } else if (!state.isAlphaTest() && context.alphaTestEnabled) {
            gl.glDisable(gl.GL_ALPHA_TEST);
            context.alphaTestEnabled = false;
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
            gl.glEnable(GL.GL_POINT_SPRITE);
            gl.glTexEnvi(GL.GL_POINT_SPRITE, GL.GL_COORD_REPLACE, GL.GL_TRUE);
            gl.glPointParameterf(GL.GL_POINT_SIZE_MIN, 1.0f);
        } else if (!state.isPointSprite() && context.pointSprite) {
            gl.glDisable(GL.GL_POINT_SPRITE);
        }
        if (state.isPolyOffset()) {
            if (!context.polyOffsetEnabled) {
                gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
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
                gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                gl.glDisable(gl.GL_CULL_FACE);
            } else {
                gl.glEnable(gl.GL_CULL_FACE);
            }

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    gl.glCullFace(gl.GL_BACK);
                    break;
                case Front:
                    gl.glCullFace(gl.GL_FRONT);
                    break;
                case FrontAndBack:
                    gl.glCullFace(gl.GL_FRONT_AND_BACK);
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }
        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                gl.glDisable(gl.GL_BLEND);
            } else {
                gl.glEnable(gl.GL_BLEND);
            }

            switch (state.getBlendMode()) {
                case Off:
                    break;
                case Additive:
                    gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE);
                    break;
                case AlphaAdditive:
                    gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE);
                    break;
                case Alpha:
                    gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case Color:
                    gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE_MINUS_SRC_COLOR);
                    break;
                case PremultAlpha:
                    gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE_MINUS_SRC_ALPHA);
                    break;
                case Modulate:
                    gl.glBlendFunc(gl.GL_DST_COLOR, gl.GL_ZERO);
                    break;
                case ModulateX2:
                    gl.glBlendFunc(gl.GL_DST_COLOR, gl.GL_SRC_COLOR);
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized blend mode: "
                            + state.getBlendMode());
            }

            context.blendMode = state.getBlendMode();
        }

    }

    public void onFrame() {
        objManager.deleteUnused(this);
    }

    public void setDepthRange(float start, float end) {
        gl.glDepthRange(start, end);
    }

    public void setViewPort(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
        vpX = x;
        vpY = y;
        vpW = width;
        vpH = height;
    }

    public void setClipRect(int x, int y, int width, int height) {
        if (!context.clipRectEnabled) {
            gl.glEnable(gl.GL_SCISSOR_TEST);
            context.clipRectEnabled = true;
        }
        gl.glScissor(x, y, width, height);
    }

    public void clearClipRect() {
        if (context.clipRectEnabled) {
            gl.glDisable(gl.GL_SCISSOR_TEST);
            context.clipRectEnabled = false;
        }
    }

    private FloatBuffer storeMatrix(Matrix4f matrix, FloatBuffer store) {
        store.rewind();
        matrix.fillFloatBuffer(store, true);
        store.rewind();
        return store;
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        this.viewMatrix.set(viewMatrix);
        this.projMatrix.set(projMatrix);

        if (context.matrixMode != gl.GL_PROJECTION) {
            gl.glMatrixMode(gl.GL_PROJECTION);
            context.matrixMode = gl.GL_PROJECTION;
        }

        gl.glLoadMatrixf(storeMatrix(projMatrix, fb16));
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
        this.worldMatrix.set(worldMatrix);

        if (context.matrixMode != gl.GL_MODELVIEW) {
            gl.glMatrixMode(gl.GL_MODELVIEW);
            context.matrixMode = gl.GL_MODELVIEW;
        }

        gl.glLoadMatrixf(storeMatrix(viewMatrix, fb16));
        gl.glMultMatrixf(storeMatrix(worldMatrix, fb16));
    }

    public void setLighting(LightList list) {
        if (list == null || list.size() == 0) {
            // turn off lighting
            gl.glDisable(gl.GL_LIGHTING);
            return;
        }

        gl.glEnable(gl.GL_LIGHTING);
        gl.glShadeModel(gl.GL_SMOOTH);

        float[] temp = new float[4];

        // reset model view to specify
        // light positions in world space
        // instead of model space
//        gl.glPushMatrix();
//        gl.glLoadIdentity();

        for (int i = 0; i < list.size() + 1; i++) {

            int lightId = gl.GL_LIGHT0 + i;

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
            gl.glLightfv(lightId, gl.GL_DIFFUSE, temp, 0);
            gl.glLightfv(lightId, gl.GL_SPECULAR, temp, 0);

            ColorRGBA.Black.toArray(temp);
            gl.glLightfv(lightId, gl.GL_AMBIENT, temp, 0);

            switch (l.getType()) {
                case Directional:
                    DirectionalLight dl = (DirectionalLight) l;
                    dl.getDirection().toArray(temp);
                    temp[3] = 0f; // marks to GL its a directional light
                    gl.glLightfv(lightId, gl.GL_POSITION, temp, 0);
                    break;
                case Point:
                    PointLight pl = (PointLight) l;
                    pl.getPosition().toArray(temp);
                    temp[3] = 1f; // marks to GL its a point light
                    gl.glLightfv(lightId, gl.GL_POSITION, temp, 0);
                    break;
            }

        }

        // restore modelview to original value
//        gl.glPopMatrix();
    }

    public void deleteShaderSource(ShaderSource source) {
    }

    public void setShader(Shader shader) {
    }

    public void deleteShader(Shader shader) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    public void setFrameBuffer(FrameBuffer fb) {
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        if (fb != null) {
            return;
        }

        gl.glReadPixels(vpX, vpY, vpW, vpH, gl.GL_BGRA, gl.GL_UNSIGNED_BYTE, byteBuf);
    }

    private int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return gl.GL_TEXTURE_2D;
            case TwoDimensionalArray:
                return gl.GL_TEXTURE_2D_ARRAY_EXT;
            case ThreeDimensional:
                return gl.GL_TEXTURE_3D;
            case CubeMap:
                return gl.GL_TEXTURE_CUBE_MAP;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return gl.GL_LINEAR;
            case Nearest:
                return gl.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter) {
        switch (filter) {
            case Trilinear:
                return gl.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return gl.GL_LINEAR_MIPMAP_NEAREST;
            case NearestLinearMipMap:
                return gl.GL_NEAREST_MIPMAP_LINEAR;
            case NearestNearestMipMap:
                return gl.GL_NEAREST_MIPMAP_NEAREST;
            case BilinearNoMipMaps:
                return gl.GL_LINEAR;
            case NearestNoMipMaps:
                return gl.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown min filter: " + filter);
        }
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
                return gl.GL_CLAMP_TO_BORDER;
            case Clamp:
                return gl.GL_CLAMP;
            case EdgeClamp:
                return gl.GL_CLAMP_TO_EDGE;
            case Repeat:
                return gl.GL_REPEAT;
            case MirroredRepeat:
                return gl.GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    private void setupTextureParams(Texture tex) {
        int target = convertTextureType(tex.getType());

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        gl.glTexParameteri(target, gl.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, gl.GL_TEXTURE_MAG_FILTER, magFilter);

        // repeat modes
        switch (tex.getType()) {
            case ThreeDimensional:
            case CubeMap:
                gl.glTexParameteri(target, gl.GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
                gl.glTexParameteri(target, gl.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                gl.glTexParameteri(target, gl.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }

        // R to Texture compare mode
        if (tex.getShadowCompareMode() != Texture.ShadowCompareMode.Off) {
            gl.glTexParameteri(target, GL.GL_TEXTURE_COMPARE_MODE, GL.GL_COMPARE_R_TO_TEXTURE);
            gl.glTexParameteri(target, GL.GL_DEPTH_TEXTURE_MODE, GL.GL_INTENSITY);
            if (tex.getShadowCompareMode() == Texture.ShadowCompareMode.GreaterOrEqual) {
                gl.glTexParameteri(target, GL.GL_TEXTURE_COMPARE_FUNC, GL.GL_GEQUAL);
            } else {
                gl.glTexParameteri(target, GL.GL_TEXTURE_COMPARE_FUNC, GL.GL_LEQUAL);
            }
        }
    }

    public void updateTexImageData(Image image, Texture.Type type, boolean mips) {
        int texId = image.getId();
        if (texId == -1) {
            // create texture
            gl.glGenTextures(1, ib1);
            texId = ib1.get(0);
            image.setId(texId);
            objManager.registerForCleanup(image);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type);
        if (context.boundTextures[0] != image) {
            if (context.boundTextureUnit != 0) {
                gl.glActiveTexture(gl.GL_TEXTURE0);
                context.boundTextureUnit = 0;
            }

            gl.glBindTexture(target, texId);
            context.boundTextures[0] = image;
        }

        boolean generateMips = false;
        if (!image.hasMipmaps() && mips) {
            // No pregenerated mips available,
            // generate from base level if required
            if (hardwareMips) {
                gl.glTexParameteri(target, GL.GL_GENERATE_MIPMAP, gl.GL_TRUE);
            } else {
                generateMips = true;
            }
        }

        TextureUtil.uploadTexture(gl, image, 0, generateMips, powerOf2);


        image.clearUpdateNeeded();
    }

    private void checkTexturingUsed() {
        IDList textureList = context.textureIndexList;
        // old mesh used texturing, new mesh doesn't use it
        // should actually go through entire oldLen and
        // disable texturing for each unit.. but that's for later.
        if (textureList.oldLen > 0 && textureList.newLen == 0) {
            gl.glDisable(gl.GL_TEXTURE_2D);
        }
    }

    public void setTexture(int unit, Texture tex) {
        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels());
        }

        int texId = image.getId();
        assert texId != -1;

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType());
        if (!context.textureIndexList.moveToNew(unit)) {
            if (context.boundTextureUnit != unit) {
                gl.glActiveTexture(gl.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            gl.glEnable(type);
        }

        if (textures[unit] != image) {
            if (context.boundTextureUnit != unit) {
                gl.glActiveTexture(gl.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            gl.glBindTexture(type, texId);
            textures[unit] = image;
        }
    }

    public void clearTextureUnits() {
        IDList textureList = context.textureIndexList;
        Image[] textures = context.boundTextures;
        for (int i = 0; i < textureList.oldLen; i++) {
            int idx = textureList.oldList[i];

            if (context.boundTextureUnit != idx) {
                gl.glActiveTexture(gl.GL_TEXTURE0 + idx);
                context.boundTextureUnit = idx;
            }
            // XXX: Uncomment me
            //gl.glDisable(convertTextureType(textures[idx].getType()));
            textures[idx] = null;
        }
        context.textureIndexList.copyNewToOld();
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            ib1.put(0, texId);
            ib1.position(0).limit(1);
            gl.glDeleteTextures(1, ib1);
            image.resetObject();
        }
    }

    private int convertUsage(Usage usage) {
        switch (usage) {
            case Static:
                return gl.GL_STATIC_DRAW;
            case Dynamic:
                return gl.GL_DYNAMIC_DRAW;
            case Stream:
                return gl.GL_STREAM_DRAW;
            default:
                throw new RuntimeException("Unknown usage type: " + usage);
        }
    }

    public void updateBufferData(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId == -1) {
            // create buffer
            gl.glGenBuffers(1, ib1);
            bufId = ib1.get(0);
            vb.setId(bufId);
            objManager.registerForCleanup(vb);
        }

        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = gl.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
            }
        } else {
            target = gl.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                gl.glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
            }
        }

        int usage = convertUsage(vb.getUsage());
        Buffer data = vb.getData();
        data.rewind();

        gl.glBufferData(target,
                data.capacity() * vb.getFormat().getComponentSize(),
                data,
                usage);

        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            ib1.put(0, bufId);
            ib1.position(0).limit(1);
            gl.glDeleteBuffers(1, ib1);
            vb.resetObject();
        }
    }

    private int convertArrayType(VertexBuffer.Type type) {
        switch (type) {
            case Position:
                return gl.GL_VERTEX_ARRAY;
            case Normal:
                return gl.GL_NORMAL_ARRAY;
            case TexCoord:
                return gl.GL_TEXTURE_COORD_ARRAY;
            case Color:
                return gl.GL_COLOR_ARRAY;
            default:
                return -1; // unsupported
        }
    }

    private int convertVertexFormat(VertexBuffer.Format fmt) {
        switch (fmt) {
            case Byte:
                return gl.GL_BYTE;
            case Double:
                return gl.GL_DOUBLE;
            case Float:
                return gl.GL_FLOAT;
            case Half:
                return gl.GL_HALF_FLOAT_ARB;
            case Int:
                return gl.GL_INT;
            case Short:
                return gl.GL_SHORT;
            case UnsignedByte:
                return gl.GL_UNSIGNED_BYTE;
            case UnsignedInt:
                return gl.GL_UNSIGNED_INT;
            case UnsignedShort:
                return gl.GL_UNSIGNED_SHORT;
            default:
                throw new UnsupportedOperationException("Unrecognized vertex format: " + fmt);
        }
    }

    private int convertElementMode(Mesh.Mode mode) {
        switch (mode) {
            case Points:
                return gl.GL_POINTS;
            case Lines:
                return gl.GL_LINES;
            case LineLoop:
                return gl.GL_LINE_LOOP;
            case LineStrip:
                return gl.GL_LINE_STRIP;
            case Triangles:
                return gl.GL_TRIANGLES;
            case TriangleFan:
                return gl.GL_TRIANGLE_FAN;
            case TriangleStrip:
                return gl.GL_TRIANGLE_STRIP;
            default:
                throw new UnsupportedOperationException("Unrecognized mesh mode: " + mode);
        }
    }

    private void setVertexAttribVBO(VertexBuffer vb, VertexBuffer idb) {
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1) {
            return; // unsupported
        }
        if (vb.isUpdateNeeded() && idb == null) {
            updateBufferData(vb);
        }

        int bufId = idb != null ? idb.getId() : vb.getId();
        if (context.boundArrayVBO != bufId) {
            gl.glBindBuffer(gl.GL_ARRAY_BUFFER, bufId);
            context.boundArrayVBO = bufId;
        }

        gl.glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal) {
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled) {
                gl.glEnable(gl.GL_NORMALIZE);
                context.normalizeEnabled = true;
            } else if (!vb.isNormalized() && context.normalizeEnabled) {
                gl.glDisable(gl.GL_NORMALIZE);
                context.normalizeEnabled = false;
            }
        }

        int comps = vb.getNumComponents();
        int type = convertVertexFormat(vb.getFormat());

        switch (vb.getBufferType()) {
            case Position:
                gl.glVertexPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case Normal:
                gl.glNormalPointer(type, vb.getStride(), vb.getOffset());
                break;
            case Color:
                gl.glColorPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case TexCoord:
                gl.glTexCoordPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
        }
    }

    private void drawTriangleListVBO(VertexBuffer indexBuf, Mesh mesh, int count) {
        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        if (indexBuf.isUpdateNeeded()) {
            updateBufferData(indexBuf);
        }

        int bufId = indexBuf.getId();
        assert bufId != -1;

        if (context.boundElementArrayVBO != bufId) {
            gl.glBindBuffer(gl.GL_ELEMENT_ARRAY_BUFFER, bufId);
            context.boundElementArrayVBO = bufId;
        }

        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt = convertVertexFormat(indexBuf.getFormat());
            int elSize = indexBuf.getFormat().getComponentSize();
//            int listStart = modeStart[0];
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
                gl.glDrawElements(elMode,
                        elementLength,
                        fmt,
                        curOffset);
                curOffset += elementLength * elSize;
            }
        } else {
            gl.glDrawElements(convertElementMode(mesh.getMode()),
                    indexBuf.getData().capacity(),
                    convertVertexFormat(indexBuf.getFormat()),
                    0);
        }
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1) {
            return; // unsupported
        }
        gl.glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal) {
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled) {
                gl.glEnable(gl.GL_NORMALIZE);
                context.normalizeEnabled = true;
            } else if (!vb.isNormalized() && context.normalizeEnabled) {
                gl.glDisable(gl.GL_NORMALIZE);
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
                gl.glVertexPointer(comps, type, vb.getStride(), data);
                break;
            case Normal:
                gl.glNormalPointer(type, vb.getStride(), data);
                break;
            case Color:
                gl.glColorPointer(comps, type, vb.getStride(), data);
                break;
            case TexCoord:
                gl.glTexCoordPointer(comps, type, vb.getStride(), data);
                break;
        }
    }

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    public void clearVertexAttribs() {
        for (int i = 0; i < 16; i++) {
            VertexBuffer vb = context.boundAttribs[i];
            if (vb != null) {
                int arrayType = convertArrayType(vb.getBufferType());
                gl.glDisableClientState(arrayType);
                context.boundAttribs[vb.getBufferType().ordinal()] = null;
            }
        }
    }

    public void drawTriangleList(VertexBuffer indexBuf, Mesh mesh, int count) {
        Mesh.Mode mode = mesh.getMode();

        Buffer indexData = indexBuf.getData();
        indexData.clear();
        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt = convertVertexFormat(indexBuf.getFormat());
//            int elSize = indexBuf.getFormat().getComponentSize();
//            int listStart = modeStart[0];
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
                indexData.position(curOffset);
                gl.glDrawElements(elMode,
                        elementLength,
                        fmt,
                        indexData);
                curOffset += elementLength;
            }
        } else {
            gl.glDrawElements(convertElementMode(mode),
                    indexData.capacity(),
                    convertVertexFormat(indexBuf.getFormat()),
                    indexData);
        }
    }

    private void renderMeshDefault(Mesh mesh, int lod, int count) {
        VertexBuffer indices = null;
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers) {
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData
                    || vb.getUsage() == Usage.CpuOnly) // ignore cpu-only buffers
            {
                continue;
            }

            if (vb.getBufferType() == Type.Index) {
                indices = vb;
            } else {
                if (vb.getStride() == 0) {
                    // not interleaved
                    setVertexAttrib(vb);
                } else {
                    // interleaved
                    setVertexAttrib(vb, interleavedData);
                }
            }
        }

        if (indices != null) {
            drawTriangleList(indices, mesh, count);
        } else {
            gl.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    private void renderMeshVBO(Mesh mesh, int lod, int count) {
        VertexBuffer indices = null;
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers) {
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData
                    || vb.getUsage() == Usage.CpuOnly // ignore cpu-only buffers
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttribVBO(vb, null);
            } else {
                // interleaved
                setVertexAttribVBO(vb, interleavedData);
            }
        }

        if (indices != null) {
            drawTriangleListVBO(indices, mesh, count);
        } else {
            gl.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    private void updateDisplayList(Mesh mesh) {
        if (mesh.getId() != -1) {
            // delete list first
            gl.glDeleteLists(mesh.getId(), mesh.getId());
            mesh.setId(-1);
        }

        // create new display list
        // first set state to NULL
        applyRenderState(RenderState.NULL);

        // disable lighting
        setLighting(null);

        int id = gl.glGenLists(1);
        mesh.setId(id);
        gl.glNewList(id, gl.GL_COMPILE);
        renderMeshDefault(mesh, 0, 1);
        gl.glEndList();
    }

    private void renderMeshDisplayList(Mesh mesh) {
        if (mesh.getId() == -1) {
            updateDisplayList(mesh);
        }
        gl.glCallList(mesh.getId());
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
    	if (mesh.getVertexCount() == 0)
            return;
        if (context.pointSize != mesh.getPointSize()) {
            gl.glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            gl.glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        checkTexturingUsed();

        if (vbo) {
            renderMeshVBO(mesh, lod, count);
        } else {
            boolean dynamic = false;
            if (mesh.getNumLodLevels() == 0) {
                IntMap<VertexBuffer> bufs = mesh.getBuffers();
                for (Entry<VertexBuffer> entry : bufs) {
                    if (entry.getValue().getUsage() != VertexBuffer.Usage.Static) {
                        dynamic = true;
                        break;
                    }
                }
            } else {
                dynamic = true;
            }

            if (!dynamic) {
                // dealing with a static object, generate display list
                renderMeshDisplayList(mesh);
            } else {
                renderMeshDefault(mesh, lod, count);
            }
        }
    }

    public void setAlphaToCoverage(boolean value) {
        if (value) {
            gl.glEnable(gl.GL_SAMPLE_ALPHA_TO_COVERAGE);
        } else {
            gl.glDisable(gl.GL_SAMPLE_ALPHA_TO_COVERAGE);
        }
    }
}
