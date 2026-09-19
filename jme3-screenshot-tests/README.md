# jme3-screenshot-tests

This module contains tests that compare screenshots of the JME3 test applications to reference images. Think of these like visual unit tests

The tests are run using the following command:

```
 ./gradlew :jme3-screenshot-test:screenshotTest
```

This runs them with the OpenGL 4.5 renderer. To run them with the ANGLE GLES3 renderer instead:

```
 ./gradlew :jme3-screenshot-test:screenshotTestAngle
```

Note: ANGLE needs a Wayland compositor (its EGL binaries have no X11 platform support).
On a headless machine start one first, e.g. `weston --backend=headless-backend.so --socket=wayland-1`,
with `XDG_RUNTIME_DIR` set, `XDG_SESSION_TYPE=wayland` and `WAYLAND_DISPLAY=wayland-1` exported.

## ANGLE reference images

ANGLE renders into files suffixed with `_angle` (e.g. `..._f1_angle.png`), kept next to the
OpenGL references in `src/test/resources` so the two backends never overwrite each other's
reference images. When accepting new ANGLE images, copy the `*_angle.png` files from the
`screenshot-test-report-angle` CI artifact (or `build/changed-images` locally).

This will create a report in `jme3-screenshot-test/build/reports/ScreenshotDiffReport.html` that shows the differences between the reference images and the screenshots taken during the test run. Note that this is an ExtentReport. 

This is most reliable when run on the CI server. The report can be downloaded from the artifacts section of the pipeline (once the full pipeline has completed). If you go into
the Actions tab (on GitHub) and find your pipeline you can download the report from the Artifacts section. It will be called screenshot-test-report.

## Machine variability

It is important to be aware that the tests are sensitive to machine variability. Different GPUs may produce subtly different pixel outputs
(that look identical to a human user). The tests are run on a specific machine and the reference images are generated on that machine. If the tests are run on a different machine, the images may not match the reference images and this is "fine". If you run these on your local machine compare the differences by eye in the report, don't wory about failing tests.

### Renderer noise is tolerated

The CI renders with software renderers (Mesa in the desktop job, the emulator's GLES renderer in
the Android job) whose rounding depends on the machine hosting the runner. The same commit can
therefore produce an image whose pixels are a little different from the reference image the tests
were baked against. Requiring every pixel to match turns that noise into a red pipeline, and
retrying the test on the same runner reproduces it exactly.

A screenshot is therefore considered to match its reference image when no more than `0.02%` of its
pixels (and never fewer than 10 pixels, so that small images still get a usable budget) differ by
more than 3/255 on any colour channel - about 40 pixels on a 500x400 desktop screenshot and about
200 pixels on a 1280x800 emulator screenshot. A change to what is actually drawn moves far more
pixels than that and still fails the test; the numbers live in `ImageDifference` if they ever need
tightening.

Failures also now report the measurement that caused them, e.g.

```
Generated images is different from committed image. (900 of 200000 pixels differ by more than 3 (at most 40 tolerated), largest single channel difference 255)
```

so a genuine change of the drawn scene (hundreds or thousands of pixels) can be told apart from a
rendering hiccup (a handful of pixels) without downloading the artefacts.

## Parameterised tests

By default, the tests use the class and method name to produce the screenshot image name. E.g. org.jmonkeyengine.screenshottests.effects.TestExplosionEffect.testExplosionEffect_f15.png is the testExplosionEffect test at frame 15. If you are using parameterised tests this won't work (as all the tests have the same function name). In this case you should specify the image name (including whatever parameterised information to make it unique). E.g.

```
    screenshotTest(
        ....
    ).setFramesToTakeScreenshotsOn(45)
    .setBaseImageFileName("some_unique_name_" + theParameterGivenToTest)
    .run();
)
```

## Non-deterministic (and known bad) tests

By default, screenshot variability will cause the pipeline to fail. If a test is non-deterministic (e.g. includes randomness) or 
is a known accepted failure (that will be fixed "at some point" but not now) that can be non-desirable. In that case you can 
change the behaviour of the test such that these are marked as warnings in the generated report but don't fail the test

```
    screenshotTest(
        ....
    ).setFramesToTakeScreenshotsOn(45)
    .setTestType(TestType.NON_DETERMINISTIC)
    .run();
)
```

## Accepting new images

It may be the case that a change makes an improvement to the library (or the test is entirely new) and the new image should be accepted as the new reference image. To do this, copy the new image to the `src/test/resources` directory. The new image can be found in the `build/changed-images` directory, however it is very important that the image come from the reference machine. This can be obtained from the CI server. The job runs only if there is an active pull request (to one of the mainline branches; e.g. master or 3.7). If you go into the Actions tab and find your pipeline you can download the report and changed images from the Artifacts section.
