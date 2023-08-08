package com.jme3.environment.baker;

import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;

/**
 * An environment baker, but this one is for Imaged Base Lighting
 *
 * @author Riccardo Balbo
 */
public interface IBLEnvBaker extends EnvBaker{
    public Texture2D genBRTF() ;
    
    public void bakeIrradiance();
    public void bakeSpecularIBL() ;

    public TextureCubeMap getSpecularIBL();
    public TextureCubeMap getIrradiance();
}