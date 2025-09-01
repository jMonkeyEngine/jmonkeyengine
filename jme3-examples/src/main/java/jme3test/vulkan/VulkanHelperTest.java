package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.util.BufferUtils;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.*;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.data.Data;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.*;
import com.jme3.vulkan.frames.UpdateFrame;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.images.*;
import com.jme3.vulkan.material.TestMaterial;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.IndexType;
import com.jme3.vulkan.mesh.TestCaseMeshDescription;
import com.jme3.vulkan.pass.Attachment;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipelines.*;
import com.jme3.vulkan.pass.RenderPass;
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
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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

    private VulkanLogger logger;

    // mesh
    private final FloatBuffer vertexData = BufferUtils.createFloatBuffer(
            -0.5f, -0.5f, 0f,   1f, 0f, 0f,   1f, 0f,
             0.5f, -0.5f, 0f,   0f, 1f, 0f,   0f, 0f,
             0.5f,  0.5f, 0f,   0f, 0f, 1f,   0f, 1f,
            -0.5f,  0.5f, 0f,   1f, 1f, 1f,   1f, 1f,

            -0.5f, -0.5f, -0.5f,   1f, 0f, 0f,   1f, 0f,
             0.5f, -0.5f, -0.5f,   0f, 1f, 0f,   0f, 0f,
             0.5f,  0.5f, -0.5f,   0f, 0f, 1f,   0f, 1f,
            -0.5f,  0.5f, -0.5f,   1f, 1f, 1f,   1f, 1f
    );
    private final IntBuffer indexData = BufferUtils.createIntBuffer(
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4);

    // material
    private TestMaterial material;

    // geometry
    private final Transform modelTransform = new Transform();

    // material
    private Texture texture;

    // framebuffer
    private ImageView depthView;

    public static void main(String[] args) {
        VulkanHelperTest app = new VulkanHelperTest();
        AppSettings settings = new AppSettings(true);
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

        // debug callbacks
        logger = new VulkanLogger(instance, Level.SEVERE);

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
        descriptorPool = new DescriptorPool(device, 3,
                new PoolSize(Descriptor.UniformBuffer, 3),
                new PoolSize(Descriptor.StorageBuffer, 4),
                new PoolSize(Descriptor.CombinedImageSampler, 2));

        CommandPool transferPool = device.getShortTermPool(physDevice.getGraphics());

        // depth texture
        depthView = createDepthAttachment(transferPool);

        // pipeline
        pipelineLayout = new PipelineLayout(device, descriptorLayout);
        vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)));
        fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanFragTest.glsl", ShaderType.Fragment)));
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
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass, 0, new TestCaseMeshDescription());
        try (GraphicsPipeline.Builder p = pipeline.build()) {
            p.addShader(vertModule, ShaderStage.Vertex, "main");
            p.addShader(fragModule, ShaderStage.Fragment, "main");
            p.getViewportState().addViewport();
            p.getViewportState().addScissor();
            p.getColorBlend().addAttachment(new ColorBlendAttachment());
            p.getDynamicState().addTypes(DynamicState.Type.ViewPort, DynamicState.Type.Scissor);
        }
        graphicsPool = device.getLongTermPool(physDevice.getGraphics());

        // vertex buffers
        // cpu-accessible memory is not usually fast for the gpu to access, but
        // the cpu cannot directly access fast gpu memory. The solution is to
        // copy cpu-side data to a mutual staging buffer, then have the gpu copy
        // that data to faster memory.
        vertexBuffer = new StaticBuffer(device, transferPool, MemorySize.floats(vertexData.capacity()),
                BufferUsage.Vertex, MemoryProp.DeviceLocal, false);
        vertexBuffer.copy(vertexData);
        indexBuffer = new StaticBuffer(device, transferPool, MemorySize.ints(indexData.capacity()),
                BufferUsage.Index, MemoryProp.DeviceLocal, false);
        indexBuffer.copy(indexData);

        frames = new UpdateFrameManager(2, n -> new Frame());

        // material color texture
        VulkanImage image = assetManager.loadAsset(VulkanImageLoader.key(transferPool, "Common/Textures/MissingTexture.png"));
        texture = new Texture(device, new ImageView(image, VulkanImage.View.TwoDemensional),
                VK_FILTER_LINEAR, VK_FILTER_LINEAR, VK_SAMPLER_ADDRESS_MODE_REPEAT, VK_SAMPLER_MIPMAP_MODE_LINEAR);

        material = new TestMaterial(descriptorPool);
        material.getMatrices().setPipe(frames.perFrame(n -> new PersistentBuffer(
                device, MemorySize.floats(16), BufferUsage.Uniform, false)));
        material.getBaseColorMap().setPipe(new Data<>(texture));

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
            depthView = createDepthAttachment(device.getShortTermPool(device.getPhysicalDevice().getGraphics()));
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

    private ImageView createDepthAttachment(CommandPool pool) {
        Format depthFormat = device.getPhysicalDevice().findSupportedFormat(
                VulkanImage.Tiling.Optimal,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
                Format.Depth32SFloat, Format.Depth32SFloat_Stencil8UInt, Format.Depth24UNorm_Stencil8UInt);
        GpuImage image = new GpuImage(device, VulkanImage.Type.TwoDemensional);
        try (GpuImage.Builder i = image.build()) {
            i.setSize(swapchain.getExtent().x, swapchain.getExtent().y);
            i.setFormat(depthFormat);
            i.setTiling(VulkanImage.Tiling.Optimal);
            i.setUsage(ImageUsage.DepthStencilAttachment);
            i.setMemoryProps(MemoryProp.DeviceLocal);
        }
        ImageView view = new ImageView(image, VulkanImage.View.TwoDemensional);
        try (ImageView.Builder v = view.build()) {
            v.allMipmaps();
        }
        CommandBuffer commands = pool.allocateOneTimeCommandBuffer();
        commands.begin();
        image.transitionLayout(commands, VulkanImage.Layout.Undefined, VulkanImage.Layout.DepthStencilAttachmentOptimal);
        commands.endAndSubmit(SyncGroup.ASYNC);
        commands.getPool().getQueue().waitIdle();
        return view;
    }

    private class Frame implements UpdateFrame {

        // render manager
        private final CommandBuffer graphicsCommands = graphicsPool.allocateCommandBuffer();
        private final Semaphore imageAvailable = new Semaphore(device, PipelineStage.ColorAttachmentOutput);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);

        // material
