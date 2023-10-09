/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author JohnKkk
 */
public abstract class FGSink {
    private String registeredName;
    private String linkPassName;
    private String linkPassResName;
    protected boolean bIsRequired = false;
    // Always validate?
    protected boolean bLinkValidate = false;
    protected FGSink(String registeredName){
        this.registeredName = registeredName;
    }

    public boolean isRequired() {
        return bIsRequired;
    }

    public String getRegisteredName() {
        return registeredName;
    }

    public String getLinkPassName() {
        return linkPassName;
    }

    public String getLinkPassResName() {
        return linkPassResName;
    }
    
    public void setTarget(String inPassName, String inPassResName){
        linkPassName = inPassName;
        linkPassResName = inPassResName;
    }
    public abstract void bind(FGSource fgSource);
    public abstract void postLinkValidate();

    public boolean isLinkValidate() {
        return bLinkValidate;
    }
    public FGBindable getBind(){return null;}
}
