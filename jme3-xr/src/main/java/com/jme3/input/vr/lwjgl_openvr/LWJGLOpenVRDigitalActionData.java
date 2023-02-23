package com.jme3.input.vr.lwjgl_openvr;

import org.lwjgl.openvr.InputDigitalActionData;

/**
 * This is a set of reusable parts that are used when accessing a digital action
 * (Digital meaning something like a button press, that is either on or off)
 */
public class LWJGLOpenVRDigitalActionData{

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
    InputDigitalActionData actionData;

    public LWJGLOpenVRDigitalActionData(String actionName, long actionHandle, InputDigitalActionData actionData){
        this.actionName = actionName;
        this.actionHandle = actionHandle;
        this.actionData = actionData;
    }
}
