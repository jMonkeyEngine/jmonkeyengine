package com.jme3.scene.plugins.blender.helpers;

import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.DataRepository;

/**
 * This class should be used for constraint calculations.
 * @author Marcin Roguski
 */
public class ConstraintHelper extends com.jme3.scene.plugins.blender.helpers.v249.ConstraintHelper {

    private static final Logger LOGGER = Logger.getLogger(ConstraintHelper.class.getName());

    /**
     * Helper constructor. It's main task is to generate the affection functions. These functions are common to all
     * ConstraintHelper instances. Unfortunately this constructor might grow large. If it becomes too large - I shall
     * consider refactoring. The constructor parses the given blender version and stores the result. Some
     * functionalities may differ in different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public ConstraintHelper(String blenderVersion, DataRepository dataRepository) {
        super(blenderVersion, dataRepository);
    }

    @Override
    public void loadConstraints(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
        if (blenderVersion < 250) {
            super.loadConstraints(objectStructure, dataRepository);
        } else {
            LOGGER.warning("Loading of constraints not yet implemented for version 2.5x !");
            //TODO: to implement
        }
    }
}
