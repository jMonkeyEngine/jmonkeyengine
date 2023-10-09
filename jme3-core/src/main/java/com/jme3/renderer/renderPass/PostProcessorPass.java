package com.jme3.renderer.renderPass;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGPass;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.util.SafeArrayList;

/**
 * @author JohnKkk
 */
public class PostProcessorPass extends FGPass {
    public PostProcessorPass(String name) {
        super(name);
    }

    @Override
    public void execute(FGRenderContext renderContext) {
        renderContext.setDepthRange(0, 1);
        ViewPort vp = renderContext.viewPort;
        SafeArrayList<SceneProcessor> processors = vp.getProcessors();
        if (processors != null) {
//            if (prof!=null) prof.vpStep(VpStep.PostFrame, vp, null);
            for (SceneProcessor proc : processors.getArray()) {
//                if (prof != null) prof.spStep(SpStep.ProcPostFrame, proc.getClass().getSimpleName());
                proc.postFrame(vp.getOutputFrameBuffer());
            }
//            if (prof != null) prof.vpStep(VpStep.ProcEndRender, vp, null);
        }
    }
}
