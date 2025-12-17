package com.jme3.vulkan.pipeline.graphics;

import com.jme3.material.RenderState;
import com.jme3.vulkan.pipeline.BlendFactor;
import com.jme3.vulkan.pipeline.BlendOp;
import com.jme3.vulkan.pipeline.ColorComponent;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.RenderStateToVulkan;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

import java.util.Objects;
import java.util.function.Consumer;

public class ColorBlendAttachment {

    private Flag<ColorComponent> writeMask = ColorComponent.All;
    private IntEnum<BlendFactor> srcColorFactor = BlendFactor.One;
    private IntEnum<BlendFactor> dstColorFactor = BlendFactor.Zero;
    private IntEnum<BlendFactor> srcAlphaFactor = BlendFactor.One;
    private IntEnum<BlendFactor> dstAlphaFactor = BlendFactor.Zero;
    private IntEnum<BlendOp> colorBlend = BlendOp.Add;
    private IntEnum<BlendOp> alphaBlend = BlendOp.Add;
    private boolean enabled = true;

    public ColorBlendAttachment(RenderState state) {
        Builder a = new Builder();
        switch (state.getBlendMode()) {
            case Off: a.setEnabled(false); break;
            case Additive: {
                a.setSrcFactor(BlendFactor.One);
                a.setDstFactor(BlendFactor.One);
            } break;
            case PremultAlpha: {
                a.setSrcFactor(BlendFactor.One);
                a.setDstFactor(BlendFactor.OneMinusSrcAlpha);
            } break;
            case AlphaAdditive: {
                a.setSrcFactor(BlendFactor.SrcAlpha);
                a.setDstFactor(BlendFactor.One);
            } break;
            case Screen: case Color: {
                a.setSrcFactor(BlendFactor.One);
                a.setDstFactor(BlendFactor.OneMinusSrcColor);
            } break;
            case Alpha: {
                a.setSrcFactor(BlendFactor.SrcAlpha);
                a.setDstFactor(BlendFactor.OneMinusSrcAlpha);
            } break;
            case AlphaSumA: {
                a.setSrcColorFactor(BlendFactor.SrcAlpha);
                a.setDstColorFactor(BlendFactor.OneMinusSrcAlpha);
                a.setSrcAlphaFactor(BlendFactor.One);
                a.setDstAlphaFactor(BlendFactor.One);
            } break;
            case Modulate: {
                a.setSrcFactor(BlendFactor.DstColor);
                a.setDstFactor(BlendFactor.Zero);
            } break;
            case ModulateX2: {
                a.setSrcFactor(BlendFactor.DstColor);
                a.setDstFactor(BlendFactor.SrcColor);
            } break;
            case Exclusion: {
                a.setSrcFactor(BlendFactor.OneMinusDstColor);
                a.setDstFactor(BlendFactor.OneMinusSrcColor);
            } break;
            case Custom: {
                a.setColorBlend(RenderStateToVulkan.blendEquation(state.getBlendEquation()));
                a.setAlphaBlend(RenderStateToVulkan.blendEquationAlpha(state.getBlendEquationAlpha(), state.getBlendEquation()));
                a.setSrcColorFactor(RenderStateToVulkan.blendFunc(state.getCustomSfactorRGB()));
                a.setSrcAlphaFactor(RenderStateToVulkan.blendFunc(state.getCustomSfactorAlpha()));
                a.setDstColorFactor(RenderStateToVulkan.blendFunc(state.getCustomDfactorRGB()));
                a.setDstAlphaFactor(RenderStateToVulkan.blendFunc(state.getCustomDfactorAlpha()));
            } break;
        }
        a.build();
    }

