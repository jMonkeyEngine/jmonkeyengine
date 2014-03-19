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
package com.jme3.gde.core.sceneexplorer.nodes.actions.impl;

import javax.swing.JOptionPane;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;

public class GenerateLODWizardPanel1 implements WizardDescriptor.ValidatingPanel<WizardDescriptor> {

    private float[] values;   
    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private GenerateLODVisualPanel1 component;

    // Get the visual component for the panel. In this template, the component
    // is kept separate. This can be more efficient: if the wizard is created
    // but never displayed, or not all panels are displayed, it is better to
    // create only those which really need to be visible.
    @Override
    public GenerateLODVisualPanel1 getComponent() {
        if (component == null) {
            component = new GenerateLODVisualPanel1();
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid() {

  
        // If it is always OK to press Next or Finish, then:
        return true;
        // If it depends on some condition (form filled out...) and
        // this condition changes (last form field filled in...) then
        // use ChangeSupport to implement add/removeChangeListener below.
        // WizardDescriptor.ERROR/WARNING/INFORMATION_MESSAGE will also be useful.
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {        
          component.setTriSize((Integer)wiz.getProperty("triSize"));
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        wiz.putProperty("reductionMethod", component.getReducitonMethod());
        wiz.putProperty("reductionValues", values);
    }

    public void validate() throws WizardValidationException {
      
        
        float[] vals = new float[component.getValuesFields().size()];
        
        for (int i = 0; i < component.getValuesFields().size(); i++) {
            try {
                String text = component.getValuesFields().get(i).getText();
                if(!text.trim().equals("")){                    
                    vals[i] = Float.parseFloat(text);
                }else{
                    if (i == 0) {
                        if (JOptionPane.showConfirmDialog(getComponent(),
                                "Warning there is no level value set.\nThis will remove all existing Level of detail from this model.\nDo you wish to continue?",
                                "Deleting LOD", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                            values = null;
                        } else {                          
                            throw new WizardValidationException(null, "No reduction value set", null);
                        }                        
                    }else{
                        values = new float[i];
                        System.arraycopy(vals, 0, values, 0, i);                      
                    }                    
                    return;
                }
            } catch (NumberFormatException e) {               
                throw new WizardValidationException( component.getValuesFields().get(i), "Invalid value for level "+(i+1), null);
            }
        }
        
    }
}
