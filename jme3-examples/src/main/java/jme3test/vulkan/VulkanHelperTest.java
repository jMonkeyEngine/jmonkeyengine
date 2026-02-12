package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.pipeline.cache.PipelineCache;
import com.jme3.vulkan.pipeline.graphics.ColorBlendAttachment;
import com.jme3.vulkan.pipeline.graphics.GraphicsState;
import com.jme3.vulkan.pipeline.states.IShaderState;
import com.jme3.vulkan.render.GeometryBatch;
import com.jme3.vulkan.render.VulkanGeometryBatch;
import com.jme3.vulkan.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.texture.ImageView;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.UpdateFrame;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.MatrixTransformMaterial;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pipeline.*;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.surface.SwapchainUpdater;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.update.BasicCommandBatch;
import com.jme3.vulkan.update.CommandBatch;
import com.jme3.vulkan.update.CommandRunner;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.Comparator;
import java.util.logging.Level;

import static org.lwjgl.vulkan.VK13.*;

public class VulkanHelperTest extends SimpleApplication implements SwapchainUpdater {

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private RenderPass renderPass;
    private CommandPool graphicsPool;
    private PipelineCache pipelineCache;
    private UpdateFrameManager<Frame> frames;
    private boolean swapchainResizeFlag = false;
    private boolean applicationStopped = false;

    // framebuffer
    private VulkanImageView depthView;

    // commands
    private CommandBatch graphics, perFrameData, sharedData;
    private Fence sharedDataFence;

    // render
    private final Comparator<VulkanGeometryBatch.Element> batchSorter = (o1, o2) -> {
        int compare = Long.compare(o1.getPipeline().getSortId(), o2.getPipeline().getSortId());
        if (compare != 0) return compare;
        return Float.compare(o1.computeDistanceSq(), o2.computeDistanceSq());
    };

