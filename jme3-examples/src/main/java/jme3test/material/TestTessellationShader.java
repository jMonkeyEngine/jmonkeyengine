package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.util.BufferUtils;

/**
 * Created by michael on 28.02.15.
 */
public class TestTessellationShader extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        Material material = new Material(getAssetManager(), "Materials/Tess/SimpleTess.j3md");
        material.setInt("TessellationFactor", 5);
        material.getAdditionalRenderState().setWireframe(true);
        Quad quad = new Quad(10, 10);
        quad.clearBuffer(VertexBuffer.Type.Index);
        quad.setBuffer(VertexBuffer.Type.Index, 4, BufferUtils.createIntBuffer(0, 1, 2, 3));
        quad.setMode(Mesh.Mode.Patch);
        quad.setPatchVertexCount(4);
        Geometry geometry = new Geometry("tessTest", quad);
        geometry.setMaterial(material);
        rootNode.attachChild(geometry);

        Geometry geometry1 = new Geometry("Demo", new Quad(2, 2));
        geometry1.setMaterial(new Material(getAssetManager(),"Common/MatDefs/Misc/Unshaded.j3md"));
        rootNode.attachChild(geometry1);
    }

    public static void main(String[] args) {
        new TestTessellationShader().start();
    }
}
