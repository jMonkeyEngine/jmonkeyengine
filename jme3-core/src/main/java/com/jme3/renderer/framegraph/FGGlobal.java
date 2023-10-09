/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 *
 * @author JohnKkk
 */
public class FGGlobal {
    public final static String S_GLOABLE_PASS_SOURCE_NAME = "$";
    public final static String S_SCENE_COLOR_FB = "sceneColorFramebuffer";
    public final static String S_SCENE_COLOR_RT = "sceneColorRT";
    public final static String S_SCENE_DEPTH_RT = "sceneDepthRT";
    public final static String S_DEFAULT_FB = "defaultFramebuffer";
    private final static ArrayList<FGSink> g_Sinks = new ArrayList<>();
    private final static ArrayList<FGSource> g_Sources = new ArrayList<>();
    public final static boolean linkSink(FGSink outSink){
        boolean bound = false;
        for(FGSource soucre : g_Sources){
            if(soucre.getName().equals(outSink.getLinkPassResName())){
                outSink.bind(soucre);
                bound = true;
                break;
            }
        }
        return bound;
    }
}
