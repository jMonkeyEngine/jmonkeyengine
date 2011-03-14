package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;


public class TestSceneLoading extends SimpleApplication {

    private void setState(Spatial s){
        s.setCullHint(CullHint.Never);
        if (s instanceof Node){
            Node n = (Node) s;
            for (int i = 0; i < n.getQuantity(); i++){
                Spatial s2 = n.getChild(i);
                setState(s2);
            }
        }
    }

    public void simpleInitApp() {
	/* XXX: does not compile */

/*        Spatial scene = inputManager.loadModel("FINAL_LEVEL2.j3o");
//        setState(scene);
        rootNode.attachChild(scene);

        cam.setLocation(new Vector3f(-18.059685f, 34.64228f, 4.5048084f));
        cam.setRotation(new Quaternion(0.22396432f, 0.5235024f, -0.1448922f, 0.8091919f));
        cam.update();
*/
    }

}
