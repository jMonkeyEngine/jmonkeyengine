/*
 * Copyright (c) 2020 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import java.util.logging.Logger;

/**
 * Test case for JME issue #1283: collision-group filter not applied to CCD.
 * <p>
 * Click RMB or press the "B" key to shoot a ball at the wall. In a successful
 * test, all balls will pass through the wall. If any ball rebounds, or is
 * deflected, the has test failed.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue1283 extends SimpleApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * radius of projectiles
     */
    final private static float projectileRadius = 0.7f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestIssue1283.class.getName());
    /**
     * Mesh to visualize projectiles
     */
    final private static Mesh projectileMesh
            = new Sphere(32, 32, projectileRadius, true, false);
    // *************************************************************************
    // fields

    /**
     * Material to visualize projectiles
     */
    private Material projectileMaterial;
    /**
     * Material to visualize the wall
     */
    private Material wallMaterial;
    /**
     * space for physics simulation
     */
    private PhysicsSpace physicsSpace;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestIssue1283 application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        TestIssue1283 application = new TestIssue1283();
        AppSettings appSettings = new AppSettings(true);
        application.setSettings(appSettings);
        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        configureCamera();
        configureMaterials();
        viewPort.setBackgroundColor(ColorRGBA.Blue.clone());
        addLighting();
        configurePhysics();
        addWall();
        configureInputs();
        showHints();
    }
    // *************************************************************************
    // private methods

    /**
     * Add lighting to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(1f, -2f, -2f).normalizeLocal();
        ColorRGBA sunColor = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        DirectionalLight sun = new DirectionalLight(direction, sunColor);
        rootNode.addLight(sun);
    }

    /**
     * Add a thin wall to the scene and physics space.
     */
    private void addWall() {
        float thickness = 0.1f;
        Box wallMesh = new Box(10f, 10f, thickness);
        Geometry geometry = new Geometry("wall", wallMesh);
        rootNode.attachChild(geometry);
        geometry.setMaterial(wallMaterial);

        float mass = 0f; // static rigid body
        RigidBodyControl physicsControl = new RigidBodyControl(mass);
        geometry.addControl(physicsControl);

        physicsControl.setRestitution(0.8f);
        physicsSpace.add(physicsControl);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        float fHeight = cam.getFrustumTop() - cam.getFrustumBottom();
        float fWidth = cam.getFrustumRight() - cam.getFrustumLeft();
        float fAspect = fWidth / fHeight;
        float yDegrees = 45f;
        float near = 0.2f;
        float far = 100f;
        cam.setFrustumPerspective(yDegrees, fAspect, near, far);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(200f);

        cam.setLocation(new Vector3f(-2f, 2f, 30f));
        cam.setRotation(new Quaternion(0f, 1f, 0f, 0f));
    }

    /**
     * Configure the InputManager during startup.
     */
    private void configureInputs() {
        final String launchActionName = "launch";
        ActionListener actionListener = new ActionListener() {
            @Override
            public void onAction(String name, boolean ongoing, float tpf) {
                if (ongoing) {
                    if (name.equals(launchActionName)) {
                        launchProjectile();
                    }
                }
            }
        };

        Trigger bTrigger = new KeyTrigger(KeyInput.KEY_B);
        Trigger rmbTrigger = new MouseButtonTrigger(MouseInput.BUTTON_RIGHT);
        inputManager.addMapping(launchActionName, bTrigger, rmbTrigger);
        inputManager.addListener(actionListener, launchActionName);
    }

    /**
     * Configure materials during startup.
     */
    private void configureMaterials() {
        wallMaterial = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        wallMaterial.setColor("Color", ColorRGBA.White.clone());
        wallMaterial.getAdditionalRenderState().setWireframe(true);

        projectileMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        projectileMaterial.setBoolean("UseMaterialColors", true);
        projectileMaterial.setColor("Ambient", ColorRGBA.Red.clone());
        projectileMaterial.setColor("Diffuse", ColorRGBA.Red.clone());
        projectileMaterial.setColor("Specular", ColorRGBA.Black.clone());
    }

    /**
     * Configure physics during startup.
     */
    private void configurePhysics() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        physicsSpace = bulletAppState.getPhysicsSpace();
        Vector3f gravityVector = new Vector3f(0f, -30f, 0f);
        physicsSpace.setGravity(gravityVector);
    }

    /**
     * Add a projectile to the scene and physics space. Its initial position and
     * velocity are determined by the camera the and mouse pointer.
     */
    private void launchProjectile() {
        Vector2f screenXY = inputManager.getCursorPosition();
        float nearZ = 0f;
        Vector3f nearLocation = cam.getWorldCoordinates(screenXY, nearZ);
        float farZ = 1f;
        Vector3f farLocation = cam.getWorldCoordinates(screenXY, farZ);
        Vector3f direction = farLocation.subtract(nearLocation);
        direction.normalizeLocal();

        Geometry geometry = new Geometry("projectile", projectileMesh);
        rootNode.attachChild(geometry);
        geometry.setLocalTranslation(nearLocation);
        geometry.setMaterial(projectileMaterial);

        float mass = 1f;
        RigidBodyControl physicsControl = new RigidBodyControl(mass);
        geometry.addControl(physicsControl);

        physicsControl.setCcdMotionThreshold(0.01f);
        physicsControl.setCcdSweptSphereRadius(projectileRadius);
        physicsControl.setCollisionGroup(
                PhysicsCollisionObject.COLLISION_GROUP_02);
        physicsControl.setCollideWithGroups(
                PhysicsCollisionObject.COLLISION_GROUP_02);
        physicsControl.setRestitution(0.8f);

        float projectileSpeed = 250f; // physics-space units per second
        Vector3f initialVelocity = direction.mult(projectileSpeed);
        physicsControl.setLinearVelocity(initialVelocity);

        physicsSpace.add(physicsControl);
    }

    /**
     * Attach hint text to the GUI node.
     */
    private void showHints() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");

        int numLines = 4;
        BitmapText lines[] = new BitmapText[numLines];
        for (int lineIndex = 0; lineIndex < numLines; ++lineIndex) {
            lines[lineIndex] = new BitmapText(guiFont);
        }

        lines[0].setText("Test for jMonkeyEngine issue #1283");
        lines[1].setText("Click RMB or press the B key to shoot a ball.");
        lines[2].setText("Use W/A/S/D/Q/Z keys to move the camera.");
        lines[3].setText("F5: toggle render statistics,"
                + " C: print camera position, M: print memory statistics");

        float textHeight = guiFont.getCharSet().getLineHeight();
        float viewHeight = cam.getHeight();
        float viewWidth = cam.getWidth();
        for (int lineIndex = 0; lineIndex < numLines; ++lineIndex) {
            float lineWidth = lines[lineIndex].getLineWidth();
            float leftX = Math.round((viewWidth - lineWidth) / 2f);
            float topY = viewHeight - lineIndex * textHeight;
            lines[lineIndex].setLocalTranslation(leftX, topY, 0f);
            guiNode.attachChild(lines[lineIndex]);
        }
    }
}
