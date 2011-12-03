package com.jme3.renderer.lwjgl;

import com.jme3.light.*;
import com.jme3.material.FixedFuncBinding;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.GL1Renderer;
import com.jme3.renderer.RenderContext;
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
import com.jme3.util.NativeObjectManager;
import java.nio.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3tools.converters.MipMapGenerator;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

public class LwjglGL1Renderer implements GL1Renderer {

    private static final Logger logger = Logger.getLogger(LwjglRenderer.class.getName());
    private final ByteBuffer nameBuf = BufferUtils.createByteBuffer(250);
    private final StringBuilder stringBuf = new StringBuilder(250);
    private final IntBuffer ib1 = BufferUtils.createIntBuffer(1);
    private final IntBuffer intBuf16 = BufferUtils.createIntBuffer(16);
    private final FloatBuffer fb16 = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer fb4Null = BufferUtils.createFloatBuffer(4);
    private final RenderContext context = new RenderContext();
    private final NativeObjectManager objManager = new NativeObjectManager();
    private final EnumSet<Caps> caps = EnumSet.noneOf(Caps.class);
    private int maxTexSize;
    private int maxCubeTexSize;
    private int maxVertCount;
    private int maxTriCount;
    private int maxLights;
    private boolean gl12 = false;
    private final Statistics statistics = new Statistics();
    private int vpX, vpY, vpW, vpH;
    private int clipX, clipY, clipW, clipH;
    
    private Matrix4f worldMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    
    private ArrayList<Light> lightList = new ArrayList<Light>(8);
    private ColorRGBA materialAmbientColor = new ColorRGBA();
    private Vector3f tempVec = new Vector3f();

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
        if (GLContext.getCapabilities().OpenGL12){
            gl12 = true;
        }
        
        // Default values for certain GL state.
        glShadeModel(GL_SMOOTH);
        glColorMaterial(GL_FRONT_AND_BACK, GL_DIFFUSE);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        
		// Enable rescaling/normaling of normal vectors.
		// Fixes lighting issues with scaled models.
        if (gl12){
            glEnable(GL12.GL_RESCALE_NORMAL);
        }else{
            glEnable(GL_NORMALIZE);
        }

        if (GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            caps.add(Caps.NonPowerOfTwoTextures);
        } else {
            logger.log(Level.WARNING, "Your graphics card does not "
                    + "support non-power-of-2 textures. "
                    + "Some features might not work.");
        }
        
