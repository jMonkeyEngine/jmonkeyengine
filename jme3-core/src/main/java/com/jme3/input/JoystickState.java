package com.jme3.input;

/**
 * Response for joystick callback events.
 * @author jayfella
 */
public enum JoystickState {

    // a list of connected/disconnected codes from various contexts.

    // using the JoystickState.fromCode(int) method, if the code matches
    // it will return the enum value.

    CONNECTED(new int[] {
            0x40001 // GLFW.GLFW_CONNECTED / LWJGL3
    }),

    DISCONNECTED(new int[] {
            0x40002 // GLFW.GLFW_DISCONNECTED / LWJGL3
    }),

    UNKNOWN(new int[0]);

    private int[] codes;

    JoystickState(int[] codes) {
        this.codes = codes;
    }

    private int[] getCodes() {
        return codes;
    }

    public static JoystickState fromCode(int value) {

        for (JoystickState state : values()) {
            for (int code : state.getCodes()) {
                if (value == code) {
                    return state;
                }
            }
        }

        return UNKNOWN;
    }

}
