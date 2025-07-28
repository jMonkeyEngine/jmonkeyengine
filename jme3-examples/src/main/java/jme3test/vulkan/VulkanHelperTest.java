package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.material.RenderState;
import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.util.BufferUtils;
import com.jme3.vulkan.*;
import com.jme3.vulkan.Queue;
import com.jme3.vulkan.buffers.BufferArgs;
import com.jme3.vulkan.buffers.GpuBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
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
    private GpuBuffer vertexBuffer, indexBuffer;
    private VulkanRenderManager renderer;
    private boolean swapchainResizeFlag = false;

    private final Collection<String> deviceExtensions = new ArrayList<>();

    private long debugMessenger = NULL;
    private final VkDebugUtilsMessengerCallbackEXT debugCallback = new VulkanDebugCallback(Level.SEVERE);

    private final FloatBuffer vertexData = BufferUtils.createFloatBuffer(
            -0.5f, -0.5f, 1f, 0f, 0f,
            0.5f, -0.5f, 0f, 1f, 0f,
            0.5f, 0.5f, 0f, 0f, 1f,
            -0.5f, 0.5f, 1f, 1f, 1f);
    private final IntBuffer indexData = BufferUtils.createIntBuffer(0, 1, 2, 2, 3, 0);

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

    @Override
    public void simpleInitApp() {

        assetManager.registerLoader(ShadercLoader.class, "glsl");
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                createDebugMessenger(stack);
            }

            // surface
            surface = new Surface(instance, window);

            // physical device
            PhysicalDevice<SimpleQueueFamilies> physDevice = PhysicalDevice.getPhysicalDevice(
                    instance.getNativeObject(),
                    Arrays.asList(surface, DeviceEvaluator.extensions(deviceExtensions), DeviceEvaluator.swapchain(surface)),
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

        // pipeline
        vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)), "main");
        fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanFragTest.glsl", ShaderType.Fragment)), "main");
        pipelineLayout = new PipelineLayout(device);
        try (RenderPassBuilder pass = new RenderPassBuilder()) {
            int color = pass.createAttachment(a -> a
                    .format(swapchain.getFormat())
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR));
            int subpass = pass.createSubpass(s -> {
                VkAttachmentReference.Buffer ref = VkAttachmentReference.calloc(1, pass.getStack())
                        .attachment(color)
                        .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                s.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                        .colorAttachmentCount(1)
                        .pColorAttachments(ref);
            });
            pass.createDependency()
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(subpass)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(subpass)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
            renderPass = pass.build(device);
        }
        swapchain.createFrameBuffers(renderPass);
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass, new RenderState(), vertModule, fragModule, new MeshDescription());
        graphicsPool = new CommandPool(device, queues.getGraphicsQueue(), false, true);
        renderer = new VulkanRenderManager(2, Frame::new);

        CommandPool transferPool = new CommandPool(device, queues.getGraphicsQueue(), true, false);

        // vertex buffers
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GpuBuffer staging = new GpuBuffer(device, vertexData.capacity() * Float.BYTES,
                    new BufferArgs().transferSrc().hostVisible().hostCoherent());
            staging.copy(stack, vertexData);
            vertexBuffer = new GpuBuffer(device, staging.getSize(),
                    new BufferArgs().transferDst().vertexBuffer().deviceLocal());
            CommandBuffer commands = transferPool.allocateOneTimeCommandBuffer();
            vertexBuffer.recordCopy(stack, commands, staging, 0, 0, (long)vertexData.limit() * Float.BYTES);
            commands.submit(null, null, null);
            transferPool.getQueue().waitIdle(); // todo: use fences to wait on transfer operations
            staging.freeMemory(); // destroys buffer
        }

        // index buffers
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GpuBuffer staging = new GpuBuffer(device, indexData.capacity() * Integer.BYTES,
                    new BufferArgs().transferSrc().hostVisible().hostCoherent());
            staging.copy(stack, indexData);
            indexBuffer = new GpuBuffer(device, staging.getSize(),
                    new BufferArgs().transferDst().indexBuffer().deviceLocal());
            CommandBuffer commands = transferPool.allocateOneTimeCommandBuffer();
            indexBuffer.recordCopy(stack, commands, staging, 0, 0, (long)indexData.limit() * Integer.BYTES);
            commands.submit(null, null, null);
            transferPool.getQueue().waitIdle();
            staging.freeMemory();
        }

    }

    @Override
    public void stop() {
        if (debugMessenger != NULL) {
            System.out.println("  destroy debug messenger");
            verifyExtensionMethod(instance.getNativeObject(), "vkDestroyDebugUtilsMessengerEXT");
            vkDestroyDebugUtilsMessengerEXT(instance.getNativeObject(), debugMessenger, null);
        }
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
                swapchain.createFrameBuffers(renderPass);
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

    private VkDebugUtilsMessengerCreateInfoEXT createDebugger(MemoryStack stack, VkDebugUtilsMessengerCallbackEXT callback) {
        VkDebugUtilsMessengerCreateInfoEXT create = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT)
                .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                        VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        if (callback != null) {
            create.pfnUserCallback(callback);
        }
        return create;
    }

    private void createDebugMessenger(MemoryStack stack) {
        verifyExtensionMethod(instance.getNativeObject(), "vkCreateDebugUtilsMessengerEXT");
        debugMessenger = getLong(stack, ptr -> vkCreateDebugUtilsMessengerEXT(
                instance.getNativeObject(), createDebugger(stack, debugCallback), null, ptr));
    }

    private class Frame implements Runnable {

        private final int index;
        private final CommandBuffer graphicsCommands = graphicsPool.allocateCommandBuffer();
        private final Semaphore imageAvailable = new Semaphore(device);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);

        private Frame(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            inFlight.block(5000);
            Swapchain.SwapchainImage image = swapchain.acquireNextImage(VulkanHelperTest.this, imageAvailable, null, 5000);
            if (image == null) {
                return;
            }
            inFlight.reset();
            graphicsCommands.reset();
            graphicsCommands.begin();
            renderPass.begin(graphicsCommands, image.getFrameBuffer());
            pipeline.bind(graphicsCommands);
            try (MemoryStack stack = MemoryStack.stackPush()) {
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
            //vkCmdDraw(graphicsCommands.getBuffer(), vertexData.limit() / 5, 1, 0, 0);
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
            IntBuffer ibuf = MemoryUtil.memAllocInt(1);
            for (int i = 0; i < properties.limit(); i++) {
                VkQueueFamilyProperties props = properties.get(i);
                if (graphicsIndex == null && (props.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) > 0) {
                    graphicsIndex = i;
                }
                if (presentIndex == null) {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                            device.getDevice(), i, surface.getNativeObject(), ibuf);
                    if (ibuf.get(0) == VK13.VK_TRUE) {
                        presentIndex = i;
                    }
                }
            }
            MemoryUtil.memFree(ibuf);
            return isComplete();
        }

        @Override
        public VkDeviceQueueCreateInfo.Buffer createLogicalBuffers(MemoryStack stack) {
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
