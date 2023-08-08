package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * TestPBRSimple
 */
public class TestPBRSimple extends SimpleApplication{

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

        
        EnvironmentProbeControl envProbe=new EnvironmentProbeControl(renderManager,assetManager,256);
        rootNode.addControl(envProbe);
        
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tag(sky);
    }
    
}