/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node.properties;

import java.beans.PropertyEditorSupport;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Nehon
 */
public class VectorPropertyEditor extends PropertyEditorSupport implements ExPropertyEditor, InplaceEditor.Factory {

    private PropertyEnv env;
    private VectorInplaceEditor editor;

    public VectorPropertyEditor(int capacity) {
        editor = new VectorInplaceEditor(capacity);
    }

    public void attachEnv(PropertyEnv env) {
        this.env = env;
        env.registerInplaceEditorFactory(this);
    }

    public InplaceEditor getInplaceEditor() {
        return editor;
    }

    @Override
    public String getAsText() {
        return (String) editor.getValue();
    }

    
    
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        editor.setValue(text);
    }


}
