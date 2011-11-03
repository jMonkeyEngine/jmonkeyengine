package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.light.DirectionalLight;
import com.jme3.scene.Mesh;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.util.TangentBinormalGenerator;

/**
 *
 * @author Kirusha
 */
public class TestTangentGenBadModels extends SimpleApplication {
    
    float angle;
    PointLight pl;
    Geometry lightMdl;

    public static void main(String[] args){
        TestTangentGenBadModels app = new TestTangentGenBadModels();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("http://jme-glsl-shaders.googlecode.com/hg/assets/Models/LightBlow/", UrlLocator.class);
        assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/", UrlLocator.class);
        
        Geometry badModel = (Geometry) assetManager.loadModel("jme_lightblow.obj");
        badModel.setLocalScale(2f);
        
//        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        mat.setTexture("NormalMap", assetManager.loadTexture("jme_lightblow_nor.png"));
        Material mat = assetManager.loadMaterial("Textures/BumpMapTest/Tangent.j3m");
        badModel.setMaterial(mat);
        rootNode.attachChild(badModel);
        
        // TODO: For some reason blender loader fails to load this.
        // need to check it
//        Spatial model = assetManager.loadModel("test.blend");
//        rootNode.attachChild(model);

        rootNode.depthFirstTraversal(new SceneGraphVisitorAdapter(){
            @Override
            public void visit(Geometry g){
                Mesh m = g.getMesh();
                Material mat = g.getMaterial();
                
//                if (mat.getParam("DiffuseMap") != null){
//                    mat.setTexture("DiffuseMap", null);
//                }
                TangentBinormalGenerator.generate(m);
            }
        });

        Geometry debug = new Geometry(
                "Debug Teapot",
                TangentBinormalGenerator.genTbnLines(((Geometry) badModel).getMesh(), 0.03f)
        );
        debug.getMesh().setLineWidth(3);
        
        Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        debug.setMaterial(debugMat);
        debug.setCullHint(Spatial.CullHint.Never);
        debug.getLocalTranslation().set(badModel.getLocalTranslation());
        debug.getLocalScale().set(badModel.getLocalScale());
        rootNode.attachChild(debug);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.8f, -0.6f, -0.08f).normalizeLocal());
        dl.setColor(new ColorRGBA(1,1,1,1));
        rootNode.addLight(dl);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.getMesh().setStatic();
        rootNode.attachChild(lightMdl);

        pl = new PointLight();
        pl.setColor(ColorRGBA.White);
//        rootNode.addLight(pl);
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf;
        angle %= FastMath.TWO_PI;
        
        pl.setPosition(new Vector3f(FastMath.cos(angle) * 2f, 2f, FastMath.sin(angle) * 2f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }
    
    
}
