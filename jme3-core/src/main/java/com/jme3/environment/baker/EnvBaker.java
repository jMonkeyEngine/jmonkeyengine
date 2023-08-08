package com.jme3.environment.baker;

import java.util.function.Function;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;

/**
 * And environment baker. It bakes the environment.
 *
 * @author Riccardo Balbo
 */
public interface EnvBaker {
    /**
     * Bake the environment
     * @param scene The scene to bake
     * @param position The position of the camera
     * @param frustumNear The near frustum
     * @param frustumFar The far frustum
     * @param filter A filter to select which geometries to bake
     */
    public void bakeEnvironment(Spatial scene, Vector3f position, float frustumNear, float frustumFar, Function<Geometry, Boolean> filter);
    
    /**
     * Get the environment map
     * @return The environment map
     */
    public TextureCubeMap getEnvMap();

    /**
     * Clean the environment baker
     * This method should be called when the baker is no longer needed
     * It will clean up all the resources
     */
    public void clean();    
}