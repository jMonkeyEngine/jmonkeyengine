/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser.properties;

import com.jme3.gde.assetpack.project.AssetPackProject;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node.Property;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class ElementAttributeProperty extends Property<String> {

    private boolean readOnly;
    private Element element;
    private String attribute;
    private PropertyEditor editor;
    private Project project;

    public ElementAttributeProperty(Project project, Element element, String attribute) {
        this(project, element, attribute, false);
    }

    public ElementAttributeProperty(Project project, Element element, String attribute, String[] tags) {
        this(project, element, attribute, tags, false);
    }

    public ElementAttributeProperty(Project project, Element element, String attribute, boolean readOnly) {
        this(project, element, attribute, null, readOnly);
    }

    public ElementAttributeProperty(Project project, Element element, String attribute, String[] tags, boolean readOnly) {
        super(String.class);
        this.project = project;
        this.readOnly = readOnly;
        this.element = element;
        this.attribute = attribute;
        setName(attribute);
        if (tags != null) {
            editor = new SelectionPropertyEditor(tags, element.getAttribute(attribute));
        }
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (editor != null) {
            return editor;
        }
        return super.getPropertyEditor();
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public String getValue() throws IllegalAccessException, InvocationTargetException {
        return element.getAttribute(attribute);
    }

    @Override
    public boolean canWrite() {
        if (this.project != null && this.project.getLookup().lookup(AssetPackProject.class) == null) {
            return false;
        }
        return !readOnly;
    }

    @Override
    public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        element.setAttribute(attribute, val);
        if (this.project == null) {
            return;
        }
        AssetPackProject project = this.project.getLookup().lookup(AssetPackProject.class);
        if (project != null) {
            project.saveSettings();
        }
    }
}
