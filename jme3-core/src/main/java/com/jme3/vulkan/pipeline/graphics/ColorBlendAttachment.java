package com.jme3.vulkan.pipeline.graphics;

import com.jme3.material.RenderState;
import com.jme3.vulkan.pipeline.BlendFactor;
import com.jme3.vulkan.pipeline.BlendOp;
import com.jme3.vulkan.pipeline.ColorComponent;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.LegacyEnumConverter;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

import java.util.Objects;

public class ColorBlendAttachment implements Cloneable {

    private Flag<ColorComponent> writeMask = ColorComponent.All;
    private IntEnum<BlendFactor> srcColorFactor = BlendFactor.One;
    private IntEnum<BlendFactor> dstColorFactor = BlendFactor.Zero;
    private IntEnum<BlendFactor> srcAlphaFactor = BlendFactor.One;
    private IntEnum<BlendFactor> dstAlphaFactor = BlendFactor.Zero;
    private IntEnum<BlendOp> colorBlend = BlendOp.Add;
    private IntEnum<BlendOp> alphaBlend = BlendOp.Add;
    private boolean enabled = true;

    public ColorBlendAttachment() {}

    public ColorBlendAttachment(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColorBlendAttachment that = (ColorBlendAttachment) o;
        // factors and blend ops do not matter if blending is not enabled
        return enabled == that.enabled && (!enabled || (
                Flag.equals(writeMask, that.writeMask)
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

    @Override
    public ColorBlendAttachment clone() {
        try {
            return (ColorBlendAttachment)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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

    public void apply(RenderState state) {
        if (state.getBlendMode() == RenderState.BlendMode.Custom) {
            setColorBlend(LegacyEnumConverter.blendEquation(state.getBlendEquation()));
            setAlphaBlend(LegacyEnumConverter.blendEquationAlpha(state.getBlendEquationAlpha(), state.getBlendEquation()));
            setSrcColorFactor(LegacyEnumConverter.blendFunc(state.getCustomSfactorRGB()));
            setSrcAlphaFactor(LegacyEnumConverter.blendFunc(state.getCustomSfactorAlpha()));
            setDstColorFactor(LegacyEnumConverter.blendFunc(state.getCustomDfactorRGB()));
            setDstAlphaFactor(LegacyEnumConverter.blendFunc(state.getCustomDfactorAlpha()));
        } else {
            applyBlendMode(state.getBlendMode());
        }
    }

    public void applyBlendMode(RenderState.BlendMode mode) {
        switch (mode) {
            case Off: setEnabled(false); break;
            case Additive: {
                setSrcFactor(BlendFactor.One);
                setDstFactor(BlendFactor.One);
            } break;
            case PremultAlpha: {
                setSrcFactor(BlendFactor.One);
                setDstFactor(BlendFactor.OneMinusSrcAlpha);
            } break;
            case AlphaAdditive: {
                setSrcFactor(BlendFactor.SrcAlpha);
                setDstFactor(BlendFactor.One);
            } break;
            case Screen: case Color: {
                setSrcFactor(BlendFactor.One);
                setDstFactor(BlendFactor.OneMinusSrcColor);
            } break;
            case Alpha: {
                setSrcFactor(BlendFactor.SrcAlpha);
                setDstFactor(BlendFactor.OneMinusSrcAlpha);
            } break;
            case AlphaSumA: {
                setSrcColorFactor(BlendFactor.SrcAlpha);
                setDstColorFactor(BlendFactor.OneMinusSrcAlpha);
                setSrcAlphaFactor(BlendFactor.One);
                setDstAlphaFactor(BlendFactor.One);
            } break;
            case Modulate: {
                setSrcFactor(BlendFactor.DstColor);
                setDstFactor(BlendFactor.Zero);
            } break;
            case ModulateX2: {
                setSrcFactor(BlendFactor.DstColor);
                setDstFactor(BlendFactor.SrcColor);
            } break;
            case Exclusion: {
                setSrcFactor(BlendFactor.OneMinusDstColor);
                setDstFactor(BlendFactor.OneMinusSrcColor);
            } break;
            case Custom: throw new IllegalArgumentException("Not enough information to apply custom blend mode.");
        }
    }

    public void set(ColorBlendAttachment a) {
        writeMask = a.writeMask;
        srcColorFactor = a.srcColorFactor;
        srcAlphaFactor = a.srcAlphaFactor;
        dstColorFactor = a.dstColorFactor;
        dstAlphaFactor = a.dstAlphaFactor;
        colorBlend = a.colorBlend;
        alphaBlend = a.alphaBlend;
        enabled = a.enabled;
    }

    public void setWriteMask(Flag<ColorComponent> writeMask) {
        this.writeMask = writeMask;
    }

    public void setSrcFactor(IntEnum<BlendFactor> srcFactor) {
        setSrcColorFactor(srcFactor);
        setSrcAlphaFactor(srcFactor);
    }

    public void setDstFactor(IntEnum<BlendFactor> dstFactor) {
        setDstColorFactor(dstFactor);
        setDstAlphaFactor(dstFactor);
    }

    public void setSrcColorFactor(IntEnum<BlendFactor> srcColorFactor) {
        this.srcColorFactor = srcColorFactor;
    }

    public void setDstColorFactor(IntEnum<BlendFactor> dstColorFactor) {
        this.dstColorFactor = dstColorFactor;
    }

    public void setSrcAlphaFactor(IntEnum<BlendFactor> srcAlphaFactor) {
        this.srcAlphaFactor = srcAlphaFactor;
    }

    public void setDstAlphaFactor(IntEnum<BlendFactor> dstAlphaFactor) {
        this.dstAlphaFactor = dstAlphaFactor;
    }

    public void setBlend(IntEnum<BlendOp> blend) {
        setColorBlend(blend);
        setAlphaBlend(blend);
    }

    public void setColorBlend(IntEnum<BlendOp> colorBlend) {
        this.colorBlend = colorBlend;
    }

    public void setAlphaBlend(IntEnum<BlendOp> alphaBlend) {
        this.alphaBlend = alphaBlend;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

}
