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
package com.jme3.material;

import com.jme3.export.*;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import java.io.IOException;

/**
 * <code>RenderState</code> specifies material rendering properties that cannot
 * be controlled by a shader on a {@link Material}. The properties
 * allow manipulation of rendering features such as depth testing, alpha blending,
 * face culling, stencil operations, and much more.
 *
 * @author Kirill Vainer
 */
public class RenderState implements Cloneable, Savable {

    /**
     * The <code>DEFAULT</code> render state is the one used by default
     * on all materials unless changed otherwise by the user.
     *
     * <p>
     * It has the following properties:
     * <ul>
     * <li>Back Face Culling</li>
     * <li>Depth Testing Enabled</li>
     * <li>Depth Writing Enabled</li>
     * </ul>
     */
    public static final RenderState DEFAULT = new RenderState();
    /**
     * The <code>NULL</code> render state is identical to the {@link RenderState#DEFAULT}
     * render state except that depth testing and face culling are disabled.
     */
    public static final RenderState NULL = new RenderState();
    /**
     * The <code>ADDITIONAL</code> render state is identical to the
     * {@link RenderState#DEFAULT} render state except that all apply
     * values are set to false. This allows the <code>ADDITIONAL</code> render
     * state to be combined with other state but only influencing values
     * that were changed from the original.
     */
    public static final RenderState ADDITIONAL = new RenderState();

    /**
     * <code>TestFunction</code> specifies the testing function for stencil test
     * function and alpha test function.
     *
     * <p>The functions work similarly as described except that for stencil
     * test function, the reference value given in the stencil command is
     * the input value while the reference is the value already in the stencil
     * buffer.
     */
    public enum TestFunction {

        /**
         * The test always fails
         */
        Never,
        /**
         * The test succeeds if the input value is equal to the reference value.
         */
        Equal,
        /**
         * The test succeeds if the input value is less than the reference value.
         */
        Less,
        /**
         * The test succeeds if the input value is less than or equal to
         * the reference value.
         */
        LessOrEqual,
        /**
         * The test succeeds if the input value is greater than the reference value.
         */
        Greater,
        /**
         * The test succeeds if the input value is greater than or equal to
         * the reference value.
         */
        GreaterOrEqual,
        /**
         * The test succeeds if the input value does not equal the
         * reference value.
         */
        NotEqual,
        /**
         * The test always passes
         */
        Always,}

    /**
     * <code>BlendMode</code> specifies the blending operation to use.
     *
     * @see RenderState#setBlendMode(com.jme3.material.RenderState.BlendMode)
     */
    public enum BlendMode {

        /**
         * No blending mode is used.
         */
        Off,
        /**
         * Additive blending. For use with glows and particle emitters.
         * <p>
         * Result = Source Color + Destination Color -> (GL_ONE, GL_ONE)
         */
        Additive,
        /**
         * Premultiplied alpha blending, for use with premult alpha textures.
         * <p>
         * Result = Source Color + (Dest Color * (1 - Source Alpha) ) -> (GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
         */
        PremultAlpha,
        /**
         * Additive blending that is multiplied with source alpha.
         * For use with glows and particle emitters.
         * <p>
         * Result = (Source Alpha * Source Color) + Dest Color -> (GL_SRC_ALPHA, GL_ONE)
         */
        AlphaAdditive,
        /**
         * Color blending, blends in color from dest color
         * using source color.
         * <p>
         * Result = Source Color + (1 - Source Color) * Dest Color -> (GL_ONE, GL_ONE_MINUS_SRC_COLOR)
         */
        Color,
        /**
         * Alpha blending, interpolates to source color from dest color
         * using source alpha.
         * <p>
         * Result = Source Alpha * Source Color +
         *          (1 - Source Alpha) * Dest Color -> (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
         */
        Alpha,
        /**
         * Multiplies the source and dest colors.
         * <p>
         * Result = Source Color * Dest Color -> (GL_DST_COLOR, GL_ZERO)
         */
        Modulate,
        /**
         * Multiplies the source and dest colors then doubles the result.
         * <p>
         * Result = 2 * Source Color * Dest Color -> (GL_DST_COLOR, GL_SRC_COLOR)
         */
        ModulateX2
    }

    /**
     * <code>FaceCullMode</code> specifies the criteria for faces to be culled.
     *
     * @see RenderState#setFaceCullMode(com.jme3.material.RenderState.FaceCullMode)
     */
    public enum FaceCullMode {

        /**
         * Face culling is disabled.
         */
        Off,
        /**
         * Cull front faces
         */
        Front,
        /**
         * Cull back faces
         */
        Back,
        /**
         * Cull both front and back faces.
         */
        FrontAndBack
    }

    /**
     * <code>StencilOperation</code> specifies the stencil operation to use
     * in a certain scenario as specified in {@link RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction) }
     */
    public enum StencilOperation {

