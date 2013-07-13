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
package com.jme3.gde.core.properties;

import com.jme3.gde.core.util.ComboInplaceEditor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Nehon
 */
public class ComboBoxPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {

    private LinkedList<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
    PropertyEnv env;
    private ComboInplaceEditor ed = null;

    public ComboBoxPropertyEditor(List<String> list) {
        ed = new ComboInplaceEditor(list);
    }

    public void attachEnv(PropertyEnv env) {
        this.env = env;
        env.registerInplaceEditorFactory(this);
    }
    
    public void setList(List<String> list){
        ed.setList(list);
    }
   
    public int getNumElements(){
        return ed.getNumElements();
    }

    @Override
    public String getAsText() {
        return ed.getValue().toString();
    }

    @Override
    public void setAsText(String s) {
        Object o = ed.getValue();
        ((ComboInplaceEditor) ed).setAsText(s);
        notifyListeners(o, ed.getValue());
    }
 

    public InplaceEditor getInplaceEditor() {
        return ed;
    }

    @Override
    public void setValue(Object value) {
        ed.setValue(value);

    }

    @Override
    public Object getValue() {
        return ed.getValue();
    }

    public PropertyEnv getEnv() {
        return env;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(Object before, Object after) {
        for (Iterator<PropertyChangeListener> it = listeners.iterator(); it.hasNext();) {
            PropertyChangeListener propertyChangeListener = it.next();
            //TODO: check what the "programmatic name" is supposed to be here.. for now its Quaternion
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, null, before, after));
        }
    }
}
