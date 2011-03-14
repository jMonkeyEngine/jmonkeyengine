package com.jme3.renderer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.jme3.glhelper.Helper;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.shader.Uniform;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.FrameBuffer.RenderBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.ListMap;

/**
 * OpenGL renderer that does not rely on a specific binding, that uses low-level OpenGL operations to 
 * perform higher-level tasks except those that requires the use of binding-specific features and that 
 * handles binding-agnostic OpenGL operations that cannot be easily made type-safe (enable, disable, ...)
 * 
 * @author Julien Gouesse
 *
 */
public abstract class AbstractRenderer implements Renderer {

    protected static final Logger logger = Logger.getLogger(AbstractRenderer.class.getName());

    protected static final boolean VALIDATE_SHADER = false;

    protected final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);

    protected final StringBuilder stringBuf = new StringBuilder(250);

    protected Statistics statistics = new Statistics();

    protected final IntBuffer intBuf1 = BufferUtils.createIntBuffer(1);

    protected final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);

    protected RenderContext context = new RenderContext();

    protected GLObjectManager objManager = new GLObjectManager();

    protected EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);

    protected Shader boundShader;

    protected int initialDrawBuf, initialReadBuf;

    protected int glslVer;

    protected int vertexTextureUnits;

    protected int fragTextureUnits;

    protected int vertexUniforms;

    protected int fragUniforms;

    protected int vertexAttribs;

    protected int maxFBOSamples;

    protected int maxFBOAttachs;

    protected int maxMRTFBOAttachs;

    protected int maxRBSize;

    protected int maxTexSize;

    protected int maxCubeTexSize;

    protected int maxVertCount;

    protected int maxTriCount;

    protected boolean tdc;

    protected int vpX, vpY, vpW, vpH;

    protected FrameBuffer lastFb = null;

    protected Matrix4f worldMatrix = new Matrix4f();

    protected Matrix4f viewMatrix = new Matrix4f();

    protected Matrix4f projMatrix = new Matrix4f();

    protected FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);

    // TODO: these flags should be moved into the Caps flags
    protected boolean powerOf2 = false;

    protected boolean hardwareMips = false;

    protected boolean vbo = false;

    protected boolean framebufferBlit = false;

    protected boolean renderbufferStorageMultisample = false;
    
    protected Helper helper;

    protected FloatBuffer storeMatrix(Matrix4f matrix, FloatBuffer store) {
        store.rewind();
        matrix.fillFloatBuffer(store, true);
        store.rewind();
        return store;
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

    public void cleanup() {
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
    }
    
    public void setBackgroundColor(ColorRGBA color) {
    	helper.setBackgroundColor(color);
    }

    public void resetGLObjects() {
        objManager.resetObjects();
        statistics.clearMemory();
        boundShader = null;
        lastFb = null;
        context.reset();
    }

    public void onFrame() {
        objManager.deleteUnused(this);
        // statistics.clearFrame();
    }

    protected void updateShaderUniforms(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        // for (Uniform uniform : shader.getUniforms()){
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.getValue(i);
            if (uniform.isUpdateNeeded()) {
                updateUniform(shader, uniform);
            }
        }
    }

    protected void updateUniform(Shader shader, Uniform uniform) {
        if (glslVer != -1) {
            int shaderId = shader.getId();

            assert uniform.getName() != null;
            assert shader.getId() > 0;
            if (context.boundShaderProgram != shaderId) {
                helper.useProgram(shaderId);
                statistics.onShaderUse(shader, true);
                boundShader = shader;
                context.boundShaderProgram = shaderId;
            }
            else {
                statistics.onShaderUse(shader, false);
            }

            updateUniformVar(shader, uniform);
        }
    }

    protected void resetUniformLocations(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        // for (Uniform uniform : shader.getUniforms()){
        for (int i = 0; i < uniforms.size(); i++) {
            Uniform uniform = uniforms.getValue(i);
            uniform.reset(); // e.g check location again
        }
    }

    protected void renderMeshDefault(Mesh mesh, int lod, int count) {
        VertexBuffer indices = null;
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        }
        else {
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers) {
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData || vb.getUsage() == Usage.CpuOnly
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            /*if (vb.getBufferType() == Type.Index) {
                indices = vb;
            }
            else {*/
            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttrib(vb);
            }
            else {
                // interleaved
                setVertexAttrib(vb, interleavedData);
            }
            /*}*/
        }

        if (indices != null) {
            drawTriangleList(indices, mesh, count);
        }
        else {
            drawArrays(mesh);
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    protected void renderMeshVBO(Mesh mesh, int lod, int count) {
        VertexBuffer indices = null;
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()) {
            updateBufferData(interleavedData);
        }
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        }
        else {
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers) {
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData || vb.getUsage() == Usage.CpuOnly // ignore
                                                                                             // cpu-only
                                                                                             // buffers
                    || vb.getBufferType() == Type.Index) {
                continue;
            }

            if (vb.getStride() == 0) {
                // not interleaved
                setVertexAttribVBO(vb, null);
            }
            else {
                // interleaved
                setVertexAttribVBO(vb, interleavedData);
            }
        }

        if (indices != null) {
            drawTriangleListVBO(indices, mesh, count);
        }
        else {
            drawArrays(mesh);
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    protected abstract void setVertexAttribVBO(VertexBuffer vb, VertexBuffer idb);

    public abstract void setVertexAttrib(VertexBuffer vb, VertexBuffer idb);

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    public void drawTriangleList(VertexBuffer indexBuf, Mesh mesh, int count) {
        Mesh.Mode mode = mesh.getMode();
        Buffer indexData = indexBuf.getData();
        indexData.clear();
        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            Mode elMode = Mode.Triangles;
            // int elSize = indexBuf.getFormat().getComponentSize();
            // int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++) {
                if (i == stripStart) {
                    elMode = Mode.TriangleStrip;
                }
                else if (i == fanStart) {
                    elMode = Mode.TriangleStrip;
                }
                int elementLength = elementLengths[i];
                indexData.position(curOffset);
                drawElements(elMode, elementLength, indexBuf.getFormat(), indexData);
                curOffset += elementLength;
            }
        }
        else {
            drawElements(mode, indexData.capacity(), indexBuf.getFormat(), indexData);
        }
    }

    protected abstract void drawElements(Mode mode, int count, Format format, Buffer indices);

    protected abstract void drawRangeElements(Mode mode, int start, int end, int count,
            Format format, long indices_buffer_offset);

    protected abstract void drawElementsInstanced(Mode mode, int indices_count, Format format,
            long indices_buffer_offset, int primcount);

    protected abstract void bindElementArrayBuffer(int buffer);

    protected int convertMagFilter(Texture.MagFilter filter) {
        switch (filter) {
            case Bilinear:
                return Helper.Filter.LINEAR.getGLConstant();
            case Nearest:
                return Helper.Filter.NEAREST.getGLConstant();
            default:
                throw new UnsupportedOperationException("Unknown mag filter: " + filter);
        }
    }
    
    public void drawTriangleListVBO(VertexBuffer indexBuf, Mesh mesh, int count) {
        if (indexBuf.getBufferType() != VertexBuffer.Type.Index) {
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");
        }

        if (indexBuf.isUpdateNeeded()) {
            updateBufferData(indexBuf);
        }

        int bufId = indexBuf.getId();
        assert bufId != -1;

        if (context.boundElementArrayVBO != bufId) {
            bindElementArrayBuffer(bufId);
            context.boundElementArrayVBO = bufId;
        }

        int vertCount = mesh.getVertexCount();
        boolean useInstancing = count > 1 && caps.contains(Caps.MeshInstancing);

        if (mesh.getMode() == Mode.Hybrid) {
            int[] modeStart = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            Mode elMode = Mode.Triangles;
            int elSize = indexBuf.getFormat().getComponentSize();
            // int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++) {
                if (i == stripStart) {
                    elMode = Mode.TriangleStrip;
                }
                else if (i == fanStart) {
                    elMode = Mode.TriangleStrip;
                }
                int elementLength = elementLengths[i];

                if (useInstancing) {
                    drawElementsInstanced(elMode, elementLength, indexBuf.getFormat(), curOffset,
                            count);
                }
                else {
                    drawRangeElements(elMode, 0, vertCount, elementLength, indexBuf.getFormat(),
                            curOffset);
                }

                curOffset += elementLength * elSize;
            }
        }
        else {
            if (useInstancing) {
                drawElementsInstanced(mesh.getMode(), indexBuf.getData().capacity(),
                        indexBuf.getFormat(), 0, count);
            }
            else {
                drawRangeElements(mesh.getMode(), 0, vertCount, indexBuf.getData().capacity(),
                        indexBuf.getFormat(), 0);
            }
        }
    }

    protected abstract int convertVertexFormat(VertexBuffer.Format fmt);

    protected abstract int convertFormat(Format format);

    protected abstract int convertElementMode(Mesh.Mode mode);

    protected abstract void drawArrays(Mesh mesh);

    protected abstract void deleteFramebuffer();

    protected abstract void deleteRenderBuffer(FrameBuffer fb, RenderBuffer rb);

    protected abstract void bindFramebuffer(int framebuffer);

    public void deleteFrameBuffer(FrameBuffer fb) {
        if (fb.getId() != -1) {
            if (context.boundFBO == fb.getId()) {
                bindFramebuffer(0);
                context.boundFBO = 0;
            }
            if (fb.getDepthBuffer() != null) {
                deleteRenderBuffer(fb, fb.getDepthBuffer());
            }
            if (fb.getColorBuffer() != null) {
                deleteRenderBuffer(fb, fb.getColorBuffer());
            }

            intBuf1.put(0, fb.getId());
            deleteFramebuffer();
            fb.resetObject();

            statistics.onDeleteFrameBuffer();
        }
    }

    protected int getUniformLocation(Shader shader) {
        return helper.getUniformLocation(shader, stringBuf.toString(),nameBuf);
    }

    protected void updateUniformLocation(Shader shader, Uniform uniform) {
        if (glslVer != -1) {
            stringBuf.setLength(0);
            stringBuf.append(uniform.getName()).append('\0');
            updateNameBuffer();
            int loc = getUniformLocation(shader);
            if (loc < 0) {
                uniform.setLocation(-1);
                // uniform is not declared in shader
                logger.log(Level.WARNING, "Uniform {0} is not declared in shader.",
                        uniform.getName());
            }
            else {
                uniform.setLocation(loc);
            }
        }
    }

    protected abstract void updateUniformVar(Shader shader, Uniform uniform);

    protected abstract void deleteShader(ShaderSource source);

    public void deleteShaderSource(ShaderSource source) {
        if (glslVer != -1) {
            if (source.getId() < 0) {
                logger.warning("Shader source is not uploaded to GPU, cannot delete.");
                return;
            }
            source.setUsable(false);
            source.clearUpdateNeeded();
            deleteShader(source);
            source.resetObject();
        }
    }

    protected abstract boolean isShaderValid(Shader shader);

    public void setShader(Shader shader) {
        if (glslVer != -1) {
            if (shader == null) {
                if (context.boundShaderProgram > 0) {
                    helper.useProgram(0);
                    statistics.onShaderUse(null, true);
                    context.boundShaderProgram = 0;
                    boundShader = null;
                }
            }
            else {
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
                if (context.boundShaderProgram != shader.getId()) {
                    if (VALIDATE_SHADER) {
                        // check if shader can be used
                        // with current state
                        boolean validateOK = isShaderValid(shader);
                        if (validateOK) {
                            logger.fine("shader validate success");
                        }
                        else {
                            logger.warning("shader validate failure");
                        }
                    }

                    helper.useProgram(shader.getId());
                    statistics.onShaderUse(shader, true);
                    context.boundShaderProgram = shader.getId();
                    boundShader = shader;
                }
                else {
                    statistics.onShaderUse(shader, false);
                }
            }
        }
    }

    public abstract void updateShaderData(Shader shader);

    protected abstract void bindDrawFramebuffer(int framebuffer);

    protected abstract void bindReadFramebuffer(int framebuffer);

    protected abstract void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0,
            int dstY0, int dstX1, int dstY1, int mask, int filter);

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
        if (framebufferBlit) {
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
                bindReadFramebuffer(0);
                // srcW = viewWidth;
                // srcH = viewHeight;
            }
            else {
                bindReadFramebuffer(src.getId());
                srcW = src.getWidth();
                srcH = src.getHeight();
            }
            if (dst == null) {
                bindDrawFramebuffer(0);
                // dstW = viewWidth;
                // dstH = viewHeight;
            }
            else {
                bindDrawFramebuffer(dst.getId());
                dstW = dst.getWidth();
                dstH = dst.getHeight();
            }
            blitFramebuffer(0, 0, srcW, srcH, 0, 0, dstW, dstH, Helper.BufferBit.COLOR_BUFFER.getGLConstant()
                    | Helper.BufferBit.DEPTH_BUFFER.getGLConstant(), Helper.Filter.NEAREST.getGLConstant());

            bindFramebuffer(prevFBO);
            try {
                checkFrameBufferError();
            }
            catch (IllegalStateException ex) {
                logger.log(Level.SEVERE, "Source FBO:\n{0}", src);
                logger.log(Level.SEVERE, "Dest FBO:\n{0}", dst);
                throw ex;
            }
        }
        else {
            throw new UnsupportedOperationException("EXT_framebuffer_blit required.");
            // TODO: support non-blit copies?
        }
    }

    protected abstract void checkFrameBufferError();

    protected abstract int genFramebufferId();

    public void updateFrameBuffer(FrameBuffer fb) {
        int id = fb.getId();
        if (id == -1) {
            // create FBO
            id = genFramebufferId();
            fb.setId(id);
            objManager.registerForCleanup(fb);

            statistics.onNewFrameBuffer();
        }

        if (context.boundFBO != id) {
            bindFramebuffer(id);
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

    public abstract void updateFrameBufferAttachment(FrameBuffer fb, RenderBuffer rb);
    
    protected abstract void deleteTexture();
    
    public void deleteTexture(Texture tex) {      
        int texId = tex.getId();
        if (texId != -1) {
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);
            deleteTexture();
            tex.resetObject();
        }
    }
    
    protected abstract int convertTextureType(Texture.Type type);
    
    public void clearTextureUnits() {
        IDList textureList = context.textureIndexList;
        Texture[] textures = context.boundTextures;
        for (int i = 0; i < textureList.oldLen; i++) {
            int idx = textureList.oldList[i];

            if (context.boundTextureUnit != idx) {
                setActiveTexture(Helper.TEXTURE0 + idx);
                context.boundTextureUnit = idx;
            }
            disable(convertTextureType(textures[idx].getType()));
            textures[idx] = null;
        }
        context.textureIndexList.copyNewToOld();
    }
    
    protected abstract void enable(int cap);
    
    protected abstract void disable(int cap);
    
    protected abstract void setActiveTexture(int unit);
    
    protected abstract void bindTexture(int type,int id);
    
    public void clearClipRect() {
        if (context.clipRectEnabled) {
            disable(Helper.TEXTURE0);
            context.clipRectEnabled = false;
        }
    }
    
    protected abstract void disableClientState(int cap);
    
    public void clearVertexAttribs() {
        for (int i = 0; i < 16; i++) {
            VertexBuffer vb = context.boundAttribs[i];
            if (vb != null) {
                int arrayType = convertArrayType(vb.getBufferType());               
                disableClientState(arrayType);
                context.boundAttribs[vb.getBufferType().ordinal()] = null;
            }
        }
    }
    
    protected abstract int convertArrayType(VertexBuffer.Type type);
    
    protected abstract void deleteBuffer();
    
    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1) {
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);
            deleteBuffer();
            vb.resetObject();
        }
    }
    
    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        if (glslVer == -1) {
            this.viewMatrix.set(viewMatrix);
            this.projMatrix.set(projMatrix);
            if (context.matrixMode != Helper.MatrixMode.PROJECTION.getGLConstant()) {
                helper.setMatrixMode(Helper.MatrixMode.PROJECTION);
                context.matrixMode = Helper.MatrixMode.PROJECTION.getGLConstant();
            }
            helper.loadMatrixf(storeMatrix(projMatrix, fb16));
        }
    }
    
    public void setWorldMatrix(Matrix4f worldMatrix) {
        if (glslVer == -1) {
            this.worldMatrix.set(worldMatrix);
            if (context.matrixMode != Helper.MatrixMode.MODELVIEW.getGLConstant()) {
                helper.setMatrixMode(Helper.MatrixMode.MODELVIEW);
                context.matrixMode = Helper.MatrixMode.MODELVIEW.getGLConstant();
            }
            helper.loadMatrixf(storeMatrix(viewMatrix, fb16));
            helper.multMatrixf(storeMatrix(worldMatrix, fb16));
        }
    }
    
    public void setViewPort(int x, int y, int width, int height) {
        helper.setViewPort(x, y, width, height);
        vpX = x;
        vpY = y;
        vpW = width;
        vpH = height;
    }
    
    public void setDepthRange(float start, float end) {
    	helper.setDepthRange(start, end);
    }
}
