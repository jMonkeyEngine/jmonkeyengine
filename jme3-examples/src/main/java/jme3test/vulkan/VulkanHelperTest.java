package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.GlVertexBuffer;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.vulkan.ColorSpace;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.buffers.VersionedBuffer;
import com.jme3.vulkan.buffers.generate.MeshBufferGenerator;
import com.jme3.vulkan.shaderc.ShaderType;
import com.jme3.vulkan.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.texture.ImageView;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.Format;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.PersistentBuffer;
import com.jme3.vulkan.buffers.StageableBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.DeviceFeature;
import com.jme3.vulkan.devices.DeviceFilter;
import com.jme3.vulkan.devices.GeneralPhysicalDevice;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.SingleResource;
import com.jme3.vulkan.frames.UpdateFrame;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.MatrixTransformMaterial;
import com.jme3.vulkan.material.TestMaterial;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pipelines.*;
import com.jme3.vulkan.pipelines.states.*;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.surface.SwapchainUpdater;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.update.CommandBatch;
import com.jme3.vulkan.update.PerFrameCommandBatch;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.lwjgl.vulkan.VK13.*;

public class VulkanHelperTest extends SimpleApplication implements SwapchainUpdater {

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private RenderPass renderPass;
    private GraphicsPipeline pipeline;
    private CommandPool graphicsPool;
    private StageableBuffer vertexBuffer, indexBuffer;
    private UpdateFrameManager<Frame> frames;
    private boolean swapchainResizeFlag = false;
    private boolean applicationStopped = false;

    // framebuffer
    private VulkanImageView depthView;

