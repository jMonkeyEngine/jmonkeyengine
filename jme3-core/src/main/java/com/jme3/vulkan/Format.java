package com.jme3.vulkan;

import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static org.lwjgl.vulkan.VK10.*;

public enum Format implements Iterable<Format.Component> {

    RGBA32SFloat(VK_FORMAT_R32G32B32A32_SFLOAT, array(cf(4), cf(4), cf(4), cf(4)), true, false, false),
    RGB32SFloat(VK_FORMAT_R32G32B32_SFLOAT, array(cf(4), cf(4), cf(4)), true, false, false),
    RG32SFloat(VK_FORMAT_R32G32_SFLOAT, array(cf(4), cf(4)), true, false, false),

    RGBA8_SRGB(VK_FORMAT_R8G8B8A8_SRGB, array(cf(1), cf(1), cf(1), cf(1)), true, false, false),
    R8_SRGB(VK_FORMAT_R8_SRGB, array(cf(1)), true, false, false),
    BGR8_SRGB(VK_FORMAT_B8G8R8_SRGB, array(cf(1), cf(1), cf(1)), true, false, false),
    ABGR8_SRGB(VK_FORMAT_A8B8G8R8_SRGB_PACK32, array(cf(1), cf(1), cf(1), cf(1)), true, false, false),
    B8G8R8A8_SRGB(VK_FORMAT_B8G8R8A8_SRGB, array(cf(1), cf(1), cf(1), cf(1)), true, false, false),

    Depth32SFloat(VK_FORMAT_D32_SFLOAT, array(cf(4)), false, true, false),
    Depth32SFloat_Stencil8UInt(VK_FORMAT_D32_SFLOAT_S8_UINT, array(cf(4), c(1)), false, true, true),
    Depth24UNorm_Stencil8UInt(VK_FORMAT_D24_UNORM_S8_UINT, array(cf(3), c(1)), false, true, true),
    Depth16UNorm(VK_FORMAT_D16_UNORM, array(cf(2)), false, true, false),
    Depth16UNorm_Stencil8UInt(VK_FORMAT_D16_UNORM_S8_UINT, array(cf(2), c(1)), false, true, true);

    public enum Feature implements Flag<Feature> {

        DepthStencilAttachment(VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT),
        BlitDst(VK_FORMAT_FEATURE_BLIT_DST_BIT),
        BlitSrc(VK_FORMAT_FEATURE_BLIT_SRC_BIT),
        ColorAttachment(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BIT),
        SampledImage(VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT),
        ColorAttachmentBlend(VK_FORMAT_FEATURE_COLOR_ATTACHMENT_BLEND_BIT),
        SampledImageFilterLinear(VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT),
        StorageImageAtomic(VK_FORMAT_FEATURE_STORAGE_IMAGE_ATOMIC_BIT),
        StorageImage(VK_FORMAT_FEATURE_STORAGE_IMAGE_BIT),
        StorageTexelBufferAtomic(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_ATOMIC_BIT),
        StorageTexelBuffer(VK_FORMAT_FEATURE_STORAGE_TEXEL_BUFFER_BIT),
        UniformTexelBuffer(VK_FORMAT_FEATURE_UNIFORM_TEXEL_BUFFER_BIT),
        VertexBuffer(VK_FORMAT_FEATURE_VERTEX_BUFFER_BIT);

        private final int bits;

