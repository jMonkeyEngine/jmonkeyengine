/*
 * Copyright (c) 2021 jMonkeyEngine
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

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cloning/saving/loading a RenderState. Related issues include #1718 and
 * #1723.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RenderStateTest {
    // *************************************************************************
    // fields

    private static final AssetManager assetManager = new DesktopAssetManager();
    private static final boolean testSerialization = true;
    private static final RenderState testObject = new RenderState();
    // *************************************************************************
    // new methods exposed

    @Test
    public void testCloneRenderState() {
        for (RenderState.BlendEquation equation : RenderState.BlendEquation.values()) {
            testObject.setBlendEquation(equation);
            test();
        }

        for (RenderState.BlendEquationAlpha eqAlpha : RenderState.BlendEquationAlpha.values()) {
            testObject.setBlendEquationAlpha(eqAlpha);
            test();
        }

        for (RenderState.BlendMode mode : RenderState.BlendMode.values()) {
            testObject.setBlendMode(mode);
            test();
        }

        testObject.setColorWrite(true);
        test();
        testObject.setColorWrite(false);
        test();

        testCustomBlendFactors();

        for (RenderState.TestFunction function : RenderState.TestFunction.values()) {
            testObject.setDepthFunc(function);
            test();
        }

        testObject.setDepthTest(true);
        test();
        testObject.setDepthTest(false);
        test();

        testObject.setDepthWrite(true);
        test();
        testObject.setDepthWrite(false);
        test();

        for (RenderState.FaceCullMode mode : RenderState.FaceCullMode.values()) {
            testObject.setFaceCullMode(mode);
            test();
        }

        testObject.setLineWidth(1f);
        test();
        testObject.setLineWidth(9f);
        test();

        for (int factor = -1; factor <= 1; ++factor) {
            for (int units = -1; units <= 1; ++units) {
                testObject.setPolyOffset(factor, units);
                test();
            }
        }

        testStencils();

        testObject.setWireframe(true);
        test();
        testObject.setWireframe(false);
        test();
    }
    // *************************************************************************
    // private methods

    private static void test() {
        /*
         * Test a clone for equality.
         */
        RenderState clone = testObject.clone();
        Assert.assertEquals(testObject, clone);

        if (testSerialization) {
            /*
             * Test a save-and-load copy for equality.
             */
            RenderState copy = BinaryExporter.saveAndLoad(assetManager, testObject);
            Assert.assertEquals(testObject, copy);
        }
    }

    /**
     * Tests 242 of the 14,641 possile blend-factor combinations.
     */
    private static void testCustomBlendFactors() {
        final RenderState.BlendFunc dAlpha = RenderState.BlendFunc.Zero;
        final RenderState.BlendFunc sAlpha = RenderState.BlendFunc.Dst_Color;

        for (RenderState.BlendFunc sourceRgb : RenderState.BlendFunc.values()) {
            for (RenderState.BlendFunc destRgb : RenderState.BlendFunc.values()) {
                testObject.setCustomBlendFactors(sourceRgb, destRgb, sAlpha, dAlpha);
                test();
            }
        }

        final RenderState.BlendFunc dRgb = RenderState.BlendFunc.One_Minus_Dst_Alpha;
        final RenderState.BlendFunc sRgb = RenderState.BlendFunc.Src_Color;

        for (RenderState.BlendFunc sourceAlpha : RenderState.BlendFunc.values()) {
            for (RenderState.BlendFunc destAlpha : RenderState.BlendFunc.values()) {
                testObject.setCustomBlendFactors(sRgb, dRgb, sourceAlpha, destAlpha);
                test();
            }
        }
    }

    /**
     * Tests a subset of the possile stencil combinations.
     */
    private static void testStencils() {
        boolean enabled = true;

        final RenderState.StencilOperation frontSfo = RenderState.StencilOperation.Increment;
        final RenderState.StencilOperation frontDfo = RenderState.StencilOperation.Invert;
        final RenderState.StencilOperation frontDpo = RenderState.StencilOperation.Zero;
        final RenderState.StencilOperation backSfo = RenderState.StencilOperation.Replace;
        final RenderState.StencilOperation backDfo = RenderState.StencilOperation.DecrementWrap;
        final RenderState.StencilOperation backDpo = RenderState.StencilOperation.Keep;
        /*
         * Vary the test functions (8th and 9th arguments).
         */
        for (RenderState.TestFunction front : RenderState.TestFunction.values()) {
            for (RenderState.TestFunction back : RenderState.TestFunction.values()) {
                testObject.setStencil(enabled,
                        frontSfo, frontDfo, frontDpo, backSfo, backDfo, backDpo,
                        front, back);
                test();
            }
        }

        final RenderState.TestFunction front = RenderState.TestFunction.GreaterOrEqual;
        final RenderState.TestFunction back = RenderState.TestFunction.NotEqual;
        /*
         * Vary the 2nd, 4th, and 7th arguments.
         */
        for (RenderState.StencilOperation arg2 : RenderState.StencilOperation.values()) {
            for (RenderState.StencilOperation arg4 : RenderState.StencilOperation.values()) {
                for (RenderState.StencilOperation arg7 : RenderState.StencilOperation.values()) {
                    testObject.setStencil(enabled,
                            arg2, frontDfo, arg4, backSfo, backDfo, arg7,
                            front, back);
                    test();
                }
            }
        }
        /*
         * Vary the 3rd, 5th, and 6th arguments.
         */
        for (RenderState.StencilOperation arg3 : RenderState.StencilOperation.values()) {
            for (RenderState.StencilOperation arg5 : RenderState.StencilOperation.values()) {
                for (RenderState.StencilOperation arg6 : RenderState.StencilOperation.values()) {
                    testObject.setStencil(enabled,
                            frontSfo, arg3, frontDpo, arg5, arg6, backDpo,
                            front, back);
                    test();
                }
            }
        }

        enabled = false;
        testObject.setStencil(enabled,
                frontSfo, frontDfo, frontDpo, backSfo, backDfo, backDpo,
                front, back);
        test();
    }
}
