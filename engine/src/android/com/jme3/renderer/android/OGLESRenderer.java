package com.jme3.renderer.android;

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
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Logger;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public final class OGLESRenderer implements Renderer {

    private static final Logger logger = Logger.getLogger(OGLESRenderer.class.getName());

    private Matrix4f worldMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f projMatrix = new Matrix4f();
    private FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);
    private float[] fa16 = new float[16];
    private IntBuffer intBuf1 = BufferUtils.createIntBuffer(1);
    private GL10 gl;
    private GL11 gl11;
    private GL11Ext glExt;

    private RenderContext context = new RenderContext();
    private GLObjectManager objManager = new GLObjectManager();

    private EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    private boolean powerOf2 = false;

    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;

    public OGLESRenderer(GL10 gl){
        setGL(gl);
    }

    public void setGL(GL10 gl){
        this.gl = gl;
        if (gl instanceof GL11){
            gl11 = (GL11) gl;
            if (gl instanceof GL11Ext){
                glExt = (GL11Ext) gl;
            }
        }
    }

    public void initialize(){
        logger.info("Vendor: "+gl.glGetString(gl.GL_VENDOR));
        logger.info("Renderer: "+gl.glGetString(gl.GL_RENDERER));
        logger.info("Version: "+gl.glGetString(gl.GL_VERSION));

        String extensions = gl.glGetString(gl.GL_EXTENSIONS);
        if (extensions.contains("GL_OES_texture_npot"))
            powerOf2 = true;
        
        applyRenderState(RenderState.DEFAULT);
//        gl.glClearDepthf(1.0f);
        gl.glDisable(GL10.GL_DITHER);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);
    }

    public Statistics getStatistics() {
        return null;
    }

    public void setClipRect(int x, int y, int width, int height) {
    }

    public void clearClipRect() {
    }

    public void resetGLObjects() {
    }

    public EnumSet<Caps> getCaps() {
        return caps;
    }

    public void setBackgroundColor(ColorRGBA color) {
        gl.glClearColor(color.r, color.g, color.b, color.a);
    }

    public void cleanup(){
        objManager.deleteAllObjects(this);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) bits = gl.GL_COLOR_BUFFER_BIT;
        if (depth) bits |= gl.GL_DEPTH_BUFFER_BIT;
        if (stencil) bits |= gl.GL_STENCIL_BUFFER_BIT;
        if (bits != 0) gl.glClear(bits);
    }

    public void applyRenderState(RenderState state){
        // TODO: is wireframe supported under OGL ES?

//        if (state.isWireframe() && !context.wireframe){
//            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
//            context.wireframe = true;
//        }else if (!state.isWireframe() && context.wireframe){
//            gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
//            context.wireframe = false;
//        }

        if (state.isDepthTest() && !context.depthTestEnabled){
            gl.glEnable(gl.GL_DEPTH_TEST);
            gl.glDepthFunc(gl.GL_LEQUAL);
            context.depthTestEnabled = true;
        }else if (!state.isDepthTest() && context.depthTestEnabled){
            gl.glDisable(gl.GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }
        if (state.isAlphaTest() && !context.alphaTestEnabled){
            gl.glEnable(gl.GL_ALPHA_TEST);
            gl.glAlphaFunc(gl.GL_GREATER, state.getAlphaFallOff());
            context.alphaTestEnabled = true;
        }else if (!state.isAlphaTest() && context.alphaTestEnabled){
            gl.glDisable(gl.GL_ALPHA_TEST);
            context.alphaTestEnabled = false;
        }
        if (state.isDepthWrite() && !context.depthWriteEnabled){
            gl.glDepthMask(true);
            context.depthWriteEnabled = true;
        }else if (!state.isDepthWrite() && context.depthWriteEnabled){
            gl.glDepthMask(false);
            context.depthWriteEnabled = false;
        }
        if (state.isColorWrite() && !context.colorWriteEnabled){
            gl.glColorMask(true,true,true,true);
            context.colorWriteEnabled = true;
        }else if (!state.isColorWrite() && context.colorWriteEnabled){
            gl.glColorMask(false,false,false,false);
            context.colorWriteEnabled = false;
        }
        if (state.isPolyOffset()){
            if (!context.polyOffsetEnabled){
                gl.glEnable(gl.GL_POLYGON_OFFSET_FILL);
                gl.glPolygonOffset(state.getPolyOffsetFactor(),
                                state.getPolyOffsetUnits());
                context.polyOffsetEnabled = true;
                context.polyOffsetFactor = state.getPolyOffsetFactor();
                context.polyOffsetUnits = state.getPolyOffsetUnits();
            }else{
                if (state.getPolyOffsetFactor() != context.polyOffsetFactor
                 || state.getPolyOffsetUnits() != context.polyOffsetUnits){
                    gl.glPolygonOffset(state.getPolyOffsetFactor(),
                                    state.getPolyOffsetUnits());
                    context.polyOffsetFactor = state.getPolyOffsetFactor();
                    context.polyOffsetUnits = state.getPolyOffsetUnits();
                }
            }
        }else{
            if (context.polyOffsetEnabled){
                gl.glDisable(gl.GL_POLYGON_OFFSET_FILL);
                context.polyOffsetEnabled = false;
                context.polyOffsetFactor = 0;
                context.polyOffsetUnits = 0;
            }
        }
        if (state.getFaceCullMode() != context.cullMode){
            if (state.getFaceCullMode() == RenderState.FaceCullMode.Off)
                gl.glDisable(gl.GL_CULL_FACE);
            else
                gl.glEnable(gl.GL_CULL_FACE);

            switch (state.getFaceCullMode()){
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
                    throw new UnsupportedOperationException("Unrecognized face cull mode: "+
                                                            state.getFaceCullMode());
            }

            context.cullMode = state.getFaceCullMode();
        }

        if (state.getBlendMode() != context.blendMode){
            if (state.getBlendMode() == RenderState.BlendMode.Off)
                gl.glDisable(gl.GL_BLEND);
            else
                gl.glEnable(gl.GL_BLEND);

            switch (state.getBlendMode()){
                case Off:
                    break;
                case Additive:
                    gl.glBlendFunc(gl.GL_ONE, gl.GL_ONE);
                    break;
                case Alpha:
                    gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA);
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
                    throw new UnsupportedOperationException("Unrecognized blend mode: "+
                                                            state.getBlendMode());
            }

            context.blendMode = state.getBlendMode();
        }
    }

    public void onFrame() {
        objManager.deleteUnused(this);
    }

    public void setDepthRange(float start, float end) {
        gl.glDepthRangef(start, end);
    }

    public void setViewPort(int x, int y, int width, int height){
        gl.glViewport(x, y, width, height);
        vpX = x;
        vpY = y;
        vpW = width;
        vpH = height;
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix){
        this.viewMatrix.set(viewMatrix);
        this.projMatrix.set(projMatrix);

        if (context.matrixMode != gl.GL_PROJECTION){
            gl.glMatrixMode(gl.GL_PROJECTION);
            context.matrixMode = gl.GL_PROJECTION;
        }

        projMatrix.fillFloatArray(fa16, true);
        gl.glLoadMatrixf(fa16, 0);

//        gl.glMatrixMode(gl.GL_MODELVIEW);
//        gl.glLoadIdentity();
//        gl.glLoadMatrixf(storeMatrix(viewMatrix, fb16));
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
        this.worldMatrix.set(worldMatrix);

        if (context.matrixMode != gl.GL_MODELVIEW){
            gl.glMatrixMode(gl.GL_MODELVIEW);
            context.matrixMode = gl.GL_MODELVIEW;
        }

        viewMatrix.fillFloatArray(fa16, true);
        gl.glLoadMatrixf(fa16, 0);
        worldMatrix.fillFloatArray(fa16, true);
        gl.glMultMatrixf(fa16, 0);
    }

    public void setMatrixPalette(Matrix4f[] offsetMatrices){
        if (glExt == null)
            throw new UnsupportedOperationException("Requires GL_OES_compressed_paletted_texture");

        if (context.matrixMode != glExt.GL_MATRIX_PALETTE_OES){
            gl.glMatrixMode(glExt.GL_MATRIX_PALETTE_OES);
            context.matrixMode = glExt.GL_MATRIX_PALETTE_OES;
        }
        
        for (int i = 0; i < offsetMatrices.length; i++){
            Matrix4f offsetMat = offsetMatrices[i];
            glExt.glCurrentPaletteMatrixOES(i);

            offsetMat.fillFloatArray(fa16, true);
            gl.glLoadMatrixf(fa16, 0);
        }
    }

    public void setLighting(LightList list) {
        if (list.size() == 0) {
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
        gl.glPushMatrix();
        gl.glLoadIdentity();

        for (int i = 0; i < list.size()+1; i++){
            if (list.size() <= i){
                // goes beyond the num lights we need
                // disable it
                gl.glDisable(gl.GL_LIGHT0 + i);
                break;
            }

            Light l = list.get(i);
            int lightId = gl.GL_LIGHT0 + i;

            ColorRGBA color = l.getColor();
            color.toArray(temp);

            gl.glEnable(lightId);
            gl.glLightfv(lightId, gl.GL_DIFFUSE,  temp, 0);
            gl.glLightfv(lightId, gl.GL_SPECULAR, temp, 0);

            ColorRGBA.Black.toArray(temp);
            gl.glLightfv(lightId, gl.GL_AMBIENT,  temp, 0);

            switch (l.getType()){
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
        gl.glPopMatrix();
    }

    public void updateShaderSourceData(ShaderSource source) {
    }

    public void deleteShaderSource(ShaderSource source) {
    }

    public void updateShaderData(Shader shader) {
    }

    public void setShader(Shader shader) {
    }

    public void deleteShader(Shader shader) {
    }

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst) {
    }

    public void setFrameBuffer(FrameBuffer fb) {
    }

    public void updateFrameBuffer(FrameBuffer fb) {
    }

    public void deleteFrameBuffer(FrameBuffer fb) {
    }

    /**
     * Warning: documentation states that this method returns data in BGRA format,
     * it actually returns data in RGBA format.
     * @param fb
     * @param byteBuf
     */
    public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
        if (fb == null){
            gl.glReadPixels(vpX, vpY, vpW, vpH, gl.GL_RGBA, gl.GL_UNSIGNED_BYTE, byteBuf);
        }else{
            throw new UnsupportedOperationException();
        }
    }

    private int convertTextureType(Texture.Type type){
        switch (type){
            case TwoDimensional:
                return gl.GL_TEXTURE_2D;
            default:
                throw new UnsupportedOperationException("Unknown texture type: "+type);
        }
    }

    private int convertMagFilter(Texture.MagFilter filter){
        switch (filter){
            case Bilinear:
                return gl.GL_LINEAR;
            case Nearest:
                return gl.GL_NEAREST;
            default:
                throw new UnsupportedOperationException("Unknown mag filter: "+filter);
        }
    }

    private int convertMinFilter(Texture.MinFilter filter){
        switch (filter){
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
                throw new UnsupportedOperationException("Unknown min filter: "+filter);
        }
    }

    private int convertWrapMode(Texture.WrapMode mode){
        switch (mode){
//            case BorderClamp:
//                return gl.GL_CLAMP_TO_BORDER;
//            case Clamp:
//                return gl.GL_CLAMP;
            case EdgeClamp:
                return gl.GL_CLAMP_TO_EDGE;
            case Repeat:
                return gl.GL_REPEAT;
//            case MirroredRepeat:
//                return gl.GL_MIRRORED_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: "+mode);
        }
    }

    public void updateTextureData(Texture tex){
        int texId = tex.getId();
        if (texId == -1){
            // create texture
            gl.glGenTextures(1, intBuf1);
            texId = intBuf1.get(0);
            tex.setId(texId);
            objManager.registerForCleanup(tex);
        }

        // bind texture
        int target = convertTextureType(tex.getType());
        if (context.boundTextures[0] != tex){
            if (context.boundTextureUnit != 0){
                gl.glActiveTexture(gl.GL_TEXTURE0);
                context.boundTextureUnit = 0;
            }

            gl.glBindTexture(target, texId);
            context.boundTextures[0] = tex;
        }

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        gl.glTexParameterx(target, gl.GL_TEXTURE_MIN_FILTER, minFilter);
		gl.glTexParameterx(target, gl.GL_TEXTURE_MAG_FILTER, magFilter);

        // repeat modes
        switch (tex.getType()){
            case TwoDimensional:
                gl.glTexParameterx(target, gl.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                gl.glTexParameterx(target, gl.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: "+tex.getType());
        }

        Image img = tex.getImage();
        if (img != null){
            boolean generateMips = false;
            if (!img.hasMipmaps() && tex.getMinFilter().usesMipMapLevels()){
                // No pregenerated mips available,
                // generate from base level if required
                if (gl11 != null){
                    gl.glTexParameterx(target, GL11.GL_GENERATE_MIPMAP, gl.GL_TRUE);
                }else{
                    generateMips = true;
                }
            }
            
            TextureUtil.uploadTexture(gl, img, tex.getImageDataIndex(), generateMips, powerOf2);
        }

        tex.clearUpdateNeeded();
    }

    private void checkTexturingUsed(){
        IDList textureList = context.textureIndexList;
        // old mesh used texturing, new mesh doesn't use it
        // should actually go through entire oldLen and
        // disable texturing for each unit.. but that's for later.
        if (textureList.oldLen > 0 && textureList.newLen == 0){
            gl.glDisable(gl.GL_TEXTURE_2D);
        }
    }

    public void setTexture(int unit, Texture tex){
         if (tex.isUpdateNeeded())
            updateTextureData(tex);

         int texId = tex.getId();
         assert texId != -1;

         Texture[] textures = context.boundTextures;

         int type = convertTextureType(tex.getType());
         if (!context.textureIndexList.moveToNew(unit)){
             if (context.boundTextureUnit != unit){
                gl.glActiveTexture(gl.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
             }

             gl.glEnable(type);
         }

         if (textures[unit] != tex){
             if (context.boundTextureUnit != unit){
                gl.glActiveTexture(gl.GL_TEXTURE0 + unit);
                context.boundTextureUnit = unit;
             }

             gl.glBindTexture(type, texId);
             textures[unit] = tex;
         }
    }

    public void clearTextureUnits(){
        IDList textureList = context.textureIndexList;
        Texture[] textures = context.boundTextures;
        for (int i = 0; i < textureList.oldLen; i++){
            int idx = textureList.oldList[i];

            if (context.boundTextureUnit != idx){
                gl.glActiveTexture(gl.GL_TEXTURE0 + idx);
                context.boundTextureUnit = idx;
            }
            gl.glDisable(convertTextureType(textures[idx].getType()));
            textures[idx] = null;
        }
        context.textureIndexList.copyNewToOld();
    }

    public void deleteTexture(Texture tex){
        int texId = tex.getId();
        if (texId != -1){
            intBuf1.put(0, texId);
            intBuf1.position(0).limit(1);
            gl.glDeleteTextures(1, intBuf1);
            tex.resetObject();
        }
    }

    private int convertUsage(Usage usage){
        switch (usage){
            case Static:
                return gl11.GL_STATIC_DRAW;
            case Dynamic:
            case Stream:
                return gl11.GL_DYNAMIC_DRAW;
            default:
                throw new RuntimeException("Unknown usage type: "+usage);
        }
    }

    public void updateBufferData(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId == -1){
            // create buffer
            gl11.glGenBuffers(1, intBuf1);
            bufId = intBuf1.get(0);
            vb.setId(bufId);
            objManager.registerForCleanup(vb);
        }

        int target;
        if (vb.getBufferType() == VertexBuffer.Type.Index){
            target = gl11.GL_ELEMENT_ARRAY_BUFFER;
            if (context.boundElementArrayVBO != bufId){
                gl11.glBindBuffer(target, bufId);
                context.boundElementArrayVBO = bufId;
            }
        }else{
            target = gl11.GL_ARRAY_BUFFER;
            if (context.boundArrayVBO != bufId){
                gl11.glBindBuffer(target, bufId);
                context.boundArrayVBO = bufId;
            }
        }

        int usage = convertUsage(vb.getUsage());
        Buffer data = vb.getData();
        data.rewind();

        gl11.glBufferData(target,
                          data.capacity() * vb.getFormat().getComponentSize(),
                          data,
                          usage);
        
        vb.clearUpdateNeeded();
    }

    public void deleteBuffer(VertexBuffer vb) {
        int bufId = vb.getId();
        if (bufId != -1){
            // delete buffer
            intBuf1.put(0, bufId);
            intBuf1.position(0).limit(1);
            gl11.glDeleteBuffers(1, intBuf1);
            vb.resetObject();
        }
    }

    private int convertArrayType(VertexBuffer.Type type){
        switch (type){
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

    private int convertVertexFormat(VertexBuffer.Format fmt){
        switch (fmt){
            case Byte:
                return gl.GL_BYTE;
            case Float:
                return gl.GL_FLOAT;
            case Short:
                return gl.GL_SHORT;
            case UnsignedByte:
                return gl.GL_UNSIGNED_BYTE;
            case UnsignedShort:
                return gl.GL_UNSIGNED_SHORT;
            case Int:
                return gl.GL_FIXED;
            default:
                throw new UnsupportedOperationException("Unrecognized vertex format: "+fmt);
        }
    }

    private int convertElementMode(Mesh.Mode mode){
        switch (mode){
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
                throw new UnsupportedOperationException("Unrecognized mesh mode: "+mode);
        }
    }

    private void setVertexAttribVBO(VertexBuffer vb, VertexBuffer idb){
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1)
            return; // unsupported

        if (vb.isUpdateNeeded() && idb == null)
            updateBufferData(vb);

        int bufId = idb != null ? idb.getId() : vb.getId();
        if (context.boundArrayVBO != bufId){
            gl11.glBindBuffer(gl11.GL_ARRAY_BUFFER, bufId);
            context.boundArrayVBO = bufId;
        }

        gl.glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal){
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled){
                gl.glEnable(gl.GL_NORMALIZE);
                context.normalizeEnabled = true;
            }else if (!vb.isNormalized() && context.normalizeEnabled){
                gl.glDisable(gl.GL_NORMALIZE);
                context.normalizeEnabled = false;
            }
        }

        int comps = vb.getNumComponents();
        int type = convertVertexFormat(vb.getFormat());

        switch (vb.getBufferType()){
            case Position:
                gl11.glVertexPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case Normal:
                gl11.glNormalPointer(type, vb.getStride(), vb.getOffset());
                break;
            case Color:
                gl11.glColorPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
            case TexCoord:
                gl11.glTexCoordPointer(comps, type, vb.getStride(), vb.getOffset());
                break;
        }
    }

    private void drawTriangleListVBO(VertexBuffer indexBuf, Mesh mesh, int count){
        if (indexBuf.getBufferType() != VertexBuffer.Type.Index)
            throw new IllegalArgumentException("Only index buffers are allowed as triangle lists.");

        if (indexBuf.isUpdateNeeded())
            updateBufferData(indexBuf);

        int bufId = indexBuf.getId();
        assert bufId != -1;

        if (context.boundElementArrayVBO != bufId){
            gl11.glBindBuffer(gl11.GL_ELEMENT_ARRAY_BUFFER, bufId);
            context.boundElementArrayVBO = bufId;
        }

        if (mesh.getMode() == Mode.Hybrid){
            int[] modeStart      = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt    = convertVertexFormat(indexBuf.getFormat());
            int elSize = indexBuf.getFormat().getComponentSize();
//            int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++){
                if (i == stripStart){
                    elMode = convertElementMode(Mode.TriangleStrip);
                }else if (i == fanStart){
                    elMode = convertElementMode(Mode.TriangleStrip);
                }
                int elementLength = elementLengths[i];
                gl11.glDrawElements(elMode,
                                    elementLength,
                                    fmt,
                                    curOffset);
                curOffset += elementLength * elSize;
            }
        }else{
            gl11.glDrawElements(convertElementMode(mesh.getMode()),
                                indexBuf.getData().capacity(),
                                convertVertexFormat(indexBuf.getFormat()),
                                0);
        }
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1)
            return; // unsupported

        gl.glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal){
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled){
                gl.glEnable(gl.GL_NORMALIZE);
                context.normalizeEnabled = true;
            }else if (!vb.isNormalized() && context.normalizeEnabled){
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

        switch (vb.getBufferType()){
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

    public void setVertexAttrib(VertexBuffer vb){
        setVertexAttrib(vb, null);
    }

    public void clearVertexAttribs() {
        for (int i = 0; i < 16; i++){
            VertexBuffer vb = context.boundAttribs[i];
            if (vb != null){
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
        if (mesh.getMode() == Mode.Hybrid){
            int[] modeStart      = mesh.getModeStart();
            int[] elementLengths = mesh.getElementLengths();

            int elMode = convertElementMode(Mode.Triangles);
            int fmt    = convertVertexFormat(indexBuf.getFormat());
//            int elSize = indexBuf.getFormat().getComponentSize();
//            int listStart = modeStart[0];
            int stripStart = modeStart[1];
            int fanStart = modeStart[2];
            int curOffset = 0;
            for (int i = 0; i < elementLengths.length; i++){
                if (i == stripStart){
                    elMode = convertElementMode(Mode.TriangleStrip);
                }else if (i == fanStart){
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
        }else{
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
        if (mesh.getNumLodLevels() > 0){
            indices = mesh.getLodLevel(lod);
        }else{
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers){
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData
             || vb.getUsage() == Usage.CpuOnly) // ignore cpu-only buffers
                continue;

            if (vb.getBufferType() == Type.Index){
                indices = vb;
            }else{
                if (vb.getStride() == 0){
                    // not interleaved
                    setVertexAttrib(vb);
                }else{
                    // interleaved
                    setVertexAttrib(vb, interleavedData);
                }
            }
        }

        if (indices != null){
            drawTriangleList(indices, mesh, count);
        }else{
            gl.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    private void renderMeshVBO(Mesh mesh, int lod, int count){
        VertexBuffer indices = null;
        VertexBuffer interleavedData = mesh.getBuffer(Type.InterleavedData);
        if (interleavedData != null && interleavedData.isUpdateNeeded()){
            updateBufferData(interleavedData);
        }
        IntMap<VertexBuffer> buffers = mesh.getBuffers();
        if (mesh.getNumLodLevels() > 0){
            indices = mesh.getLodLevel(lod);
        }else{
            indices = buffers.get(Type.Index.ordinal());
        }
        for (Entry<VertexBuffer> entry : buffers){
            VertexBuffer vb = entry.getValue();

            if (vb.getBufferType() == Type.InterleavedData
             || vb.getUsage() == Usage.CpuOnly // ignore cpu-only buffers
             || vb.getBufferType() == Type.Index)
                continue;

            if (vb.getStride() == 0){
                // not interleaved
                setVertexAttribVBO(vb, null);
            }else{
                // interleaved
                setVertexAttribVBO(vb, interleavedData);
            }
        }

        if (indices != null){
            drawTriangleListVBO(indices, mesh, count);
        }else{
            gl.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
        }
        clearVertexAttribs();
        clearTextureUnits();
    }

    public void renderMesh(Mesh mesh, int lod, int count){
        // check if texturing is used for new model, if not
        // disable texturing entirely.
        checkTexturingUsed();
        if (gl11 != null){
            // use vbo
            renderMeshVBO(mesh, lod, count);
        }else{
            // use vertex arrays
            renderMeshDefault(mesh, lod, count);
        }
    }

}
