package com.jme3.input;

public interface JoystickConnectionListener {

    void connectionChanged(int joystickId, JoystickState action);

}
