package com.jme3.scene.plugins.blender.helpers;

/**
 * A class that is used in mesh calculations.
 * @author Marcin Roguski
 */
public class CurvesHelper extends com.jme3.scene.plugins.blender.helpers.v249.CurvesHelper {

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public CurvesHelper(String blenderVersion) {
        super(blenderVersion);
    }
}
