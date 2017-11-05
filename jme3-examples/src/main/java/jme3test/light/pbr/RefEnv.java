package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;

/**
 * test
 *
 * @author nehon
 */
public class RefEnv extends SimpleApplication {

    private Node tex;
    private Node ref;
    private Picture refImg;

    public static void main(String[] args) {
        RefEnv app = new RefEnv();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        cam.setLocation(new Vector3f(-17.95047f, 4.917353f, -17.970531f));
        cam.setRotation(new Quaternion(0.11724457f, 0.29356146f, -0.03630452f, 0.94802815f));
//        cam.setLocation(new Vector3f(14.790441f, 7.164179f, 19.720007f));
//        cam.setRotation(new Quaternion(-0.038261678f, 0.9578362f, -0.15233073f, -0.24058504f));
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(5);
        Spatial sc = assetManager.loadModel("Scenes/PBR/ref/scene.gltf");
        rootNode.attachChild(sc);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        rootNode.getChild(0).setCullHint(Spatial.CullHint.Always);

        ref = new Node("reference pictures");
        refImg = new Picture("refImg");
        refImg.setHeight(cam.getHeight());
        refImg.setWidth(cam.getWidth());
        refImg.setImage(assetManager, "jme3test/light/pbr/ref.png", false);

        ref.attachChild(refImg);

        stateManager.attach(new EnvironmentCamera(256, Vector3f.ZERO));

        inputManager.addMapping("tex", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("switch", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("ref", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(new ActionListener() {

            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("tex") && isPressed) {
                    if (tex == null) {
                        tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(stateManager.getState(EnvironmentCamera.class).debugEnv, assetManager);
                    }
                    if (tex.getParent() == null) {
                        guiNode.attachChild(tex);
                    } else {
                        tex.removeFromParent();
                    }
                }

                if (name.equals("switch") && isPressed) {
                    switchMat(rootNode.getChild("Scene"));
                }
                if (name.equals("ref") && isPressed) {
                    if (ref.getParent() == null) {
                        guiNode.attachChild(ref);
                    } else {
                        ref.removeFromParent();
                    }
                }
            }
        }, "tex", "switch", "ref");

    }

    private void switchMat(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial children : n.getChildren()) {
                switchMat(children);
            }
        } else if (s instanceof Geometry) {
            Geometry g = (Geometry) s;
            Material mat = g.getMaterial();
            if (((Float) mat.getParam("Metallic").getValue()) == 1f) {
                mat.setFloat("Metallic", 0);
                mat.setColor("BaseColor", ColorRGBA.Black);
                ref.attachChild(refImg);
            } else {
                mat.setFloat("Metallic", 1);
                mat.setColor("BaseColor", ColorRGBA.White);
                refImg.removeFromParent();
            }
        }
    }

    private int frame = 0;

    @Override
    public void simpleUpdate(float tpf) {
        frame++;

        if (frame == 2) {
            final LightProbe probe = LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, EnvMapUtils.GenerationType.Fast, new JobProgressAdapter<LightProbe>() {

                @Override
                public void done(LightProbe result) {
                    System.err.println("Done rendering env maps");
                    tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                    rootNode.getChild(0).setCullHint(Spatial.CullHint.Dynamic);
                }
            });
            ((BoundingSphere) probe.getBounds()).setRadius(100);
            rootNode.addLight(probe);

        }
    }
}
