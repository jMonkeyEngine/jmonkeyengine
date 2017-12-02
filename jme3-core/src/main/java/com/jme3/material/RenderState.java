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
     * function.
     *
     * <p>
     * The reference value given in the stencil command is the input value while
     * the reference is the value already in the stencil buffer.
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
        Always
    }

    /**
     * <code>BlendEquation</code> specifies the blending equation to combine
     * pixels.
     */
    public enum BlendEquation {
        /**
         * Sets the blend equation so that the source and destination data are
         * added. (Default) Clamps to [0,1] Useful for things like antialiasing
         * and transparency.
         */
        Add,
        /**
         * Sets the blend equation so that the source and destination data are
         * subtracted (Src - Dest). Clamps to [0,1] Falls back to Add if
         * supportsSubtract is false.
         */
        Subtract,
        /**
         * Same as Subtract, but the order is reversed (Dst - Src). Clamps to
         * [0,1] Falls back to Add if supportsSubtract is false.
         */
        ReverseSubtract,
        /**
         * Sets the blend equation so that each component of the result color is
         * the minimum of the corresponding components of the source and
         * destination colors. This and Max are useful for applications that
         * analyze image data (image thresholding against a constant color, for
         * example). Falls back to Add if supportsMinMax is false.
         */
        Min,
        /**
         * Sets the blend equation so that each component of the result color is
         * the maximum of the corresponding components of the source and
         * destination colors. This and Min are useful for applications that
         * analyze image data (image thresholding against a constant color, for
         * example). Falls back to Add if supportsMinMax is false.
         */
        Max
    }
    
    /**
     * <code>BlendEquationAlpha</code> specifies the blending equation to
     * combine pixels for the alpha component.
     */
    public enum BlendEquationAlpha {
        /**
         * Sets the blend equation to be the same as the one defined by
         * {@link #blendEquation}.
         *
         */
        InheritColor,
        /**
         * Sets the blend equation so that the source and destination data are
         * added. (Default) Clamps to [0,1] Useful for things like antialiasing
         * and transparency.
         */
        Add,
        /**
         * Sets the blend equation so that the source and destination data are
         * subtracted (Src - Dest). Clamps to [0,1] Falls back to Add if
         * supportsSubtract is false.
         */
        Subtract,
        /**
         * Same as Subtract, but the order is reversed (Dst - Src). Clamps to
         * [0,1] Falls back to Add if supportsSubtract is false.
         */
        ReverseSubtract,
        /**
         * Sets the blend equation so that the result alpha is the minimum of
         * the source alpha and destination alpha. This and Max are useful for
         * applications that analyze image data (image thresholding against a
         * constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Min,
        /**
         * sSets the blend equation so that the result alpha is the maximum of
         * the source alpha and destination alpha. This and Min are useful for
         * applications that analyze image data (image thresholding against a
         * constant color, for example). Falls back to Add if supportsMinMax is
         * false.
         */
        Max
    }
    
    /**
     * <code>BlendFunc</code> defines the blending functions for use with 
     * <code>BlendMode.Custom</code>.
     * Source color components are referred to as (R_s0, G_s0, B_s0, A_s0).
     * Destination color components are referred to as (R_d, G_d, B_d, A_d).
     */
    public enum BlendFunc {
        /**
         * RGB Factor (0, 0, 0), Alpha Factor (0)
         */
        Zero,
        /**
         * RGB Factor (1, 1, 1), Alpha Factor (1)
         */
        One,
        /**
         * RGB Factor (R_s0, G_s0, B_s0), Alpha Factor (A_s0)
         */
        Src_Color,
        /**
         * RGB Factor (1-R_s0, 1-G_s0, 1-B_s0), Alpha Factor (1-A_s0)
         */
        One_Minus_Src_Color,
        /**
         * RGB Factor (R_d, G_d, B_d), Alpha Factor (A_d)
         */
        Dst_Color,
        /**
         * RGB Factor (1-R_d, 1-G_d, 1-B_d), Alpha Factor (1-A_d)
         */
        One_Minus_Dst_Color,
        /**
         * RGB Factor (A_s0, A_s0, A_s0), Alpha Factor (A_s0)
         */
        Src_Alpha,
        /**
         * RGB Factor (1-A_s0, 1-A_s0, 1-A_s0), Alpha Factor (1-A_s0)
         */
        One_Minus_Src_Alpha,
        /**
         * RGB Factor (A_d, A_d, A_d), Alpha Factor (A_d)
         */
        Dst_Alpha,
        /**
         * RGB Factor (1-A_d, 1-A_d, 1-A_d), Alpha Factor (1-A_d)
         */
        One_Minus_Dst_Alpha,
        /**
         * RGB Factor (i, i, i), Alpha Factor (1)
         */
        Src_Alpha_Saturate;
    }
    
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
        ModulateX2,
        /**
         * Opposite effect of Modulate/Multiply. Invert both colors, multiply and
         * then invert the result.
         * <p>
         * Result = 1 - (1 - Source Color) * (1 - Dest Color) -> (GL_ONE, GL_ONE_MINUS_SRC_COLOR)
         */
        Screen,
        /**
         * Mixes the destination and source colors similar to a color-based XOR
         * operation.  This is directly equivalent to Photoshop's "Exclusion" blend.
         * <p>
         * Result = (Source Color * (1 - Dest Color)) + (Dest Color * (1 - Source Color))
         *  -> (GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR) 
         */
        Exclusion,
        /**
         * Allows for custom blending by using glBlendFuncSeparate.
         * <p>
         * 
         */
        Custom
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
        ADDITIONAL.applyWireFrame = false;
        ADDITIONAL.applyCullMode = false;
        ADDITIONAL.applyDepthWrite = false;
        ADDITIONAL.applyDepthTest = false;
        ADDITIONAL.applyColorWrite = false;
        ADDITIONAL.applyBlendEquation = false;
        ADDITIONAL.applyBlendEquationAlpha = false;
        ADDITIONAL.applyBlendMode = false;
        ADDITIONAL.applyPolyOffset = false;
    }
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
    BlendEquation blendEquation = BlendEquation.Add;
    boolean applyBlendEquation = true;
    BlendEquationAlpha blendEquationAlpha = BlendEquationAlpha.InheritColor;
    boolean applyBlendEquationAlpha = true;
    BlendMode blendMode = BlendMode.Off;
    boolean applyBlendMode = true;
    float offsetFactor = 0;
    float offsetUnits = 0;
    boolean offsetEnabled = false;
    boolean applyPolyOffset = true;
    boolean stencilTest = false;
    boolean applyStencilTest = false;
    float lineWidth = 1;
    boolean applyLineWidth = false;
    TestFunction depthFunc = TestFunction.LessOrEqual;
    //by default depth func will be applied anyway if depth test is applied
    boolean applyDepthFunc = false;
    StencilOperation frontStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthPassOperation = StencilOperation.Keep;
    StencilOperation backStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthPassOperation = StencilOperation.Keep;
    TestFunction frontStencilFunction = TestFunction.Always;
    TestFunction backStencilFunction = TestFunction.Always;
    int cachedHashCode = -1;
    BlendFunc sfactorRGB=BlendFunc.One;
    BlendFunc dfactorRGB=BlendFunc.Zero;
    BlendFunc sfactorAlpha=BlendFunc.One;
    BlendFunc dfactorAlpha=BlendFunc.Zero;
            
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(true, "pointSprite", false);
        oc.write(wireframe, "wireframe", false);
        oc.write(cullMode, "cullMode", FaceCullMode.Back);
        oc.write(depthWrite, "depthWrite", true);
        oc.write(depthTest, "depthTest", true);
        oc.write(colorWrite, "colorWrite", true);
        oc.write(blendMode, "blendMode", BlendMode.Off);
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
        oc.write(blendEquation, "blendEquation", BlendEquation.Add);
        oc.write(blendEquationAlpha, "blendEquationAlpha", BlendEquationAlpha.InheritColor);
        oc.write(depthFunc, "depthFunc", TestFunction.LessOrEqual);
        oc.write(lineWidth, "lineWidth", 1);
        oc.write(sfactorRGB, "sfactorRGB", sfactorRGB);
        oc.write(dfactorRGB, "dfactorRGB", dfactorRGB);
        oc.write(sfactorAlpha, "sfactorAlpha", sfactorAlpha);
        oc.write(dfactorAlpha, "dfactorAlpha", dfactorAlpha);

        // Only "additional render state" has them set to false by default
        oc.write(applyWireFrame, "applyWireFrame", true);
        oc.write(applyCullMode, "applyCullMode", true);
        oc.write(applyDepthWrite, "applyDepthWrite", true);
        oc.write(applyDepthTest, "applyDepthTest", true);
        oc.write(applyColorWrite, "applyColorWrite", true);
        oc.write(applyBlendEquation, "applyBlendEquation", true);
        oc.write(applyBlendEquationAlpha, "applyBlendEquationAlpha", true);
        oc.write(applyBlendMode, "applyBlendMode", true);
        oc.write(applyPolyOffset, "applyPolyOffset", true);
        oc.write(applyDepthFunc, "applyDepthFunc", true);
        oc.write(applyLineWidth, "applyLineWidth", true);

    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        wireframe = ic.readBoolean("wireframe", false);
        cullMode = ic.readEnum("cullMode", FaceCullMode.class, FaceCullMode.Back);
        depthWrite = ic.readBoolean("depthWrite", true);
        depthTest = ic.readBoolean("depthTest", true);
        colorWrite = ic.readBoolean("colorWrite", true);
        blendMode = ic.readEnum("blendMode", BlendMode.class, BlendMode.Off);
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
        blendEquation = ic.readEnum("blendEquation", BlendEquation.class, BlendEquation.Add);
        blendEquationAlpha = ic.readEnum("blendEquationAlpha", BlendEquationAlpha.class, BlendEquationAlpha.InheritColor);
        depthFunc = ic.readEnum("depthFunc", TestFunction.class, TestFunction.LessOrEqual);
        lineWidth = ic.readFloat("lineWidth", 1);
        sfactorRGB = ic.readEnum("sfactorRGB", BlendFunc.class, BlendFunc.One);
        dfactorAlpha = ic.readEnum("dfactorRGB", BlendFunc.class, BlendFunc.Zero);
        sfactorRGB = ic.readEnum("sfactorAlpha", BlendFunc.class, BlendFunc.One);
        dfactorAlpha = ic.readEnum("dfactorAlpha", BlendFunc.class, BlendFunc.Zero);


        applyWireFrame = ic.readBoolean("applyWireFrame", true);
        applyCullMode = ic.readBoolean("applyCullMode", true);
        applyDepthWrite = ic.readBoolean("applyDepthWrite", true);
        applyDepthTest = ic.readBoolean("applyDepthTest", true);
        applyColorWrite = ic.readBoolean("applyColorWrite", true);
        applyBlendEquation = ic.readBoolean("applyBlendEquation", true);
        applyBlendEquationAlpha = ic.readBoolean("applyBlendEquationAlpha", true);
        applyBlendMode = ic.readBoolean("applyBlendMode", true);
        applyPolyOffset = ic.readBoolean("applyPolyOffset", true);
        applyDepthFunc = ic.readBoolean("applyDepthFunc", true);
        applyLineWidth = ic.readBoolean("applyLineWidth", true);

        
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
     * returns true if the given renderState is equal to this one
     * @param o the renderState to compare to
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

        if (blendEquation != rs.blendEquation) {
            return false;
        }

        if (blendEquationAlpha != rs.blendEquationAlpha) {
            return false;
        }

        if (blendMode != rs.blendMode) {
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

        if(lineWidth != rs.lineWidth){
            return false;
        }
        
        if (blendMode.equals(BlendMode.Custom)) {
           return sfactorRGB==rs.getCustomSfactorRGB()
               && dfactorRGB==rs.getCustomDfactorRGB()
               && sfactorAlpha==rs.getCustomSfactorAlpha()
               && dfactorAlpha==rs.getCustomDfactorAlpha();
           
        }

        return true;
    }

    /**
     * @deprecated Does nothing. Point sprite is already enabled by default for
     * all supported platforms. jME3 does not support rendering conventional
     * point clouds.
     */
    @Deprecated
    public void setPointSprite(boolean pointSprite) {
    }

    /**
     * @deprecated Does nothing. To use alpha test, set the
     * <code>AlphaDiscardThreshold</code> material parameter.
     * @param alphaFallOff does nothing
     */
    @Deprecated
    public void setAlphaFallOff(float alphaFallOff) {
    }

    /**
     * @deprecated Does nothing. To use alpha test, set the
     * <code>AlphaDiscardThreshold</code> material parameter.
     * @param alphaTest does nothing
     */
    @Deprecated
    public void setAlphaTest(boolean alphaTest) {
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
     * Set the blending equation.
     * <p>
     * When blending is enabled, (<code>blendMode</code> is not
     * {@link BlendMode#Off}) the input pixel will be blended with the pixel
     * already in the color buffer. The blending equation is determined by the
     * {@link BlendEquation}. For example, the mode {@link BlendMode#Additive}
     * and {@link BlendEquation#Add} will add the input pixel's color to the
     * color already in the color buffer:
     * <br/>
     * <code>Result = Source Color + Destination Color</code>
     * <br/>
     * However, the mode {@link BlendMode#Additive}
     * and {@link BlendEquation#Subtract} will subtract the input pixel's color to the
     * color already in the color buffer:
     * <br/>
     * <code>Result = Source Color - Destination Color</code>
     *
     * @param blendEquation The blend equation to use. 
     */
    public void setBlendEquation(BlendEquation blendEquation) {
        applyBlendEquation = true;
        this.blendEquation = blendEquation;
        cachedHashCode = -1;
    }
    
    /**
     * Set the blending equation for the alpha component.
     * <p>
     * When blending is enabled, (<code>blendMode</code> is not
     * {@link BlendMode#Off}) the input pixel will be blended with the pixel
     * already in the color buffer. The blending equation is determined by the
     * {@link BlendEquation} and can be overrode for the alpha component using
     * the {@link BlendEquationAlpha} . For example, the mode
     * {@link BlendMode#Additive} and {@link BlendEquationAlpha#Add} will add
     * the input pixel's alpha to the alpha component already in the color
     * buffer:
     * <br/>
     * <code>Result = Source Alpha + Destination Alpha</code>
     * <br/>
     * However, the mode {@link BlendMode#Additive} and
     * {@link BlendEquationAlpha#Subtract} will subtract the input pixel's alpha
     * to the alpha component already in the color buffer:
     * <br/>
     * <code>Result = Source Alpha - Destination Alpha</code>
     *
     * @param blendEquationAlpha The blend equation to use for the alpha
     *                           component.
     */
    public void setBlendEquationAlpha(BlendEquationAlpha blendEquationAlpha) {
        applyBlendEquationAlpha = true;
        this.blendEquationAlpha = blendEquationAlpha;
        cachedHashCode = -1;
    }

    
    /**
     * Sets the custom blend factors for <code>BlendMode.Custom</code> as 
     * defined by the appropriate <code>BlendFunc</code>.
     * 
     * @param sfactorRGB   The source blend factor for RGB components.
     * @param dfactorRGB   The destination blend factor for RGB components.
     * @param sfactorAlpha The source blend factor for the alpha component.
     * @param dfactorAlpha The destination blend factor for the alpha component.
     */
    public void setCustomBlendFactors(BlendFunc sfactorRGB, BlendFunc dfactorRGB, BlendFunc sfactorAlpha, BlendFunc dfactorAlpha)
    {
       this.sfactorRGB = sfactorRGB;
       this.dfactorRGB = dfactorRGB;
       this.sfactorAlpha = sfactorAlpha;
       this.dfactorAlpha = dfactorAlpha;
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
     * @deprecated
     */
    @Deprecated
    public void setAlphaFunc(TestFunction alphaFunc) {
    }

    /**
     * Sets the mesh line width.
     * This is to use in conjunction with {@link #setWireframe(boolean)} or with a mesh in {@link Mesh.Mode#Lines} mode.
     * @param lineWidth the line width.
     */
    public void setLineWidth(float lineWidth) {
        if (lineWidth < 1f) {
            throw new IllegalArgumentException("lineWidth must be greater than or equal to 1.0");
        }
        this.lineWidth = lineWidth;
        this.applyLineWidth = true;
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
     * Retrieve the blend equation.
     *
     * @return the blend equation.
     */
    public BlendEquation getBlendEquation() {
        return blendEquation;
    }
    
    /**
     * Retrieve the blend equation used for the alpha component.
     *
     * @return the blend equation for the alpha component.
     */
    public BlendEquationAlpha getBlendEquationAlpha() {
        return blendEquationAlpha;
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
     * Provides the source factor for the RGB components in 
     * <code>BlendMode.Custom</code>.
     * 
     * @return the custom source factor for RGB components.
     */
    public BlendFunc getCustomSfactorRGB() {
       return sfactorRGB;
    }
    
    /**
     * Provides the destination factor for the RGB components in 
     * <code>BlendMode.Custom</code>.
     * 
     * @return the custom destination factor for RGB components.
     */
    public BlendFunc getCustomDfactorRGB() {
       return dfactorRGB;
    }
    
    /**
     * Provides the source factor for the alpha component in 
     * <code>BlendMode.Custom</code>.
     * 
     * @return the custom destination factor for alpha component.
     */
    public BlendFunc getCustomSfactorAlpha() {
       return sfactorAlpha;
    }
    
    /**
     * Provides the destination factor for the alpha component in 
     * <code>BlendMode.Custom</code>.
     * 
     * @return the custom destination factor for alpha component.
     */
    public BlendFunc getCustomDfactorAlpha() {
       return dfactorAlpha;
    }
    
    /**
     * @return true
     * @deprecated Always returns true since point sprite is always enabled.
     * @see #setPointSprite(boolean)
     */
    @Deprecated
    public boolean isPointSprite() {
        return true;
    }

    /**
     * @deprecated To use alpha test, set the <code>AlphaDiscardThreshold</code>
     * material parameter.
     * @return false
     */
    public boolean isAlphaTest() {
        return false;
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
     * @return 0
     * @deprecated
     */
    @Deprecated
    public float getAlphaFallOff() {
        return 0f;
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
     * @return {@link TestFunction#Greater}.
     * @deprecated
     */
    @Deprecated
    public TestFunction getAlphaFunc() {
        return TestFunction.Greater;
    }

    /**
     * returns the wireframe line width
     *
     * @return the line width
     */
    public float getLineWidth() {
        return lineWidth;
    }



    public boolean isApplyBlendMode() {
        return applyBlendMode;
    }

    public boolean isApplyBlendEquation() {
        return applyBlendEquation;
    }

    public boolean isApplyBlendEquationAlpha() {
        return applyBlendEquationAlpha;
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


    public boolean isApplyPolyOffset() {
        return applyPolyOffset;
    }

    public boolean isApplyWireFrame() {
        return applyWireFrame;
    }

    public boolean isApplyDepthFunc() {
        return applyDepthFunc;
    }


    public boolean isApplyLineWidth() {
        return applyLineWidth;
    }

    /**
     *
     */
    public int contentHashCode() {
        if (cachedHashCode == -1){
            int hash = 7;
            hash = 79 * hash + (this.wireframe ? 1 : 0);
            hash = 79 * hash + (this.cullMode != null ? this.cullMode.hashCode() : 0);
            hash = 79 * hash + (this.depthWrite ? 1 : 0);
            hash = 79 * hash + (this.depthTest ? 1 : 0);
            hash = 79 * hash + (this.depthFunc != null ? this.depthFunc.hashCode() : 0);
            hash = 79 * hash + (this.colorWrite ? 1 : 0);
            hash = 79 * hash + (this.blendMode != null ? this.blendMode.hashCode() : 0);
            hash = 79 * hash + (this.blendEquation != null ? this.blendEquation.hashCode() : 0);
            hash = 79 * hash + (this.blendEquationAlpha != null ? this.blendEquationAlpha.hashCode() : 0);
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
            hash = 79 * hash + Float.floatToIntBits(this.lineWidth);
            
            hash = 79 * hash + this.sfactorRGB.hashCode();
            hash = 79 * hash + this.dfactorRGB.hashCode();
            hash = 79 * hash + this.sfactorAlpha.hashCode();
            hash = 79 * hash + this.dfactorAlpha.hashCode();
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
        if (additionalState.applyBlendEquation) {
            state.blendEquation = additionalState.blendEquation;
        } else {
            state.blendEquation = blendEquation;
        }
        if (additionalState.applyBlendEquationAlpha) {
            state.blendEquationAlpha = additionalState.blendEquationAlpha;
        } else {
            state.blendEquationAlpha = blendEquationAlpha;
        }        
        if (additionalState.applyBlendMode) {
            state.blendMode = additionalState.blendMode;
            if (additionalState.getBlendMode().equals(BlendMode.Custom)) {
               state.setCustomBlendFactors(
                additionalState.getCustomSfactorRGB(),
                additionalState.getCustomDfactorRGB(),
                additionalState.getCustomSfactorAlpha(),
                additionalState.getCustomDfactorAlpha());
            }
        } else {
            state.blendMode = blendMode;
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
        if (additionalState.applyLineWidth) {
            state.lineWidth = additionalState.lineWidth;
        } else {
            state.lineWidth = lineWidth;
        }
        state.cachedHashCode = -1;
        return state;
    }

    public void set(RenderState state) {
        wireframe = state.wireframe;
        cullMode = state.cullMode;
        depthWrite = state.depthWrite;
        depthTest = state.depthTest;
        colorWrite = state.colorWrite;
        blendMode = state.blendMode;
        offsetEnabled = state.offsetEnabled;
        offsetFactor = state.offsetFactor;
        offsetUnits = state.offsetUnits;
        stencilTest = state.stencilTest;
        frontStencilStencilFailOperation = state.frontStencilStencilFailOperation;
        frontStencilDepthFailOperation = state.frontStencilDepthFailOperation;
        frontStencilDepthPassOperation = state.frontStencilDepthPassOperation;
        backStencilStencilFailOperation = state.backStencilStencilFailOperation;
        backStencilDepthFailOperation = state.backStencilDepthFailOperation;
        backStencilDepthPassOperation = state.backStencilDepthPassOperation;
        frontStencilFunction = state.frontStencilFunction;
        backStencilFunction = state.backStencilFunction;
        blendEquationAlpha = state.blendEquationAlpha;
        blendEquation = state.blendEquation;
        depthFunc = state.depthFunc;
        lineWidth = state.lineWidth;

        applyWireFrame =  true;
        applyCullMode =  true;
        applyDepthWrite =  true;
        applyDepthTest =  true;
        applyColorWrite = true;
        applyBlendEquation =  true;
        applyBlendEquationAlpha =  true;
        applyBlendMode = true;
        applyPolyOffset =  true;
        applyDepthFunc = true;
        applyLineWidth = true;
        
        sfactorRGB = state.sfactorRGB;
        dfactorRGB = state.dfactorRGB;
        sfactorAlpha = state.sfactorAlpha;
        dfactorAlpha = state.dfactorAlpha;
    }

    @Override
    public String toString() {
        return "RenderState[\n"
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
                + "\nblendEquation=" + blendEquation
                + "\napplyBlendEquation=" + applyBlendEquation
                + "\napplyBlendEquationAlpha=" + applyBlendEquationAlpha
                + "\nblendMode=" + blendMode
                + "\napplyBlendMode=" + applyBlendMode
                + "\noffsetEnabled=" + offsetEnabled
                + "\napplyPolyOffset=" + applyPolyOffset
                + "\noffsetFactor=" + offsetFactor
                + "\noffsetUnits=" + offsetUnits
                + "\nlineWidth=" + lineWidth
                + (blendMode.equals(BlendMode.Custom)? "\ncustomBlendFactors=("+sfactorRGB+", "+dfactorRGB+", "+sfactorAlpha+", "+dfactorAlpha+")":"")
                +"\n]";
    }
}
