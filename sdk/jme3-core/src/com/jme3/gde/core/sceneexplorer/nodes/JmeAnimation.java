/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.Animation;
import com.jme3.animation.LoopMode;
import com.jme3.gde.core.icons.IconList;
import com.jme3.gde.core.properties.AnimationProperty;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ChannelDialog;
import com.jme3.gde.core.sceneexplorer.nodes.actions.ExtractSubAnimationDialog;
import com.jme3.gde.core.sceneexplorer.nodes.actions.impl.tracks.AudioTrackWizardAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.impl.tracks.EffectTrackWizardAction;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.openide.awt.Actions;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;

/**
 *
 * @author nehon
 */
@org.openide.util.lookup.ServiceProvider(service = SceneExplorerNode.class)
public class JmeAnimation extends AbstractSceneExplorerNode {

    private Animation animation;
    private Image icon;
    private JmeAnimControl jmeControl;
    private boolean playing = false;
    private LoopMode animLoopMode = LoopMode.Loop;
    private float animSpeed = 1.0f;
    private AnimChannel channel = null;

    public JmeAnimation() {
    }

    public JmeAnimation(JmeAnimControl control, Animation animation, JmeTrackChildren children, DataObject dataObject) {
        super(children);
        this.dataObject = dataObject;
        children.setDataObject(dataObject);
        this.animation = animation;
        this.jmeControl = control;
        lookupContents.add(this);
        lookupContents.add(animation);
        setName(animation.getName());
        children.setAnimation(this);
        children.setAnimControl(jmeControl);
        icon = IconList.animation.getImage();

    }

    @Override
    public Image getIcon(int type) {
        return icon;
    }

    @Override
    public Image getOpenedIcon(int type) {

        return icon;

    }

    public void toggleIcon(boolean enabled) {
        if (!playing) {
            icon = IconList.animation.getImage();

        } else {
            icon = IconList.animationPlay.getImage();

        }
        fireIconChange();
    }

    @Override
    public Action getPreferredAction() {
        return Actions.alwaysEnabled(new PlayAction(), "Play", "", false);

    }

    private void play() {
        playing = !playing;
        toggleIcon(playing);
        jmeControl.setAnim(this);
    }

    @Override
    protected Sheet createSheet() {
        //TODO: multithreading..
        Sheet sheet = Sheet.createDefault();
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName("Animation");
        set.setName(Animation.class.getName());
        if (animation == null) {
            return sheet;
        }

        set.put(new AnimationProperty(jmeControl.getLookup().lookup(AnimControl.class)));

        sheet.put(set);
        return sheet;
    }

    public void setChanged() {
        fireSave(true);
    }

