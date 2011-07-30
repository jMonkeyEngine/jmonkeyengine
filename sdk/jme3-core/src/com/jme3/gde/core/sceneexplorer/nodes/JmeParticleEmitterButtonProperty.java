package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.effect.ParticleEmitter;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.util.ButtonInplaceEditor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;
import java.util.concurrent.Callable;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Nehon
 */
public class JmeParticleEmitterButtonProperty extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {

    JmeParticleEmitter pe;

    public JmeParticleEmitterButtonProperty(JmeParticleEmitter pe) {
        super();

        this.pe = pe;
    }
    PropertyEnv env;

    public void attachEnv(PropertyEnv env) {
        this.env = env;
        env.registerInplaceEditorFactory(this);
    }
    private ButtonInplaceEditor ed = null;

    public InplaceEditor getInplaceEditor() {
        if (ed == null) {
            ed = new ButtonInplaceEditor("Emit!");
            ed.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    SceneApplication.getApplication().enqueue(new Callable<Object>() {

                        public Object call() throws Exception {

                            pe.getEmitter().killAllParticles();
                            pe.getEmitter().emitAllParticles();
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
        pe.refresh(false);
    }
}
