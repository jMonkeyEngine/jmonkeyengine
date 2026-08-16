package com.jme3.vulkan.images.newimage;

import com.jme3.math.IntVector;
import com.jme3.texture.Texture;
import com.jme3.util.natives.Destructable;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.formats.EnumInterpreter;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.pipeline.Access;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.KHRSharedPresentableImage;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK14;

import static org.lwjgl.vulkan.VK10.*;

public interface EngineImage extends Destructable {

    enum Create implements Flag<Create> {

        MutableFormat(VK_IMAGE_CREATE_MUTABLE_FORMAT_BIT),
        CubeCompatible(VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT),
        SparseAliased(VK_IMAGE_CREATE_SPARSE_ALIASED_BIT),
        SparseBinding(VK_IMAGE_CREATE_SPARSE_BINDING_BIT),
        SparseResidency(VK_IMAGE_CREATE_SPARSE_RESIDENCY_BIT);

        private final int bits;

        Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }

    enum Role implements Flag<Role> {

        TransferDst(VK_IMAGE_USAGE_TRANSFER_DST_BIT),
        TransferSrc(VK_IMAGE_USAGE_TRANSFER_SRC_BIT),
        ColorAttachment(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT),
        Sampled(VK_IMAGE_USAGE_SAMPLED_BIT),
        Storage(VK_IMAGE_USAGE_STORAGE_BIT),
        DepthStencilAttachment(VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT),
        InputAttachment(VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT),
        TransientAttachment(VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT);

        private final int bits;

        Role(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    enum Type implements IntEnum<Type> {

        OneDemensional(VK_IMAGE_TYPE_1D),
        TwoDimensional(VK_IMAGE_TYPE_2D),
        ThreeDimensional(VK_IMAGE_TYPE_3D);

        private final int vkEnum;

        Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    enum Layout implements IntEnum<Layout> {

        Undefined(VK_IMAGE_LAYOUT_UNDEFINED,
                Flag.empty(), PipelineStage.TopOfPipe),
        General(VK_IMAGE_LAYOUT_GENERAL,
                Flag.empty(), Flag.empty()), // not sure how this layout should be treated for transitions
        PreInitialized(VK_IMAGE_LAYOUT_PREINITIALIZED),
        ReadOnlyOptimal(VK14.VK_IMAGE_LAYOUT_READ_ONLY_OPTIMAL),
        ColorAttachmentOptimal(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                Flag.of(Access.ColorAttachmentRead, Access.ColorAttachmentWrite), PipelineStage.ColorAttachmentOutput),
        DepthStencilAttachmentOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                Flag.of(Access.DepthStencilAttachmentRead, Access.DepthStencilAttachmentWrite), PipelineStage.EarlyFragmentTests),
        DepthStencilReadOnlyOptimal(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                Access.DepthStencilAttachmentRead, PipelineStage.EarlyFragmentTests),
        TransferSrcOptimal(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                Access.TransferRead, PipelineStage.Transfer),
        TransferDstOptimal(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                Access.TransferWrite, PipelineStage.Transfer),
        ShaderReadOnlyOptimal(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                Access.ShaderRead, PipelineStage.FragmentShader),
        PresentSrc(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR),
        SharedPresent(KHRSharedPresentableImage.VK_IMAGE_LAYOUT_SHARED_PRESENT_KHR);

        private final int vkEnum;
        private final Flag<Access> access;
        private final Flag<PipelineStage> stage;

        Layout(int vkEnum) {
            this.vkEnum = vkEnum;
            access = Flag.empty();
            stage = Flag.empty();
        }

        Layout(int vkEnum, Flag<Access> access, Flag<PipelineStage> stage) {
            this.vkEnum = vkEnum;
            this.access = access;
            this.stage = stage;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

        public Flag<Access> getAccessHint() {
            return access;
        }

        public Flag<PipelineStage> getStageHint() {
            return stage;
        }

    }

    enum Tiling implements IntEnum<Tiling> {

        Optimal(VK_IMAGE_TILING_OPTIMAL),
        Linear(VK_IMAGE_TILING_LINEAR);

        private final int vkEnum;

        Tiling(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int getEnum() {
            return vkEnum;
        }

    }

    enum Load {

        Clear, Load, DontCare;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getImageLoadEnum(this);
        }

    }

    enum Store {

        Store, DontCare;

        public int getEnum(EnumInterpreter interpreter) {
            return interpreter.getImageStoreEnum(this);
        }

    }

    enum Aspect implements Flag<Aspect> {

        Color(VK_IMAGE_ASPECT_COLOR_BIT),
        Depth(VK_IMAGE_ASPECT_DEPTH_BIT),
        Stencil(VK_IMAGE_ASPECT_STENCIL_BIT),
        MetaData(VK_IMAGE_ASPECT_METADATA_BIT),
        DepthStencil(VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT);

        private final int bits;

        Aspect(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    long getHandle();

    Type getType();

    Format getFormat();

    /**
     * Gets the current internal memory layout of this image on the device. Images must
     * be in the correct layout for the tasks it is being used for. To change the layout
     * on the fly, use {@link #transitionLayout(CommandBuffer, Layout)}.
     *
     * @return current memory layout
     */
    Layout getLayout();

    /**
     * Gets the 3D size in pixels of this image. For unused dimensions, the size is 1.
     *
     * @return size in pixels
     */
    IntVector getSize();

    /**
     * Gets the number of samples per pixel in this image.
     *
     * @return samples per pixel
     */
    int getSamples();

    /**
     * Gets the number of mipmap levels in this image.
     *
     * @return mipmap levels
     */
    int getMipLevels();

    /**
     * Gets the number of array layers in this image. OpenGL images
     * may interpret this property to be the same as the height or depth
     * property, depending on the property type.
     *
     * @return array layers
     */
    int getArrayLayers();

    /**
     * Gets the tiling mode of this image. {@link Tiling#Optimal} allows the device
     * drivers to make significant optimizations for the image and is almost always
     * the correct tiling mode to use. It is not recommended to use {@link Tiling#Linear}
     * except when needing to directly interact with image memory on the host.
     *
     * @return tiling mode
     */
    Tiling getTiling();

    /**
     * Gets the roles this image is allowed to fill.
     *
     * @return image roles
     */
    Flag<Role> getRoles();

    /**
     * Gets the properties of the memory backing this image.
     *
     * @return memory properties
     */
    Flag<MemoryProp> getMemoryProperties();

    /**
     * Transitions the layout of this image from its current layout to {@code layout}.
     * Images must be in the correct layout for the task it is being used in.
     *
     * @param cmd command buffer
     * @param layout layout to transition to
     */
    void transitionLayout(CommandBuffer cmd, Layout layout);

    /**
     * Creates an image view that sees all aspects of this image. The type of image view
     * is auto-detected from the properties of this image.
     *
     * @return image view
     */
    Texture createTexture();

}
