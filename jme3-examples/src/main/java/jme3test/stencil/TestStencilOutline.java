package jme3test.stencil;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TestStencilOutline extends SimpleApplication {

    class OutlineComparator extends OpaqueComparator {
        @Override
        public int compare(Geometry o1, Geometry o2) {
            boolean ol1 = o1.getUserData("Outline") != null;
            boolean ol2 = o2.getUserData("Outline") != null;
            if (ol1 == ol2) {
                return super.compare(o1, o2);
            } else {
                if (ol1) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

    class OutlineControl extends AbstractControl {
        private Material outlineMaterial;

        public OutlineControl(AssetManager assetManager, ColorRGBA colorRGBA) {
            outlineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            outlineMaterial.setColor("Color", colorRGBA);
            outlineMaterial.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
            outlineMaterial.getAdditionalRenderState().setDepthFunc(RenderState.TestFunction.Always);
            outlineMaterial.getAdditionalRenderState().setStencil(true,
                    RenderState.StencilOperation.Keep, //front triangle fails stencil test
                    RenderState.StencilOperation.Keep, //front triangle fails depth test
                    RenderState.StencilOperation.Keep, //front triangle passes depth test
                    RenderState.StencilOperation.Keep, //back triangle fails stencil test
                    RenderState.StencilOperation.Keep, //back triangle fails depth test
                    RenderState.StencilOperation.Keep, //back triangle passes depth test
                    RenderState.TestFunction.NotEqual, //front triangle stencil test function
                    RenderState.TestFunction.NotEqual);    //back triangle stencil test function
            outlineMaterial.getAdditionalRenderState().setFrontStencilReference(1);
            outlineMaterial.getAdditionalRenderState().setBackStencilReference(1);
            outlineMaterial.getAdditionalRenderState().setFrontStencilMask(0xFF);
            outlineMaterial.getAdditionalRenderState().setBackStencilMask(0xFF);
        }

        @Override
        protected void controlUpdate(float v) {

        }

        @Override
        protected void controlRender(RenderManager renderManager, ViewPort viewPort) {
            if (spatial instanceof Geometry) {
                Geometry geometry= (Geometry) spatial;
                Geometry clone = geometry.clone();
                clone.scale(1.1f);
                clone.setUserData("Outline", true);
                clone.setMaterial(outlineMaterial);
                clone.updateGeometricState();
                viewPort.getQueue().addToQueue(clone, RenderQueue.Bucket.Opaque);
            }
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(24);
        flyCam.setZoomSpeed(-5);
        viewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new OutlineComparator());
        viewPort.setClearFlags(true,true,true);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.getAdditionalRenderState().setStencil(true,
                RenderState.StencilOperation.Keep, //front triangle fails stencil test
                RenderState.StencilOperation.Replace, //front triangle fails depth test
                RenderState.StencilOperation.Replace, //front triangle passes depth test
                RenderState.StencilOperation.Keep, //back triangle fails stencil test
                RenderState.StencilOperation.Replace, //back triangle fails depth test
                RenderState.StencilOperation.Replace, //back triangle passes depth test
                RenderState.TestFunction.Always, //front triangle stencil test function
                RenderState.TestFunction.Always);    //back triangle stencil test function
        boxMat.getAdditionalRenderState().setFrontStencilReference(1);
        boxMat.getAdditionalRenderState().setBackStencilReference(1);
        boxMat.getAdditionalRenderState().setFrontStencilMask(0xFF);
        boxMat.getAdditionalRenderState().setBackStencilMask(0xFF);
        boxMat.setTexture("ColorMap", assetManager.loadTexture("Common/Textures/MissingTexture.png"));

        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        floorMat.setTexture("ColorMap", assetManager.loadTexture("Common/Textures/MissingTexture.png"));

        Geometry floor = new Geometry("Floor", new Box(10f, 0, 10f));
        floor.setMaterial(floorMat);
        rootNode.attachChild(floor);

        Geometry box1 = new Geometry("Box1", new Box(0.5f, 0.5f, 0.5f));
        box1.setLocalTranslation(3, 1.5f, 0);
        box1.setLocalScale(4);
        box1.addControl(new OutlineControl(assetManager, ColorRGBA.Blue));
        box1.setMaterial(boxMat);
        Geometry box2 = new Geometry("Box2", new Box(0.5f, 0.5f, 0.5f));
        box2.setLocalTranslation(-3, 1.5f, 0);
        box2.setLocalScale(3);
        box2.addControl(new OutlineControl(assetManager,ColorRGBA.Red));
        box2.setMaterial(boxMat);

        rootNode.attachChild(box1);
        rootNode.attachChild(box2);

        //This is to make sure a depth stencil format is used in the TestChooser app.
        FilterPostProcessor postProcessor=new FilterPostProcessor(assetManager);
        postProcessor.setFrameBufferDepthFormat(Image.Format.Depth24Stencil8);
        viewPort.addProcessor(postProcessor);
        postProcessor.addFilter(new BloomFilter());
    }


    public static void main(String[] args) {
        Logger.getLogger("").setLevel(Level.FINEST);
        TestStencilOutline app = new TestStencilOutline();
        AppSettings settings = new AppSettings(true);
        settings.setGraphicsDebug(true);
        settings.setDepthBits(24);
        settings.setStencilBits(8);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
}
