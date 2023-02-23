package com.jme3.input.vr;

public class AnalogActionState{

    /**
     * The X coordinate of the analog data (typically between -1 and 1 for joystick coordinates or 0 and 1 for
     * trigger pulls)
     */
    public final float x;

    /**
     * The Y coordinate of the analog data (typically between -1 and 1)
     *
     * Will be zero if the analog action doesn't have at least 2 dimensions
     */
    public final float y;

    /**
     * The Z coordinate of the analog data (typically between -1 and 1)
     *
     * Will be zero if the analog action doesn't have at least 3 dimensions
     */
    public final float z;

    /**
     * The change in the X coordinate since the last frame
     */
    public final float deltaX;

    /**
     * The change in the Y coordinate since the last frame
     */
    public final float deltaY;

    /**
     * The change in the Z coordinate since the last frame
     */
    public final float deltaZ;

    public AnalogActionState(float x, float y, float z, float deltaX, float deltaY, float deltaZ){
        this.x = x;
        this.y = y;
        this.z = z;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.deltaZ = deltaZ;
    }
}
