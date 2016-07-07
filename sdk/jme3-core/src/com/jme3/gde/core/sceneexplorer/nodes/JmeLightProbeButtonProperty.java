package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.SceneToolController;
import com.jme3.gde.core.util.ButtonInplaceEditor;
import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;
import java.util.concurrent.Callable;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Nehon
 */
public class JmeLightProbeButtonProperty extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {

    JmeLightProbe probe;
    Node node;

    public JmeLightProbeButtonProperty(JmeLightProbe pe, Node node) {
        super();
        this.node = node;
        this.probe = pe;
    }
    PropertyEnv env;

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
        env.registerInplaceEditorFactory(this);
    }
    private ButtonInplaceEditor ed = null;

    @Override
    public InplaceEditor getInplaceEditor() {
        if (ed == null) {
            ed = new ButtonInplaceEditor("Refresh");
            ed.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    SceneApplication.getApplication().enqueue(new Callable<Object>() {

                        @Override
                        public Object call() throws Exception {

                            EnvironmentCamera envCam = SceneApplication.getApplication().getStateManager().getState(EnvironmentCamera.class);
                            SceneToolController toolController = SceneApplication.getApplication().getStateManager().getState(SceneToolController.class);
                            if (toolController != null) {
                                envCam.setPosition(toolController.getCursorLocation());
                            } else {
                                envCam.setPosition(new Vector3f(0, 0, 0));
                            }
                            LightProbeFactory.updateProbe(probe.getLightProbe(), envCam, node, new JmeLightProbeProgressHandler());

                            probe.setModified();

                            return null;
                        }
                    });
                }
            });
        }
        return ed;
    }

    @Override
    public boolean isPaintable() {
        return true;
    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {
        if (ed == null) {
            getInplaceEditor();
        }
        ed.setSize(box.width, box.height);
        ed.doLayout();
        Graphics g = gfx.create(box.x, box.y, box.width, box.height);
        ed.setOpaque(false);
        ed.paint(g);
        g.dispose();
        probe.refresh(false);
    }
}
