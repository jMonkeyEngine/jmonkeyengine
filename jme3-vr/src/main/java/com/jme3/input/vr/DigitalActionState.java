package com.jme3.input.vr;

/**
 * @deprecated The jme3-vr module is deprecated and will be removed in a future version (as it only supports OpenVR).
 *             For new Virtual Reality projects, use user libraries that provide OpenXR support.
 *             See <a href = "https://wiki.jmonkeyengine.org/docs/3.4/core/vr/virtualreality.html">Virtual Reality JME wiki section</a>
 *             for more information.
 */
@Deprecated
public class DigitalActionState{

    /**
     * The current value of this action
     */
    public final boolean state;

    /**
     * If since the last loop the value of this action has changed
     */
    public final boolean changed;

    public DigitalActionState(boolean state, boolean changed){
        this.state = state;
        this.changed = changed;
    }
}
