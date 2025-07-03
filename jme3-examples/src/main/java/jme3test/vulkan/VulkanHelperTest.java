package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.material.RenderState;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.vulkan.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.VK13.*;

public class VulkanHelperTest extends SimpleApplication {

    private VulkanInstance instance;
    private Surface surface;
    private LogicalDevice device;
    private SimpleQueueFamilies queues;
    private Swapchain swapchain;
    private List<ImageView> swapchainImages;
    private ShaderModule vertModule, fragModule;
    private PipelineLayout pipelineLayout;
    private RenderPass renderPass;
    private GraphicsPipeline pipeline;
    private final List<FrameBuffer> frameBuffers = new ArrayList<>();
    private CommandBuffer graphicsCommands;
    private VulkanRenderManager renderer;

    private long debugMessenger = NULL;
    private final VkDebugUtilsMessengerCallbackEXT debugCallback = new VulkanDebugCallback(Level.SEVERE);

    public static void main(String[] args) {
        VulkanHelperTest app = new VulkanHelperTest();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        settings.setRenderer("CUSTOM" + LwjglVulkanContext.class.getName());
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        long window = ((LwjglVulkanContext)context).getWindowHandle();
        try (InstanceBuilder inst = new InstanceBuilder(VK13.VK_API_VERSION_1_3)) {
            inst.addGlfwExtensions();
            inst.addDebugExtension();
            inst.addLunarGLayer();
            inst.setApplicationName(VulkanHelperTest.class.getSimpleName());
            inst.setApplicationVersion(1, 0, 0);
            instance = inst.build();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                createDebugMessenger(stack);
            }
            surface = new Surface(instance, window);
            PhysicalDevice<SimpleQueueFamilies> physDevice = PhysicalDevice.getPhysicalDevice(
                    instance.getNativeObject(),
                    Arrays.asList(surface, DeviceEvaluator.extensions(inst.getNamedExtensions()), DeviceEvaluator.swapchain(surface)),
                    () -> new SimpleQueueFamilies(surface));
            queues = physDevice.getQueueFamilies();
            device = new LogicalDevice(physDevice, inst.getExtensions(), inst.getLayers());
            physDevice.getQueueFamilies().createQueues(device);
            try (SimpleSwapchainSupport support = new SimpleSwapchainSupport(physDevice, surface, window)) {
                swapchain = new Swapchain(device, surface, support);
            }
        }
        swapchainImages = swapchain.createViews();
        vertModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shaders/VulkanVertTest.glsl", ShaderType.Vertex)), "main");
        fragModule = new ShaderModule(device, assetManager.loadAsset(ShadercLoader.key(
                "Shader/VulkanFragTest.glsl", ShaderType.Fragment)), "main");
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
        pipeline = new GraphicsPipeline(device, pipelineLayout, renderPass, new RenderState(), vertModule, fragModule);
        for (ImageView v : swapchainImages) {
            frameBuffers.add(new FrameBuffer(device, renderPass,
                    swapchain.getExtent().width(), swapchain.getExtent().height(), 1, v));
        }
        CommandPool graphicsPool = new CommandPool(device, queues.getGraphicsQueue(), false, true);
        graphicsCommands = graphicsPool.allocateCommandBuffer();
        renderer = new VulkanRenderManager(2, Frame::new);
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
        private final Semaphore imageAvailable = new Semaphore(device);
        private final Semaphore renderFinished = new Semaphore(device);
        private final Fence inFlight = new Fence(device, true);

        private Frame(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            inFlight.blockThenReset(5000);
            Swapchain.SwapchainImage image = swapchain.acquireNextImage(imageAvailable, null, 5000);
            graphicsCommands.reset();
            graphicsCommands.begin();
            renderPass.begin(graphicsCommands, frameBuffers.get(index));
            pipeline.bind(graphicsCommands);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                        .x(0f).y(0f)
                        .width(swapchain.getExtent().width())
                        .height(swapchain.getExtent().height())
                        .minDepth(0f).maxDepth(1f);
                vkCmdSetViewport(graphicsCommands.getBuffer(), 0, vp);
                VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                scissor.offset().set(0, 0);
                scissor.extent(swapchain.getExtent());
                vkCmdSetScissor(graphicsCommands.getBuffer(), 0, scissor);
            }
            vkCmdDraw(graphicsCommands.getBuffer(), 3, 1, 0, 0);
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
            return null;
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                GLFW.glfwGetFramebufferSize(window, width, height);
                VkExtent2D ext = VkExtent2D.malloc(stack);
                ext.width(Math.min(Math.max(width.get(0), caps.minImageExtent().width()), caps.maxImageExtent().width()));
                ext.height(Math.min(Math.max(width.get(0), caps.minImageExtent().height()), caps.maxImageExtent().height()));
                return ext;
            }
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
