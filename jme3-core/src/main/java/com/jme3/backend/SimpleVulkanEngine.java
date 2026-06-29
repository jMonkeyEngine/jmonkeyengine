package com.jme3.backend;

import com.jme3.app.Application;
import com.jme3.material.plugins.VulkanMaterialLoader;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.VulkanLogger;
import com.jme3.vulkan.buffer.BufferStream;
import com.jme3.vulkan.commands.*;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.descriptors.uniforms.BufferBinding;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.exp2.RenderSession;
import com.jme3.vulkan.material.experimental.*;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.*;
import com.jme3.util.cache.InlineTimedCache;
import com.jme3.vulkan.pipeline.framebuffer.*;
import com.jme3.vulkan.pipeline.graphics.DynamicGraphicsPipeline;
import com.jme3.vulkan.pipeline.state.GraphicsState;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.render.bucket.GraphicsElement;
import com.jme3.vulkan.render.bucket.VulkanRenderElement;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.BinarySemaphore;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import org.lwjgl.vulkan.*;

import java.util.*;
import java.util.function.Consumer;

public class SimpleVulkanEngine implements Engine {

    private final Application app;
    private final Frame[] framesInFlight;
    private long frameCount = -1;

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private DescriptorPool descriptorPool;
    private CommandPool graphicsPool, transientGraphicsPool;
    private BufferStream stream;
    private OutputFrameBuffer outFrameBuffer;

    private final InlineTimedCache<Pipeline> pipelineCache = new InlineTimedCache<>();
    private final InlineTimedCache<PipelineLayout> pipelineLayoutCache = new InlineTimedCache<>();
    private final InlineTimedCache<ShaderModule> shaderCache = new InlineTimedCache<>();
    private final InlineTimedCache<DescriptorSetLayout> descSetLayoutCache = new InlineTimedCache<>();

    private DescriptorSet globalBindings;

    public SimpleVulkanEngine(Application app, int framesInFlight) {
        this.app = app;
        this.framesInFlight = new Frame[framesInFlight];
        initialize();
        app.getViewPort().addGeometryBucket("Opaque", new GeometryBucket(new OpaqueComparator()));
        app.getViewPort().addGeometryBucket("Sky", new GeometryBucket(new OpaqueComparator()) {
            @Override
            public void setupRender(ViewPort vp, StandardRenderSettings settings) {
                settings.pushViewPort(settings.getViewPort().clone().toMaxDepth());
            }
            @Override
            public void cleanupRender(ViewPort vp, StandardRenderSettings settings) {
                settings.popViewPort();
            }
        });
        app.getViewPort().addGeometryBucket("Transparent", new GeometryBucket(new TransparentComparator()));
        app.getViewPort().addGeometryBucket("Translucent", new GeometryBucket(new TransparentComparator()));
    }

    private void initialize() {

        app.getAssetManager().registerLoader(VulkanMaterialLoader.class, "j4md");

        instance = VulkanInstance.build(VulkanInstance.Version.v11, i -> {
            i.addGlfwExtensions();
            i.addDebugExtension();
            i.addDynamicRenderingExtension();
            i.addLunarGLayer();
        });
        instance.createLogger(VulkanLogger.Severity.All, VulkanLogger.Type.All);

        surface = new Surface(instance, app);

        device = LogicalDevice.build(instance, id -> new GeneralPhysicalDevice(instance, surface, id), d -> {
            d.addFilter(surface);
            d.addFilter(DeviceFilter.swapchain(surface));
            d.addCriticalExtension(Swapchain.EXTENSION_NAME);
            d.addCriticalExtension(EXTMemoryBudget.VK_EXT_MEMORY_BUDGET_EXTENSION_NAME);
            d.addOptionalExtension(EXTRobustness2.VK_EXT_ROBUSTNESS_2_EXTENSION_NAME, 1f);
            d.addFeatureContainer(p -> VkPhysicalDeviceRobustness2FeaturesEXT.calloc().pNext(p));
            d.addFeature(DeviceFeature.anisotropy(1f));
            d.addFeature(DeviceFeature.nullDescriptor(1f));
            if (instance.getApiVersion().getEnum() < VulkanInstance.Version.v13.getEnum()) {
                d.addFeatureContainer(p -> VkPhysicalDeviceDynamicRenderingFeatures.calloc().pNext(p));
                d.addFeature(DeviceFeature.dynamicRendering(null));
            }
        });

        descriptorPool = new DescriptorPool(device, 1000,
                new PoolSize(Descriptor.UniformBuffer, 1000),
                new PoolSize(Descriptor.StorageBuffer, 1000),
                new PoolSize(Descriptor.CombinedImageSampler, 1000));
        graphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.ResetCommandBuffer);
        transientGraphicsPool = new CommandPool(device.getPhysicalDevice().getGraphics(), CommandPool.Create.Transient);

