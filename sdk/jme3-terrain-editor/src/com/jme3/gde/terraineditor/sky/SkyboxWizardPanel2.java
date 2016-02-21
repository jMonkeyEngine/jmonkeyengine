/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.terraineditor.sky;

import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.awt.Component;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SkyboxWizardPanel2 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private Component component;

    private boolean multipleTextures;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    public Component getComponent() {
        if (component == null) {
            component = new SkyboxVisualPanel2();
        }
        return component;
    }

    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx(SampleWizardPanel1.class);
    }

    public boolean isValid() {
        // If it is always OK to press Next or Finish, then:
        return true;
        // If it depends on some condition (form filled out...), then:
        // return someCondition();
        // and when this condition changes (last form field filled in...) then:
        // fireChangeEvent();
        // and uncomment the complicated stuff below.
    }
    
    @Override
    public final void addChangeListener(ChangeListener l) {
    }
    
    @Override
    public final void removeChangeListener(ChangeListener l) {
    }
    /*
    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1); // or can use ChangeSupport in NB 6.0
    public final void addChangeListener(ChangeListener l) {
    synchronized (listeners) {
    listeners.add(l);
    }
    }
    public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
    listeners.remove(l);
    }
    }
    protected final void fireChangeEvent() {
    Iterator<ChangeListener> it;
    synchronized (listeners) {
    it = new HashSet<ChangeListener>(listeners).iterator();
    }
    ChangeEvent ev = new ChangeEvent(this);
    while (it.hasNext()) {
    it.next().stateChanged(ev);
    }
    }
     */
    
    @Override
    public void validate() throws WizardValidationException {
        SkyboxVisualPanel2 sky = (SkyboxVisualPanel2)component;
        
        /* Check if there are empty textures */
        if (multipleTextures) {
            if (sky.getEditorNorth().getAsText() == null) { throw new WizardValidationException(null, " Texture North: Missing texture!", null); }
            if (sky.getEditorSouth().getAsText() == null) { throw new WizardValidationException(null, " Texture South: Missing texture!", null); }
            if (sky.getEditorWest().getAsText() == null) { throw new WizardValidationException(null, " Texture West: Missing texture!", null); }
            if (sky.getEditorEast().getAsText() == null) { throw new WizardValidationException(null, " Texture East: Missing texture!", null); }
            if (sky.getEditorTop().getAsText() == null) { throw new WizardValidationException(null, " Texture Top: Missing texture!", null); }
            if (sky.getEditorBottom().getAsText() == null) { throw new WizardValidationException(null, " Texture Bottom: Missing texture!", null); }
            
            /* Prevent Null-Pointer Exception. If this is triggered, the Texture has no Image or the AssetKey is invalid (which should never happen) */
            if (sky.getEditorNorth().getValue() == null || ((Texture)sky.getEditorNorth().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture North: Cannot load texture!", null); }
            if (sky.getEditorSouth().getValue() == null || ((Texture)sky.getEditorSouth().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture South: Cannot load texture!", null); }
            if (sky.getEditorWest().getValue() == null || ((Texture)sky.getEditorWest().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture West: Cannot load texture!", null); }
            if (sky.getEditorEast().getValue() == null || ((Texture)sky.getEditorEast().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture East: Cannot load texture!", null); }
            if (sky.getEditorTop().getValue() == null || ((Texture)sky.getEditorTop().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture Top: Cannot load texture!", null); }
            if (sky.getEditorBottom().getValue() == null || ((Texture)sky.getEditorBottom().getValue()).getImage() == null) { throw new WizardValidationException(null, " Texture Bottom: Cannot load texture!", null); }
            
            /* Check for squares */
            Image I = ((Texture)sky.getEditorNorth().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture North: Image has to be a square (width == height)!", null); }
            I = ((Texture)sky.getEditorSouth().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture South: Image has to be a square (width == height)!", null); }
            I = ((Texture)sky.getEditorWest().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture West: Image has to be a square (width == height)!", null); }
            I = ((Texture)sky.getEditorEast().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture East: Image has to be a square (width == height)!", null); }
            I = ((Texture)sky.getEditorTop().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture Top: Image has to be a square (width == height)!", null); }
            I = ((Texture)sky.getEditorBottom().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Texture Bottom: Image has to be a square (width == height)!", null); }
        } else {
            if (sky.getEditorSingle().getAsText() == null){ throw new WizardValidationException(null, " Single Texture: Missing texture!", null); }
            if (sky.getEditorSingle().getValue() == null || ((Texture)sky.getEditorSingle().getValue()).getImage() == null){ throw new WizardValidationException(null, " Single Texture: Cannot load texture!", null); }
            Image I = ((Texture)sky.getEditorSingle().getValue()).getImage();
            if (I.getWidth() != I.getHeight()) { throw new WizardValidationException(null, " Single Texture: Image has to be a square (width == height)!", null); }
        }
    }
    
    // You can use a settings object to keep track of state. Normally the
    // settings object will be the WizardDescriptor, so you can use
    // WizardDescriptor.getProperty & putProperty to store information entered
    // by the user.
    @Override
    public void readSettings(WizardDescriptor settings) {
        multipleTextures = (Boolean)settings.getProperty("multipleTextures");
        SkyboxVisualPanel2 comp = (SkyboxVisualPanel2) getComponent();
        if (multipleTextures) {
            comp.getMultipleTexturePanel().setVisible(true);
            comp.getSingleTexturePanel().setVisible(false);
        } else {
            comp.getMultipleTexturePanel().setVisible(false);
            comp.getSingleTexturePanel().setVisible(true);
        }
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        SkyboxVisualPanel2 comp = (SkyboxVisualPanel2) getComponent();
        if (multipleTextures) {
            settings.putProperty("textureSouth", (Texture)comp.getEditorSouth().getValue());
            settings.putProperty("textureNorth", (Texture)comp.getEditorNorth().getValue());
            settings.putProperty("textureEast", (Texture)comp.getEditorEast().getValue());
            settings.putProperty("textureWest", (Texture)comp.getEditorWest().getValue());
            settings.putProperty("textureTop", (Texture)comp.getEditorTop().getValue());
            settings.putProperty("textureBottom", (Texture)comp.getEditorBottom().getValue());
            float x = new Float(comp.getNormal1X().getText());
            float y = new Float(comp.getNormal1Y().getText());
            float z = new Float(comp.getNormal1Z().getText());
            settings.putProperty("normalScale", new Vector3f(x,y,z) );
        } else {
            settings.putProperty("textureSingle", (Texture)comp.getEditorSingle().getValue());
            float x = new Float(comp.getNormal2X().getText());
            float y = new Float(comp.getNormal2Y().getText());
            float z = new Float(comp.getNormal2Z().getText());
            settings.putProperty("normalScale", new Vector3f(x,y,z) );
            settings.putProperty("envMapType", comp.getEnvMapType());         
            settings.putProperty("flipY", comp.getFlipYCheckBox().isSelected());
        }
    }
}
