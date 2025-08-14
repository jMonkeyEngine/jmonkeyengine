package com.jme3.vulkan.images;

import com.jme3.asset.*;
import com.jme3.util.BufferUtils;
import com.jme3.vulkan.buffers.MemorySize;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.CommandPool;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.flags.BufferUsageFlags;
import com.jme3.vulkan.flags.ImageUsageFlags;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.sync.SyncGroup;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanImageLoader implements AssetLoader {

    @Override
    public Object load(AssetInfo info) throws IOException {
        if (ImageIO.getImageReadersBySuffix(info.getKey().getExtension()) != null) {
            boolean flip = ((Key)info.getKey()).isFlip();
            try (InputStream stream = info.openStream(); BufferedInputStream bin = new BufferedInputStream(stream)) {
                ImageData data = load(bin, flip);
                if (data == null) {
                    throw new AssetLoadException("The given image cannot be loaded " + info.getKey());
                }
                if (info.getKey() instanceof ImageKey) {
                    return loadGpuImage(((ImageKey)info.getKey()).getPool(), data);
                }
                return data;
            }
        }
        throw new AssetLoadException("Image extension " + info.getKey().getExtension() + " is not supported.");
    }

    public ImageData load(InputStream in, boolean flip) throws IOException {
        ImageIO.setUseCache(false);
        BufferedImage img = ImageIO.read(in);
        if (img == null){
            return null;
        }
        return load(img, flip);
    }

    public ImageData load(BufferedImage img, boolean flipY) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] data = (byte[])extractImageData(img);
        switch (img.getType()) {
            case BufferedImage.TYPE_4BYTE_ABGR: { // most common in PNG images w/ alpha
                if (flipY) {
                    flipImage(data, width, height, 32);
                }
                ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
                buffer.put(data);
                return new ImageData(buffer, width, height, Image.Format.ABGR8_SRGB);
            }
            case BufferedImage.TYPE_3BYTE_BGR: { // most common in JPEG images
                if (flipY) {
                    flipImage(data, width, height, 24);
                }
                ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 3);
                buffer.put(data);
                return new ImageData(buffer, width, height, Image.Format.BGR8_SRGB);
            }
            case BufferedImage.TYPE_BYTE_GRAY: { // grayscale fonts
                if (flipY) {
                    flipImage(data, width, height, 8);
                }
                ByteBuffer buffer = BufferUtils.createByteBuffer(width * height);
                buffer.put(data);
                return new ImageData(buffer, width, height, Image.Format.R8_SRGB);
            }
        }
        if (img.getTransparency() == Transparency.OPAQUE) {
            ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int ny = y;
                    if (flipY) {
                        ny = height - y - 1;
                    }
                    int rgb = img.getRGB(x, ny);
                    byte r = (byte) ((rgb & 0x00FF0000) >> 16);
                    byte g = (byte) ((rgb & 0x0000FF00) >> 8);
                    byte b = (byte) ((rgb & 0x000000FF));
                    byte a = Byte.MAX_VALUE;
                    buffer.put(r).put(g).put(b).put(a);
                }
            }
            buffer.flip();
            return new ImageData(buffer, width, height, Image.Format.RGBA8_SRGB);
        } else {
            ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
            for (int y = 0; y < height; y++){
                for (int x = 0; x < width; x++){
                    int ny = y;
                    if (flipY) {
                        ny = height - y - 1;
                    }
                    int rgb = img.getRGB(x,ny);
                    byte a = (byte) ((rgb & 0xFF000000) >> 24);
                    byte r = (byte) ((rgb & 0x00FF0000) >> 16);
                    byte g = (byte) ((rgb & 0x0000FF00) >> 8);
                    byte b = (byte) ((rgb & 0x000000FF));
                    buffer.put(r).put(g).put(b).put(a);
                }
            }
            buffer.flip();
            return new ImageData(buffer, width, height, Image.Format.RGBA8_SRGB);
        }
    }

    private Object extractImageData(BufferedImage img) {
        DataBuffer buf = img.getRaster().getDataBuffer();
        switch (buf.getDataType()) {
            case DataBuffer.TYPE_BYTE: {
                DataBufferByte byteBuf = (DataBufferByte) buf;
                return byteBuf.getData();
            }
            case DataBuffer.TYPE_USHORT: {
                DataBufferUShort shortBuf = (DataBufferUShort) buf;
                return shortBuf.getData();
            }
            default: throw new UnsupportedOperationException("Image data type not supported: " + buf.getDataType());
        }
    }

    private void flipImage(byte[] img, int width, int height, int bpp) {
        int scSz = (width * bpp) / 8;
        byte[] sln = new byte[scSz];
        for (int y1 = 0; y1 < height / 2; y1++) {
            int y2 = height - y1 - 1;
            System.arraycopy(img, y1 * scSz, sln, 0,         scSz);
            System.arraycopy(img, y2 * scSz, img, y1 * scSz, scSz);
            System.arraycopy(sln, 0,         img, y2 * scSz, scSz);
        }
    }

    private GpuImage loadGpuImage(CommandPool transferPool, ImageData data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GpuBuffer staging = new GpuBuffer(transferPool.getDevice(), MemorySize.bytes(data.getBuffer().limit()),
                    new BufferUsageFlags().transferSrc(), new MemoryFlags().hostVisible().hostCoherent(), false);
            staging.copy(stack, data.getBuffer());
            GpuImage image = new GpuImage(transferPool.getDevice(), data.getWidth(), data.getHeight(), data.getFormat(),
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
            commands.submit(new SyncGroup());
            transferPool.getQueue().waitIdle();
            return image;
        }
    }

    public static Key<ImageData> key(String name) {
        return new Key<>(name);
    }

    public static ImageKey key(CommandPool pool, String name) {
        return new ImageKey(pool, name);
    }

    public static class Key <T> extends AssetKey<T> {

        private boolean flip;

        public Key(String name) {
            this(name, false);
        }

        public Key(String name, boolean flip) {
            super(name);
            this.flip = flip;
        }

        public void setFlip(boolean flip) {
            this.flip = flip;
        }

        public boolean isFlip() {
            return flip;
        }

    }

    public static class ImageKey extends Key<GpuImage> {

        private final CommandPool pool;

        public ImageKey(CommandPool pool, String name) {
            super(name);
            this.pool = pool;
        }

        public ImageKey(CommandPool pool, String name, boolean flip) {
            super(name, flip);
            this.pool = pool;
        }

        public CommandPool getPool() {
            return pool;
        }

    }

    public static class ImageData {

        private final ByteBuffer buffer;
        private final int width, height;
        private final Image.Format format;

        public ImageData(ByteBuffer buffer, int width, int height, Image.Format format) {
            this.buffer = buffer;
            this.width = width;
            this.height = height;
            this.format = format;
        }

        public ByteBuffer getBuffer() {
            return buffer;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public Image.Format getFormat() {
            return format;
        }

    }

}
