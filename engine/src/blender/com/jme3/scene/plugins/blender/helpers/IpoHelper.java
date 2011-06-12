package com.jme3.scene.plugins.blender.helpers;

/**
 * This class helps to compute values from interpolation curves for features like animation or constraint influence. The
 * curves are 3rd degree bezier curves.
 * @author Marcin Roguski
 */
public class IpoHelper extends com.jme3.scene.plugins.blender.helpers.v249.IpoHelper {

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public IpoHelper(String blenderVersion) {
        super(blenderVersion);
    }
}
