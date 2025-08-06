package com.jme3.vulkan.pass;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSubpassDependency;

import java.util.List;

/**
 * Immutable definition of a render pass dependency.
 */
public class SubpassDependency {

    private final Subpass srcSubpass, dstSubpass;
    private int srcStageMask, srcAccessMask;
    private int dstStageMask, dstAccessMask;

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
                .srcStageMask(srcStageMask)
                .srcAccessMask(srcAccessMask)
                .dstStageMask(dstStageMask)
                .dstAccessMask(dstAccessMask);
    }

    public void setSrcStageMask(int srcStageMask) {
        this.srcStageMask = srcStageMask;
    }

    public void setSrcAccessMask(int srcAccessMask) {
        this.srcAccessMask = srcAccessMask;
    }

    public void setDstStageMask(int dstStageMask) {
        this.dstStageMask = dstStageMask;
    }

    public void setDstAccessMask(int dstAccessMask) {
        this.dstAccessMask = dstAccessMask;
    }

    public Subpass getSrcSubpass() {
        return srcSubpass;
    }

    public Subpass getDstSubpass() {
        return dstSubpass;
    }

    public int getSrcStageMask() {
        return srcStageMask;
    }

    public int getSrcAccessMask() {
        return srcAccessMask;
    }

    public int getDstStageMask() {
        return dstStageMask;
    }

    public int getDstAccessMask() {
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
