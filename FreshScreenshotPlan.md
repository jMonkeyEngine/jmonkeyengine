# Fresh Reference Screenshot Plan

Replace the committed reference screenshots with reference images that are generated **fresh on the
same runner, in the same job, from a reference git ref**, so that GPU/driver/CPU variability between
machines can never fail a test.

Status: **Phase 1 implemented** (see 9), and the committed reference images are deleted, so the
screenshot CI jobs fail until Phase 3 gives them a mode. Phases 2-5 are still plan only.

---

## 1. How it works today

| Concern | Where |
| --- | --- |
| Test entry point / comparison logic | `jme3-screenshot-tests/jme3-screenshot-tests-shared/src/main/java/org/jmonkeyengine/screenshottests/testframework/ScreenshotTest.java` |
| Reference image lookup | `ScreenshotTest.java:293` - `ScreenshotTest.class.getClassLoader().getResources(name + ".png")` |
| "New test" handling | `ScreenshotTest.java:295-307` - missing reference **fails** the test |
| Noise tolerance | `.../testframework/ImageDifference.java` (0.02% of pixels, min 10, 3/255 per channel) |
| Platform hooks | `.../testframework/AppRunner.java` + `DesktopRunner` / `AndroidRunner` |
| Desktop reference images | `jme3-screenshot-tests/jme3-screenshot-tests-desktop/src/main/resources/*.png` (76 files, ~14 MB) |
| Android reference images | `jme3-screenshot-tests/jme3-screenshot-tests-android/src/main/resources/*.png` (16 files, ~2.4 MB) |
| Desktop gradle tasks | `jme3-screenshot-tests-desktop/build.gradle` - `screenshotTest`, `screenshotTestAngle` (`-Djme.screenshot.renderer=ANGLE_GLES3`) |
| Desktop CI | `.github/workflows/main.yml` job `ScreenshotTests` (matrix `opengl`/`angle`, container `ghcr.io/onemillionworlds/opengl-docker-image:v1`) |
| Android CI | `.github/workflows/android-screenshot.yml` (emulator-runner, inline `android-test-script.sh`) |

Reference images are loaded from the **classpath**, so on Android they are baked into the APK and on
desktop they come off `src/main/resources`. ANGLE runs use the same names with an `_angle` suffix
(`ScreenshotTest.ANGLE_REFERENCE_IMAGE_SUFFIX`).

### Why this hurts

* Reference images are machine-specific artefacts stored in git. Any change of GitHub runner CPU
  (Mesa/llvmpipe rasterises differently on different instruction sets) or emulator host shifts pixels.
