/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.palette;

import com.google.common.base.Predicate;

/**
 *
 * @author cris
 */
abstract class  ElementFilter implements Predicate<Class>{
    private final String name;

    public ElementFilter(String name) {
        this.name = name;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    
}
