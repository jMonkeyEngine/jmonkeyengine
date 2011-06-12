package com.jme3.scene.plugins.blender.helpers;

/**
 * This class helps to import the special effects from blender file.
 * @author Marcin Roguski (Kaelthas)
 */
public class ParticlesHelper extends com.jme3.scene.plugins.blender.helpers.v249.ParticlesHelper {

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public ParticlesHelper(String blenderVersion) {
        super(blenderVersion);
    }
}
