package com.jme3.vulkan.pass;

import com.jme3.vulkan.pipelines.Access;
import com.jme3.vulkan.pipelines.PipelineStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSubpassDependency;

import java.util.List;

/**
 * Immutable definition of a render pass dependency.
 */
public class SubpassDependency {

    private final Subpass srcSubpass, dstSubpass;
    private Flag<PipelineStage> srcStageMask, dstStageMask;
    private Flag<Access> srcAccessMask, dstAccessMask;

    protected SubpassDependency(Subpass srcSubpass, Subpass dstSubpass) {
        this.srcSubpass = srcSubpass;
        this.dstSubpass = dstSubpass;
    }

    protected SubpassDependency(SubpassDependency base, List<Subpass> subpasses) {
        srcSubpass = base.srcSubpass != null ? subpasses.get(base.srcSubpass.getPosition()) : null;
        dstSubpass = base.dstSubpass != null ? subpasses.get(base.dstSubpass.getPosition()) : null;
        this.srcStageMask = base.srcStageMask;
        this.srcAccessMask = base.srcAccessMask;
        this.dstStageMask = base.dstStageMask;
        this.dstAccessMask = base.dstAccessMask;
    }

    public void fillStruct(VkSubpassDependency struct) {
        struct.srcSubpass(srcSubpass != null ? srcSubpass.getPosition() : VK10.VK_SUBPASS_EXTERNAL)
                .dstSubpass(dstSubpass != null ? dstSubpass.getPosition() : VK10.VK_SUBPASS_EXTERNAL)
                .srcStageMask(srcStageMask.bits())
                .srcAccessMask(srcAccessMask.bits())
                .dstStageMask(dstStageMask.bits())
                .dstAccessMask(dstAccessMask.bits());
    }

    public void setSrcStageMask(Flag<PipelineStage> srcStageMask) {
        this.srcStageMask = srcStageMask;
    }

    public void setSrcAccessMask(Flag<Access> srcAccessMask) {
        this.srcAccessMask = srcAccessMask;
    }

    public void setDstStageMask(Flag<PipelineStage> dstStageMask) {
        this.dstStageMask = dstStageMask;
    }

    public void setDstAccessMask(Flag<Access> dstAccessMask) {
        this.dstAccessMask = dstAccessMask;
    }

    public Subpass getSrcSubpass() {
        return srcSubpass;
    }

    public Subpass getDstSubpass() {
        return dstSubpass;
    }

    public Flag<PipelineStage> getSrcStageMask() {
        return srcStageMask;
    }

    public Flag<Access> getSrcAccessMask() {
        return srcAccessMask;
    }

    public Flag<PipelineStage> getDstStageMask() {
        return dstStageMask;
    }

    public Flag<Access> getDstAccessMask() {
        return dstAccessMask;
    }

    public boolean isSourceExternal() {
        return srcSubpass == null;
    }

    public boolean isDestinationExternal() {
        return dstSubpass == null;
    }

    public boolean isCompatible(SubpassDependency dependency) {
        return srcSubpass.getPosition() == dependency.srcSubpass.getPosition()
            && dstSubpass.getPosition() == dependency.dstSubpass.getPosition()
            && srcStageMask == dependency.srcStageMask
            && srcAccessMask == dependency.srcAccessMask
            && dstStageMask == dependency.dstStageMask
            && dstAccessMask == dependency.dstAccessMask;
    }

}
