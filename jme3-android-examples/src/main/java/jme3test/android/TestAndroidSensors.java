package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.MouseInput;
import com.jme3.input.SensorJoystickAxis;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.texture.Texture;
import com.jme3.util.IntMap;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example Test Case to test using Android sensors as Joystick axes.  Make sure to enable Joystick Events from
 * the test chooser menus.  Rotating the device will cause the block to rotate.  Tapping the screen will cause the
 * sensors to be calibrated (reset to zero) at the current orientation.  Continuously tapping the screen causes
 * the "rumble" to intensify until it reaches the maximum amount and then it shuts off.
 *
 * @author iwgeric
 */
public class TestAndroidSensors extends SimpleApplication implements ActionListener, AnalogListener {

    private static final Logger logger = Logger.getLogger(TestAndroidSensors.class.getName());

    private Geometry geomZero = null;
    // Map of joysticks saved with the joyId as the key
    private IntMap<Joystick> joystickMap = new IntMap<>();
    // flag to allow for the joystick axis to be calibrated on startup
    private boolean initialCalibrationComplete = false;
    // mappings used for onAnalog
    private final String ORIENTATION_X_PLUS = "Orientation_X_Plus";
    private final String ORIENTATION_X_MINUS = "Orientation_X_Minus";
    private final String ORIENTATION_Y_PLUS = "Orientation_Y_Plus";
    private final String ORIENTATION_Y_MINUS = "Orientation_Y_Minus";
    private final String ORIENTATION_Z_PLUS = "Orientation_Z_Plus";
    private final String ORIENTATION_Z_MINUS = "Orientation_Z_Minus";


    // variables to save the current rotation
    // Used when controlling the geometry with device orientation
    private float[] anglesCurrent = new float[]{0f, 0f, 0f};
    private Quaternion rotationQuat = new Quaternion();

    // switch to apply an absolute rotation (geometry.setLocalRotation) or
    // an incremental constant rotation (geometry.rotate)
    // Used when controlling the geometry with device orientation
    private boolean useAbsolute = false;

    // rotation speed to use when apply constant incremental rotation
    // Used when controlling the geometry with device orientation
    private float rotationSpeedX = 1f;
    private float rotationSpeedY = 1f;

    // current intensity of the rumble
    float rumbleAmount = 0f;

    // toggle to enable rumble
    boolean enableRumble = true;

    // toggle to enable device orientation in FlyByCamera
    boolean enableFlyByCameraRotation = false;

    // toggle to enable controlling geometry rotation
    boolean enableGeometryRotation = true;

    // Make sure to set joystickEventsEnabled = true in MainActivity for Android

    private float toDegrees(float rad) {
        return rad * FastMath.RAD_TO_DEG;
    }

    @Override
    public void simpleInitApp() {

        // useAbsolute = true;
        // enableRumble = true;

        if (enableFlyByCameraRotation) {
            flyCam.setEnabled(true);
        } else {
            flyCam.setEnabled(false);
        }

        Mesh lineX = new Line(Vector3f.ZERO, Vector3f.ZERO.add(Vector3f.UNIT_X.mult(3)));
        Mesh lineY = new Line(Vector3f.ZERO, Vector3f.ZERO.add(Vector3f.UNIT_Y.mult(3)));
        Mesh lineZ = new Line(Vector3f.ZERO, Vector3f.ZERO.add(Vector3f.UNIT_Z.mult(3)));

        Geometry geoX = new Geometry("X", lineX);
        Material matX = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matX.setColor("Color", ColorRGBA.Red);
        matX.getAdditionalRenderState().setLineWidth(30);
        geoX.setMaterial(matX);
        rootNode.attachChild(geoX);

        Geometry geoY = new Geometry("Y", lineY);
        Material matY = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matY.setColor("Color", ColorRGBA.Green);
        matY.getAdditionalRenderState().setLineWidth(30);
        geoY.setMaterial(matY);
        rootNode.attachChild(geoY);

        Geometry geoZ = new Geometry("Z", lineZ);
        Material matZ = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matZ.setColor("Color", ColorRGBA.Blue);
        matZ.getAdditionalRenderState().setLineWidth(30);
        geoZ.setMaterial(matZ);
        rootNode.attachChild(geoZ);

        Box b = new Box(1, 1, 1);
        geomZero = new Geometry("Box", b);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Yellow);
        Texture tex_ml = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
        mat.setTexture("ColorMap", tex_ml);
        geomZero.setMaterial(mat);
        geomZero.setLocalTranslation(Vector3f.ZERO);
        geomZero.setLocalRotation(Quaternion.IDENTITY);
        rootNode.attachChild(geomZero);


