/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author JohnKkk
 */
public abstract class FGSource {
    private String name;
    public FGSource(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public abstract void postLinkValidate();
    public abstract FGBindable yieldBindable();
}
