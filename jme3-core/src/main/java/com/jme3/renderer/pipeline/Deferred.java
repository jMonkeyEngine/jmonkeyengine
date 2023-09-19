package com.jme3.renderer.pipeline;

import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.math.FastMath;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.VpStep;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.scene.shape.Box;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

import java.util.ArrayList;
import java.util.List;

public class Deferred extends RenderPipeline{
    private static AssetManager assetManager;

    public final static String S_CONTEXT_InGBUFF_0 = "Context_InGBuff0";
    public final static String S_CONTEXT_InGBUFF_1 = "Context_InGBuff1";
    public final static String S_CONTEXT_InGBUFF_2 = "Context_InGBuff2";
    public final static String S_CONTEXT_InGBUFF_3 = "Context_InGBuff3";
    public final static String S_CONTEXT_InGBUFF_4 = "Context_InGBuff4";
    public final static String S_GBUFFER_PASS = "GBufferPass";
    public final static String S_DEFERRED_PASS = "DeferredPass";
    public final static String S_LIGHT_CULL_DRAW_STAGE = "Light_Cull_Draw_Stage";


    private FrameBuffer gBuffer;
    private Texture2D gBufferData0;
    private Texture2D gBufferData1;
    private Texture2D gBufferData2;
    private Texture2D gBufferData3;
    private Texture2D gBufferData4;
    private Material fsMat;
    private Picture fsQuad;
    private InstancedGeometry fsPointLightsCullShapes;
    private InstancedNode fsPointLightsCullInstancedNode;
    private boolean reshape;
    private boolean drawing;
    // todo:由于ADDITIVE_LIGHT会使得lightCull的后续绘制出现问题,所以暂时关闭
    private boolean enablePointLightsCull = false;
    private final List<Light> tempLights = new ArrayList<Light>();
    private final LightList fullScreenLightList = new LightList(null);
    private final LightList notFullScreenLightList = new LightList(null);

    public Deferred(TechniqueDef.Pipeline pipeline) {
        super(pipeline);
        initAssetManager();
        MaterialDef def = (MaterialDef) assetManager.loadAsset("Common/MatDefs/ShadingCommon/DeferredShading.j3md");
        fsMat = new Material(def);
    }

    private static void initAssetManager(){
        assetManager = JmeSystem.newAssetManager();
        assetManager.registerLocator(".", FileLocator.class);
        assetManager.registerLocator("/", ClasspathLocator.class);
        assetManager.registerLoader(J3MLoader.class, "j3m");
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLoader(GLSLLoader.class, "vert", "frag","geom","tsctrl","tseval","glsllib","glsl");
    }

    @Override
    public void begin(RenderManager rm, ViewPort vp) {
        // Make sure to create gBuffer only when needed
        if(gBuffer == null){
            fsQuad = new Picture("filter full screen quad");
            fsQuad.setWidth(1);
            fsQuad.setHeight(1);
            reshape(vp.getCamera().getWidth(), vp.getCamera().getHeight());
            fsQuad.setMaterial(fsMat);

            // pointLights cull
            fsPointLightsCullInstancedNode = new InstancedNode("box_point_light_cull");
            Geometry boxGeo = new Geometry("point_light_cull_box", new Box(1, 1, 1));
            fsMat.setBoolean("UseInstancing", true);
            boxGeo.setMaterial(fsMat);
            int num = 1024;
            for(int i = 0;i < num;i++){
                Geometry b = boxGeo.clone(false);
                fsPointLightsCullInstancedNode.attachChild(b);
            }
            fsPointLightsCullInstancedNode.instance();
            for(int i = 0,size = fsPointLightsCullInstancedNode.getChildren().size();i < size;i++){
                if(fsPointLightsCullInstancedNode.getChild(i) instanceof InstancedGeometry){
                    fsPointLightsCullShapes = (InstancedGeometry)fsPointLightsCullInstancedNode.getChild(i);
                    fsPointLightsCullShapes.setForceNumVisibleInstances(0);
                    fsPointLightsCullShapes.setUserData(S_LIGHT_CULL_DRAW_STAGE, true);
                    break;
                }
            }
        }
        tempLights.clear();
        fullScreenLightList.clear();
        notFullScreenLightList.clear();
    }

