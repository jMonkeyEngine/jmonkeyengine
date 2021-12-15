package com.jme3.input.vr;

import com.jme3.app.VREnvironment;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.Vector2f;

/**
 * A class dedicated to the handling of the mouse within VR environment.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 */
public interface VRMouseManager {
  /**
   * Initialize the VR mouse manager.
   */
  public void initialize();

  /**
   * Get the {@link VREnvironment VR Environment} to which this manager is attached.
   * @return the {@link VREnvironment VR Environment} to which this manager is attached.
   */
  public VREnvironment getVREnvironment();

  /**
   * Set if the mouse cursor should be used in the VR view.
   * @param enabled <code>true</code> if the mouse cursor should be displayed in VR and <code>false</code> otherwise.
   */
  public void setVRMouseEnabled(boolean enabled);

  /**
   * Set if the VR device controller is used within thumb stick mode.
   * @param set <code>true</code> if the VR device controller is used within thumb stick mode and <code>false</code> otherwise.
   */
  public void setThumbstickMode(boolean set);

  /**
   * Get if the VR device controller is used within thumb stick mode.
   * @return <code>true</code> if the VR device controller is used within thumb stick mode and <code>false</code> otherwise.
   */
  public boolean isThumbstickMode();

  /**
   * Set the speed of the mouse.
   * @param sensitivity the sensitivity of the mouse.
   * @param acceleration the acceleration of the mouse.
   * @see #getSpeedAcceleration()
   * @see #getSpeedSensitivity()
   */
  public void setSpeed(float sensitivity, float acceleration);

  /**
   * Get the sensitivity of the mouse.
   * @return the sensitivity of the mouse.
   * @see #setSpeed(float, float)
   */
  public float getSpeedSensitivity();

  /**
   * Get the acceleration of the mouse.
   * @return the acceleration of the mouse.
   * @see #setSpeed(float, float)
   */
  public float getSpeedAcceleration();

  /**
   * Get the move scale.
   * return the move scale.
   * @see #setMouseMoveScale(float)
   */
  public float getMouseMoveScale();

  /**
   * Set the mouse move scale.
   * @param set the mouse move scale.
   * @see #getMouseMoveScale()
   */
  public void setMouseMoveScale(float set);

  /**
   * Set the image to use as mouse cursor. The given string describe an asset that the underlying application asset manager has to load.
   * @param texture the image to use as mouse cursor.
   */
  public void setImage(String texture);

  /**
   * Update analog controller as it was a mouse controller.
   * @param inputIndex the index of the controller attached to the VR system.
   * @param mouseListener the JMonkey mouse listener to trigger.
   * @param mouseXName the mouseX identifier.
   * @param mouseYName the mouseY identifier
   * @param tpf the time per frame.
   */
  public void updateAnalogAsMouse(int inputIndex, AnalogListener mouseListener, String mouseXName, String mouseYName, float tpf);

  /**
   * Get the actual cursor position.
   * @return the actual cursor position.
   */
  public Vector2f getCursorPosition();

  /**
   * Center the mouse on the display.
   */
  public void centerMouse();

  /**
   * Update the mouse manager. This method should not be called manually.
   * The standard behavior for this method is to be called from the {@link VRViewManager#update(float) update method} of the attached {@link VRViewManager VR view manager}.
   * @param tpf the time per frame.
   */
  public void update(float tpf);
}
