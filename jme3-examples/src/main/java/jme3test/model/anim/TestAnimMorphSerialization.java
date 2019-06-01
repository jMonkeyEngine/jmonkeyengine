package jme3test.model.anim;

import com.jme3.anim.*;
import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.*;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.custom.ArmatureDebugAppState;
import com.jme3.system.JmeSystem;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by Nehon on 18/12/2017.
 */
public class TestAnimMorphSerialization extends SimpleApplication {

    ArmatureDebugAppState debugAppState;
    AnimComposer composer;
    Queue<String> anims = new LinkedList<>();
    boolean playAnim = true;
    File file;

    public static void main(String... argv) {
        TestAnimMorphSerialization app = new TestAnimMorphSerialization();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setTimer(new EraseTimer());
        //cam.setFrustumPerspective(90f, (float) cam.getWidth() / cam.getHeight(), 0.01f, 10f);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        //rootNode.addLight(new DirectionalLight(new Vector3f(-1, -1, -1).normalizeLocal()));
        //rootNode.addLight(new AmbientLight(ColorRGBA.DarkGray));
        Node probeNode = (Node) assetManager.loadModel("Scenes/defaultProbe.j3o");
        rootNode.attachChild(probeNode);
        Spatial model = assetManager.loadModel("Models/gltf/zophrac/scene.gltf");

        File storageFolder = JmeSystem.getStorageFolder();
        file = new File(storageFolder.getPath() + File.separator + "zophrac.j3o");
        BinaryExporter be = new BinaryExporter();
        try {
            be.save(model, file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assetManager.registerLocator(storageFolder.getPath(), FileLocator.class);
        Spatial model2 = assetManager.loadModel("zophrac.j3o");
        model2.setLocalScale(0.1f);
        probeNode.attachChild(model2);

        debugAppState = new ArmatureDebugAppState();
        stateManager.attach(debugAppState);

        setupModel(model2);

        flyCam.setEnabled(false);

        Node target = new Node("CamTarget");
        //target.setLocalTransform(model.getLocalTransform());
        target.move(0, 0, 0);
        ChaseCameraAppState chaseCam = new ChaseCameraAppState();
        chaseCam.setTarget(target);
        getStateManager().attach(chaseCam);
        chaseCam.setInvertHorizontalAxis(true);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setZoomSpeed(0.5f);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI);
        chaseCam.setRotationSpeed(3);
        chaseCam.setDefaultDistance(3);
        chaseCam.setMinDistance(0.01f);
        chaseCam.setZoomSpeed(0.01f);
        chaseCam.setDefaultVerticalRotation(0.3f);

        initInputs();
    }

    public void initInputs() {
        inputManager.addMapping("toggleAnim", new KeyTrigger(KeyInput.KEY_RETURN));

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    playAnim = !playAnim;
                    if (playAnim) {
                        String anim = anims.poll();
                        anims.add(anim);
                        composer.setCurrentAction(anim);
                        System.err.println(anim);
                    } else {
                        composer.reset();
                    }
                }
            }
        }, "toggleAnim");
        inputManager.addMapping("nextAnim", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed && composer != null) {
                    String anim = anims.poll();
                    anims.add(anim);
                    composer.setCurrentAction(anim);
                    System.err.println(anim);
                }
            }
        }, "nextAnim");
    }

    private void setupModel(Spatial model) {
        if (composer != null) {
            return;
        }
        composer = model.getControl(AnimComposer.class);
        if (composer != null) {
//            model.getControl(SkinningControl.class).setEnabled(false);
//            model.getControl(MorphControl.class).setEnabled(false);
//            composer.setEnabled(false);


            SkinningControl sc = model.getControl(SkinningControl.class);
            debugAppState.addArmatureFrom(sc);

            anims.clear();
            for (String name : composer.getAnimClipsNames()) {
                anims.add(name);
            }
            if (anims.isEmpty()) {
                return;
            }
            if (playAnim) {
                String anim = anims.poll();
                anims.add(anim);
                composer.setCurrentAction(anim);
                System.err.println(anim);
            }

        } else {
            if (model instanceof Node) {
                Node n = (Node) model;
                for (Spatial child : n.getChildren()) {
                    setupModel(child);
                }
            }
        }

    }


    @Override
    public void destroy() {
        super.destroy();
        file.delete();
    }
}
