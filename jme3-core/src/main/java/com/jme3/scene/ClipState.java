/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene;

/**
 * <code>ClipState</code> holds the clipping parameters for a {@link Geometry}.
 * These are applied to the renderer when required to provide the desired
 * scissor effect.
 *
 * @author Philip Spencer
 */
public class ClipState
{
    /** Flag to denote whether or not clipping is enabled. */
    private boolean isClippingEnabled;

    /** The x coordinate of the bottom left corner of the clipping area. */
    private int x;

    /** The y coordinate of the bottom left corner of the clipping area. */
    private int y;

    /** The width of the clipping area. */
    private int w;

    /** The height of the clipping area. */
    private int h;

    /**
     * Instantiates a new ClipState.
     */
    public ClipState()
    {
        // Set initial attributes.
        this.disable();
    }

    /**
     * Attempts to enable clipping with the given parameters. The parameters are in
     * terms of screen space with x = 0, y = 0 representing the pixel in the very
     * bottom left of the screen. If the parameters represent a valid clipping area,
     * the isClippingEnabled flag is set to true, false otherwise.
     *
     * @param x the x coordinate of the bottom left of the clip area
     * @param y the y coordinate of the bottom left of the clip area
     * @param w the width of the clip area
     * @param h the height of the clip area
     */
    public final void enable(final int x, final int y, final int w, final int h)
    {
        // Set values to those given.
        this.set(x, y, w, h);
    }

    /**
     * Disables clipping.
     */
    public final void disable()
    {
        // Set values to zero.
        this.set(0, 0, 0, 0);
    }

    /**
     * Sets the clipping parameters to the given values and sets
     * the isClippingEnabled flag dependent on whether or not the
     * values represent a valid clipping area.
     *
     * @param x the x coordinate of the bottom left of the clip area
     * @param y the y coordinate of the bottom left of the clip area
     * @param w the width of the clip area
     * @param h the height of the clip area
     */
    private void set(final int x, final int y, final int w, final int h)
    {
        // Assign values.
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;

        // Determine whether this is enabled or not.
        isClippingEnabled = this.isValid();
    }

    /**
     * Determines whether or not the values held by this ClipState represent
     * a valid clipping area.
     *
     * @return true, if the values form a valid clipping area
     */
    public final boolean isValid()
    {
        // If width or height are zero or less.
        if ((w <= 0) || (h <= 0))
        {
            // This isn't a valid clipping area.
            return false;
        }
        else
        {
            // This is valid.
            return true;
        }
    }

    /**
     * Checks to see if clipping is enabled.
     *
     * @return true, if clipping is enabled
     */
    public final boolean isClippingEnabled()
    {
        return isClippingEnabled;
    }

    /**
     * Gets the x coordinate of the bottom left corner of the clipping area.
     *
     * @return the x coordinate of the bottom left corner of the clipping area
     */
    public final int getX()
    {
        return x;
    }

    /**
     * Gets the y coordinate of the bottom left corner of the clipping area.
     *
     * @return the y coordinate of the bottom left corner of the clipping area
     */
    public final int getY()
    {
        return y;
    }

    /**
     * Gets the width of the clipping area.
     *
     * @return the width of the clipping area
     */
    public final int getW()
    {
        return w;
    }

    /**
     * Gets the height of the clipping area.
     *
     * @return the height of the clipping area
     */
    public final int getH()
    {
        return h;
    }
}