        VulkanImageView renderDepth = createPresentDepth();
        CommandBuffer initCmds = transientGraphicsPool.allocate(CommandBuffer.Level.Primary);
        initCmds.beginRecording();
        renderDepth.getImage().transitionLayout(initCmds, VulkanImage.Layout.DepthStencilAttachmentOptimal);
        TimelineSemaphore.SignalEvent initEvent = initCmds.signalEvent(new TimelineSemaphore(device));
        initCmds.endRecording();
        initCmds.submit();

        swapchain = Swapchain.build(device, surface, s -> {
            s.addSupportedQueue(device.getPhysicalDevice().getGraphics());
            s.addSupportedQueue(device.getPhysicalDevice().getPresent());
            s.selectFormat(Swapchain.format(Format.BGRA8_SRGB, ColorSpace.SrgbNonlinear));
            s.selectMode(Swapchain.PresentMode.Mailbox);
            s.selectExtentByWindow();
            s.selectImageCount(framesInFlight.length);
        });
        swapchain.setUpdater(new SwapchainUpdater());

        outFrameBuffer = new OutputFrameBuffer(swapchain, null);
        stream = new BufferStream(device, 500);
        for (int i = 0; i < framesInFlight.length; i++) {
            framesInFlight[i] = new Frame();
        }

        globalBindings = descriptorPool.allocateSets(DescriptorSetLayout.build(device, l -> {
            l.setCache(descSetLayoutCache);
            l.addBinding(0, new BufferBinding(Descriptor.UniformBuffer, ShaderStage.All));
        }))[0];

