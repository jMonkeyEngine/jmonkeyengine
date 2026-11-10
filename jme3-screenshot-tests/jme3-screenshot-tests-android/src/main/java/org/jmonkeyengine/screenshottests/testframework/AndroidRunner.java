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

import com.jme3.texture.Image;

import org.junit.Assert;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@SuppressLint("RestrictedApi")
public class AndroidRunner implements AppRunner {

    /**
     * Instrumentation argument used to select the {@link ScreenshotTestMode}, the Android
     * equivalent of {@link org.jmonkeyengine.screenshottests.testframework.ScreenshotTest#MODE_SYSTEM_PROPERTY}.
     * Pass it with
     * {@code -Pandroid.testInstrumentationRunnerArguments.jmeScreenshotMode=generate}.
     */
    public static final String MODE_INSTRUMENTATION_ARGUMENT = "jmeScreenshotMode";

    /**
     * Directory (within test storage) that reference images are written to and read from.
     */
    public static final String REFERENCE_IMAGES_DIRECTORY = "reference-images";

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
    public ScreenshotTestMode getScreenshotTestMode() {
        return ScreenshotTestMode.fromConfigurationValue(
                InstrumentationRegistry.getArguments().getString(MODE_INSTRUMENTATION_ARGUMENT));
    }

    /**
     * Reads a reference image from the test storage input directory, where the reference run's
     * images are placed between the two passes.
     *
     * <p>
     * Test storage belongs to the test services app rather than to the app under test, which is
     * what makes it the right home for these: it survives the
     * {@code adb uninstall org.jmonkeyengine.screenshottests.android} that the CI script does
     * between retry attempts, whereas the app's own external files directory does not.
     * </p>
     */
    @Override
    public Optional<Image> loadReferenceImage(String fileName) {
        try (InputStream referenceImage = testStorage.openInputFile(REFERENCE_IMAGES_DIRECTORY + "/" + fileName)) {
            return Optional.of(ScreenshotImageIo.readImage(referenceImage));
        } catch (FileNotFoundException e) {
            // No image under that name, so the test did not exist on the reference branch.
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Could not read reference image " + fileName, e);
        }
    }

    @Override
    public void saveReferenceImage(Path screenshot, String fileName) {
        String relativePath = REFERENCE_IMAGES_DIRECTORY + "/" + fileName;
        try (OutputStream out = getPersistentFileOutputStream(relativePath)) {
            Files.copy(screenshot, out);
        } catch (IOException e) {
            throw new RuntimeException("Could not write " + relativePath, e);
        }
    }

    /**
     * The marker is written into test storage by the CI script once the generate pass has come back
     * clean, since "the run finished with nothing failing" is not something the run itself can know
     * from the inside.
     */
    @Override
    public boolean isReferenceRunComplete() {
        try (InputStream marker = testStorage.openInputFile(
                REFERENCE_IMAGES_DIRECTORY + "/" + ScreenshotTest.REFERENCE_RUN_COMPLETE_MARKER)) {
            return marker != null;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException("Could not read the reference run marker", e);
        }
    }

    @Override
    public void saveGeneratedImageToChangedImages(Image generatedImage, String fileName) {
        writePng(generatedImage, "changed-images/" + fileName);
    }

    private void writePng(Image image, String relativePath) {
        try (OutputStream out = getPersistentFileOutputStream(relativePath)) {
            ScreenshotImageIo.writePng(image, out);
        } catch (IOException e) {
            throw new RuntimeException("Could not write " + relativePath, e);
        }
    }
}
