package com.jme3.math;

/**
 * Created by Nehon on 26/03/2017.
 */
public interface EaseFunction {

    /**
     * @param value a value from 0 to 1. Passing a value out of this range will have unexpected behavior.
     * @return
     */
    float apply(float value);
}
