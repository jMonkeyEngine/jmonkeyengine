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
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.texture.Image;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * This interface is to allow the desktop vs android specificness to be passed down into the
 * main test on run
 */
public interface AppRunner {

    void runApplicationUntilScenarioCompletes(TestContainingApp application, CountDownLatch applicationFinishedLatch);

    Path getChangedImagesDirectory();

    <V> V fail(String s);

    void saveGeneratedImageToChangedImages(Image generatedImage, String fileName);

    /**
     * Returns whether this run is generating reference images or comparing against them. How that
     * is configured is platform specific: a system property on desktop, an instrumentation
     * argument on Android.
     *
     * @return the mode this run is in
     */
    ScreenshotTestMode getScreenshotTestMode();

    /**
     * Loads a reference image produced by an earlier {@link ScreenshotTestMode#GENERATE} run.
     *
     * <p>
     * An empty result means the reference run produced no image under this name, which is taken to
     * mean the test did not exist on the reference branch. Note that reference images do not live
     * in the same place on every platform (a directory on desktop, test storage on Android), which
     * is why loading them belongs here rather than in the shared test code.
     * </p>
     *
     * @param fileName the reference image file name, e.g. {@code com.example.TestFoo.testFoo_f1.png}
     * @return the reference image, or empty if there is none under that name
     */
    Optional<Image> loadReferenceImage(String fileName);

    /**
     * Copies a screenshot, byte for byte, to be the reference for later comparison runs on this
     * machine, so that a reference has been through exactly the same encoding as the screenshots
     * it is compared with.
     */
    void saveReferenceImage(Path screenshot, String fileName);

    /**
     * Returns whether the reference images available here were left by a reference run that
     * finished with every test passing, i.e. whether
     * {@link ScreenshotTest#REFERENCE_RUN_COMPLETE_MARKER} is present alongside them.
     *
     * <p>
     * A comparison run must refuse to run without this. Reference images that are missing because
     * the reference run died look exactly like reference images that are missing because the tests
     * are new, and the second of those does not fail the run.
     * </p>
     *
     * @return true if a completed reference run left its images here
     */
    boolean isReferenceRunComplete();
}
