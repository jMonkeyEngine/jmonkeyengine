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
import com.jme3.renderer.RendererException;
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
import com.jme3.util.NativeObjectManager;
import java.nio.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;
import jme3tools.converters.MipMapGenerator;

public class JoglGL1Renderer implements GL1Renderer {

    private static final Logger logger = Logger.getLogger(JoglRenderer.class.getName());
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
        GL gl = GLContext.getCurrentGL();
        if (gl.isExtensionAvailable("GL_VERSION_1_2")){
            gl12 = true;
        }
        
        // Default values for certain GL state.
        gl.getGL2ES1().glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.getGL2().glColorMaterial(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_DIFFUSE);
        gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        
        // Enable rescaling/normaling of normal vectors.
        // Fixes lighting issues with scaled models.
        if (gl12){
            gl.glEnable(GL2ES1.GL_RESCALE_NORMAL);
        }else{
            gl.glEnable(GLLightingFunc.GL_NORMALIZE);
        }

        if (gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two")) {
            caps.add(Caps.NonPowerOfTwoTextures);
        } else {
            logger.log(Level.WARNING, "Your graphics card does not "
                    + "support non-power-of-2 textures. "
                    + "Some features might not work.");
        }
        
        gl.glGetIntegerv(GL2ES1.GL_MAX_LIGHTS, ib1);
        maxLights = ib1.get(0);
        
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, ib1);
        maxTexSize = ib1.get(0);
    }
    
    public void invalidateState() {
        context.reset();
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

    public void setDepthRange(float start, float end) {
        GL gl = GLContext.getCurrentGL();
        gl.getGL2ES2().glDepthRange(start, end);
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

    private void setMaterialColor(int type, ColorRGBA color, ColorRGBA defaultColor) {
        GL gl = GLContext.getCurrentGL();
        if (color != null){
            fb16.put(color.r).put(color.g).put(color.b).put(color.a).flip();
        }else{
            fb16.put(defaultColor.r).put(defaultColor.g).put(defaultColor.b).put(defaultColor.a).flip();
        }
        gl.getGL2().glMaterialfv(GL.GL_FRONT_AND_BACK, type, fb16);
    }
    
    /**
     * Applies fixed function bindings from the context to OpenGL
     */
    private void applyFixedFuncBindings(boolean forLighting){
        GL gl = GLContext.getCurrentGL();
        if (forLighting) {
            gl.getGL2().glMaterialf(GL.GL_FRONT_AND_BACK, GLLightingFunc.GL_SHININESS, context.shininess);
            setMaterialColor(GLLightingFunc.GL_AMBIENT, context.ambient, ColorRGBA.DarkGray);
            setMaterialColor(GLLightingFunc.GL_DIFFUSE, context.diffuse, ColorRGBA.White);
            setMaterialColor(GLLightingFunc.GL_SPECULAR, context.specular, ColorRGBA.Black);

            if (context.useVertexColor) {
                gl.glEnable(GLLightingFunc.GL_COLOR_MATERIAL);
            } else {
                gl.glDisable(GLLightingFunc.GL_COLOR_MATERIAL);
            }
        } else {
            // Ignore other values as they have no effect when 
            // GL_LIGHTING is disabled.
            ColorRGBA color = context.color;
            if (color != null) {
                gl.getGL2().glColor4f(color.r, color.g, color.b, color.a);
            } else {
                gl.getGL2().glColor4f(1, 1, 1, 1);
            }
        }
        if (context.alphaTestFallOff > 0f) {
            gl.glEnable(GL2ES1.GL_ALPHA_TEST);
            gl.getGL2ES1().glAlphaFunc(GL.GL_GREATER, context.alphaTestFallOff);
        } else {
            gl.glDisable(GL2ES1.GL_ALPHA_TEST);
        }
    }
    
    /**
     * Reset fixed function bindings to default values.
     */
    private void resetFixedFuncBindings(){
        context.alphaTestFallOff = 0f; // zero means disable alpha test!
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
            case AlphaTestFallOff:
                context.alphaTestFallOff = (Float) val;
                break;
        }
    }
    
    public void applyRenderState(RenderState state) {
        GL gl = GLContext.getCurrentGL();
        if (state.isWireframe() && !context.wireframe) {
            gl.getGL2GL3().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
            context.wireframe = true;
        } else if (!state.isWireframe() && context.wireframe) {
            gl.getGL2GL3().glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);
            context.wireframe = false;
        }

        if (state.isDepthTest() && !context.depthTestEnabled) {
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            context.depthTestEnabled = true;
        } else if (!state.isDepthTest() && context.depthTestEnabled) {
            gl.glDisable(GL.GL_DEPTH_TEST);
            context.depthTestEnabled = false;
        }

        if (state.isAlphaTest()) {
            setFixedFuncBinding(FixedFuncBinding.AlphaTestFallOff, state.getAlphaFallOff());
        } else {
            setFixedFuncBinding(FixedFuncBinding.AlphaTestFallOff, 0f); // disable it
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

        if (state.isPointSprite()) {
            logger.log(Level.WARNING, "Point Sprite unsupported!");
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

        if (state.isStencilTest()) {
            throw new UnsupportedOperationException("OpenGL 1.1 doesn't support two sided stencil operations.");
        }

    }

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
        GL gl = GLContext.getCurrentGL();
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

    private FloatBuffer storeMatrix(Matrix4f matrix, FloatBuffer store) {
        store.clear();
        matrix.fillFloatBuffer(store, true);
        store.clear();
        return store;
    }
    
    private void setModelView(Matrix4f modelMatrix, Matrix4f viewMatrix){
        GL gl = GLContext.getCurrentGL();
        if (context.matrixMode != GLMatrixFunc.GL_MODELVIEW) {
            gl.getGL2ES1().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            context.matrixMode = GLMatrixFunc.GL_MODELVIEW;
        }

        gl.getGL2ES1().glLoadMatrixf(storeMatrix(viewMatrix, fb16));
        gl.getGL2ES1().glMultMatrixf(storeMatrix(modelMatrix, fb16));
    }
    
    private void setProjection(Matrix4f projMatrix){
        GL gl = GLContext.getCurrentGL();
        if (context.matrixMode != GLMatrixFunc.GL_PROJECTION) {
            gl.getGL2ES1().glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            context.matrixMode = GLMatrixFunc.GL_PROJECTION;
        }

        gl.getGL2ES1().glLoadMatrixf(storeMatrix(projMatrix, fb16));
    }

    public void setWorldMatrix(Matrix4f worldMatrix) {
        this.worldMatrix.set(worldMatrix);
    }

    public void setViewProjectionMatrices(Matrix4f viewMatrix, Matrix4f projMatrix) {
        this.viewMatrix.set(viewMatrix);
        setProjection(projMatrix);
    }

    public void setLighting(LightList list) {
        GL gl = GLContext.getCurrentGL();
        // XXX: This is abuse of setLighting() to
        // apply fixed function bindings
        // and do other book keeping.
        if (list == null || list.size() == 0){
            gl.glDisable(GLLightingFunc.GL_LIGHTING);
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
        
        gl.glEnable(GLLightingFunc.GL_LIGHTING);
        
        fb16.clear();
        fb16.put(materialAmbientColor.r)
            .put(materialAmbientColor.g)
            .put(materialAmbientColor.b)
            .put(1).flip();
        
        gl.getGL2ES1().glLightModelfv(GL2ES1.GL_LIGHT_MODEL_AMBIENT, fb16);
        
        if (context.matrixMode != GLMatrixFunc.GL_MODELVIEW) {
            gl.getGL2ES1().glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            context.matrixMode = GLMatrixFunc.GL_MODELVIEW;
        }
        // Lights are already in world space, so just convert
        // them to view space.
        gl.getGL2ES1().glLoadMatrixf(storeMatrix(viewMatrix, fb16));
        
        for (int i = 0; i < lightList.size(); i++){
            int glLightIndex = GLLightingFunc.GL_LIGHT0 + i;
            Light light = lightList.get(i);
            Light.Type lightType = light.getType();
            ColorRGBA col = light.getColor();
            Vector3f pos;
            
            // Enable the light
            gl.glEnable(glLightIndex);
            
            // OGL spec states default value for light ambient is black
            switch (lightType){
                case Directional:
                    DirectionalLight dLight = (DirectionalLight) light;

                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_DIFFUSE, fb16);
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_SPECULAR, fb16);

                    pos = tempVec.set(dLight.getDirection()).negateLocal().normalizeLocal();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(0.0f).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_POSITION, fb16);
                    gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_SPOT_CUTOFF, 180);
                    break;
                case Point:
                    PointLight pLight = (PointLight) light;
      
                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_DIFFUSE, fb16);
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_SPECULAR, fb16);

                    pos = pLight.getPosition();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(1.0f).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_POSITION, fb16);
                    gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_SPOT_CUTOFF, 180);

                    if (pLight.getRadius() > 0) {
                        // Note: this doesn't follow the same attenuation model
                        // as the one used in the lighting shader.
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_CONSTANT_ATTENUATION,  1);
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_LINEAR_ATTENUATION,    pLight.getInvRadius() * 2);
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_QUADRATIC_ATTENUATION, pLight.getInvRadius() * pLight.getInvRadius()); 
                    }else{
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_CONSTANT_ATTENUATION,  1);
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_LINEAR_ATTENUATION,    0);
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_QUADRATIC_ATTENUATION, 0);
                    }

                    break;
                case Spot:
                    SpotLight sLight = (SpotLight) light;

                    fb16.clear();
                    fb16.put(col.r).put(col.g).put(col.b).put(col.a).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_DIFFUSE, fb16);
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_SPECULAR, fb16);

                    pos = sLight.getPosition();
                    fb16.clear();
                    fb16.put(pos.x).put(pos.y).put(pos.z).put(1.0f).flip();
                    gl.getGL2().glLightfv(glLightIndex, GLLightingFunc.GL_POSITION, fb16);

                    Vector3f dir = sLight.getDirection();
                    fb16.clear();
                    fb16.put(dir.x).put(dir.y).put(dir.z).put(1.0f).flip();
                    gl.getGL2ES1().glLightfv(glLightIndex, GLLightingFunc.GL_SPOT_DIRECTION, fb16);

                    float outerAngleRad = sLight.getSpotOuterAngle();
                    float innerAngleRad = sLight.getSpotInnerAngle();
                    float spotCut = outerAngleRad * FastMath.RAD_TO_DEG;
                    float spotExpo = 0.0f;
                    if (outerAngleRad > 0) {
                        spotExpo = (1.0f - (innerAngleRad / outerAngleRad)) * 128.0f;
                    }

                    gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_SPOT_CUTOFF, spotCut);
                    gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_SPOT_EXPONENT, spotExpo);

                    if (sLight.getSpotRange() > 0) {
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_LINEAR_ATTENUATION, sLight.getInvSpotRange());
                    }else{
                        gl.getGL2ES1().glLightf(glLightIndex, GLLightingFunc.GL_LINEAR_ATTENUATION, 0);
                    }

                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unrecognized light type: " + lightType);
            }
        }
        
        // Disable lights after the index
        for (int i = lightList.size(); i < numLightsSetPrev; i++){
            gl.glDisable(GLLightingFunc.GL_LIGHT0 + i);
        }
        
        // This will set view matrix as well.
        setModelView(worldMatrix, viewMatrix);
    }

    private int convertTextureType(Texture.Type type) {
        switch (type) {
            case TwoDimensional:
                return GL.GL_TEXTURE_2D;
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
            case EdgeClamp:
            case Clamp:
            case BorderClamp:
                return GL2.GL_CLAMP;
            case Repeat:
                return GL.GL_REPEAT;
            default:
                throw new UnsupportedOperationException("Unknown wrap mode: " + mode);
        }
    }

    private void setupTextureParams(Texture tex) {
        int target = convertTextureType(tex.getType());

        // filter things
        int minFilter = convertMinFilter(tex.getMinFilter());
        int magFilter = convertMagFilter(tex.getMagFilter());
        GL gl = GLContext.getCurrentGL();
        gl.glTexParameteri(target, GL.GL_TEXTURE_MIN_FILTER, minFilter);
        gl.glTexParameteri(target, GL.GL_TEXTURE_MAG_FILTER, magFilter);

        // repeat modes
        switch (tex.getType()) {
//            case ThreeDimensional:
//            case CubeMap:
//                glTexParameteri(target, GL_TEXTURE_WRAP_R, convertWrapMode(tex.getWrap(WrapAxis.R)));
            case TwoDimensional:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_T, convertWrapMode(tex.getWrap(WrapAxis.T)));
                // fall down here is intentional..
//            case OneDimensional:
                gl.glTexParameteri(target, GL.GL_TEXTURE_WRAP_S, convertWrapMode(tex.getWrap(WrapAxis.S)));
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + tex.getType());
        }
    }

    public void updateTexImageData(Image img, Texture.Type type, int unit) {
        int texId = img.getId();
        GL gl = GLContext.getCurrentGL();
        if (texId == -1) {
            // create texture
            gl.glGenTextures(1, ib1);
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
            gl.glEnable(target);
            gl.glBindTexture(target, texId);
            context.boundTextures[unit] = img;

            statistics.onTextureUse(img, true);
        }

        // Check sizes if graphics card doesn't support NPOT
        if (!gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two")) {
            if (img.getWidth() != 0 && img.getHeight() != 0) {
                if (!FastMath.isPowerOfTwo(img.getWidth())
                        || !FastMath.isPowerOfTwo(img.getHeight())) {

                    // Resize texture to Power-of-2 size
                    MipMapGenerator.resizeToPowerOf2(img);
                }
            }
        }

        if (!img.hasMipmaps() && img.isGeneratedMipmapsRequired()) {
            // No pregenerated mips available,
            // generate from base level if required

            // Check if hardware mips are supported
            if (gl.isExtensionAvailable("GL_VERSION_1_4")) {
                gl.glTexParameteri(target, GL2ES1.GL_GENERATE_MIPMAP, GL.GL_TRUE);
            } else {
                MipMapGenerator.generateMipMaps(img);
            }
            img.setMipmapsGenerated(true);
        } else {
        }

        if (img.getWidth() > maxTexSize || img.getHeight() > maxTexSize) {
            throw new RendererException("Cannot upload texture " + img + ". The maximum supported texture resolution is " + maxTexSize);
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
        TextureUtil.uploadTexture(img, target, 0, 0);
        //}

        img.clearUpdateNeeded();
    }

    public void setTexture(int unit, Texture tex) {
        if (unit != 0 || tex.getType() != Texture.Type.TwoDimensional) {
            //throw new UnsupportedOperationException();
            return;
        }

        Image image = tex.getImage();
        if (image.isUpdateNeeded() || (image.isGeneratedMipmapsRequired() && !image.isMipmapsGenerated()) ) {
            updateTexImageData(image, tex.getType(), unit);
        }

        int texId = image.getId();
        assert texId != -1;

        Image[] textures = context.boundTextures;

        int type = convertTextureType(tex.getType());
//        if (!context.textureIndexList.moveToNew(unit)) {
//             if (context.boundTextureUnit != unit){
//                gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
//                context.boundTextureUnit = unit;
//             }
//             gl.glEnable(type);
//        }

//        if (context.boundTextureUnit != unit) {
//            gl.glActiveTexture(GL.GL_TEXTURE0 + unit);
//            context.boundTextureUnit = unit;
//        }

        if (textures[unit] != image) {
            GL gl = GLContext.getCurrentGL();
            gl.glEnable(type);
            gl.glBindTexture(type, texId);
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
            GL gl = GLContext.getCurrentGL();
            gl.glDisable(GL.GL_TEXTURE_2D);
            textures[0] = null;
        }
    }

    public void deleteImage(Image image) {
        int texId = image.getId();
        if (texId != -1) {
            ib1.put(0, texId);
            ib1.position(0).limit(1);
            GL gl = GLContext.getCurrentGL();
            gl.glDeleteTextures(ib1.limit() ,ib1);
            image.resetObject();
        }
    }

    private int convertArrayType(VertexBuffer.Type type) {
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

    private int convertVertexFormat(VertexBuffer.Format fmt) {
        switch (fmt) {
            case Byte:
                return GL.GL_BYTE;
            case Float:
                return GL.GL_FLOAT;
            case Int:
                return GL2ES2.GL_INT;
            case Short:
                return GL.GL_SHORT;
            case UnsignedByte:
                return GL.GL_UNSIGNED_BYTE;
            case UnsignedInt:
                return GL.GL_UNSIGNED_INT;
            case UnsignedShort:
                return GL.GL_UNSIGNED_SHORT;
            default:
                throw new UnsupportedOperationException("Unrecognized vertex format: " + fmt);
        }
    }

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

    public void drawTriangleArray(Mesh.Mode mode, int count, int vertCount) {
        if (count > 1) {
            throw new UnsupportedOperationException();
        }
        GL gl = GLContext.getCurrentGL();
        gl.glDrawArrays(convertElementMode(mode), 0, vertCount);
    }

    public void setVertexAttrib(VertexBuffer vb, VertexBuffer idb) {
        if (vb.getBufferType() == VertexBuffer.Type.Color && !context.useVertexColor) {
            // Ignore vertex color buffer if vertex color is disabled.
            return;
        }
        
        int arrayType = convertArrayType(vb.getBufferType());
        if (arrayType == -1) {
            return; // unsupported
        }
        GL gl = GLContext.getCurrentGL();
        gl.getGL2GL3().glEnableClientState(arrayType);
        context.boundAttribs[vb.getBufferType().ordinal()] = vb;

        if (vb.getBufferType() == Type.Normal) {
            // normalize if requested
            if (vb.isNormalized() && !context.normalizeEnabled) {
                gl.glEnable(GLLightingFunc.GL_NORMALIZE);
                context.normalizeEnabled = true;
            } else if (!vb.isNormalized() && context.normalizeEnabled) {
                gl.glDisable(GLLightingFunc.GL_NORMALIZE);
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

                gl.getGL2().glVertexPointer(comps, type, vb.getStride(), (FloatBuffer) data);
                break;
            case Normal:
                if (!(data instanceof FloatBuffer)) {
                    throw new UnsupportedOperationException();
                }

                gl.getGL2().glNormalPointer(type, vb.getStride(), (FloatBuffer) data);
                break;
            case Color:
                if (data instanceof FloatBuffer) {
                    gl.getGL2().glColorPointer(comps, type, vb.getStride(), (FloatBuffer) data);
                } else if (data instanceof ByteBuffer) {
                    gl.getGL2().glColorPointer(comps, type, vb.getStride(), (ByteBuffer) data);
                } else {
                    throw new UnsupportedOperationException();
                }
                break;
            case TexCoord:
                if (!(data instanceof FloatBuffer)) {
                    throw new UnsupportedOperationException();
                }

                gl.getGL2().glTexCoordPointer(comps, type, vb.getStride(), (FloatBuffer) data);
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
        GL gl = GLContext.getCurrentGL();
        switch (format) {
            case GL.GL_UNSIGNED_BYTE:
                gl.glDrawElements(mode, data.limit(), format, (ByteBuffer) data);
                break;
            case GL.GL_UNSIGNED_SHORT:
                gl.glDrawElements(mode, data.limit(), format, (ShortBuffer) data);
                break;
            case GL.GL_UNSIGNED_INT:
                gl.glDrawElements(mode, data.limit(), format, (IntBuffer) data);
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
                GL gl = GLContext.getCurrentGL();
                gl.getGL2().glDisableClientState(arrayType);
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

        if (mesh.getNumLodLevels() > 0) {
            indices = mesh.getLodLevel(lod);
        } else {
            indices = mesh.getBuffer(Type.Index);
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
            GL gl = GLContext.getCurrentGL();
            gl.glDrawArrays(convertElementMode(mesh.getMode()), 0, mesh.getVertexCount());
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
        GL gl = GLContext.getCurrentGL();
        if (context.pointSize != mesh.getPointSize()) {
            gl.getGL2().glPointSize(mesh.getPointSize());
            context.pointSize = mesh.getPointSize();
        }
        if (context.lineWidth != mesh.getLineWidth()) {
            gl.getGL2().glLineWidth(mesh.getLineWidth());
            context.lineWidth = mesh.getLineWidth();
        }
        
        boolean dynamic = false;
        if (mesh.getBuffer(Type.InterleavedData) != null) {
            throw new UnsupportedOperationException("Interleaved meshes are not supported");
        }

        if (mesh.getNumLodLevels() == 0) {
            for (VertexBuffer vb : mesh.getBufferList().getArray()) {
                if (vb.getUsage() != VertexBuffer.Usage.Static) {
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
