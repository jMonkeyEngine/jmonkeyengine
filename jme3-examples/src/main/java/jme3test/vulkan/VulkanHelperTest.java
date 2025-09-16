package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
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
import com.jme3.vulkan.pipelines.states.ColorBlendAttachment;
import com.jme3.vulkan.pipelines.states.DynamicState;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.surface.Swapchain;
import com.jme3.vulkan.surface.SwapchainUpdater;
import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.SyncGroup;
import com.jme3.vulkan.update.CommandBatch;
import com.jme3.vulkan.update.BasicCommandBatch;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.logging.Level;

import static org.lwjgl.vulkan.VK13.*;

public class VulkanHelperTest extends SimpleApplication implements SwapchainUpdater {

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice<GeneralPhysicalDevice> device;
    private Swapchain swapchain;
    private ShaderModule vertModule, fragModule;
    private PipelineLayout pipelineLayout;
    private RenderPass renderPass;
    private GraphicsPipeline pipeline;
    private CommandPool graphicsPool;
    private StageableBuffer vertexBuffer, indexBuffer;
    private DescriptorPool descriptorPool;
    private DescriptorSetLayout descriptorLayout;
    private UpdateFrameManager frames;
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
            s.selectFormat(Format.B8G8R8A8_SRGB.getVkEnum(), KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
            s.selectMode(Swapchain.PresentMode.Mailbox);
            s.selectExtentByWindow();
            s.selectImageCount(2);
        });

        // Describes the layout of a descriptor set. The descriptor set
        // will represent two uniforms: a uniform buffer at binding 0
        // requiring 1 descriptor, and an image sampler at binding 1
        // requiring 1 descriptor.
        descriptorLayout = new DescriptorSetLayout(device,
                new SetLayoutBinding(Descriptor.UniformBuffer, 0, 1, ShaderStage.Vertex),
                new SetLayoutBinding(Descriptor.CombinedImageSampler, 1, 1, ShaderStage.Fragment));
        descriptorPool = new DescriptorPool(device, 10,
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
            });
            Attachment depth = p.createAttachment(depthView.getImage().getFormat(), VK_SAMPLE_COUNT_1_BIT, a -> {
                a.setLoad(VulkanImage.Load.Clear);
                a.setStore(VulkanImage.Store.DontCare);
                a.setStencilLoad(VulkanImage.Load.DontCare);
                a.setStencilStore(VulkanImage.Store.DontCare);
                a.setInitialLayout(VulkanImage.Layout.Undefined);
                a.setFinalLayout(VulkanImage.Layout.DepthStencilAttachmentOptimal);
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
        int meshBinding0 = meshDesc.addAttribute(BuiltInAttribute.Position, InputRate.Vertex, Format.RGB32SFloat, 0);
        meshDesc.addAttribute(BuiltInAttribute.TexCoord, meshBinding0, Format.RG32SFloat, 1);
        meshDesc.addAttribute(BuiltInAttribute.Normal, meshBinding0, Format.RGB32SFloat, 2);

        // pipeline
        pipelineLayout = new PipelineLayout(device, descriptorLayout);
        vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)));
        fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanFragTest.glsl", ShaderType.Fragment)));
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass, 0);
        try (GraphicsPipeline.Builder p = pipeline.build()) {
            p.addShader(vertModule, ShaderStage.Vertex, "main");
            p.addShader(fragModule, ShaderStage.Fragment, "main");
            p.getVertexInput().setMesh(meshDesc);
            p.getRasterization().setCullMode(CullMode.None);
            p.getViewportState().addViewport();
            p.getViewportState().addScissor();
            p.getColorBlend().addAttachment(new ColorBlendAttachment());
            p.getDynamicState().addTypes(DynamicState.Type.ViewPort, DynamicState.Type.Scissor);
        }

        frames = new UpdateFrameManager(2, n -> new Frame());

        // material color texture
        VulkanImage image = assetManager.loadAsset(VulkanImageLoader.key(initPool, "Common/Textures/MissingTexture.png"));
        VulkanImageView imgView = new VulkanImageView(image, ImageView.Type.TwoDemensional);
        try (VulkanImageView.Builder i = imgView.build()) {
            i.setAspect(VulkanImage.Aspect.Color);
        }
        // material
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

        TestMaterial material = new TestMaterial(descriptorPool);
        material.getBaseColorMap().setResource(new SingleResource<>(texture));

        Mesh m = new MyCustomMesh(device, frames, meshDesc, sharedData,
                Vector3f.UNIT_Z, Vector3f.UNIT_Y, 1f, 1f, 0.5f, 0.5f);
        MatrixTransformMaterial t = new MatrixTransformMaterial(descriptorPool);
        t.getTransforms().setResource(frames.perFrame(n ->
                new PersistentBuffer(device, MemorySize.floats(16), BufferUsage.Uniform, false)));
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
        if (swapchainResizeFlag || imageAcquireCode == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR
                || imageAcquireCode == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
            swapchainResizeFlag = false;
//            try (Swapchain.Builder s = swapchain.build()) {
//                s.addQueue(device.getPhysicalDevice().getGraphics());
//                s.addQueue(device.getPhysicalDevice().getPresent());
//                s.selectFormat(Format.B8G8R8A8_SRGB.getVkEnum(), KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
//                s.selectMode(Swapchain.PresentMode.Mailbox);
//                s.selectExtentByWindow();
//                s.selectImageCount(2);
//            }
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
                VulkanImage.Tiling.Optimal,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
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
            v.allMipmaps();
            v.setAspect(VulkanImage.Aspect.Depth);
        }
        image.transitionLayout(cmd, VulkanImage.Layout.Undefined, VulkanImage.Layout.DepthStencilAttachmentOptimal);
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

            // set camera to render with
            renderManager.setCamera(cam, false);

            // update matrix uniform (geometry)
            for (Spatial s : rootNode) {
                if (s instanceof Geometry) {
                    ((Geometry)s).updateTransformMaterial(cam);
                }
            }

            // update shared data
            {
                sharedDataFence.block(5000);
                sharedDataCommands.resetAndBegin();
                SyncGroup dataSync = sharedDataFinished.toGroupSignal();
                if (sharedData.run(sharedDataCommands, frames.getCurrentFrame())) {
                    sharedDataFence.reset();
                    dataSync.setFence(sharedDataFence); // force the next frame to wait
                }
                sharedDataCommands.endAndSubmit(dataSync);
            }

            // update per-frame data
            {
                perFrameDataCommands.resetAndBegin();
                perFrameData.run(perFrameDataCommands, frames.getCurrentFrame());
                perFrameDataCommands.endAndSubmit(perFrameDataFinished.toGroupSignal());
            }

            // begin graphics commands
            graphicsCommands.resetAndBegin();

            // material graphics
            renderPass.begin(graphicsCommands, image.getFrameBuffer());
            pipeline.bind(graphicsCommands);

            // viewport and scissor
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                        .x(0f).y(0f)
                        .width(swapchain.getExtent().getX())
                        .height(swapchain.getExtent().getY())
                        .minDepth(0f).maxDepth(1f);
                vkCmdSetViewport(graphicsCommands.getBuffer(), 0, vp);
                VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                scissor.offset().set(0, 0);
                scissor.extent(swapchain.getExtent().toStruct(stack));
                vkCmdSetScissor(graphicsCommands.getBuffer(), 0, scissor);
            }

            // run graphics commands via CommandBatch
            graphics.run(graphicsCommands, frames.getCurrentFrame());

            // draw all geometries in the rootNode
            for (Spatial s : rootNode) {
                if (s instanceof Geometry) {
                    Geometry g = (Geometry)s;
                    g.draw(graphicsCommands, pipeline);
                }
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
