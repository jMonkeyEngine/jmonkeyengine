package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ContrastAdjustmentFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;


public class TestContrastAdjustmentFilter extends SimpleApplication {

    public static void main(String[] args) {
        new TestContrastAdjustmentFilter().start();
    }

    @Override
    public void simpleInitApp() {
        //setup a spatial and a texture
        final Quad quad = new Quad(40, 20);
        final Geometry quadGeo = new Geometry("Ball", quad);
        final Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        final Texture texture = assetManager.loadTexture("Textures/Sky/Earth/Earth.jpg");
        material.setTexture("ColorMap", texture);
        quadGeo.setMaterial(material);

        rootNode.attachChild(quadGeo);

        //add light
        final AmbientLight ambientLight = new AmbientLight(ColorRGBA.White);
        rootNode.addLight(ambientLight);

        //setup the filter
        final FilterPostProcessor postProcessor = new FilterPostProcessor(assetManager);
        final ContrastAdjustmentFilter contrastAdjustmentFilter = new ContrastAdjustmentFilter(2.2f, 2.2f, 2.2f);
        postProcessor.addFilter(contrastAdjustmentFilter);
        viewPort.addProcessor(postProcessor);
    }
}
