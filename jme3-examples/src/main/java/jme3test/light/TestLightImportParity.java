package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.ChaseCamera;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.KHRToneMapFilter;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;

/**
 * Test how lights render compared to the same scene in Blender. Open
 * jme3-testdata/src/main/resources/BlenderParity/scene.blend in blender to compare.
 */
public class TestLightImportParity extends SimpleApplication {
    public static void main(String[] args) {
        TestLightImportParity app = new TestLightImportParity();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(100f);
        
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Alien.png", SkyFactory.EnvMapType.EquirectMap);
        sky.rotate(new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y));
        rootNode.attachChild(sky);

        EnvironmentProbeControl probe = new EnvironmentProbeControl(assetManager, 512);
        rootNode.addControl(probe);
        probe.tag(sky);
        
        Node scene = (Node)assetManager.loadModel("BlenderParity/scene.glb");
        rootNode.attachChild(scene);

        KHRToneMapFilter toneMap = new KHRToneMapFilter();
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(toneMap);
        viewPort.addProcessor(fpp);

        ChaseCamera chaseCam = new ChaseCamera(cam, scene, inputManager);
        chaseCam.setDefaultDistance(100);
        chaseCam.setMaxDistance(200);
        
        

         
    }
}