//        private final GpuBuffer uniforms;
//        private final DescriptorSet descriptorSet;
        //private final TestMaterial material = new TestMaterial(descriptorPool);

        public Frame() {
            // using a persistent buffer because we will be writing to the buffer very often
//            uniforms = new PersistentBuffer(device, MemorySize.floats(16),
//                    new BufferUsageFlags().uniformBuffer(),
//                    new MemoryFlags().hostVisible().hostCoherent(), false);
//            descriptorSet = descriptorPool.allocateSets(descriptorLayout)[0];
//            descriptorSet.update(true,
//                    new BufferSetWriter(Descriptor.UniformBuffer, 0, 0, new BufferDescriptor(uniforms)),
//                    new ImageSetWriter(Descriptor.CombinedImageSampler, 1, 0, new ImageDescriptor(texture, Image.Layout.ShaderReadOnlyOptimal)));
//            material.getMatrices().setValue(frames.wrap(n -> new PersistentBuffer(device,
//                    MemorySize.floats(16),
//                    BufferUsage.Uniform,
//                    Flag.of(MemoryFlag.HostVisible, MemoryFlag.HostCoherent),
//                    false)));
//            material.getBaseColorMap().setValue(texture);
        }

        @Override
        public void update(UpdateFrameManager frames, float tpf) {

            // render manager
            if (applicationStopped) return;
            inFlight.block(5000);
            if (applicationStopped) return;
            Swapchain.PresentImage image = swapchain.acquireNextImage(VulkanHelperTest.this, imageAvailable, null, 5000);
            if (image == null) {
                return; // no image available: skip rendering this frame
            }
            inFlight.reset();
            renderManager.setCamera(cam, false);
            graphicsCommands.reset();
            graphicsCommands.begin();

            // material
            renderPass.begin(graphicsCommands, image.getFrameBuffer());
            pipeline.bind(graphicsCommands);

            // geometry
            modelTransform.getRotation().multLocal(new Quaternion().fromAngleAxis(tpf, Vector3f.UNIT_Y));
            Matrix4f worldViewProjection = cam.getViewProjectionMatrix().mult(modelTransform.toTransformMatrix());

            // material
            GpuBuffer matrixBuffer = material.getMatrices().getVersion();
            worldViewProjection.fillFloatBuffer(matrixBuffer.mapFloats(), true);
            matrixBuffer.unmap();
            material.bind(graphicsCommands, pipeline);
//                vkCmdBindDescriptorSets(graphicsCommands.getBuffer(), pipeline.getBindPoint().getVkEnum(),
//                        pipelineLayout.getNativeObject(), 0, stack.longs(descriptorSet.getId()), null);

            try (MemoryStack stack = MemoryStack.stackPush()) {

                // viewport
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

                // mesh
                vkCmdBindVertexBuffers(graphicsCommands.getBuffer(), 0, stack.longs(vertexBuffer.getId()), stack.longs(0));
                vkCmdBindIndexBuffer(graphicsCommands.getBuffer(), indexBuffer.getId(), 0, IndexType.UInt32.getEnum());
                vkCmdDrawIndexed(graphicsCommands.getBuffer(), indexBuffer.size().getElements(), 1, 0, 0, 0);

            }

            // material
            renderPass.end(graphicsCommands);

            // render manager
            graphicsCommands.endAndSubmit(new SyncGroup(imageAvailable, renderFinished, inFlight));
            swapchain.present(device.getPhysicalDevice().getPresent(), image, renderFinished.toGroupWait());

        }

    }

}
