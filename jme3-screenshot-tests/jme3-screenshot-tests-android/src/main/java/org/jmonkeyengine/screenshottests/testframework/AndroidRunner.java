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

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.services.storage.TestStorage;

import com.jme3.system.JmeSystem;
import com.jme3.texture.Image;

import org.junit.Assert;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@SuppressLint("RestrictedApi")
public class AndroidRunner implements AppRunner {

    private static final Logger logger = Logger.getLogger(AndroidRunner.class.getName());

    /**
     * Any created files are normally deleted at the end of the test. Using TestStorage
     * we can persist them (requires test-services to be installed, see the readme)
     */
    TestStorage testStorage = new TestStorage();

    @Override
    public void runApplicationUntilScenarioCompletes(TestContainingApp application, CountDownLatch applicationFinishedLatch) {

        FragmentFactory fragmentFactory = new FragmentFactory(){
            @NonNull
            @Override
            public Fragment instantiate(@NonNull ClassLoader classLoader, @NonNull String className) {
                if (className.equals(AndroidTestHarness.class.getName())) {
                    return new AndroidTestHarness(application, applicationFinishedLatch);
                }
                return super.instantiate(classLoader, className);
            }
        };
        try (FragmentScenario<AndroidTestHarness> scenario = FragmentScenario.launchInContainer(
                AndroidTestHarness.class,
                Bundle.EMPTY,
                androidx.appcompat.R.style.Theme_AppCompat,
                fragmentFactory
        )) {
            int maxWaitTimeMilliseconds = 45000;

            try {
                boolean exitedProperly = applicationFinishedLatch.await(maxWaitTimeMilliseconds, TimeUnit.MILLISECONDS);

                if (!exitedProperly) {
                    logger.warning("Test driver did not exit in " + maxWaitTimeMilliseconds + "ms. Timed out");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        try{
            Thread.sleep(1000); //give time for openGL is fully released before starting a new test (get random JVM crashes without this)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Path getChangedImagesDirectory() {
        return InstrumentationRegistry
                .getInstrumentation()
                .getTargetContext()
                .getExternalFilesDir(null)
                .toPath()
                .resolve("changed-images");
    }

    public OutputStream getPersistentFileOutputStream(String relativePath){
        try{
            return testStorage.openOutputFile(relativePath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <V> V fail(String s) {
        Assert.fail(s);
        return (V)null;
    }

    @Override
    public void saveGeneratedImageToChangedImages(Image generatedImage, String fileName) {
        Image rgbaImage = TestReportCaptureBase.convertToRGBA8(generatedImage);
        try (OutputStream out = getPersistentFileOutputStream("changed-images/" + fileName)) {
            JmeSystem.writeImageFile(out, "png",rgbaImage.getData(0), rgbaImage.getWidth(), rgbaImage.getHeight());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
