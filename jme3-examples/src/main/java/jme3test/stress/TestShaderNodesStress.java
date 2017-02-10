package jme3test.stress;

import com.jme3.app.BasicProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.profile.*;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestShaderNodesStress extends SimpleApplication {

    public static void main(String[] args) {
        TestShaderNodesStress app = new TestShaderNodesStress();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        Quad q = new Quad(1, 1);
        Geometry g = new Geometry("quad", q);
        g.setLocalTranslation(-500, -500, 0);
        g.setLocalScale(1000);

        rootNode.attachChild(g);
        cam.setLocation(new Vector3f(0.0f, 0.0f, 0.40647888f));
        cam.setRotation(new Quaternion(0.0f, 1.0f, 0.0f, 0.0f));

        Texture tex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/UnshadedNodes.j3md");
      //Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        mat.setColor("Color", ColorRGBA.Yellow);
        mat.setTexture("ColorMap", tex);
        g.setMaterial(mat);
        //place the geoms in the transparent bucket so that they are rendered back to front for maximum overdraw
        g.setQueueBucket(RenderQueue.Bucket.Transparent);

        for (int i = 0; i < 1000; i++) {
            Geometry cl = g.clone(false);
            cl.move(0, 0, -(i + 1));
            rootNode.attachChild(cl);
        }

        flyCam.setMoveSpeed(20);
        Logger.getLogger("com.jme3").setLevel(Level.WARNING);

        this.setAppProfiler(new Profiler());

    }

    private class Profiler implements AppProfiler {

        private long startTime;
        private long updateTime;
        private long renderTime;
        private long sum;
        private int nbFrames;

        @Override
        public void appStep(AppStep step) {

            switch (step) {
                case BeginFrame:
                    startTime = System.nanoTime();
                    break;
                case RenderFrame:
                    updateTime = System.nanoTime();
                    //   System.err.println("Update time : " + (updateTime - startTime));
                    break;
                case EndFrame:
                    nbFrames++;
                    if (nbFrames >= 150) {
                        renderTime = System.nanoTime();
                        sum += renderTime - updateTime;
                        System.err.println("render time : " + (renderTime - updateTime));
                        System.err.println("Average render time : " + ((float)sum / (float)(nbFrames-150)));
                    }
                    break;

            }

        }

        @Override
        public void vpStep(VpStep step, ViewPort vp, RenderQueue.Bucket bucket) {

        }

        @Override
        public void spStep(SpStep step, String... additionalInfo) {

        }

    }
}