    @Override
    public Action[] getActions(boolean context) {

        return new Action[]{Actions.alwaysEnabled(new PlayAction(), playing ? "Stop" : "Play", "", false),
                    Actions.alwaysEnabled(new PlayBackParamsAction(), "Playback parameters", "", false),
                    Actions.alwaysEnabled(new EffectTrackWizardAction(jmeControl.getLookup().lookup(AnimControl.class).getSpatial(), this), "Add Effect Track", "", false),
                    Actions.alwaysEnabled(new AudioTrackWizardAction(jmeControl.getLookup().lookup(AnimControl.class).getSpatial(), this), "Add Audio Track", "", false),
                    Actions.alwaysEnabled(new ExtractAnimationAction(), "Extract sub-animation", "", true)
                };
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    public void stop() {
        playing = false;
        toggleIcon(playing);
    }

    @Override
    public void destroy() throws IOException {
//        super.destroy();
//        final Spatial spat = getParentNode().getLookup().lookup(Spatial.class);
//        try {
//            SceneApplication.getApplication().enqueue(new Callable<Void>() {
//
//                public Void call() throws Exception {
//                    spat.removeControl(skeletonControl);
//                    return null;
//                }
//            }).get();
//            ((AbstractSceneExplorerNode) getParentNode()).refresh(true);
//        } catch (InterruptedException ex) {
//            Exceptions.printStackTrace(ex);
//        } catch (ExecutionException ex) {
//            Exceptions.printStackTrace(ex);
//        }
    }

    @Override
    public void refresh(boolean immediate) {
        super.refresh(immediate);
        ((JmeTrackChildren) getChildren()).refreshChildren(false);
    }

    public Class getExplorerObjectClass() {
        return Animation.class;
    }

    @Override
    public Class getExplorerNodeClass() {
        return JmeAnimation.class;
    }

    @Override
    public Node[] createNodes(Object key, DataObject key2, boolean cookie) {
        JmeTrackChildren children = new JmeTrackChildren(this, jmeControl);
        JmeAnimation jsc = new JmeAnimation(jmeControl, (Animation) key, children, key2);
        return new Node[]{jsc};
    }

    class PlayAction implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            final AnimControl control = jmeControl.getLookup().lookup(AnimControl.class);
            if (control == null) {
                return;
            }

            try {
                SceneApplication.getApplication().enqueue(new Callable<Void>() {
                    public Void call() throws Exception {
                        if (playing) {
                            control.clearChannels();
                            channel = null;
                            jmeControl.setAnim(null);
                            return null;
                        }
                        control.clearChannels();
                        channel = control.createChannel();
                        channel.setAnim(animation.getName());
                        innerSetLoopMode();
                        channel.setSpeed(animSpeed);
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                play();
                            }
                        });
                        return null;

                    }
                }).get();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
                return;
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
                return;
            }

        }
    }

    private void innerSetLoopMode() {
        final AnimControl control = jmeControl.getLookup().lookup(AnimControl.class);
        channel.setLoopMode(animLoopMode);
        if (animLoopMode == LoopMode.DontLoop) {
            control.addListener(new AnimEventListener() {
                public void onAnimCycleDone(AnimControl ac, AnimChannel ac1, String animName) {
                    if (animName.equals(animation.getName())) {
                        if (playing) {
                            control.clearChannels();
                            channel = null;
                            jmeControl.setAnim(null);
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    stop();
                                }
                            });
                        }

                        ac.removeListener(this);
                    }
                }

                public void onAnimChange(AnimControl ac, AnimChannel ac1, String animName) {
//                    if (animation.getName().equals(ac1.getAnimationName())) {
//                         java.awt.EventQueue.invokeLater(new Runnable() {
//
//                            public void run() {
//                                 stop();
//                            }
//                        });
//                    }
                }
            });
        }
    }

    class PlayBackParamsAction implements ActionListener {

        ChannelDialog dialog = new ChannelDialog(null, false, JmeAnimation.this);

        public void actionPerformed(ActionEvent e) {
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);

        }
    }

    public LoopMode getAnimLoopMode() {
        return animLoopMode;
    }

    public void setAnimLoopMode(LoopMode loopMode) {
        this.animLoopMode = loopMode;

        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {
                public Void call() throws Exception {
                    if (channel != null) {
                        innerSetLoopMode();
                    }

                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }


    }

    public float getAnimSpeed() {
        return animSpeed;
    }

    public void setAnimSpeed(float speed) {
        this.animSpeed = speed;
        try {
            SceneApplication.getApplication().enqueue(new Callable<Void>() {
                public Void call() throws Exception {
                    if (channel != null) {
                        channel.setSpeed(animSpeed);
                    }
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    class ExtractAnimationAction implements ActionListener {

        ExtractSubAnimationDialog dialog = new ExtractSubAnimationDialog();

        public void actionPerformed(ActionEvent e) {
            try {
                dialog.setAnimControl(JmeAnimation.this.jmeControl);
                dialog.setAnimation(JmeAnimation.this.animation);
                //animation
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);

                JmeAnimation.this.jmeControl.fireSave(true);
                JmeAnimation.this.jmeControl.refresh(true);
                JmeAnimation.this.jmeControl.refreshChildren();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
//    class AddTrackAction implements ActionListener {
//
//        public void actionPerformed(ActionEvent e) {
//
//            WizardDescriptor.Iterator iterator = new NewEffectTrackWizardIterator();
//            WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
//            // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
//            // {1} will be replaced by WizardDescriptor.Iterator.name()
//            wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})"));
//            wizardDescriptor.setTitle("Your wizard dialog title here");
//            Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
//            dialog.setVisible(true);
//            dialog.toFront();
//            boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
//            if (!cancelled) {
//                // do something
//            }
//            final AnimControl control = jmeControl.getLookup().lookup(AnimControl.class);
//            if (control == null) {
//                return;
//            }
//            try {
//                SceneApplication.getApplication().enqueue(new Callable<Void>() {
//
//                    public Void call() throws Exception {
//                        animation.addTrack(new EffectTrack());
//                        animation.addTrack(new AudioTrack());
//                        return null;
//                    }
//                }).get();
//            } catch (InterruptedException ex) {
//                Exceptions.printStackTrace(ex);
//            } catch (ExecutionException ex) {
//                Exceptions.printStackTrace(ex);
//            }
//            ((JmeTrackChildren) getChildren()).refreshChildren(false);
//        }
//    }
}
