package com.jme3.renderer.ios;

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
import jme3tools.shader.ShaderDebug;

/**
 * The <code>Renderer</code> is responsible for taking rendering commands and
 * executing them on the underlying video hardware.
 * 
 * @author Kirill Vainer
 */
public class IGLESShaderRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(IGLESShaderRenderer.class.getName());
    private static final boolean VALIDATE_SHADER = false;

    private final NativeObjectManager objManager = new NativeObjectManager();
    private final EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    private final Statistics statistics = new Statistics();
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final RenderContext context = new RenderContext();
    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);

    private final int maxFBOAttachs = 1; // Only 1 color attachment on ES
    private final int maxMRTFBOAttachs = 1; // FIXME for now, not sure if > 1 is needed for ES
    
    private final int[] intBuf1 = new int[1];
    private final int[] intBuf16 = new int[16];

    private int glslVer;
    private int vertexTextureUnits;
    private int fragTextureUnits;
    private int vertexUniforms;
    private int fragUniforms;
    private int vertexAttribs;
    private int maxRBSize;
    private int maxTexSize;
    private int maxCubeTexSize;

    private FrameBuffer lastFb = null;
    private FrameBuffer mainFbOverride = null;
    private boolean useVBO = true;
    private boolean powerVr = false;
    private boolean uintIndexSupport = false;

    private Shader boundShader;

    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;

    public IGLESShaderRenderer() {
        logger.log(Level.FINE, "IGLESShaderRenderer Constructor");
    }

    /**
     * Get the capabilities of the renderer.
     * @return The capabilities of the renderer.
     */
    public EnumSet<Caps> getCaps() {
        logger.log(Level.FINE, "IGLESShaderRenderer getCaps");
        return caps;
    }

    /**
     * The statistics allow tracking of how data
     * per frame, such as number of objects rendered, number of triangles, etc.
     * These are updated when the Renderer's methods are used, make sure
     * to call {@link Statistics#clearFrame() } at the appropriate time
     * to get accurate info per frame.
     */
    public Statistics getStatistics() {
        logger.log(Level.FINE, "IGLESShaderRenderer getStatistics");
        return statistics;
    }

    /**
     * Invalidates the current rendering state. Should be called after
     * the GL state was changed manually or through an external library.
     */
    public void invalidateState() {
        logger.log(Level.FINE, "IGLESShaderRenderer invalidateState");
    }

    /**
     * Clears certain channels of the currently bound framebuffer.
     *
     * @param color True if to clear colors (RGBA)
     * @param depth True if to clear depth/z
     * @param stencil True if to clear stencil buffer (if available, otherwise
     * ignored)
     */
    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        logger.log(Level.FINE, "IGLESShaderRenderer clearBuffers");
        int bits = 0;
        if (color) {
            //See explanations of the depth below, we must enable color write to be able to clear the color buffer
            if (context.colorWriteEnabled == false) {
                JmeIosGLES.glColorMask(true, true, true, true);
                context.colorWriteEnabled = true;
            }
            bits = JmeIosGLES.GL_COLOR_BUFFER_BIT;
        }
        if (depth) {
            //glClear(GL_DEPTH_BUFFER_BIT) seems to not work when glDepthMask is false
            //here s some link on openl board
            //http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=257223
            //if depth clear is requested, we enable the depthMask
            if (context.depthWriteEnabled == false) {
                JmeIosGLES.glDepthMask(true);
                context.depthWriteEnabled = true;
            }
            bits |= JmeIosGLES.GL_DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            bits |= JmeIosGLES.GL_STENCIL_BUFFER_BIT;
        }
        if (bits != 0) {
            JmeIosGLES.glClear(bits);
            JmeIosGLES.checkGLError();
        }
    }

    /**
     * Sets the background (aka clear) color.
     * 
     * @param color The background color to set
     */
    public void setBackgroundColor(ColorRGBA color) {
        logger.log(Level.FINE, "IGLESShaderRenderer setBackgroundColor");
        JmeIosGLES.glClearColor(color.r, color.g, color.b, color.a);
        JmeIosGLES.checkGLError();
    }

    /**
     * Applies the given {@link RenderState}, making the necessary
     * GL calls so that the state is applied.
     */
    public void applyRenderState(RenderState state) {
        logger.log(Level.FINE, "IGLESShaderRenderer applyRenderState");
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
            JmeIosGLES.glEnable(JmeIosGLES.GL_DEPTH_TEST);
            JmeIosGLES.glDepthFunc(convertTestFunction(context.depthFunc));
            JmeIosGLES.checkGLError();
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            JmeIosGLES.glDisable(JmeIosGLES.GL_DEPTH_TEST);
            JmeIosGLES.checkGLError();
            context.depthTestEnabled = false;
        }
		if (state.getDepthFunc() != context.depthFunc) {
			JmeIosGLES.glDepthFunc(convertTestFunction(state.getDepthFunc()));
			context.depthFunc = state.getDepthFunc();
		}

        if (state.isDepthWrite() && !context.depthWriteEnabled) {
            JmeIosGLES.glDepthMask(true);
            JmeIosGLES.checkGLError();
            context.depthWriteEnabled = true;
        } else if (!state.isDepthWrite() && context.depthWriteEnabled) {
            JmeIosGLES.glDepthMask(false);
            JmeIosGLES.checkGLError();
            context.depthWriteEnabled = false;
        }
        if (state.isColorWrite() && !context.colorWriteEnabled) {
            JmeIosGLES.glColorMask(true, true, true, true);
            JmeIosGLES.checkGLError();
            context.colorWriteEnabled = true;
        } else if (!state.isColorWrite() && context.colorWriteEnabled) {
            JmeIosGLES.glColorMask(false, false, false, false);
            JmeIosGLES.checkGLError();
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
                JmeIosGLES.glEnable(JmeIosGLES.GL_POLYGON_OFFSET_FILL);
                JmeIosGLES.glPolygonOffset(state.getPolyOffsetFactor(),
                        state.getPolyOffsetUnits());
                JmeIosGLES.checkGLError();

                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            } else {
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                        || state.getPolyOffsetUnits() != context.polyOffsetUnits) {
                    JmeIosGLES.glPolygonOffset(state.getPolyOffsetFactor(),
                            state.getPolyOffsetUnits());
                    JmeIosGLES.checkGLError();

                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        } else {
            if (context.polyOffsetEnabled) {
                JmeIosGLES.glDisable(JmeIosGLES.GL_POLYGON_OFFSET_FILL);
                JmeIosGLES.checkGLError();

                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode) {
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off) {
                JmeIosGLES.glDisable(JmeIosGLES.GL_CULL_FACE);
                JmeIosGLES.checkGLError();
            } else {
                JmeIosGLES.glEnable(JmeIosGLES.GL_CULL_FACE);
                JmeIosGLES.checkGLError();
            }

            switch (state.getFaceCullMode()) {
                case Off:
                    break;
                case Back:
                    JmeIosGLES.glCullFace(JmeIosGLES.GL_BACK);
                    JmeIosGLES.checkGLError();
                    break;
                case Front:
                    JmeIosGLES.glCullFace(JmeIosGLES.GL_FRONT);
                    JmeIosGLES.checkGLError();
                    break;
                case FrontAndBack:
                    JmeIosGLES.glCullFace(JmeIosGLES.GL_FRONT_AND_BACK);
                    JmeIosGLES.checkGLError();
                    break;
                default:
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "
                            + state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode) {
            if (state.getBlendMode() == RenderState.BlendMode.Off) {
                JmeIosGLES.glDisable(JmeIosGLES.GL_BLEND);
                JmeIosGLES.checkGLError();
            } else {
                JmeIosGLES.glEnable(JmeIosGLES.GL_BLEND);
                switch (state.getBlendMode()) {
                    case Off:
                        break;
                    case Additive:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_ONE, JmeIosGLES.GL_ONE);
                        break;
                    case AlphaAdditive:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_SRC_ALPHA, JmeIosGLES.GL_ONE);
                        break;
                    case Color:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_ONE, JmeIosGLES.GL_ONE_MINUS_SRC_COLOR);
                        break;
                    case Alpha:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_SRC_ALPHA, JmeIosGLES.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case PremultAlpha:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_ONE, JmeIosGLES.GL_ONE_MINUS_SRC_ALPHA);
                        break;
                    case Modulate:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_DST_COLOR, JmeIosGLES.GL_ZERO);
                        break;
                    case ModulateX2:
                        JmeIosGLES.glBlendFunc(JmeIosGLES.GL_DST_COLOR, JmeIosGLES.GL_SRC_COLOR);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unrecognized blend mode: "
                                + state.getBlendMode());
                }
                JmeIosGLES.checkGLError();
            }
            context.blendMode = state.getBlendMode();
        }
    }

    /**
     * Set the range of the depth values for objects. All rendered
     * objects will have their depth clamped to this range.
     * 
     * @param start The range start
     * @param end The range end
     */
    public void setDepthRange(float start, float end) {
        logger.log(Level.FINE, "IGLESShaderRenderer setDepthRange");
        JmeIosGLES.glDepthRangef(start, end);
        JmeIosGLES.checkGLError();
    }

    /**
     * Called when a new frame has been rendered.
     */
    public void onFrame() {
        logger.log(Level.FINE, "IGLESShaderRenderer onFrame");
        //JmeIosGLES.checkGLErrorForced();
        JmeIosGLES.checkGLError();

        objManager.deleteUnused(this);
    }

    /**
     * Set the world matrix to use. Does nothing if the Renderer is 
     * shader based.
     * 
     * @param worldMatrix World matrix to use.
     */
    public void setWorldMatrix(Matrix4f worldMatrix) {
        logger.log(Level.FINE, "IGLESShaderRenderer setWorldMatrix");
    }

    /**
     * Sets the view and projection matrices to use. Does nothing if the Renderer 
     * is shader based.
     * 
     * @param viewMatrix The view matrix to use.
     * @param projMatrix The projection matrix to use.
     */
    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        logger.log(Level.FINE, "IGLESShaderRenderer setViewProjectionMatrices");
    }

    /**
     * Set the viewport location and resolution on the screen.
     * 
     * @param x The x coordinate of the viewport
     * @param y The y coordinate of the viewport
     * @param width Width of the viewport
     * @param height Height of the viewport
     */
    public void setViewPort(int x, int y, int width, int height) {
        logger.log(Level.FINE, "IGLESShaderRenderer setViewPort");
        if (x != vpX || vpY != y || vpW != width || vpH != height) {
            JmeIosGLES.glViewport(x, y, width, height);
            JmeIosGLES.checkGLError();

            vpX = x;
            vpY = y;
            vpW = width;
            vpH = height;
        }
    }

    /**
     * Specifies a clipping rectangle.
     * For all future rendering commands, no pixels will be allowed
     * to be rendered outside of the clip rectangle.
     * 
     * @param x The x coordinate of the clip rect
     * @param y The y coordinate of the clip rect
     * @param width Width of the clip rect
     * @param height Height of the clip rect
     */
    public void setClipRect(int x, int y, int width, int height) {
        logger.log(Level.FINE, "IGLESShaderRenderer setClipRect");
        if (!context.clipRectEnabled) {
            JmeIosGLES.glEnable(JmeIosGLES.GL_SCISSOR_TEST);
            JmeIosGLES.checkGLError();
            context.clipRectEnabled = true;
        }
        if (clipX != x || clipY != y || clipW != width || clipH != height) {
            JmeIosGLES.glScissor(x, y, width, height);
            JmeIosGLES.checkGLError();
            clipX = x;
            clipY = y;
            clipW = width;
            clipH = height;
        }
    }

    /**
     * Clears the clipping rectangle set with 
     * {@link #setClipRect(int, int, int, int) }.
     */
    public void clearClipRect() {
        logger.log(Level.FINE, "IGLESShaderRenderer clearClipRect");
        if (context.clipRectEnabled) {
            JmeIosGLES.glDisable(JmeIosGLES.GL_SCISSOR_TEST);
            JmeIosGLES.checkGLError();
            context.clipRectEnabled = false;

            clipX = 0;
            clipY = 0;
            clipW = 0;
            clipH = 0;
        }
    }

    /**
     * Set lighting state.
     * Does nothing if the renderer is shader based.
     * The lights should be provided in world space. 
     * Specify <code>null</code> to disable lighting.
     * 
     * @param lights The light list to set.
     */
    public void setLighting(LightList lights) {
        logger.log(Level.FINE, "IGLESShaderRenderer setLighting");
    }

    /**
     * Sets the shader to use for rendering.
     * If the shader has not been uploaded yet, it is compiled
     * and linked. If it has been uploaded, then the 
     * uniform data is updated and the shader is set.
     * 
     * @param shader The shader to use for rendering.
     */
    public void setShader(Shader shader) {
        logger.log(Level.FINE, "IGLESShaderRenderer setShader");
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

    /**
     * Deletes a shader. This method also deletes
     * the attached shader sources.
     * 
     * @param shader Shader to delete.
     */
    public void deleteShader(Shader shader) {
        logger.log(Level.FINE, "IGLESShaderRenderer deleteShader");
        if (shader.getId() == -1) {
            logger.warning("Shader is not uploaded to GPU, cannot delete.");
            return;
        }

        for (ShaderSource source : shader.getSources()) {
            if (source.getId() != -1) {
                JmeIosGLES.glDetachShader(shader.getId(), source.getId());
                JmeIosGLES.checkGLError();

                deleteShaderSource(source);
            }
        }

        JmeIosGLES.glDeleteProgram(shader.getId());
        JmeIosGLES.checkGLError();

        statistics.onDeleteShader();
        shader.resetObject();
    }

    /**
     * Deletes the provided shader source.
     * 
     * @param source The ShaderSource to delete.
     */
    public void deleteShaderSource(ShaderSource source) {
        logger.log(Level.FINE, "IGLESShaderRenderer deleteShaderSource");
        if (source.getId() < 0) {
            logger.warning("Shader source is not uploaded to GPU, cannot delete.");
            return;
        }

        source.clearUpdateNeeded();

        JmeIosGLES.glDeleteShader(source.getId());
        JmeIosGLES.checkGLError();

        source.resetObject();
    }

    /**
     * Copies contents from src to dst, scaling if necessary.
     */
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        logger.log(Level.FINE, "IGLESShaderRenderer copyFrameBuffer");
        copyFrameBuffer(src, dst, true);
    }

    /**
     * Copies contents from src to dst, scaling if necessary.
     * set copyDepth to false to only copy the color buffers.
     */
    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
        logger.warning("IGLESShaderRenderer copyFrameBuffer with depth TODO");
        throw new RendererException("Copy framebuffer not implemented yet.");
    }

    /**
     * Sets the framebuffer that will be drawn to.
     */
    public void setFrameBuffer(FrameBuffer fb) {
        logger.log(Level.FINE, "IGLESShaderRenderer setFrameBuffer");
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
                    JmeIosGLES.glGenerateMipmap(textureType);
                    JmeIosGLES.checkGLError();
                }
            }
        }

        if (fb == null) {
            // unbind any fbos
            if (context.boundFBO != 0) {
                JmeIosGLES.glBindFramebuffer(JmeIosGLES.GL_FRAMEBUFFER, 0);
                JmeIosGLES.checkGLError();

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
                JmeIosGLES.glBindFramebuffer(JmeIosGLES.GL_FRAMEBUFFER, fb.getId());
                JmeIosGLES.checkGLError();

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
                        //intBuf16.clear();
                        for (int i = 0; i < 16; i++) {
                            intBuf16[i] = i < fb.getNumColorBuffers() ? JmeIosGLES.GL_COLOR_ATTACHMENT0 + i : 0;
                        }

                        //intBuf16.flip();// TODO: flip
//                        glDrawBuffers(intBuf16);
                        context.boundDrawBuf = 100 + fb.getNumColorBuffers();
                    }
                } else {
                    RenderBuffer rb = fb.getColorBuffer(fb.getTargetIndex());
                    // select this draw buffer
                    if (context.boundDrawBuf != rb.getSlot()) {
                        JmeIosGLES.glActiveTexture(convertAttachmentSlot(rb.getSlot()));
                        JmeIosGLES.checkGLError();

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
     * Set the framebuffer that will be set instead of the main framebuffer
     * when a call to setFrameBuffer(null) is made.
     * 
     * @param fb 
     */
    public void setMainFrameBufferOverride(FrameBuffer fb) {
        logger.log(Level.FINE, "IGLESShaderRenderer setMainFrameBufferOverride");
        mainFbOverride = fb;
    }

    /**
     * Reads the pixels currently stored in the specified framebuffer
     * into the given ByteBuffer object. 
     * Only color pixels are transferred, the format is BGRA with 8 bits 
     * per component. The given byte buffer should have at least
     * fb.getWidth() * fb.getHeight() * 4 bytes remaining.
     * 
     * @param fb The framebuffer to read from
     * @param byteBuf The bytebuffer to transfer color data to
     */
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        logger.log(Level.FINE, "IGLESShaderRenderer readFrameBuffer");
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

        //JmeIosGLES.glReadPixels2(vpX, vpY, vpW, vpH, JmeIosGLES.GL_RGBA, JmeIosGLES.GL_UNSIGNED_BYTE, byteBuf.array(), 0, vpW * vpH * 4);
        JmeIosGLES.glReadPixels(vpX, vpY, vpW, vpH, JmeIosGLES.GL_RGBA, JmeIosGLES.GL_UNSIGNED_BYTE, byteBuf);
        JmeIosGLES.checkGLError();
    }

    /**
     * Deletes a framebuffer and all attached renderbuffers
     */
    public void deleteFrameBuffer(FrameBuffer fb) {
        logger.log(Level.FINE, "IGLESShaderRenderer deleteFrameBuffer");
        if (fb.getId() != -1) {
            if (context.boundFBO == fb.getId()) {
                JmeIosGLES.glBindFramebuffer(JmeIosGLES.GL_FRAMEBUFFER, 0);
                JmeIosGLES.checkGLError();

                context.boundFBO = 0;
            }

            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1[0] = fb.getId();
            JmeIosGLES.glDeleteFramebuffers(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

    /**
     * Sets the texture to use for the given texture unit.
     */
    public void setTexture(int unit, Texture tex) {
        logger.log(Level.FINE, "IGLESShaderRenderer setTexture");
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
                JmeIosGLES.glActiveTexture(JmeIosGLES.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
            }

            JmeIosGLES.glBindTexture(type, texId);
            JmeIosGLES.checkGLError();

            textures[unit] = image;

            statistics.onTextureUse(tex.getImage(), true);
        } else {
            statistics.onTextureUse(tex.getImage(), false);
        }

        setupTextureParams(tex);
    }

    /**
     * Modify the given Texture tex with the given Image. The image will be put at x and y into the texture.
     *
     * @param tex the Texture that will be modified
     * @param pixels the source Image data to copy data from
     * @param x the x position to put the image into the texture
     * @param y the y position to put the image into the texture
     */
    public void modifyTexture(Texture tex, Image pixels, int x, int y) {
		logger.log(Level.FINE, "IGLESShaderRenderer modifyTexture");
		setTexture(0, tex);
		TextureUtil.uploadSubTexture(pixels, convertTextureType(tex.getType()), 0, x, y);
    }

    /**
     * Deletes a texture from the GPU.
     */
    public void deleteImage(Image image) {
        logger.log(Level.FINE, "IGLESShaderRenderer deleteImage");
        int texId = image.getId();
        if (texId != -1) {
            intBuf1[0] = texId;

            JmeIosGLES.glDeleteTextures(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            image.resetObject();

            statistics.onDeleteTexture();
        }
    }

    /**
     * Uploads a vertex buffer to the GPU.
     * 
     * @param vb The vertex buffer to upload
     */
    public void updateBufferData(VertexBuffer vb) {
        logger.log(Level.FINE, "IGLESShaderRenderer updateBufferData");
        int bufId = vb.getId();
        boolean created = false;
        if (bufId == -1) {
            // create buffer
            JmeIosGLES.glGenBuffers(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            bufId = intBuf1[0];
            vb.setId(bufId);
            objManager.registerObject(vb);

            created = true;
        }

        // bind buffer
        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index) {
            target = JmeIosGLES.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId) {
                JmeIosGLES.glBindBuffer(target, bufId);
                JmeIosGLES.checkGLError();

                context.boundElementArrayVBO = bufId;
            }
        } else {
            target = JmeIosGLES.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId) {
                JmeIosGLES.glBindBuffer(target, bufId);
                JmeIosGLES.checkGLError();

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
					JmeIosGLES.glBufferData(target, size, (ByteBuffer) vb.getData(), usage);
                    JmeIosGLES.checkGLError();
                    break;
                case Short:
                case UnsignedShort:
                    JmeIosGLES.glBufferData(target, size, (ShortBuffer) vb.getData(), usage);
                    JmeIosGLES.checkGLError();
                    break;
                case Int:
                case UnsignedInt:
                    JmeIosGLES.glBufferData(target, size, (IntBuffer) vb.getData(), usage);
                    JmeIosGLES.checkGLError();
                    break;
                case Float:
                    JmeIosGLES.glBufferData(target, size, (FloatBuffer) vb.getData(), usage);
                    JmeIosGLES.checkGLError();
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
            }
        } else {
            int size = vb.getData().limit() * vb.getFormat().getComponentSize();
			
            switch (vb.getFormat()) {
                case Byte:
                case UnsignedByte:
                    JmeIosGLES.glBufferSubData(target, 0, size, (ByteBuffer) vb.getData());
                    JmeIosGLES.checkGLError();
                    break;
                case Short:
                case UnsignedShort:
                    JmeIosGLES.glBufferSubData(target, 0, size, (ShortBuffer) vb.getData());
                    JmeIosGLES.checkGLError();
                    break;
                case Int:
                case UnsignedInt:
                    JmeIosGLES.glBufferSubData(target, 0, size, (IntBuffer) vb.getData());
                    JmeIosGLES.checkGLError();
                    break;
                case Float:
                    JmeIosGLES.glBufferSubData(target, 0, size, (FloatBuffer) vb.getData());
                    JmeIosGLES.checkGLError();
                    break;
                default:
                    throw new RuntimeException("Unknown buffer format.");
            }
        }
        vb.clearUpdateNeeded();
    }

    /**
     * Deletes a vertex buffer from the GPU.
     * @param vb The vertex buffer to delete
     */
    public void deleteBuffer(VertexBuffer vb) {
        logger.log(Level.FINE, "IGLESShaderRenderer deleteBuffer");
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1[0] = bufId;

            JmeIosGLES.glDeleteBuffers(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            vb.resetObject();
        }
    }

    /**
     * Renders <code>count</code> meshes, with the geometry data supplied.
     * The shader which is currently set with <code>setShader</code> is
     * responsible for transforming the input verticies into clip space
     * and shading it based on the given vertex attributes.
     * The int variable gl_InstanceID can be used to access the current
     * instance of the mesh being rendered inside the vertex shader.
     *
     * @param mesh The mesh to render
     * @param lod The LOD level to use, see {@link Mesh#setLodLevels(com.jme3.scene.VertexBuffer[]) }.
     * @param count Number of mesh instances to render
     */
    public void renderMesh(Mesh mesh, int lod, int count, VertexBuffer[] instanceData) {
        logger.log(Level.FINE, "IGLESShaderRenderer renderMesh");
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
            JmeIosGLES.glLineWidth(mesh.getLineWidth());
            JmeIosGLES.checkGLError();
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
     * Resets all previously used {@link NativeObject Native Objects} on this Renderer.
     * The state of the native objects is reset in such way, that using
     * them again will cause the renderer to reupload them.
     * Call this method when you know the GL context is going to shutdown.
     * 
     * @see NativeObject#resetObject() 
     */
    public void resetGLObjects() {
        logger.log(Level.FINE, "IGLESShaderRenderer resetGLObjects");
        objManager.resetObjects();
        statistics.clearMemory();
        boundShader = null;
        lastFb = null;
        context.reset();
    }

    /**
     * Deletes all previously used {@link NativeObject Native Objects} on this Renderer, and
     * then resets the native objects.
     * 
     * @see #resetGLObjects() 
     * @see NativeObject#deleteObject(java.lang.Object) 
     */
    public void cleanup() {
        logger.log(Level.FINE, "IGLESShaderRenderer cleanup");
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
    }
    
    /**
     * Sets the alpha to coverage state.
     * <p>
     * When alpha coverage and multi-sampling is enabled, 
     * each pixel will contain alpha coverage in all
     * of its subsamples, which is then combined when
     * other future alpha-blended objects are rendered.
     * </p>
     * <p>
     * Alpha-to-coverage is useful for rendering transparent objects
     * without having to worry about sorting them.
     * </p>
     */
    public void setAlphaToCoverage(boolean value) {
        logger.log(Level.FINE, "IGLESShaderRenderer setAlphaToCoverage");
        if (value) {
            JmeIosGLES.glEnable(JmeIosGLES.GL_SAMPLE_ALPHA_TO_COVERAGE);
            JmeIosGLES.checkGLError();
        } else {
            JmeIosGLES.glDisable(JmeIosGLES.GL_SAMPLE_ALPHA_TO_COVERAGE);
            JmeIosGLES.checkGLError();
        }
    }
    
    
    /* ------------------------------------------------------------------------------ */
    
    
    public void initialize() {
        Level store = logger.getLevel();
        logger.setLevel(Level.FINE);
        
        logger.log(Level.FINE, "Vendor: {0}", JmeIosGLES.glGetString(JmeIosGLES.GL_VENDOR));
        logger.log(Level.FINE, "Renderer: {0}", JmeIosGLES.glGetString(JmeIosGLES.GL_RENDERER));
        logger.log(Level.FINE, "Version: {0}", JmeIosGLES.glGetString(JmeIosGLES.GL_VERSION));
        logger.log(Level.FINE, "Shading Language Version: {0}", JmeIosGLES.glGetString(JmeIosGLES.GL_SHADING_LANGUAGE_VERSION));

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
        int openGlVer = extractVersion("OpenGL ES ", JmeIosGLES.glGetString(JmeIosGLES.GL_VERSION));
        if (openGlVer == -1) {
            glslVer = -1;
            throw new UnsupportedOperationException("OpenGL ES 2.0+ is required for IGLESShaderRenderer!");
        }

        // Check shader language version
        glslVer = extractVersion("OpenGL ES GLSL ES ", JmeIosGLES.glGetString(JmeIosGLES.GL_SHADING_LANGUAGE_VERSION));
        switch (glslVer) {
            // TODO: When new versions of OpenGL ES shader language come out,
            // update this.
            default:
                caps.add(Caps.GLSL100);
                break;
        }

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS, intBuf16, 0);
        vertexTextureUnits = intBuf16[0];
        logger.log(Level.FINE, "VTF Units: {0}", vertexTextureUnits);
        if (vertexTextureUnits > 0) {
            caps.add(Caps.VertexTextureFetch);
        }

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_TEXTURE_IMAGE_UNITS, intBuf16, 0);
        fragTextureUnits = intBuf16[0];
        logger.log(Level.FINE, "Texture Units: {0}", fragTextureUnits);

        // Multiply vector count by 4 to get float count.
        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_VERTEX_UNIFORM_VECTORS, intBuf16, 0);
        vertexUniforms = intBuf16[0] * 4;
        logger.log(Level.FINER, "Vertex Uniforms: {0}", vertexUniforms);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_FRAGMENT_UNIFORM_VECTORS, intBuf16, 0);
        fragUniforms = intBuf16[0] * 4;
        logger.log(Level.FINER, "Fragment Uniforms: {0}", fragUniforms);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_VARYING_VECTORS, intBuf16, 0);
        int varyingFloats = intBuf16[0] * 4;
        logger.log(Level.FINER, "Varying Floats: {0}", varyingFloats);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_VERTEX_ATTRIBS, intBuf16, 0);
        vertexAttribs = intBuf16[0];
        logger.log(Level.FINE, "Vertex Attributes: {0}", vertexAttribs);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_SUBPIXEL_BITS, intBuf16, 0);
        int subpixelBits = intBuf16[0];
        logger.log(Level.FINE, "Subpixel Bits: {0}", subpixelBits);

