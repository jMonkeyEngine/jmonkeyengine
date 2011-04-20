package com.jme3.renderer.lwjgl;

import com.jme3.math.FastMath;
import com.jme3.renderer.GL1Renderer;
import com.jme3.shader.Shader;
import com.jme3.shader.Shader.ShaderSource;
import com.jme3.texture.FrameBuffer;
import java.nio.ShortBuffer;
import java.nio.Buffer;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.IntMap;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.texture.Texture;
import com.jme3.light.LightList;
import com.jme3.material.FixedFuncBinding;
import java.nio.FloatBuffer;
import com.jme3.math.Matrix4f;
import java.util.logging.Level;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Caps;
import com.jme3.renderer.GLObjectManager;
import com.jme3.renderer.RenderContext;
import com.jme3.renderer.Statistics;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;
import java.util.logging.Logger;
import jme3tools.converters.MipMapGenerator;
import org.lwjgl.opengl.GLContext;

import static org.lwjgl.opengl.GL11.*;

public class LwjglGL1Renderer implements GL1Renderer {

    private static final Logger logger = Logger.getLogger(LwjglRenderer.class.getName());

    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final IntBuffer ib1 = BufferUtils.createIntBuffer(1);
    private final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private final FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);
    private final RenderContext context = new RenderContext();
    private final GLObjectManager objManager = new GLObjectManager();
    private final EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);

    private int maxTexSize;
    private int maxCubeTexSize;
    private int maxVertCount;
    private int maxTriCount;

    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;

