package jme3test.animation;

import java.io.IOException;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimFactory;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.app.SimpleApplication;
import com.jme3.cinematic.PlayState;
import com.jme3.cinematic.events.AnimEvent;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;


public class TestAnimEventSavable extends SimpleApplication {

    public static void main(String[] args) {
        TestAnimEventSavable app = new TestAnimEventSavable();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private Spatial teapot;
    private AnimEvent evt;

    public static class AnimatedScene implements Savable{
        public Node scene;
        public AnimEvent anim;

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule oc = ex.getCapsule(this);
            oc.write(scene, "scene", null);
            oc.write(anim, "anim", null);
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule ic = im.getCapsule(this);
            scene = (Node) ic.readSavable("scene", null);
            anim = (AnimEvent) ic.readSavable("anim", null);
        }
        
    }

    @Override
    public void simpleInitApp() {

        viewPort.setBackgroundColor(ColorRGBA.DarkGray);


        setupLightsAndFilters();
        setupModel();

        Node jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        AnimMigrationUtils.migrate(jaime);
        jaime.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);       
        evt = new AnimEvent(jaime.getControl(AnimComposer.class), "JumpStart", AnimComposer.DEFAULT_LAYER);
        
        AnimatedScene original = new AnimatedScene();
        original.scene = jaime;
        original.anim = evt;

        AnimatedScene copy  = BinaryExporter.saveAndLoad(assetManager, original);
        rootNode.attachChild(copy.scene);
        evt = copy.anim;
        
        assert copy.anim.getComposer()!=original.anim.getComposer();
        assert copy.scene.getControl(AnimComposer.class)==copy.anim.getComposer();
         

        initInputs();
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

   
    private void initInputs() {
        inputManager.addMapping("togglePlay", new KeyTrigger(KeyInput.KEY_SPACE));
        ActionListener acl = new ActionListener() {

            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("togglePlay") && keyPressed) {
                    if (evt.getPlayState() == PlayState.Playing) {
                        evt.pause();
                    } else {
                        System.out.println("Play");
                        evt.play();
                    }
                }

            }
        };
        inputManager.addListener(acl, "togglePlay");
    }
}