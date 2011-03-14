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

package com.jme3.scene.control;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.FastMath;

/**
 * <code>AreaUtils</code> is used to calculate the area of various objects, such as bounding volumes.  These
 * functions are very loose approximations.
 * @author Joshua Slack
 * @version $Id: AreaUtils.java 4131 2009-03-19 20:15:28Z blaine.dev $
 */

public class AreaUtils {

  /**
   * calcScreenArea -- in Pixels
   * Aproximates the screen area of a bounding volume.  If the volume isn't a
   * BoundingSphere, BoundingBox, or OrientedBoundingBox 0 is returned.
   *
   * @param bound The bounds to calculate the volume from.
   * @param distance The distance from camera to object.
   * @param screenWidth The width of the screen.
   * @return The area in pixels on the screen of the bounding volume.
   */
  public static float calcScreenArea(BoundingVolume bound, float distance, float screenWidth) {
      if (bound.getType() == BoundingVolume.Type.Sphere){
          return calcScreenArea((BoundingSphere) bound, distance, screenWidth);
      }else if (bound.getType() == BoundingVolume.Type.AABB){
          return calcScreenArea((BoundingBox) bound, distance, screenWidth);
      }
      return 0.0f;
  }

  private static float calcScreenArea(BoundingSphere bound, float distance, float screenWidth) {
    // Where is the center point and a radius point that lies in a plan parallel to the view plane?
//    // Calc radius based on these two points and plug into circle area formula.
//    Vector2f centerSP = null;
//    Vector2f outerSP = null;
//    float radiusSq = centerSP.subtract(outerSP).lengthSquared();
      float radius = (bound.getRadius() * screenWidth) / (distance * 2);
      return radius * radius * FastMath.PI;
  }

  private static float calcScreenArea(BoundingBox bound, float distance, float screenWidth) {
      // Calc as if we are a BoundingSphere for now...
      float radiusSquare = bound.getXExtent() * bound.getXExtent()
                         + bound.getYExtent() * bound.getYExtent()
                         + bound.getZExtent() * bound.getZExtent();
      return ((radiusSquare * screenWidth * screenWidth) / (distance * distance * 4)) * FastMath.PI;
  }
}