    protected ColorBlendAttachment() {}

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColorBlendAttachment that = (ColorBlendAttachment) o;
        // factors and blend ops do not matter if blending is not enabled
        return enabled == that.enabled && (!enabled || (
                Flag.is(writeMask, that.writeMask)
                && IntEnum.is(srcColorFactor, that.srcColorFactor)
                && IntEnum.is(dstColorFactor, that.dstColorFactor)
                && IntEnum.is(srcAlphaFactor, that.srcAlphaFactor)
                && IntEnum.is(dstAlphaFactor, that.dstAlphaFactor)
                && IntEnum.is(colorBlend, that.colorBlend)
                && IntEnum.is(alphaBlend, that.alphaBlend)));
    }

    @Override
    public int hashCode() {
        // factors and blend ops do not matter if blending is not enabled
        return !enabled ? 0 : Objects.hash(writeMask, srcColorFactor, dstColorFactor, srcAlphaFactor, dstAlphaFactor, colorBlend, alphaBlend);
    }

    public void fill(VkPipelineColorBlendAttachmentState struct) {
        struct.colorWriteMask(writeMask.bits())
                .srcColorBlendFactor(srcColorFactor.getEnum())
                .dstColorBlendFactor(dstColorFactor.getEnum())
                .srcAlphaBlendFactor(srcAlphaFactor.getEnum())
                .dstAlphaBlendFactor(dstAlphaFactor.getEnum())
                .colorBlendOp(colorBlend.getEnum())
                .alphaBlendOp(alphaBlend.getEnum())
                .blendEnable(enabled);
    }

    public Flag<ColorComponent> getWriteMask() {
        return writeMask;
    }

    public IntEnum<BlendFactor> getSrcColorFactor() {
        return srcColorFactor;
    }

    public IntEnum<BlendFactor> getDstColorFactor() {
        return dstColorFactor;
    }

    public IntEnum<BlendFactor> getSrcAlphaFactor() {
        return srcAlphaFactor;
    }

    public IntEnum<BlendFactor> getDstAlphaFactor() {
        return dstAlphaFactor;
    }

    public IntEnum<BlendOp> getColorBlend() {
        return colorBlend;
    }

    public IntEnum<BlendOp> getAlphaBlend() {
        return alphaBlend;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static ColorBlendAttachment build() {
        return new ColorBlendAttachment();
    }

    public static ColorBlendAttachment build(Consumer<Builder> config) {
        Builder b = new ColorBlendAttachment().new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder {

        public ColorBlendAttachment build() {
            return ColorBlendAttachment.this;
        }

        public void setWriteMask(Flag<ColorComponent> writeMask) {
            ColorBlendAttachment.this.writeMask = writeMask;
        }

        public void setSrcColorFactor(IntEnum<BlendFactor> srcColorFactor) {
            ColorBlendAttachment.this.srcColorFactor = srcColorFactor;
        }

        public void setDstColorFactor(IntEnum<BlendFactor> dstColorFactor) {
            ColorBlendAttachment.this.dstColorFactor = dstColorFactor;
        }

        public void setSrcAlphaFactor(IntEnum<BlendFactor> srcAlphaFactor) {
            ColorBlendAttachment.this.srcAlphaFactor = srcAlphaFactor;
        }

        public void setDstAlphaFactor(IntEnum<BlendFactor> dstAlphaFactor) {
            ColorBlendAttachment.this.dstAlphaFactor = dstAlphaFactor;
        }

        public void setSrcFactor(IntEnum<BlendFactor> srcFactor) {
            setSrcColorFactor(srcFactor);
            setSrcAlphaFactor(srcFactor);
        }

        public void setDstFactor(IntEnum<BlendFactor> dstFactor) {
            setDstColorFactor(dstFactor);
            setDstAlphaFactor(dstFactor);
        }

        public void setColorBlend(IntEnum<BlendOp> colorBlend) {
            ColorBlendAttachment.this.colorBlend = colorBlend;
        }

        public void setAlphaBlend(IntEnum<BlendOp> alphaBlend) {
            ColorBlendAttachment.this.alphaBlend = alphaBlend;
        }

        public void setBlend(IntEnum<BlendOp> blend) {
            setColorBlend(blend);
            setAlphaBlend(blend);
        }

        public void setEnabled(boolean enabled) {
            ColorBlendAttachment.this.enabled = enabled;
        }

    }

}
