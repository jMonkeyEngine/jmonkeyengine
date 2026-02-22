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
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.texture.ImageView;
import com.jme3.texture.Texture;
import com.jme3.util.TempVars;
import com.jme3.util.struct.*;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.VulkanLogger;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.HostVisibleBuffer;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.buffers.stream.StreamingBuffer;
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
import com.jme3.vulkan.formats.VulkanEnums;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.NewMaterial;
import com.jme3.vulkan.material.NewMaterialDef;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.uniforms.TextureUniform;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.material.uniforms.StructUniform;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attribute.*;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;
import com.jme3.vulkan.pipeline.framebuffer.OutputFrameBuffer;
import com.jme3.vulkan.pipeline.graphics.GraphicsPipeline;
import com.jme3.vulkan.render.*;
import com.jme3.vulkan.render.bucket.GeometryBucket;
import com.jme3.vulkan.render.bucket.VulkanBucketElement;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.sync.BinarySemaphore;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.TimelineSemaphore;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.EXTRobustness2;
import org.lwjgl.vulkan.VkPhysicalDeviceRobustness2FeaturesEXT;

import java.util.*;
import java.util.function.Consumer;

public class SimpleVulkanEngine implements RenderEngine {

    public static class LightData implements Struct {

        private Light light;
        @Member(0) public final ColorRGBA color = new ColorRGBA();
        @Member(1) public final Vector4f position = new Vector4f();
        @Member(2) public final Vector4f direction = new Vector4f();

        public LightData() {}

        public LightData(Light light) {
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
            color.set(light.getColor());
            color.a = light.getType().getId();
            direction.set(light.getDirection()); // this breaks protocol with jme's shaders
        }

        public void set(PointLight light) {
            this.light = light;
            color.set(light.getColor());
            color.a = light.getType().getId();
            position.set(light.getPosition());
            position.w = light.getInvRadius();
        }

        public void set(SpotLight light) {
            this.light = light;
            color.set(light.getColor());
            color.a = light.getType().getId();
            position.set(light.getPosition());
            position.w = light.getInvSpotRange();
            direction.set(light.getDirection());
            direction.w = light.getPackedAngleCos();
        }

        public Light getLight() {
            return light;
        }

    }

    public static class Lighting implements Struct {
        @Member(0) public final List<LightData> lights = new ArrayList<>();
        @Member(1) public final List<Integer> indices = new ArrayList<>();
    }

    public static class Transforms implements Struct {
        @Member(0) public final Matrix4f worldViewProjectionMatrix = new Matrix4f();
        @Member(1) public final Matrix4f viewProjectionMatrix = new Matrix4f();
    }

    private final Application app;
    private final Frame[] framesInFlight;
    private long frameCount = -1;

    private VulkanInstance instance;
    private VulkanLogger logger;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private DescriptorPool descriptorPool;
    private CommandPool graphicsPool, transientGraphicsPool;
    private RenderPass renderPass;
    private BufferStream stream;
    private MeshLayout meshLayout;
    private OutputFrameBuffer outFrameBuffer;
    private final BufferGenerator<VulkanBuffer> bufferGen = new BufferGeneratorImpl();

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

        // assign vulkan enums to engine enums
        VulkanEnums.setup();

        app.getAssetManager().registerLoader(VulkanMaterialLoader.class, "j4md");

        instance = VulkanInstance.build(VulkanInstance.Version.v11, i -> {
            i.addGlfwExtensions();
            i.addDebugExtension();
            i.addLunarGLayer();
        });
        logger = new VulkanLogger(instance, VulkanLogger.Severity.All, VulkanLogger.Type.All);

        surface = new Surface(instance, app);