    @Override
    public void reshape(int w, int h) {
        gBufferData0 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData1 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData2 = new Texture2D(w, h, Image.Format.RGBA16F);
        gBufferData3 = new Texture2D(w, h, Image.Format.RGBA32F);   // The third buffer provides 32-bit floating point to store high-precision information, such as normals
        // todo:后续调整为Depth24Stencil8,然后使用一个SceneColorFBO用于渲染所有3D部分,然后将其color_attach_0复制到BackBuffer中
        // todo:然后开启DepthTest绘制最后的所有GUI
        gBufferData4 = new Texture2D(w, h, Image.Format.Depth);
        gBuffer = new FrameBuffer(w, h, 1);
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData0));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData1));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData2));
        gBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData3));
        gBuffer.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(gBufferData4));
        gBuffer.setMultiTarget(true);
        reshape = true;
    }

    @Override
    public void draw(RenderManager rm, RenderQueue rq, ViewPort vp, boolean flush) {
        if (rq.isQueueEmpty(RenderQueue.Bucket.Opaque)) {
            drawing = false;
            return;
        }
//        drawing = true;
        Camera cam = vp.getCamera();
        Renderer renderer = rm.getRenderer();
        AppProfiler prof = rm.getAppProfiler();

        // render opaque objects with default depth range
        // opaque objects are sorted front-to-back, reducing overdraw
        if (prof!=null) prof.vpStep(VpStep.RenderBucket, vp, RenderQueue.Bucket.Opaque);

        // todo:后续在这里使用FrameGraph,FGNode内部使用SceneProcessor成员,然后使用:
        // Framegraph fg = new Framegraph();
        //
        //// 创建节点
        //FGNode gBufferPass = new FGNode();
        //FGNode deferredPass = new FGNode();
        //
        //// 添加节点到Framegraph
        //fg.addNode(gBufferPass);
        //fg.addNode(deferredPass);
        //
        //// 连接两个节点
        //fg.connect(gBufferPass, deferredPass);
        //
        //// 执行Framegraph
        //fg.execute(renderPass);
        // 这样一来,可以在gBufferPass中封装对gBufferPass阶段的绘制,并且可以gBufferPass节点中对shadingModel开启新的FGNode,比如shadingModel
        // 为SSS的着色,可以开启对该类物体进行multiPass(为此,gBufferPass需要对物体列表进行分类,然后按类别进行合并状态机绘制)
        // G-Buffer Pass
        FrameBuffer opfb = vp.getOutputFrameBuffer();
        vp.setOutputFrameBuffer(gBuffer);
        renderer.setFrameBuffer(gBuffer);
        renderer.clearBuffers(vp.isClearColor(), vp.isClearDepth(), vp.isClearStencil());
        String techOrig = rm.getForcedTechnique();
        rm.setForcedTechnique(S_GBUFFER_PASS);
        rq.renderQueue(RenderQueue.Bucket.Opaque, rm, cam, flush);
        rm.setForcedTechnique(techOrig);
        vp.setOutputFrameBuffer(opfb);
        renderer.setFrameBuffer(vp.getOutputFrameBuffer());

        // Deferred Pass
        if(fsMat != null){
            fsMat.selectTechnique(S_DEFERRED_PASS, rm);
            // Check context parameters
            MaterialDef matDef = fsMat.getMaterialDef();
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_0) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_0) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_0, gBufferData0);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_1) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_1) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_1, gBufferData1);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_2) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_2) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_2, gBufferData2);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_3) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_3) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_3, gBufferData3);
            }
            if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_4) != null && (reshape || fsMat.getTextureParam(S_CONTEXT_InGBUFF_4) == null)){
                fsMat.setTexture(S_CONTEXT_InGBUFF_4, gBufferData4);
            }
            PointLight pl = null;
            for(Light l : tempLights){
                if(enablePointLightsCull && (l instanceof PointLight)){
                    pl = (PointLight)l;
                    if(pl.getRadius() > 0){
                        notFullScreenLightList.add(l);
                        continue;
                    }
                }
                fullScreenLightList.add(l);
            }
            boolean depthWrite = fsMat.getAdditionalRenderState().isDepthWrite();
            RenderState.FaceCullMode faceCullMode = fsMat.getAdditionalRenderState().getFaceCullMode();
            fsMat.getAdditionalRenderState().setDepthWrite(false);
