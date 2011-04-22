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
package com.jme3.material;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

public class RenderState implements Cloneable, Savable {

    public static final RenderState DEFAULT = new RenderState();
    public static final RenderState NULL = new RenderState();
    public static final RenderState ADDITIONAL = new RenderState();

    public enum TestFunc {

        Never,
        Equal,
        Less,
        LessOrEqual,
        Greater,
        GreaterOrEqual,
        NotEqual,
        Always,
    }

    public enum BlendMode {

        /**
         * No blending mode is used.
         */
        Off,
        /**
         * Additive blending. For use with glows and particle emitters.
         *
         * Result = Source Color + Destination Color
         */
        Additive,
        /**
         * Premultiplied alpha blending, for use with premult alpha textures.
         *
         * Result = Source Color + (Dest Color * 1 - Source Alpha)
         */
        PremultAlpha,
        /**
         * Additive blending that is multiplied with source alpha.
         * For use with glows and particle emitters.
         *
         * Result = (Source Alpha * Source Color) + Dest Color
         */
        AlphaAdditive,
        /**
         * Color blending, blends in color from dest color
         * using source color.
         *
         * Result = Source Color + (1 - Source Color) * Dest Color
         */
        Color,
        /**
         * Alpha blending, interpolates to source color from dest color
         * using source alpha.
         *
         * Result = Source Alpha * Source Color +
         *          (1 - Source Alpha) * Dest Color
         */
        Alpha,
        /**
         * Multiplies the source and dest colors.
         *
         * Result = Source Color * Dest Color
         */
        Modulate,
        /**
         * Multiplies the source and dest colors then doubles the result.
         *
         * Result = 2 * Source Color * Dest Color
         */
        ModulateX2
    }

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


    public enum StencilOperation {
        Keep, //keep the current value
        Zero, //set the value to 0
        Replace,  //sets the buffer to
        Increment,
        IncrementWrap,
        Decrement,
        DecrementWrap,
        Invert
    }

    public enum StencilFunction {
        Never,
        Less,
        LessEqual,
        Greater,
        GreaterEqual,
        Equal,
        NotEqual,
        Always
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
    boolean offsetEnabled = false;
    boolean applyPolyOffset = true;
    float offsetFactor = 0;
    float offsetUnits = 0;
    boolean stencilTest = false;
    StencilOperation frontStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation frontStencilDepthPassOperation = StencilOperation.Keep;
    StencilOperation backStencilStencilFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthFailOperation = StencilOperation.Keep;
    StencilOperation backStencilDepthPassOperation = StencilOperation.Keep;
    StencilFunction frontStencilFunction = StencilFunction.Always;
    StencilFunction backStencilFunction = StencilFunction.Always;

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
        oc.write(frontStencilFunction, "frontStencilFunction", StencilFunction.Always);
        oc.write(backStencilFunction, "backStencilFunction", StencilFunction.Always);
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
        frontStencilFunction = ic.readEnum("frontStencilFunction", StencilFunction.class, StencilFunction.Always);
        backStencilFunction = ic.readEnum("backStencilFunction", StencilFunction.class, StencilFunction.Always);
    }

