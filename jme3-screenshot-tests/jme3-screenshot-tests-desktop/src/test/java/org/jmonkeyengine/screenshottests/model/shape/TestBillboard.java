/*
 * Copyright (c) 2025 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.model.shape;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Quad;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTestBase;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * Screenshot test for the Billboard test.
 * 
 * <p>This test creates three different billboard alignments (Screen, Camera, AxialY)
 * with different colored quads. Each billboard is positioned at a different x-coordinate
 * and has a blue Z-axis arrow attached to it. Screenshots are taken from three different angles:
 * front, above, and right.
 * 
 * @author Richard Tingle (screenshot test adaptation)
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TestBillboard extends ScreenshotTestBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
                Arguments.of("fromFront", new Vector3f(0, 1, 15)),
                Arguments.of("fromAbove", new Vector3f(0, 15, 6)),
                Arguments.of("fromRight", new Vector3f(-15, 10, 5))
        );
    }

    /**
     * A billboard test with the specified camera parameters.
     *
     * @param cameraPosition The position of the camera
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testParameters")
    public void testBillboard(String testName, Vector3f cameraPosition, TestInfo testInfo) {
        String imageName = testInfo.getTestClass().get().getName() + "." + testInfo.getTestMethod().get().getName() + "_" + testName;

        screenshotTest(new BaseAppState() {
            @Override
            protected void initialize(Application app) {
                SimpleApplication simpleApplication = (SimpleApplication) app;
                Node rootNode = simpleApplication.getRootNode();

                // Set up the camera
                simpleApplication.getCamera().setLocation(cameraPosition);
                simpleApplication.getCamera().lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

                // Set background color
                simpleApplication.getViewPort().setBackgroundColor(ColorRGBA.DarkGray);

                // Create grid
                Geometry grid = makeShape(simpleApplication, "DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
                grid.center().move(0, 0, 0);
                rootNode.attachChild(grid);

                // Create billboards with different alignments
                Node node = createBillboard(simpleApplication, BillboardControl.Alignment.Screen, ColorRGBA.Red);
                node.setLocalTranslation(-6f, 0, 0);
                rootNode.attachChild(node);

                node = createBillboard(simpleApplication, BillboardControl.Alignment.Camera, ColorRGBA.Green);
                node.setLocalTranslation(-2f, 0, 0);
                rootNode.attachChild(node);

                node = createBillboard(simpleApplication, BillboardControl.Alignment.AxialY, ColorRGBA.Blue);
                node.setLocalTranslation(2f, 0, 0);
                rootNode.attachChild(node);
            }

            @Override
            protected void cleanup(Application app) {}

            @Override
            protected void onEnable() {}

            @Override
            protected void onDisable() {}

            private Node createBillboard(SimpleApplication app, BillboardControl.Alignment alignment, ColorRGBA color) {
                Node node = new Node("Parent");
                Quad quad = new Quad(2, 2);
                Geometry g = makeShape(app, alignment.name(), quad, color);
                BillboardControl bc = new BillboardControl();
                bc.setAlignment(alignment);
                g.addControl(bc);
                node.attachChild(g);
                node.attachChild(makeShape(app, "ZAxis", new Arrow(Vector3f.UNIT_Z), ColorRGBA.Blue));
                return node;
            }

            private Geometry makeShape(SimpleApplication app, String name, Mesh shape, ColorRGBA color) {
                Geometry geo = new Geometry(name, shape);
                Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", color);
                geo.setMaterial(mat);
                return geo;
            }
        })
        .setBaseImageFileName(imageName)
        .setFramesToTakeScreenshotsOn(1)
        .run();
    }
}
