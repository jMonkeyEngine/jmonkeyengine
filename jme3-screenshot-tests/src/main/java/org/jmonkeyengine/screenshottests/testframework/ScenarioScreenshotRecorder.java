package org.jmonkeyengine.screenshottests.testframework;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ScenarioScreenshotRecorder {

    /**
     * scenario name -> frame number -> screenshot path
     */
    Map<String, Map<Integer, Path>> screenshotsAtFrames = new HashMap<>();

    public void recordScreenshot(String scenarioName, int frameNumber, Path screenshotPath){
        screenshotsAtFrames.computeIfAbsent(scenarioName, k -> new HashMap<>()).put(frameNumber, screenshotPath);
    }

    public Optional<Path> getScreenshotsAtFrame(String scenarioName, int frameNumber){
        if(!screenshotsAtFrames.containsKey(scenarioName) || !screenshotsAtFrames.get(scenarioName).containsKey(frameNumber)){
            return Optional.empty();
        }else{
            return Optional.of(screenshotsAtFrames.get(scenarioName).get(frameNumber));
        }
    }

    public void addAll(ScenarioScreenshotRecorder other) {
        for (Map.Entry<String, Map<Integer, Path>> scenarioEntry : other.screenshotsAtFrames.entrySet()) {
            String scenarioName = scenarioEntry.getKey();
            for (Map.Entry<Integer, Path> frameEntry : scenarioEntry.getValue().entrySet()) {
                recordScreenshot(scenarioName, frameEntry.getKey(), frameEntry.getValue());
            }
        }
    }
}
