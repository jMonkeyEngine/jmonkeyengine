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
| `-Pscreenshot.mode=generate` | Renders and records what every test looks like **on this machine**, into `build/reference-images`. Compares nothing against reference images, so a change in how a scene looks cannot fail it. A crash, a timeout, or the scenarios of a multi-scenario test rendering differently from each other still do. |
| `-Pscreenshot.mode=compare` | Renders and compares against the images an earlier `generate` run recorded. A test with no reference image did not exist on the branch that was generated from, so it is a new test and is reported as one. Images that differ are recorded in the report and in `build/changed-images`; they do not fail the run. |

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

The Android tests take the same flags and pass them through to the instrumentation runner, but the
images live on the device, so a comparison needs the steps CI does between its two passes. With an
emulator running and the test services app installed (`android-screenshot.yml` shows where to get
it, install it with `adb install -r -g`):

```
 TASK=:jme3-screenshot-tests:jme3-screenshot-tests-android:connectedDebugAndroidTest
 STORAGE=/storage/emulated/0/googletest
 REFERENCE=$STORAGE/test_runfiles/reference-images

 adb shell rm -rf $REFERENCE $STORAGE/test_outputfiles
 git switch master
 ./gradlew $TASK -Pscreenshot.mode=generate

 adb shell mkdir -p $REFERENCE
 adb shell "cp $STORAGE/test_outputfiles/reference-images/*.png $REFERENCE/"
 adb shell "echo done > $REFERENCE/reference-run-complete.txt"
 adb shell rm -rf $STORAGE/test_outputfiles

 git switch my-branch
 ./gradlew $TASK -Pscreenshot.mode=compare
```

Any images that differ can then be fetched with `adb pull $STORAGE/test_outputfiles/changed-images`.

ANGLE records into files suffixed with `_angle` (e.g. `..._f1_angle.png`) so that the two desktop
backends never overwrite each other's reference images.

## The report

A run produces `jme3-screenshot-tests/build/reports/ScreenshotDiffReport.html` showing the
differences between the reference images and the screenshots taken during the run. It is an
ExtentReport. On CI it can be downloaded from the Artifacts section of the pipeline once it has
finished; it is called `screenshot-test-report`.

Images that differ from their reference are also written to `build/changed-images`, except those
from non-deterministic and known bad tests (see below), whose differences only appear in the report.

## Changed images

A change that alters how something looks is expected to change these images, so a changed image
does not fail the check. Instead the changed (and new) images are listed in a comment on the pull
request, which is updated on every push, and the reviewer confirms the change was intended. Because
references are regenerated every run there is nothing to copy and commit.

The check does still fail when the comparison could not run to the end: a crash, a timeout, or a
reference run that did not finish. It also fails when a test's scenarios render differently from
each other. Scenarios are different ways of producing the same picture, so that is a serious visual
failure rather than a change to review, and the comment reports it as one.

The screenshot checks only run on pull requests, since a push has no reviewer to look at the result.

A test that is new on your branch has no reference to compare against, so it is listed as new;
nobody has to do anything for it.

## Renderer noise is tolerated

CI renders with software renderers (Mesa in the desktop job, the emulator's GLES renderer in the
Android job) whose rounding is not perfectly reproducible, so a small number of pixels can differ
between two runs of the very same code.

A screenshot is therefore considered to match its reference image when no more than `0.02%` of its
pixels (and never fewer than 10 pixels, so that small images still get a usable budget) differ by
more than 3/255 on any colour channel - about 40 pixels on a 500x400 desktop screenshot and about
200 pixels on a 1280x800 emulator screenshot. A change to what is actually drawn moves far more
pixels than that and is reported as a change; the numbers live in `ImageDifference`.

A reported difference includes the measurement behind it, e.g.

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

By default, a changed screenshot is listed on the pull request for the reviewer to confirm. If a test is non-deterministic (e.g. includes randomness) or 
is a known accepted failure (that will be fixed "at some point" but not now) that is just noise. In that case you can 
change the behaviour of the test such that these are marked as warnings in the generated report but never listed as changes

```
    screenshotTest(
        ....
    ).setFramesToTakeScreenshotsOn(45)
    .setTestType(TestType.NON_DETERMINISTIC)
    .run();
)
```
