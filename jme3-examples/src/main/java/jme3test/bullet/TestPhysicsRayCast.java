package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.List;

/**
 *
 * @author @wezrule
 */
public class TestPhysicsRayCast extends SimpleApplication {

    private BulletAppState bulletAppState = new BulletAppState();

    public static void main(String[] args) {
        new TestPhysicsRayCast().start();
    }

    @Override
    public void simpleInitApp() {
        stateManager.attach(bulletAppState);
        initCrossHair();

        Spatial s = assetManager.loadModel("Models/Elephant/Elephant.mesh.xml");
        s.setLocalScale(0.1f);

        CollisionShape collisionShape = CollisionShapeFactory.createMeshShape(s);
        Node n = new Node("elephant");
        n.addControl(new RigidBodyControl(collisionShape, 1));
        n.getControl(RigidBodyControl.class).setKinematic(true);
        bulletAppState.getPhysicsSpace().add(n);
        rootNode.attachChild(n);
        bulletAppState.setDebugEnabled(true);
    }

    @Override
    public void simpleUpdate(float tpf) {
        float rayLength = 50f;
        Vector3f start = cam.getLocation();
        Vector3f end = cam.getDirection().scaleAdd(rayLength, start);
        List<PhysicsRayTestResult> rayTest
                = bulletAppState.getPhysicsSpace().rayTest(start, end);
        if (rayTest.size() > 0) {
            PhysicsRayTestResult get = rayTest.get(0);
            PhysicsCollisionObject collisionObject = get.getCollisionObject();
            // Display the name of the 1st object in place of FPS.
            fpsText.setText(collisionObject.getUserObject().toString());
        } else {
            // Provide prompt feedback that no collision object was hit.
            fpsText.setText("MISSING");
        }
    }

    private void initCrossHair() {
        BitmapText bitmapText = new BitmapText(guiFont);
        bitmapText.setText("+");
        bitmapText.setLocalTranslation((settings.getWidth() - bitmapText.getLineWidth())*0.5f, (settings.getHeight() + bitmapText.getLineHeight())*0.5f, 0);
        guiNode.attachChild(bitmapText);
    }
}