        /**
         * Keep the current value.
         */
        Keep,
        /**
         * Set the value to 0
         */
        Zero,
        /**
         * Replace the value in the stencil buffer with the reference value.
         */
        Replace,
        /**
         * Increment the value in the stencil buffer, clamp once reaching
         * the maximum value.
         */
        Increment,
        /**
         * Increment the value in the stencil buffer and wrap to 0 when
         * reaching the maximum value.
         */
        IncrementWrap,
        /**
         * Decrement the value in the stencil buffer and clamp once reaching 0.
         */
        Decrement,
        /**
         * Decrement the value in the stencil buffer and wrap to the maximum
         * value when reaching 0.
         */
        DecrementWrap,
        /**
         * Does a bitwise invert of the value in the stencil buffer.
         */
        Invert
    }

    static {
        NULL.cullMode = FaceCullMode.Off;
        NULL.depthTest = false;
    }

    static {
        ADDITIONAL.applyPointSprite = false;
        ADDITIONAL.applyWireFrame = false;
        ADDITIONAL.applyCullMode = false;
        ADDITIONAL.applyDepthWrite = false;
        ADDITIONAL.applyDepthTest = false;
        ADDITIONAL.applyColorWrite = false;
        ADDITIONAL.applyBlendMode = false;
        ADDITIONAL.applyAlphaTest = false;
        ADDITIONAL.applyAlphaFallOff = false;
        ADDITIONAL.applyPolyOffset = false;
    }
    boolean pointSprite = false;
    boolean applyPointSprite = true;
    boolean wireframe = false;
    boolean applyWireFrame = true;
    FaceCullMode cullMode = FaceCullMode.Back;
    boolean applyCullMode = true;
    boolean depthWrite = true;
    boolean applyDepthWrite = true;
    boolean depthTest = true;
    boolean applyDepthTest = true;
    boolean colorWrite = true;
    boolean applyColorWrite = true;
    BlendMode blendMode = BlendMode.Off;
    boolean applyBlendMode = true;
    boolean alphaTest = false;
    boolean applyAlphaTest = true;
    float alphaFallOff = 0;
    boolean applyAlphaFallOff = true;
    float offsetFactor = 0;
    float offsetUnits = 0;
    boolean offsetEnabled = false;
    boolean applyPolyOffset = true;
    boolean stencilTest = false;
    boolean applyStencilTest = false;
    TestFunction depthFunc = TestFunction.LessOrEqual;
    //by default depth func will be applied anyway if depth test is applied
    boolean applyDepthFunc = false;    
    //by default alpha func will be applied anyway if alpha test is applied
    TestFunction alphaFunc = TestFunction.Greater;    
    boolean applyAlphaFunc = false;
    StencilOperation frontStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthPassOperation = StencilOperation.Keep;
    StencilOperation backStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthPassOperation = StencilOperation.Keep;
    TestFunction frontStencilFunction = TestFunction.Always;
    TestFunction backStencilFunction = TestFunction.Always;
    int cachedHashCode = -1;

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(pointSprite, "pointSprite", false);
        oc.write(wireframe, "wireframe", false);
        oc.write(cullMode, "cullMode", FaceCullMode.Back);
        oc.write(depthWrite, "depthWrite", true);
        oc.write(depthTest, "depthTest", true);
        oc.write(colorWrite, "colorWrite", true);
        oc.write(blendMode, "blendMode", BlendMode.Off);
        oc.write(alphaTest, "alphaTest", false);
        oc.write(alphaFallOff, "alphaFallOff", 0);
        oc.write(offsetEnabled, "offsetEnabled", false);
        oc.write(offsetFactor, "offsetFactor", 0);
        oc.write(offsetUnits, "offsetUnits", 0);
        oc.write(stencilTest, "stencilTest", false);
        oc.write(frontStencilStencilFailOperation, "frontStencilStencilFailOperation", StencilOperation.Keep);
        oc.write(frontStencilDepthFailOperation, "frontStencilDepthFailOperation", StencilOperation.Keep);
        oc.write(frontStencilDepthPassOperation, "frontStencilDepthPassOperation", StencilOperation.Keep);
        oc.write(backStencilStencilFailOperation, "frontStencilStencilFailOperation", StencilOperation.Keep);
        oc.write(backStencilDepthFailOperation, "backStencilDepthFailOperation", StencilOperation.Keep);
        oc.write(backStencilDepthPassOperation, "backStencilDepthPassOperation", StencilOperation.Keep);
        oc.write(frontStencilFunction, "frontStencilFunction", TestFunction.Always);
        oc.write(backStencilFunction, "backStencilFunction", TestFunction.Always);

