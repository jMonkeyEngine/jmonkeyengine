/*
 * Copyright (c) 2009-2023 jMonkeyEngine All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system;

import java.util.ArrayList;

/**
 * This class holds all information about all displays that where return from the glfwGetMonitors()
 * call. It stores them into an ArrayList
 *
 * @author Kevin Bales
 */
public class Displays {

    private ArrayList<DisplayInfo> displays = new ArrayList<DisplayInfo>();

    public int addNewMonitor(long displaysID) {
        DisplayInfo info = new DisplayInfo();
        info.displayID = displaysID;
        displays.add(info);
        return displays.size() - 1;
    }

    /**
     * This function returns the size of the display ArrayList
     *
     * @return the
     */
    public int size() {
        return displays.size();
    }

    /**
     * Call to get display information on a certain display.
     *
     * @param pos the position in the ArrayList of the display information that you want to get.
     * @return returns the DisplayInfo data for the display called for.
     */
    public DisplayInfo get(int pos) {
        if (pos < displays.size()) return displays.get(pos);

        return null;
    }

    /**
     * Set information about this display stored in displayPos display in the array list.
     *
     * @param displayPos ArrayList position of display to update
     * @param name name of the display
     * @param width the current width the display is displaying
     * @param height the current height the display is displaying
     * @param rate the current refresh rate the display is set to
     */
    public void setInfo(int displayPos, String name, int width, int height, int rate) {
        if (displayPos < displays.size()) {
            DisplayInfo info = displays.get(displayPos);
            if (info != null) {
                info.width = width;
                info.height = height;
                info.rate = rate;
                info.name = name;
            }
        }
    }

    /**
     * This function will mark a certain display as the primary display.
     *
     * @param displayPos the position in the ArrayList of which display is the primary display
     */
    public void setPrimaryDisplay(int displayPos) {
        if (displayPos < displays.size()) {
            DisplayInfo info = displays.get(displayPos);
            if (info != null) info.primary = true;
        }
    }
}
