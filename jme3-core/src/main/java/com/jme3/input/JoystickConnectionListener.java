package com.jme3.input;

/**
 * Listens for the state of a joystick connection.
 * @author jayfella
 */
public interface JoystickConnectionListener {

    /**
     * Occurs when a new joystick has been detected.
     * @param joystick the joystick that has been detected.
     */
    void onConnected(Joystick joystick);

    /**
     * Occurs when an existing joystick has been disconnected.
     * @param joystick the joystick that has been disconnected.
     */
    void onDisconnected(Joystick joystick);

}
