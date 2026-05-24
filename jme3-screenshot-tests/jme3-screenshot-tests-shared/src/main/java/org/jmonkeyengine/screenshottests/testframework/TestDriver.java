/*
 * Copyright (c) 2024 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework;

import com.aventstack.extentreports.ExtentTest;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The test driver injects the screenshot taking into the application lifecycle, pauses the main test thread until the
 * screenshots have been taken and then compares the screenshots to the expected images.
 *
 * @author Richard Tingle (aka richtea)
 *
 */
public class TestDriver extends BaseAppState{

    private static final Logger logger = Logger.getLogger(TestDriver.class.getName());


    int tick = 0;

    Collection<Integer> framesToTakeScreenshotsOn;

    public CountDownLatch waitLatch;

    private final int tickToTerminateApp;

    OffScreenshotAppState offScreenshotAppState;

    ScenarioScreenshotRecorder screenshotsAtFrames = new ScenarioScreenshotRecorder();

    private final String scenarioName;

    public TestDriver(String scenarioName, Collection<Integer> framesToTakeScreenshotsOn){
        this.scenarioName = scenarioName;
        this.framesToTakeScreenshotsOn = framesToTakeScreenshotsOn;
        this.tickToTerminateApp = framesToTakeScreenshotsOn.stream().mapToInt(i -> i).max().orElse(0) + 1;
    }

    @Override
    public void update(float tpf){
        super.update(tpf);

        if(framesToTakeScreenshotsOn.contains(tick)){
            Path screenshotPath;
            try {
                screenshotPath = Files.createTempFile("screenshot_" + scenarioName + "_" + tick + "_", ".png");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            screenshotPath.toFile().deleteOnExit();
            screenshotsAtFrames.recordScreenshot(scenarioName, tick, screenshotPath);
            offScreenshotAppState.takeScreenshot(screenshotPath);
        }
        if(tick >= tickToTerminateApp){
            waitLatch.countDown();
        }

        tick++;
    }

    @Override protected void initialize(Application app){
        AppSettings settings = app.getContext().getSettings();
        int width = settings.getWidth();
        int height = settings.getHeight();
        Texture2D renderTexture = new Texture2D(width, height, Image.Format.RGBA8);
        renderTexture.setMinFilter(Texture.MinFilter.BilinearNearestMipMap);
        renderTexture.setMagFilter(Texture.MagFilter.Bilinear);
        renderTexture.getImage().setColorSpace(ColorSpace.sRGB);

        FrameBuffer offBuffer = new FrameBuffer(width, height, 1);
        offBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(Image.Format.Depth));
        offBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(renderTexture));
        offBuffer.setSrgb(true);

        offScreenshotAppState = new OffScreenshotAppState(renderTexture, offBuffer);

        app.getRenderer().setMainFrameBufferOverride(offBuffer);


        getStateManager().attach(offScreenshotAppState);
    }

    @Override protected void cleanup(Application app){}

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

}
