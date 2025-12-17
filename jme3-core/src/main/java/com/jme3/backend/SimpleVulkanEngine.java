package com.jme3.backend;

import com.jme3.app.Application;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.descriptors.PoolSize;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.mesh.InputRate;
import com.jme3.vulkan.mesh.MeshLayout;
import com.jme3.vulkan.mesh.VertexBinding;
import com.jme3.vulkan.mesh.VulkanVertexBinding;
import com.jme3.vulkan.mesh.attribute.Normal;
import com.jme3.vulkan.mesh.attribute.Position;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.TheNewOneAndOnlyCache;
import com.jme3.vulkan.pipeline.graphics.CompatGraphicsPipeline;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.render.GeometryBatch;
import com.jme3.vulkan.render.VulkanGeometryBatch;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.ScenePropertyStack;
import org.lwjgl.vulkan.KHRSwapchain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SimpleVulkanEngine implements Engine {

    private final Application app;
    private final int frames;

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private DescriptorPool descriptorPool;
    private CommandPool graphicsPool, transientGraphicsPool;
    private RenderPass renderPass;
    private BufferStream stream;
    private MeshLayout meshLayout;
    private VertexPipeline opaquePipeline;

    private final Map<String, Bucket> buckets = new HashMap<>();
    private final TheNewOneAndOnlyCache<Pipeline> pipelineCache = new TheNewOneAndOnlyCache<>();

    public SimpleVulkanEngine(Application app, int frames) {
        this.app = app;
        this.frames = frames;
    }

    @Override
    public VertexBinding.Builder createMeshVertexBinding(IntEnum<InputRate> rate) {
        return VulkanVertexBinding.create(device, stream, rate);
    }

    public void initialize() {

        instance = VulkanInstance.build(VulkanInstance.Version.v10, i -> {
            i.addGlfwExtensions();
            i.addDebugExtension();
            i.addLunarGLayer();
        });
        instance.createLogger(Level.SEVERE);

        surface = new Surface(instance, app);

        device = LogicalDevice.build(instance, id -> new GeneralPhysicalDevice(instance, surface, id), d -> {
            d.addFilter(surface);
            d.addFilter(DeviceFilter.swapchain(surface));
            d.addCriticalExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            d.addFeature(DeviceFeature.anisotropy(1f, true));
        });

        swapchain = Swapchain.build(device, surface, s -> {
            s.addQueue(device.getPhysicalDevice().getGraphics());
            s.addQueue(device.getPhysicalDevice().getPresent());
            s.selectFormat(Swapchain.format(Format.B8G8R8A8_SRGB, ColorSpace.KhrSrgbNonlinear));
            s.selectMode(Swapchain.PresentMode.Mailbox);
            s.selectExtentByWindow();
            s.selectImageCount(frames);
        });

        descriptorPool = new DescriptorPool(device, 1000,
                new PoolSize(Descriptor.UniformBuffer, 1000),
                new PoolSize(Descriptor.CombinedImageSampler, 1000));
        graphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.ResetCommandBuffer);
        transientGraphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.Transient);

        VulkanImage renderDepth = BasicVulkanImage.build(device, GpuImage.Type.TwoDemensional, i -> {
            i.setSize(swapchain.getExtent().x, swapchain.getExtent().y);
            i.setFormat(device.getPhysicalDevice().findSupportedFormat(
                    VulkanImage.Tiling.Optimal,
                    FormatFeature.DepthStencilAttachment,
                    Format.Depth32SFloat, Format.Depth32SFloat_Stencil8UInt, Format.Depth24UNorm_Stencil8UInt));
            i.setTiling(VulkanImage.Tiling.Optimal);
            i.setUsage(ImageUsage.DepthStencilAttachment);
            i.setMemoryProps(MemoryProp.DeviceLocal);
        });
        VulkanImageView depthView = VulkanImageView.build(renderDepth, ImageView.Type.TwoDemensional, v -> {
            v.setAspect(VulkanImage.Aspect.Depth);
        });

        CommandBuffer initCmds = transientGraphicsPool.allocateCommandBuffer();
        initCmds.begin();
        renderDepth.transitionLayout(initCmds, VulkanImage.Layout.DepthStencilAttachmentOptimal);
        TimelineSemaphore initWait = new TimelineSemaphore(device, 0L);
        final long INIT_SIGNAL = 1L;
        initCmds.signal(initWait, INIT_SIGNAL);
        initCmds.endAndSubmit();

        renderPass = RenderPass.build(device, r -> {
            Attachment color = r.createAttachment(swapchain.getFormat(), 1, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.Store);
                a.setFinalLayout(VulkanImage.Layout.ColorAttachmentOptimal);
            });
            Attachment depth = r.createAttachment(depthView.getImage().getFormat(), 1, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setFinalLayout(VulkanImage.Layout.DepthStencilAttachmentOptimal);
            });
            Subpass scene = r.createSubpass(PipelineBindPoint.Graphics, s -> {
                s.addColorAttachment(color.createReference(VulkanImage.Layout.ColorAttachmentOptimal));
                s.setDepthStencilAttachment(depth.createReference(VulkanImage.Layout.DepthStencilAttachmentOptimal));
            });
            r.createDependency(null, scene, d -> {
                d.setSrcStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setSrcAccessMask(Flag.of(scene.getPosition()));
                d.setDstStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setDstAccessMask(Flag.of(Access.ColorAttachmentWrite, Access.DepthStencilAttachmentWrite));
            });
        });

        stream = new BufferStream(device, 500);

        // handled by the technique
        meshLayout = MeshLayout.build(l -> {
            l.addBinding(VulkanVertexBinding.create(device, stream, InputRate.Vertex)
                    .add("Position", Format.RGB32SFloat, Position::new)
                    .build());
            l.addBinding(VulkanVertexBinding.create(device, stream, InputRate.Vertex)
                    .add("TexCoord", Format.RG32SFloat, iTexCoord::new)
                    .build());
            l.addBinding(VulkanVertexBinding.create(device, stream, InputRate.Vertex)
                    .add("Normal", Format.RGB32SFloat, Normal::new)
                    .build());
        });

        // Pipeline layout maps uniforms into shader bindings, so it should be handled
        // specifically by whatever knows all the uniforms the shaders need and in what
        // bindings. Unfortunately, the exact layout depends on the defines used, so
        // this could be a bit sticky.
        PipelineLayout opaqueLayout = PipelineLayout.build(device, l -> {
            // apply uniform properties defined by material definition
            l.nextUniformSet(u -> {
                u.addBinding("ColorMap", new SetLayoutBinding(Descriptor.CombinedImageSampler, 0, ShaderStage.Fragment));
            });
        });

        // Pipelines are created by the geometry batch in response to geometric properties.
        opaquePipeline = CompatGraphicsPipeline.build(device, renderPass.getSubpasses().get(0), opaqueLayout, meshLayout, renderState, p -> {
            p.setCache(pipelineCache);
            p.setNumBlendAttachments(1);
            p.setDynamic(DynamicState.ViewPort, true);
            p.setDynamic(DynamicState.Scissor, true);
            // apply attribute locations defined by material definition
            p.setAttributeLocation("Position", 0);
            p.setAttributeLocation("TexCoord", 1);
            p.setAttributeLocation("Normal", 2);
        });

        initWait.block(INIT_SIGNAL, 3000L);
        swapchain.createFrameBuffers(renderPass, depthView);

    }

    public void renderViewPort(ViewPort viewPort) {

        Swapchain.PresentImage presentImage = swapchain.acquireNextImage();

        CommandBuffer cmd = transientGraphicsPool.allocateCommandBuffer();
        cmd.begin();

        stream.stream();
        stream.upload(cmd);

        renderPass.begin(cmd, presentImage.getFrameBuffer());
        opaquePipeline.bind(cmd);

        ScenePropertyStack<RenderQueue.Bucket> bucket = new ScenePropertyStack<>(RenderQueue.Bucket.Opaque, RenderQueue.Bucket.Inherit, Spatial::getLocalQueueBucket);
        ScenePropertyStack<Spatial.CullHint> cullHint = new ScenePropertyStack<>(Spatial.CullHint.Dynamic, Spatial.CullHint.Inherit, Spatial::getLocalCullHint);
        Map<String, GeometryBatch> batches = new HashMap<>();
        for (Spatial scene : viewPort.getScenes()) {
            for (Spatial.GraphIterator it = scene.iterator(bucket, cullHint); it.hasNext();) {
                Spatial child = it.next();
                if (cullHint.peek() == Spatial.CullHint.Always || (cullHint.peek() == Spatial.CullHint.Dynamic
                        && viewPort.getCamera().contains(child.getWorldBound()) == Camera.FrustumIntersect.Outside)) {
                    it.skipChildren();
                    continue;
                }
                if (child instanceof Geometry) {
                    Bucket b = buckets.get(bucket.peek().name());
                    if (b != null) {
                        GeometryBatch batch = batches.computeIfAbsent(bucket.peek().name(), n -> new VulkanGeometryBatch(b.getPipeline(), descriptorPool, viewPort.getCamera(), b.getSorter()));
                        batch.add((Geometry)child, false, false);
                    }
                }
            }
        }

        renderPass.end(cmd);

        cmd.await(swapchainImageAcquire, PipelineStage.ColorAttachmentOutput);
        cmd.signal(renderComplete);
        cmd.endAndSubmit(inFlight);

        swapchain.present(device.getPhysicalDevice().getPresent(), presentImage, renderComplete);

    }

    private static class Bucket {

        private final GraphicsPipeline pipeline;
        private final Comparator<VulkanGeometryBatch.Element> sorter;

        private Bucket(GraphicsPipeline pipeline, Comparator<VulkanGeometryBatch.Element> sorter) {
            this.pipeline = pipeline;
            this.sorter = sorter;
        }

        public GraphicsPipeline getPipeline() {
            return pipeline;
        }

        public Comparator<VulkanGeometryBatch.Element> getSorter() {
            return sorter;
        }

    }

}