        device = LogicalDevice.build(instance, id -> new GeneralPhysicalDevice(instance, surface, id), d -> {
            d.addFilter(surface);
            d.addFilter(DeviceFilter.swapchain(surface));
            d.addCriticalExtension(Swapchain.EXTENSION_NAME);
            d.addOptionalExtension(EXTRobustness2.VK_EXT_ROBUSTNESS_2_EXTENSION_NAME, 1f);
            d.addFeatureContainer(p -> VkPhysicalDeviceRobustness2FeaturesEXT.calloc().pNext(p));
            d.addFeature(DeviceFeature.anisotropy(1f, true));
            d.addFeature(DeviceFeature.nullDescriptor(1f, false));
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

        renderPass = RenderPass.build(device, r -> {
            Attachment sceneColor = r.createAttachment(Format.RGBA8_SRGB, 1, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.DontCare);
                a.setInitialLayout(VulkanImage.Layout.Undefined);
                a.setFinalLayout(VulkanImage.Layout.ColorAttachmentOptimal);
            });
            Attachment depth = r.createAttachment(renderDepth.getImage().getFormat(), 1, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.DontCare);
                a.setInitialLayout(VulkanImage.Layout.Undefined);
                a.setFinalLayout(VulkanImage.Layout.DepthStencilAttachmentOptimal);
            });
            Subpass scene = r.createSubpass(PipelineBindPoint.Graphics, s -> {
                s.addColorAttachment(sceneColor.createReference(VulkanImage.Layout.ColorAttachmentOptimal));
                s.setDepthStencilAttachment(depth.createReference(VulkanImage.Layout.DepthStencilAttachmentOptimal));
            });
            r.createDependency(null, scene, d -> {
                d.setSrcStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setSrcAccessMask(Flag.of(scene.getPosition()));
                d.setDstStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setDstAccessMask(Flag.of(Access.ColorAttachmentWrite, Access.DepthStencilAttachmentWrite));
            });
        });

        meshLayout = MeshLayout.build(m -> {
            m.addBinding(VertexBinding.build(this, InputRate.Vertex, v -> {
                v.add(GlVertexBuffer.Type.Position, Format.RGB32_SFloat, i -> new Position(ValueMapper.Float32, i));
                v.add(GlVertexBuffer.Type.TexCoord, Format.RG32_SFloat, i -> new TexCoord(ValueMapper.Float32, i));
            }));
            m.addBinding(VertexBinding.build(this, InputRate.Vertex, v -> {
                v.add(GlVertexBuffer.Type.Normal, Format.RGB32_SFloat, i -> new Normal(ValueMapper.Float32, i));
                v.add(GlVertexBuffer.Type.Tangent, Format.RGBA32_SFloat, i -> new Tangent(ValueMapper.Float32, i));
            }));
        });

        outFrameBuffer = new OutputFrameBuffer(swapchain, renderPass);
        stream = new BufferStream(device, 500);
        for (int i = 0; i < framesInFlight.length; i++) {
            framesInFlight[i] = new Frame();
        }

        initEvent.awaitSignal(3000L);
        outFrameBuffer.setDepthTarget(renderDepth);

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
    public Mesh createMesh(int vertices, int instances) {
        return new AdaptiveMesh(meshLayout, vertices, instances);
    }

    @Override
    public <T extends Struct> Uniform<T> createUniformBuffer(StructLayout layout, T struct) {
        return new StructUniform<>(Descriptor.UniformBuffer, BufferUsage.Uniform, layout, struct, bufferGen);
    }

    @Override
    public <T extends Struct> Uniform<T> createShaderStorageUniform(StructLayout layout, T struct) {
        return new StructUniform<>(Descriptor.StorageBuffer, BufferUsage.Storage, layout, struct, bufferGen);
    }

    @Override
    public Uniform<Texture> createTextureUniform() {
        return new TextureUniform(VulkanImage.Layout.ShaderReadOnlyOptimal);
    }

    @Override
    public VulkanBuffer createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage) {
        return bufferGen.createBuffer(size, bufUsage, dataUsage);
    }

    @Override
    public void render(Collection<ViewPort> viewPorts) {
        frameCount++;
        getCurrentFrame().render(viewPorts);
    }

    private Frame getCurrentFrame() {
        return framesInFlight[(int)(frameCount % framesInFlight.length)];
    }

    public void setMeshLayout(MeshLayout meshLayout) {
        this.meshLayout = meshLayout;
    }

    public FrameBuffer<?> getOutputFrameBuffer() {
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
            if (!outFrameBuffer.acquireNextImage(imageAcquired, null, 5000L)) {
                return false;
            }
            graphics.reset();
            graphics.beginRecording();
            renderPass.begin(graphics, outFrameBuffer);
            for (ViewPort vp : viewPorts) {
                render(vp);
            }
            renderPass.end(graphics);
            graphics.await(imageAcquired, PipelineStage.ColorAttachmentOutput);
            graphics.signal(renderComplete);
            graphics.endRecording();
            graphics.submit(inFlight);
            swapchain.present(device.getPhysicalDevice().getPresent(), outFrameBuffer.getCurrentImage(), renderComplete);
            return true;
        }

        private void render(ViewPort vp) {
            if (!vp.isEnabled() || vp.getScenes().isEmpty()) {
                return;
            }
            lighting.lights.clear();
            for (Spatial scene : vp.getScenes()) for (Spatial child : scene) {
                for (Light l : child.getLocalLightList()) {
                    lighting.lights.add(new LightData(l));
                }
            }
            settings.pushViewPort(vp.getArea());
            settings.pushScissor(vp.getArea().toScissor(null));
            TempVars vars = TempVars.get();
            VertexPipeline currentPipeline = null;
            VulkanMaterial currentMaterial = null;
            for (GeometryBucket b : vp.gatherGeometry(s -> s.runControlRender(SimpleVulkanEngine.this, vp))) {
                b.setupRender(vp, settings);
                settings.applySettings();
                for (VulkanBucketElement e : b.sort(g -> new EngineBucketElement(vp.getCamera(), g, null))) {
                    if (e.getPipeline() != currentPipeline) {
                        (currentPipeline = e.getPipeline()).bind(graphics);
                        currentMaterial = null;
                    }
                    if (e.getMaterial() != currentMaterial) {
                        (currentMaterial = e.getMaterial()).bind(graphics, currentPipeline, descriptorPool);
                    }
                    lighting.indices.clear();
                    int lightIndex = 0;
                    for (LightData l : lighting.lights) {
                        if (l.getLight().intersectsVolume(e.getGeometry().getWorldBound(), vars)) {
                            lighting.indices.add(lightIndex);
                        }
                        lightIndex++;
                    }
                    transforms.viewProjectionMatrix.mult(e.getGeometry().getWorldMatrix(), transforms.worldViewProjectionMatrix);
                    e.getMaterial().set("Lighting", lighting);
                    e.getMaterial().set("Transforms", transforms);
                    stream.stream(graphics); // this is a problem because it can bypass wrappers
                    e.getMesh().render(graphics, currentPipeline);
                }
                b.cleanupRender(vp, settings);
            }
            vars.release();
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
            outFrameBuffer.setDepthTarget(depth);
        }

    }

    private class BufferGeneratorImpl implements BufferGenerator<VulkanBuffer> {

        @Override
        public VulkanBuffer createBuffer(MemorySize size, Flag<BufferUsage> bufUsage, GlVertexBuffer.Usage dataUsage) {
            switch (dataUsage) {
                case Static: case Dynamic: {
                    return stream.add(new StreamingBuffer(device, size, bufUsage));
                }
                case Stream: {
                    return new PersistentVulkanBuffer<>(HostVisibleBuffer.build(
                            device, size, b -> b.setUsage(BufferUsage.Vertex)));
                }
                case CpuOnly: throw new IllegalArgumentException("Cannot create cpu-only buffer for Vulkan.");
                default: throw new UnsupportedOperationException("Unrecognized: " + dataUsage);
            }
        }

    }

    private class EngineBucketElement extends VulkanBucketElement {

        public EngineBucketElement(Camera camera, Geometry geometry, String technique) {
            super(camera, geometry, technique);
        }

        @Override
        protected VertexPipeline createPipeline() {
            return GraphicsPipeline.build(device, p -> {
                p.setCache(pipelineCache);
                p.setLayoutCache(pipelineLayoutCache);
                p.setShaderCache(shaderCache);
                p.setSetLayoutCache(descSetLayoutCache);
                p.setSubpass(renderPass.getSubpass(0));
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
