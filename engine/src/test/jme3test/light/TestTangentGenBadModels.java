package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.*;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
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
//        assetManager.registerLocator("http://jme-glsl-shaders.googlecode.com/hg/assets/Models/LightBlow/", UrlLocator.class);
//        assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/", UrlLocator.class);
        
        final Spatial badModel = assetManager.loadModel("Models/TangentBugs/test.blend");
//        badModel.setLocalScale(1f);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setTexture("NormalMap", assetManager.loadTexture("Models/TangentBugs/test_normal.png"));
//        Material mat = assetManager.loadMaterial("Textures/BumpMapTest/Tangent.j3m");
        badModel.setMaterial(mat);
        rootNode.attachChild(badModel);
        
        // TODO: For some reason blender loader fails to load this.
        // need to check it
//        Spatial model = assetManager.loadModel("test.blend");
//        rootNode.attachChild(model);
        
        final Node debugTangents = new Node("debug tangents");
        debugTangents.setCullHint(CullHint.Always);
        rootNode.attachChild(debugTangents);

        final Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        
        badModel.depthFirstTraversal(new SceneGraphVisitorAdapter(){
            @Override
            public void visit(Geometry g){
                Mesh m = g.getMesh();
                Material mat = g.getMaterial();
                
//                if (mat.getParam("DiffuseMap") != null){
//                    mat.setTexture("DiffuseMap", null);
//                }
                TangentBinormalGenerator.generate(m);
                
                Geometry debug = new Geometry(
                    "debug tangents geom",
                    TangentBinormalGenerator.genTbnLines(g.getMesh(), 0.2f)
                );
                debug.getMesh().setLineWidth(1);
                debug.setMaterial(debugMat);
                debug.setCullHint(Spatial.CullHint.Never);
                debug.setLocalTransform(g.getWorldTransform());
                debugTangents.attachChild(debug);
            }
        });

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
        
        
        BitmapText info = new BitmapText(guiFont);
        info.setText("Press SPACE to switch between lighting and tangent display");
        info.setQueueBucket(Bucket.Gui);
        info.move(0, settings.getHeight() - info.getLineHeight(), 0);
        rootNode.attachChild(info);
        
        inputManager.addMapping("space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(new ActionListener() {
            
            private boolean isLit = true;
            
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) return;
                Material mat;
                if (isLit){
                    mat = assetManager.loadMaterial("Textures/BumpMapTest/Tangent.j3m");
                    debugTangents.setCullHint(CullHint.Inherit);
                }else{
                    mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                    mat.setTexture("NormalMap", assetManager.loadTexture("Models/TangentBugs/test_normal.png"));
                    debugTangents.setCullHint(CullHint.Always);
                }
                isLit = !isLit;
                badModel.setMaterial(mat);
            }
        }, "space");
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf;
        angle %= FastMath.TWO_PI;
        
        pl.setPosition(new Vector3f(FastMath.cos(angle) * 2f, 2f, FastMath.sin(angle) * 2f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }
    
    
}
