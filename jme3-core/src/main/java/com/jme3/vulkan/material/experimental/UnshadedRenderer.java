package com.jme3.vulkan.material.experimental;

import com.jme3.material.Material;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.material.structs.UnshadedParams;

import java.util.List;

public class UnshadedRenderer {

    public void render(Material material) {
        /*
        There are 2 types of buffers:
            1. Buffers that are local to a material. Their contents don't change often
               so it's best to use more memory per material and keep their contents on the gpu.
            2. Buffers that are local to the renderer. Their contents change often, so it's
               best to save memory and use the same buffer for every material.
            3. Buffers that are shared among multiple renderers. Their content changes often,
               and renderers are likely to be using the same buffer layout.
        How do we identify which buffers fall into which category?
            1. Have the user/shader specify an update hint. For the camera buffer, this would
               be specified by the shader. For random stuff the user happens to want to update
               often, the hint would be specified by the user.
            2. For renderer-local and renderer-global buffers, I think these concepts can be
               merged into one, since the only difference is the formatting. The format can be
               identified via the struct type. So the renderer manager will store all renderer
               buffers by the struct used on them.
        Next, a buffer is a collection of parameters. How does the user specify the update
        hint when the user only has access to individual parameters?
            1. Treat as renderer managed if at least one hint is Stream.
            2. Treat as renderer managed if a majority of hints are Stream.
            3. Treat as renderer managed if all hints are Stream.
        */
        UpdateHint hint = material.getParameter("Color").getUpdateHint();
        if (hint == UpdateHint.Stream) {
            MappableBuffer buffer = manager.getBuffer(new UnshadedParams());
        } else {
            MappableBuffer buffer = material.getBuffer(getClass(), new UnshadedParams());
        }
    }

}