//            fsQuad.setMaterial(fsMat);
            if(fullScreenLightList.size() > 0){
                fsMat.setBoolean("UseLightsCullMode", false);
                fsQuad.updateGeometricState();
                fsMat.render(fsQuad, fullScreenLightList, rm);
            }
            if(enablePointLightsCull && notFullScreenLightList.size() > 0){
                int i = 0, plSize = notFullScreenLightList.size();
                for (Spatial instance : fsPointLightsCullInstancedNode.getChildren()) {
                    if (!(instance instanceof InstancedGeometry)) {
                        pl = (PointLight) notFullScreenLightList.get(i++);
                        instance.setLocalTranslation(pl.getPosition());
                        instance.setLocalScale(pl.getRadius() * 0.5f);
                        if(i >= plSize){
                            fsPointLightsCullShapes.setUserData(S_LIGHT_CULL_DRAW_STAGE, fullScreenLightList.size() != 0);
                            fsPointLightsCullShapes.setForceNumVisibleInstances(plSize);
//                            fsPointLightsCullInstancedNode.updateGeometricState();
                            fsPointLightsCullShapes.updateGeometricState();
                            fsPointLightsCullShapes.updateInstances();
                            fsMat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
                            fsMat.setBoolean("UseLightsCullMode", true);
                            fsMat.render(fsPointLightsCullShapes, notFullScreenLightList, rm);
                            fsMat.getAdditionalRenderState().setFaceCullMode(faceCullMode);
                            // todo:后续改善这里以便进行无限数量灯光
                            break;
                        }
                    }
                }
            }
    //        rm.renderGeometry(fsQuad);
            fsMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        }
        else{
            drawing = false;
        }

        reshape = false;
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null)return false;
//        // Check context parameters
//        MaterialDef matDef = material.getMaterialDef();
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_0) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_0) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_0, gBufferData0);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_1) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_1) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_1, gBufferData1);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_2) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_2) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_2, gBufferData2);
//        }
//        if(matDef.getMaterialParam(S_CONTEXT_InGBUFF_3) != null && (reshape || material.getTextureParam(S_CONTEXT_InGBUFF_3) == null)){
//            material.setTexture(S_CONTEXT_InGBUFF_3, gBufferData3);
//        }
        rm.renderGeometry(geom);
        if(material.getActiveTechnique() != null){
            // todo:应该使用一个统一的材质材质,其中根据lightModeId分开着色
            if(material.getMaterialDef().getTechniqueDefs(S_GBUFFER_PASS) != null || rm.joinPipeline(material.getActiveTechnique().getDef().getPipeline())){
//                if(fsMat == null){
//                    fsMat = material.clone();
//                    fsMat.selectTechnique(S_DEFERRED_PASS, rm);
//                }
                LightList lights = geom.getFilterWorldLights();
                for(Light light : lights){
                    if(!tempLights.contains(light)){
                        tempLights.add(light);
                    }
                }
                drawing = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void end(RenderManager rm, ViewPort vp) {
        if(drawing){
//            System.out.println("copy depth!");
            rm.getRenderer().copyFrameBuffer(gBuffer, vp.getOutputFrameBuffer(), false, true);
        }
    }
}
