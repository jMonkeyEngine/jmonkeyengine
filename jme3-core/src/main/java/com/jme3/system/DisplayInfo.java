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

import java.util.Objects;

/**
 * This class holds information about the display that was returned by glfwGetMonitors() calls in
 * the context class
 *
 * @author Kevin Bales
 * @author wil
 */
public final class DisplayInfo {

    /** displayID - display id that was return from Lwjgl3. */
    private long display;
    
    /** width - width that was return from Lwjgl3.  */
    private int width;

    /**  height - height that was return from Lwjgl3. */
    private int height;

    /** rate - refresh rate that was return from Lwjgl3. */
    private int rate;

    /** primary - indicates if the display is the primary monitor. */
    private boolean primary;

    /**
     * name - display name that was return from Lwjgl3.
     */
    private String name;

    /**
     * Create a new display mode object with the default values
     */
    DisplayInfo() {
        this(0L /*NULL*/, 1080, 1920, 60, false, "Generic Monitor");
    }
    
    /**
     * Create a new display mode object with the supplied parameters.
     * @param display the monitor pointer (native), the virtual memory 
     *                  address used by lwjgl.
     * @param width the width of the display, provided by lwjgl.
     * @param height the height of the display, provided by lwjgl.
     * @param rate the refresh rate of the display, in hertz.
     * @param primary a logical value that determines whether this display is 
     *                  primary or not; {@code true | false}
     * @param name display name
     */
    DisplayInfo(long display, int width, int height, int rate, boolean primary, String name) {
        this.display  = display;
        this.width      = width;
        this.height     = height;
        this.rate       = rate;
        this.primary    = primary;
        this.name       = name;
    }

    // === ----------------------------------------------------------------- ===
    // ===                            SETTERS                                ===
    // === ----------------------------------------------------------------- ===
    
    /**
     * Sets the monitor pointer (native), the virtual memory address used by lwjgl.
     * 
     * @param display the monitor pointer (native), the virtual memory 
     *                  address used by lwjgl
     */
    void setDisplay(long display) {
        this.display = display;
    }

    /**
     * Sets the width of the display.
     * @param width the width of the display
     */
    void setWidth(int width) {
        this.width = width;
    }

    /**
     * Sets the height of the display.
     * @param height the height of the display
     */
    void setHeight(int height) {
        this.height = height;
    }

    /**
     * Sets the refresh rate of the display, in hertz.
     * @param rate the refresh rate of the display, in hertz
     */
    void setRate(int rate) {
        this.rate = rate;
    }

    /**
     * Set this display as primary or not.
     * @param primary {@code true} if the display is primary, {@code false} otherwise.
     */
    void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Set the screen (display) name
     * @param name display name
     */
    void setName(String name) {
        this.name = name;
    }

    // === ----------------------------------------------------------------- ===
    // ===                              GETTERS                              ===
    // === ----------------------------------------------------------------- ===
    
    /**
     * Returns the monitor pointer (native), the virtual memory address used by lwjgl.
     * @return the monitor pointer (native), the virtual memory address used by lwjgl
     */
    public long getDisplay() {
        return display;
    }

    /**
     * Returns the width of the display.
     * @return the width of the display.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the display.
     * @return the height of the display
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the refresh rate of the display, in hertz.
     * @return the refresh rate of the display, in hertz
     */
    public int getRate() {
        return rate;
    }

    /**
     * Determines if this display belongs to the main monitor.
     * @return {@code true} if the display is primary, {@code false} otherwise.
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Returns the display name.
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (int) (this.display ^ (this.display >>> 32));
        hash = 97 * hash + this.width;
        hash = 97 * hash + this.height;
        hash = 97 * hash + this.rate;
        hash = 97 * hash + (this.primary ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisplayInfo other = (DisplayInfo) obj;
        if (this.display != other.display) {
            return false;
        }
        if (this.width != other.width) {
            return false;
        }
        if (this.height != other.height) {
            return false;
        }
        if (this.rate != other.rate) {
            return false;
        }
        if (this.primary != other.primary) {
            return false;
        }
        return Objects.equals(this.name, other.name);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return getDisplay() == 0L ? "NULL" : ("(" + getName() + "|" 
                + getDisplay() + ")" + getWidth() + "x" + getHeight() + "@" 
                + (getRate() > 0 ? getRate()  + "Hz" : "[Unknown refresh rate]"));
    }
}
