/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;

import org.jmonkeyengine.screenshottests.testframework.TestContainingApp;
import org.jmonkeyengine.screenshottests.testframework.AppRunner;
import org.jmonkeyengine.screenshottests.testframework.TestReportCaptureBase;
import org.junit.jupiter.api.Assertions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DesktopRunner implements AppRunner {

    private static final Logger logger = Logger.getLogger(DesktopRunner.class.getName());


    private static final Executor executor = Executors.newSingleThreadExecutor( (r) -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void runApplicationUntilScenarioCompletes(TestContainingApp application, CountDownLatch applicationFinishedLatch) {
        executor.execute(() -> application.start(JmeContext.Type.Display));

        application.onError = error -> {
            logger.log(Level.WARNING, "Error in test application", error);
            applicationFinishedLatch.countDown();
        };

        int maxWaitTimeMilliseconds = 45000;

        try {
            boolean exitedProperly = applicationFinishedLatch.await(maxWaitTimeMilliseconds, TimeUnit.MILLISECONDS);

            if (!exitedProperly) {
                logger.warning("Test driver did not exit in " + maxWaitTimeMilliseconds + "ms. Timed out");
            }
            application.stop(true);
            Thread.sleep(1000); //give time for openGL is fully released before starting a new test (get random JVM crashes without this)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getChangedImagesDirectory() {
        return Paths.get("build/changed-images/");
    }

    public Path getReportsDirectory() {
        return Paths.get("build/reports/");
    }

    @Override
    public <V> V fail(String s) {
        return Assertions.fail(s);
    }

    @Override
    public void saveGeneratedImageToChangedImages(Image generatedImage, String fileName) {
        Image rgbaImage = TestReportCaptureBase.convertToRGBA8(generatedImage);

        Path savedImage = getChangedImagesDirectory().resolve(fileName);
        try {
            Files.createDirectories(savedImage.getParent());
            try (FileOutputStream fileOutBuf = new FileOutputStream(savedImage.toFile())) {
                JmeSystem.writeImageFile(fileOutBuf, "png",rgbaImage.getData(0), rgbaImage.getWidth(), rgbaImage.getHeight());
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
