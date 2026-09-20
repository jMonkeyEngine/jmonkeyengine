# jme3-screenshot-tests

This module contains tests that compare screenshots of the JME3 test applications to reference
images. Think of these like visual unit tests.

Reference images are **not** committed. They are generated fresh, on the machine running the tests,
from whichever branch you are comparing against. A reference image is really a recording of how one
particular machine rasterises a scene, so a committed one makes the tests sensitive to the hardware
that happens to run them; generating both halves of the comparison on one machine removes that
variable entirely, and means the difference you are shown was caused by the code.

## Choosing a mode

Every run has to say which half of the comparison it is. There is no default: a run that picks no
mode fails immediately, with an explanation, rather than rendering anything.

| Mode | What it does |
| --- | --- |
| `-Pscreenshot.mode=generate` | Renders and records what every test looks like **on this machine**, into `build/reference-images`. Asserts nothing about the images, so it cannot fail on how a scene looks. |
| `-Pscreenshot.mode=compare` | Renders and compares against the images an earlier `generate` run recorded. A test with no reference image did not exist on the branch that was generated from, so it is a new test and is accepted with a warning. |

`-Pscreenshot.referenceDir=<path>` overrides where reference images are written and read.

To check a visual change, generate on the branch you are comparing against and then compare on
yours - both on the same machine, so the hardware cancels out:

```
 git switch master
 ./gradlew :jme3-screenshot-tests:jme3-screenshot-tests-desktop:screenshotTest -Pscreenshot.mode=generate
 git switch my-branch
 ./gradlew :jme3-screenshot-tests:jme3-screenshot-tests-desktop:screenshotTest -Pscreenshot.mode=compare
```

Unlike the old committed-reference setup, this works on your own machine and its result means
something there - you no longer have to push to CI to find out whether you changed how something
looks.

The desktop commands above use the OpenGL 4.5 renderer. To use the ANGLE GLES3 renderer instead, run
`screenshotTestAngle` in place of `screenshotTest` for both passes. Note that ANGLE needs a Wayland
compositor (its EGL binaries have no X11 platform support). On a headless machine start one first,
e.g. `weston --backend=headless-backend.so --socket=wayland-1`, with `XDG_RUNTIME_DIR` set, and
`XDG_SESSION_TYPE=wayland` and `WAYLAND_DISPLAY=wayland-1` exported.

The same flag works for the Android tests, which pass it through to the instrumentation runner:

```
 ./gradlew :jme3-screenshot-tests:jme3-screenshot-tests-android:connectedDebugAndroidTest -Pscreenshot.mode=generate
```

ANGLE records into files suffixed with `_angle` (e.g. `..._f1_angle.png`) so that the two desktop
backends never overwrite each other's reference images.

## The report

A run produces `jme3-screenshot-tests/build/reports/ScreenshotDiffReport.html` showing the
differences between the reference images and the screenshots taken during the run. It is an
ExtentReport. On CI it can be downloaded from the Artifacts section of the pipeline once it has
finished; it is called `screenshot-test-report`.

Images that differ from their reference are also written to `build/changed-images`.

## Accepting a changed image

A change that alters how something looks is expected to change these images. Because references are
regenerated every run, there is nothing to copy and commit: review the difference in the report, and
if it is what you intended, say so on the pull request. A test that is new on your branch has no
reference to compare against and is accepted automatically, with a warning in the report.

## Renderer noise is tolerated

CI renders with software renderers (Mesa in the desktop job, the emulator's GLES renderer in the
Android job) whose rounding is not perfectly reproducible, so a small number of pixels can differ
between two runs of the very same code.

A screenshot is therefore considered to match its reference image when no more than `0.02%` of its
pixels (and never fewer than 10 pixels, so that small images still get a usable budget) differ by
more than 3/255 on any colour channel - about 40 pixels on a 500x400 desktop screenshot and about
200 pixels on a 1280x800 emulator screenshot. A change to what is actually drawn moves far more
pixels than that and still fails the test; the numbers live in `ImageDifference`.

Failures report the measurement that caused them, e.g.

```
Generated image is different from the reference image. ... (900 of 200000 pixels differ by more than 3 (at most 40 tolerated), largest single channel difference 255)
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