        maxLights = glGetInteger(GL_MAX_LIGHTS);
        
    }
    
    public void invalidateState() {
        context.reset();
    }

    public void resetGLObjects() {
        logger.log(Level.INFO, "Reseting objects and invalidating state");
        objManager.resetObjects();
        statistics.clearMemory();
        invalidateState();
    }

    public void cleanup() {
        logger.log(Level.INFO, "Deleting objects and invalidating state");
        objManager.deleteAllObjects(this);
        statistics.clearMemory();
        invalidateState();
    }

    public void setDepthRange(float start, float end) {
        glDepthRange(start, end);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        int bits = 0;
        if (color) {
            //See explanations of the depth below, we must enable color write to be able to clear the color buffer
            if (context.colorWriteEnabled == false) {
                glColorMask(true, true, true, true);
                context.colorWriteEnabled = true;
            }
            bits = GL_COLOR_BUFFER_BIT;
        }
        if (depth) {

            //glClear(GL_DEPTH_BUFFER_BIT) seems to not work when glDepthMask is false
            //here s some link on openl board
            //http://www.opengl.org/discussion_boards/ubbthreads.php?ubb=showflat&Number=257223
            //if depth clear is requested, we enable the depthMask
            if (context.depthWriteEnabled == false) {
                glDepthMask(true);
                context.depthWriteEnabled = true;
            }
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

    private void setMaterialColor(int type, ColorRGBA color, ColorRGBA defaultColor) {
        if (color != null){
            fb16.put(color.r).put(color.g).put(color.b).put(color.a).flip();
        }else{
            fb16.put(defaultColor.r).put(defaultColor.g).put(defaultColor.b).put(defaultColor.a).flip();
        }
        glMaterial(GL_FRONT_AND_BACK, type, fb16);
    }
    
    /**
     * Applies fixed function bindings from the context to OpenGL
     */
    private void applyFixedFuncBindings(boolean forLighting){
        if (forLighting){
            glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, context.shininess);
            setMaterialColor(GL_AMBIENT,  context.ambient,  ColorRGBA.DarkGray);
            setMaterialColor(GL_DIFFUSE,  context.diffuse,  ColorRGBA.White);
            setMaterialColor(GL_SPECULAR, context.specular, ColorRGBA.Black);
            
            if (context.useVertexColor){
                glEnable(GL_COLOR_MATERIAL);
            }else{
                glDisable(GL_COLOR_MATERIAL);
            }
        }else{
            // Ignore other values as they have no effect when 
            // GL_LIGHTING is disabled.
            ColorRGBA color = context.color;
            if (color != null){
                glColor4f(color.r, color.g, color.b, color.a);
            }else{
                glColor4f(1,1,1,1);
            }
        }
    }
    
    /**
     * Reset fixed function bindings to default values.
     */
    private void resetFixedFuncBindings(){
        context.color = null;
        context.ambient = null;
        context.diffuse = null;
        context.specular = null;
        context.shininess = 0;
        context.useVertexColor = false;
    }
    
    public void setFixedFuncBinding(FixedFuncBinding ffBinding, Object val) {
        switch (ffBinding) {
            case Color:
                context.color = (ColorRGBA) val;
                break;
            case MaterialAmbient:
                context.ambient = (ColorRGBA) val;
                break;
            case MaterialDiffuse:
                context.diffuse = (ColorRGBA) val;
                break;
            case MaterialSpecular:
                context.specular = (ColorRGBA) val;
                break;
            case MaterialShininess:
                context.shininess = (Float) val;
                break;
            case UseVertexColor:
                context.useVertexColor = (Boolean) val;
                break;
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

        if (state.isPointSprite()) {
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

        if (state.isStencilTest()) {
            throw new UnsupportedOperationException("OpenGL 1.1 doesn't support two sided stencil operations.");
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
    
    private void setModelView(Matrix4f modelMatrix, Matrix4f viewMatrix){
        if (context.matrixMode != GL_MODELVIEW) {
            glMatrixMode(GL_MODELVIEW);
            context.matrixMode = GL_MODELVIEW;
        }

        glLoadMatrix(storeMatrix(viewMatrix, fb16));
        glMultMatrix(storeMatrix(modelMatrix, fb16));
    }
    
    private void setProjection(Matrix4f projMatrix){
        if (context.matrixMode != GL_PROJECTION) {
            glMatrixMode(GL_PROJECTION);
            context.matrixMode = GL_PROJECTION;
        }

        glLoadMatrix(storeMatrix(projMatrix, fb16));
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
        this.worldMatrix.set(worldMatrix);
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        this.viewMatrix.set(viewMatrix);
        setProjection(projMatrix);
    }

    public void setLighting(LightList list) {
        // XXX: This is abuse of setLighting() to
		// apply fixed function bindings
        // and do other book keeping.
        if (list == null || list.size() == 0){
            glDisable(GL_LIGHTING);
            applyFixedFuncBindings(false);
            setModelView(worldMatrix, viewMatrix);
            return;
        }
        
        // Number of lights set previously
        int numLightsSetPrev = lightList.size();
        
        // If more than maxLights are defined, they will be ignored.
        // The GL1 renderer is not permitted to crash due to a 
        // GL1 limitation. It must render anything that the GL2 renderer
        // can render (even incorrectly).
        lightList.clear();
        materialAmbientColor.set(0, 0, 0, 0);
        
        for (int i = 0; i < list.size(); i++){
            Light l = list.get(i);
            if (l.getType() == Light.Type.Ambient){
                // Gather
                materialAmbientColor.addLocal(l.getColor());
            }else{
                // Add to list
                lightList.add(l);
                
                // Once maximum lights reached, exit loop.
                if (lightList.size() >= maxLights){
                    break;
                }
            }
        }
        
        applyFixedFuncBindings(true);
        
        glEnable(GL_LIGHTING);
        
        fb16.clear();
        fb16.put(materialAmbientColor.r)
            .put(materialAmbientColor.g)
            .put(materialAmbientColor.b)
            .put(1).flip();
        
        glLightModel(GL_LIGHT_MODEL_AMBIENT, fb16);
        
        if (context.matrixMode != GL_MODELVIEW) {
            glMatrixMode(GL_MODELVIEW);
            context.matrixMode = GL_MODELVIEW;
        }
        // Lights are already in world space, so just convert
        // them to view space.
        glLoadMatrix(storeMatrix(viewMatrix, fb16));
        
        for (int i = 0; i < lightList.size(); i++){
            int glLightIndex = GL_LIGHT0 + i;
            Light light = lightList.get(i);
            Light.Type lightType = light.getType();
            ColorRGBA col = light.getColor();
            Vector3f pos;
            
            // Enable the light
            glEnable(glLightIndex);
            
            // OGL spec states default value for light ambient is black
            switch (lightType){
                case Directional:
                    DirectionalLight dLight = (DirectionalLight) light;

                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    glLight(glLightIndex, GL_DIFFUSE, fb16);
                    glLight(glLightIndex, GL_SPECULAR, fb16);

                    pos = tempVec.set(dLight.getDirection()).negateLocal().normalizeLocal();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(0.0f).flip();
                    glLight(glLightIndex, GL_POSITION, fb16);
                    glLightf(glLightIndex, GL_SPOT_CUTOFF, 180);
                    break;
                case Point:
                    PointLight pLight = (PointLight) light;
      
                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    glLight(glLightIndex, GL_DIFFUSE, fb16);
                    glLight(glLightIndex, GL_SPECULAR, fb16);

                    pos = pLight.getPosition();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(1.0f).flip();
                    glLight(glLightIndex, GL_POSITION, fb16);
                    glLightf(glLightIndex, GL_SPOT_CUTOFF, 180);

                    if (pLight.getRadius() > 0) {
                        // Note: this doesn't follow the same attenuation model
                        // as the one used in the lighting shader.
                        glLightf(glLightIndex, GL_CONSTANT_ATTENUATION,  1);
                        glLightf(glLightIndex, GL_LINEAR_ATTENUATION,    pLight.getInvRadius() * 2);
                        glLightf(glLightIndex, GL_QUADRATIC_ATTENUATION, pLight.getInvRadius() * pLight.getInvRadius()); 
                    }else{
                        glLightf(glLightIndex, GL_CONSTANT_ATTENUATION,  1);
                        glLightf(glLightIndex, GL_LINEAR_ATTENUATION,    0);
                        glLightf(glLightIndex, GL_QUADRATIC_ATTENUATION, 0);
                    }

                    break;
                case Spot:
                    SpotLight sLight = (SpotLight) light;

                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    glLight(glLightIndex, GL_DIFFUSE, fb16);
                    glLight(glLightIndex, GL_SPECULAR, fb16);

                    pos = sLight.getPosition();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(1.0f).flip();
                    glLight(glLightIndex, GL_POSITION, fb16);

                    Vector3f dir = sLight.getDirection();
                    fb16.clear();
                    fb16.put(dir.x).put(dir.y).put(dir.z).put(1.0f).flip();
                    glLight(glLightIndex, GL_SPOT_DIRECTION, fb16);

                    float outerAngleRad = sLight.getSpotOuterAngle();
                    float innerAngleRad = sLight.getSpotInnerAngle();
                    float spotCut = outerAngleRad * FastMath.RAD_TO_DEG;
                    float spotExpo = 0.0f;
                    if (outerAngleRad > 0) {
                        spotExpo = (1.0f - (innerAngleRad / outerAngleRad)) * 128.0f;
                    }

                    glLightf(glLightIndex, GL_SPOT_CUTOFF, spotCut);
                    glLightf(glLightIndex, GL_SPOT_EXPONENT, spotExpo);

                    if (sLight.getSpotRange() > 0) {
                        glLightf(glLightIndex, GL_LINEAR_ATTENUATION, sLight.getInvSpotRange());
                    }else{
                        glLightf(glLightIndex, GL_LINEAR_ATTENUATION, 0);
                    }

                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unrecognized light type: " + lightType);
            }
        }
        
        // Disable lights after the index
        for (int i = lightList.size(); i < numLightsSetPrev; i++){
            glDisable(GL_LIGHT0 + i);
        }
        
        // This will set view matrix as well.
        setModelView(worldMatrix, viewMatrix);
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
            case BorderClamp:
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
        if (!GLContext.getCapabilities().GL_ARB_texture_non_power_of_two) {
            if (img.getWidth() != 0 && img.getHeight() != 0) {
                if (!FastMath.isPowerOfTwo(img.getWidth())
                        || !FastMath.isPowerOfTwo(img.getHeight())) {

                    // Resize texture to Power-of-2 size
                    MipMapGenerator.resizeToPowerOf2(img);

                }
            }
        }

        if (!img.hasMipmaps() && mips) {
            // No pregenerated mips available,
            // generate from base level if required

            // Check if hardware mips are supported
            if (GLContext.getCapabilities().OpenGL14) {
                glTexParameteri(target, GL14.GL_GENERATE_MIPMAP, GL_TRUE);
            } else {
                MipMapGenerator.generateMipMaps(img);
            }
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
        if (unit != 0 || tex.getType() != Texture.Type.TwoDimensional) {
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

    private void clearTextureUnits() {
        Image[] textures = context.boundTextures;
        if (textures[0] != null) {
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
        if (count > 1) {
            throw new UnsupportedOperationException();
        }

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
                if (!(data instanceof FloatBuffer)) {
                    throw new UnsupportedOperationException();
                }

                glVertexPointer(comps, vb.getStride(), (FloatBuffer) data);
                break;
            case Normal:
                if (!(data instanceof FloatBuffer)) {
                    throw new UnsupportedOperationException();
                }

                glNormalPointer(vb.getStride(), (FloatBuffer) data);
                break;
            case Color:
                if (data instanceof FloatBuffer) {
                    glColorPointer(comps, vb.getStride(), (FloatBuffer) data);
                } else if (data instanceof ByteBuffer) {
                    glColorPointer(comps, true, vb.getStride(), (ByteBuffer) data);
                } else {
                    throw new UnsupportedOperationException();
                }
                break;
            case TexCoord:
                if (!(data instanceof FloatBuffer)) {
                    throw new UnsupportedOperationException();
                }

                glTexCoordPointer(comps, vb.getStride(), (FloatBuffer) data);
                break;
            default:
                // Ignore, this is an unsupported attribute for OpenGL1.
                break;
        }
    }

    public void setVertexAttrib(VertexBuffer vb) {
        setVertexAttrib(vb, null);
    }

    private void drawElements(int mode, int format, Buffer data) {
        switch (format) {
            case GL_UNSIGNED_BYTE:
                glDrawElements(mode, (ByteBuffer) data);
                break;
            case GL_UNSIGNED_SHORT:
                glDrawElements(mode, (ShortBuffer) data);
                break;
            case GL_UNSIGNED_INT:
                glDrawElements(mode, (IntBuffer) data);
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
        
        // TODO: Fix these to use IDList??
        clearVertexAttribs();
        clearTextureUnits();
        resetFixedFuncBindings();
    }

    public void renderMesh(Mesh mesh, int lod, int count) {
        if (mesh.getVertexCount() == 0) {
            return;
        }

        if (context.pointSize != mesh.getPointSize()) {
            glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }
        
        boolean dynamic = false;
        if (mesh.getBuffer(Type.InterleavedData) != null) {
            throw new UnsupportedOperationException("Interleaved meshes are not supported");
        }

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

    public void copyFrameBuffer(FrameBuffer src, FrameBuffer dst, boolean copyDepth) {
    }

    public void setMainFrameBufferOverride(FrameBuffer fb){
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
