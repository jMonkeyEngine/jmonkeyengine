package com.jme3.input.vr.lwjgl_openvr;

import org.lwjgl.openvr.InputAnalogActionData;

/**
 * This is a set of reusable parts that are used when accessing an analogue action
 * (Analogue meaning something like a trigger pull or joystick coordinate)
 */
public class LWJGLOpenVRAnalogActionData{

    /**
     * This is the address string for the action. It will be something like /actions/main/in/openInventory
     */
    String actionName;

    /**
     * The handle used to request the action's state from LWJGL.
     *
     * It is how the action is addressed efficiently
     */
    long actionHandle;

    /**
     * This is a LWJGL object that will have the actions state passed into it. It is mapped to native memory so we
     * don't want to keep creating new ones.
     */
    InputAnalogActionData actionData;

    public LWJGLOpenVRAnalogActionData(String actionName, long actionHandle, InputAnalogActionData actionData){
        this.actionName = actionName;
        this.actionHandle = actionHandle;
        this.actionData = actionData;
    }
}
