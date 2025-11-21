package jme3test.vulkan;

import com.jme3.backend.Engine;
import com.jme3.backend.VulkanEngine;
import com.jme3.math.Vector3f;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.mesh.AdaptiveMesh;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.attribute.Position;

public class VulkanComponentsTest {

    public static void main(String[] args) {

        Engine engine = new VulkanEngine(null, 3);
        AdaptiveMesh mesh = new AdaptiveMesh(new MeshLayout(), 100, 1);
        mesh.getLayout().addBinding(engine.createMeshVertexBinding(InputRate.Vertex)
                .add(GlVertexBuffer.Type.Position, Format.RGB32SFloat, Position::new)
                .build());
        mesh.setUsage("attr_name", GlVertexBuffer.Usage.Dynamic);
        Position pos = mesh.mapAttribute(GlVertexBuffer.Type.Position);
        for (int i : pos.indices()) {
            pos.set(i, i, 0, 0);
        }
        float xExtent = 0;
        for (Vector3f p : pos.iterator(new Vector3f())) {
            xExtent = Math.max(xExtent, p.x);
        }
        pos.unmap();
        mesh.render(null, 0);

    }

}
