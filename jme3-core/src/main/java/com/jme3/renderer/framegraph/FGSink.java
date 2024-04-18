/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public interface FGSink {
    
    public boolean isRequired();

    public String getRegisteredName();

    public String getLinkPassName();

    public String getLinkPassResName();
    
    public void setTarget(String inPassName, String inPassResName);
    
    public void bind(FGSource fgSource);
    
    public void postLinkValidate();

    public boolean isLinkValidate();
    
    public default FGBindable getBind() {
        return null;
    }
    
}
