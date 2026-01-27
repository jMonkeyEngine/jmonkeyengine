package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.state.AppState;

public class Scenario {
    String scenarioName;
    AppState[] states;

    public Scenario(String scenarioName, AppState... states) {
        this.scenarioName = scenarioName;
        this.states = states;
    }
}
