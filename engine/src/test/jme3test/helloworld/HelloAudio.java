package jme3test.helloworld;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/** Sample 11 - playing 3D audio. */
public class HelloAudio extends SimpleApplication {

  private AudioNode audio_gun;
  private AudioNode audio_nature;
  private Geometry player;

  public static void main(String[] args) {
    HelloAudio app = new HelloAudio();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    flyCam.setMoveSpeed(40);
    
    /** just a blue box floating in space */
    Box box1 = new Box(Vector3f.ZERO, 1, 1, 1);
    player = new Geometry("Player", box1);
    Material mat1 = new Material(assetManager, 
            "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setColor("Color", ColorRGBA.Blue);
    player.setMaterial(mat1);
    rootNode.attachChild(player);

    /** custom init methods, see below */
    initKeys();
    initAudio();
  }

  /** We create two audio nodes. */
  private void initAudio() {
    /* gun shot sound is to be triggered by a mouse click. */
    audio_gun = new AudioNode(assetManager, "Sound/Effects/Gun.wav", false);
    audio_gun.setLooping(false);
    audio_gun.setVolume(2);
    rootNode.attachChild(audio_gun);

    /* nature sound - keeps playing in a loop. */
    audio_nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
    audio_nature.setLooping(true);  // activate continuous playing
    audio_nature.setPositional(true);
    audio_nature.setLocalTranslation(Vector3f.ZERO.clone());
    audio_nature.setVolume(3);
    rootNode.attachChild(audio_nature);
    audio_nature.play(); // play continuously!
  }

  /** Declaring "Shoot" action, mapping it to a trigger (mouse click). */
  private void initKeys() {
    inputManager.addMapping("Shoot", new MouseButtonTrigger(0));
    inputManager.addListener(actionListener, "Shoot");
  }

  /** Defining the "Shoot" action: Play a gun sound. */
  private ActionListener actionListener = new ActionListener() {
    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("Shoot") && !keyPressed) {
        audio_gun.playInstance(); // play each instance once!
      }
    }
  };

  /** Move the listener with the a camera - for 3D audio. */
  @Override
  public void simpleUpdate(float tpf) {
    listener.setLocation(cam.getLocation());
    listener.setRotation(cam.getRotation());
  }

}
