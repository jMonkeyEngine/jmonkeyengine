package com.jme3.environment.baker;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;

/**
 * And environment baker. It bakes the environment. ( ͡° ͜ʖ ͡°)
 *
 * @author Riccardo Balbo
 */
public interface EnvBaker {
    public void bakeEnvironment(Spatial scene, Vector3f position, float frustumNear, float frustumFar);
    public TextureCubeMap getEnvMap();
    public void clean();    
}