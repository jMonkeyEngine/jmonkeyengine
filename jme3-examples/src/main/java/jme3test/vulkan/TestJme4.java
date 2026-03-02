package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.backend.Engine;
import com.jme3.backend.SimpleVulkanEngine;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.util.struct.StructLayout;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.material.structs.UnshadedParams;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.VertexBinding;
import com.jme3.vulkan.mesh.attribute.*;

public class TestJme4 extends SimpleApplication {

    private final Engine engine = new SimpleVulkanEngine(this, 3);

    public static void main(String[] args) {
        TestJme4 app = new TestJme4();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        MeshLayout layout = MeshLayout.build(m -> {
            m.addBinding(VertexBinding.build(engine, InputRate.Vertex, b -> {
                b.add("Position", Format.RGB32_SFloat, i -> new Position(ValueMapper.Float32, i));
                b.add("TexCoord", Format.RG32_SFloat, i -> new TexCoord(ValueMapper.Float32, i));
                b.add("Normal", Format.RGB32_SFloat, i -> new Normal(ValueMapper.Float32, i));
                b.add("Tangent", Format.RGBA32_SFloat, i -> new Tangent(ValueMapper.Float32, i));
            }));
            m.addBinding(VertexBinding.build(engine, InputRate.Vertex, b -> {
                b.add("BindPosition", Format.RGB32_SFloat, i -> new Position(ValueMapper.Float32, i));
            }));
        });

        MeshLayout l2 = MeshLayout.build(m -> {
            m.addBinding(new VertexBinding(engine, InputRate.Vertex, () -> new Vertex(StructLayout.std140)));
        });

        Geometry g = new Geometry("geom_jme4", new Box(1f, 1f, 1f));
        Material m = engine.createMaterial("Common/MatDefs/Misc/Unshaded.j3md");
        UnshadedParams p = m.get("Parameters");
        p.color.get().set(ColorRGBA.Blue);
        p.glowColor.set(ColorRGBA.Blue.mult(0.2f));
        p.vertexColor.set(true);
        g.setMaterial(m);
        rootNode.attachChild(g);

    }

    @Override
    public Engine getEngine() {
        return engine;
    }

}
