package jme3test.vulkan;

import com.jme3.backend.Engine;
import com.jme3.backend.SimpleVulkanEngine;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.mesh.AdaptiveMesh;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.attribute.Position;
import com.jme3.vulkan.pipeline.DynamicState;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.render.BatchElement;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.sync.BinarySemaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;

public class VulkanComponentsTest {

    public static void compact() {

        Engine engine = Engine.createBestEngine();

        Material mat = engine.createMaterial("Path/To/Matdef.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        Mesh mesh = engine.createBoxMesh();
        Position pos = mesh.mapAttribute(GlVertexBuffer.Type.Position);
        for (Vector3f p : pos.readWrite(new Vector3f())) {
            p.multLocal(5f);
            p.addLocal((float)Math.random(), (float)Math.random(), (float)Math.random());
        }
        Geometry g = new Geometry("my_geom", mesh);
        g.setLocalTranslation(0f, 1f, 0f);
        g.setMaterial(mat);
        rootNode.attachChild(g);

        // in render manager
        engine.renderViewPort(viewPort);

        RenderState renderState = new RenderState();
        engine.addBucket("Opaque", engine.createGraphicsPipeline(renderState), (BatchElement o1, BatchElement o2) -> {
            return Float.compare(o1.computeDistanceSq(), o2.computeDistanceSq());
        });

    }

    public static void main(String[] args) {

        LogicalDevice<?> device = null;
        Engine engine = new SimpleVulkanEngine(device, 3);
        BufferStream stream = new BufferStream(device, 1000);
        MeshLayout meshLayout = MeshLayout.build(b -> {
            b.addBinding(engine.createMeshVertexBinding(InputRate.Vertex)
                    .add(GlVertexBuffer.Type.Position, Format.RGB32SFloat, Position::new)
                    .setUsage(GlVertexBuffer.Usage.Dynamic)
                    .build());
        });
        AdaptiveMesh mesh = new AdaptiveMesh(meshLayout, 100, 1);
        Position pos = mesh.mapAttribute(GlVertexBuffer.Type.Position);
        for (int i : pos.indices()) {
            pos.set(i, i, 0, 0);
        }
        float xExtent = 0;
        for (Vector3f p : pos.read(new Vector3f())) {
            xExtent = Math.max(xExtent, p.x);
        }
        pos.unmap();

        CommandBuffer cmd = new CommandBuffer(null);

        pos.push(20, 3);
        pos.push();
        mesh.pushElements(InputRate.Vertex, 20, 3);
        mesh.pushElements(InputRate.Instance);
        stream.stream();
        stream.upload(cmd);

        cmd.resetAndBegin();
        cmd.await(new BinarySemaphore(null), PipelineStage.TopOfPipe);
        cmd.signal(new TimelineSemaphore(null, 0L), 15L);
        cmd.endAndSubmit();

        NewMaterial mat = new NewMaterial();
        PipelineLayout pipelineLayout = PipelineLayout.build(device, b -> {
            b.nextUniformSet(d -> {
                d.addBinding("Matrices", new SetLayoutBinding(Descriptor.UniformBuffer, 0, ShaderStage.Vertex));
                d.addBinding("ColorMap", new SetLayoutBinding(Descriptor.CombinedImageSampler, 1, ShaderStage.Fragment));
            });
        });
        GraphicsPipeline pipeline = GraphicsPipeline.build(device, null, pipelineLayout, meshLayout, b -> {
            b.addBlendAttachment(ColorBlendAttachment.build());
            b.setDynamic(DynamicState.ViewPort, true);
            b.setDynamic(DynamicState.Scissor, true);
            b.setAttributeLocation(GlVertexBuffer.Type.Position, 0);
            b.setAttributeLocation(GlVertexBuffer.Type.TexCoord, 1);
            b.setAttributeLocation(GlVertexBuffer.Type.Normal, 2);
        });

        pipeline.bind(null);      // set shader context
        if (pipeline.isDynamic(DynamicState.ViewPort)) {
            // set viewport
        }
        if (pipeline.isDynamic(DynamicState.Scissor)) {
            // set scissor
        }
        mat.bind(null, pipeline, pool);
        mesh.render(null, pipeline, 15f);

    }

}