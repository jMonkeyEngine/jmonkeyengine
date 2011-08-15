/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.materials.multiview.widgets;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.materials.MaterialProperty;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;

/**
 *
 * @author normenhansen
 */
public class WidgetFactory {

    public static MaterialPropertyWidget getWidget(MaterialProperty prop, ProjectAssetManager manager){      
        MaterialPropertyWidget widget;
        if(prop.getType().indexOf("Texture")>=0){
            widget=new TexturePanel(manager);
            widget.setProperty(prop);
            return widget;
        }
        else if("Boolean".equals(prop.getType())){
            widget=new BooleanPanel();
            widget.setProperty(prop);
            return widget;
        }
        else if("OnOff".equals(prop.getType())){
            widget=new OnOffPanel();
            widget.setProperty(prop);
            return widget;
        }
        else if("Float".equals(prop.getType())){
            widget=new FloatPanel();
            widget.setProperty(prop);
            return widget;
        }
        else if("Int".equals(prop.getType())){
            widget=new IntPanel();
            widget.setProperty(prop);
            return widget;
        }
        else if("Color".equals(prop.getType())){
            widget=new ColorPanel();
            widget.setProperty(prop);
            return widget;
        }
        else if("FaceCullMode".equals(prop.getType())){
            widget=new SelectionPanel();
            String[] strings=new String[FaceCullMode.values().length];
            for (int i = 0; i < strings.length; i++) {
                strings[i]=FaceCullMode.values()[i].name();
            }
            ((SelectionPanel)widget).setSelectionList(strings);
            widget.setProperty(prop);
            return widget;
        }
        else if("BlendMode".equals(prop.getType())){
            widget=new SelectionPanel();
            String[] strings=new String[BlendMode.values().length];
            for (int i = 0; i < strings.length; i++) {
                strings[i]=BlendMode.values()[i].name();
            }
            ((SelectionPanel)widget).setSelectionList(strings);
            widget.setProperty(prop);
            return widget;
        }
        widget = new TextPanel();      
        widget.setProperty(prop);
        return widget;
    }

}