//    private Matrix4f worldMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
//    private Matrix4f projMatrix = new Matrix4f();

    private boolean colorSet = false;
    private boolean materialSet = false;

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
        //glDisable(GL_DEPTH_TEST);
        glShadeModel(GL_SMOOTH);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

        if (GLContext.getCapabilities().GL_ARB_texture_non_power_of_two){
            caps.add(Caps.NonPowerOfTwoTextures);
        }else{
            logger.log(Level.WARNING, "Your graphics card does not "
                                    + "support non-power-of-2 textures. "
                                    + "Some features might not work.");
        }
    }

    public void invalidateState(){
        context.reset();
    }

    public void resetGLObjects() {
        colorSet = false;

        objManager.resetObjects();
        statistics.clearMemory();
        context.reset();
    }

    public void cleanup() {
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
    }

    public void setDepthRange(float start, float end) {
        glDepthRange(start, end);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            bits = GL_COLOR_BUFFER_BIT;
        }
        if (depth) {
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

    private void setMaterialColor(int type, ColorRGBA color){
        if (!materialSet){
            materialSet = true;
            glEnable(GL_COLOR_MATERIAL);
        }

        fb16.clear();
        fb16.put(color.r).put(color.g).put(color.b).put(color.a);
        fb16.clear();
        glMaterial(GL_FRONT_AND_BACK, type, fb16);
    }

    public void setFixedFuncBinding(FixedFuncBinding ffBinding, Object val){
        switch (ffBinding){
            case Color:
                ColorRGBA color = (ColorRGBA) val;
                glColor4f(color.r, color.g, color.b, color.a);
                colorSet = true;
                break;
            case MaterialAmbient:
                ColorRGBA ambient = (ColorRGBA) val;
                setMaterialColor(GL_AMBIENT, ambient);
                break;
            case MaterialDiffuse:
                ColorRGBA diffuse = (ColorRGBA) val;
                setMaterialColor(GL_DIFFUSE, diffuse);
                break;
            case MaterialSpecular:
                ColorRGBA specular = (ColorRGBA) val;
                setMaterialColor(GL_SPECULAR, specular);
                break;
        }
    }

    public void clearSetFixedFuncBindings(){
        if (colorSet){
            glColor4f(1,1,1,1);
            colorSet = false;
        }
        if (materialSet){
            glDisable(GL_COLOR_MATERIAL);
            materialSet = false; // TODO: not efficient
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

        if (state.isPointSprite()){
            logger.log(Level.WARNING, "Point Sprite unsupported!");
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

    }

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

    private FloatBuffer storeMatrix(Matrix4f matrix, FloatBuffer store) {
        store.clear();
        matrix.fillFloatBuffer(store, true);
        store.clear();
        return store;
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
        if (context.matrixMode != GL_MODELVIEW) {
            glMatrixMode(GL_MODELVIEW);
            context.matrixMode = GL_MODELVIEW;
        }

        glLoadMatrix(storeMatrix(viewMatrix, fb16));
        glMultMatrix(storeMatrix(worldMatrix, fb16));
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        if (context.matrixMode != GL_PROJECTION) {
            glMatrixMode(GL_PROJECTION);
            context.matrixMode = GL_PROJECTION;
        }

        storeMatrix(projMatrix, fb16);
        glLoadMatrix(fb16);

        this.viewMatrix.set(viewMatrix);
    }

    public void setLighting(LightList list) {
        if (list == null || list.size() == 0) {
            // turn off lighting
            //glDisable(GL_LIGHTING);
            return;
        }

//        glEnable(GL_LIGHTING);

        //TODO: ...
    }

    private int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return GL_TEXTURE_2D;
//            case ThreeDimensional:
//                return GL_TEXTURE_3D;
//            case CubeMap:
//                return GL_TEXTURE_CUBE_MAP;
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
            case EdgeClamp:
            case Clamp:
                return GL_CLAMP;
            case Repeat:
                return GL_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    private void setupTextureParams(Texture tex) {
        int target = convertTextureType(tex.getType());

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);

        // repeat modes
        switch (tex.getType()) {
//            case ThreeDimensional:
//            case CubeMap:
//                glTexParameteri(target, GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
                glTexParameteri(target, GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                glTexParameteri(target, GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }
    }

    public void updateTexImageData(Image img, Texture.Type type, boolean mips, int unit) {
        int texId = img.getId();
        if (texId == -1) {
            // create texture
            glGenTextures(ib1);
            texId = ib1.get(0);
            img.setId(texId);
            objManager.registerForCleanup(img);

            statistics.onNewTexture();
        }

        // bind texture
        int target = convertTextureType(type);
//        if (context.boundTextureUnit != unit) {
//            glActiveTexture(GL_TEXTURE0 + unit);
//            context.boundTextureUnit = unit;
//        }
        if (context.boundTextures[unit] != img) {
            glEnable(target);
            glBindTexture(target, texId);
            context.boundTextures[unit] = img;

            statistics.onTextureUse(img, true);
        }

        // Check sizes if graphics card doesn't support NPOT
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two){
            if (img.getWidth() != 0 && img.getHeight() != 0){
                if (!FastMath.isPowerOfTwo(img.getWidth())
                    || !FastMath.isPowerOfTwo(img.getHeight())
                    || img.getWidth() != img.getHeight()){
                    
                    // Resize texture to Power-of-2 size
                    MipMapGenerator.resizeToPowerOf2(img);

                }
            }
        }

        if (!img.hasMipmaps() && mips) {
            // No pregenerated mips available,
            // generate from base level if required
//            glTexParameteri(target, GL_GENERATE_MIPMAP, GL_TRUE);
            // TODO: Generate mipmaps here
            MipMapGenerator.generateMipMaps(img);
        } else {
        }

        /*
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
        } else {*/
            TextureUtil.uploadTexture(img, target, 0, 0, false);
        //}

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        if (unit != 0 || tex.getType() != Texture.Type.TwoDimensional){
            //throw new UnsupportedOperationException();
            return;
        }

        Image image = tex.getImage();
        if (image.isUpdateNeeded()) {
            updateTexImageData(image, tex.getType(), tex.getMinFilter().usesMipMapLevels(), unit);
        }

        int texId = image.getId();
        assert texId != -1;

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType());
//        if (!context.textureIndexList.moveToNew(unit)) {
//             if (context.boundTextureUnit != unit){
//                glActiveTexture(GL_TEXTURE0 + unit);
//                context.boundTextureUnit = unit;
//             }
//             glEnable(type);
//        }

//        if (context.boundTextureUnit != unit) {
//            glActiveTexture(GL_TEXTURE0 + unit);
//            context.boundTextureUnit = unit;
//        }

        if (textures[unit] != image) {
            glEnable(type);
            glBindTexture(type, texId);
            textures[unit] = image;

            statistics.onTextureUse(image, true);
        } else {
            statistics.onTextureUse(image, false);
        }

        setupTextureParams(tex);
    }

    private void checkTexturingUsed() {
        Image[] textures = context.boundTextures;
        if (textures[0] != null){
            glDisable(GL_TEXTURE_2D);
            textures[0] = null;
        }
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            ib1.put(0, texId);
            ib1.position(0).limit(1);
            glDeleteTextures(ib1);
            image.resetObject();
        }
    }

    private int convertArrayType(VertexBuffer.Type type) {
        switch (type) {
            case Position:
                return GL_VERTEX_ARRAY;
            case Normal:
                return GL_NORMAL_ARRAY;
            case TexCoord:
                return GL_TEXTURE_COORD_ARRAY;
            case Color:
                return GL_COLOR_ARRAY;
            default:
                return -1; // unsupported
        }
    }

    private int convertVertexFormat(VertexBuffer.Format fmt) {
        switch (fmt) {
            case Byte:
                return GL_BYTE;
            case Float:
                return GL_FLOAT;
            case Int:
                return GL_INT;
            case Short:
                return GL_SHORT;
            case UnsignedByte:
                return GL_UNSIGNED_BYTE;
            case UnsignedInt:
                return GL_UNSIGNED_INT;
            case UnsignedShort:
                return GL_UNSIGNED_SHORT;
            default:
                throw new UnsupportedOperationException("Unrecognized vertex format: " + fmt);
        }
    }

    private int convertElementMode(Mesh.Mode mode) {
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

    public void drawTriangleArray(Mesh.Mode mode, int count, int vertCount) {
        if (count > 1)
            throw new UnsupportedOperationException();
        
        glDrawArrays(convertElementMode(mode), 0, vertCount);
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1) {
            return; // unsupported
        }
        glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal) {
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled) {
                glEnable(GL_NORMALIZE);
                context.normalizeEnabled = true;
            } else if (!vb.isNormalized() && context.normalizeEnabled) {
                glDisable(GL_NORMALIZE);
                context.normalizeEnabled = false;
            }
        }

        // NOTE: Use data from interleaved buffer if specified
        Buffer data = idb != null ? idb.getData() : vb.getData();
        int comps = vb.getNumComponents();
        int type = convertVertexFormat(vb.getFormat());

        data.rewind();

        switch (vb.getBufferType()) {
            case Position:
                if (!(data instanceof FloatBuffer))
                    throw new UnsupportedOperationException();

                glVertexPointer(comps, vb.getStride(), (FloatBuffer) data);
                break;
            case Normal:
                if (!(data instanceof FloatBuffer))
                    throw new UnsupportedOperationException();

                glNormalPointer(vb.getStride(), (FloatBuffer)data);
                break;
            case Color:
                if (data instanceof FloatBuffer){
                    glColorPointer(comps, vb.getStride(), (FloatBuffer)data);
                }else if (data instanceof ByteBuffer){
                    glColorPointer(comps, true, vb.getStride(), (ByteBuffer)data);
                }else{
                    throw new UnsupportedOperationException();
                }
                break;
            case TexCoord:
                if (!(data instanceof FloatBuffer))
                    throw new UnsupportedOperationException();

                glTexCoordPointer(comps, vb.getStride(), (FloatBuffer)data);
                break;
            default:
                // Ignore, this is an unsupported attribute for OpenGL1.
                break;
        }
    }

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    private void drawElements(int mode, int format, Buffer data){
        switch (format){
            case GL_UNSIGNED_BYTE:
                glDrawElements(mode, (ByteBuffer)data);
                break;
            case GL_UNSIGNED_SHORT:
                glDrawElements(mode, (ShortBuffer)data);
                break;
            case GL_UNSIGNED_INT:
                glDrawElements(mode, (IntBuffer)data);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void drawTriangleList(VertexBuffer indexBuf, Mesh mesh, int count) {
        Mesh.Mode mode = mesh.getMode();

        Buffer indexData = indexBuf.getData();
        indexData.rewind();

        if (mesh.getMode() == Mode.Hybrid) {
            throw new UnsupportedOperationException();
            /*
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

                drawElements(elMode,
                             fmt,
                             indexData);
                
                curOffset += elementLength;
            }*/
        } else {
            drawElements(convertElementMode(mode),
                         convertVertexFormat(indexBuf.getFormat()),
                         indexData);
        }
    }

    

    public void clearVertexAttribs() {
        for (int i = 0; i < 16; i++) {
            VertexBuffer vb = context.boundAttribs[i];
            if (vb != null) {
                int arrayType = convertArrayType(vb.getBufferType());
                glDisableClientState(arrayType);
                context.boundAttribs[vb.getBufferType().ordinal()] = null;
            }
        }
    }

    private void renderMeshDefault(Mesh mesh, int lod, int count) {
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
                setVertexAttrib(vb);
            } else {
                // interleaved
                setVertexAttrib(vb, interleavedData);
            }
        }

        if (indices != null) {
            drawTriangleList(indices, mesh, count);
        } else {
            glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        checkTexturingUsed();
        clearSetFixedFuncBindings();
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
    	if (mesh.getVertexCount() == 0)
            return;

        if (context.pointSize != mesh.getPointSize()) {
            glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }

        boolean dynamic = false;
        if (mesh.getBuffer(Type.InterleavedData) != null)
            throw new UnsupportedOperationException();

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

        statistics.onMeshDrawn(mesh, lod);
        
//        if (!dynamic) {
            // dealing with a static object, generate display list
//            renderMeshDisplayList(mesh);
//        } else {
            renderMeshDefault(mesh, lod, count);
//        }


    }

    public void setAlphaToCoverage(boolean value) {
    }

    public void setShader(Shader shader) {
    }

    public void deleteShader(Shader shader) {
    }

    public void deleteShaderSource(ShaderSource source) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    public void setFrameBuffer(FrameBuffer fb) {
    }

    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    public void updateBufferData(VertexBuffer vb) {
    }

    public void deleteBuffer(VertexBuffer vb) {
    }

}