        // Touch (aka MouseInput.BUTTON_LEFT) is used to record the starting
        // orientation when using absolute rotations
        inputManager.addMapping("MouseClick", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "MouseClick");

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks == null || joysticks.length < 1) {
            logger.log(Level.INFO, "Cannot find any joysticks!");
        } else {

            // Joysticks return a value of 0 to 1 based on how far the stick is
            // push on the axis. This value is then scaled based on how long
            // during the frame the joystick axis has been in that position.
            // If the joystick is push all the way for the whole frame,
            // then the value in onAnalog is equal to tpf.
            // If the joystick is push 1/2 way for the entire frame, then the
            // onAnalog value is 1/2 tpf.
            // Similarly, if the joystick is pushed to the maximum during a frame
            // the value in onAnalog will also be scaled.
            // For Android sensors, rotating the device 90deg is the same as
            // pushing an actual joystick axis to the maximum.

            logger.log(Level.INFO, "Number of joysticks: {0}", joysticks.length);
            JoystickAxis axis;

            for (Joystick joystick : joysticks) {
                // Get and display all axes in joystick.
                List<JoystickAxis> axes = joystick.getAxes();
                for (JoystickAxis joystickAxis : axes) {
                    logger.log(Level.INFO, "{0} axis scan Name: {1}, LogicalId: {2}, AxisId: {3}",
                        new Object[]{joystick.getName(), joystickAxis.getName(), joystickAxis.getLogicalId(), joystickAxis.getAxisId()});
                }

                // Get specific axis based on LogicalId of the JoystickAxis
                // If found, map axis
                axis = joystick.getAxis(SensorJoystickAxis.ORIENTATION_X);
                if (axis != null) {
                    axis.assignAxis(ORIENTATION_X_PLUS, ORIENTATION_X_MINUS);
                    inputManager.addListener(this, ORIENTATION_X_PLUS, ORIENTATION_X_MINUS);
                    logger.log(Level.INFO, "Found {0} Joystick, assigning mapping for X axis: {1}, with max value: {2}",
                            new Object[]{joystick.toString(), axis.toString(), ((SensorJoystickAxis) axis).getMaxRawValue()});
                }

                axis = joystick.getAxis(SensorJoystickAxis.ORIENTATION_Y);
                if (axis != null) {
                    axis.assignAxis(ORIENTATION_Y_PLUS, ORIENTATION_Y_MINUS);
                    inputManager.addListener(this, ORIENTATION_Y_PLUS, ORIENTATION_Y_MINUS);
                    logger.log(Level.INFO, "Found {0} Joystick, assigning mapping for Y axis: {1}, with max value: {2}",
                            new Object[]{joystick.toString(), axis.toString(), ((SensorJoystickAxis) axis).getMaxRawValue()});
                }

                axis = joystick.getAxis(SensorJoystickAxis.ORIENTATION_Z);
                if (axis != null) {
                    axis.assignAxis(ORIENTATION_Z_PLUS, ORIENTATION_Z_MINUS);
                    inputManager.addListener(this, ORIENTATION_Z_PLUS, ORIENTATION_Z_MINUS);
                    logger.log(Level.INFO, "Found {0} Joystick, assigning mapping for Z axis: {1}, with max value: {2}",
                            new Object[]{joystick.toString(), axis.toString(), ((SensorJoystickAxis) axis).getMaxRawValue()});
                }

                joystickMap.put(joystick.getJoyId(), joystick);

            }
        }
    }


    @Override
    public void simpleUpdate(float tpf) {
        if (!initialCalibrationComplete) {
            // Calibrate the axis (set new zero position) if the axis
            // is a sensor joystick axis
            for (IntMap.Entry<Joystick> entry : joystickMap) {
                for (JoystickAxis axis : entry.getValue().getAxes()) {
                    if (axis instanceof SensorJoystickAxis) {
                        logger.log(Level.INFO, "Calibrating Axis: {0}", axis.toString());
                        ((SensorJoystickAxis) axis).calibrateCenter();
                    }
                }
            }
            initialCalibrationComplete = true;
        }

        if (enableGeometryRotation) {
            rotationQuat.fromAngles(anglesCurrent);
            rotationQuat.normalizeLocal();

            if (useAbsolute) {
                geomZero.setLocalRotation(rotationQuat);
            } else {
                geomZero.rotate(rotationQuat);
            }

            anglesCurrent[0] = anglesCurrent[1] = anglesCurrent[2] = 0f;
        }
    }

    @Override
    public void onAction(String string, boolean pressed, float tpf) {
       if (string.equalsIgnoreCase("MouseClick") && pressed) {
            // Calibrate the axis (set new zero position) if the axis
            // is a sensor joystick axis
            for (IntMap.Entry<Joystick> entry : joystickMap) {
                for (JoystickAxis axis : entry.getValue().getAxes()) {
                    if (axis instanceof SensorJoystickAxis) {
                        logger.log(Level.INFO, "Calibrating Axis: {0}", axis.toString());
                        ((SensorJoystickAxis) axis).calibrateCenter();
                    }
                }
            }

            if (enableRumble) {
                // manipulate joystick rumble
                for (IntMap.Entry<Joystick> entry : joystickMap) {
                    rumbleAmount += 0.1f;
                    if (rumbleAmount > 1f + FastMath.ZERO_TOLERANCE) {
                        rumbleAmount = 0f;
                    }
                    logger.log(Level.INFO, "rumbling with amount: {0}", rumbleAmount);
                    entry.getValue().rumble(rumbleAmount);
                }
            }
        }
    }

    @Override
    public void onAnalog(String string, float value, float tpf) {
        logger.log(Level.INFO, "onAnalog for {0}, value: {1}, tpf: {2}",
                new Object[]{string, value, tpf});
        float scaledValue = value;

        if (string.equalsIgnoreCase(ORIENTATION_X_PLUS)) {
            if (useAbsolute) {
                // set rotation amount
                // divide by tpf to get back to actual axis value (0 to 1)
                // multiply by 90deg so that 90deg = full axis (value = tpf)
                anglesCurrent[0] = (scaledValue / tpf * FastMath.HALF_PI);
            } else {
                // apply an incremental rotation amount based on rotationSpeed
                anglesCurrent[0] += scaledValue * rotationSpeedX;
            }
        }

        if (string.equalsIgnoreCase(ORIENTATION_X_MINUS)) {
            if (useAbsolute) {
                // set rotation amount
                // divide by tpf to get back to actual axis value (0 to 1)
                // multiply by 90deg so that 90deg = full axis (value = tpf)
                anglesCurrent[0] = (-scaledValue / tpf * FastMath.HALF_PI);
            } else {
                // apply an incremental rotation amount based on rotationSpeed
                anglesCurrent[0] -= scaledValue * rotationSpeedX;
            }
        }

        if (string.equalsIgnoreCase(ORIENTATION_Y_PLUS)) {
            if (useAbsolute) {
                // set rotation amount
                // divide by tpf to get back to actual axis value (0 to 1)
                // multiply by 90deg so that 90deg = full axis (value = tpf)
                anglesCurrent[1] = (scaledValue / tpf * FastMath.HALF_PI);
            } else {
                // apply an incremental rotation amount based on rotationSpeed
                anglesCurrent[1] += scaledValue * rotationSpeedY;
            }
        }

        if (string.equalsIgnoreCase(ORIENTATION_Y_MINUS)) {
            if (useAbsolute) {
                // set rotation amount
                // divide by tpf to get back to actual axis value (0 to 1)
                // multiply by 90deg so that 90deg = full axis (value = tpf)
                anglesCurrent[1] = (-scaledValue / tpf * FastMath.HALF_PI);
            } else {
                // apply an incremental rotation amount based on rotationSpeed
                anglesCurrent[1] -= scaledValue * rotationSpeedY;
            }
        }

    }
}