/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        for(FGSource src : g_Sources){
            if(src.getName().equals(outSink.getLinkPassResName())){
                outSink.bind(src);
                bound = true;
                break;
            }
        }
        return bound;
    }
    
}
