package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.math.ColorRGBA;

public enum PixelSamenessDegree{
    SAME(1, null),
    SUBTLY_DIFFERENT(10, ColorRGBA.Blue),

    MEDIUMLY_DIFFERENT(20, ColorRGBA.Yellow),

    VERY_DIFFERENT(60,ColorRGBA.Orange),

    EXTREMELY_DIFFERENT(100,ColorRGBA.Orange);

    private final int maximumAllowedDifference;

    private final ColorRGBA colorInDebugImage;

    PixelSamenessDegree(int maximumAllowedDifference, ColorRGBA colorInDebugImage){
        this.colorInDebugImage = colorInDebugImage;
        this.maximumAllowedDifference = maximumAllowedDifference;
    }

    public ColorRGBA getColorInDebugImage(){
        return colorInDebugImage;
    }

    public int getMaximumAllowedDifference(){
        return maximumAllowedDifference;
    }
}