        Feature(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    private final int vkEnum, totalBytes;
    private final Component[] components;
    private final boolean color, depth, stencil;

    Format(int vkEnum, Component[] components, boolean color, boolean depth, boolean stencil) {
        this.vkEnum = vkEnum;
        this.components = components;
        this.color = color;
        this.depth = depth;
        this.stencil = stencil;
        int total = 0;
        for (Component c : components) {
            c.setOffset(total);
            total += c.getBytes();
        }
        this.totalBytes = total;
    }

    @Override
    public Iterator<Component> iterator() {
        return new ComponentIterator(components);
    }

    public int getVkEnum() {
        return vkEnum;
    }

    public Flag<VulkanImage.Aspect> getAspects() {
        int bits = 0;
        if (color) bits |= VulkanImage.Aspect.Color.bits();
        if (depth) bits |= VulkanImage.Aspect.Depth.bits();
        if (stencil) bits |= VulkanImage.Aspect.Stencil.bits();
        return Flag.of(bits);
    }

    public int getTotalBits() {
        return totalBytes * Byte.SIZE;
    }

    public int getTotalBytes() {
        return totalBytes;
    }

    public int getNumComponents() {
        return components.length;
    }

    public Component getComponent(int component) {
        return components[component];
    }

    public boolean isColor() {
        return color;
    }

    public boolean isDepth() {
        return depth;
    }

    public boolean isStencil() {
        return stencil;
    }

    public static Format byVkEnum(int vkEnum) {
        for (Format f : Format.values()) {
            if (f.getVkEnum() == vkEnum) {
                return f;
            }
        }
        return null;
    }
    
    private static Component[] array(Component... components) {
        return components;
    }
    
    private static Component c(int bytes) {
        return new Component(bytes, false);
    }
    
    private static Component cf(int bytes) {
        return new Component(bytes, true);
    }

    public static class Component {

        private final int bytes;
        private final boolean floatingPoint;
        private int offset;

        private Component(int bytes, boolean floatingPoint) {
            assert bytes > 0 : "Component size in bytes must be positive";
            this.bytes = bytes;
            this.floatingPoint = floatingPoint && bytes >= 4;
        }
        
        private void setOffset(int offset) {
            this.offset = offset;
        }

        public int getBytes() {
            return bytes;
        }

        public int getOffset() {
            return offset;
        }

        public boolean isFloatingPoint() {
            return floatingPoint;
        }

        public Component putByte(ByteBuffer buffer, int position, byte value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, value); break;
                case 2: case 3: buffer.putShort(position, value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, value);
                    else buffer.putInt(position, value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, value);
                }
            }
            return this;
        }

        public Component putShort(ByteBuffer buffer, int position, short value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, (byte)value); break;
                case 2: case 3: buffer.putShort(position, value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, value);
                    else buffer.putInt(position, value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, value);
                }
            }
            return this;
        }

        public Component putInt(ByteBuffer buffer, int position, int value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, (byte)value); break;
                case 2: case 3: buffer.putShort(position, (short)value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, value);
                    else buffer.putInt(position, value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, value);
                }
            }
            return this;
        }

        public Component putFloat(ByteBuffer buffer, int position, float value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, (byte)value); break;
                case 2: case 3: buffer.putShort(position, (short)value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, value);
                    else buffer.putInt(position, (int)value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, (long)value);
                }
            }
            return this;
        }

        public Component putDouble(ByteBuffer buffer, int position, double value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, (byte)value); break;
                case 2: case 3: buffer.putShort(position, (short)value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, (float)value);
                    else buffer.putInt(position, (int)value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, (long)value);
                }
            }
            return this;
        }

        public Component putLong(ByteBuffer buffer, int position, long value) {
            position += offset;
            switch (bytes) {
                case 1: buffer.put(position, (byte)value); break;
                case 2: case 3: buffer.putShort(position, (short)value); break;
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) buffer.putFloat(position, value);
                    else buffer.putInt(position, (int)value);
                } break;
                default: {
                    if (floatingPoint) buffer.putDouble(position, value);
                    else buffer.putLong(position, value);
                }
            }
            return this;
        }

        public byte getByte(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return (byte)buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return (byte)buffer.getFloat(position);
                    else return (byte)buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return (byte)buffer.getDouble(position);
                    else return (byte)buffer.getLong(position);
                }
            }
        }

        public short getShort(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return (short)buffer.getFloat(position);
                    else return (short)buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return (short)buffer.getDouble(position);
                    else return (short)buffer.getLong(position);
                }
            }
        }

        public int getInt(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return (int)buffer.getFloat(position);
                    else return buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return (int)buffer.getDouble(position);
                    else return (int)buffer.getLong(position);
                }
            }
        }

        public float getFloat(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return buffer.getFloat(position);
                    else return buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return (float)buffer.getDouble(position);
                    else return buffer.getLong(position);
                }
            }
        }

        public double getDouble(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return buffer.getFloat(position);
                    else return buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return buffer.getDouble(position);
                    else return buffer.getLong(position);
                }
            }
        }

        public long getLong(ByteBuffer buffer, int position) {
            position += offset;
            switch (bytes) {
                case 1: return buffer.get(position);
                case 2: case 3: return buffer.getShort(position);
                case 4: case 5: case 6: case 7: {
                    if (floatingPoint) return (long)buffer.getFloat(position);
                    else return buffer.getInt(position);
                }
                default: {
                    if (floatingPoint) return (long)buffer.getDouble(position);
                    else return buffer.getLong(position);
                }
            }
        }

    }

    private static class ComponentIterator implements Iterator<Component> {

        private final Component[] components;
        private int index = 0;

        private ComponentIterator(Component[] components) {
            this.components = components;
        }

        @Override
        public boolean hasNext() {
            return index < components.length;
        }

        @Override
        public Component next() {
            return components[index++];
        }

    }

}