        initEvent.awaitSignal(3000L);
        outFrameBuffer.setDepthTarget(VulkanRenderTarget.createDepthTarget(renderDepth));

    }

    private VulkanImageView createPresentDepth() {
        VulkanImage depth = BasicVulkanImage.build(device, GpuImage.Type.TwoDemensional, i -> {
            i.setSize(swapchain.getExtent().x, swapchain.getExtent().y);
            i.setFormat(device.getPhysicalDevice().findSupportedFormat(
                    VulkanImage.Tiling.Optimal, FormatFeature.DepthStencilAttachment,
                    Format.Depth32_SFloat, Format.Depth32_SFloat_Stencil8_UInt, Format.Depth24_UNorm_Stencil8_UInt));
            i.setTiling(VulkanImage.Tiling.Optimal);
            i.setUsage(ImageUsage.DepthStencilAttachment);
            i.setMemoryProps(MemoryProp.DeviceLocal);
        });
        return VulkanImageView.build(depth, ImageView.Type.TwoDemensional, v -> {
            v.setAspect(VulkanImage.Aspect.Depth);
        });
    }

    @Override
    public RenderSession createRenderSession(float tpf) {
        frameCount++;
        return getCurrentFrame().init();
    }

    @Override
    public DescriptorSet createShaderBindings(Consumer<ShaderSetBuilder> builder) {
        DescriptorSetLayout layout = DescriptorSetLayout.build(device, l -> {
            l.setCache(descSetLayoutCache);
            builder.accept(l);
        });
        return descriptorPool.allocateSets(layout)[0];
    }

    private Frame getCurrentFrame() {
        return framesInFlight[(int)(frameCount % framesInFlight.length)];
    }

    public FrameBuffer<?> getOutputFrameBuffer() {
        return outFrameBuffer;
    }

    public class Frame implements RenderSession<SimpleVulkanEngine> {

        private final CommandBuffer graphics = graphicsPool.allocate(CommandBuffer.Level.Primary);
        private final Semaphore imageAcquired = new BinarySemaphore(device);
        private final Semaphore renderComplete = new BinarySemaphore(device);
        private final Fence inFlight = new Fence(device, true);
        private boolean outputAvailable = false;

        protected Frame init() {
            inFlight.block(5000L);
            outputAvailable = outFrameBuffer.acquireNextTarget(imageAcquired, null, 5000L);
            graphics.reset();
            graphics.beginRecording();
            return this;
        }

        @Override
        public void close() {
            if (!outputAvailable) return;
            Swapchain.PresentImage present = outFrameBuffer.getCurrentImage();
            present.transitionLayout(graphics, VulkanImage.Layout.PresentSrc);
            graphics.await(imageAcquired, PipelineStage.ColorAttachmentOutput);
            graphics.signal(renderComplete);
            graphics.endRecording();
            graphics.submit(inFlight);
            swapchain.present(device.getPhysicalDevice().getPresent(), present, renderComplete);
        }

        @Override
        public SimpleVulkanEngine getEngine() {
            return SimpleVulkanEngine.this;
        }

        @Override
        public void render(Collection<ViewPort> viewPorts, Queue<ShadingTechnique> techniques) {
            graphics.stageShaderSets(0, globalBindings.bind());
            for (ViewPort vp : viewPorts) {
                if (!vp.isEnabled()) {
                    continue;
                }
                VulkanFrameBuffer fbo = (VulkanFrameBuffer)vp.getOutputFrameBuffer();
                fbo.beginDynamicRender(graphics,
                        VulkanImage.Load.DontCare, VulkanImage.Store.Store,
                        VulkanImage.Load.DontCare, VulkanImage.Store.DontCare);
                //settings.pushViewPort(vp.getArea());
                //settings.pushScissor(vp.getArea().toScissor(null));
                for (GeometryBucket b : vp.gatherGeometry(s -> s.runControlRender(SimpleVulkanEngine.this, vp))) {
                    //b.setupRender(vp, settings);
                    //settings.applySettings();
                    for (ShadingTechnique t : techniques) {
                        t.render(this, vp, b);
                        if (b.isEmpty()) {
                            break;
                        }
                    }
                    //b.cleanupRender(vp, settings);
                }
                //settings.popViewPort();
                //settings.popScissor();
                fbo.endDynamicRender(graphics);
            }

        }

        @Override
        public GraphicsElement createRenderElement(ViewPort vp, Geometry g, GraphicsState state) {
            return new EngineRenderElement(graphics, vp.getOutputFrameBuffer(), vp.getCamera(), g, state);
        }

        @Override
        public void stageShaderSet(int location, SetBindCommand sets) {
            graphics.stageShaderSets(location, sets);
        }

        @Override
        public void bindShaderSets() {
            graphics.cmdBindStagedSets();
        }

    }

    private class SwapchainUpdater implements Consumer<Swapchain> {

        private final TimelineSemaphore wait = new TimelineSemaphore(device);

        @Override
        public void accept(Swapchain swapchain) {
            swapchain.update();
            VulkanImageView depth = createPresentDepth();
            CommandBuffer cmd = transientGraphicsPool.allocate(CommandBuffer.Level.Primary);
            cmd.beginRecording();
            depth.getImage().transitionLayout(cmd, VulkanImage.Layout.DepthStencilAttachmentOptimal);
            TimelineSemaphore.SignalEvent event = cmd.signalEvent(wait);
            cmd.endRecording();
            cmd.submit();
            event.awaitSignal(1000L);
            outFrameBuffer.setDepthTarget(VulkanRenderTarget.createDepthTarget(depth));
        }

    }

    private class EngineRenderElement extends VulkanRenderElement {

        private final CommandBuffer cmd;
        private final FrameBuffer frameBuffer;
        private final VertexPipeline pipeline;

        public EngineRenderElement(CommandBuffer cmd, FrameBuffer frameBuffer, Camera cam, Geometry g, GraphicsState state) {
            super(cam, g);
            this.cmd = cmd;
            this.frameBuffer = frameBuffer;
            this.pipeline = DynamicGraphicsPipeline.build(device, p -> {
                p.setCache(pipelineCache);
                p.setLayoutCache(pipelineLayoutCache);
                p.setShaderCache(shaderCache);
                p.setSetLayoutCache(descSetLayoutCache);
                p.setLayout(PipelineLayout.build(device, l -> {
                    l.setCache(pipelineLayoutCache);
                    cmd.fillPipelineLayoutSets(l);
                }));
                p.setFrameBuffer(frameBuffer);
                p.setDynamic(DynamicState.ViewPort, true);
                p.setDynamic(DynamicState.Scissor, true);
                p.setDepthClamp(true);
                p.applyGeometry(app.getAssetManager(), getMesh(), getMaterial(), state);
//                p.applyRenderState(new RenderState().integrateGeometryStates(getGeometry(), new RenderState(),
//                        getMaterial().getAdditionalRenderState(), state.getState()));
            });
        }

        @Override
        public void bind() {
            cmd.cmdBindPipeline(pipeline);
        }

        @Override
        public void render() {
            getMesh().render(cmd, pipeline);
        }

        @Override
        public long getPipelineSortPosition() {
            return 0;
        }

    }

}