    @Override
    public RenderState clone() {
        try {
            return (RenderState) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    public boolean isPointSprite() {
        return pointSprite;
    }

    public void setPointSprite(boolean pointSprite) {
        applyPointSprite = true;
        this.pointSprite = pointSprite;
    }

    public boolean isColorWrite() {
        return colorWrite;
    }

    public float getPolyOffsetFactor() {
        return offsetFactor;
    }

    public float getPolyOffsetUnits() {
        return offsetUnits;
    }

    public boolean isPolyOffset() {
        return offsetEnabled;
    }

    public float getAlphaFallOff() {
        return alphaFallOff;
    }

    public void setAlphaFallOff(float alphaFallOff) {
        applyAlphaFallOff = true;
        this.alphaFallOff = alphaFallOff;
    }

    public boolean isAlphaTest() {
        return alphaTest;
    }

    public void setAlphaTest(boolean alphaTest) {
        applyAlphaTest = true;
        this.alphaTest = alphaTest;
    }

    public FaceCullMode getFaceCullMode() {
        return cullMode;
    }

    public void setColorWrite(boolean colorWrite) {
        applyColorWrite = true;
        this.colorWrite = colorWrite;
    }

    /**
      * Offsets the on-screen z-order of the material's polygons, to combat visual artefacts like
      * stitching, bleeding and z-fighting for overlapping polygons.
      * Factor and units are summed to produce the depth offset. This offset is applied in screen space,
      * typically with positive Z pointing into the screen.
      * Typical values are (1.0f, 1.0f) or (-1.0f, -1.0f)
      *
      * @see <a href="http://www.opengl.org/resources/faq/technical/polygonoffset.htm" rel="nofollow">http://www.opengl.org/resources/faq/technical/polygonoffset.htm</a>
      * @param factor scales the maximum Z slope, with respect to X or Y of the polygon
      * @param units scales the minimum resolvable depth buffer value
      **/
    public void setPolyOffset(float factor, float units) {
        applyPolyOffset = true;
        offsetEnabled = true;
        offsetFactor = factor;
        offsetUnits = units;
    }

    public void setStencil(boolean enabled,
            StencilOperation _frontStencilStencilFailOperation,
            StencilOperation _frontStencilDepthFailOperation,
            StencilOperation _frontStencilDepthPassOperation,
            StencilOperation _backStencilStencilFailOperation,
            StencilOperation _backStencilDepthFailOperation,
            StencilOperation _backStencilDepthPassOperation,
            StencilFunction _frontStencilFunction,
            StencilFunction _backStencilFunction){

        stencilTest = enabled;
        this.frontStencilStencilFailOperation = _frontStencilStencilFailOperation;
        this.frontStencilDepthFailOperation = _frontStencilDepthFailOperation;
        this.frontStencilDepthPassOperation = _frontStencilDepthPassOperation;
        this.backStencilStencilFailOperation = _backStencilStencilFailOperation;
        this.backStencilDepthFailOperation = _backStencilDepthFailOperation;
        this.backStencilDepthPassOperation = _backStencilDepthPassOperation;
        this.frontStencilFunction = _frontStencilFunction;
        this.backStencilFunction = _backStencilFunction;
    }

    public boolean isStencilTest() {
        return stencilTest;
    }

    public StencilOperation getFrontStencilStencilFailOperation(){ return frontStencilStencilFailOperation; }
    public StencilOperation getFrontStencilDepthFailOperation(){ return frontStencilDepthFailOperation; }
    public StencilOperation getFrontStencilDepthPassOperation(){ return frontStencilDepthPassOperation; }
    public StencilOperation getBackStencilStencilFailOperation(){ return backStencilStencilFailOperation; }
    public StencilOperation getBackStencilDepthFailOperation(){ return backStencilDepthFailOperation; }
    public StencilOperation getBackStencilDepthPassOperation(){ return backStencilDepthPassOperation; }

    public StencilFunction getFrontStencilFunction(){ return frontStencilFunction; }
    public StencilFunction getBackStencilFunction(){ return backStencilFunction; }

    public void setFaceCullMode(FaceCullMode cullMode) {
        applyCullMode = true;
        this.cullMode = cullMode;
    }

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(BlendMode blendMode) {
        applyBlendMode = true;
        this.blendMode = blendMode;
    }

    public boolean isDepthTest() {
        return depthTest;
    }

    public void setDepthTest(boolean depthTest) {
        applyDepthTest = true;
        this.depthTest = depthTest;
    }

    public boolean isDepthWrite() {
        return depthWrite;
    }

    public void setDepthWrite(boolean depthWrite) {
        applyDepthWrite = true;
        this.depthWrite = depthWrite;
    }

    public boolean isWireframe() {
        return wireframe;
    }

    public void setWireframe(boolean wireframe) {
        applyWireFrame = true;
        this.wireframe = wireframe;
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

    public RenderState copyMergedTo(RenderState additionalState,RenderState state) {
        if (additionalState == null) {
            return this;
        }

        if (additionalState.isApplyPointSprite()) {
            state.pointSprite = additionalState.pointSprite;
        }else{
            state.pointSprite = pointSprite;
        }
        if (additionalState.isApplyWireFrame()) {
            state.wireframe = additionalState.wireframe;
        }else{
            state.wireframe = wireframe;
        }

        if (additionalState.isApplyCullMode()) {
            state.cullMode = additionalState.cullMode;
        }else{
            state.cullMode = cullMode;
        }
        if (additionalState.isApplyDepthWrite()) {
            state.depthWrite = additionalState.depthWrite;
        }else{
            state.depthWrite = depthWrite;
        }
        if (additionalState.isApplyDepthTest()) {
            state.depthTest = additionalState.depthTest;
        }else{
            state.depthTest = depthTest;
        }
        if (additionalState.isApplyColorWrite()) {
            state.colorWrite = additionalState.colorWrite;
        }else{
            state.colorWrite = colorWrite;
        }
        if (additionalState.isApplyBlendMode()) {
            state.blendMode = additionalState.blendMode;
        }else{
            state.blendMode = blendMode;
        }
        if (additionalState.isApplyAlphaTest()) {
            state.alphaTest = additionalState.alphaTest;
        }else{
            state.alphaTest = alphaTest;
        }

        if (additionalState.isApplyAlphaFallOff()) {
            state.alphaFallOff = additionalState.alphaFallOff;
        }else{
            state.alphaFallOff = alphaFallOff;
        }
        if (additionalState.isApplyPolyOffset()) {
            state.offsetEnabled = additionalState.offsetEnabled;
            state.offsetFactor = additionalState.offsetFactor;
            state.offsetUnits = additionalState.offsetUnits;
        }else{
            state.offsetEnabled = offsetEnabled;
            state.offsetFactor = offsetFactor;
            state.offsetUnits = offsetUnits;
        }
        return state;
    }

    @Override
    public String toString() {
        return "RenderState{" + "pointSprite=" + pointSprite + "applyPointSprite=" + applyPointSprite + "wireframe=" + wireframe + "applyWireFrame=" + applyWireFrame + "cullMode=" + cullMode + "applyCullMode=" + applyCullMode + "depthWrite=" + depthWrite + "applyDepthWrite=" + applyDepthWrite + "depthTest=" + depthTest + "applyDepthTest=" + applyDepthTest + "colorWrite=" + colorWrite + "applyColorWrite=" + applyColorWrite + "blendMode=" + blendMode + "applyBlendMode=" + applyBlendMode + "alphaTest=" + alphaTest + "applyAlphaTest=" + applyAlphaTest + "alphaFallOff=" + alphaFallOff + "applyAlphaFallOff=" + applyAlphaFallOff + "offsetEnabled=" + offsetEnabled + "applyPolyOffset=" + applyPolyOffset + "offsetFactor=" + offsetFactor + "offsetUnits=" + offsetUnits + '}';
    }
}
