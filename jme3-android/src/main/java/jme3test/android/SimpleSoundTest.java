package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector3f;

public class SimpleSoundTest extends SimpleApplication implements InputListener {

    private AudioNode gun;
    private AudioNode nature;

    @Override
    public void simpleInitApp() {
        gun = new AudioNode(assetManager, "Sound/Effects/Gun.wav");
        gun.setPositional(true);
        gun.setLocalTranslation(new Vector3f(0, 0, 0));
        gun.setMaxDistance(100);
        gun.setRefDistance(5);

        nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
        nature.setVolume(3);
        nature.setLooping(true);
        nature.play();

        inputManager.addMapping("click", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "click");

        rootNode.attachChild(gun);
        rootNode.attachChild(nature);
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("click") && isPressed) {
            gun.playInstance();
        }
    }
}
