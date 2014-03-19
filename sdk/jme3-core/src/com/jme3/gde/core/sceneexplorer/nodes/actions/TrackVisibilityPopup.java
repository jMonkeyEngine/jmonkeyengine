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
package com.jme3.gde.core.sceneexplorer.nodes.actions;

import com.jme3.gde.core.sceneexplorer.nodes.JmeAnimControl;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.util.actions.Presenter;

/**
 *
 * @author normenhansen
 */
public class TrackVisibilityPopup extends AbstractAction implements Presenter.Popup {

    protected JmeAnimControl jmeControl;

    public TrackVisibilityPopup(JmeAnimControl jmeControl) {
        this.jmeControl = jmeControl;
    }

    public void actionPerformed(ActionEvent e) {
    }

    public JMenuItem getPopupPresenter() {
        JMenu result = new JMenu("Display...");
        JCheckBoxMenuItem boneTrackItem = new JCheckBoxMenuItem(new AbstractAction("Bone tracks") {

            public void actionPerformed(ActionEvent e) {
                jmeControl.setDisplayBoneTracks(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        boneTrackItem.setSelected(jmeControl.isDisplayBoneTracks());
        result.add(boneTrackItem);
        JCheckBoxMenuItem effectTrackItem = new JCheckBoxMenuItem(new AbstractAction("Effect tracks") {

            public void actionPerformed(ActionEvent e) {
                jmeControl.setDisplayEffectTracks(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        effectTrackItem.setSelected(jmeControl.isDisplayEffectTracks());
        result.add(effectTrackItem);
        JCheckBoxMenuItem audioTrackItem = new JCheckBoxMenuItem(new AbstractAction("Audio tracks") {

            public void actionPerformed(ActionEvent e) {
                jmeControl.setDisplayAudioTracks(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }
        });
        audioTrackItem.setSelected(jmeControl.isDisplayAudioTracks());
        result.add(audioTrackItem);


        return result;
    }
}
