package com.jme3.environment.baker;

import com.jme3.math.Vector3f;
import com.jme3.texture.TextureCubeMap;

/**
 * An environment baker for IBL, that uses spherical harmonics for irradiance.
 *
 * @author Riccardo Balbo
 */
public interface IBLEnvBakerLight extends EnvBaker{   
    public void bakeSpecularIBL();
    public void bakeSphericalHarmonicsCoefficients();

    public TextureCubeMap getSpecularIBL();
    public Vector3f[] getSphericalHarmonicsCoefficients();
}