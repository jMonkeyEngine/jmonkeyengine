package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;

public class TestDeviceRumble extends SimpleApplication {

    private static final String CLICK_MAPPING = "ClickCube";

    private Geometry cube;
    private Material cubeMaterial;
    private BitmapText statusText;
    private boolean rumbling;

    public static void main(String[] args) {
        TestDeviceRumble app = new TestDeviceRumble();
        AppSettings settings = new AppSettings(true);
        settings.setUseJoysticks(false);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        inputManager.setCursorVisible(true);

        cubeMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        cubeMaterial.setColor("Color", ColorRGBA.Blue);

        cube = new Geometry("Device rumble cube", new Box(1.25f, 1.25f, 1.25f));
        cube.setMaterial(cubeMaterial);
        rootNode.attachChild(cube);

        DirectionalLight light = new DirectionalLight();
        light.setDirection(new Vector3f(-1f, -1f, -1f).normalizeLocal());
        rootNode.addLight(light);

        cam.setLocation(new Vector3f(0f, 0f, 6f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        statusText = new BitmapText(guiFont);
        statusText.setLocalTranslation(12f, cam.getHeight() - 12f, 0f);
        guiNode.attachChild(statusText);
        updateStatusText();

        inputManager.addMapping(CLICK_MAPPING, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed) {
                startRumble(inputManager.getCursorPosition());
            } else {
                stopRumble();
            }
        }, CLICK_MAPPING);

        inputManager.addRawInputListener(new RawInputListenerAdapter() {
            @Override
            public void onTouchEvent(TouchEvent evt) {
                if (evt.getType() == TouchEvent.Type.DOWN) {
                    startRumble(new Vector2f(evt.getX(), evt.getY()));
                } else if (evt.getType() == TouchEvent.Type.UP) {
                    stopRumble();
                }
            }
        });
    }

    private void startRumble(Vector2f screenPosition) {
        if (!isCubeHit(screenPosition)) {
            return;
        }

        rumbling = true;
        cubeMaterial.setColor("Color", ColorRGBA.Red);
        if (JmeSystem.isDeviceRumbleSupported()) {
            JmeSystem.rumble(1f, 1f, Float.POSITIVE_INFINITY);
        }
        updateStatusText();
    }

    private void stopRumble() {
        if (!rumbling) {
            return;
        }
        rumbling = false;
        JmeSystem.stopRumble();
        cubeMaterial.setColor("Color", ColorRGBA.Blue);
        updateStatusText();
    }

    private boolean isCubeHit(Vector2f screenPosition) {
        Vector3f origin = cam.getWorldCoordinates(screenPosition, 0f);
        Vector3f direction = cam.getWorldCoordinates(screenPosition, 1f).subtractLocal(origin).normalizeLocal();
        CollisionResults results = new CollisionResults();
        cube.collideWith(new Ray(origin, direction), results);
        return results.size() > 0;
    }

    private void updateStatusText() {
        String state = rumbling ? "rumbling" : "idle";
        String supported = JmeSystem.isDeviceRumbleSupported() ? "supported" : "not supported";
        statusText.setText("Device rumble: " + supported + "\nHold the cube to test\nState: " + state);
    }
}
