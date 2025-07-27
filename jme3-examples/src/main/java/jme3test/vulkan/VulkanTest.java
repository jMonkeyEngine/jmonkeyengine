package jme3test.vulkan;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.shaderc.ShaderType;
import com.jme3.shaderc.ShadercLoader;
import com.jme3.system.AppSettings;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.DeviceEvaluator;
import com.jme3.system.vulkan.LwjglVulkanContext;
import com.jme3.vulkan.Fence;
import com.jme3.vulkan.Semaphore;
import com.jme3.vulkan.VulkanRenderManager;
import jme3tools.shader.ShaderDebug;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK13.*;

public class VulkanTest extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(VulkanTest.class.getName());

    private VkInstance instance;
    private VkDevice device;
    private VkQueue graphicsQueue, presentQueue;
    private VkCommandBuffer commandBuffer;
    private final VulkanRenderManager renderer = new VulkanRenderManager(2, n -> new Frame());
    private final Collection<PointerBuffer> instanceExtensions = new ArrayList<>();
    private final Collection<String> deviceExtensions = new ArrayList<>();
    private final List<String> layers = new ArrayList<>();
    private final Collection<DeviceEvaluator> deviceEvaluators = new ArrayList<>();
    private final VkDebugUtilsMessengerCallbackEXT debugCallback = new VulkanDebugCallback(Level.SEVERE);
    private LwjglVulkanContext vulkanContext;
    private Swapchain swapchain;
    private LongBuffer imageViews;
    private LongBuffer frameBuffers;
    private long surface = NULL;
    private long vertModule = NULL;
    private long fragModule = NULL;
    private long pipelineLayout = NULL;
    private long renderPass = NULL;
    private long pipeline = NULL;
    private long debugMessenger = NULL;
    private long commandPool = NULL;
    private Semaphore imageAvailableSemaphore;
    private Semaphore renderFinishedSemaphore;
    private Fence inFlightFence;

    public static void main(String[] args) {
        VulkanTest app = new VulkanTest();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        settings.setRenderer("CUSTOM" + LwjglVulkanContext.class.getName());
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        vulkanContext = (LwjglVulkanContext)context;
        assetManager.registerLoader(ShadercLoader.class, "glsl");
        flyCam.setDragToRotate(true);

        deviceExtensions.add(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        layers.add("VK_LAYER_KHRONOS_validation"); // basic validation layer from the Vulkan SDK
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer layerPtrs = createLayerBuffer(stack, layers);
            createInstance(stack, layerPtrs);
            if (layerPtrs != null) {
                createDebugMessenger(stack);
            }
            surface = createSurface(stack, instance);
            PhysicalDevice physDevice = findPhysicalDevice(stack, instance, surface);
            QueueFamilyIndices queues = populateQueueFamilies(stack, physDevice.getDevice());
            device = createLogicalDevice(stack, physDevice, queues, createLayerBuffer(stack, layers));
            graphicsQueue = getQueue(stack, device, queues.getGraphics(), 0);
            presentQueue = getQueue(stack, device, queues.getPresents(), 0);
            swapchain = new Swapchain(stack, new SwapchainSupport(stack, physDevice.getDevice(), surface),
                    device, queues, surface, vulkanContext.getWindowHandle());
            imageViews = swapchain.createImageViews(stack, device);
            vertModule = createVertexModule(stack, device);
            fragModule = createFragmentModule(stack, device);
            pipelineLayout = createPipelineLayout(stack, device);
            renderPass = createRenderPass(stack, device, swapchain);
            pipeline = createGraphicsPipeline(stack, device, vertModule, fragModule, pipelineLayout, renderPass);
            frameBuffers = createFrameBuffers(stack, device, swapchain, imageViews, renderPass);
            commandPool = createCommandPool(stack, device, queues);
            commandBuffer = createCommandBuffer(stack, device, commandPool);
            createSyncObjects(device);
        }

    }

    @Override
    public void stop() {
        Native.get().clear(); // clear all native objects
        // destruction will later be handled by a NativeObjectManager
        System.out.println("Destroy vulkan objects...");
        if (commandPool != NULL) {
            System.out.println("  destroy command pool");
            vkDestroyCommandPool(device, commandPool, null);
        }
        if (frameBuffers != null) {
            System.out.println("  destroy framebuffers (" + frameBuffers.limit() + ")");
            for (int i = 0; i < frameBuffers.limit(); i++) {
                vkDestroyFramebuffer(device, frameBuffers.get(i), null);
            }
        }
        if (vertModule != NULL) {
            System.out.println("  destroy vertex module");
            vkDestroyShaderModule(device, vertModule, null);
        }
        if (fragModule != NULL) {
            System.out.println("  destroy fragment module");
            vkDestroyShaderModule(device, fragModule, null);
        }
        if (renderPass != NULL) {
            System.out.println("  destroy render pass");
            vkDestroyRenderPass(device, renderPass, null);
        }
        if (pipeline != NULL) {
            System.out.println("  destroy graphics pipeline");
            vkDestroyPipeline(device, pipeline, null);
        }
        if (pipelineLayout != NULL) {
            System.out.println("  destroy pipeline layout");
            vkDestroyPipelineLayout(device, pipelineLayout, null);
        }
        if (imageViews != null) {
            System.out.println("  destroy image views");
            for (int i = 0; i < imageViews.limit(); i++) {
                vkDestroyImageView(device, imageViews.get(i), null);
            }
        }
        if (swapchain != null) {
            System.out.println("  destroy swapchain");
            swapchain.destroy(device);
        }
        if (device != null) {
            System.out.println("  destroy device");
            vkDeviceWaitIdle(device);
            vkDestroyDevice(device, null);
        }
        if (debugMessenger != NULL) {
            System.out.println("  destroy debug messenger");
            verifyExtensionMethod(instance, "vkDestroyDebugUtilsMessengerEXT");
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
        if (surface != NULL) {
            System.out.println("  destroy surface");
            KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        }
        if (instance != null) {
            System.out.println("  destroy instance");
            vkDestroyInstance(instance, null);
        }
        System.out.println("Vulkan destruction complete!");
        super.stop();
    }

    @Override
    public void simpleUpdate(float tpf) {
        renderer.render(tpf);
        Native.get().flush(); // flush unused native objects
    }

    private void createInstance(MemoryStack stack, PointerBuffer layers) {
        VkApplicationInfo app = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.ASCII(context.getSettings().getTitle()))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.ASCII("JMonkeyEngine"))
                .engineVersion(VK_MAKE_VERSION(3, 9, 0))
                .apiVersion(VK_API_VERSION_1_3);
        VkInstanceCreateInfo create = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(app);
        addExtension(Objects.requireNonNull(GLFWVulkan.glfwGetRequiredInstanceExtensions()));
        addExtension(stack, VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        create.ppEnabledExtensionNames(gatherPointers(stack, instanceExtensions));
        if (layers != null) {
            //create.pNext(createDebugger(stack, debugCallback)); // causing native crashes?
            create.ppEnabledLayerNames(layers);
        }
        instance = new VkInstance(getPointer(stack,
                ptr -> check(vkCreateInstance(create, null, ptr), "Failed to create instance.")), create);
    }

    private PointerBuffer createLayerBuffer(MemoryStack stack, Collection<String> layers) {
        verifyValidationLayerSupport(stack);
        return layers.isEmpty() ? null : toPointers(stack, layers.stream(), layers.size(), stack::UTF8);
    }

    private void createDebugMessenger(MemoryStack stack) {
        verifyExtensionMethod(instance, "vkCreateDebugUtilsMessengerEXT");
        debugMessenger = getLong(stack, ptr -> vkCreateDebugUtilsMessengerEXT(instance, createDebugger(stack, debugCallback), null, ptr));
    }

    private long createSurface(MemoryStack stack, VkInstance instance) {
        return getLong(stack, ptr -> GLFWVulkan.glfwCreateWindowSurface(instance, vulkanContext.getWindowHandle(), null, ptr));
    }

    private PhysicalDevice findPhysicalDevice(MemoryStack stack, VkInstance instance, long surface) {
        PointerBuffer devices = enumerateBuffer(stack, stack::mallocPointer,
                (count, buffer) -> check(vkEnumeratePhysicalDevices(instance, count, buffer), "Failed to enumerate physical devices"));
        PhysicalDevice device = null;
        float score = 0f;
        for (PhysicalDevice d : iteratePointers(devices, p -> new PhysicalDevice(new VkPhysicalDevice(p, instance)))) {
            Float s = evaluateDevice(d, surface);
            if (s != null && (device == null || s > score) && populateQueueFamilies(stack, d.getDevice()).isComplete()) {
                device = d;
                score = s;
            }
        }
        if (device == null) {
            throw new NullPointerException("Failed to find suitable GPU.");
        }
        return device;
    }

    private Float evaluateDevice(PhysicalDevice device, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            System.out.println("evaluating device");
            VkExtensionProperties.Buffer exts = Objects.requireNonNull(enumerateBuffer(stack, n -> {
                        System.out.println("create extension properties buffer: " + n);
                        return VkExtensionProperties.malloc(n, stack);
                    },
                    (count, buffer) -> vkEnumerateDeviceExtensionProperties(device.getDevice(), (ByteBuffer) null, count, buffer)));
            if (!deviceExtensions.stream().allMatch(e -> {
                for (VkExtensionProperties p : exts) {
                    if (p.extensionNameString().equals(e)) {
                        return true;
                    }
                }
                return false;
            })) return null;
            if (!new SwapchainSupport(stack, device.getDevice(), surface).isSupported()) {
                return null;
            }
            if (deviceEvaluators.isEmpty()) {
                return 0f;
            }
            float score = 0f;
            for (DeviceEvaluator e : deviceEvaluators) {
                //Float s = e.evaluateDevice(device.getDevice());
                Float s = 0f;
                if (s == null) {
                    return null;
                }
                score += s;
            }
            return score;
        }
    }

    private QueueFamilyIndices populateQueueFamilies(MemoryStack stack, VkPhysicalDevice device) {
        QueueFamilyIndices fams = new QueueFamilyIndices();
        VkQueueFamilyProperties.Buffer props = enumerateBuffer(stack, VkQueueFamilyProperties::malloc,
                (count, buffer) -> vkGetPhysicalDeviceQueueFamilyProperties(device, count, buffer));
        int index = 0;
        for (VkQueueFamilyProperties p : props) {
            final int i = index;
            if (isBitSet(p.queueFlags(), VK_QUEUE_GRAPHICS_BIT)) {
                fams.setGraphics(i);
            }
            if (getInt(stack, result -> KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, result)) == VK_TRUE) {
                fams.setPresents(i);
            }
            index++;
        }
        return fams;
    }

    private VkDevice createLogicalDevice(MemoryStack stack, PhysicalDevice device, QueueFamilyIndices fams, PointerBuffer layers) {
        // todo: register present queue here
        VkDeviceQueueCreateInfo.Buffer queueCreate = VkDeviceQueueCreateInfo.calloc(fams.getLength(), stack);
        for (int i = 0; i < fams.getLength(); i++) {
            System.out.println("Queue family index: " + fams.getQueue(i));
            queueCreate.get(i).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(fams.getQueue(i))
                    .pQueuePriorities(stack.floats(1f));
        }
        queueCreate.rewind();
        PointerBuffer exts = toPointers(stack, deviceExtensions.stream(), deviceExtensions.size(), stack::UTF8);
        VkDeviceCreateInfo deviceCreate = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreate)
                .pEnabledFeatures(device.getFeatures())
                .ppEnabledExtensionNames(exts);
        if (layers != null) {
            deviceCreate.ppEnabledLayerNames(layers);
        }
        System.out.println("device: " + device.getDevice() + ", device_create: " + deviceCreate);
        long devHandle = getPointer(stack, ptr -> check(vkCreateDevice(device.getDevice(), deviceCreate, null, ptr),
                "Failed to create logical device."));
        return new VkDevice(devHandle, device.getDevice(), deviceCreate);
    }

    private VkQueue getQueue(MemoryStack stack, VkDevice device, int queueIndex, int i) {
        return new VkQueue(getPointer(stack, ptr -> vkGetDeviceQueue(device, queueIndex, i, ptr)), device);
    }

    private long createVertexModule(MemoryStack stack, VkDevice device) {
        return createShaderModule(stack, device, assetManager.loadAsset(ShadercLoader.key("Shaders/VulkanVertTest.glsl", ShaderType.Vertex)));
    }

    private long createFragmentModule(MemoryStack stack, VkDevice device) {
        return createShaderModule(stack, device, assetManager.loadAsset(ShadercLoader.key("Shaders/VulkanFragTest.glsl", ShaderType.Fragment)));
    }

    private long createPipelineLayout(MemoryStack stack, VkDevice device) {
        VkPipelineLayoutCreateInfo layoutCreate = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        return getLong(stack, ptr -> check(vkCreatePipelineLayout(device, layoutCreate, null, ptr),
                "Failed to create pipeline layout."));
    }

    private long createShaderModule(MemoryStack stack, VkDevice device, ByteBuffer code) {
        VkShaderModuleCreateInfo create = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(code);
        return getLong(stack, ptr -> check(vkCreateShaderModule(device, create, null, ptr),
                "Failed to create shader module."));
    }

    private long createGraphicsPipeline(MemoryStack stack, VkDevice device, long vert, long frag, long layout, long renderPass) {
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_VERTEX_BIT)
                .module(vert)
                .pName(stack.UTF8("main")); // function initially called in the shader
        stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(frag)
                .pName(stack.UTF8("main"));
        VkPipelineDynamicStateCreateInfo dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
        VkPipelineVertexInputStateCreateInfo vertInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        VkPipelineInputAssemblyStateCreateInfo assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);
        VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0f).y(0f)
                .width(swapchain.getExtent().width()).height(swapchain.getExtent().height())
                .minDepth(0f).maxDepth(1f);
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(swapchain.getExtent());
        VkPipelineViewportStateCreateInfo vpState = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .pViewports(viewport)
                .pScissors(scissor);
        VkPipelineRasterizationStateCreateInfo raster = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1f)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false);
        VkPipelineMultisampleStateCreateInfo multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
        // todo: configure depth and stencil buffers
        VkPipelineColorBlendAttachmentState.Buffer blendAtt = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(false)
                .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                .alphaBlendOp(VK_BLEND_OP_ADD);
        VkPipelineColorBlendStateCreateInfo blend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .pAttachments(blendAtt);
        VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .stageCount(2)
                .pStages(stages)
                .pVertexInputState(vertInput)
                .pInputAssemblyState(assembly)
                .pViewportState(vpState)
                .pRasterizationState(raster)
                .pMultisampleState(multisample)
                .pColorBlendState(blend)
                .pDynamicState(dynamic)
                .layout(layout)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE)
                .basePipelineIndex(-1);
        return getLong(stack, ptr -> check(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipeline, null, ptr),
                "Failed to create graphics pipeline."));
    }

    private long createRenderPass(MemoryStack stack, VkDevice device, Swapchain swapchain) {
        VkAttachmentDescription.Buffer color = VkAttachmentDescription.calloc(1, stack)
                .format(swapchain.getFormat())
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        VkAttachmentReference.Buffer refs = VkAttachmentReference.calloc(1, stack)
                .attachment(0) // references the attachment in the render pass attachment array
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(refs);
        VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack)
                .srcSubpass(VK_SUBPASS_EXTERNAL) // refers to the implicit pass before our subpass
                .dstSubpass(0) // refers to our only subpass
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
        VkRenderPassCreateInfo create = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(color) // render pass attachment array
                .pSubpasses(subpass)
                .pDependencies(dependency);
        return getLong(stack, ptr -> check(vkCreateRenderPass(device, create, null, ptr),
                "Failed to create render pass."));
    }

    private LongBuffer createFrameBuffers(MemoryStack stack, VkDevice device, Swapchain swapchain, LongBuffer imageViews, long renderPass) {
        LongBuffer buffers = BufferUtils.createLongBuffer(imageViews.limit());
        for (int i = 0; i < imageViews.limit(); i++) {
            VkFramebufferCreateInfo create = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .pAttachments(stack.longs(imageViews.get(i)))
                    .width(swapchain.getExtent().width())
                    .height(swapchain.getExtent().height())
                    .layers(1);
            buffers.put(i, getLong(stack, ptr -> check(vkCreateFramebuffer(device, create, null, ptr),
                    "Failed to create framebuffer.")));
        }
        return buffers;
    }

    private long createCommandPool(MemoryStack stack, VkDevice device, QueueFamilyIndices queues) {
        VkCommandPoolCreateInfo create = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queues.getGraphics());
        return getLong(stack, ptr -> check(vkCreateCommandPool(device, create, null, ptr),
                "Failed to create command pool."));
    }

    private VkCommandBuffer createCommandBuffer(MemoryStack stack, VkDevice device, long commandPool) {
        VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
        return new VkCommandBuffer(getPointer(stack, ptr -> check(vkAllocateCommandBuffers(device, allocate, ptr),
                "Failed to create command buffer.")), device);
    }

    private void recordCommandBuffer(MemoryStack stack, int imageIndex) {
        VkCommandBufferBeginInfo commandBegin = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        check(vkBeginCommandBuffer(commandBuffer, commandBegin), "Failed to begin recording command buffer");
        VkRenderPassBeginInfo passBegin = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(frameBuffers.get(imageIndex))
                .clearValueCount(1);
        VkClearValue.Buffer clear = VkClearValue.calloc(1, stack);
        clear.color().float32(stack.floats(0f, 0f, 0f, 1f));
        passBegin.pClearValues(clear);
        passBegin.renderArea().offset(VkOffset2D.malloc(stack).set(0, 0))
                .extent(swapchain.getExtent());
        vkCmdBeginRenderPass(commandBuffer, passBegin, VK_SUBPASS_CONTENTS_INLINE);
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
        VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                .x(0f).y(0f).width(swapchain.getExtent().width()).height(swapchain.getExtent().height())
                .minDepth(0f).maxDepth(1f);
        vkCmdSetViewport(commandBuffer, 0, vp);
        VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.malloc(stack).set(0, 0))
                .extent(swapchain.getExtent());
        vkCmdSetScissor(commandBuffer, 0, scissor);
        vkCmdDraw(commandBuffer, 3, 1, 0, 0);
        vkCmdEndRenderPass(commandBuffer);
        check(vkEndCommandBuffer(commandBuffer), "Failed to record command buffer.");
    }

    private void createSyncObjects(VkDevice device) {
        //imageAvailableSemaphore = new Semaphore(device);
        //renderFinishedSemaphore = new Semaphore(device);
        //inFlightFence = new Fence(device, true);
    }

    private void verifyValidationLayerSupport(MemoryStack stack) {
        VkLayerProperties.Buffer supported = enumerateBuffer(stack, n -> VkLayerProperties.malloc(n, stack),
                VK10::vkEnumerateInstanceLayerProperties);
        Objects.requireNonNull(supported);
        requestLoop: for (String r : layers) {
            for (VkLayerProperties l : supported) {
                if (r.equals(l.layerNameString())) {
                    continue requestLoop;
                }
            }
            throw new NullPointerException("Validation layer " + r + " is not available.");
        }
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

    private void addExtension(MemoryStack stack, String ext) {
        instanceExtensions.add(stack.mallocPointer(1).put(stack.UTF8(ext)).rewind());
    }

    private void addExtension(PointerBuffer ext) {
        instanceExtensions.add(ext);
    }

    private class Frame implements Runnable {

        private final Semaphore imageAvailable = null;
        private final Semaphore renderFinished = null;
        private final Fence inFlight = null;

        public Frame() {}

        @Override
        public void run() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                System.out.println("start frame render commands");
                inFlight.blockThenReset(5000);
                int image = getInt(stack, i -> check(KHRSwapchain.vkAcquireNextImageKHR(
                        device, swapchain.getSwapchain(), TimeUnit.SECONDS.toNanos(5), imageAvailable.getId(), VK_NULL_HANDLE, i),
                        "Failed to acquire next swapchain image.")); // forces rendering to wait for image availability
                vkResetCommandBuffer(commandBuffer, 0);
                recordCommandBuffer(stack, image);
                VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack)
                        .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .waitSemaphoreCount(1)
                        .pWaitSemaphores(imageAvailableSemaphore.toBuffer(stack)) // waits until the image has been acquired
                        .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                        .pCommandBuffers(stack.pointers(commandBuffer))
                        .pSignalSemaphores(stack.longs(renderFinished.getId())); // forces the present operation to wait
                check(vkQueueSubmit(graphicsQueue, submit, inFlightFence.getId()), "Failed to submit commands to graphics queue.");
                VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack)
                        .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                        .pWaitSemaphores(renderFinishedSemaphore.toBuffer(stack)) // waits until rendering has completed
                        .swapchainCount(1)
                        .pSwapchains(stack.longs(swapchain.getSwapchain()))
                        .pImageIndices(stack.ints(image));
                check(KHRSwapchain.vkQueuePresentKHR(presentQueue, present), "Failed to present image to swapchain");
                System.out.println("end frame render commands");
                BufferUtils bufutils;
            }
        }

        private void recordCommandBuffer(MemoryStack stack, int imageIndex) {
            VkCommandBufferBeginInfo commandBegin = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            check(vkBeginCommandBuffer(commandBuffer, commandBegin), "Failed to begin recording command buffer");
            VkRenderPassBeginInfo passBegin = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(frameBuffers.get(imageIndex))
                    .clearValueCount(1);

            VkClearValue.Buffer clear = VkClearValue.calloc(1, stack);
            clear.color().float32(stack.floats(0f, 0f, 0f, 1f));
            passBegin.pClearValues(clear);
            passBegin.renderArea().offset(VkOffset2D.malloc(stack).set(0, 0))
                    .extent(swapchain.getExtent());
            vkCmdBeginRenderPass(commandBuffer, passBegin, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
            VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                    .x(0f).y(0f).width(swapchain.getExtent().width()).height(swapchain.getExtent().height())
                    .minDepth(0f).maxDepth(1f);
            vkCmdSetViewport(commandBuffer, 0, vp);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .offset(VkOffset2D.malloc(stack).set(0, 0))
                    .extent(swapchain.getExtent());
            vkCmdSetScissor(commandBuffer, 0, scissor);
            vkCmdDraw(commandBuffer, 3, 1, 0, 0);
            vkCmdEndRenderPass(commandBuffer);
            check(vkEndCommandBuffer(commandBuffer), "Failed to record command buffer.");
        }

    }

    private static class QueueFamilyIndices {

        public static final int GRAPHICS = 0, PRESENTS = 1;
        private final Integer[] queues = new Integer[2];

        public void setGraphics(Integer graphics) {
            queues[GRAPHICS] = graphics;
        }

        public void setPresents(Integer presents) {
            queues[PRESENTS] = presents;
        }

        public Integer[] getQueues() {
            return queues;
        }

        public Integer getQueue(int i) {
            return queues[i];
        }

        public Integer getGraphics() {
            return queues[GRAPHICS];
        }

        public Integer getPresents() {
            return queues[PRESENTS];
        }

        public int getLength() {
            return queues.length;
        }

        public boolean isComplete() {
            return Arrays.stream(queues).allMatch(Objects::nonNull);
        }

        public boolean requiresConcurrentSharing() {
            return !Objects.equals(getGraphics(), getPresents());
        }

        private IntBuffer toBuffer(MemoryStack stack) {
            if (!isComplete()) {
                throw new IllegalStateException("Not all queues found in this context.");
            }
            IntBuffer buf = stack.mallocInt(queues.length);
            for (int i = 0; i < queues.length; i++) {
                buf.put(i, queues[i]);
            }
            return buf;
        }

    }

    private static class PhysicalDevice {

        private final VkPhysicalDevice device;
        private final VkPhysicalDeviceProperties props;
        private final VkPhysicalDeviceFeatures features;

        private PhysicalDevice(VkPhysicalDevice device) {
            props = VkPhysicalDeviceProperties.create();
            features = VkPhysicalDeviceFeatures.create();
            vkGetPhysicalDeviceProperties(device, props);
            vkGetPhysicalDeviceFeatures(device, features);
            this.device = device;
        }

        public VkPhysicalDevice getDevice() {
            return device;
        }

        public VkPhysicalDeviceProperties getProps() {
            return props;
        }

        public VkPhysicalDeviceFeatures getFeatures() {
            return features;
        }

    }

    private static class SwapchainSupport {

        private final VkSurfaceCapabilitiesKHR caps;
        private final VkSurfaceFormatKHR.Buffer formats;
        private final IntBuffer modes;

        public SwapchainSupport(MemoryStack stack, VkPhysicalDevice device, long surface) {
            caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, caps);
            formats = enumerateBuffer(stack, n -> VkSurfaceFormatKHR.malloc(n, stack),
                    (count, buffer) -> KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, buffer));
            modes = enumerateBuffer(stack, stack::mallocInt,
                    (count, buffer) -> KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, buffer));
        }

        public VkSurfaceFormatKHR selectFormat() {
            return formats.stream()
                    .filter(f -> f.format() == VK_FORMAT_B8G8R8A8_SRGB)
                    .filter(f -> f.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .findAny().orElse(formats.get(0));
        }

        public int selectMode() {
            for (int i = 0; i < modes.limit(); i++) {
                if (modes.get(i) == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                    return modes.get(i);
                }
            }
            return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
        }

        public VkExtent2D selectExtent(MemoryStack stack, long window) {
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

        public int selectImageCount() {
            int n = caps.minImageCount() + 1;
            return caps.minImageCount() > 0 ? Math.min(n, caps.minImageCount()) : n;
        }

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

    private static class Swapchain {

        private final long swapchain;
        private final LongBuffer images;
        private final int format;
        private final VkExtent2D extent;

        @SuppressWarnings("resource") // todo: watch for memory leaks
        public Swapchain(MemoryStack stack, SwapchainSupport support, VkDevice device, QueueFamilyIndices queues, long surface, long window) {
            VkSurfaceFormatKHR fmt = support.selectFormat();
            format = fmt.format();
            extent = support.selectExtent(stack, window);
            VkSwapchainCreateInfoKHR create = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(support.selectImageCount())
                    .imageFormat(format)
                    .imageColorSpace(fmt.colorSpace())
                    .imageExtent(extent)
                    .imageArrayLayers(1) // this will probably never change for games
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) // rendering directly to the swapchain images
                    .preTransform(support.getCaps().currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // blending with other windows
                    .presentMode(support.selectMode())
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);
            if (queues.requiresConcurrentSharing()) { // different graphics and present queues
                create.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .queueFamilyIndexCount(queues.getLength())
                        .pQueueFamilyIndices(queues.toBuffer(stack));
            } else { // use a faster sharing mode
                create.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }
            swapchain = getLong(stack, ptr -> check(KHRSwapchain.vkCreateSwapchainKHR(device, create, null, ptr),
                    "Failed to create swapchain."));
            images = enumerateBuffer(stack, BufferUtils::createLongBuffer,
                    (count, buffer) -> KHRSwapchain.vkGetSwapchainImagesKHR(device, swapchain, count, buffer));
        }

        public LongBuffer createImageViews(MemoryStack stack, VkDevice device) {
            LongBuffer views = BufferUtils.createLongBuffer(images.limit());
            for (int i = 0; i < images.limit(); i++) {
                VkImageViewCreateInfo create = VkImageViewCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(images.get(i))
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(format);
                create.components().r(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .a(VK_COMPONENT_SWIZZLE_IDENTITY);
                create.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0)
                        .levelCount(1)
                        .baseArrayLayer(0)
                        .layerCount(1);
                views.put(getLong(stack, ptr -> check(vkCreateImageView(device, create, null, ptr),
                        "Failed to create image view.")));
            }
            views.rewind();
            return views;
        }

        public void destroy(VkDevice device) {
            KHRSwapchain.vkDestroySwapchainKHR(device, swapchain, null);
        }

        public long getSwapchain() {
            return swapchain;
        }

        public LongBuffer getImages() {
            return images;
        }

        public int getFormat() {
            return format;
        }

        public VkExtent2D getExtent() {
            return extent;
        }

    }

    private static class VulkanDebugCallback extends VkDebugUtilsMessengerCallbackEXT {

        private Level exceptionThreshold;

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
