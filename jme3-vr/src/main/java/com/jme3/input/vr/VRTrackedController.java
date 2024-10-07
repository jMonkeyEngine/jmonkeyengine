package com.jme3.input.vr;

import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * TODO
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public interface VRTrackedController {
    /**
     * Get the controller name.
     * @return the controller name.
     */
    public String getControllerName();

    /**
     * Get the controller manufacturer.
     * @return the controller manufacturer.
     */
    public String getControllerManufacturer();

    /**
     * Get the position of the tracked device. This value is the translation component of the device {@link #getPose() pose}.
     * @return the position of the tracked device.
     * @see #getOrientation()
     * @see #getPose()
     */
    public Vector3f getPosition();

    /**
     * Get the orientation of the tracked device. This value is the rotation component of the device {@link #getPose() pose}.
     * @return the orientation of the tracked device.
     * @see #getPosition()
     * @see #getPose()
     */
    public Quaternion getOrientation();

    /**
     * Get the pose of the tracked device.
     * The pose is a 4x4 matrix than combine the {@link #getPosition() position} and the {@link #getOrientation() orientation} of the device.
     * @return the pose of the tracked device.
     * @see #getPosition()
     * @see #getOrientation()
     */
    public Matrix4f getPose();
}
