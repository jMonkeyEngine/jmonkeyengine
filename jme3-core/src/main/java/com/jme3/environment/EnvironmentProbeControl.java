package com.jme3.environment;

import java.util.function.Function;
import java.util.function.Predicate;

import com.jme3.asset.AssetManager;
import com.jme3.environment.baker.IBLGLEnvBakerLight;
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
 * SmartLightProbe
 */
public class EnvironmentProbeControl extends LightProbe implements Control {



    RenderManager renderManager;
    AssetManager assetManager;
    int envMapSize;
    Spatial spatial;
    boolean BAKE_NEEDED=true;
    Function<Geometry,Boolean> filter=(s)->{
        return s.getUserData("tags.env")!=null;
    };

    public static void tag(Spatial s){
        if(s instanceof Node){
            Node n=(Node)s;
            for(Spatial sx:n.getChildren()){
                tag(sx);
            }
        }else if(s instanceof Geometry){
            s.setUserData("tags.env", true);
        }
    }

    public EnvironmentProbeControl(RenderManager rm,AssetManager am, int size){
        renderManager=rm;
        assetManager=am;
        envMapSize=size;
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        return null;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        
        spatial.addLight(this);
        this.spatial=spatial;

    }

    @Override
    public void update(float tpf) {

    }

    @Override
	public void render(RenderManager rm, ViewPort vp) {
        if(BAKE_NEEDED){
            BAKE_NEEDED=false;
            rebakeNow();
        }
    }

    public void rebake(){
        BAKE_NEEDED=true;
    }
    
    void rebakeNow() {
        System.out.println("BAKE");

        IBLGLEnvBakerLight baker = new IBLGLEnvBakerLight(renderManager, assetManager, Format.RGB16F, Format.Depth,
                envMapSize, envMapSize);
        
       
        baker.bakeEnvironment(spatial, Vector3f.ZERO, 0.001f, 1000f,filter);
        baker.bakeSpecularIBL();
        baker.bakeSphericalHarmonicsCoefficients();


        // probe.setPosition(Vector3f.ZERO);
        setPrefilteredMap(baker.getSpecularIBL());
        setNbMipMaps(getPrefilteredEnvMap().getImage().getMipMapSizes().length);
        setShCoeffs(baker.getSphericalHarmonicsCoefficients());
        setPosition(Vector3f.ZERO);
        setReady(true);

        baker.clean();

    }
    
}