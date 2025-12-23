package com.jme3.backend;

import com.jme3.app.Application;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.PersistentVulkanBuffer;
import com.jme3.vulkan.buffers.newbuf.HostVisibleBuffer;
import com.jme3.vulkan.buffers.newbuf.StreamingBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.descriptors.PoolSize;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attribute.Normal;
import com.jme3.vulkan.mesh.attribute.Position;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.framebuffer.OutputFrameBuffer;
import com.jme3.vulkan.render.GeometryBatch;
import com.jme3.vulkan.render.VulkanGeometryBatch;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.BinarySemaphore;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.ScenePropertyStack;
import org.lwjgl.vulkan.KHRSwapchain;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SimpleVulkanEngine implements Engine {

    private final Application app;
    private final Frame[] framesInFlight;
    private long frameCount = 0;

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private DescriptorPool descriptorPool;
    private CommandPool graphicsPool, transientGraphicsPool;
    private RenderPass renderPass;
    private BufferStream stream;
    private MeshLayout meshLayout;

    private OutputFrameBuffer outFrameBuffer;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>();
    private final Cache<Pipeline> pipelineCache = new Cache<>();
    private final Cache<ShaderModule> shaderCache = new Cache<>();

    public SimpleVulkanEngine(Application app, int framesInFlight) {
        this.app = app;
        this.framesInFlight = new Frame[framesInFlight];
    }

    @Override
    public VertexBinding createMeshVertexBinding(IntEnum<InputRate> rate, Consumer<VertexBinding.Builder> config) {
        return VulkanVertexBinding.build(device, stream, rate, config);
    }

    @Override
    public Material createMaterial(String matdefName) {
        return null;
    }

    @Override
    public Mesh createMesh(int vertices, int instances) {
        return new AdaptiveMesh(meshLayout, vertices, instances);
    }

    @Override
    public GpuBuffer createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage) {
        switch (dataUsage) {
            case Static: case Dynamic: {
                StreamingBuffer buf = new StreamingBuffer(device, size, bufUsage);
                stream.add(buf);
                return buf;
            }
            case Stream: {
                return new PersistentVulkanBuffer<>(HostVisibleBuffer.build(
                        device, size, b -> b.setUsage(BufferUsage.Vertex)));
            }
            case CpuOnly: throw new IllegalArgumentException("Cannot create cpu-only vertex buffer for Vulkan.");
            default: throw new UnsupportedOperationException("Unrecognized: " + dataUsage);
        }
    }

    public void initialize() {

        instance = VulkanInstance.build(VulkanInstance.Version.v10, i -> {
            i.addGlfwExtensions();
            i.addDebugExtension(); // for game development only
            i.addLunarGLayer(); // for game development only
        });
        instance.createLogger(Level.SEVERE);

        surface = new Surface(instance, app);

        device = LogicalDevice.build(instance, id -> new GeneralPhysicalDevice(instance, surface, id), d -> {
            d.addFilter(surface);
            d.addFilter(DeviceFilter.swapchain(surface));
            d.addCriticalExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            d.addFeature(DeviceFeature.anisotropy(1f, true));
        });

        descriptorPool = new DescriptorPool(device, 1000,
                new PoolSize(Descriptor.UniformBuffer, 1000),
                new PoolSize(Descriptor.CombinedImageSampler, 1000));
        graphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.ResetCommandBuffer);
        transientGraphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.Transient);

        VulkanImageView renderDepth = createPresentDepth();
        CommandBuffer initCmds = transientGraphicsPool.allocateCommandBuffer();
        initCmds.begin();
        renderDepth.getImage().transitionLayout(initCmds, VulkanImage.Layout.DepthStencilAttachmentOptimal);
        TimelineSemaphore initWait = new TimelineSemaphore(device, 0L);
        final long INIT_SIGNAL = 1L;
        initCmds.signal(initWait, INIT_SIGNAL);
        initCmds.endAndSubmit();

        swapchain = Swapchain.build(device, surface, s -> {
            s.addQueue(device.getPhysicalDevice().getGraphics());
            s.addQueue(device.getPhysicalDevice().getPresent());
            s.selectFormat(Swapchain.format(Format.B8G8R8A8_SRGB, ColorSpace.KhrSrgbNonlinear));
            s.selectMode(Swapchain.PresentMode.Mailbox);
            s.selectExtentByWindow();
            s.selectImageCount(framesInFlight.length);
        });
        swapchain.setUpdater(new SwapchainUpdater());

        renderPass = RenderPass.build(device, r -> {
            Attachment color = r.createAttachment(swapchain.getFormat(), 1, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.Store);
                a.setFinalLayout(VulkanImage.Layout.ColorAttachmentOptimal);
            });
            Attachment depth = r.createAttachment(renderDepth.getImage().getFormat(), 1, a -> {
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

        outFrameBuffer = new OutputFrameBuffer(swapchain, renderPass);

        stream = new BufferStream(device, 500);

        meshLayout = MeshLayout.build(l -> {
            l.addBinding(createMeshVertexBinding(InputRate.Vertex, v -> {
                v.add("Position", Format.RGB32SFloat, Position::new);
            }));
            l.addBinding(createMeshVertexBinding(InputRate.Vertex, v -> {
                v.add("TexCoord", Format.RG32SFloat, Position::new);
            }));
            l.addBinding(createMeshVertexBinding(InputRate.Vertex, v -> {
                v.add("Normal", Format.RGB32SFloat, Normal::new);
            }));
        });

        for (int i = 0; i < framesInFlight.length; i++) {
            framesInFlight[i] = new Frame();
        }

        initWait.block(INIT_SIGNAL, 3000L);
        outFrameBuffer.setDepthTarget(renderDepth);

    }

    public boolean render(List<ViewPort> viewPorts) {
        return framesInFlight[(int)(frameCount++ % framesInFlight.length)].render(viewPorts);
    }

    private VulkanImageView createPresentDepth() {
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
        return VulkanImageView.build(renderDepth, ImageView.Type.TwoDemensional, v -> {
            v.setAspect(VulkanImage.Aspect.Depth);
        });
    }

    public FrameBuffer<?> getOutputFrameBuffer() {
        return outFrameBuffer;
    }

    private class Bucket {

        private final Comparator<VulkanGeometryBatch.Element> sorter;
        private GeometryBatch<VulkanGeometryBatch.Element> batch;

        private Bucket(Comparator<VulkanGeometryBatch.Element> sorter) {
            this.sorter = sorter;
        }

        public GeometryBatch<VulkanGeometryBatch.Element> getOrCreateBatch(Camera camera) {
            if (batch == null) {
                batch = new VulkanGeometryBatch(camera, sorter, app.getAssetManager(),
                        descriptorPool, pipelineCache, shaderCache);
            }
            return batch;
        }

        public void render(CommandBuffer cmd) {
            if (batch != null) {
                batch.render(cmd);
                batch = null;
            }
        }

    }

    protected class Frame {

        private final CommandBuffer graphics = graphicsPool.allocateCommandBuffer();
        private final Semaphore imageAcquired = new BinarySemaphore(device);
        private final Semaphore renderComplete = new BinarySemaphore(device);
        private final Fence inFlight = new Fence(device, true);

        public boolean render(List<ViewPort> viewPorts) {
            inFlight.block(5000L);
            if (!outFrameBuffer.acquireNextImage(imageAcquired, null, 5000L)) {
                return false;
            }
            graphics.resetAndBegin();
            stream.stream(); // move to before inFlight.block?
            stream.upload(graphics);
            renderPass.begin(graphics, outFrameBuffer);
            for (ViewPort vp : viewPorts) {
                render(vp);
            }
            renderPass.end(graphics);
            graphics.await(imageAcquired, PipelineStage.ColorAttachmentOutput);
            graphics.signal(renderComplete);
            graphics.endAndSubmit(inFlight);
            swapchain.present(device.getPhysicalDevice().getPresent(), outFrameBuffer.getCurrentImage(), renderComplete);
            return true;
        }

        private void render(ViewPort vp) {
            ScenePropertyStack<RenderQueue.Bucket> bucket = new ScenePropertyStack<>(
                    RenderQueue.Bucket.Opaque, RenderQueue.Bucket.Inherit, Spatial::getLocalQueueBucket);
            ScenePropertyStack<Spatial.CullHint> cullHint = new ScenePropertyStack<>(
                    Spatial.CullHint.Dynamic, Spatial.CullHint.Inherit, Spatial::getLocalCullHint);
            for (Spatial scene : vp.getScenes()) {
                for (Spatial.GraphIterator it = scene.iterator(bucket, cullHint); it.hasNext();) {
                    Spatial child = it.next();
                    if (cullHint.peek() == Spatial.CullHint.Always || (cullHint.peek() == Spatial.CullHint.Dynamic
                            && vp.getCamera().contains(child.getWorldBound()) == Camera.FrustumIntersect.Outside)) {
                        it.skipChildren();
                        continue;
                    }
                    if (child instanceof Geometry) {
                        Bucket b = buckets.get(bucket.peek().name());
                        if (b != null) {
                            b.getOrCreateBatch(vp.getCamera()).add((Geometry)child);
                        }
                    }
                }
            }
            for (Bucket b : buckets.values()) {
                b.render(graphics);
            }
        }

    }

    private class SwapchainUpdater implements Consumer<Swapchain> {

        @Override
        public void accept(Swapchain swapchain) {
            swapchain.update();
            VulkanImageView depth = createPresentDepth();
            CommandBuffer cmd = transientGraphicsPool.allocateCommandBuffer();
            cmd.begin();
            depth.getImage().transitionLayout(cmd, VulkanImage.Layout.DepthStencilAttachmentOptimal);
            TimelineSemaphore wait = new TimelineSemaphore(device, 0L);
            cmd.signal(wait, 1L);
            cmd.endAndSubmit();
            wait.block(1L, 1000L);
            outFrameBuffer.setDepthTarget(depth);
        }

    }

}
