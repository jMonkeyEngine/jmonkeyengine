package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;

/**
 * This test demonstrates that destroying and recreating the OpenAL Context
 * upon device disconnection is not an optimal solution.
 *
 * As shown, AudioNode instances playing in a loop cease to play after a device disconnection
 * and would require explicit restarting.
 *
 * This test serves solely to highlight this issue,
 * which should be addressed with a more robust solution within
 * the ALAudioRenderer class in a dedicated future pull request on Git.
 */
public class TestAudioDeviceDisconnect extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestAudioDeviceDisconnect test = new TestAudioDeviceDisconnect();
        test.start();
    }

    private AudioNode audioSource;

    @Override
    public void simpleInitApp() {
        audioSource = new AudioNode(assetManager,
                "Sound/Environment/Ocean Waves.ogg", AudioData.DataType.Buffer);
        audioSource.setName("Waves");
        audioSource.setLooping(true);
        rootNode.attachChild(audioSource);

        audioSource.play();

        registerInputMappings();
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("play")) {
            // re-play active sounds
            audioSource.play();
        }
    }

    private void registerInputMappings() {
        addMapping("play", new KeyTrigger(KeyInput.KEY_SPACE));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

}