//        GLES10.glGetIntegerv(GLES10.GL_MAX_ELEMENTS_VERTICES, intBuf16);
//        maxVertCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Vertex Count: {0}", maxVertCount);
//
//        GLES10.glGetIntegerv(GLES10.GL_MAX_ELEMENTS_INDICES, intBuf16);
//        maxTriCount = intBuf16.get(0);
//        logger.log(Level.FINER, "Preferred Batch Index Count: {0}", maxTriCount);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_TEXTURE_SIZE, intBuf16, 0);
        maxTexSize = intBuf16[0];
        logger.log(Level.FINE, "Maximum Texture Resolution: {0}", maxTexSize);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_CUBE_MAP_TEXTURE_SIZE, intBuf16, 0);
        maxCubeTexSize = intBuf16[0];
        logger.log(Level.FINE, "Maximum CubeMap Resolution: {0}", maxCubeTexSize);

        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_MAX_RENDERBUFFER_SIZE, intBuf16, 0);
        maxRBSize = intBuf16[0];
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

        String extensions = JmeIosGLES.glGetString(JmeIosGLES.GL_EXTENSIONS);
        logger.log(Level.FINE, "GL_EXTENSIONS: {0}", extensions);

        // Get number of compressed formats available.
        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_NUM_COMPRESSED_TEXTURE_FORMATS, intBuf16, 0);
        int numCompressedFormats = intBuf16[0];

        // Allocate buffer for compressed formats.
        int[] compressedFormats = new int[numCompressedFormats];
        JmeIosGLES.glGetIntegerv(JmeIosGLES.GL_COMPRESSED_TEXTURE_FORMATS, compressedFormats, 0);

        // Check for errors after all glGet calls.
        JmeIosGLES.checkGLError();

        // Print compressed formats.
        for (int i = 0; i < numCompressedFormats; i++) {
            logger.log(Level.FINE, "Compressed Texture Formats: {0}", compressedFormats[i]);
        }

        TextureUtil.loadTextureFeatures(extensions);

        applyRenderState(RenderState.DEFAULT);
        JmeIosGLES.glDisable(JmeIosGLES.GL_DITHER);
        JmeIosGLES.checkGLError();

        logger.log(Level.FINE, "Caps: {0}", caps);
        logger.setLevel(store);

        uintIndexSupport = extensions.contains("GL_OES_element_index_uint");
        logger.log(Level.FINE, "Support for UInt index: {0}", uintIndexSupport);
    }
    
    
    /* ------------------------------------------------------------------------------ */
    
    
    private int extractVersion(String prefixStr, String versionStr) {
        if (versionStr != null) {
            int spaceIdx = versionStr.indexOf(" ", prefixStr.length());
            if (spaceIdx >= 1) {
                versionStr = versionStr.substring(prefixStr.length(), spaceIdx).trim();
            } else {
                versionStr = versionStr.substring(prefixStr.length()).trim();
            }
            float version = Float.parseFloat(versionStr);
            return (int) (version * 100);
        } else {
            return -1;
        }
    }

    private void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        intBuf1[0] = rb.getId();
        JmeIosGLES.glDeleteRenderbuffers(1, intBuf1, 0);
        JmeIosGLES.checkGLError();
    }
    
	private int convertUsage(Usage usage) {
        switch (usage) {
            case Static:
                return JmeIosGLES.GL_STATIC_DRAW;
            case Dynamic:
                return JmeIosGLES.GL_DYNAMIC_DRAW;
            case Stream:
                return JmeIosGLES.GL_STREAM_DRAW;
            default:
                throw new RuntimeException("Unknown usage type.");
        }
    }
 
 
    protected void bindProgram(Shader shader) {
        int shaderId = shader.getId();
        if (context.boundShaderProgram != shaderId) {
            JmeIosGLES.glUseProgram(shaderId);
            JmeIosGLES.checkGLError();

            statistics.onShaderUse(shader, true);
            boundShader = shader;
            context.boundShaderProgram = shaderId;
        } else {
            statistics.onShaderUse(shader, false);
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


    protected void updateUniform(Shader shader, Uniform uniform) {
        logger.log(Level.FINE, "IGLESShaderRenderer private updateUniform: " + uniform.getVarType());
        int shaderId = shader.getId();

        assert uniform.getName() != null;
        assert shader.getId() > 0;

        if (context.boundShaderProgram != shaderId) {
            JmeIosGLES.glUseProgram(shaderId);
            JmeIosGLES.checkGLError();

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
        ByteBuffer bb;//GetPrimitiveArrayCritical
        FloatBuffer fb;
        IntBuffer ib;
        switch (uniform.getVarType()) {
            case Float:
                Float f = (Float) uniform.getValue();
                JmeIosGLES.glUniform1f(loc, f.floatValue());
                break;
            case Vector2:
                Vector2f v2 = (Vector2f) uniform.getValue();
                JmeIosGLES.glUniform2f(loc, v2.getX(), v2.getY());
                break;
            case Vector3:
                Vector3f v3 = (Vector3f) uniform.getValue();
                JmeIosGLES.glUniform3f(loc, v3.getX(), v3.getY(), v3.getZ());
                break;
            case Vector4:
                Object val = uniform.getValue();
                if (val instanceof ColorRGBA) {
                    ColorRGBA c = (ColorRGBA) val;
                    JmeIosGLES.glUniform4f(loc, c.r, c.g, c.b, c.a);
                } else if (val instanceof Vector4f) {
                    Vector4f c = (Vector4f) val;
                    JmeIosGLES.glUniform4f(loc, c.x, c.y, c.z, c.w);
                } else {
                    Quaternion c = (Quaternion) uniform.getValue();
                    JmeIosGLES.glUniform4f(loc, c.getX(), c.getY(), c.getZ(), c.getW());
                }
                break;
            case Boolean:
                Boolean b = (Boolean) uniform.getValue();
                JmeIosGLES.glUniform1i(loc, b.booleanValue() ? JmeIosGLES.GL_TRUE : JmeIosGLES.GL_FALSE);
                break;
            case Matrix3:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 9;
                JmeIosGLES.glUniformMatrix3fv(loc, 1, false, fb);
                break;
            case Matrix4:
                fb = (FloatBuffer) uniform.getValue();
                assert fb.remaining() == 16;
                JmeIosGLES.glUniformMatrix4fv(loc, 1, false, fb);
                break;
            case IntArray:
                ib = (IntBuffer) uniform.getValue();
                JmeIosGLES.glUniform1iv(loc, ib.limit(), ib);
                break;
            case FloatArray:
                fb = (FloatBuffer) uniform.getValue();
                JmeIosGLES.glUniform1fv(loc, fb.limit(), fb);
                break;
            case Vector2Array:
                fb = (FloatBuffer) uniform.getValue();
                JmeIosGLES.glUniform2fv(loc, fb.limit() / 2, fb);
                break;
            case Vector3Array:
                fb = (FloatBuffer) uniform.getValue();
                JmeIosGLES.glUniform3fv(loc, fb.limit() / 3, fb);
                break;
            case Vector4Array:
                fb = (FloatBuffer) uniform.getValue();
                JmeIosGLES.glUniform4fv(loc, fb.limit() / 4, fb);
                break;
            case Matrix4Array:
                fb = (FloatBuffer) uniform.getValue();
                JmeIosGLES.glUniformMatrix4fv(loc, fb.limit() / 16, false, fb);
                break;
            case Int:
                Integer i = (Integer) uniform.getValue();
                JmeIosGLES.glUniform1i(loc, i.intValue());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported uniform type: " + uniform.getVarType());
        }
        JmeIosGLES.checkGLError();
    }

    protected void updateUniformLocation(Shader shader, Uniform uniform) {
        stringBuf.setLength(0);
        stringBuf.append(uniform.getName()).append('\0');
        updateNameBuffer();
        int loc = JmeIosGLES.glGetUniformLocation(shader.getId(), uniform.getName());
        JmeIosGLES.checkGLError();

        if (loc < 0) {
            uniform.setLocation(-1);
            // uniform is not declared in shader
        } else {
            uniform.setLocation(loc);
        }
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
    
    
    public void updateShaderData(Shader shader) {
        int id = shader.getId();
        boolean needRegister = false;
        if (id == -1) {
            // create program
            id = JmeIosGLES.glCreateProgram();
            JmeIosGLES.checkGLError();

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

            JmeIosGLES.glAttachShader(id, source.getId());
            JmeIosGLES.checkGLError();
        }

        // link shaders to program
        JmeIosGLES.glLinkProgram(id);
        JmeIosGLES.checkGLError();

        JmeIosGLES.glGetProgramiv(id, JmeIosGLES.GL_LINK_STATUS, intBuf1, 0);
        JmeIosGLES.checkGLError();

        boolean linkOK = intBuf1[0] == JmeIosGLES.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !linkOK) {
            JmeIosGLES.glGetProgramiv(id, JmeIosGLES.GL_INFO_LOG_LENGTH, intBuf1, 0);
            JmeIosGLES.checkGLError();

            int length = intBuf1[0];
            if (length > 3) {
                // get infos
                infoLog = JmeIosGLES.glGetProgramInfoLog(id);
                JmeIosGLES.checkGLError();
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
                throw new RendererException("Shader link failure, shader:" + shader + "\ninfo: <not provided>");
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

    public void updateTexImageData(Image img, Texture.Type type) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            JmeIosGLES.glGenTextures(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            texId = intBuf1[0];
            img.setId(texId);
            objManager.registerObject(img);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type);
        if (context.boundTextures[0] != img) {
            if (context.boundTextureUnit != 0) {
                JmeIosGLES.glActiveTexture(JmeIosGLES.GL_TEXTURE0);
                JmeIosGLES.checkGLError();

                context.boundTextureUnit = 0;
            }

            JmeIosGLES.glBindTexture(target, texId);
            JmeIosGLES.checkGLError();

            context.boundTextures[0] = img;
        }

        boolean needMips = false;
        if (img.isGeneratedMipmapsRequired()) {
            needMips = true;
            img.setMipmapsGenerated(true);
        }

        if (target == JmeIosGLES.GL_TEXTURE_CUBE_MAP) {
            // Check max texture size before upload
            if (img.getWidth() > maxCubeTexSize || img.getHeight() > maxCubeTexSize) {
                throw new RendererException("Cannot upload cubemap " + img + ". The maximum supported cubemap resolution is " + maxCubeTexSize);
            }
        } else {
            if (img.getWidth() > maxTexSize || img.getHeight() > maxTexSize) {
                throw new RendererException("Cannot upload texture " + img + ". The maximum supported texture resolution is " + maxTexSize);
            }
        }

        if (target == JmeIosGLES.GL_TEXTURE_CUBE_MAP) {
            // Upload a cube map / sky box
            /*
            @SuppressWarnings("unchecked")
            List<AndroidImageInfo> bmps = (List<AndroidImageInfo>) img.getEfficentData();
            if (bmps != null) {
                // Native android bitmap
                if (bmps.size() != 6) {
                    throw new UnsupportedOperationException("Invalid texture: " + img
                            + "Cubemap textures must contain 6 data units.");
                }
                for (int i = 0; i < 6; i++) {
                    TextureUtil.uploadTextureBitmap(JmeIosGLES.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, bmps.get(i).getBitmap(), needMips);
                    bmps.get(i).notifyBitmapUploaded();
                }
            } else {
        	*/
                // Standard jme3 image data
                List<ByteBuffer> data = img.getData();
                if (data.size() != 6) {
                    throw new UnsupportedOperationException("Invalid texture: " + img
                            + "Cubemap textures must contain 6 data units.");
                }
                for (int i = 0; i < 6; i++) {
                    TextureUtil.uploadTextureAny(img, JmeIosGLES.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, i, needMips);
                }
            //}
        } else {
            TextureUtil.uploadTextureAny(img, target, 0, needMips);
            /*
            if (img.getEfficentData() instanceof AndroidImageInfo) {
                AndroidImageInfo info = (AndroidImageInfo) img.getEfficentData();
                info.notifyBitmapUploaded();
            }
            */
        }

        img.clearUpdateNeeded();
    }

    private void setupTextureParams(Texture tex) {
        int target = convertTextureType(tex.getType());

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());

        JmeIosGLES.glTexParameteri(target, JmeIosGLES.GL_TEXTURE_MIN_FILTER, minFilter);
        JmeIosGLES.checkGLError();
        JmeIosGLES.glTexParameteri(target, JmeIosGLES.GL_TEXTURE_MAG_FILTER, magFilter);
        JmeIosGLES.checkGLError();

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
                JmeIosGLES.glTexParameteri(target, JmeIosGLES.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));

                // fall down here is intentional..
//          case OneDimensional:
                JmeIosGLES.glTexParameteri(target, JmeIosGLES.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));

                JmeIosGLES.checkGLError();
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
    
    private int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return JmeIosGLES.GL_TEXTURE_2D;
            //        case TwoDimensionalArray:
            //            return EXTTextureArray.GL_TEXTURE_2D_ARRAY_EXT;
//            case ThreeDimensional:
            //               return GLES20.GL_TEXTURE_3D;
            case CubeMap:
                return JmeIosGLES.GL_TEXTURE_CUBE_MAP;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return JmeIosGLES.GL_LINEAR;
            case Nearest:
                return JmeIosGLES.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter) {
        switch (filter) {
            case Trilinear:
                return JmeIosGLES.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return JmeIosGLES.GL_LINEAR_MIPMAP_NEAREST;
            case NearestLinearMipMap:
                return JmeIosGLES.GL_NEAREST_MIPMAP_LINEAR;
            case NearestNearestMipMap:
                return JmeIosGLES.GL_NEAREST_MIPMAP_NEAREST;
            case BilinearNoMipMaps:
                return JmeIosGLES.GL_LINEAR;
            case NearestNoMipMaps:
                return JmeIosGLES.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown min filter: " + filter);
        }
    }

    private int convertWrapMode(Texture.WrapMode mode) {
        switch (mode) {
            case BorderClamp:
            case Clamp:
            case EdgeClamp:
                return JmeIosGLES.GL_CLAMP_TO_EDGE;
            case Repeat:
                return JmeIosGLES.GL_REPEAT;
            case MirroredRepeat:
                return JmeIosGLES.GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

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
            JmeIosGLES.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
            JmeIosGLES.checkGLError();
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
            JmeIosGLES.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
            JmeIosGLES.checkGLError();
        }
        clearVertexAttribs();
        clearTextureUnits();
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
                loc = JmeIosGLES.glGetAttribLocation(programId, attributeName);
                JmeIosGLES.checkGLError();

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
                JmeIosGLES.glEnableVertexAttribArray(loc);
                JmeIosGLES.checkGLError();
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
                    JmeIosGLES.glBindBuffer(JmeIosGLES.GL_ARRAY_BUFFER, bufId);
                    JmeIosGLES.checkGLError();

                    context.boundArrayVBO = bufId;
                }

                vb.getData().rewind();
				/*
                Android22Workaround.glVertexAttribPointer(loc,
                                    vb.getNumComponents(),
                                    convertVertexBufferFormat(vb.getFormat()),
                                    vb.isNormalized(),
                                    vb.getStride(),
                                    0);
				*/
                logger.warning("iTODO Android22Workaround");
				
                JmeIosGLES.glVertexAttribPointer(loc,
                                    vb.getNumComponents(),
                                    convertVertexBufferFormat(vb.getFormat()),
                                    vb.isNormalized(),
                                    vb.getStride(),
                                    null);
					
                JmeIosGLES.checkGLError();

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
        JmeIosGLES.glDrawArrays(convertElementMode(mode), 0, vertCount);
        JmeIosGLES.checkGLError();
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
            JmeIosGLES.glBindBuffer(JmeIosGLES.GL_ELEMENT_ARRAY_BUFFER, bufId);
            JmeIosGLES.checkGLError();

            context.boundElementArrayVBO = bufId;
        }

        int vertCount = mesh.getVertexCount();
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);

        Buffer indexData = indexBuf.getData();

        if (!uintIndexSupport && (indexBuf.getFormat() == Format.UnsignedInt)) {
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
                    JmeIosGLES.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());
                    JmeIosGLES.checkGLError();
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
				logger.log(Level.FINE, "IGLESShaderRenderer drawTriangleList TODO check");
                indexData.rewind();
                JmeIosGLES.glDrawElementsIndex(
                        convertElementMode(mesh.getMode()),
                        indexBuf.getData().limit(),
                        convertVertexBufferFormat(indexBuf.getFormat()),
                        0);
				/*TODO:
                indexData.rewind();
                JmeIosGLES.glDrawElements(
                        convertElementMode(mesh.getMode()),
                        indexBuf.getData().limit(),
                        convertVertexBufferFormat(indexBuf.getFormat()),
                        0);
                */
                JmeIosGLES.checkGLError();
            }
        }
    }

    public int convertElementMode(Mesh.Mode mode) {
        switch (mode) {
            case Points:
                return JmeIosGLES.GL_POINTS;
            case Lines:
                return JmeIosGLES.GL_LINES;
            case LineLoop:
                return JmeIosGLES.GL_LINE_LOOP;
            case LineStrip:
                return JmeIosGLES.GL_LINE_STRIP;
            case Triangles:
                return JmeIosGLES.GL_TRIANGLES;
            case TriangleFan:
                return JmeIosGLES.GL_TRIANGLE_FAN;
            case TriangleStrip:
                return JmeIosGLES.GL_TRIANGLE_STRIP;
            default:
                throw new UnsupportedOperationException("Unrecognized mesh mode: " + mode);
        }
    }


    private int convertVertexBufferFormat(Format format) {
        switch (format) {
            case Byte:
                return JmeIosGLES.GL_BYTE;
            case UnsignedByte:
                return JmeIosGLES.GL_UNSIGNED_BYTE;
            case Short:
                return JmeIosGLES.GL_SHORT;
            case UnsignedShort:
                return JmeIosGLES.GL_UNSIGNED_SHORT;
            case Int:
                return JmeIosGLES.GL_INT;
            case UnsignedInt:
                return JmeIosGLES.GL_UNSIGNED_INT;
            /*
            case Half:
            return NVHalfFloat.GL_HALF_FLOAT_NV;
            //                return ARBHalfFloatVertex.GL_HALF_FLOAT;
             */
            case Float:
                return JmeIosGLES.GL_FLOAT;
//            case Double:
//                return JmeIosGLES.GL_DOUBLE;
            default:
                throw new RuntimeException("Unknown buffer format.");

        }
    }
    
    public void clearVertexAttribs() {
        IDList attribList = context.attribIndexList;
        for (int i = 0; i < attribList.oldLen; i++) {
            int idx = attribList.oldList[i];

            JmeIosGLES.glDisableVertexAttribArray(idx);
            JmeIosGLES.checkGLError();

            context.boundAttribs[idx] = null;
        }
        context.attribIndexList.copyNewToOld();
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


    public void updateFrameBuffer(FrameBuffer fb) {
        int id = fb.getId();
        if (id == -1) {
            // create FBO
            JmeIosGLES.glGenFramebuffers(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            id = intBuf1[0];
            fb.setId(id);
            objManager.registerObject(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            JmeIosGLES.glBindFramebuffer(JmeIosGLES.GL_FRAMEBUFFER, id);
            JmeIosGLES.checkGLError();

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
            JmeIosGLES.glFramebufferRenderbuffer(JmeIosGLES.GL_FRAMEBUFFER,
                    convertAttachmentSlot(rb.getSlot()),
                    JmeIosGLES.GL_RENDERBUFFER,
                    rb.getId());

            JmeIosGLES.checkGLError();
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

        JmeIosGLES.glFramebufferTexture2D(JmeIosGLES.GL_FRAMEBUFFER,
                convertAttachmentSlot(rb.getSlot()),
                convertTextureType(tex.getType()),
                image.getId(),
                0);

        JmeIosGLES.checkGLError();
    }


    private void updateRenderBuffer(FrameBuffer fb, RenderBuffer rb) {
        int id = rb.getId();
        if (id == -1) {
            JmeIosGLES.glGenRenderbuffers(1, intBuf1, 0);
            JmeIosGLES.checkGLError();

            id = intBuf1[0];
            rb.setId(id);
        }

        if (context.boundRB != id) {
            JmeIosGLES.glBindRenderbuffer(JmeIosGLES.GL_RENDERBUFFER, id);
            JmeIosGLES.checkGLError();

            context.boundRB = id;
        }

        if (fb.getWidth() > maxRBSize || fb.getHeight() > maxRBSize) {
            throw new RendererException("Resolution " + fb.getWidth()
                    + ":" + fb.getHeight() + " is not supported.");
        }

        TextureUtil.IosGLImageFormat imageFormat = TextureUtil.getImageFormat(rb.getFormat());
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
            JmeIosGLES.glRenderbufferStorage(JmeIosGLES.GL_RENDERBUFFER,
                    imageFormat.renderBufferStorageFormat,
                    fb.getWidth(),
                    fb.getHeight());

            JmeIosGLES.checkGLError();
        }
    }


    private int convertAttachmentSlot(int attachmentSlot) {
        // can also add support for stencil here
        if (attachmentSlot == -100) {
            return JmeIosGLES.GL_DEPTH_ATTACHMENT;
        } else if (attachmentSlot == 0) {
            return JmeIosGLES.GL_COLOR_ATTACHMENT0;
        } else {
            throw new UnsupportedOperationException("Android does not support multiple color attachments to an FBO");
        }
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
        int status = JmeIosGLES.glCheckFramebufferStatus(JmeIosGLES.GL_FRAMEBUFFER);
        switch (status) {
            case JmeIosGLES.GL_FRAMEBUFFER_COMPLETE:
                break;
            case JmeIosGLES.GL_FRAMEBUFFER_UNSUPPORTED:
                //Choose different formats
                throw new IllegalStateException("Framebuffer object format is "
                        + "unsupported by the video hardware.");
            case JmeIosGLES.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                throw new IllegalStateException("Framebuffer has erronous attachment.");
            case JmeIosGLES.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                throw new IllegalStateException("Framebuffer doesn't have any renderbuffers attached.");
            case JmeIosGLES.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
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
        System.out.println("Is proper? " + JmeIosGLES.glIsRenderbuffer(rb.getId()));

        int attachment = convertAttachmentSlot(rb.getSlot());

        //intBuf16.clear();
        JmeIosGLES.glGetFramebufferAttachmentParameteriv(JmeIosGLES.GL_FRAMEBUFFER,
                attachment, JmeIosGLES.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, intBuf16, 0);
        int type = intBuf16[0];

        //intBuf16.clear();
        JmeIosGLES.glGetFramebufferAttachmentParameteriv(JmeIosGLES.GL_FRAMEBUFFER,
                attachment, JmeIosGLES.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, intBuf16, 0);
        int rbName = intBuf16[0];

        switch (type) {
            case JmeIosGLES.GL_NONE:
                System.out.println("Type: None");
                break;
            case JmeIosGLES.GL_TEXTURE:
                System.out.println("Type: Texture");
                break;
            case JmeIosGLES.GL_RENDERBUFFER:
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
        //intBuf16.clear();
//        int curDrawBinding = GLES20.glGetIntegerv(GLES20.GL_DRAW_FRAMEBUFFER_BINDING);
//        int curReadBinding = glGetInteger(ARBFramebufferObject.GL_READ_FRAMEBUFFER_BINDING);

        System.out.println("=== OpenGL FBO State ===");
        System.out.println("Context doublebuffered? " + doubleBuffer);
        System.out.println("FBO ID: " + fbId);
        System.out.println("Is proper? " + JmeIosGLES.glIsFramebuffer(fbId));
//        System.out.println("Is bound to draw? " + (fbId == curDrawBinding));
//        System.out.println("Is bound to read? " + (fbId == curReadBinding));
//        System.out.println("Draw buffer: " + drawBuf);
//        System.out.println("Read buffer: " + readBuf);

        if (context.boundFBO != fbId) {
            JmeIosGLES.glBindFramebuffer(JmeIosGLES.GL_FRAMEBUFFER, fbId);
            context.boundFBO = fbId;
        }

        if (fb.getDepthBuffer() != null) {
            printRealRenderBufferInfo(fb, fb.getDepthBuffer(), "Depth");
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++) {
            printRealRenderBufferInfo(fb, fb.getColorBuffer(i), "Color" + i);
        }
 

    }

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
                JmeIosGLES.glDrawElements(elMode, elementLength, fmt, indexBuf.getData());
                JmeIosGLES.checkGLError();

                curOffset += elementLength * elSize;
            }
        } else {
           JmeIosGLES.glDrawElements(
                    convertElementMode(mesh.getMode()),
                    indexBuf.getData().limit(),
                    convertVertexBufferFormat(indexBuf.getFormat()),
                    indexBuf.getData());
            JmeIosGLES.checkGLError();
        }
    }

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

                loc = JmeIosGLES.glGetAttribLocation(programId, attributeName);
                JmeIosGLES.checkGLError();

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
                JmeIosGLES.glVertexAttribPointer(loc,
                        vb.getNumComponents(),
                        convertVertexBufferFormat(vb.getFormat()),
                        vb.isNormalized(),
                        vb.getStride(),
                        avb.getData());

                JmeIosGLES.checkGLError();

                JmeIosGLES.glEnableVertexAttribArray(loc);
                JmeIosGLES.checkGLError();

                attribs[loc] = vb;
            } // if (attribs[loc] != vb)
        } else {
            throw new IllegalStateException("Cannot render mesh without shader bound");
        }
    }

    public void setVertexAttrib_Array(VertexBuffer vb) {
        setVertexAttrib_Array(vb, null);
    }


    public void updateShaderSourceData(ShaderSource source) {
        int id = source.getId();
        if (id == -1) {
            // Create id
            id = JmeIosGLES.glCreateShader(convertShaderType(source.getType()));
            JmeIosGLES.checkGLError();

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
            JmeIosGLES.glShaderSource(
                    id, source.getDefines()
                    + source.getSource());
        } else {
            String precision ="";
            if (source.getType() == ShaderType.Fragment) {
                precision =  "precision mediump float;\n";
            }
            JmeIosGLES.glShaderSource(
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

        JmeIosGLES.glCompileShader(id);
        JmeIosGLES.checkGLError();

        JmeIosGLES.glGetShaderiv(id, JmeIosGLES.GL_COMPILE_STATUS, intBuf1, 0);
        JmeIosGLES.checkGLError();

        boolean compiledOK = intBuf1[0] == JmeIosGLES.GL_TRUE;
        String infoLog = null;

        if (VALIDATE_SHADER || !compiledOK) {
            // even if compile succeeded, check
            // log for warnings
            JmeIosGLES.glGetShaderiv(id, JmeIosGLES.GL_INFO_LOG_LENGTH, intBuf1, 0);
            JmeIosGLES.checkGLError();
            infoLog = JmeIosGLES.glGetShaderInfoLog(id);
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
                throw new RendererException("compile error in: " + source + "\n" + infoLog);
            } else {
                throw new RendererException("compile error in: " + source + "\nerror: <not provided>");
            }
        }
    }


    public int convertShaderType(ShaderType type) {
        switch (type) {
            case Fragment:
                return JmeIosGLES.GL_FRAGMENT_SHADER;
            case Vertex:
                return JmeIosGLES.GL_VERTEX_SHADER;
//            case Geometry:
//                return ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB;
            default:
                throw new RuntimeException("Unrecognized shader type.");
        }
    }
    
	private int convertTestFunction(RenderState.TestFunction testFunc) {
		switch (testFunc) {
			case Never:
				return JmeIosGLES.GL_NEVER;
			case Less:
				return JmeIosGLES.GL_LESS;
			case LessOrEqual:
				return JmeIosGLES.GL_LEQUAL;
			case Greater:
				return JmeIosGLES.GL_GREATER;
			case GreaterOrEqual:
				return JmeIosGLES.GL_GEQUAL;
			case Equal:
				return JmeIosGLES.GL_EQUAL;
			case NotEqual:
				return JmeIosGLES.GL_NOTEQUAL;
			case Always:
				return JmeIosGLES.GL_ALWAYS;
			default:
				throw new UnsupportedOperationException("Unrecognized test function: " + testFunc);
		}
	}    	

    public void setMainFrameBufferSrgb(boolean srgb) {
        
    }

    public void setLinearizeSrgbImages(boolean linearize) {
      
    }
}