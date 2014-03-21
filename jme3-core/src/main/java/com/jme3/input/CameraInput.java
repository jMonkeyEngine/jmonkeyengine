/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.input;

/**
 * This class defines all the constants used in camera handlers for registration
 * with the inputManager
 *
 * @author Nehon
 */
public class CameraInput {

    //ChaseCamera constants
    /**
     * Chase camera mapping for moving down. Default assigned to
     * MouseInput.AXIS_Y direction depending on the invertYaxis configuration
     */
    public final static String CHASECAM_DOWN = "ChaseCamDown";
    /**
     * Chase camera mapping for moving up. Default assigned to MouseInput.AXIS_Y
     * direction depending on the invertYaxis configuration
     */
    public final static String CHASECAM_UP = "ChaseCamUp";
    /**
     * Chase camera mapping for zooming in. Default assigned to
     * MouseInput.AXIS_WHEEL direction positive
     */
    public final static String CHASECAM_ZOOMIN = "ChaseCamZoomIn";
    /**
     * Chase camera mapping for zooming out. Default assigned to
     * MouseInput.AXIS_WHEEL direction negative
     */
    public final static String CHASECAM_ZOOMOUT = "ChaseCamZoomOut";
    /**
     * Chase camera mapping for moving left. Default assigned to
     * MouseInput.AXIS_X direction depending on the invertXaxis configuration
     */
    public final static String CHASECAM_MOVELEFT = "ChaseCamMoveLeft";
    /**
     * Chase camera mapping for moving right. Default assigned to
     * MouseInput.AXIS_X direction depending on the invertXaxis configuration
     */
    public final static String CHASECAM_MOVERIGHT = "ChaseCamMoveRight";
    /**
     * Chase camera mapping to initiate the rotation of the cam. Default assigned
     * to MouseInput.BUTTON_LEFT and MouseInput.BUTTON_RIGHT
     */
    public final static String CHASECAM_TOGGLEROTATE = "ChaseCamToggleRotate";
    
        
    
    //fly cameara constants
    /**
     * Fly camera mapping to look left. Default assigned to MouseInput.AXIS_X,
     * direction negative
     */
    public final static String FLYCAM_LEFT = "FLYCAM_Left";
    /**
     * Fly camera mapping to look right. Default assigned to MouseInput.AXIS_X,
     * direction positive
     */
    public final static String FLYCAM_RIGHT = "FLYCAM_Right";
    /**
     * Fly camera mapping to look up. Default assigned to MouseInput.AXIS_Y,
     * direction positive
     */
    public final static String FLYCAM_UP = "FLYCAM_Up";
    /**
     * Fly camera mapping to look down. Default assigned to MouseInput.AXIS_Y,
     * direction negative
     */
    public final static String FLYCAM_DOWN = "FLYCAM_Down";
    /**
     * Fly camera mapping to move left. Default assigned to KeyInput.KEY_A   
     */
    public final static String FLYCAM_STRAFELEFT = "FLYCAM_StrafeLeft";
    /**
     * Fly camera mapping to move right. Default assigned to KeyInput.KEY_D  
     */
    public final static String FLYCAM_STRAFERIGHT = "FLYCAM_StrafeRight";
    /**
     * Fly camera mapping to move forward. Default assigned to KeyInput.KEY_W   
     */
    public final static String FLYCAM_FORWARD = "FLYCAM_Forward";
    /**
     * Fly camera mapping to move backward. Default assigned to KeyInput.KEY_S   
     */
    public final static String FLYCAM_BACKWARD = "FLYCAM_Backward";
    /**
     * Fly camera mapping to zoom in. Default assigned to MouseInput.AXIS_WHEEL,
     * direction positive
     */
    public final static String FLYCAM_ZOOMIN = "FLYCAM_ZoomIn";
    /**
     * Fly camera mapping to zoom in. Default assigned to MouseInput.AXIS_WHEEL,
     * direction negative
     */
    public final static String FLYCAM_ZOOMOUT = "FLYCAM_ZoomOut";
    /**
     * Fly camera mapping to toggle rotation. Default assigned to 
     * MouseInput.BUTTON_LEFT   
     */
    public final static String FLYCAM_ROTATEDRAG = "FLYCAM_RotateDrag";
    /**
     * Fly camera mapping to move up. Default assigned to KeyInput.KEY_Q   
     */
    public final static String FLYCAM_RISE = "FLYCAM_Rise";
    /**
     * Fly camera mapping to move down. Default assigned to KeyInput.KEY_W   
     */
    public final static String FLYCAM_LOWER = "FLYCAM_Lower";
    
    public final static String FLYCAM_INVERTY = "FLYCAM_InvertY";
}