    public static void main(String[] args) {
        VulkanHelperTest app = new VulkanHelperTest();
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(0);
        settings.setVSync(false);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setRenderer("CUSTOM" + LwjglVulkanContext.class.getName());
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    public VulkanHelperTest() {
        super(new FlyCamAppState());
    }

    @Override
    public void simpleInitApp() {

        assetManager.registerLoader(ShadercLoader.class, "glsl");
        assetManager.registerLoader(VulkanImageLoader.class, "png", "jpg");
        flyCam.setMoveSpeed(5f);
        flyCam.setDragToRotate(true);

        long window = ((LwjglVulkanContext)context).getWindowHandle();

        instance = new VulkanInstance(VK_API_VERSION_1_3);
        try (VulkanInstance.Builder i = instance.build()) {
            i.addGlfwExtensions();
            i.addDebugExtension();
            i.addLunarGLayer();
            i.setApplicationName(VulkanHelperTest.class.getSimpleName());
            i.setApplicationVersion(1, 0, 0);
        }
        instance.createLogger(Level.SEVERE);

        // surface
        surface = new Surface(instance, window);

        // logical device
        device = new LogicalDevice<>(instance);
        try (LogicalDevice.Builder d = device.build(id -> new GeneralPhysicalDevice(instance, surface, id))) {
            d.addFilter(surface);
            d.addFilter(DeviceFilter.swapchain(surface));
            d.addCriticalExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            d.addFeature(DeviceFeature.anisotropy(1f, true));
        }
        GeneralPhysicalDevice physDevice = device.getPhysicalDevice();

        graphicsPool = device.getLongTermPool(physDevice.getGraphics());

        // swapchain
        swapchain = Swapchain.build(device, surface, s -> {
            s.addQueue(physDevice.getGraphics());
            s.addQueue(physDevice.getPresent());
            s.selectFormat(Swapchain.format(Format.B8G8R8A8_SRGB, ColorSpace.KhrSrgbNonlinear));
            s.selectMode(Swapchain.PresentMode.Mailbox);
            s.selectExtentByWindow();
            s.selectImageCount(2);
        });

        DescriptorPool descriptorPool = new DescriptorPool(device, 10,
                new PoolSize(Descriptor.UniformBuffer, 3),
                new PoolSize(Descriptor.StorageBuffer, 4),
                new PoolSize(Descriptor.CombinedImageSampler, 2));

        CommandPool initPool = device.getShortTermPool(physDevice.getGraphics());
        CommandBuffer initCommands = initPool.allocateTransientCommandBuffer();
        initCommands.beginRecording();

        // depth texture
        depthView = createDepthAttachment(initCommands);

        initCommands.endAndSubmit(SyncGroup.ASYNC);
        initCommands.getPool().getQueue().waitIdle();

        // render pass
        renderPass = new RenderPass(device);
        try (RenderPass.Builder p = renderPass.build()) {
            Attachment color = p.createAttachment(swapchain.getFormat(), VK_SAMPLE_COUNT_1_BIT, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.Store);
                a.setStencilLoad(VulkanImage.Load.DontCare);
                a.setStencilStore(VulkanImage.Store.DontCare);
                a.setInitialLayout(VulkanImage.Layout.Undefined);
                a.setFinalLayout(VulkanImage.Layout.PresentSrc);
                a.setClearColor(ColorRGBA.Black);
            });
            Attachment depth = p.createAttachment(depthView.getImage().getFormat(), VK_SAMPLE_COUNT_1_BIT, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.DontCare);
                a.setStencilLoad(VulkanImage.Load.DontCare);
                a.setStencilStore(VulkanImage.Store.DontCare);
                a.setInitialLayout(VulkanImage.Layout.Undefined);
                a.setFinalLayout(VulkanImage.Layout.DepthStencilAttachmentOptimal);
                a.setClearDepth(1f);
            });
            Subpass subpass = p.createSubpass(PipelineBindPoint.Graphics, s -> {
                s.addColorAttachment(color.createReference(VulkanImage.Layout.ColorAttachmentOptimal));
                s.setDepthStencilAttachment(depth.createReference(VulkanImage.Layout.DepthStencilAttachmentOptimal));
            });
            p.createDependency(null, subpass, d -> {
                d.setSrcStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setSrcAccessMask(Flag.of(subpass.getPosition()));
                d.setDstStageMask(Flag.of(PipelineStage.ColorAttachmentOutput, PipelineStage.EarlyFragmentTests));
                d.setDstAccessMask(Flag.of(Access.ColorAttachmentWrite, Access.DepthStencilAttachmentWrite));
            });
        }

        swapchain.createFrameBuffers(renderPass, depthView);

        // mesh description
        MeshDescription meshDesc = new MeshDescription();
        try (MeshDescription.Builder m = meshDesc.build()) {
            OldVertexBinding b = m.addBinding(InputRate.Vertex);
            m.addAttribute(b, GlVertexBuffer.Type.Position.getName(), Format.RGB32_SFloat, 0);
            m.addAttribute(b, GlVertexBuffer.Type.TexCoord.getName(), Format.RG32_SFloat, 1);
            m.addAttribute(b, GlVertexBuffer.Type.Normal.getName(), Format.RGB32_SFloat, 2);
        }

        TestMaterial material = new TestMaterial(descriptorPool);
        PipelineLayout pipelineLayout = new PipelineLayout(device);
        try (PipelineLayout.Builder p = pipelineLayout.build()) {
            p.supportMaterial(material);
        }

        GraphicsState state = new GraphicsState();
        state.setLayout(pipelineLayout);
        state.setSubpass(renderPass.getSubpasses().get(0));
        state.addShader(new IShaderState("Shaders/VulkanVertTest.glsl", "main", ShaderStage.Vertex));
        state.addShader(new IShaderState("Shaders/VulkanFragTest.glsl", "main", ShaderStage.Fragment));
        state.setBlendAttachment(0, new ColorBlendAttachment());
        state.setCullMode(CullMode.None);
        state.setViewPort(0);
        state.setScissor(0);
        state.addDynamic(DynamicState.ViewPort);
        state.addDynamic(DynamicState.Scissor);
        material.setTechnique(TechniqueDef.DEFAULT_TECHNIQUE_NAME, state);

        frames = new UpdateFrameManager<>(2, Frame::new);

        // material color texture
        VulkanImage image = assetManager.loadAsset(VulkanImageLoader.key(initPool, "Common/Textures/MissingTexture.png"));
        VulkanImageView imgView = new VulkanImageView(image, ImageView.Type.TwoDemensional);
        try (VulkanImageView.Builder i = imgView.build()) {
            i.setAspect(VulkanImage.Aspect.Color);
        }
        VulkanTexture texture = new VulkanTexture(device, imgView);
        try (Sampler.Builder t = texture.build()) {
            t.setMinMagFilters(Filter.Linear, Filter.Linear);
            t.setEdgeModes(AddressMode.Repeat);
            t.setMipmapMode(MipmapMode.Linear);
        }

        graphics = new BasicCommandBatch();
        perFrameData = new BasicCommandBatch();
        sharedData = new BasicCommandBatch();
        sharedDataFence = new Fence(device, true);

        // set material parameters
        material.getBaseColorMap().set(texture);

        // create geometry
        Mesh m = new MyCustomMesh(meshDesc, new MeshBufferGenerator(device, frames, null, sharedData),
                Vector3f.UNIT_Z, Vector3f.UNIT_Y, 1f, 1f, 0.5f, 0.5f);
        MatrixTransformMaterial t = new MatrixTransformMaterial(descriptorPool);
        PerFrameBuffer<OldPersistentBuffer> transformBuffer = new PerFrameBuffer<>(frames, MemorySize.floats(16),
                s -> new OldPersistentBuffer(device, s));
        for (OldPersistentBuffer buf : transformBuffer) {
            try (OldPersistentBuffer.Builder b = buf.build()) {
                b.setUsage(BufferUsage.Uniform);
            }
        }
        t.getTransforms().set(transformBuffer);
        Geometry geometry = new Geometry("geometry", m, t);
        geometry.setMaterial(material);
        rootNode.attachChild(geometry);

        pipelineCache = new PipelineCache(device, assetManager);

    }

    @Override
    public void stop() {
        applicationStopped = true;
        device.waitIdle();
        Native.get().clear(); // destroy all native objects
        super.stop();
    }

    @Override
    public boolean outOfDate(Swapchain swapchain, int imageAcquireCode) {
        if (swapchainResizeFlag || imageAcquireCode == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || imageAcquireCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            swapchainResizeFlag = false;
            swapchain.update();
            CommandBuffer cmd = device.getShortTermPool(device.getPhysicalDevice().getGraphics()).allocateTransientCommandBuffer();
            cmd.beginRecording();
            depthView = createDepthAttachment(cmd);
            cmd.endAndSubmit();
            cmd.getPool().getQueue().waitIdle();
            swapchain.createFrameBuffers(renderPass, depthView);
            return true;
        }
        if (imageAcquireCode != VK_SUCCESS) {
            throw new RuntimeException("Failed to acquire swapchain image.");
        }
        return false;
    }

    @Override
    public void reshape(int w, int h) {
        swapchainResizeFlag = true;
    }

    @Override
    public void simpleUpdate(float tpf) {
        frames.update(tpf);
    }

    private VulkanImageView createDepthAttachment(CommandBuffer cmd) {
        Format depthFormat = device.getPhysicalDevice().findSupportedFormat(
                VulkanImage.Tiling.Optimal, FormatFeature.DepthStencilAttachment,
                Format.Depth32_SFloat, Format.Depth32_SFloat_Stencil8_UInt, Format.Depth24_UNorm_Stencil8_UInt);
        BasicVulkanImage image = new BasicVulkanImage(device, VulkanImage.Type.TwoDemensional);
        try (BasicVulkanImage.Builder i = image.build()) {
            i.setSize(swapchain.getExtent().x, swapchain.getExtent().y);
            i.setFormat(depthFormat);
            i.setTiling(VulkanImage.Tiling.Optimal);
            i.setUsage(ImageUsage.DepthStencilAttachment);
            i.setMemoryProps(MemoryProp.DeviceLocal);
        }
        VulkanImageView view = new VulkanImageView(image, ImageView.Type.TwoDemensional);
        try (VulkanImageView.Builder v = view.build()) {
            v.setAspect(VulkanImage.Aspect.Depth);
        }
        image.transitionLayout(cmd, VulkanImage.Layout.DepthStencilAttachmentOptimal);
        return view;
    }

    private class Frame implements UpdateFrame {

        // render manager
        private final CommandBuffer graphicsCommands = graphicsPool.allocateCommandBuffer();
        private final CommandRunner perFrameDataCommands, sharedDataCommands;
        private final Semaphore imageAvailable = new Semaphore(device, PipelineStage.ColorAttachmentOutput);
        private final Semaphore perFrameDataFinished = new Semaphore(device, PipelineStage.TopOfPipe);
        private final Semaphore sharedDataFinished = new Semaphore(device, PipelineStage.TopOfPipe);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);

        public Frame(int frame) {
            perFrameDataCommands = new CommandRunner(frame, graphicsPool.allocateCommandBuffer(), perFrameData);
            sharedDataCommands = new CommandRunner(frame, graphicsPool.allocateCommandBuffer(), sharedData);
        }

        @Override
        public void update(UpdateFrameManager frames, float tpf) {

            // block until this frame has fully completed previous rendering commands
            if (applicationStopped) return;
            inFlight.block(5000);
            if (applicationStopped) return;

            // get swapchain image to present with
            Swapchain.PresentImage image = swapchain.acquireNextImage(VulkanHelperTest.this, imageAvailable, null, 5000);
            if (image == null) {
                return; // no image available: skip rendering this frame
            }
            inFlight.reset();

            // update viewport camera
            viewPort.getCamera().update();
            viewPort.getCamera().updateViewProjection();

            // update data
            sharedDataFence.block(5000);
            sharedDataCommands.run(sharedDataFence.toGroup(), c -> sharedDataFence.reset());
            perFrameDataCommands.run(perFrameDataFinished.toGroupSignal());

            // begin rendering
            graphicsCommands.resetAndBegin();
            renderPass.begin(graphicsCommands, image.getFrameBuffer());

            // viewport and scissor
            try (MemoryStack stack = MemoryStack.stackPush()) {
                viewPort.getCamera().setViewPortAndScissor(stack, graphicsCommands);
            }

            // run misc graphics commands via CommandBatch
            graphics.run(graphicsCommands, frames.getCurrentFrame());

            // render geometry under rootNode
            GeometryBatch<?> batch = new VulkanGeometryBatch(pipelineCache, cam, batchSorter);
            batch.addAll(rootNode);
            batch.render(graphicsCommands);

            // end rendering
            renderPass.end(graphicsCommands);
            graphicsCommands.endAndSubmit(new SyncGroup(new Semaphore[] {imageAvailable, perFrameDataFinished,
                    sharedDataFinished}, renderFinished, inFlight));
            swapchain.present(device.getPhysicalDevice().getPresent(), image, renderFinished.toGroupWait());

        }

    }

}
