# JMonkeyEngine Android Screenshot Tests

This project contains screenshot tests specifically for Android.

## Prerequisites

To run these tests and collect the results, you need to install the Android Test Services APK on your device/emulator. This service allows the tests to write files (like screenshots and reports) to a persistent location that can be accessed via ADB.

1. Download the Test Services APK: [test-services-1.6.0.apk](https://maven.google.com/androidx/test/services/test-services/1.6.0/test-services-1.6.0.apk)
2. Install it with necessary permissions:
   ```bash
   adb install -r -g test-services-1.6.0.apk
   ```

## Collecting Test Results

The tests are configured to use `androidx.test.services.storage.TestStorage` to save output files. After running the tests, you can pull the entire report folder (which includes the captured images and the proto-report JSON) using the following command:

```bash
adb pull /storage/emulated/0/googletest/test_outputfiles/report/
```

Individual files can also be pulled if needed:
```bash
adb pull /storage/emulated/0/googletest/test_outputfiles/report/screenshotProtoReport.json
```

## Generating Extent Reports

The raw report is saved in a "proto-report" format (`screenshotProtoReport.json`). To convert this into a human-readable Extent Report (HTML), use the `upgradeProtoReport` Gradle task located in the `jme3-screenshot-tests-proto-report` module.

### Usage

Run the task and provide the input directory (where the pulled report is) and the desired output directory:

```bash
./gradlew :jme3-screenshot-tests:jme3-screenshot-tests-proto-report:upgradeProtoReport --args="path/to/pulled/report path/to/output/extent-report"
```

## How it Works

1. **Capture**: When tests run on Android, `ExtentReportExtensionJunit4` captures test status, logs, and screenshots.
2. **Storage**: Screenshots are saved as PNG files and test metadata is collected into a `ProtoReport` object.
3. **Persistence**: At the end of each test, the report metadata is serialized to `screenshotProtoReport.json` and saved to the device's persistent storage via `TestStorage`.
4. **Post-Processing**: Once pulled from the device, the `UpgradeProtoReportToExtentReport` tool processes the JSON and images to create a standalone HTML report with embedded screenshots.