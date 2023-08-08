package com.jme3.environment;

import java.util.function.Function;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLGLEnvBakerLight;
import com.jme3.environment.baker.IBLHybridEnvBakerLight;
import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Image.Format;

/**
 * A control that automatically handles environment bake and rebake including
 * only tagged spatials.
 * 
 * @author Riccardo Balbo
 */
public class EnvironmentProbeControl extends LightProbe implements Control {

    private RenderManager renderManager;
    private AssetManager assetManager;
    private int envMapSize;
    private Spatial spatial;
    private boolean BAKE_NEEDED = true;
    private boolean USE_GL_IR = true;
    private boolean serializable = false;

    private Function<Geometry, Boolean> filter = (s) -> {
        return s.getUserData("tags.env") != null;
    };

    /**
     * Tag spatial as part of the environment. Only tagged spatials will be
     * rendered in the environment map.
     * 
     * @param s
     *            the spatial
     */
    public static void tag(Spatial s) {
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sx : n.getChildren()) {
                tag(sx);
            }
        } else if (s instanceof Geometry) {
            s.setUserData("tags.env", true);
        }
    }

    public EnvironmentProbeControl(RenderManager rm, AssetManager am, int size) {
        renderManager = rm;
        assetManager = am;
        envMapSize = size;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return null;
    }

    public void setSerializeBakeResults(boolean v) {
        serializable = v;
    }

    public boolean isSerializeBakeResults() {
        return serializable;
    }

    @Override
    public void setSpatial(Spatial spatial) {

        spatial.addLight(this);
        this.spatial = spatial;

    }

    @Override
    public void update(float tpf) {

    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        if (BAKE_NEEDED) {
            BAKE_NEEDED = false;
            rebakeNow();
        }
    }

    /**
     * Schedule a rebake of the environment map.
     */
    public void rebake() {
        BAKE_NEEDED = true;
    }

    void rebakeNow() {

        IBLHybridEnvBakerLight baker;
        if(!USE_GL_IR){
            baker = new IBLHybridEnvBakerLight(renderManager, assetManager, Format.RGB16F, Format.Depth, envMapSize, envMapSize);
        } else {
            baker = new IBLGLEnvBakerLight(renderManager, assetManager, Format.RGB16F, Format.Depth, envMapSize, envMapSize);
        }
        baker.setTexturePulling(isSerializeBakeResults());

        baker.bakeEnvironment(spatial, Vector3f.ZERO, 0.001f, 1000f, filter);
        baker.bakeSpecularIBL();
        baker.bakeSphericalHarmonicsCoefficients();

        setPrefilteredMap(baker.getSpecularIBL());
        setNbMipMaps(getPrefilteredEnvMap().getImage().getMipMapSizes().length);
        setShCoeffs(baker.getSphericalHarmonicsCoefficients());
        setPosition(Vector3f.ZERO);
        setReady(true);

        baker.clean();

    }

}