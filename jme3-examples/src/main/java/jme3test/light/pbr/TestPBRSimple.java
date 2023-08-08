package jme3test.light.pbr;


import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * TestPBRSimple
 */
public class TestPBRSimple extends SimpleApplication {
    private boolean REALTIME_BAKING = false;

    public static void main(String[] args) {
        new TestPBRSimple().start();
    }
    
    @Override
    public void simpleInitApp() {
 
        
        Geometry model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);

        Material pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);
        rootNode.attachChild(model);

        ChaseCamera chaseCam = new ChaseCamera(cam, model, inputManager);
        chaseCam.setDragToRotate(true);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI);
        chaseCam.setMaxDistance(1000);
        chaseCam.setSmoothMotion(true);
        chaseCam.setRotationSensitivity(10);
        chaseCam.setZoomSensitivity(5);
        flyCam.setEnabled(false);

        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        // Create baker control
        EnvironmentProbeControl envProbe=new EnvironmentProbeControl(renderManager,assetManager,256);
        rootNode.addControl(envProbe);
       
        // Tag the sky, only the tagged spatials will be rendered in the env map
        EnvironmentProbeControl.tag(sky);


        
    }
    

    float lastBake = 0;
    @Override
    public void simpleUpdate(float tpf) {
        if (REALTIME_BAKING) {
            lastBake += tpf;
            if (lastBake > 1.4f) {
                rootNode.getControl(EnvironmentProbeControl.class).rebake();
                lastBake = 0;
            }
        }
    }
}