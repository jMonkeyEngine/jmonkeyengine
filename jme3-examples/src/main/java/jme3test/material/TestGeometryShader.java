package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

/**
 * Created by michael on 23.02.15.
 */
public class TestGeometryShader extends SimpleApplication {
    @Override
    public void simpleInitApp() {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(new int[]{1}));
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(new float[]{0, 0, 0}));
        mesh.setMode(Mesh.Mode.Points);
        mesh.setBound(new BoundingBox(new Vector3f(0, 0, 0), 10, 10, 10));
        mesh.updateCounts();
        Geometry geometry = new Geometry("Test", mesh);
        geometry.updateGeometricState();
        geometry.setMaterial(new Material(assetManager, "Materials/Geom/SimpleGeom.j3md"));
        //geometry.getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        //geometry.setMaterial(assetManager.loadMaterial("Materials/Geom/SimpleTess.j3md"));
        rootNode.attachChild(geometry);

        Geometry geometry1 = new Geometry("T1", new Sphere(10, 10, 1));
        geometry1.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        rootNode.attachChild(geometry1);

    }

    public static void main(String[] args) {
        TestGeometryShader app = new TestGeometryShader();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33);
        app.setSettings(settings);
        app.start();
    }
}