    // commands
    private CommandBatch graphics, perFrameData, sharedData;
    private Fence sharedDataFence;

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
        swapchain = new Swapchain(device, surface);
        swapchain.build(s -> {
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
        initCommands.begin();

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
            VertexBinding b = m.addBinding(InputRate.Vertex);
            m.addAttribute(b, GlVertexBuffer.Type.Position.getName(), Format.RGB32SFloat, 0);
            m.addAttribute(b, GlVertexBuffer.Type.TexCoord.getName(), Format.RG32SFloat, 1);
            m.addAttribute(b, GlVertexBuffer.Type.Normal.getName(), Format.RGB32SFloat, 2);
        }

        TestMaterial material = new TestMaterial(descriptorPool);

        // pipeline
        PipelineLayout pipelineLayout = new PipelineLayout(device);
        try (PipelineLayout.Builder p = pipelineLayout.build()) {
            p.addMaterial(material);
        }
        ShaderModule vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)));
        ShaderModule fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanFragTest.glsl", ShaderType.Fragment)));
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass.getSubpasses().get(0));
        try (GraphicsPipeline.Builder p = pipeline.build()) {
            p.addShader(vertModule, ShaderStage.Vertex, "main");
            p.addShader(fragModule, ShaderStage.Fragment, "main");
            p.getVertexInput().setMesh(meshDesc);
            p.getRasterization().setCullMode(CullMode.None);
            p.getViewport().addViewport();
            p.getViewport().addScissor();
            p.getColorBlend().addAttachment(new ColorBlendAttachment());
            p.getDynamic().addTypes(DynamicState.Type.ViewPort, DynamicState.Type.Scissor);
        }

        frames = new UpdateFrameManager<>(2, n -> new Frame());

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

        graphics = new PerFrameCommandBatch(frames);
        perFrameData = new PerFrameCommandBatch(frames);
        sharedData = new PerFrameCommandBatch(frames);
        sharedDataFence = new Fence(device, true);

        // set material parameters
        material.getBaseColorMap().setResource(new SingleResource<>(texture));

        // create geometry
        Mesh m = new MyCustomMesh(meshDesc, new MeshBufferGenerator(device, frames, null, sharedData),
                Vector3f.UNIT_Z, Vector3f.UNIT_Y, 1f, 1f, 0.5f, 0.5f);
        MatrixTransformMaterial t = new MatrixTransformMaterial(descriptorPool);
        VersionedBuffer<PersistentBuffer> transformBuffer = new VersionedBuffer<>(frames, MemorySize.floats(16),
                s -> new PersistentBuffer(device, s));
        for (PersistentBuffer buf : transformBuffer) {
            try (PersistentBuffer.Builder b = buf.build()) {
                b.setUsage(BufferUsage.Uniform);
            }
        }
        t.getTransforms().setResource(transformBuffer);
        Geometry geometry = new Geometry("geometry", m, t);
        geometry.setMaterial(material);
        rootNode.attachChild(geometry);

    }

    @Override
    public void stop() {
        applicationStopped = true;
        device.waitIdle();
        Native.get().clear(); // destroy all native objects
        super.stop();
    }

    @Override
    public boolean swapchainOutOfDate(Swapchain swapchain, int imageAcquireCode) {
        if (swapchainResizeFlag || imageAcquireCode == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || imageAcquireCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            swapchainResizeFlag = false;
            swapchain.update();
            CommandBuffer cmd = device.getShortTermPool(device.getPhysicalDevice().getGraphics()).allocateTransientCommandBuffer();
            cmd.begin();
            depthView = createDepthAttachment(cmd);
            cmd.endAndSubmit(SyncGroup.ASYNC);
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
                Format.Depth32SFloat, Format.Depth32SFloat_Stencil8UInt, Format.Depth24UNorm_Stencil8UInt);
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
        private final CommandBuffer perFrameDataCommands = graphicsPool.allocateCommandBuffer();
        private final CommandBuffer sharedDataCommands = graphicsPool.allocateCommandBuffer();
        private final CommandBuffer graphicsCommands = graphicsPool.allocateCommandBuffer();
        private final Semaphore imageAvailable = new Semaphore(device, PipelineStage.ColorAttachmentOutput);
        private final Semaphore perFrameDataFinished = new Semaphore(device, PipelineStage.TopOfPipe);
        private final Semaphore sharedDataFinished = new Semaphore(device, PipelineStage.TopOfPipe);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);

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

            // update geometry matrix transforms
            for (Spatial s : rootNode) {
                if (s instanceof Geometry) {
                    ((Geometry)s).updateMatrixTransforms(viewPort.getCamera());
                }
            }

            // update shared data
            sharedDataFence.block(5000);
            sharedDataCommands.resetAndBegin();
            SyncGroup dataSync = sharedDataFinished.toGroupSignal();
            if (sharedData.run(sharedDataCommands, frames.getCurrentFrame())) {
                sharedDataFence.reset();
                dataSync.setFence(sharedDataFence); // force the next frame to wait
            }
            sharedDataCommands.endAndSubmit(dataSync);

            // update per-frame data
            perFrameDataCommands.resetAndBegin();
            perFrameData.run(perFrameDataCommands, frames.getCurrentFrame());
            perFrameDataCommands.endAndSubmit(perFrameDataFinished.toGroupSignal());

            // begin graphics commands
            graphicsCommands.resetAndBegin();

            // set render pass and pipeline
            renderPass.begin(graphicsCommands, image.getFrameBuffer());
            pipeline.bind(graphicsCommands);

            // viewport and scissor
            try (MemoryStack stack = MemoryStack.stackPush()) {
                viewPort.getCamera().setViewPort(stack, graphicsCommands);
            }

            // run graphics commands via CommandBatch
            graphics.run(graphicsCommands, frames.getCurrentFrame());


            // flatten scene into a render bucket
            List<Geometry> bucket = new ArrayList<>();
            PipelineCache cache;
            for (Spatial s : rootNode) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry)s;
                    g.getMaterial().selectPipeline(cache);
                    bucket.add(g);
                }
            }

            // sort geometries to minimize pipeline switches and overdraw
            Camera cam = viewPort.getCamera();
            bucket.sort((g1, g2) -> {
                Pipeline p1 = g1.getMaterial().getPipeline();
                Pipeline p2 = g2.getMaterial().getPipeline();
                if (p1 == p2) {
                    float dist1 = g1.getWorldTranslation().distanceSquared(cam.getLocation());
                    float dist2 = g2.getWorldTranslation().distanceSquared(cam.getLocation());
                    return Float.compare(dist1, dist2);
                } else {
                    int ph1 = g1.getMaterial().getPipeline().hashCode();
                    int ph2 = g2.getMaterial().getPipeline().hashCode();
                    return Integer.compare(ph1, ph2);
                }
            });

            // render geometries in the sorted order
            Pipeline current = null;
            for (Geometry g : bucket) {
                Pipeline p = g.getMaterial().getPipeline();
                if (p != current) {
                    (current = p).bind(graphicsCommands);
                }
                g.render(renderManager, graphicsCommands);
            }

            // material
            renderPass.end(graphicsCommands);

            // render manager
            graphicsCommands.endAndSubmit(new SyncGroup(new Semaphore[]
                    {imageAvailable, perFrameDataFinished, sharedDataFinished}, renderFinished, inFlight));
            swapchain.present(device.getPhysicalDevice().getPresent(), image, renderFinished.toGroupWait());

        }

    }

}
