package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import jme3tools.converters.model.ModelConverter;

public class Test extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        Sphere s = new Sphere(8, 8, .5f);
        Geometry geom = new Geometry("sphere", s);
        //      ModelConverter.optimize(geom);

        Material mat = new Material(assetManager, "plain_texture.j3md");
        Texture tex = assetManager.loadTexture(new TextureKey("monkey.j3i"));
        mat.setTexture("ColorMap", tex);
//        geom.setMaterial(mat);

        for (int y = -1; y < 2; y++) {
            for (int x = -1; x < 2; x++) {
                Geometry geomClone = new Geometry("geom", s);
                geomClone.setMaterial(mat);
                geomClone.setLocalTranslation(x, y, 0);

                Transform t = geom.getLocalTransform().clone();
                Transform t2 = geomClone.getLocalTransform().clone();
                t.combineWithParent(t2);
                geomClone.setLocalTransform(t);

                rootNode.attachChild(geomClone);
            }
        }
    }
}
