package com.jme3.vulkan.pass;

import com.jme3.vulkan.pipelines.PipelineBindPoint;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkSubpassDescription;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Immutable definition of a subpass within a render pass.
 */
public class Subpass {

    private final int position;
    private final PipelineBindPoint bindPoint;
    private final List<AttachmentReference> color = new ArrayList<>();
    private final List<AttachmentReference> input = new ArrayList<>();
    private final List<AttachmentReference> resolve = new ArrayList<>();
    private final List<AttachmentReference> preserve = new ArrayList<>();
    private AttachmentReference depthStencil;

    protected Subpass(int position, PipelineBindPoint bindPoint) {
        this.position = position;
        this.bindPoint = bindPoint;
    }

    protected Subpass(int position, Subpass base, List<Attachment> attachments) {
        this.position = position;
        this.bindPoint = base.bindPoint;
        transferRefs(base.color, color, attachments);
        transferRefs(base.input, input, attachments);
        transferRefs(base.resolve, resolve, attachments);
        transferRefs(base.preserve, preserve, attachments);
        if (base.depthStencil != null) {
            if (!base.depthStencil.isUnused()) {
                depthStencil = attachments.get(base.depthStencil.getAttachment().getPosition())
                        .createReference(base.depthStencil.getLayout());
            } else {
                depthStencil = AttachmentReference.unused(base.depthStencil.getLayout());
            }
        }
    }

    private void transferRefs(List<AttachmentReference> srcRefs, List<AttachmentReference> dstRefs, List<Attachment> attachments) {
        for (AttachmentReference r : srcRefs) {
            if (!r.isUnused()) {
                dstRefs.add(attachments.get(r.getAttachment().getPosition()).createReference(r.getLayout()));
            } else {
                dstRefs.add(AttachmentReference.unused(r.getLayout()));
            }
        }
    }

    public void fillStruct(MemoryStack stack, VkSubpassDescription struct) {
        struct.pipelineBindPoint(bindPoint.getVkEnum());
        if (!color.isEmpty()) {
            struct.colorAttachmentCount(color.size());
            struct.pColorAttachments(getColorReferences(stack));
        }
        if (depthStencil != null) {
            struct.pDepthStencilAttachment(getDepthStencil(stack));
        }
        if (!input.isEmpty()) {
            struct.pInputAttachments(getInputReferences(stack));
        }
        if (!resolve.isEmpty()) {
            struct.pResolveAttachments(getResolveReferences(stack));
        }
        if (!preserve.isEmpty()) {
            struct.pPreserveAttachments(getPreserveIndices(stack));
        }
    }

    public void addColorAttachment(AttachmentReference ref) {
        color.add(ref);
    }

    public void addInputAttachment(AttachmentReference ref) {
        input.add(ref);
    }

    public void addResolveAttachment(AttachmentReference ref) {
        resolve.add(ref);
    }

    public void addPreserveAttachment(AttachmentReference ref) {
        resolve.add(ref);
    }

    public void setDepthStencilAttachment(AttachmentReference depthStencil) {
        this.depthStencil = depthStencil;
    }

    public AttachmentReference getDepthStencil() {
        return depthStencil;
    }

    public List<AttachmentReference> getColor() {
        return color;
    }

    public List<AttachmentReference> getInput() {
        return input;
    }

    public List<AttachmentReference> getResolve() {
        return resolve;
    }

    public List<AttachmentReference> getPreserve() {
        return preserve;
    }

    public VkAttachmentReference getDepthStencil(MemoryStack stack) {
        VkAttachmentReference ref = VkAttachmentReference.calloc(stack);
        depthStencil.fillStruct(ref);
        return ref;
    }

    private VkAttachmentReference.Buffer getReferenceBuffer(MemoryStack stack, Collection<AttachmentReference> refs) {
        VkAttachmentReference.Buffer att = VkAttachmentReference.calloc(color.size(), stack);
        for (AttachmentReference ref : refs) {
            ref.fillStruct(att.get());
        }
        return att.flip();
    }

    public VkAttachmentReference.Buffer getColorReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, color);
    }

    public VkAttachmentReference.Buffer getInputReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, input);
    }

    public VkAttachmentReference.Buffer getResolveReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, resolve);
    }

    public IntBuffer getPreserveIndices(MemoryStack stack) {
        IntBuffer indices = stack.mallocInt(preserve.size());
        for (AttachmentReference ref : preserve) {
            indices.put(ref.getAttachmentPosition());
        }
        indices.flip();
        return indices;
    }

    public int getPosition() {
        return position;
    }

    public PipelineBindPoint getBindPoint() {
        return bindPoint;
    }

    public boolean hasDepthStencil() {
        return depthStencil != null;
    }

    public boolean isCompatible(Subpass pass) {
        return bindPoint == pass.bindPoint
            && hasDepthStencil() == pass.hasDepthStencil()
            && (!hasDepthStencil() || depthStencil.isCompatible(pass.depthStencil))
            && compareReferenceLists(color, pass.color)
            && compareReferenceLists(input, pass.input)
            && compareReferenceLists(resolve, pass.resolve)
            && compareReferenceLists(preserve, pass.preserve);
    }

    private boolean compareReferenceLists(List<AttachmentReference> list1, List<AttachmentReference> list2) {
        int lower = Math.min(list1.size(), list2.size());
        int higher = Math.max(list1.size(), list2.size());
        for (int i = 0; i < lower; i++) {
            if (!list1.get(i).isCompatible(list2.get(i))) {
                return false;
            }
        }
        for (int i = lower; i < higher; i++) {
            if (i < list1.size()) {
                if (!list1.get(i).isUnused()) {
                    return false;
                }
            } else if (!list2.get(i).isUnused()) {
                return false;
            }
        }
        return true;
    }

}