        // Only "additional render state" has them set to false by default
        oc.write(applyPointSprite, "applyPointSprite", true);
        oc.write(applyWireFrame, "applyWireFrame", true);
        oc.write(applyCullMode, "applyCullMode", true);
        oc.write(applyDepthWrite, "applyDepthWrite", true);
        oc.write(applyDepthTest, "applyDepthTest", true);
        oc.write(applyColorWrite, "applyColorWrite", true);
        oc.write(applyBlendMode, "applyBlendMode", true);
        oc.write(applyAlphaTest, "applyAlphaTest", true);
        oc.write(applyAlphaFallOff, "applyAlphaFallOff", true);
        oc.write(applyPolyOffset, "applyPolyOffset", true);
        oc.write(applyDepthFunc, "applyDepthFunc", true);
        oc.write(applyAlphaFunc, "applyAlphaFunc", false);
        oc.write(depthFunc, "depthFunc", TestFunction.LessOrEqual);
        oc.write(alphaFunc, "alphaFunc", TestFunction.Greater);  

    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        pointSprite = ic.readBoolean("pointSprite", false);
        wireframe = ic.readBoolean("wireframe", false);
        cullMode = ic.readEnum("cullMode", FaceCullMode.class, FaceCullMode.Back);
        depthWrite = ic.readBoolean("depthWrite", true);
        depthTest = ic.readBoolean("depthTest", true);
        colorWrite = ic.readBoolean("colorWrite", true);
        blendMode = ic.readEnum("blendMode", BlendMode.class, BlendMode.Off);
        alphaTest = ic.readBoolean("alphaTest", false);
        alphaFallOff = ic.readFloat("alphaFallOff", 0);
        offsetEnabled = ic.readBoolean("offsetEnabled", false);
        offsetFactor = ic.readFloat("offsetFactor", 0);
        offsetUnits = ic.readFloat("offsetUnits", 0);
        stencilTest = ic.readBoolean("stencilTest", false);
        frontStencilStencilFailOperation = ic.readEnum("frontStencilStencilFailOperation", StencilOperation.class, StencilOperation.Keep);
        frontStencilDepthFailOperation = ic.readEnum("frontStencilDepthFailOperation", StencilOperation.class, StencilOperation.Keep);
        frontStencilDepthPassOperation = ic.readEnum("frontStencilDepthPassOperation", StencilOperation.class, StencilOperation.Keep);
        backStencilStencilFailOperation = ic.readEnum("backStencilStencilFailOperation", StencilOperation.class, StencilOperation.Keep);
        backStencilDepthFailOperation = ic.readEnum("backStencilDepthFailOperation", StencilOperation.class, StencilOperation.Keep);
        backStencilDepthPassOperation = ic.readEnum("backStencilDepthPassOperation", StencilOperation.class, StencilOperation.Keep);
        frontStencilFunction = ic.readEnum("frontStencilFunction", TestFunction.class, TestFunction.Always);
        backStencilFunction = ic.readEnum("backStencilFunction", TestFunction.class, TestFunction.Always);
        depthFunc = ic.readEnum("depthFunc", TestFunction.class, TestFunction.LessOrEqual);
        alphaFunc = ic.readEnum("alphaFunc", TestFunction.class, TestFunction.Greater);

