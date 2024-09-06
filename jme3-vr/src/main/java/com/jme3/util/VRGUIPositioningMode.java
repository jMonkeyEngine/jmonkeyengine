package com.jme3.util;

/**
 * An enumeration that describes the GUI display positioning modes.
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 *
 * @deprecated The jme3-vr module is deprecated and will be removed in a future version (as it only supports OpenVR).
 *             For new Virtual Reality projects, use user libraries that provide OpenXR support.
 *             See <a href = "https://wiki.jmonkeyengine.org/docs/3.4/core/vr/virtualreality.html">Virtual Reality JME wiki section</a>
 *             for more information.
 */
@Deprecated
public enum VRGUIPositioningMode {
    MANUAL,
    AUTO_CAM_ALL,
    AUTO_CAM_ALL_SKIP_PITCH,
    AUTO_OBSERVER_POS_CAM_ROTATION,
    AUTO_OBSERVER_ALL,
    AUTO_OBSERVER_ALL_CAMHEIGHT
}
