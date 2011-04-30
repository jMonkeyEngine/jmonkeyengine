/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;

/**
 *
 * @author Nehon
 */
public class TestKinematicAddToPhysicsSpaceIssue extends SimpleApplication {

    public static void main(String[] args) {
        TestKinematicAddToPhysicsSpaceIssue app = new TestKinematicAddToPhysicsSpaceIssue();
        app.start();
    }
    BulletAppState bulletAppState;

    @Override
    public void simpleInitApp() {

        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        // Add a physics sphere to the world
        Node physicsSphere = PhysicsTestHelper.createPhysicsTestNode(assetManager, new SphereCollisionShape(1), 1);
        physicsSphere.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(3, 6, 0));
        rootNode.attachChild(physicsSphere);

        //Setting the rigidBody to kinematic before adding it to the physic space
        physicsSphere.getControl(RigidBodyControl.class).setKinematic(true);
        //adding it to the physic space
        getPhysicsSpace().add(physicsSphere);         
        //Making it not kinematic again, it should fall under gravity, it doesn't
        physicsSphere.getControl(RigidBodyControl.class).setKinematic(false);

        // Add a physics sphere to the world using the collision shape from sphere one
        Node physicsSphere2 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new SphereCollisionShape(1), 1);
        physicsSphere2.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(5, 6, 0));
        rootNode.attachChild(physicsSphere2);
        
        //Adding the rigid body to physic space
        getPhysicsSpace().add(physicsSphere2);
        //making it kinematic
        physicsSphere2.getControl(RigidBodyControl.class).setKinematic(false);
        //Making it not kinematic again, it works properly, the rigidbody is affected by grvity.
        physicsSphere2.getControl(RigidBodyControl.class).setKinematic(false);

      

        // an obstacle mesh, does not move (mass=0)
        Node node2 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new MeshCollisionShape(new Sphere(16, 16, 1.2f)), 0);
        node2.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(2.5f, -4, 0f));
        rootNode.attachChild(node2);
        getPhysicsSpace().add(node2);

        // the floor mesh, does not move (mass=0)
        Node node3 = PhysicsTestHelper.createPhysicsTestNode(assetManager, new PlaneCollisionShape(new Plane(new Vector3f(0, 1, 0), 0)), 0);
        node3.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(0f, -6, 0f));
        rootNode.attachChild(node3);
        getPhysicsSpace().add(node3);

    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
}
