/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser.properties;

import com.jme3.gde.assetpack.project.AssetPackProject;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.Node.Property;
import org.w3c.dom.Element;
import com.jme3.gde.assetpack.XmlHelper;
import org.netbeans.api.project.Project;

/**
 *
 * @author normenhansen
 */
public class ElementNodeTextProperty extends Property<String> {

    private boolean readOnly;
    private Element element;
    private String attribute;
    private PropertyEditor editor;
    private Project project;

    public ElementNodeTextProperty(Project project, Element element, String attribute) {
        this(project, element, attribute, false);
    }

    public ElementNodeTextProperty(Project project, Element element, String attribute, String[] tags) {
        this(project, element, attribute, tags, false);
    }

    public ElementNodeTextProperty(Project project, Element element, String attribute, boolean readOnly) {
        this(project, element, attribute, null, readOnly);
    }

    public ElementNodeTextProperty(Project project, Element element, String attribute, String[] tags, boolean readOnly) {
        super(String.class);
        this.readOnly = readOnly;
        this.element = element;
        this.attribute = attribute;
        this.project = project;
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
        return XmlHelper.findChildElement(element, attribute).getTextContent();// element.getAttribute(attribute);
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
        XmlHelper.findChildElement(element, attribute).setTextContent(val);
        if (this.project == null) {
            return;
        }
        AssetPackProject project = this.project.getLookup().lookup(AssetPackProject.class);
        if (project != null) {
            project.saveSettings();
        }
    }
}
