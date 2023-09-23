package jme3test.model.anim;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

public class TestSpatialAnim extends SimpleApplication {

    public static void main(String[] args) {
        TestSpatialAnim app = new TestSpatialAnim();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(Vector3f.UNIT_XYZ.negate());
        rootNode.addLight(dl);

        // Create model
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("box", box);
        geom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node model = new Node("model");
        model.attachChild(geom);

        Box child = new Box(0.5f, 0.5f, 0.5f);
        Geometry childGeom = new Geometry("box", child);
        childGeom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node childModel = new Node("childmodel");
        childModel.setLocalTranslation(2, 2, 2);
        childModel.attachChild(childGeom);
        model.attachChild(childModel);
        
        //animation parameters
        float animTime = 5;
        int fps = 25;
        float totalXLength = 10;
        
        //calculating frames
        int totalFrames = (int) (fps * animTime);
        float dT = animTime / totalFrames, t = 0;
        float dX = totalXLength / totalFrames, x = 0;
        float[] times = new float[totalFrames];
        Vector3f[] translations = new Vector3f[totalFrames];
        Quaternion[] rotations = new Quaternion[totalFrames];
        Vector3f[] scales = new Vector3f[totalFrames];
        for (int i = 0; i < totalFrames; ++i) {
                times[i] = t;
                t += dT;
                translations[i] = new Vector3f(x, 0, 0);
                x += dX;
                rotations[i] = Quaternion.IDENTITY;
                scales[i] = Vector3f.UNIT_XYZ;
        }
        TransformTrack transformTrack = new TransformTrack(geom, times, translations, rotations, scales);
        TransformTrack transformTrackChild = new TransformTrack(childGeom, times, translations, rotations, scales);
        // creating the animation
        AnimClip animClip = new AnimClip("anim");
        animClip.setTracks(new AnimTrack[] { transformTrack, transformTrackChild });

        // create spatial animation control
        AnimComposer animComposer = new AnimComposer();
        animComposer.addAnimClip(animClip);

        model.addControl(animComposer);
        rootNode.attachChild(model);

        // run animation
        model.getControl(AnimComposer.class).setCurrentAction("anim");
    }
}
