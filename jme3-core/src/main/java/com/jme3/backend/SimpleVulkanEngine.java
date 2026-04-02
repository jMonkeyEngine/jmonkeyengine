package com.jme3.backend;

import com.jme3.app.Application;
import com.jme3.asset.AssetKey;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.RenderState;
import com.jme3.material.plugins.VulkanMaterialLoader;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.ImageView;
import com.jme3.util.TempVars;
import com.jme3.util.struct.*;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.VulkanLogger;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.*;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorPool;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.PoolSize;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.material.NewMaterialDef;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.pipeline.framebuffer.*;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.render.bucket.VulkanBucketElement;
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

    public static class LightData extends Struct {

        private Light light;
        public final Field<ColorRGBA> color = new Field<>(new ColorRGBA());
        public final Field<Vector4f> position = new Field<>(new Vector4f());
        public final Field<Vector4f> direction = new Field<>(new Vector4f());

        public LightData(Light light) {
            addFields(color, position, direction);
            set(light);
        }

        public void set(Light light) {
            if (light instanceof DirectionalLight) {
                set((DirectionalLight)light);
            } else if (light instanceof PointLight) {
                set((PointLight)light);
            } else if (light instanceof SpotLight) {
                set((SpotLight)light);
            }
        }

        public void set(DirectionalLight light) {
            this.light = light;
            color.get().set(light.getColor());
            color.get().a = light.getType().getId();
            direction.get().set(light.getDirection()); // this breaks protocol with jme's shaders
        }

        public void set(PointLight light) {
            this.light = light;
            color.get().set(light.getColor());
            color.get().a = light.getType().getId();
            position.get().set(light.getPosition());
            position.get().w = light.getInvRadius();
        }

        public void set(SpotLight light) {
            this.light = light;
            color.get().set(light.getColor());
            color.get().a = light.getType().getId();
            position.get().set(light.getPosition());
            position.get().w = light.getInvSpotRange();
            direction.get().set(light.getDirection());
            direction.get().w = light.getPackedAngleCos();
        }

        public Light getLight() {
            return light;
        }

    }

    public static class Lighting extends Struct {

        public final Field<List<LightData>> lights = new Field<>(new ArrayList<>());
        public final Field<List<Integer>> indices = new Field<>(new ArrayList<>());

        public Lighting() {
            addFields(lights, indices);
        }

    }

    public static class Transforms extends Struct {

        public final Field<Matrix4f> worldViewProjectionMatrix = new Field<>(new Matrix4f());
        public final Field<Matrix4f> viewProjectionMatrix = new Field<>(new Matrix4f());

        public Transforms() {
            addFields(worldViewProjectionMatrix, viewProjectionMatrix);
        }

    }

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

    private final Cache<Pipeline> pipelineCache = new Cache<>();
    private final Cache<PipelineLayout> pipelineLayoutCache = new Cache<>();
    private final Cache<ShaderModule> shaderCache = new Cache<>();
    private final Cache<DescriptorSetLayout> descSetLayoutCache = new Cache<>();

    private final Lighting lighting = new Lighting();
    private final Transforms transforms = new Transforms();

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
            d.addOptionalExtension(EXTRobustness2.VK_EXT_ROBUSTNESS_2_EXTENSION_NAME, 1f);
            d.addFeatureContainer(p -> VkPhysicalDeviceRobustness2FeaturesEXT.calloc().pNext(p));
            d.addFeatureContainer(p -> VkPhysicalDeviceDynamicRenderingFeatures.calloc().pNext(p));
            d.addFeature(DeviceFeature.anisotropy(1f));
            d.addFeature(DeviceFeature.nullDescriptor(1f));
            d.addFeature(DeviceFeature.dynamicRendering(null));
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
    public VulkanMaterial createMaterial() {
        return new NewMaterial();
    }

    @Override
    public VulkanMaterial createMaterial(String matdefName) {
        return app.getAssetManager().loadAsset(new AssetKey<NewMaterialDef<VulkanMaterial>>(matdefName)).createMaterial(this);
    }

    @Override
    public void render(Collection<ViewPort> viewPorts) {
        frameCount++;
        getCurrentFrame().render(viewPorts);
    }

    private Frame getCurrentFrame() {
        return framesInFlight[(int)(frameCount % framesInFlight.length)];
    }

    public com.jme3.vulkan.pipeline.framebuffer.FrameBuffer<?> getOutputFrameBuffer() {
        return outFrameBuffer;
    }

    protected class Frame {

        private final CommandBuffer graphics = graphicsPool.allocate(CommandBuffer.Level.Primary);
        private final Semaphore imageAcquired = new BinarySemaphore(device);
        private final Semaphore renderComplete = new BinarySemaphore(device);
        private final Fence inFlight = new Fence(device, true);
        private final StandardRenderSettings settings = new VulkanRenderSettings(graphics);

        public boolean render(Collection<ViewPort> viewPorts) {
            inFlight.block(5000L);
            if (!outFrameBuffer.acquireNextTarget(imageAcquired, null, 5000L)) {
                return false;
            }
            graphics.reset();
            graphics.beginRecording();
            for (ViewPort vp : viewPorts) {
                render(vp);
            }
            Swapchain.PresentImage present = outFrameBuffer.getCurrentImage();
            present.transitionLayout(graphics, VulkanImage.Layout.PresentSrc);
            graphics.await(imageAcquired, PipelineStage.ColorAttachmentOutput);
            graphics.signal(renderComplete);
            graphics.endRecording();
            graphics.submit(inFlight);
            swapchain.present(device.getPhysicalDevice().getPresent(), present, renderComplete);
            return true;
        }

        private void render(ViewPort vp) {
            if (!vp.isEnabled() || vp.getScenes().isEmpty()) {
                return;
            }
            VulkanFrameBuffer fbo = (VulkanFrameBuffer)vp.getOutputFrameBuffer();
            fbo.beginDynamicRender(graphics,
                    VulkanImage.Load.DontCare, VulkanImage.Store.Store,
                    VulkanImage.Load.DontCare, VulkanImage.Store.DontCare);
            transforms.viewProjectionMatrix.alias().set(vp.getCamera().getViewProjectionMatrix());
            transforms.viewProjectionMatrix.set();
            lighting.lights.alias().clear();
            for (Spatial scene : vp.getScenes()) for (Spatial child : scene) {
                for (Light l : child.getLocalLightList()) {
                    lighting.lights.alias().add(new LightData(l));
                }
            }
            lighting.computeOffsets();
            lighting.lights.set();
            settings.pushViewPort(vp.getArea());
            settings.pushScissor(vp.getArea().toScissor(null));
            TempVars vars = TempVars.get();
            VertexPipeline currentPipeline = null;
            VulkanMaterial currentMaterial = null;
            for (GeometryBucket b : vp.gatherGeometry(s -> s.runControlRender(SimpleVulkanEngine.this, vp))) {
                b.setupRender(vp, settings);
                settings.applySettings();
                for (VulkanBucketElement e : b.sort(g -> new EngineBucketElement(vp.getCamera(), g, null, vp.getOutputFrameBuffer()))) {
                    if (e.getPipeline() != currentPipeline) {
                        (currentPipeline = e.getPipeline()).bind(graphics);
                        currentMaterial = null;
                    }
                    if (e.getMaterial() != currentMaterial) {
                        (currentMaterial = e.getMaterial()).bind(graphics, currentPipeline, descriptorPool);
                    }
                    lighting.indices.alias().clear();
                    int lightIndex = 0;
                    for (LightData l : lighting.lights.get()) {
                        if (l.getLight().intersectsVolume(e.getGeometry().getWorldBound(), vars)) {
                            lighting.indices.alias().add(lightIndex);
                        }
                        lightIndex++;
                    }
                    lighting.computeOffsets();
                    lighting.indices.set();
                    transforms.viewProjectionMatrix.alias().mult(e.getGeometry().getWorldMatrix(),
                            transforms.worldViewProjectionMatrix.alias());
                    transforms.worldViewProjectionMatrix.set();
                    e.getMaterial().set("Lighting", lighting);
                    e.getMaterial().set("Transforms", transforms);
                    graphics.uploadBuffers(stream);
                    e.getMesh().render(graphics, currentPipeline);
                }
                b.cleanupRender(vp, settings);
            }
            vars.release();
            settings.popViewPort();
            settings.popScissor();
            fbo.endDynamicRender(graphics);
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

    private class EngineBucketElement extends VulkanBucketElement {

        private final FrameBuffer frameBuffer;

        public EngineBucketElement(Camera camera, Geometry geometry, String technique, FrameBuffer frameBuffer) {
            super(camera, geometry, technique);
            this.frameBuffer = frameBuffer;
        }

        @Override
        protected VertexPipeline createPipeline() {
            return GraphicsPipeline.build(device, p -> {
                p.setFrameBuffer(frameBuffer);
                p.setCache(pipelineCache);
                p.setLayoutCache(pipelineLayoutCache);
                p.setShaderCache(shaderCache);
                p.setSetLayoutCache(descSetLayoutCache);
                p.setDynamic(DynamicState.ViewPort, true);
                p.setDynamic(DynamicState.Scissor, true);
                p.setDepthClamp(true);
                p.applyGeometry(app.getAssetManager(), getMesh(), getMaterial(), getTechnique());
                p.applyRenderState(new RenderState().integrateGeometryStates(getGeometry(), new RenderState(),
                        getMaterial().getAdditionalRenderState(), getTechnique().getRenderState()));
            });
        }

    }

}
