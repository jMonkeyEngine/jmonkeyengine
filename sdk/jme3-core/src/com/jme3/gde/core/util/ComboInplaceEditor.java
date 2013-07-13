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
package com.jme3.gde.core.util;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Nehon
 */
public class ComboInplaceEditor implements InplaceEditor {
    
    private PropertyEditor editor = null;
    private PropertyModel model;
    private PropertyEnv env;
    private JComboBox combo = new JComboBox();
    
    public ComboInplaceEditor(List<String> list) {
        super();
        setList(list);
    }
    
    public final void setList(List<String> list) {
        combo.removeAllItems();
        for (String string : list) {
            combo.addItem(string);
        }
    }
    
    public int getNumElements(){
        return combo.getItemCount();
    }
    
    public void connect(PropertyEditor pe, PropertyEnv pe1) {
        editor = pe;
        env = pe1;
        reset();
    }
    
    public JComponent getComponent() {
        return combo;
    }
    
    public void setAsText(String s) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (((String) combo.getItemAt(i)).equals(s)) {
                combo.setSelectedIndex(i);
                return;
            }
        }        
    }
    
    public void clear() {
        editor = null;
        model = null;
    }
    
    public Object getValue() {
        return combo.getSelectedItem();
        
    }
    
    public void setValue(Object o) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if ((combo.getItemAt(i)).equals(o)) {
                combo.setSelectedIndex(i);
                return;
            }
        }        
    }
    
    public boolean supportsTextEntry() {
        return true;
    }
    
    public void reset() {        
        
        
    }
    
    public KeyStroke[] getKeyStrokes() {
        return new KeyStroke[0];
    }
    
    public PropertyEditor getPropertyEditor() {
        return editor;
    }
    
    public PropertyModel getPropertyModel() {
        return model;
    }
    
    public void setPropertyModel(PropertyModel pm) {
        
        this.model = pm;
    }
    
    public boolean isKnownComponent(Component cmpnt) {
        return cmpnt == combo || combo.isAncestorOf(cmpnt);
    }
    
    public void addActionListener(ActionListener al) {
        combo.addActionListener(al);
    }
    
    public void removeActionListener(ActionListener al) {
        combo.removeActionListener(al);
    }
}
