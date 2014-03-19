/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack.browser.nodes;

import com.jme3.gde.assetpack.XmlHelper;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.w3c.dom.Element;

/**
 *
 * @author normenhansen
 */
public class AssetPackBrowserChildren extends Children.Keys<Element> {

    private Project project;
    private Element[] xmlNode;
    private String[] categories;
    private String[] tags;

    public AssetPackBrowserChildren(Element xmlNode, Project project, String[] categories, String[] tags) {
        this.xmlNode = new Element[]{xmlNode};
        this.categories = categories;
        this.tags = tags;
        this.project = project;
    }

    public AssetPackBrowserChildren(Element xmlNode, Project project) {
        this.xmlNode = new Element[]{xmlNode};
        this.project = project;
    }

    public AssetPackBrowserChildren(Element[] xmlNode, Project project, String[] categories, String[] tags) {
        this.xmlNode = xmlNode;
        this.categories = categories;
        this.tags = tags;
        this.project = project;
    }

    public AssetPackBrowserChildren(Element[] xmlNode, Project project) {
        this.xmlNode = xmlNode;
        this.project = project;
    }

    @Override
    protected void addNotify() {
        super.addNotify();
        setKeys(createKeys());
    }

    protected List<Element> createKeys() {
        LinkedList<Element> ret = new LinkedList<Element>();
        for (int i = 0; i < xmlNode.length; i++) {
            Element element = xmlNode[i];
            Element curElement = XmlHelper.findFirstChildElement(element);
            while (curElement != null) {
                if (checkElement(curElement)) {
                    ret.add(curElement);

                }
                curElement = XmlHelper.findNextSiblingElement(curElement);
            }

        }
        return ret;
    }

    private boolean checkElement(Element curElement) {
        if (!"asset".equals(curElement.getTagName())) {
            return false;
        }
        boolean checkCategories = false;
        boolean checkTags = false;
        if (categories != null) {
            for (int i = 0; i < categories.length; i++) {
                String string = categories[i].trim();
                String cats = curElement.getAttribute("categories");
                if (cats != null) {
                    String[] catg = cats.split(",");
                    for (int j = 0; j < catg.length; j++) {
                        String string1 = catg[j].trim();
                        if (string.equalsIgnoreCase(string1)) {
                            checkCategories = true;
                        }
                    }
                }
            }
        } else {
            checkCategories = true;
        }
        if (tags != null) {
            for (int i = 0; i < tags.length; i++) {
                String string = tags[i].trim();
                String tags = curElement.getAttribute("tags");
                if (tags != null) {
                    String[] catg = tags.split(",");
                    for (int j = 0; j < catg.length; j++) {
                        String string1 = catg[j].trim();
                        if (string.equalsIgnoreCase(string1)) {
                            checkTags = true;
                        }
                    }
                }
            }
        } else {
            checkTags = true;
        }
        return checkTags && checkCategories;
    }

    @Override
    protected Node[] createNodes(Element key) {
        return new Node[]{new AssetPackBrowserItem(key, project)};
    }
}
