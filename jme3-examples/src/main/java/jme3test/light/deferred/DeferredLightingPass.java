package jme3test.light.deferred;

import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.Vector4f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shader.BufferObject;
import com.jme3.shader.VarType;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DeferredLightingPass extends Filter {
    protected AssetManager assetManager;
    protected Material material;
    protected ViewPort vp;
    protected Node rootNode;

    public DeferredLightingPass(Node rootNode) {
        super("Deferred: Lighting Pass");
        this.rootNode = rootNode;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        this.assetManager = manager;
        this.vp = vp;
        if (material == null) { // allow it to be pre-set from somewhere.
            material = new Material(assetManager, "TestDeferred/MatDefs/LightingPass.j3md");
        }
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setRootNode(Node rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    protected void preFrame(float tpf) {
        super.preFrame(tpf);
        material.setVector3("ViewPosition", vp.getCamera().getLocation());
        List<PointLight> lights = StreamSupport.stream(rootNode.getWorldLightList().spliterator(), false)
                .filter(l -> l instanceof PointLight)
                .map(l -> (PointLight)l).collect(Collectors.toList());

        Vector4f[] positions = lights.stream().map(pl -> new Vector4f(pl.getPosition().x, pl.getPosition().y, pl.getPosition().z, pl.getRadius()))
                .toArray(Vector4f[]::new);
        Vector4f[] colors = lights.stream().map(pl -> pl.getColor().toVector4f()).toArray(Vector4f[]::new);
        material.setParam("PointLight_Position", VarType.Vector4Array, positions);
        material.setParam("PointLight_Color", VarType.Vector4Array, colors);
        /*bo.setPointLights(lights);
        material.setUniformBufferObject("PointLights", bo);*/
    }

    @Override
    protected boolean isRequiresSceneTexture() {
        return false;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return false;
    }

    private static class CustomBufferObject extends BufferObject {
        private Iterable<PointLight> lights;

        public CustomBufferObject() {
            super(Layout.std140);
            setBufferType(BufferType.UniformBufferObject);
        }

        public void setPointLights(Iterable<PointLight> lights) {
            this.lights = lights;
        }

        @Override
        public ByteBuffer computeData(int maxSize) {
            // TODO: assert lights.length <= 32
            ByteBuffer data = ByteBuffer.allocateDirect(32 * 8 * 4);
            for (PointLight light: lights) {
                write(data, light.getPosition());
                writeVec4(data, light.getColor());
                data.putFloat(light.getRadius());
            }

            return data;
        }
    }
}
