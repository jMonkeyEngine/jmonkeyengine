/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.terrain.geomipmap.picking;

import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**
 * Works on the XZ plane, with positive Y as up.
 *
 * @author Joshua Slack
 * @author Brent Owens
 */
public class BresenhamYUpGridTracer {

    protected Vector3f gridOrigin = new Vector3f();
    protected Vector3f gridSpacing = new Vector3f();
    protected Vector2f gridLocation = new Vector2f();
    protected Vector3f rayLocation = new Vector3f();
    protected Ray walkRay = new Ray();

    protected Direction stepDirection = Direction.None;
    protected float rayLength;

    public static enum Direction {
        None, PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ;
    };

    // a "near zero" value we will use to determine if the walkRay is
    // perpendicular to the grid.
    protected static float TOLERANCE = 0.0000001f;

    private int stepXDirection;
    private int stepZDirection;

    // from current position along ray
    private float distToNextXIntersection, distToNextZIntersection;
    private float distBetweenXIntersections, distBetweenZIntersections;

    public void startWalk(final Ray walkRay) {
        // store ray
        this.walkRay.set(walkRay);

        // simplify access to direction
        Vector3f direction = this.walkRay.getDirection();

        // Move start point to grid space
        Vector3f start = this.walkRay.getOrigin().subtract(gridOrigin);

        gridLocation.x = (int) (start.x / gridSpacing.x);
        gridLocation.y = (int) (start.z / gridSpacing.z);

        Vector3f ooDirection = new Vector3f(1.0f / direction.x, 1,1.0f / direction.z);

        // Check which direction on the X world axis we are moving.
        if (direction.x > TOLERANCE) {
            distToNextXIntersection = ((gridLocation.x + 1) * gridSpacing.x - start.x) * ooDirection.x;
            distBetweenXIntersections = gridSpacing.x * ooDirection.x;
            stepXDirection = 1;
        } else if (direction.x < -TOLERANCE) {
            distToNextXIntersection = (start.x - (gridLocation.x * gridSpacing.x)) * -direction.x;
            distBetweenXIntersections = -gridSpacing.x * ooDirection.x;
            stepXDirection = -1;
        } else {
            distToNextXIntersection = Float.MAX_VALUE;
            distBetweenXIntersections = Float.MAX_VALUE;
            stepXDirection = 0;
        }

        // Check which direction on the Z world axis we are moving.
        if (direction.z > TOLERANCE) {
            distToNextZIntersection = ((gridLocation.y + 1) * gridSpacing.z - start.z) * ooDirection.z;
            distBetweenZIntersections = gridSpacing.z * ooDirection.z;
            stepZDirection = 1;
        } else if (direction.z < -TOLERANCE) {
            distToNextZIntersection = (start.z - (gridLocation.y * gridSpacing.z)) * -direction.z;
            distBetweenZIntersections = -gridSpacing.z * ooDirection.z;
            stepZDirection = -1;
        } else {
            distToNextZIntersection = Float.MAX_VALUE;
            distBetweenZIntersections = Float.MAX_VALUE;
            stepZDirection = 0;
        }

        // Reset some variables
        rayLocation.set(start);
        rayLength = 0.0f;
        stepDirection = Direction.None;
    }

    public void next() {
        // Walk us to our next location based on distances to next X or Z grid
        // line.
        if (distToNextXIntersection < distToNextZIntersection) {
            rayLength = distToNextXIntersection;
            gridLocation.x += stepXDirection;
            distToNextXIntersection += distBetweenXIntersections;
            switch (stepXDirection) {
            case -1:
                stepDirection = Direction.NegativeX;
                break;
            case 0:
                stepDirection = Direction.None;
                break;
            case 1:
                stepDirection = Direction.PositiveX;
                break;
            }
        } else {
            rayLength = distToNextZIntersection;
            gridLocation.y += stepZDirection;
            distToNextZIntersection += distBetweenZIntersections;
            switch (stepZDirection) {
            case -1:
                stepDirection = Direction.NegativeZ;
                break;
            case 0:
                stepDirection = Direction.None;
                break;
            case 1:
                stepDirection = Direction.PositiveZ;
                break;
            }
        }

        rayLocation.set(walkRay.direction).multLocal(rayLength).addLocal(walkRay.origin);
    }

    public Direction getLastStepDirection() {
        return stepDirection;
    }

    public boolean isRayPerpendicularToGrid() {
        return stepXDirection == 0 && stepZDirection == 0;
    }


    public Vector2f getGridLocation() {
        return gridLocation;
    }

    public Vector3f getGridOrigin() {
        return gridOrigin;
    }

    public Vector3f getGridSpacing() {
        return gridSpacing;
    }


    public void setGridLocation(Vector2f gridLocation) {
        this.gridLocation = gridLocation;
    }

    public void setGridOrigin(Vector3f gridOrigin) {
        this.gridOrigin = gridOrigin;
    }

    public void setGridSpacing(Vector3f gridSpacing) {
        this.gridSpacing = gridSpacing;
    }
}
