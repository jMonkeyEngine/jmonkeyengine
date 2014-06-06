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
package com.jme3.math;

import com.jme3.export.*;

import java.io.IOException;


/**
 *
 * <code>ClipRectangle</code> defines a finite plane within two dimensional
 * pixel space that is specified via four values (x, y, w, h.) x and y define
 * the coordinates of the pixel representing the lower left corner of the
 * rectangle. w and h define the width and height of the rectangle.
 *
 * @author Philip Spencer
 */

public final class ClipRectangle implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private int x, y, w, h;

    /**
     * Constructor creates a new <code>ClipRectangle</code> with default
     * values of (0, 0, 0, 0).
     */
    public ClipRectangle() {
        x = y = w = h = 0;
    }

    /**
     * Constructor creates a new <code>ClipRectangle</code> with defined
     * x, y, w and h points that define the area of the rectangle.
     *
     * @param x the x coordinate of the lower left corner of the rectangle
     * @param y the y coordinate of the lower left corner of the rectangle
     * @param w the width of the rectangle
     * @param h the height of the rectangle
     */
    public ClipRectangle(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * <code>set</code> sets all defining values of this
     * <code>ClipRectangle</code> in one go.
     *
     * @param x the x coordinate of the lower left corner of the rectangle
     * @param y the y coordinate of the lower left corner of the rectangle
     * @param w the width of the rectangle
     * @param h the height of the rectangle
     */
    public void set(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     * <code>getX</code> returns the x coordinate of the lower left
     * corner of the rectangle.
     *
     * @return the x coordinate of the lower left corner
     */
    public int getX() {
        return x;
    }

    /**
     * <code>setX</code> sets the x coordinate of the lower left
     * corner of the rectangle.
     *
     * @param x the x coordinate of the lower left corner
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * <code>getY</code> returns the y coordinate of the lower left
     * corner of the rectangle.
     *
     * @return the y coordinate of the lower left corner
     */
    public int getY() {
        return y;
    }

    /**
     * <code>setY</code> sets the y coordinate of the lower left
     * corner of the rectangle.
     *
     * @param y the y coordinate of the lower left corner
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * <code>getW</code> returns the width of the rectangle.
     *
     * @return the width
     */
    public int getW() {
        return w;
    }

    /**
     * <code>setW</code> sets width of the rectangle.
     *
     * @param w the width
     */
    public void setW(int w) {
        this.w = w;
    }

    /**
     * <code>getH</code> returns the height of the rectangle.
     *
     * @return the height
     */
    public int getH() {
        return h;
    }

    /**
     * <code>setH</code> sets height of the rectangle.
     *
     * @param h the height
     */
    public void setH(int h) {
        this.h = h;
    }

    /**
     * Intersects the given <code>ClipRectangle</code> with this
     * <code>ClipRectangle</code>. If there is no intersection
     * between them, the result is null. Otherwise it is a new
     * <code>ClipRectangle</code> representing the intersection.
     *
     * @param cr the <code>ClipRectangle</code> to intersect with this
     * @return null if there is no intersection otherwise the resulting
     * <code>ClipRectangle</code>
     */
    public ClipRectangle intersect(ClipRectangle cr)
    {
        ClipRectangle destination = new ClipRectangle();

        if (ClipRectangle.intersect(this, cr, destination)) {
            return destination;
        }
        else
        {
            return null;
        }
    }

    /**
     * Intersects two <code>ClipRectangles</code> and stores the result
     * in the destination <code>ClipRectangle</code>. If there is no
     * intersection between them the method returns false, otherwise it
     * return true.
     *
     * @param cr0 the first <code>ClipRectangle</code>
     * @param cr1 the second <code>ClipRectangle</code>
     * @param destination the <code>ClipRectangle</code> in which to store
     * the result
     * @return false if there is no intersection, true otherwise
     */
    public static boolean intersect(ClipRectangle cr0, ClipRectangle cr1,
                                    ClipRectangle destination) {
        return (ClipRectangle.intersect(cr0.getX(), cr0.getY(), cr0.getW(),
                                        cr0.getH(), cr1.getX(), cr1.getY(),
                                        cr1.getW(), cr1.getH(),
                                        destination));
    }

    /**
     * Intersects two <code>ClipRectangles</code> represented by the values
     * that define them and stores the result in the destination
     * <code>ClipRectangle</code>. If there is no intersection between them
     * the method returns false, otherwise it returns true.
     *
     * @param x0 the x coordinate of the lower left of the first rectangle
     * @param y0 the y coordinate of the lower left of the first rectangle
     * @param w0 the width of the first rectangle
     * @param h0 the height of the first rectangle
     * @param x1 the x coordinate of the lower left of the second rectangle
     * @param y1 the y coordinate of the lower left of the second rectangle
     * @param w1 the width of the second rectangle
     * @param h1 the height of the second rectangle
     * @param destination the <code>ClipRectangle</code> in which to store
     * the result
     * @return false if there is no intersection, true otherwise
     */
    public static boolean intersect(int x0, int y0, int w0, int h0,
                                    int x1, int y1, int w1, int h1,
                                    ClipRectangle destination)
    {
        int left = Math.max(x0, x1);
        int bottom = Math.max(y0, y1);
        int right = Math.min(x0 + w0, x1 + w1);
        int top = Math.min(y0 + h0, y1 + h1);
        int width = right - left;
        int height = top - bottom;

        destination.set(left, bottom, width, height);

        // If width or height are zero or less, there was no intersection.
        if ((width <= 0) || (height <= 0)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * <code>equals</code> determines if two ClipRectangles are logically equal,
     * that is, if the values of (x, y, w, h) are the same for both.
     *
     * @param o the object to compare for equality
     * @return true if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClipRectangle)) {
            return false;
        }

        if (this == o) {
            return true;
        }

        ClipRectangle comp = (ClipRectangle) o;
        if (Integer.compare(x, comp.x) != 0) {
            return false;
        }
        if (Integer.compare(y, comp.y) != 0) {
            return false;
        }
        if (Integer.compare(w, comp.w) != 0) {
            return false;
        }
        if (Integer.compare(h, comp.h) != 0) {
            return false;
        }
        return true;
    }

    public void write(JmeExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(x, "x", 0);
        capsule.write(y, "y", 0);
        capsule.write(w, "w", 0);
        capsule.write(h, "h", 0);
    }

    public void read(JmeImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        x = capsule.readInt("x", 0);
        y = capsule.readInt("y", 0);
        w = capsule.readInt("w", 0);
        h = capsule.readInt("h", 0);
    }

    @Override
    public ClipRectangle clone() {
        try {
            ClipRectangle cr = (ClipRectangle) super.clone();
            cr.x = x;
            cr.y = y;
            cr.w = w;
            cr.h = h;
            return cr;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
