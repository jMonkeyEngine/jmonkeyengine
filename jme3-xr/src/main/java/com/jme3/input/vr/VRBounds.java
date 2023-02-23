package com.jme3.input.vr;

import com.jme3.math.Vector2f;

/**
 * This interface describes the VR playground bounds.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 *
 */
public interface VRBounds {

    /**
     * Get the size of the VR playground.
     * @return the size of the VR playground.
     */
    public Vector2f getPlaySize();
}
