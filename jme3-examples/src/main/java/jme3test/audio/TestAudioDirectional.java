package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.environment.util.BoundingSphereDebug;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.GLMesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Line;

/**
 * @author capdevon
 */
public class TestAudioDirectional extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        TestAudioDirectional app = new TestAudioDirectional();
        app.start();
    }

    private AudioNode audioSource;
    private final Vector3f tempDirection = new Vector3f();
    private boolean rotationEnabled = true;

    @Override
    public void simpleInitApp() {
        configureCamera();

        audioSource = new AudioNode(assetManager,
                "Sound/Environment/Ocean Waves.ogg", AudioData.DataType.Buffer);
        audioSource.setLooping(true);
        audioSource.setPositional(true);
        audioSource.setMaxDistance(100);
        audioSource.setRefDistance(5);
        audioSource.setDirectional(true);
//        audioSource.setOuterGain(0.2f); // Volume outside the cone is 20% of the inner volume (Not Supported by jME)
        audioSource.setInnerAngle(30); // 30-degree cone (15 degrees on each side of the direction)
        audioSource.setOuterAngle(90); // 90-degree cone (45 degrees on each side of the direction)
        audioSource.play();

        // just a green sphere to mark the spot
        Geometry sphere =  BoundingSphereDebug.createDebugSphere(assetManager);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Green);
        sphere.setMaterial(mat);
        sphere.setLocalScale(0.5f);
        audioSource.attachChild(sphere);

        float angleIn = audioSource.getInnerAngle() * FastMath.DEG_TO_RAD;
        float angleOut = audioSource.getOuterAngle() * FastMath.DEG_TO_RAD;
        Vector3f forwardDir = audioSource.getWorldRotation().mult(Vector3f.UNIT_Z);

        audioSource.attachChild(createFOV(angleIn, 20f));
        audioSource.attachChild(createFOV(angleOut, 20f));
        audioSource.attachChild(makeShape("ZAxis", new Arrow(forwardDir.mult(5)), ColorRGBA.Blue));
        rootNode.attachChild(audioSource);

        Geometry grid = makeShape("DebugGrid", new Grid(21, 21, 2), ColorRGBA.Gray);
        grid.center().move(0, 0, 0);
        rootNode.attachChild(grid);

        registerInputMappings();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (rotationEnabled) {
            // Example: Rotate the audio node
            audioSource.rotate(0, tpf * 0.5f, 0);
            audioSource.setDirection(audioSource.getWorldRotation().mult(Vector3f.UNIT_Z, tempDirection));
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if (name.equals("toggleDirectional")) {
            boolean directional = !audioSource.isDirectional();
            audioSource.setDirectional(directional);
            System.out.println("directional: " + directional);

        } else if (name.equals("toggleRotationEnabled")) {
            rotationEnabled = !rotationEnabled;
            System.out.println("rotationEnabled: " + rotationEnabled);
        }
    }

    private void registerInputMappings() {
        addMapping("toggleDirectional", new KeyTrigger(KeyInput.KEY_SPACE));
        addMapping("toggleRotationEnabled", new KeyTrigger(KeyInput.KEY_P));
    }

    private void addMapping(String mappingName, Trigger... triggers) {
        inputManager.addMapping(mappingName, triggers);
        inputManager.addListener(this, mappingName);
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);

        cam.setLocation(new Vector3f(12, 5, 12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private Geometry makeShape(String name, GLMesh mesh, ColorRGBA color) {
        Geometry geo = new Geometry(name, mesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geo.setMaterial(mat);
        return geo;
    }

    private Spatial createFOV(float angleRad, float extent) {
        Vector3f origin = new Vector3f();
        Node node = new Node("Cone");
        Vector3f sx = dirFromAngle(angleRad/2).scaleAdd(extent, origin);
        Vector3f dx = dirFromAngle(-angleRad/2).scaleAdd(extent, origin);
        node.attachChild(makeShape("Line.SX", new Line(origin, sx), ColorRGBA.Red));
        node.attachChild(makeShape("Line.DX", new Line(origin, dx), ColorRGBA.Red));

        return node;
    }

    private Vector3f dirFromAngle(float angleRad) {
        return new Vector3f(FastMath.sin(angleRad), 0, FastMath.cos(angleRad));
    }
}
