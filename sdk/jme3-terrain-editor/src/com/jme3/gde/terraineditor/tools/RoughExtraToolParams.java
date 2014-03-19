/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.terraineditor.tools;

import com.jme3.gde.terraineditor.ExtraToolParams;

/**
 * Parameters, and default values, for the fractal roughen tool
 * @author Brent Owens
 */
public class RoughExtraToolParams implements ExtraToolParams{
    public float roughness = 1.2f;
    public float frequency = 0.2f;
    public float amplitude = 1.0f;
    public float lacunarity = 2.12f;
    public float octaves = 8;
    public float scale = 1.0f;
    
    
    // the below parameters are not get in the UI yet:
    
    float perturbMagnitude = 0.2f;
    float erodeRadius = 5;
    float erodeTalus = 0.011f;
    
    float smoothRadius = 1;
    float smoothEffect = 0.1f;
    
    int iterations = 1;
}
