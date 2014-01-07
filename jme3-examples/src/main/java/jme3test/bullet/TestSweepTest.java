package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsSweepTestResult;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Transform;
import com.jme3.scene.Node;
import java.util.List;

/**
 *
 * A spatial moves and sweeps its next movement for obstacles before moving
 * there Run this example with Vsync enabled
 *
 * @author
 * @wezrule
 */
public class TestSweepTest extends SimpleApplication {

    private BulletAppState bulletAppState = new BulletAppState();
    private CapsuleCollisionShape obstacleCollisionShape = new CapsuleCollisionShape(0.3f, 0.5f);
    private CapsuleCollisionShape capsuleCollisionShape = new CapsuleCollisionShape(1f, 1f);
    private Node capsule;
    private Node obstacle;
    private float dist = .5f;

    public static void main(String[] args) {
        new TestSweepTest().start();
    }

    @Override
    public void simpleInitApp() {
        stateManager.attach(bulletAppState);

        capsule = new Node("capsule");
        capsule.move(-2, 0, 0);
        capsule.addControl(new RigidBodyControl(capsuleCollisionShape, 1));
        capsule.getControl(RigidBodyControl.class).setKinematic(true);
        bulletAppState.getPhysicsSpace().add(capsule);
        rootNode.attachChild(capsule);

        obstacle = new Node("obstacle");
        obstacle.move(2, 0, 0);
        RigidBodyControl bodyControl = new RigidBodyControl(obstacleCollisionShape, 0);
        obstacle.addControl(bodyControl);
        bulletAppState.getPhysicsSpace().add(obstacle);
        rootNode.attachChild(obstacle);

        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
    }

    @Override
    public void simpleUpdate(float tpf) {

        float move = tpf * 1;

        List<PhysicsSweepTestResult> sweepTest = bulletAppState.getPhysicsSpace().sweepTest(capsuleCollisionShape, new Transform(capsule.getWorldTranslation()), new Transform(capsule.getWorldTranslation().add(dist, 0, 0)));

        if (sweepTest.size() > 0) {
            PhysicsSweepTestResult get = sweepTest.get(0);
            PhysicsCollisionObject collisionObject = get.getCollisionObject();
            fpsText.setText("Almost colliding with " + collisionObject.getUserObject().toString());
        } else {
            // if the sweep is clear then move the spatial
            capsule.move(move, 0, 0);
        }
    }
}
