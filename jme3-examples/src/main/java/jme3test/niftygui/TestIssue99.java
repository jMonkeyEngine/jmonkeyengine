/*
 * Copyright (c) 2019-2022 jMonkeyEngine
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
package jme3test.niftygui;

import com.jme3.app.SimpleApplication;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.texture.image.ColorSpace;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

/**
 * Test case for JME issue #99: blendMode="multiply" in Nifty renders
 * incorrectly.
 * <p>
 * If successful, two text labels will be legible. If unsuccessful, only the top
 * one will be legible.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue99
        extends SimpleApplication
        implements ScreenController {

    public static void main(String[] args) {
        TestIssue99 app = new TestIssue99();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        /*
         * GUI requires a cursor; prevent flyCam from hiding it.
         */
        flyCam.setDragToRotate(true);
        /*
         * Start NiftyGUI without the batched renderer.
         */
        ColorSpace colorSpace = renderer.isMainFrameBufferSrgb()
                ? ColorSpace.sRGB : ColorSpace.Linear;
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
                assetManager, inputManager, audioRenderer, guiViewPort, colorSpace);
        guiViewPort.addProcessor(niftyDisplay);
        /*
         * Load GUI controls, styles, and layout from XML assets.
         */
        Nifty nifty = niftyDisplay.getNifty();
        nifty.loadControlFile("nifty-default-controls.xml");
        nifty.loadStyleFile("nifty-default-styles.xml");
        nifty.fromXml("Interface/Nifty/test-issue-99.xml",
                "test-issue-99", this);
    }

    /**
     * A callback from Nifty, invoked when the screen gets enabled for the first
     * time.
     *
     * @param nifty (not null)
     * @param screen (not null)
     */
    @Override
    public void bind(Nifty nifty, Screen screen) {
    }

    /**
     * A callback from Nifty, invoked each time the screen shuts down.
     */
    @Override
    public void onEndScreen() {
    }

    /**
     * A callback from Nifty, invoked each time the screen starts up.
     */
    @Override
    public void onStartScreen() {
    }
}
