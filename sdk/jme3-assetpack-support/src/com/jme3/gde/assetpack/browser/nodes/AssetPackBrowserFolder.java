/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser.nodes;

import com.jme3.gde.assetpack.project.AssetPackProject;
import com.jme3.gde.assetpack.project.actions.ImportAssetAction;
import java.awt.Image;
import javax.swing.Action;
import org.netbeans.api.project.Project;
import org.openide.nodes.AbstractNode;
import org.openide.util.ImageUtilities;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class AssetPackBrowserFolder extends AbstractNode {

    Image icon = ImageUtilities.loadImage("/com/jme3/gde/assetpack/icons/assets.gif");
    Project proj;

    public AssetPackBrowserFolder(Element[] elem, Project lib, String[] categories, String[] tags) {
        super(new AssetPackBrowserChildren(elem, lib, categories, tags));
        proj = lib;
        setName("Assets");
    }

    public AssetPackBrowserFolder(Element[] elem, Project lib) {
        super(new AssetPackBrowserChildren(elem, lib));
        proj = lib;
        setName("Assets");
    }

    public void refresh() {
        ((AssetPackBrowserChildren) getChildren()).addNotify();
    }

    public Action[] getActions(boolean context) {
        if (proj instanceof AssetPackProject) {
            return new Action[]{new ImportAssetAction((AssetPackProject)proj)};
        } else {
            return new Action[]{ //                    SystemAction.get(RenameAction.class),
                    //                    SystemAction.get(CopyAction.class),
                    //                    SystemAction.get(CutAction.class),
                    //                    SystemAction.get(PasteAction.class),
                    //                    SystemAction.get(DeleteAction.class)
                    };
        }
    }

    @Override
    public Image getIcon(int type) {
        return icon;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return icon;
    }
}
