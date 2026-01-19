package jme3test.animation;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimFactory;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.PlayState;
import com.jme3.cinematic.events.AnimEvent;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.cinematic.events.SoundEvent;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Caps;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;

/**
 * 
 * @author capdevon
 */
public class TestCinematicSavable extends SimpleApplication {

    public static void main(String[] args) {
        TestCinematicSavable app = new TestCinematicSavable();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private Cinematic cinematic;
    private Spatial teapot;
    private MotionEvent cameraMotionEvent;

    @Override
    public void simpleInitApp() {

        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        cinematic = new Cinematic(rootNode, 10);
        // cinematic.initialize(stateManager, this);
        // stateManager.attach(cinematic);

        setupLightsAndFilters();
        setupModel();
        createCameraMotion();

        Node jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        AnimMigrationUtils.migrate(jaime);
        jaime.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(jaime);

        cinematic.activateCamera(0, "aroundCam");
        // cinematic.activateCamera(0, "topView");
        cinematic.addCinematicEvent(0f, new AnimEvent(teapot.getControl(AnimComposer.class), "teapotAnim",
                AnimComposer.DEFAULT_LAYER));
        cinematic.addCinematicEvent(0f,
                new AnimEvent(jaime.getControl(AnimComposer.class), "JumpStart", AnimComposer.DEFAULT_LAYER));
        cinematic.addCinematicEvent(0f, cameraMotionEvent);
        cinematic.addCinematicEvent(0f, new SoundEvent("Sound/Environment/Nature.ogg", LoopMode.Loop));
        cinematic.addCinematicEvent(3f, new SoundEvent("Sound/Effects/kick.wav"));
        cinematic.addCinematicEvent(5.1f, new SoundEvent("Sound/Effects/Beep.ogg", 1));

        cinematic.addListener(new CinematicEventListener() {

            @Override
            public void onPlay(CinematicEvent cinematic) {
                flyCam.setEnabled(false);
                System.out.println("play");
            }

            @Override
            public void onPause(CinematicEvent cinematic) {
                System.out.println("pause");
            }

            @Override
            public void onStop(CinematicEvent cinematic) {
                flyCam.setEnabled(true);
                System.out.println("stop");
            }
        });

        Cinematic copy = BinaryExporter.saveAndLoad(assetManager, cinematic);
        stateManager.detach(cinematic);

        cinematic = copy;
        cinematic.setScene(rootNode);
        stateManager.attach(cinematic);

        configureCamera();

        initInputs();
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(25f);
        flyCam.setDragToRotate(true);
        cam.setLocation(Vector3f.UNIT_XYZ.mult(12));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private void setupLightsAndFilters() {
        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(0, -1, -1).normalizeLocal());
        light.setColor(ColorRGBA.White.mult(1.5f));
        rootNode.addLight(light);

        if (renderer.getCaps().contains(Caps.GLSL100)) {
            DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, 512, 1);
            dlsr.setLight(light);
            dlsr.setShadowIntensity(0.4f);
            viewPort.addProcessor(dlsr);
        }
    }

    private void setupModel() {
        teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalTranslation(5, 0, 5);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Cyan);
        teapot.setMaterial(mat);
        teapot.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(teapot);

        // creating spatial animation for the teapot
        AnimFactory factory = new AnimFactory(20f, "teapotAnim", 30f);
        factory.addTimeTranslation(0, new Vector3f(5, 0, 5));
        factory.addTimeTranslation(4, new Vector3f(5, 0, -5));
        AnimClip animClip = factory.buildAnimation(teapot);

        AnimComposer animComposer = new AnimComposer();
        animComposer.addAnimClip(animClip);
        teapot.addControl(animComposer);
    }

    private void createCameraMotion() {
        CameraNode camNode = cinematic.bindCamera("topView", cam);
        camNode.setLocalTranslation(new Vector3f(0, 50, 0));
        camNode.lookAt(teapot.getLocalTranslation(), Vector3f.UNIT_Y);

        CameraNode camNode2 = cinematic.bindCamera("aroundCam", cam);
        MotionPath path = new MotionPath();
        path.setCycle(true);
        path.addWayPoint(new Vector3f(20, 3, 0));
        path.addWayPoint(new Vector3f(0, 3, 20));
        path.addWayPoint(new Vector3f(-20, 3, 0));
        path.addWayPoint(new Vector3f(0, 3, -20));
        path.setCurveTension(0.83f);
        cameraMotionEvent = new MotionEvent(camNode2, path);
        cameraMotionEvent.setLoopMode(LoopMode.Loop);
        cameraMotionEvent.setLookAt(teapot.getWorldTranslation(), Vector3f.UNIT_Y);
        cameraMotionEvent.setDirectionType(MotionEvent.Direction.LookAt);
    }

    private void initInputs() {
        inputManager.addMapping("togglePlay", new KeyTrigger(KeyInput.KEY_SPACE));
        ActionListener acl = new ActionListener() {

            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("togglePlay") && keyPressed) {
                    if (cinematic.getPlayState() == PlayState.Playing) {
                        cinematic.pause();
                    } else {
                        System.out.println("Play");
                        cinematic.play();
                    }
                }

            }
        };
        inputManager.addListener(acl, "togglePlay");
    }
}