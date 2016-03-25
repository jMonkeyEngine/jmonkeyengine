package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * test
 *
 * @author normenhansen
 */
public class TestTangentSpace extends SimpleApplication {
    
    public static void main(String[] args) {
        TestTangentSpace app = new TestTangentSpace();
        app.start();
    }
    
    private Node debugNode = new Node("debug");
    
    @Override
    public void simpleInitApp() {
        renderManager.setSinglePassLightBatchSize(2);
        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        initView();
        
        Spatial s = assetManager.loadModel("Models/Test/BasicCubeLow.obj");
        rootNode.attachChild(s);

        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setTexture("NormalMap", assetManager.loadTexture("Models/Test/Normal_pixel.png"));

        Geometry g = (Geometry)s;
        Geometry g2 = (Geometry) g.deepClone();
        g2.move(5, 0, 0);
        g.getParent().attachChild(g2);

        g.setMaterial(m);
        g2.setMaterial(m);

        //Regular tangent generation (left geom)
        TangentBinormalGenerator.generate(g2.getMesh(), true);

        //MikkTSPace Tangent generation (right geom)        

        MikktspaceTangentGenerator.generate(g);
        
        createDebugTangents(g2);
        createDebugTangents(g);
        
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("toggleDebug") && isPressed) {
                    if (debugNode.getParent() == null) {
                        rootNode.attachChild(debugNode);
                    } else {
                        debugNode.removeFromParent();
                    }
                }
            }
        }, "toggleDebug");

        inputManager.addMapping("toggleDebug", new KeyTrigger(KeyInput.KEY_SPACE));
        
        
        DirectionalLight dl = new DirectionalLight(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
    }
    
    private void initView() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        cam.setLocation(new Vector3f(8.569681f, 3.335546f, 5.4372444f));
        cam.setRotation(new Quaternion(-0.07608022f, 0.9086564f, -0.18992864f, -0.3639813f));
        flyCam.setMoveSpeed(10);
    }
    
    private void createDebugTangents(Geometry geom) {
        Geometry debug = new Geometry(
                "Debug " + geom.getName(),
                TangentBinormalGenerator.genTbnLines(geom.getMesh(), 0.8f)
        );
        Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        debug.setMaterial(debugMat);
        debug.setCullHint(Spatial.CullHint.Never);
        debug.getLocalTranslation().set(geom.getWorldTranslation());
        debugNode.attachChild(debug);
    }
}
