/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.assetpack;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Configuration for a single asset item while loading
 * @author normenhansen
 */
public class AssetConfiguration {

    private Element assetElement;
    private List<NodeList> variationAssets;

    public AssetConfiguration(Element assetElement) {
        this.assetElement = assetElement;
    }

    public Element getAssetElement() {
        return assetElement;
    }

    public List<NodeList> getVariationAssets() {
        return variationAssets;
    }

    public void addVariationAssets(Element variationElement) {
        if (variationAssets == null) {
            variationAssets = new ArrayList<NodeList>();
        }
        variationAssets.add(variationElement.getElementsByTagName("file"));
    }

    public void clearExtraAssets() {
        variationAssets.clear();
    }

}
