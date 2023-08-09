package com.jme3.environment;

import java.io.IOException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLGLEnvBakerLight;
import com.jme3.environment.baker.IBLHybridEnvBakerLight;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
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
    private final boolean USE_GL_IR = true;
    private static final Logger LOG = Logger.getLogger(EnvironmentProbeControl.class.getName());

    
    private AssetManager assetManager;
    private boolean bakeNeeded = true;
    private int envMapSize=256;
    private Spatial spatial;
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

    protected EnvironmentProbeControl() {
    }
    
    public EnvironmentProbeControl(AssetManager assetManager,int size) {     
        this.envMapSize = size;
        this.assetManager = assetManager;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
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
        if (bakeNeeded) {
            bakeNeeded = false;
            rebakeNow(rm);
        }
    }

    /**
     * Schedule a rebake of the environment map.
     */
    public void rebake() {
        bakeNeeded = true;
    }

    /**
     * Set the asset manager used to load the shaders needed for the baking
     * @param assetManager
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    void rebakeNow(RenderManager renderManager) {
        if (assetManager == null) {
            LOG.log(Level.SEVERE, "AssetManager is null, cannot bake environment. Please use setAssetManager() to set it.");
            return;            
        }
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


    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(envMapSize, "size", 256);
        oc.write(serializable, "serializable", false);
        oc.write(bakeNeeded, "bakeNeeded", true);
    }
    
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        envMapSize = ic.readInt("size", 256);
        serializable = ic.readBoolean("serializable", false);
        bakeNeeded = ic.readBoolean("bakeNeeded", true);
        assetManager = im.getAssetManager();
    }

}