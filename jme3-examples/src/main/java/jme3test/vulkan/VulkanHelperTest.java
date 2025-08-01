package jme3test.vulkan;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.material.RenderState;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.opencl.CommandQueue;
import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.util.BufferUtils;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.*;
import com.jme3.vulkan.Queue;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.flags.ImageUsageFlags;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.flags.BufferUsageFlags;
import com.jme3.vulkan.images.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.VK13.*;

public class VulkanHelperTest extends SimpleApplication implements SwapchainUpdater {

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice device;
    private SimpleQueueFamilies queues;
    private Swapchain swapchain;
    private ShaderModule vertModule, fragModule;
    private PipelineLayout pipelineLayout;
    private RenderPass renderPass;
    private GraphicsPipeline pipeline;
    private CommandPool graphicsPool;
    private StageableBuffer vertexBuffer, indexBuffer;
    private DescriptorPool descriptorPool;
    private DescriptorSetLayout descriptorLayout;
    private VulkanRenderManager renderer;
    private boolean swapchainResizeFlag = false;
    private boolean applicationStopped = false;

    private final Collection<String> deviceExtensions = new ArrayList<>();

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

        deviceExtensions.add(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        long window = ((LwjglVulkanContext)context).getWindowHandle();

        try (InstanceBuilder inst = new InstanceBuilder(VK13.VK_API_VERSION_1_3)) {

            // build instance
            inst.addGlfwExtensions();
            inst.addDebugExtension();
            inst.addLunarGLayer();
            inst.setApplicationName(VulkanHelperTest.class.getSimpleName());
            inst.setApplicationVersion(1, 0, 0);
            instance = inst.build();

            // debug callbacks
            logger = new VulkanLogger(instance, Level.SEVERE);

            // surface
            surface = new Surface(instance, window);

            // physical device
            PhysicalDevice<SimpleQueueFamilies> physDevice = PhysicalDevice.getSuitableDevice(
                    instance, Arrays.asList(
                        surface,
                        DeviceEvaluator.extensions(deviceExtensions),
                        DeviceEvaluator.swapchain(surface),
                        DeviceEvaluator.anisotropy()
                    ),
                    () -> new SimpleQueueFamilies(surface));

            // queue families
            queues = physDevice.getQueueFamilies();

            // logical device
            PointerBuffer deviceExts = VulkanUtils.toPointers(inst.getStack(), deviceExtensions, inst.getStack()::UTF8);
            device = new LogicalDevice(physDevice, deviceExts, inst.getLayers());

            // create queues
            physDevice.getQueueFamilies().createQueues(device);

            // swapchain
            try (SimpleSwapchainSupport support = new SimpleSwapchainSupport(physDevice, surface, window)) {
                swapchain = new Swapchain(device, surface, support);
            }
        }

        descriptorLayout = new DescriptorSetLayout(device,
                SetLayoutBinding.uniformBuffer(0, 1, VK_SHADER_STAGE_VERTEX_BIT),
                SetLayoutBinding.combinedImageSampler(1, 1, VK_SHADER_STAGE_FRAGMENT_BIT));
        descriptorPool = new DescriptorPool(device, 2,
                PoolSize.uniformBuffers(2), PoolSize.combinedImageSamplers(2));

        CommandPool transferPool = new CommandPool(device, queues.getGraphicsQueue(), true, false);

        // depth texture
        Image.Format depthFormat = device.getPhysicalDevice().findSupportedFormat(
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT,
                Image.Format.Depth32SFloat, Image.Format.Depth32SFloat_Stencil8UInt, Image.Format.Depth24UNorm_Stencil8UInt);
        GpuImage depthImage = new GpuImage(device, swapchain.getExtent().x, swapchain.getExtent().y, depthFormat,
                Image.Tiling.Optimal, new ImageUsageFlags().depthStencilAttachment(), new MemoryFlags().deviceLocal());
        depthView = depthImage.createView(VK_IMAGE_VIEW_TYPE_2D, VK_IMAGE_ASPECT_DEPTH_BIT, 0, 1, 0, 1);
        CommandBuffer commands = transferPool.allocateOneTimeCommandBuffer();
        commands.begin();
        depthImage.transitionLayout(commands, Image.Layout.Undefined, Image.Layout.DepthStencilAttachmentOptimal);
        commands.end();
        commands.submit(null, null, null);
        commands.getPool().getQueue().waitIdle();

        // pipeline
        pipelineLayout = new PipelineLayout(device, descriptorLayout);
        vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)), "main");
        fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanFragTest.glsl", ShaderType.Fragment)), "main");
        try (RenderPassBuilder pass = new RenderPassBuilder()) {
            int color = pass.createAttachment(a -> a
                    .format(swapchain.getFormat().getVkEnum())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(Image.Layout.Undefined.getVkEnum())
                    .finalLayout(Image.Layout.PresentSrc.getVkEnum()));
            int depth = pass.createAttachment(a -> a
                    .format(depthFormat.getVkEnum())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(Image.Layout.Undefined.getVkEnum())
                    .finalLayout(Image.Layout.DepthStencilAttachmentOptimal.getVkEnum()));
            int subpass = pass.createSubpass(s -> {
                VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, pass.getStack())
                        .attachment(color)
                        .layout(Image.Layout.ColorAttachmentOptimal.getVkEnum());
                VkAttachmentReference depthRef = VkAttachmentReference.calloc(pass.getStack())
                        .attachment(depth)
                        .layout(Image.Layout.DepthStencilAttachmentOptimal.getVkEnum());
                s.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                        .colorAttachmentCount(1)
                        .pColorAttachments(colorRef)
                        .pDepthStencilAttachment(depthRef);
            });
            pass.createDependency()
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(subpass)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .srcAccessMask(subpass)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
            renderPass = pass.build(device);
        }
        swapchain.createFrameBuffers(renderPass, depthView);
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass, new RenderState(), vertModule, fragModule, new MeshDescription());
        graphicsPool = new CommandPool(device, queues.getGraphicsQueue(), false, true);

        // vertex buffers
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // cpu-accessible memory is not usually fast for the gpu to access, but
            // the cpu cannot directly access fast gpu memory. The solution is to
            // copy cpu-side data to a mutual staging buffer, then have the gpu copy
            // that data to faster memory. Hence, why we do two copy operations here.
            vertexBuffer = new StageableBuffer(device, vertexData.capacity() * Float.BYTES,
                    new BufferUsageFlags().vertexBuffer(), new MemoryFlags().deviceLocal(), false);
            vertexBuffer.copy(stack, vertexData); // copy data to staging buffer
            vertexBuffer.transfer(transferPool); // transfer staged data to vertex buffer
            vertexBuffer.freeStagingBuffer();
            // index buffer
            indexBuffer = new StageableBuffer(device, indexData.capacity() * Integer.BYTES,
                    new BufferUsageFlags().indexBuffer(), new MemoryFlags().deviceLocal(), false);
            indexBuffer.copy(stack, indexData);
            indexBuffer.transfer(transferPool);
            indexBuffer.freeStagingBuffer();
        }

        // material color texture
        GpuImage image = loadImage("Common/Textures/MissingTexture.png", transferPool);
        texture = new Texture(device, image.createView(VK_IMAGE_VIEW_TYPE_2D, VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1),
                VK_FILTER_LINEAR, VK_FILTER_LINEAR, VK_SAMPLER_ADDRESS_MODE_REPEAT, VK_SAMPLER_MIPMAP_MODE_LINEAR);

        renderer = new VulkanRenderManager(2, n -> new Frame());

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
            long window = ((LwjglVulkanContext)context).getWindowHandle();
            try (SimpleSwapchainSupport support = new SimpleSwapchainSupport(device.getPhysicalDevice(), surface, window)) {
                swapchain.reload(support);
                swapchain.createFrameBuffers(renderPass, depthView);
            }
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
        renderer.render(tpf);
    }

    private GpuImage loadImage(String file, CommandPool transferPool) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VulkanImageLoader.ImageData data = assetManager.loadAsset(VulkanImageLoader.key(file));
            GpuBuffer staging = new GpuBuffer(device, data.getBuffer().limit(),
                    new BufferUsageFlags().transferSrc(), new MemoryFlags().hostVisible().hostCoherent(), false);
            staging.copy(stack, data.getBuffer());
            GpuImage image = new GpuImage(device, data.getWidth(), data.getHeight(), data.getFormat(),
                    Image.Tiling.Optimal, new ImageUsageFlags().transferDst().sampled(),
                    new MemoryFlags().deviceLocal());
            CommandBuffer commands = transferPool.allocateOneTimeCommandBuffer();
            commands.begin();
            image.transitionLayout(commands, Image.Layout.Undefined, Image.Layout.TransferDstOptimal);
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0) // padding bytes
                    .bufferImageHeight(0); // padding bytes
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(data.getWidth(), data.getHeight(), 1);
            vkCmdCopyBufferToImage(commands.getBuffer(), staging.getNativeObject(),
                    image.getNativeObject(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
            image.transitionLayout(commands, Image.Layout.TransferDstOptimal, Image.Layout.ShaderReadOnlyOptimal);
            commands.end();
            commands.submit(null, null, null);
            transferPool.getQueue().waitIdle();
            return image;
        }
    }

    private class Frame implements Consumer<Float> {

        private final CommandBuffer graphicsCommands = graphicsPool.allocateCommandBuffer();
        private final Semaphore imageAvailable = new Semaphore(device);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);
        private final GpuBuffer uniforms;
        private final DescriptorSet descriptorSet;

        public Frame() {
            uniforms = new PersistentBuffer(device, 16 * Float.BYTES,
                    new BufferUsageFlags().uniformBuffer(),
                    new MemoryFlags().hostVisible().hostCoherent(), false);
            descriptorSet = descriptorPool.allocateSets(descriptorLayout)[0];
            descriptorSet.write(BufferSetWriter.uniformBuffers(0, 0, new BufferDescriptor(uniforms)),
                    ImageSetWriter.combinedImageSampler(1, 0, new ImageDescriptor(texture, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)));
        }

        @Override
        public void accept(Float tpf) {
            if (applicationStopped) {
                return;
            }
            inFlight.block(5000);
            if (applicationStopped) {
                return;
            }
            Swapchain.SwapchainImage image = swapchain.acquireNextImage(VulkanHelperTest.this, imageAvailable, null, 5000);
            if (image == null) {
                return; // no image available: skip rendering this frame
            }
            inFlight.reset();
            renderManager.setCamera(cam, false);
            graphicsCommands.reset();
            graphicsCommands.begin();
            renderPass.begin(graphicsCommands, image.getFrameBuffer());
            pipeline.bind(graphicsCommands);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                modelTransform.getRotation().multLocal(new Quaternion().fromAngleAxis(tpf, Vector3f.UNIT_Y));
                cam.getViewProjectionMatrix().mult(modelTransform.toTransformMatrix())
                        .fillFloatBuffer(uniforms.mapFloats(stack, 0, 16, 0), true);
                vkCmdBindDescriptorSets(graphicsCommands.getBuffer(), VK_PIPELINE_BIND_POINT_GRAPHICS,
                        pipelineLayout.getNativeObject(), 0, stack.longs(descriptorSet.getId()), null);
                uniforms.unmap();
                vkCmdBindVertexBuffers(graphicsCommands.getBuffer(), 0, stack.longs(vertexBuffer.getNativeObject()), stack.longs(0));
                vkCmdBindIndexBuffer(graphicsCommands.getBuffer(), indexBuffer.getNativeObject(), 0, VK_INDEX_TYPE_UINT32);
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
            vkCmdDrawIndexed(graphicsCommands.getBuffer(), indexData.limit(), 1, 0, 0, 0);
            vkCmdEndRenderPass(graphicsCommands.getBuffer());
            graphicsCommands.end();
            graphicsCommands.submit(imageAvailable, renderFinished, inFlight);
            swapchain.present(queues.getPresentQueue(), image, renderFinished);
        }

    }

    private static class SimpleQueueFamilies implements QueueFamilies {

        public static final int NUM_QUEUES = 2;

        private final Surface surface;
        private Integer graphicsIndex = null;
        private Integer presentIndex = null;
        private Queue graphics, present;

        public SimpleQueueFamilies(Surface surface) {
            this.surface = surface;
        }

        @Override
        public boolean populate(PhysicalDevice device, VkQueueFamilyProperties.Buffer properties) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ibuf = stack.callocInt(1);
                for (int i = 0; i < properties.limit(); i++) {
                    VkQueueFamilyProperties props = properties.get(i);
                    if (graphicsIndex == null && (props.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) > 0) {
                        graphicsIndex = i;
                    } else if (presentIndex == null) {
                        KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                                device.getDevice(), i, surface.getNativeObject(), ibuf);
                        if (ibuf.get(0) == VK13.VK_TRUE) {
                            presentIndex = i;
                        }
                    }
                    if (isComplete()) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public VkDeviceQueueCreateInfo.Buffer createLogicalBuffers(MemoryStack stack) {
            System.out.println("selected queues: " + graphicsIndex + ", " + presentIndex);
            VkDeviceQueueCreateInfo.Buffer create = VkDeviceQueueCreateInfo.calloc(NUM_QUEUES, stack);
            create.get(0).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(graphicsIndex)
                    .pQueuePriorities(stack.floats(1f));
            create.get(1).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(presentIndex)
                    .pQueuePriorities(stack.floats(1f));
            return create;
        }

        @Override
        public void createQueues(LogicalDevice device) {
            graphics = new Queue(device, graphicsIndex, 0);
            present = new Queue(device, presentIndex, 0);
        }

        @Override
        public boolean isComplete() {
            return graphicsIndex != null && presentIndex != null;
        }

        @Override
        public IntBuffer getSwapchainConcurrentBuffers(MemoryStack stack) {
            if (Objects.equals(graphicsIndex, presentIndex)) {
                return null;
            }
            IntBuffer buf = stack.mallocInt(NUM_QUEUES);
            buf.put(0, graphicsIndex);
            buf.put(1, presentIndex);
            return buf;
        }

        public Queue getGraphicsQueue() {
            return graphics;
        }

        public Queue getPresentQueue() {
            return present;
        }

    }

    private static class SimpleSwapchainSupport implements SwapchainSupport, AutoCloseable {

        private final MemoryStack stack;
        private final VkSurfaceCapabilitiesKHR caps;
        private final VkSurfaceFormatKHR.Buffer formats;
        private final IntBuffer modes;
        private final long window;

        public SimpleSwapchainSupport(PhysicalDevice device, Surface surface, long window) {
            stack = MemoryStack.stackPush();
            caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getDevice(), surface.getNativeObject(), caps);
            formats = enumerateBuffer(stack, n -> VkSurfaceFormatKHR.malloc(n, stack),
                    (count, buffer) -> KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(
                            device.getDevice(), surface.getNativeObject(), count, buffer));
            modes = enumerateBuffer(stack, stack::mallocInt,
                    (count, buffer) -> KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(
                            device.getDevice(), surface.getNativeObject(), count, buffer));
            this.window = window;
        }

        @Override
        public void close() {
            stack.pop();
        }

        @Override
        public VkSurfaceFormatKHR selectFormat() {
            return formats.stream()
                    .filter(f -> f.format() == VK_FORMAT_B8G8R8A8_SRGB)
                    .filter(f -> f.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .findAny().orElse(formats.get(0));
        }

        @Override
        public int selectMode() {
            for (int i = 0; i < modes.limit(); i++) {
                if (modes.get(i) == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                    return modes.get(i);
                }
            }
            return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
        }

        @Override
        public VkExtent2D selectExtent() {
            if (caps.currentExtent().width() != UINT32_MAX) {
                return caps.currentExtent();
            }
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, width, height);
            VkExtent2D ext = VkExtent2D.malloc(stack);
            ext.width(Math.min(Math.max(width.get(0), caps.minImageExtent().width()), caps.maxImageExtent().width()));
            ext.height(Math.min(Math.max(width.get(0), caps.minImageExtent().height()), caps.maxImageExtent().height()));
            return ext;
        }

        @Override
        public int selectImageCount() {
            int n = caps.minImageCount() + 1;
            return caps.minImageCount() > 0 ? Math.min(n, caps.minImageCount()) : n;
        }

        @Override
        public boolean isSupported() {
            return formats != null && modes != null;
        }

        public VkSurfaceCapabilitiesKHR getCaps() {
            return caps;
        }

        public VkSurfaceFormatKHR.Buffer getFormats() {
            return formats;
        }

        public IntBuffer getModes() {
            return modes;
        }

    }

    private static class VulkanDebugCallback extends VkDebugUtilsMessengerCallbackEXT {

        private final Level exceptionThreshold;

        public VulkanDebugCallback(Level exceptionThreshold) {
            this.exceptionThreshold = exceptionThreshold;
        }

        @Override
        public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
            try (VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData)) {
                //LOG.log(getLoggingLevel(messageSeverity), data.pMessageString());
                Level lvl = getLoggingLevel(messageSeverity);
                if (exceptionThreshold != null && lvl.intValue() >= exceptionThreshold.intValue()) {
                    throw new RuntimeException(lvl.getName() + ": " + data.pMessageString());
                } else {
                    System.err.println(lvl.getName() + "  " + data.pMessageString());
                }
            }
            return VK_FALSE; // always return false, true is only really used for testing validation layers
        }

        public Level getLoggingLevel(int messageSeverity) {
            switch (messageSeverity) {
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
                    return Level.SEVERE;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
                    return Level.WARNING;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT:
                    return Level.INFO;
                case EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT:
                    return Level.FINE;
                default: throw new UnsupportedOperationException("Unsupported severity bit: "
                        + Integer.numberOfTrailingZeros(Integer.highestOneBit(messageSeverity)));
            }
        }

    }

}