* The workaround is `ImageDifference`'s tolerance, which *also* blinds the tests to small genuine
  regressions, and had to be widened once already (#2965).
* Accepting a change means downloading an artefact, copying PNGs into `src/main/resources` and
  committing binaries - 16 MB of binaries already live in the repo and every accepted change adds a
  new blob to history forever.
* Running the tests locally is close to useless: the README tells developers to ignore failures.

---

## 2. Target design

Two passes **inside one job, on one runner**:

```
 job (one runner, one container, one emulator)
 |- checkout reference_ref  -> ./reference
 |- checkout target_ref     -> ./target
 |- pass 1: (cd reference && gradlew screenshotTest -Pscreenshot.mode=generate
 |                                                  -Pscreenshot.referenceDir=$RUNNER_TEMP/refs)
 |            -> writes every screenshot to $RUNNER_TEMP/refs, asserts nothing image-vs-reference,
 |               always passes (unless the app itself crashes)
 `- pass 2: (cd target    && gradlew screenshotTest -Pscreenshot.mode=compare
                                                    -Pscreenshot.referenceDir=$RUNNER_TEMP/refs)
              -> normal comparison against the freshly generated references
```

Properties this gives us:

* Both images come from the same GPU/driver/CPU, so **any** difference is caused by the code diff.
* A missing reference image means the test does not exist on the reference ref -> **auto-accept**.
* Nothing machine-specific is committed. `src/main/resources/*.png` is deleted (~16 MB).
* Locally, `-Pscreenshot.mode=generate` on `master` then `compare` on your branch works identically -
  local screenshot testing becomes genuinely useful for the first time.
* The comparison is a **branch-pair** operation, so the same machinery serves PRs, pushes and manual
  "compare v3.7 to master" runs.

### Tolerance

Same-runner, same-code rendering should be bit-identical. Do **not** tighten `ImageDifference` in the
same change - land the new pipeline first, watch a handful of reference-vs-reference runs to measure
the real noise floor, then tighten (see Phase 5). Keeping the current tolerance initially means the
new pipeline cannot be blamed for new flakiness.

---

## 3. Java / Gradle changes

### 3.1 New mode + reference directory (shared module)

Add a small value class, e.g. `ScreenshotTestMode` (`GENERATE` / `COMPARE`) and resolve configuration
next to the existing `RENDERER_SYSTEM_PROPERTY` in `ScreenshotTest`:

Both platforms take the same `-Pscreenshot.mode=` flag; the Android module forwards it to the
instrumentation runner so that nobody has to remember the
`-Pandroid.testInstrumentationRunnerArguments.jmeScreenshotMode=` spelling.

| Setting | Desktop (system property) | Android (instrumentation arg) | Default |
| --- | --- | --- | --- |
| mode | `jme.screenshot.mode` | `jmeScreenshotMode` | **none - a run must choose** |
| reference dir | `jme.screenshot.referenceDir` | n/a (test storage path, see 3.4) | `build/reference-images` |

**Two modes, and no default.** An interim `LEGACY_CLASSPATH` mode was built so that the framework
change could merge while CI still read the committed images, then removed again: keeping CI green
through the transition was not worth carrying a second reference-image model through the codebase,
and the CI rewrite is the very next step. The classpath reading, the committed PNGs and the "expected
image not found" failure all went with it.

Neither remaining mode can be the default, because both defaults fail silently and green -
defaulting to `COMPARE` means a run meant to generate produces no reference images at all and the
comparison then accepts every test as new; defaulting to `GENERATE` means a run meant to compare
asserts nothing. So a run that names no mode fails in about three seconds, before booting a renderer,
with the message that explains both modes and the local generate-then-compare workflow. That failure
is how most people will learn how these tests work, so it teaches rather than scolds. An unrecognised
value gets the same treatment rather than a guess.

**Consequence: the screenshot jobs are red until Phase 3 lands**, because they pass no mode. That is
deliberate and was chosen over carrying the legacy path.

Reading these differs per platform, so add to `AppRunner`:

```java
ScreenshotTestMode getMode();
/** @return the reference image if the reference run produced one */
Optional<Image> loadReferenceImage(String fileName);
void saveReferenceImage(Image image, String fileName);
```

`DesktopRunner` implements these against
`Paths.get(System.getProperty("jme.screenshot.referenceDir", "build/reference-images"))`;
`AndroidRunner` against `TestStorage` (3.4). This keeps `ScreenshotTest` platform-agnostic, matching
the existing `saveGeneratedImageToChangedImages` pattern.

### 3.2 `ScreenshotTest.bootAppForTest` changes (`ScreenshotTest.java:217-360`)

* **Generate mode**: after the scenarios run, write each frame's screenshot with
  `saveReferenceImage(...)` and skip the reference comparison entirely.
  *Keep* the scenario-vs-scenario comparison (`:238-278`) - that is a same-run assertion, is still
  meaningful, and catches a genuinely broken reference run early.
  `TestType.KNOWN_TO_FAIL` / `NON_DETERMINISTIC` are irrelevant in this mode.
* **Compare mode**: replace the classpath lookup at `:293` with `osSpecificRunner.loadReferenceImage(...)`.
* **Missing reference** (`:295-307`): no longer a failure. Write the image to `changed-images`, attach
  it to the report as "New test - no reference on `<reference_ref>`", log a **warning**, and pass.
* Every other branch (size mismatch, `ImageDifference` over budget, test types) is unchanged.

### 3.3 A failed reference run must abort the job before the branch under test runs

This is a hard requirement, not a nicety. If the reference pass dies or fails tests and the compare
pass runs anyway, every missing reference image looks like a new test and is auto-accepted - a broken
`master` would silently green-light the entire suite.

**Rule: if the reference pass does not complete cleanly, fail the job immediately and never start
pass 2.** "Cleanly" means every generate-mode test passed: no app crash, no 45 s `TestDriver` timeout,
no scenario-vs-scenario mismatch. In generate mode there is nothing else that can fail, so the gradle
task's own exit code is already an accurate signal - the workflow simply does not proceed past it
(4.1), and the failure is reported as "reference generation failed on `<reference_ref>`", never as an
image difference.

Two refinements on top of the exit code:

* **Completion marker.** The exit code does not distinguish "task failed" from "runner was killed".
  Have the generate task write a `reference-run-complete` marker in a `doLast`, and have the workflow
  assert it exists. Cheap, and it makes the failure mode unambiguous.
* **Manifest (recommended).** The marker says the run finished, but not *which* tests it covered.
  Write `reference-manifest.json` into the reference directory:
  `{ "referenceRef": "...", "renderer": "...", "tests": { "<image name>": "produced" | "errored" } }`.
  Compare mode then treats a missing image as a new test only if the manifest exists and does not
  list it; if the manifest lists it as `errored`, that single test fails with "reference generation
  failed" rather than being accepted. This matters for the partial case - a suite where one test
  errored but the run as a whole was allowed through (e.g. a future decision to tolerate a
  `KNOWN_TO_FAIL` reference test).

Write the manifest from a JUnit `LauncherSessionListener` / the existing report extension, or simply
append a line per image from `saveReferenceImage`.

Consequence worth accepting explicitly: **a red `master` blocks every PR's screenshot check.** That is
the correct behaviour (it is no different from any other red master), and the escape hatch is to fix
master or use `workflow_dispatch` with an older, known-good reference ref.

### 3.4 Android specifics

The Android job already runs everything inside one emulator session
(`android-screenshot.yml:46-72`), so both passes share one emulator.

* Reference images are written through `TestStorage.openOutputFile("reference-images/<name>.png")`
  in the generate pass - i.e. into `/storage/emulated/0/googletest/test_outputfiles/reference-images`.
* Between the passes the script copies them into the test-services **input** directory:
  `adb shell mkdir -p /storage/emulated/0/googletest/test_inputfiles` then
  `adb shell cp -r .../test_outputfiles/reference-images /storage/emulated/0/googletest/test_inputfiles/`
* The compare pass reads them with `TestStorage.openInputFile("reference-images/<name>.png")`.
* Also `adb pull` them to the host and upload as an artefact (useful when a diff needs eyeballing).
* Clear `test_outputfiles` between the passes so the reference run's report/changed-images do not
  pollute the comparison run's artefacts.
* Test-services storage belongs to the *test services app*, so it survives the
  `adb uninstall org.jmonkeyengine.screenshottests.android` in the retry loop - unlike the app's own
  external files dir, which does not. This is why test storage is the right home for the references.
* Mode is passed with `-Pandroid.testInstrumentationRunnerArguments.jmeScreenshotMode=generate`,
  read via `InstrumentationRegistry.getArguments()`.

### 3.5 Gradle

* `jme3-screenshot-tests-desktop/build.gradle`: map project properties onto system properties for both
  `screenshotTest` and `screenshotTestAngle` (factor the shared config into a helper rather than a
  third copy/paste):

```groovy
def screenshotMode = findProperty('screenshot.mode')
def screenshotReferenceDirectory = findProperty('screenshot.referenceDir')
        ?: layout.buildDirectory.dir('reference-images').get().asFile.absolutePath

def configureScreenshotTest = { Test task, String renderer ->
    task.testClassesDirs = sourceSets.test.output.classesDirs
    task.classpath = sourceSets.test.runtimeClasspath
    task.useJUnitPlatform { options -> options.includeTags 'integration' }
    if (renderer != null) task.systemProperty "jme.screenshot.renderer", renderer
    // Left unset when no mode was given, so the test framework is the one place that explains the
    // modes rather than the build script guessing one.
    if (screenshotMode != null) task.systemProperty "jme.screenshot.mode", screenshotMode
    task.systemProperty "jme.screenshot.referenceDir", screenshotReferenceDirectory
    task.outputs.upToDateWhen { false }   // two passes over the same inputs must both actually run
}
```

The Android module maps the same property onto an instrumentation argument in its `defaultConfig`,
again passing nothing on when no mode was given:

```groovy
def screenshotMode = findProperty('screenshot.mode')
if (screenshotMode != null) {
    testInstrumentationRunnerArguments jmeScreenshotMode: screenshotMode.toString()
}
```

One Groovy trap, hit while implementing this and worth knowing before Phase 3 touches the file again:
the original `useJUnitPlatform { filter { includeTags 'integration' } }` only works by accident.
`JUnitPlatformOptions` has no `filter`, so Groovy resolves it on the enclosing task closure
(`Test.filter`, a `TestFilter`), which in turn has no `includeTags`, so *that* resolves back out to
the `JUnitPlatformOptions` delegate. Move the same lines into a helper closure and the owner chain no
longer contains the task, so it fails with "No signature of method: JUnitPlatformOptions.filter()".
Calling `options.includeTags` directly is what was meant all along.

* `.gitignore`: nothing new needed if the default reference dir stays under `build/`.
* Delete `jme3-screenshot-tests-desktop/src/main/resources/*.png` and
  `jme3-screenshot-tests-android/src/main/resources/*.png` (92 files, ~16 MB).
  Note this only removes them from the working tree; history still carries the blobs. That is fine -
  do **not** rewrite history for this.

---

## 4. Workflow restructure

### 4.1 One reusable workflow

New `.github/workflows/screenshot-compare.yml`:

```yaml
name: Screenshot Comparison
on:
  workflow_call:
    inputs:
      reference_ref:  { type: string, required: true }
      target_ref:     { type: string, required: true }
      platforms:      { type: string, default: 'desktop,android' }
      accept_changes: { type: boolean, default: false }
  workflow_dispatch:
    inputs:
      reference_ref: { description: 'Ref to generate reference images from', default: 'master' }
      target_ref:    { description: 'Ref to test', required: true }
      platforms:     { description: 'desktop,android', default: 'desktop,android' }
```

Jobs: `desktop` (matrix `opengl`/`angle`, existing container + display-server setup moved across
verbatim) and `android` (existing emulator-runner setup). Each does two checkouts:

```yaml
- uses: actions/checkout@v7.0.1
  with: { ref: ${{ inputs.reference_ref }}, path: reference }
- uses: actions/checkout@v7.0.1
  with: { ref: ${{ inputs.target_ref }},    path: target }
```

The reference pass runs first, in its own step, and that step is **not** `continue-on-error`. A
following step asserts the completion marker (3.3) exists and that the manifest lists no `errored`
tests; if either check fails the job stops there with "reference generation failed on
`<reference_ref>`" and the compare pass never runs. The Android script needs the same guard between
its two `./gradlew connectedDebugAndroidTest` invocations - it currently swallows exit codes with
`|| true` in places, so the reference invocation must be exempt from that.

**Decided: Android stays its own workflow.** Its emulator setup (KVM enablement, AVD cache,
`reactivecircus/android-emulator-runner` wrapping a single script) has nothing in common with the
desktop container job, and folding it into `main.yml` would also change the required-check names in
branch protection. `android-screenshot.yml` therefore remains a thin wrapper that keeps its own
status check.

Sharing what genuinely *is* common, without pretending the two jobs are the same:

* **Ref resolution** (4.2) becomes a composite action, `.github/actions/resolve-screenshot-refs`, used
  by both workflows. This is the piece that must not drift between them.
* **The acceptance-label check** and the `$GITHUB_STEP_SUMMARY` writer (5) become composite actions
  too - identical text and identical accept/reject logic on both platforms is exactly what a
  contributor needs.
* **The generate/compare sequencing and the 3.3 abort** stay expressed per platform, because they are
  genuinely different shapes: the desktop job is two workflow steps, the Android job is two
  invocations inside one bash script handed to the emulator runner. Forcing one abstraction over both
  would be worse than the duplication.

`main.yml`'s `ScreenshotTests` job becomes a `uses: ./.github/workflows/screenshot-compare.yml` call;
`android-screenshot.yml` calls the same reusable workflow with `platforms: android`, or keeps its job
inline and just consumes the shared composite actions - whichever reads better once the composite
actions exist.

Manual runs ("does v3.7 render the same as master?") come free via `workflow_dispatch`.

### 4.2 Resolving the refs

A tiny `Resolve refs` job outputs the pair:

| Event | reference_ref | target_ref |
| --- | --- | --- |
| `pull_request` | first parent of `refs/pull/<n>/merge` (the base tip the merge ref was built from) | `refs/pull/<n>/merge` |
| `push` | `${{ github.event.before }}` | `${{ github.sha }}` |
| `workflow_dispatch` | input | input |

Using the merge ref's **first parent** rather than `master`'s current tip matters: the merge ref is
`base tip + PR changes`, so comparing against that base tip attributes only the PR's own changes to
the PR. `github.event.pull_request.base.sha` is a close approximation and simpler, but can drift;
prefer `git rev-parse "$(git rev-parse refs/pull/<n>/merge)^1"`.

Edge case: on `push`, `github.event.before` is `000...0` for a new branch and can point at a
force-pushed-away commit. Fall back to `${{ github.sha }}^` and, if that also fails, skip the
comparison and run generate-only as a smoke test.

### 4.3 Retries

The desktop job retries 3x and the Android script retries 3x. With fresh references:

* Retrying the **reference** pass on failure is correct (infrastructure flake).
* Retrying the **compare** pass is correct for infrastructure flake but pure waste (3x the runtime)
  for a genuine image difference, which is now the common failure.
* Improvement: only retry when the JUnit XML results are missing/incomplete (JVM died, X server
  corrupted), not when they exist and contain image-difference failures. Worth doing, but it can land
  after the main change.

### 4.4 Runtime cost

The screenshot jobs roughly double. Mitigations, in order of value:

1. **Gradle build cache.** The desktop job uses no `gradle/actions/setup-gradle` at all today. Adding
   it (with `org.gradle.caching=true`) means the *reference* checkout - which is almost always a
   recent `master` commit - compiles from cache in most PR runs, so the second pass costs little more
   than the test execution itself. Set a shared `GRADLE_USER_HOME` so both checkouts share it.
2. Run the two passes with one Gradle daemon; do not add `--no-daemon`.
3. Do **not** cache the generated reference *images* across runs keyed on the master SHA. It looks
   attractive but re-introduces exactly the bug being fixed: the cache is shared across runners with
   different CPUs, and Mesa's output depends on the host CPU. If it is ever done, the cache key must
   include the runner's CPU model and the container image digest - and even then it is fragile.

Measure the before/after wall clock on the first run and record it; if the Android job (60 min
timeout) gets close to the limit, raise the timeout rather than dropping coverage.

---

## 5. Accepting intentional visual changes

This is the real design decision. Five options were considered.

### Option A - Non-blocking: report only

The screenshot job never fails on an image difference; it posts the report/diffs and leaves it to the
reviewer.

* Pro: zero friction, no infrastructure, no false red.
* Con: nothing stops an unnoticed regression, and reviewers reliably stop reading a check that is
  never red. This throws away most of the value of having the tests.
* Verdict: **rejected** as the default (it remains the right behaviour for
  `TestType.NON_DETERMINISTIC`, which already warns rather than fails).

### Option B - Chat-ops comment (`/accept-screenshots`)

An `issue_comment`-triggered workflow re-runs the comparison in "accept" mode.

* Pro: no file churn, familiar from other projects, can be per-image (`/accept-screenshots TestFog*`).
* Con: the most machinery by far. `issue_comment` workflows run from the default branch with a token
  that cannot simply turn the PR's existing check green, so it needs a `workflow_dispatch` /
  `repository_dispatch` re-run plus a Checks API call plus a commenter-permission check plus careful
  handling of fork PRs. Acceptance is also not tied to a commit, so it silently survives a later push
  unless extra work is done.
* Verdict: **rejected** - all of Option D's downsides plus far more code.

### Option C - Checked-in acceptance file

The PR adds entries to e.g. `jme3-screenshot-tests/accepted-screenshot-changes.txt` listing the image
names whose change is intentional.

* Pro: acceptance is tied to the commit, visible in the diff, reviewable, per-image, works for fork
  PRs with no tokens or permissions, and needs no GitHub API code at all. Contributors can unblock
  themselves without waiting for a maintainer.
* Con: **stale entries**. After the PR merges, master-vs-master produces no difference, so the entries
  sit there permanently masking future changes to those very tests. Treating a stale entry as a
  failure breaks the first master push after the merge; treating it as a warning means it is ignored.
  Either way it needs a cleanup chore.
* Verdict: **strong second choice** - pick it over D if maintainer bandwidth for labelling is the
  binding constraint, and pair it with a "stale acceptance entry" warning in the job summary.

### Option D - Maintainer label (recommended)

A maintainer adds the label `accept-screenshot-changes` to the PR; the workflow reads the label and
downgrades image differences from failure to warning.

* Pro: acceptance state lives on the PR and dies with it, so there is nothing to clean up and nothing
  can leak into master. Adding `labeled`/`unlabeled` to the `pull_request` trigger types makes the
  workflow re-run and the check go green **without a push**. Works for fork PRs (the workflow only
  *reads* `github.event.pull_request.labels`, so no write token is needed). Roughly ten lines of YAML.
* Con: needs a maintainer (contributors cannot self-accept - arguably a feature); it is all-or-nothing
  for the PR rather than per-image; and it is not recorded in git history (the PR still records it).
* **Decided: the label blesses the PR for its lifetime.** An earlier draft proposed a companion
  `pull_request_target` job that stripped the label whenever new commits arrived, so that every push
  needed re-acceptance. That is dropped: once a maintainer has accepted the visual change, later
  pushes to the same PR stay accepted. The trade is that a subsequent push could introduce a further
  visual change that nobody re-reviews - accepted deliberately, because the reviewer is reading the
  diff at merge time anyway, and because it removes the only `pull_request_target` job from the
  design (see 6.3: less write-token surface is a real security benefit, not just less YAML).
* Verdict: **chosen.**

### Option E - Auto-accept anything new

Not an alternative but a rule inside whichever option is chosen: an image with no reference on the
reference ref is a new test and always passes with a warning. This is already in the plan (3.2/3.3).

### Decision

**Option D (label) as the primary mechanism, Option E always on, Option A's reporting layer on top.**

Concretely:

1. Image differences fail the check by default.
2. The job writes a markdown summary to `$GITHUB_STEP_SUMMARY` containing everything needed to act on
   the result without downloading anything first: a table of every changed image with its
   `ImageDifference.describe()` numbers, which tests were auto-accepted as new, a link to the
   `ScreenshotDiffReport.html` artefact, and **the acceptance instructions themselves** ("if these
   changes are intended, a maintainer can add the `accept-screenshot-changes` label and approve the
   run it triggers"). This needs no extra permissions.
3. A maintainer who has reviewed the report adds `accept-screenshot-changes`; the workflow re-runs on
   the `labeled` event and the check passes, logging every accepted image by name in the summary so
   the merge record shows what was accepted.
4. The label stays for the life of the PR.

**No PR comment.** It was considered and dropped: for fork PRs the comparison workflow has a
read-only token, so a comment requires the `workflow_run` pattern - a second workflow in base context
with `pull-requests: write` that downloads artefacts from an untrusted run, validates the PR number,
and posts a sticky comment. That is a meaningful amount of machinery plus a write-token attack
surface (6.3), to deliver information the job summary already carries. The summary plus the attached
report is the whole reporting story; if the summary turns out to be too easy to miss in practice,
revisit it then.

---

## 6. Gating: PRs from unknown contributors

### 6.1 The existing mechanism

There is **no workflow-level gate in this repository** - `grep` over `.github/workflows/*.yml` finds
no `pull_request_target` approval job, no collaborator check and no gating label (`bounty.yml` uses
`issues: [labeled]`, but only to post a comment on issues). The mechanism is a **repository setting**,
not code:

> Settings -> Actions -> General -> **Fork pull request workflows from outside collaborators**

with three choices: require approval for first-time contributors who are new to GitHub; require
approval for **first-time contributors** (GitHub's default for public repositories); or require
approval for **all outside collaborators**. When it applies, the PR shows *"N workflows awaiting
approval"* and a maintainer clicks **Approve and run**; until then nothing executes and the checks sit
pending, not failed.

**Action for a maintainer: confirm which of the three is set** (it cannot be read from the repo
contents, and the setting page needs admin rights). The rest of this section holds for any of them;
only the frequency of the approval prompt changes.

### 6.2 How it interacts with this plan

* **It is the real gate; the acceptance label is not.** Nothing in this plan weakens it. An unknown
  contributor's PR runs no screenshot job, generates no reference images and consumes no runner
  minutes until a maintainer approves. The `accept-screenshot-changes` label sits strictly
  *downstream*: approve the run -> read the report -> label if the visual change was intended.
* **Approval is per workflow run, so the label re-run needs approving too.** This is the one real
  wrinkle. Adding the label fires a fresh `pull_request: labeled` run, and for a gated contributor
  that run also lands in "awaiting approval". So the maintainer's sequence is: *label, then approve
  the run it triggers.* Worth a sentence in the README and in the job summary's failure text
  ("a maintainer can accept these changes by adding the `accept-screenshot-changes` label and
  approving the run it triggers"), because a maintainer who labels and walks away will leave the check
  pending forever. For repo collaborators and returning contributors the approval prompt does not
  appear and labelling alone is enough.
* **The doubled runtime is spent only after approval**, so the gate also caps the cost of this change
  for drive-by PRs. It does mean each approval now costs roughly twice the minutes it used to.
* **Fork refs are reachable from the base repo.** `refs/pull/<n>/merge` and its first parent live on
  the base repository, so the two-checkout design in 4.1 works for fork PRs with no extra remote, no
  token and no special permission.
* **`workflow_dispatch` is maintainer-only** (it requires write access), so the manual "compare any
  two refs" entry point cannot be used by an outsider to run unapproved code. A maintainer
  dispatching against a fork's PR ref is making the same trust decision as clicking "Approve and run" -
  worth saying so in the input description.

### 6.3 Security constraints this imposes on the new jobs

The approval gate exists because `pull_request` jobs execute untrusted code. Two pieces of this plan
deliberately run *outside* that gate and must therefore be written defensively:

* **The label-removal job (5, Option D) uses `pull_request_target`**, which runs in the base-repo
  context with a write token and **no approval requirement**. It must therefore *never* check out or
  execute PR code - no `actions/checkout`, no `./gradlew`, nothing but the two REST calls that drop
  the label. This is the classic "pwn request" foot-gun; a checkout step added to it later would hand
  any contributor a write token.
* **The optional `workflow_run` comment poster (5, phase 4)** likewise runs in base context with
  write permissions and consumes artefacts produced by untrusted code. Treat every byte of those
  artefacts as hostile data: validate the PR number as an integer before using it, never interpolate
  artefact content into a shell command, and post the body as plain text via the API. Markdown
  injection into the comment is the worst acceptable outcome.
* **Within the screenshot job itself, the PR controls both passes.** The reference pass runs
  `master`'s code but on a runner whose workspace the PR's build can touch, so a malicious PR could
  tamper with the generated reference images. This is not a new trust boundary - the PR already
  supplies the comparison code itself - and the only consequence is a falsely green screenshot check,
  which a reviewer reading the diff would catch. No mitigation proposed; noted so it is a decision
  rather than an oversight.

---

## 7. Platform coverage

### 7.1 What actually runs where today

| Job | Runner | Screenshot tests? |
| --- | --- | --- |
| `ScreenshotTests` (`main.yml:133`) | `ubuntu-latest` + `opengl-docker-image` container | **Yes** - `LWJGL_OPENGL45` and `ANGLE_GLES3` (matrix) |
| `screenshot-test-android` (`android-screenshot.yml`) | `ubuntu-latest` + API 35 emulator | **Yes** - emulator GLES |
| `TestIOSBuild` (`main.yml:287`) | `macos-latest` | No - `:jme3-ios-examples:buildIosSimulatorApp` only, it never launches anything |
| `BuildJMonkey` (`main.yml:311`) | ubuntu / windows / macOS | No - `./gradlew build` runs the `test` task, which `excludeTags 'integration'` |

So despite the three-OS build matrix, **every screenshot comparison happens on Linux**, across three
render backends. Windows and macOS build the engine but render nothing; iOS is compiled but not run.
The plan therefore touches three jobs, not the whole matrix - and `TestIOSBuild` / `BuildJMonkey` need
no changes at all.

### 7.2 The `_angle` suffix becomes cosmetic

`ANGLE_REFERENCE_IMAGE_SUFFIX` exists for exactly one reason: two backends' reference images share one
`src/main/resources` directory and would otherwise overwrite each other. Once each job generates its
references into its own directory, that collision is impossible - Android's names already differ
anyway (`org.jmonkeyengine.screenshottests.android.*`).

Recommendation: **keep the suffix, drop the explanation.** It costs nothing and still helps when a
reviewer downloads the `opengl` and `angle` artefacts into one folder, but it is no longer
load-bearing, so the README's "ANGLE reference images" section and the `*_angle.png` instruction in
`main.yml:275` should be deleted rather than updated. Removing the suffix entirely is also defensible;
it is a coin-flip, not a design question, now that nothing depends on it.

### 7.3 Both passes must stay in one job - per platform

The invariant that makes this whole design work is *same runner*, so:

* Each platform's generate and compare passes live in the **same job**. Never split them across jobs
  to parallelise - two jobs means two runners, which is the original bug.
* Different platforms get different jobs with wildly different setup (container vs emulator vs, in
  future, a simulator), so the reusable workflow should select them with
  `if: contains(inputs.platforms, 'android')` per job rather than forcing one matrix to cover all of
  them. The `opengl`/`angle` split stays a matrix inside the desktop job, since only one systemProperty
  differs.
* Keep `fail-fast: false` on that matrix. The 3.3 abort applies per job, so a reference run that dies
  on ANGLE must not cancel the OpenGL job's verdict.
* The runtime doubling in 4.4 is therefore "three jobs doubled", not "the pipeline doubled".

### 7.4 Cross-OS hygiene for the new unit tests

`BuildJMonkey` runs `./gradlew build` on Windows and macOS, and that **does** run the shared module's
unit tests (`ImageDifferenceTest`, `RendererSelectionTest`, ...). The Phase 1 tests for mode and
reference-directory resolution will run there too, so they must be OS-agnostic:

* `@TempDir`, never a hardcoded absolute path or `/tmp`.
* Build paths with `Path`/`resolve`, never string concatenation with `/`.
* Do not rely on case-sensitive file lookup. Reference file names are derived from fully-qualified
  class names, so a name that differs from another only by case would resolve differently on
  Linux than on macOS/Windows. Nothing hits this today, but a lookup written to depend on it would
  pass CI on Linux and fail on a contributor's Mac.

Note also that `AppRunner`'s new methods (3.1) are deliberately `Image`/stream-shaped rather than
`Path`-shaped - Android has no filesystem path for its references, it has `TestStorage` streams.
Keep it that way; it is what lets a future platform plug in without touching `ScreenshotTest`.

### 7.5 What this unlocks (and what it does not)

The committed-reference model is *why* screenshot coverage never spread beyond Linux: each new OS
meant another set of machine-specific PNGs in git that would break the moment GitHub changed the
runner. Fresh references delete that blocker - any platform that can run the tests self-references.
What remains for each platform is a genuine rendering problem, unrelated to this plan:

* **Local development, any OS - the immediate win.** Generate on `master`, compare on your branch, on
  your own GPU, and the result is meaningful. The README currently tells developers to ignore local
  failures; the rewrite should say the opposite, and this is worth advertising in the PR description
  because it changes the day-to-day experience more than the CI change does.
* **Windows CI** - feasible. GitHub's Windows runners have no GPU, so it needs a software GL (a Mesa
  llvmpipe `opengl32.dll` alongside the app) or ANGLE. Work, but no reference-image problem.
* **macOS CI** - `LWJGL_OPENGL45` cannot work: Apple caps OpenGL at 4.1 core and has deprecated it.
  It would need `AppSettings.LWJGL_OPENGL41` (`AppSettings.java:206`) and possibly shader changes,
  with ANGLE-on-Metal the more future-proof route.
* **iOS - out of scope, and not merely "another `AppRunner`".** The iOS launcher comes from the
  third-party `org.ngengine.libjglios` plugin and runs jME as a **GraalVM native-image** binary, not on
  a JVM. There is no JUnit or instrumentation host on the device to drive scenarios and assert on
  them, so the tests would have to be compiled into the native-image app itself and their output
  exfiltrated from the simulator's app container (`xcrun simctl get_app_container <device> <id> data`
  is a plain host directory, so the exfiltration end is the easy half). That is a project of its own.

None of these are part of this change. The point is only that after it, adding one is a rendering
question rather than a "who owns the golden images" question.

---

## 8. Risks and open questions

| Risk | Mitigation |
| --- | --- |
| A broken reference run silently passes everything | Hard requirement in 3.3: exit code + completion marker + manifest, checked before pass 2 starts, so the compare pass never runs against an incomplete reference set. |
| A maintainer adds the acceptance label and the check stays pending | 6.2: the label's re-run needs approving too for gated contributors; the job summary's acceptance instructions say so explicitly. |
| A blessed PR receives a further, unreviewed visual change in a later push | Accepted deliberately (5, Option D): the label blesses the PR for its lifetime. The reviewer reads the diff at merge time, and the alternative cost a `pull_request_target` job holding a write token. |
| Genuine per-run non-determinism (timing-dependent particles, thread races) shows up now that machine noise is gone | Keep the current `ImageDifference` tolerance initially; `TestType.NON_DETERMINISTIC` already exists for the worst offenders. |
| Job runtime doubles | Gradle build cache (4.4); measure and raise the Android timeout if needed. |
| Reference ref itself is red (master broken) | The comparison pass still runs; the summary says "reference run failed N tests", and the job fails with that reason rather than a confusing image diff. |
| Branch-protection check names change when workflows are restructured | `android-screenshot.yml` stays a separate workflow (4.1), so its check name survives; only the desktop job's naming needs review at merge time. |
| Shared logic drifts between the desktop and Android workflows | 4.1: ref resolution, the label check and the summary writer are composite actions used by both; only the platform-specific sequencing is duplicated. |
| No write permissions anywhere in the design | By construction: the job summary and label *read* need none, and dropping both the PR comment and the label-removal job means nothing in this plan holds a write token. |
| Two checkouts double disk usage in the container | Both are shallow where the ref allows; the repo plus build output fits comfortably on a GitHub runner, but verify on the first run. |

Decisions taken:

1. ~~Separate Android workflow or folded into `main.yml`?~~ **Separate**, because of the emulator
   setup; shared logic moves into composite actions (4.1).
2. ~~Self-accept (Option C) or maintainer acceptance (Option D)?~~ **Option D**, and the label blesses
   the PR for its lifetime - no removal on push.
3. ~~`workflow_run` PR comment?~~ **No.** Job summary + attached report only, with the acceptance
   instructions in the summary.

Still open:

4. Label name - `accept-screenshot-changes` proposed.
5. Which "Fork pull request workflows from outside collaborators" setting is actually in force
   (6.1)? It cannot be read from the repo and changes how often maintainers see the approval prompt.

---

## 9. Phased implementation

**Phase 1 - framework - DONE**
- [x] `ScreenshotTestMode` (`GENERATE` / `COMPARE`; no default, and unknown values rejected, both
      with the message that explains the model), mode and reference-directory resolution,
      `AppRunner.getScreenshotTestMode()` / `loadReferenceImage()` / `saveReferenceImage()`.
- [x] The committed reference images are gone: 92 PNGs (~16 MB) deleted along with the classpath
      reading, so `src/main/resources` no longer exists in either module.
- [x] Workflows deliberately left untouched, so the screenshot jobs fail until Phase 3.
- [x] `jme3-screenshot-tests/README.md` gains a "Choosing a mode" section, since the failure message
      sends people there.
- [x] `ScreenshotTest`: generate mode records and asserts nothing; compare mode reads through
      `AppRunner`; a missing reference warns and passes instead of failing.
- [x] `DesktopRunner` (a directory) + `AndroidRunner` (test storage, input files for reading,
      output files for writing).
- [x] `ScreenshotImageIo` so both platforms and both kinds of image decode through one path - a
      comparison must measure the renderer, not the decoder. Android reference images arrive as a
      stream rather than a file, so they are spooled to a temp file and decoded identically.
- [x] Gradle wiring for `screenshotTest` / `screenshotTestAngle`, including `upToDateWhen { false }`
      so the second of two passes over identical inputs is not skipped as up to date.
- [x] 12 new unit tests (6 in shared, 6 in desktop), OS-agnostic per 7.4: `@TempDir`, `Path`
      comparison rather than string paths.
- [x] Verified locally on Windows with a real GPU:
      generate then compare -> passes (the committed Linux reference is irrelevant, which is the
      whole point); compare with no reference generated -> passes as a new test; compare against a
      deliberately swapped-in different image -> fails with
      `200000 of 200000 pixels differ by more than 3 (at most 40 tolerated)`; no mode -> fails in 3s
      with the explanation, without booting a renderer; `-Pscreenshot.mode=genrate` and the
      now-removed `legacy` -> same.
- [x] Verified the Android flag actually reaches the runner:
      `-Pscreenshot.mode=generate` yields `[jmeScreenshotMode:generate]`, no flag yields `[:]`.
- [x] Android module compiles and dexes; no `Optional.isEmpty()`, `Path.of` or `List.of` in
      Android-reachable code (all are Java 11+ APIs that Android only gained at API 30-33, and this
      app has `minSdk 28` with no core library desugaring).
- [x] `checkstyleMain` / `checkstyleTest` clean on both modules.

**Phase 2 - reference completeness guard**
- [ ] Reference-run completion marker + manifest, and the "reference generation failed" failure path.
- [ ] Workflow guard that aborts before pass 2 (desktop step ordering, and the Android script's
      `|| true` exemption for the reference invocation).

**Phase 3 - CI**
- [ ] `.github/workflows/screenshot-compare.yml` (reusable + `workflow_dispatch`), desktop jobs.
- [ ] `.github/actions/resolve-screenshot-refs` composite action, used by both workflows.
- [ ] Composite actions for the acceptance-label check and the `$GITHUB_STEP_SUMMARY` writer.
- [ ] `main.yml` calls the reusable workflow; `android-screenshot.yml` stays separate and consumes the
      shared composite actions.
- [ ] Gradle build cache in the screenshot jobs.
- [ ] Job summary table, including the acceptance instructions.
- [ ] `accept-screenshot-changes` label handling (read-only; no label removal, no
      `pull_request_target` job anywhere).
- [ ] Confirm the fork-PR approval setting (6.1) and document the label-then-approve sequence in the
      README and in the job summary's failure text.
- [ ] Give both workflows the generate/compare pair. **This is what un-breaks the screenshot jobs**,
      which fail on "no mode selected" from Phase 1 onwards.
- [ ] Rewrite the failure hint in `main.yml`'s `Report Screenshot Test failure` step - it still tells
      people to "commit replacements to jme3-screenshot-tests/src/test/resources", a directory that
      no longer exists and a workflow that no longer applies.

**Phase 4 - optional polish**
- [ ] Retry only on infrastructure failure, not on image differences.

**Phase 5 - tighten tolerance**
- [ ] After ~10 green runs, measure the real same-runner noise floor from the reported
      `ImageDifference.describe()` numbers and reduce `ALLOWED_DIFFERENT_PIXEL_RATIO` /
      `PIXEL_TOLERANCE` towards exact match. Do this as its own PR so a regression in sensitivity is
      easy to revert.
