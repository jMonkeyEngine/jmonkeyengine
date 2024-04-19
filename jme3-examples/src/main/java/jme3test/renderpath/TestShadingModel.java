package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ToneMapFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.MatRenderParam;
import com.jme3.renderer.framegraph.MyFrameGraph;
import com.jme3.renderer.framegraph.RenderPipelineFactory;
import com.jme3.renderer.pass.GBufferModule;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shader.VarType;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * This example demonstrates unified handling of several built-in shading models under the same render path.
 * @author JohnKkk
 */
public class TestShadingModel extends SimpleApplication {
    private DirectionalLight dl;

    private float roughness = 0.0f;

    private Node modelNode;
    private int frame = 0;
    private Material pbrMat;
    private Geometry model;
    private Node tex;

    @Override
    public void simpleInitApp() {
        
        MyFrameGraph graph = RenderPipelineFactory.create(this, RenderManager.RenderPath.Deferred);
        renderManager.setFrameGraph(graph);
        
        Geometry debugView = new Geometry("debug", new Quad(200, 200));
        debugView.setLocalTranslation(0, 200, 0);
        Material debugMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        debugView.setMaterial(debugMat);
        MatRenderParam texParam = new MatRenderParam("ColorMap", debugMat, VarType.Texture2D);
        //texParam.enableDebug();
        graph.bindToOutput(GBufferModule.RENDER_TARGETS[3], texParam);
        guiNode.attachChild(debugView);
        
        // UNLIT
        Material unlitMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        unlitMat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
        Sphere sp = new Sphere(15, 15, 1.0f);
        Geometry unlitSphere = new Geometry("unlitSphere", sp);
        unlitSphere.setLocalTranslation(-5, 0, 0);
        unlitSphere.setLocalRotation(new Quaternion(new float[]{(float) Math.toRadians(-90), 0, 0}));
        unlitSphere.setMaterial(unlitMat);
        rootNode.attachChild(unlitSphere);

        // LEGACY_LIGHTING
        Geometry lightSphere = unlitSphere.clone(false);
        TangentBinormalGenerator.generate(lightSphere.getMesh());
        Material lightMat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        lightSphere.setLocalTranslation(5, 0, 0);
        lightSphere.setMaterial(lightMat);
        rootNode.attachChild(lightSphere);

        // STANDARD_LIGHTING
        roughness = 1.0f;
        assetManager.registerLoader(KTXLoader.class, "ktx");

        viewPort.setBackgroundColor(ColorRGBA.White);
        modelNode = new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        modelNode.setLocalScale(0.3f);
        rootNode.attachChild(modelNode);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

//        fpp.addFilter(new FXAAFilter());
        fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(1.0f)));
//        fpp.addFilter(new SSAOFilter(0.5f, 3, 0.2f, 0.2f));
        viewPort.addProcessor(fpp);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);


        final EnvironmentCamera envCam = new EnvironmentCamera(256, new Vector3f(0, 3f, 0));
        stateManager.attach(envCam);

        //new RenderPathHelper(this);
        flyCam.setMoveSpeed(10.0f);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        super.simpleRender(rm);
        frame++;

        if (frame == 2) {
            modelNode.removeFromParent();
            final LightProbe probe = LightProbeFactory.makeProbe(stateManager.getState(EnvironmentCamera.class), rootNode, new JobProgressAdapter<LightProbe>() {

                @Override
                public void done(LightProbe result) {
                    System.err.println("Done rendering env maps");
                    tex = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(result.getPrefilteredEnvMap(), assetManager);
                }
            });
            probe.getArea().setRadius(100);
            rootNode.addLight(probe);
            //getStateManager().getState(EnvironmentManager.class).addEnvProbe(probe);

        }
        if (frame > 10 && modelNode.getParent() == null) {
            rootNode.attachChild(modelNode);
        }
    }

    public static void main(String[] args) {
        TestShadingModel testShadingModel = new TestShadingModel();
        testShadingModel.start();
    }
}
