/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser.nodes;

import com.jme3.gde.assetpack.XmlHelper;
import com.jme3.gde.assetpack.actions.AddAssetAction;
import com.jme3.gde.assetpack.actions.AddToProjectAction;
import com.jme3.gde.assetpack.actions.PreviewAssetAction;
import com.jme3.gde.assetpack.browser.AssetPackLibrary;
import com.jme3.gde.assetpack.browser.properties.ElementAttributeProperty;
import com.jme3.gde.assetpack.browser.properties.ElementNodeTextProperty;
import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.Image;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.nodes.Sheet.Set;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class AssetPackBrowserItem extends AbstractNode {

    public static final String[] ASSET_TYPES = new String[]{
        "model",
        "texture",
        "sound",
        "material",
        "other"};
//    public static final String[] ASSET_FORMATS = new String[]{
//        "ogrexml",
//        "wavefront",
//        "j3o",
//        "image",
//        "sound",
//        "other"};
    private Element item;
    private Image icon;
    private Project project;
    private PreviewAssetAction previewAction;
    private AddAssetAction addAction;
    private AddToProjectAction addProjectAction;

    public AssetPackBrowserItem(Element item, Project proj) {
        super(Children.LEAF, proj != null ? Lookups.fixed(item, proj, proj.getLookup().lookup(ProjectAssetManager.class)) : Lookups.fixed(item));
        addAction = new AddAssetAction(this);
        previewAction = new PreviewAssetAction(this);
        addProjectAction = new AddToProjectAction(this);
        this.item = item;
        this.project = proj;
        setName(item.getAttribute("name"));
        setImage();
    }

    private void setImage() {
        try {
            String add = item.getAttribute("type");
            icon = ImageUtilities.loadImage("/com/jme3/gde/assetpack/icons/" + add + ".gif");
        } catch (Exception e) {
        }
    }

    @Override
    public Image getIcon(int type) {
        if (icon != null) {
            return icon;
        }
        return super.getIcon(type);
    }

    public Action[] getActions(boolean context) {
        if (project.getLookup().lookup(AssetPackLibrary.class) != null) {
            return new Action[]{
                        addProjectAction,
                        addAction,
                        previewAction
                    };
        } else {
            return new Action[]{previewAction};
        }
    }

    @Override
    public Action getPreferredAction() {
//        if (project.getLookup().lookup(AssetPackLibrary.class) != null) {
//            return addAction;
//        } else {
        return previewAction;
//        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Set set = Sheet.createPropertiesSet();
        set.put(new ElementAttributeProperty(project, item, "name"));
        set.put(new ElementNodeTextProperty(project, item, "description"));
        set.put(new ElementAttributeProperty(project, item, "categories"));
        set.put(new ElementAttributeProperty(project, item, "tags"));
        Element elem = XmlHelper.findChildElement(item, "license");
        if ((project.getLookup().lookup(AssetPackLibrary.class) != null) && (elem == null || elem.getTextContent().trim().length() <= 0)) {
            set.put(new ElementNodeTextProperty(project, (Element) item.getParentNode().getParentNode(), "license"));
        } else {
            set.put(new ElementNodeTextProperty(project, item, "license"));
        }
        set.put(new ElementAttributeProperty(project, item, "type", ASSET_TYPES));
//        set.put(new ElementAttributeProperty(project, item, "format", ASSET_FORMATS));
        sheet.put(set);
        return sheet;
    }
}
