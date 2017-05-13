package jme3test.scene;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.debug.WireframeDebugAppState;

/**
 * @author Riccardo Balbo
 */
public class TestWireframeDebugAppState extends TestTanBnNDebugAppState{

	public static void main(String[] args) {
		TestWireframeDebugAppState app=new TestWireframeDebugAppState();
		app.start();
	}

	@Override
	public void applyAppState() {
		WireframeDebugAppState appstate=new WireframeDebugAppState(this);
		appstate.setColor(ColorRGBA.Yellow); // Default color is blue
		stateManager.attach(appstate);
	}
}
