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

package com.jme3.input.lwjgl;

import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.system.lwjgl.LwjglAbstractDisplay;
import com.jme3.system.lwjgl.LwjglTimer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

public class LwjglMouseInput implements MouseInput {

    private static final Logger logger = Logger.getLogger(LwjglMouseInput.class.getName());

    private LwjglAbstractDisplay context;

    private RawInputListener listener;

    private boolean supportHardwareCursor = false;
    private boolean cursorVisible = true;

    /**
     * We need to cache the cursors
     * (https://github.com/jMonkeyEngine/jmonkeyengine/issues/537)
     */
    private Map<JmeCursor, Cursor> cursorMap = new HashMap<JmeCursor, Cursor>();

    private int curX, curY, curWheel;

    public LwjglMouseInput(LwjglAbstractDisplay context){
        this.context = context;
    }

    public void initialize() {
        if (!context.isRenderable())
            return;

        try {
            Mouse.create();
            logger.fine("Mouse created.");
            supportHardwareCursor = (Cursor.getCapabilities() & Cursor.CURSOR_ONE_BIT_TRANSPARENCY) != 0;

            // Recall state that was set before initialization
            Mouse.setGrabbed(!cursorVisible);
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Error while creating mouse", ex);
        }
    }

    public boolean isInitialized(){
        return Mouse.isCreated();
    }

    public int getButtonCount(){
        return Mouse.getButtonCount();
    }

    public void update() {
        if (!context.isRenderable())
            return;

        while (Mouse.next()){
            int btn = Mouse.getEventButton();

            int wheelDelta = Mouse.getEventDWheel();
            int xDelta = Mouse.getEventDX();
            int yDelta = Mouse.getEventDY();
            int x = Mouse.getX();
            int y = Mouse.getY();

            curWheel += wheelDelta;
            if (cursorVisible){
                xDelta = x - curX;
                yDelta = y - curY;
                curX = x;
                curY = y;
            }else{
                x = curX + xDelta;
                y = curY + yDelta;
                curX = x;
                curY = y;
            }

            if (xDelta != 0 || yDelta != 0 || wheelDelta != 0){
                MouseMotionEvent evt = new MouseMotionEvent(x, y, xDelta, yDelta, curWheel, wheelDelta);
                evt.setTime(Mouse.getEventNanoseconds());
                listener.onMouseMotionEvent(evt);
            }
            if (btn != -1){
                MouseButtonEvent evt = new MouseButtonEvent(btn,
                        Mouse.getEventButtonState(), x, y);
                evt.setTime(Mouse.getEventNanoseconds());
                listener.onMouseButtonEvent(evt);
            }
        }
    }

    public void destroy() {
        if (!context.isRenderable())
            return;

        Mouse.destroy();

        // Destroy the cursor cache
        for (Cursor cursor : cursorMap.values()) {
            cursor.destroy();
        }
        cursorMap.clear();

        logger.fine("Mouse destroyed.");
    }

    public void setCursorVisible(boolean visible){
        cursorVisible = visible;
        if (!context.isRenderable())
            return;

        Mouse.setGrabbed(!visible);
    }

    public void setInputListener(RawInputListener listener) {
        this.listener = listener;
    }

    public long getInputTimeNanos() {
        return Sys.getTime() * LwjglTimer.LWJGL_TIME_TO_NANOS;
    }

    public void setNativeCursor(JmeCursor jmeCursor) {
        try {
            Cursor newCursor = null;
            if (jmeCursor != null) {
                newCursor = cursorMap.get(jmeCursor);
                if (newCursor == null) {
                    newCursor = new Cursor(
                            jmeCursor.getWidth(),
                            jmeCursor.getHeight(),
                            jmeCursor.getXHotSpot(),
                            jmeCursor.getYHotSpot(),
                            jmeCursor.getNumImages(),
                            jmeCursor.getImagesData(),
                            jmeCursor.getImagesDelay());

                    // Add to cache
                    cursorMap.put(jmeCursor, newCursor);
                }
            }
            Mouse.setNativeCursor(newCursor);
        } catch (LWJGLException ex) {
            Logger.getLogger(LwjglMouseInput.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
