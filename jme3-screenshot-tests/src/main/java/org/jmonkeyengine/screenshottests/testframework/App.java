package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.math.ColorRGBA;

public class App extends SimpleApplication {

    public App(AppState... initialStates){
        super(initialStates);
    }

    @Override
    public void simpleInitApp(){
        getViewPort().setBackgroundColor(ColorRGBA.Black);
        setTimer(new VideoRecorderAppState.IsoTimer(60));
    }

}