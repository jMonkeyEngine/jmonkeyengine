package com.jme3.vulkan.pipelines.newnewstates;

import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipelines.Pipeline;
import com.jme3.vulkan.pipelines.PipelineLayout;
import com.jme3.vulkan.pipelines.states.*;
import com.jme3.vulkan.pipelines.states.PipelineState;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class GraphicsState implements PipelineState<VkGraphicsPipelineCreateInfo> {

    private Subpass subpass;
    private PipelineLayout layout;
    private Flag<Pipeline.Create> flags = Flag.empty();
    private final Map<Integer, ShaderState> shaders = new HashMap<>();
    private VertexInputState vertexInput;
    private InputAssemblyState inputAssembly;
    private ViewportState viewport;
    private DepthStencilState depthStencil;
    private RasterizationState rasterization;
    private MultisampleState multisample;
    private ColorBlendState colorBlend;
    private DynamicState dynamic;
    protected long localVersion = 0L;

    @Override
    public VkGraphicsPipelineCreateInfo create(MemoryStack stack) {
        return VkGraphicsPipelineCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
    }

    @Override
    public VkGraphicsPipelineCreateInfo fill(MemoryStack stack, VkGraphicsPipelineCreateInfo struct) {
        struct.flags(flags.bits())
                .layout(layout.getNativeObject())
                .renderPass(subpass.getPass().getNativeObject())
                .subpass(subpass.getPosition())
                .basePipelineHandle(VK_NULL_HANDLE)
                .basePipelineIndex(-1);
        if (!shaders.isEmpty()) {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
            for (ShaderState s : shaders.values()) {
                s.fill(stack, stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO));
            }
            stageBuf.flip();
            struct.stageCount(stageBuf.limit()).pStages(stageBuf);
        }
        return struct.pVertexInputState(vertexInput.fill(stack))
                .pInputAssemblyState(inputAssembly.fill(stack))
                .pViewportState(viewport.fill(stack))
                .pDepthStencilState(depthStencil.fill(stack))
                .pRasterizationState(rasterization.fill(stack))
                .pMultisampleState(multisample.fill(stack))
                .pColorBlendState(colorBlend.fill(stack))
                .pDynamicState(dynamic.fill(stack));
    }

    @Override
    public long getCurrentVersion() {
        long version = localVersion
                + PipelineState.versionOf(vertexInput)
                + PipelineState.versionOf(inputAssembly)
                + PipelineState.versionOf(viewport)
                + PipelineState.versionOf(depthStencil)
                + PipelineState.versionOf(rasterization)
                + PipelineState.versionOf(multisample)
                + PipelineState.versionOf(colorBlend)
                + PipelineState.versionOf(dynamic);
        for (ShaderState shader : shaders.values()) {
            version += shader.getCurrentVersion();
        }
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GraphicsState that = (GraphicsState) o;
        return subpass == that.subpass
                && layout == that.layout
                && Flag.is(flags, that.flags)
                && Objects.equals(shaders, that.shaders)
                && Objects.equals(vertexInput, that.vertexInput)
                && Objects.equals(inputAssembly, that.inputAssembly)
                && Objects.equals(viewport, that.viewport)
                && Objects.equals(depthStencil, that.depthStencil)
                && Objects.equals(rasterization, that.rasterization)
                && Objects.equals(multisample, that.multisample)
                && Objects.equals(colorBlend, that.colorBlend)
                && Objects.equals(dynamic, that.dynamic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subpass, layout, flags, shaders, vertexInput, inputAssembly, viewport,
                depthStencil, rasterization, multisample, colorBlend, dynamic);
    }

    protected void incrementVersion(PipelineState<?> oldState, PipelineState<?> newState) {
        if (oldState != newState) {
            // must make sure the new state's version doesn't make this state "travel back in time"
            localVersion += 1 + Math.abs(PipelineState.versionOf(oldState) - PipelineState.versionOf(newState));
        }
    }

    public void addShader(Flag<ShaderStage> stage, ShaderModule module) {
        addShader(new ShaderState(stage, module));
    }

    public void addShader(Flag<ShaderStage> stage, ShaderModule module, String entryPoint) {
        addShader(new ShaderState(stage, module, entryPoint));
    }

    public void addShader(ShaderState shader) {
        if (shader.getStage().bitCount() > 1) {
            throw new IllegalArgumentException("Shader stage flag must specify only one shader stage.");
        }
        ShaderState old = shaders.put(shader.getStage().bits(), shader);
        incrementVersion(old, shader);
    }

    public void removeShader(Flag<ShaderStage> stage) {
        if (stage.bitCount() > 1) {
            throw new IllegalArgumentException("Shader stage flag must specify only one shader stage.");
        }
        incrementVersion(shaders.remove(stage.bits()), null);
    }

    public void setSubpass(Subpass subpass) {
        if (this.subpass != subpass) {
            this.subpass = subpass;
            localVersion++;
        }
    }

    public void setLayout(PipelineLayout layout) {
        if (this.layout != layout) {
            this.layout = layout;
            localVersion++;
        }
    }

    public void setFlags(Flag<Pipeline.Create> flags) {
        if (!Flag.is(this.flags, flags)) {
            this.flags = flags;
            localVersion++;
        }
    }

    public void setVertexInput(VertexInputState vertexInput) {
        incrementVersion(this.vertexInput, vertexInput);
        this.vertexInput = vertexInput;
    }

    public void setInputAssembly(InputAssemblyState inputAssembly) {
        incrementVersion(this.inputAssembly, inputAssembly);
        this.inputAssembly = inputAssembly;
    }

    public void setViewport(ViewportState viewport) {
        incrementVersion(this.viewport, viewport);
        this.viewport = viewport;
    }

    public void setDepthStencil(DepthStencilState depthStencil) {
        incrementVersion(this.depthStencil, depthStencil);
        this.depthStencil = depthStencil;
    }

    public void setRasterization(RasterizationState rasterization) {
        incrementVersion(this.rasterization, rasterization);
        this.rasterization = rasterization;
    }

    public void setMultisample(MultisampleState multisample) {
        incrementVersion(this.multisample, multisample);
        this.multisample = multisample;
    }

    public void setColorBlend(ColorBlendState colorBlend) {
        incrementVersion(this.colorBlend, colorBlend);
        this.colorBlend = colorBlend;
    }

    public void setDynamic(DynamicState dynamic) {
        incrementVersion(this.dynamic, dynamic);
        this.dynamic = dynamic;
    }

    public Subpass getSubpass() {
        return subpass;
    }

    public PipelineLayout getLayout() {
        return layout;
    }

    public Flag<Pipeline.Create> getFlags() {
        return flags;
    }

    public VertexInputState getVertexInput() {
        return vertexInput;
    }

    public InputAssemblyState getInputAssembly() {
        return inputAssembly;
    }

    public ViewportState getViewport() {
        return viewport;
    }

    public DepthStencilState getDepthStencil() {
        return depthStencil;
    }

    public RasterizationState getRasterization() {
        return rasterization;
    }

    public MultisampleState getMultisample() {
        return multisample;
    }

    public ColorBlendState getColorBlend() {
        return colorBlend;
    }

    public DynamicState getDynamic() {
        return dynamic;
    }

}