        applyPointSprite = ic.readBoolean("applyPointSprite", true);
        applyWireFrame = ic.readBoolean("applyWireFrame", true);
        applyCullMode = ic.readBoolean("applyCullMode", true);
        applyDepthWrite = ic.readBoolean("applyDepthWrite", true);
        applyDepthTest = ic.readBoolean("applyDepthTest", true);
        applyColorWrite = ic.readBoolean("applyColorWrite", true);
        applyBlendMode = ic.readBoolean("applyBlendMode", true);
        applyAlphaTest = ic.readBoolean("applyAlphaTest", true);
        applyAlphaFallOff = ic.readBoolean("applyAlphaFallOff", true);
        applyPolyOffset = ic.readBoolean("applyPolyOffset", true);
        applyDepthFunc = ic.readBoolean("applyDepthFunc", true);
        applyAlphaFunc = ic.readBoolean("applyAlphaFunc", false);
        
    }

    /**
     * Create a clone of this <code>RenderState</code>
     *
     * @return Clone of this render state.
     */
    @Override
    public RenderState clone() {
        try {
            return (RenderState) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * returns true if the given renderState is equall to this one
     * @param o the renderState to compate to
     * @return true if the renderStates are equal
     */
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof RenderState)) {
            return false;
        }
        RenderState rs = (RenderState) o;
        if (pointSprite != rs.pointSprite) {
            return false;
        }

        if (wireframe != rs.wireframe) {
            return false;
        }

        if (cullMode != rs.cullMode) {
            return false;
        }

        if (depthWrite != rs.depthWrite) {
            return false;
        }

        if (depthTest != rs.depthTest) {
            return false;
        }
        if (depthTest) {
            if (depthFunc != rs.depthFunc) {
                return false;
            }
        }

        if (colorWrite != rs.colorWrite) {
            return false;
        }

        if (blendMode != rs.blendMode) {
            return false;
        }

        if (alphaTest != rs.alphaTest) {
            return false;
        }
        if (alphaTest) {
            if (alphaFunc != rs.alphaFunc) {
                return false;
            }
        }

        if (alphaFallOff != rs.alphaFallOff) {
            return false;
        }

        if (offsetEnabled != rs.offsetEnabled) {
            return false;
        }

        if (offsetFactor != rs.offsetFactor) {
            return false;
        }

        if (offsetUnits != rs.offsetUnits) {
            return false;
        }

        if (stencilTest != rs.stencilTest) {
            return false;
        }

        if (stencilTest) {
            if (frontStencilStencilFailOperation != rs.frontStencilStencilFailOperation) {
                return false;
            }
            if (frontStencilDepthFailOperation != rs.frontStencilDepthFailOperation) {
                return false;
            }
            if (frontStencilDepthPassOperation != rs.frontStencilDepthPassOperation) {
                return false;
            }
            if (backStencilStencilFailOperation != rs.backStencilStencilFailOperation) {
                return false;
            }
            if (backStencilDepthFailOperation != rs.backStencilDepthFailOperation) {
                return false;
            }

            if (backStencilDepthPassOperation != rs.backStencilDepthPassOperation) {
                return false;
            }
            if (frontStencilFunction != rs.frontStencilFunction) {
                return false;
            }
            if (backStencilFunction != rs.backStencilFunction) {
                return false;
            }
        }

        return true;
    }

    /**
     * Enables point sprite mode.
     *
     * <p>When point sprite is enabled, any meshes
     * with the type of {@link Mode#Points} will be rendered as 2D quads
     * with texturing enabled. Fragment shaders can write to the
     * <code>gl_PointCoord</code> variable to manipulate the texture coordinate
     * for each pixel. The size of the 2D quad can be controlled by writing
     * to the <code>gl_PointSize</code> variable in the vertex shader.
     *
     * @param pointSprite Enables Point Sprite mode.
     */
    public void setPointSprite(boolean pointSprite) {
        applyPointSprite = true;
        this.pointSprite = pointSprite;
        cachedHashCode = -1;
    }

    /**
     * Sets the alpha fall off value for alpha testing.
     *
     * <p>If the pixel's alpha value is greater than the
     * <code>alphaFallOff</code> then the pixel will be rendered, otherwise
     * the pixel will be discarded.
     * 
     * Note : Alpha test is deprecated since opengl 3.0 and does not exists in
     * openglES 2.0.
     * The prefered way is to use the alphaDiscardThreshold on the material
     * Or have a shader that discards the pixel when its alpha value meets the
     * discarding condition.
     *
     * @param alphaFallOff The alpha of all rendered pixels must be higher
     * than this value to be rendered. This value should be between 0 and 1.
     *
     * @see RenderState#setAlphaTest(boolean)
     */
    public void setAlphaFallOff(float alphaFallOff) {
        applyAlphaFallOff = true;
        this.alphaFallOff = alphaFallOff;
        cachedHashCode = -1;
    }

    /**
     * Enable alpha testing.
     *
     * <p>When alpha testing is enabled, all input pixels' alpha are compared
     * to the {@link RenderState#setAlphaFallOff(float) constant alpha falloff}.
     * If the input alpha is greater than the falloff, the pixel will be rendered,
     * otherwise it will be discarded.
     *
     * @param alphaTest Set to true to enable alpha testing.
     * 
     * Note : Alpha test is deprecated since opengl 3.0 and does not exists in
     * openglES 2.0.
     * The prefered way is to use the alphaDiscardThreshold on the material
     * Or have a shader that discards the pixel when its alpha value meets the
     * discarding condition.
     * 
     *
     * @see RenderState#setAlphaFallOff(float)
     */
    public void setAlphaTest(boolean alphaTest) {
        applyAlphaTest = true;
        this.alphaTest = alphaTest;
        cachedHashCode = -1;
    }

    /**
     * Enable writing color.
     *
     * <p>When color write is enabled, the result of a fragment shader, the
     * <code>gl_FragColor</code>, will be rendered into the color buffer
     * (including alpha).
     *
     * @param colorWrite Set to true to enable color writing.
     */
    public void setColorWrite(boolean colorWrite) {
        applyColorWrite = true;
        this.colorWrite = colorWrite;
        cachedHashCode = -1;
    }

    /**
     * Set the face culling mode.
     *
     * <p>See the {@link FaceCullMode} enum on what each value does.
     * Face culling will project the triangle's points onto the screen
     * and determine if the triangle is in counter-clockwise order or
     * clockwise order. If a triangle is in counter-clockwise order, then
     * it is considered a front-facing triangle, otherwise, it is considered
     * a back-facing triangle.
     *
     * @param cullMode the face culling mode.
     */
    public void setFaceCullMode(FaceCullMode cullMode) {
        applyCullMode = true;
        this.cullMode = cullMode;
        cachedHashCode = -1;
    }

    /**
     * Set the blending mode.
     *
     * <p>When blending is enabled, (<code>blendMode</code> is not {@link BlendMode#Off})
     * the input pixel will be blended with the pixel
     * already in the color buffer. The blending operation is determined
     * by the {@link BlendMode}. For example, the {@link BlendMode#Additive}
     * will add the input pixel's color to the color already in the color buffer:
     * <br/>
     * <code>Result = Source Color + Destination Color</code>
     *
     * @param blendMode The blend mode to use. Set to {@link BlendMode#Off}
     * to disable blending.
     */
    public void setBlendMode(BlendMode blendMode) {
        applyBlendMode = true;
        this.blendMode = blendMode;
        cachedHashCode = -1;
    }

    /**
     * Enable depth testing.
     *
     * <p>When depth testing is enabled, a pixel must pass the depth test
     * before it is written to the color buffer.
     * The input pixel's depth value must be less than or equal than
     * the value already in the depth buffer to pass the depth test.
     *
     * @param depthTest Enable or disable depth testing.
     */
    public void setDepthTest(boolean depthTest) {
        applyDepthTest = true;
        this.depthTest = depthTest;
        cachedHashCode = -1;
    }

    /**
     * Enable depth writing.
     *
     * <p>After passing the {@link RenderState#setDepthTest(boolean) depth test},
     * a pixel's depth value will be written into the depth buffer if
     * depth writing is enabled.
     *
     * @param depthWrite True to enable writing to the depth buffer.
     */
    public void setDepthWrite(boolean depthWrite) {
        applyDepthWrite = true;
        this.depthWrite = depthWrite;
        cachedHashCode = -1;
    }

    /**
     * Enables wireframe rendering mode.
     *
     * <p>When in wireframe mode, {@link Mesh meshes} rendered in triangle mode
     * will not be solid, but instead, only the edges of the triangles
     * will be rendered.
     *
     * @param wireframe True to enable wireframe mode.
     */
    public void setWireframe(boolean wireframe) {
        applyWireFrame = true;
        this.wireframe = wireframe;
        cachedHashCode = -1;
    }

    /**
     * Offsets the on-screen z-order of the material's polygons, to combat visual artefacts like
     * stitching, bleeding and z-fighting for overlapping polygons.
     * Factor and units are summed to produce the depth offset.
     * This offset is applied in screen space,
     * typically with positive Z pointing into the screen.
     * Typical values are (1.0f, 1.0f) or (-1.0f, -1.0f)
     *
     * @see <a href="http://www.opengl.org/resources/faq/technical/polygonoffset.htm" rel="nofollow">http://www.opengl.org/resources/faq/technical/polygonoffset.htm</a>
     * @param factor scales the maximum Z slope, with respect to X or Y of the polygon
     * @param units scales the minimum resolvable depth buffer value
     **/
    public void setPolyOffset(float factor, float units) {
        applyPolyOffset = true;
        if (factor == 0 && units == 0) {
            offsetEnabled = false;
        } else {
            offsetEnabled = true;
            offsetFactor = factor;
            offsetUnits = units;
        }
        cachedHashCode = -1;
    }    

    /**
     * Enable stencil testing.
     *
     * <p>Stencil testing can be used to filter pixels according to the stencil
     * buffer. Objects can be rendered with some stencil operation to manipulate
     * the values in the stencil buffer, then, other objects can be rendered
     * to test against the values written previously.
     *
     * @param enabled Set to true to enable stencil functionality. If false
     * all other parameters are ignored.
     *
     * @param _frontStencilStencilFailOperation Sets the operation to occur when
     * a front-facing triangle fails the front stencil function.
     * @param _frontStencilDepthFailOperation Sets the operation to occur when
     * a front-facing triangle fails the depth test.
     * @param _frontStencilDepthPassOperation Set the operation to occur when
     * a front-facing triangle passes the depth test.
     * @param _backStencilStencilFailOperation Set the operation to occur when
     * a back-facing triangle fails the back stencil function.
     * @param _backStencilDepthFailOperation Set the operation to occur when
     * a back-facing triangle fails the depth test.
     * @param _backStencilDepthPassOperation Set the operation to occur when
     * a back-facing triangle passes the depth test.
     * @param _frontStencilFunction Set the test function for front-facing triangles.
     * @param _backStencilFunction Set the test function for back-facing triangles.
     */
    public void setStencil(boolean enabled,
            StencilOperation _frontStencilStencilFailOperation,
            StencilOperation _frontStencilDepthFailOperation,
            StencilOperation _frontStencilDepthPassOperation,
            StencilOperation _backStencilStencilFailOperation,
            StencilOperation _backStencilDepthFailOperation,
            StencilOperation _backStencilDepthPassOperation,
            TestFunction _frontStencilFunction,
            TestFunction _backStencilFunction) {

        stencilTest = enabled;
        applyStencilTest = true;
        this.frontStencilStencilFailOperation = _frontStencilStencilFailOperation;
        this.frontStencilDepthFailOperation = _frontStencilDepthFailOperation;
        this.frontStencilDepthPassOperation = _frontStencilDepthPassOperation;
        this.backStencilStencilFailOperation = _backStencilStencilFailOperation;
        this.backStencilDepthFailOperation = _backStencilDepthFailOperation;
        this.backStencilDepthPassOperation = _backStencilDepthPassOperation;
        this.frontStencilFunction = _frontStencilFunction;
        this.backStencilFunction = _backStencilFunction;
        cachedHashCode = -1;
    }

    /**
     * Set the depth conparison function to the given TestFunction 
     * default is LessOrEqual (GL_LEQUAL)
     * @see TestFunction
     * @see RenderState#setDepthTest(boolean) 
     * @param depthFunc the depth comparison function
     */
    public void setDepthFunc(TestFunction depthFunc) {       
        applyDepthFunc = true;
        this.depthFunc = depthFunc;
        cachedHashCode = -1;
    }

    /**
     * Sets the alpha comparision function to the given TestFunction
     * default is Greater (GL_GREATER)
     * 
     * Note : Alpha test is deprecated since opengl 3.0 and does not exists in
     * openglES 2.0.
     * The prefered way is to use the alphaDiscardThreshold on the material
     * Or have a shader taht discards the pixel when its alpha value meets the
     * discarding condition.
     * 
     * @see TestFunction
     * @see RenderState#setAlphaTest(boolean) 
     * @see RenderState#setAlphaFallOff(float) 
     * @param alphaFunc the alpha comparision function
     */
    public void setAlphaFunc(TestFunction alphaFunc) {        
        applyAlphaFunc = true;
        this.alphaFunc = alphaFunc;
        cachedHashCode = -1;
    }
    
    

    /**
     * Check if stencil test is enabled.
     *
     * @return True if stencil test is enabled.
     */
    public boolean isStencilTest() {
        return stencilTest;
    }

    /**
     * Retrieve the front stencil fail operation.
     *
     * @return the front stencil fail operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getFrontStencilStencilFailOperation() {
        return frontStencilStencilFailOperation;
    }

    /**
     * Retrieve the front depth test fail operation.
     *
     * @return the front depth test fail operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getFrontStencilDepthFailOperation() {
        return frontStencilDepthFailOperation;
    }

    /**
     * Retrieve the front depth test pass operation.
     *
     * @return the front depth test pass operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getFrontStencilDepthPassOperation() {
        return frontStencilDepthPassOperation;
    }

    /**
     * Retrieve the back stencil fail operation.
     *
     * @return the back stencil fail operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getBackStencilStencilFailOperation() {
        return backStencilStencilFailOperation;
    }

    /**
     * Retrieve the back depth test fail operation.
     *
     * @return the back depth test fail operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getBackStencilDepthFailOperation() {
        return backStencilDepthFailOperation;
    }

    /**
     * Retrieve the back depth test pass operation.
     *
     * @return the back depth test pass operation.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public StencilOperation getBackStencilDepthPassOperation() {
        return backStencilDepthPassOperation;
    }

    /**
     * Retrieve the front stencil function.
     *
     * @return the front stencil function.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public TestFunction getFrontStencilFunction() {
        return frontStencilFunction;
    }

    /**
     * Retrieve the back stencil function.
     *
     * @return the back stencil function.
     *
     * @see RenderState#setStencil(boolean,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.StencilOperation,
     * com.jme3.material.RenderState.TestFunction,
     * com.jme3.material.RenderState.TestFunction)
     */
    public TestFunction getBackStencilFunction() {
        return backStencilFunction;
    }

    /**
     * Retrieve the blend mode.
     *
     * @return the blend mode.
     */
    public BlendMode getBlendMode() {
        return blendMode;
    }

    /**
     * Check if point sprite mode is enabled
     *
     * @return True if point sprite mode is enabled.
     *
     * @see RenderState#setPointSprite(boolean)
     */
    public boolean isPointSprite() {
        return pointSprite;
    }

    /**
     * Check if alpha test is enabled.
     *
     * @return True if alpha test is enabled.
     *
     * @see RenderState#setAlphaTest(boolean)
     */
    public boolean isAlphaTest() {
        return alphaTest;
    }

    /**
     * Retrieve the face cull mode.
     *
     * @return the face cull mode.
     *
     * @see RenderState#setFaceCullMode(com.jme3.material.RenderState.FaceCullMode)
     */
    public FaceCullMode getFaceCullMode() {
        return cullMode;
    }

    /**
     * Check if depth test is enabled.
     *
     * @return True if depth test is enabled.
     *
     * @see RenderState#setDepthTest(boolean)
     */
    public boolean isDepthTest() {
        return depthTest;
    }

    /**
     * Check if depth write is enabled.
     *
     * @return True if depth write is enabled.
     *
     * @see RenderState#setDepthWrite(boolean)
     */
    public boolean isDepthWrite() {
        return depthWrite;
    }

    /**
     * Check if wireframe mode is enabled.
     *
     * @return True if wireframe mode is enabled.
     *
     * @see RenderState#setWireframe(boolean)
     */
    public boolean isWireframe() {
        return wireframe;
    }

    /**
     * Check if color writing is enabled.
     *
     * @return True if color writing is enabled.
     *
     * @see RenderState#setColorWrite(boolean)
     */
    public boolean isColorWrite() {
        return colorWrite;
    }

    /**
     * Retrieve the poly offset factor value.
     *
     * @return the poly offset factor value.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public float getPolyOffsetFactor() {
        return offsetFactor;
    }

    /**
     * Retrieve the poly offset units value.
     *
     * @return the poly offset units value.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public float getPolyOffsetUnits() {
        return offsetUnits;
    }

    /**
     * Check if polygon offset is enabled.
     *
     * @return True if polygon offset is enabled.
     *
     * @see RenderState#setPolyOffset(float, float)
     */
    public boolean isPolyOffset() {
        return offsetEnabled;
    }

    /**
     * Retrieve the alpha falloff value.
     *
     * @return the alpha falloff value.
     *
     * @see RenderState#setAlphaFallOff(float)
     */
    public float getAlphaFallOff() {
        return alphaFallOff;
    }

    /**
     * Retrieve the depth comparison function
     *
     * @return the depth comparison function
     *
     * @see RenderState#setDepthFunc(com.jme3.material.RenderState.TestFunction)
     */
    public TestFunction getDepthFunc() {
        return depthFunc;
    }

    /**
     * Retrieve the alpha comparison function
     *
     * @return the alpha comparison function
     *
     * @see RenderState#setAlphaFunc(com.jme3.material.RenderState.TestFunction)
     */
    public TestFunction getAlphaFunc() {
        return alphaFunc;
    }
    
    

    public boolean isApplyAlphaFallOff() {
        return applyAlphaFallOff;
    }

    public boolean isApplyAlphaTest() {
        return applyAlphaTest;
    }

    public boolean isApplyBlendMode() {
        return applyBlendMode;
    }

    public boolean isApplyColorWrite() {
        return applyColorWrite;
    }

    public boolean isApplyCullMode() {
        return applyCullMode;
    }

    public boolean isApplyDepthTest() {
        return applyDepthTest;
    }

    public boolean isApplyDepthWrite() {
        return applyDepthWrite;
    }

    public boolean isApplyPointSprite() {
        return applyPointSprite;
    }

    public boolean isApplyPolyOffset() {
        return applyPolyOffset;
    }

    public boolean isApplyWireFrame() {
        return applyWireFrame;
    }

    public boolean isApplyDepthFunc() {
        return applyDepthFunc;
    }

    public boolean isApplyAlphaFunc() {
        return applyAlphaFunc;
    }
    
    

    /**
     *
     */
    public int contentHashCode() {
        if (cachedHashCode == -1){
            int hash = 7;
            hash = 79 * hash + (this.pointSprite ? 1 : 0);
            hash = 79 * hash + (this.wireframe ? 1 : 0);
            hash = 79 * hash + (this.cullMode != null ? this.cullMode.hashCode() : 0);
            hash = 79 * hash + (this.depthWrite ? 1 : 0);
            hash = 79 * hash + (this.depthTest ? 1 : 0);
            hash = 79 * hash + (this.depthFunc != null ? this.depthFunc.hashCode() : 0);
            hash = 79 * hash + (this.colorWrite ? 1 : 0);
            hash = 79 * hash + (this.blendMode != null ? this.blendMode.hashCode() : 0);
            hash = 79 * hash + (this.alphaTest ? 1 : 0);
            hash = 79 * hash + (this.alphaFunc != null ? this.alphaFunc.hashCode() : 0);
            hash = 79 * hash + Float.floatToIntBits(this.alphaFallOff);
            hash = 79 * hash + Float.floatToIntBits(this.offsetFactor);
            hash = 79 * hash + Float.floatToIntBits(this.offsetUnits);
            hash = 79 * hash + (this.offsetEnabled ? 1 : 0);
            hash = 79 * hash + (this.stencilTest ? 1 : 0);
            hash = 79 * hash + (this.frontStencilStencilFailOperation != null ? this.frontStencilStencilFailOperation.hashCode() : 0);
            hash = 79 * hash + (this.frontStencilDepthFailOperation != null ? this.frontStencilDepthFailOperation.hashCode() : 0);
            hash = 79 * hash + (this.frontStencilDepthPassOperation != null ? this.frontStencilDepthPassOperation.hashCode() : 0);
            hash = 79 * hash + (this.backStencilStencilFailOperation != null ? this.backStencilStencilFailOperation.hashCode() : 0);
            hash = 79 * hash + (this.backStencilDepthFailOperation != null ? this.backStencilDepthFailOperation.hashCode() : 0);
            hash = 79 * hash + (this.backStencilDepthPassOperation != null ? this.backStencilDepthPassOperation.hashCode() : 0);
            hash = 79 * hash + (this.frontStencilFunction != null ? this.frontStencilFunction.hashCode() : 0);
            hash = 79 * hash + (this.backStencilFunction != null ? this.backStencilFunction.hashCode() : 0);
            cachedHashCode = hash;
        }
        return cachedHashCode;
    }

    /**
     * Merges <code>this</code> state and <code>additionalState</code> into
     * the parameter <code>state</code> based on a specific criteria.
     *
     * <p>The criteria for this merge is the following:<br/>
     * For every given property, such as alpha test or depth write, check
     * if it was modified from the original in the <code>additionalState</code>
     * if it was modified, then copy the property from the <code>additionalState</code>
     * into the parameter <code>state</code>, otherwise, copy the property from <code>this</code>
     * into the parameter <code>state</code>. If <code>additionalState</code>
     * is <code>null</code>, then no modifications are made and <code>this</code> is returned,
     * otherwise, the parameter <code>state</code> is returned with the result
     * of the merge.
     *
     * @param additionalState The <code>additionalState</code>, from which data is taken only
     * if it was modified by the user.
     * @param state Contains output of the method if <code>additionalState</code>
     * is not null.
     * @return <code>state</code> if <code>additionalState</code> is non-null,
     * otherwise returns <code>this</code>
     */
    public RenderState copyMergedTo(RenderState additionalState, RenderState state) {
        if (additionalState == null) {
            return this;
        }

        if (additionalState.applyPointSprite) {
            state.pointSprite = additionalState.pointSprite;
        } else {
            state.pointSprite = pointSprite;
        }
        if (additionalState.applyWireFrame) {
            state.wireframe = additionalState.wireframe;
        } else {
            state.wireframe = wireframe;
        }

        if (additionalState.applyCullMode) {
            state.cullMode = additionalState.cullMode;
        } else {
            state.cullMode = cullMode;
        }
        if (additionalState.applyDepthWrite) {
            state.depthWrite = additionalState.depthWrite;
        } else {
            state.depthWrite = depthWrite;
        }
        if (additionalState.applyDepthTest) {
            state.depthTest = additionalState.depthTest;
        } else {
            state.depthTest = depthTest;
        }
        if (additionalState.applyDepthFunc) {
            state.depthFunc = additionalState.depthFunc;
        } else {
            state.depthFunc = depthFunc;
        }
        if (additionalState.applyColorWrite) {
            state.colorWrite = additionalState.colorWrite;
        } else {
            state.colorWrite = colorWrite;
        }
        if (additionalState.applyBlendMode) {
            state.blendMode = additionalState.blendMode;
        } else {
            state.blendMode = blendMode;
        }
        if (additionalState.applyAlphaTest) {
            state.alphaTest = additionalState.alphaTest;
        } else {
            state.alphaTest = alphaTest;
        }
        if (additionalState.applyAlphaFunc) {
            state.alphaFunc = additionalState.alphaFunc;
        } else {
            state.alphaFunc = alphaFunc;
        }

        if (additionalState.applyAlphaFallOff) {
            state.alphaFallOff = additionalState.alphaFallOff;
        } else {
            state.alphaFallOff = alphaFallOff;
        }
        if (additionalState.applyPolyOffset) {
            state.offsetEnabled = additionalState.offsetEnabled;
            state.offsetFactor = additionalState.offsetFactor;
            state.offsetUnits = additionalState.offsetUnits;
        } else {
            state.offsetEnabled = offsetEnabled;
            state.offsetFactor = offsetFactor;
            state.offsetUnits = offsetUnits;
        }
        if (additionalState.applyStencilTest) {
            state.stencilTest = additionalState.stencilTest;

            state.frontStencilStencilFailOperation = additionalState.frontStencilStencilFailOperation;
            state.frontStencilDepthFailOperation = additionalState.frontStencilDepthFailOperation;
            state.frontStencilDepthPassOperation = additionalState.frontStencilDepthPassOperation;

            state.backStencilStencilFailOperation = additionalState.backStencilStencilFailOperation;
            state.backStencilDepthFailOperation = additionalState.backStencilDepthFailOperation;
            state.backStencilDepthPassOperation = additionalState.backStencilDepthPassOperation;

            state.frontStencilFunction = additionalState.frontStencilFunction;
            state.backStencilFunction = additionalState.backStencilFunction;
        } else {
            state.stencilTest = stencilTest;

            state.frontStencilStencilFailOperation = frontStencilStencilFailOperation;
            state.frontStencilDepthFailOperation = frontStencilDepthFailOperation;
            state.frontStencilDepthPassOperation = frontStencilDepthPassOperation;

            state.backStencilStencilFailOperation = backStencilStencilFailOperation;
            state.backStencilDepthFailOperation = backStencilDepthFailOperation;
            state.backStencilDepthPassOperation = backStencilDepthPassOperation;

            state.frontStencilFunction = frontStencilFunction;
            state.backStencilFunction = backStencilFunction;
        }
        state.cachedHashCode = -1;
        return state;
    }

    @Override
    public String toString() {
        return "RenderState[\n"
                + "pointSprite=" + pointSprite
                + "\napplyPointSprite=" + applyPointSprite
                + "\nwireframe=" + wireframe
                + "\napplyWireFrame=" + applyWireFrame
                + "\ncullMode=" + cullMode
                + "\napplyCullMode=" + applyCullMode
                + "\ndepthWrite=" + depthWrite
                + "\napplyDepthWrite=" + applyDepthWrite
                + "\ndepthTest=" + depthTest
                + "\ndepthFunc=" + depthFunc
                + "\napplyDepthTest=" + applyDepthTest
                + "\ncolorWrite=" + colorWrite
                + "\napplyColorWrite=" + applyColorWrite
                + "\nblendMode=" + blendMode
                + "\napplyBlendMode=" + applyBlendMode
                + "\nalphaTest=" + alphaTest
                + "\nalphaFunc=" + alphaFunc
                + "\napplyAlphaTest=" + applyAlphaTest
                + "\nalphaFallOff=" + alphaFallOff
                + "\napplyAlphaFallOff=" + applyAlphaFallOff
                + "\noffsetEnabled=" + offsetEnabled
                + "\napplyPolyOffset=" + applyPolyOffset
                + "\noffsetFactor=" + offsetFactor
                + "\noffsetUnits=" + offsetUnits      
                + "\n]";
    }
}
