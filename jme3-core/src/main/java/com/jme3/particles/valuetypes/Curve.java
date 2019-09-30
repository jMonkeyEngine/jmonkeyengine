package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.Vector2f;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Curve implements Savable, Cloneable {

  private LinkedList<ControlPoint> points = new LinkedList<>();

  public Curve() {
  }

  public Curve addControlPoint(Vector2f in, Vector2f point, Vector2f out) {
    points.add(new ControlPoint(in, point, out));
    sort();
    return this;
  }

  private void sort() {
    points.sort((c1, c2)->{
      if (c1.point.x < c2.point.x) return -1;
      else if (c1.point.x > c2.point.x) return 1;
      else return 0;
    });

  }

  public List<ControlPoint> getControlPoints() {
    return points;
  }

  public float getValue(float blendTime) {
    // find which points we are in between
    ControlPoint lastPoint = null;
    ControlPoint currentPoint = null;
    for (int i = 0; i < points.size(); i++) {
      lastPoint = currentPoint;
      currentPoint = points.get(i);

      if (currentPoint.point.x >= blendTime) {
        // now get the interpolated value
        if (lastPoint == null && currentPoint != null) {
          // just use the current points y value
          return currentPoint.point.y;
        } else if (lastPoint != null && currentPoint != null) {
          // Calculate the percent distance we are in between the two points
          float perc = (blendTime - lastPoint.point.x) / (currentPoint.point.x - lastPoint.point.x);

          // get the midpoints of the 3 line segments
          //float p1x = lastPoint.point.x - ((lastPoint.point.x - lastPoint.outControlPoint.x) * perc);
          float p1y = lastPoint.point.y - ((lastPoint.point.y - lastPoint.outControlPoint.y) * perc);

          //float p2x = lastPoint.outControlPoint.x - ((lastPoint.outControlPoint.x - currentPoint.inControlPoint.x) * perc);
          float p2y = lastPoint.outControlPoint.y - ((lastPoint.outControlPoint.y - currentPoint.inControlPoint.y) * perc);

          //float p3x = currentPoint.inControlPoint.x - ((currentPoint.inControlPoint.x - currentPoint.point.x) * perc);
          float p3y = currentPoint.inControlPoint.y - ((currentPoint.inControlPoint.y - currentPoint.point.y) * perc);

          // now get the midpoints of the two segments
          //float s1x = p1x - ((p1x - p2x) * perc);
          float s1y = p1y - ((p1y - p2y) * perc);

          //float s2x = p2x - ((p2x - p3x) * perc);
          float s2y = p2y - ((p2y - p3y) * perc);

          // now get our final value
          //float fx = s1x - ((s1x - s2x) * perc);
          float fy = s1y - ((s1y - s2y) * perc);

          return fy;
        }
      }
    }

    // we must be past the last point?
    if (currentPoint != null && currentPoint.point.x < blendTime) {
      return currentPoint.point.y;
    }


    return 0;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    ControlPoint[] pointArray = points.toArray(new ControlPoint[points.size()]);
    oc.write(pointArray, "points", new ControlPoint[]{});
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    Savable[] pointArray = (Savable[]) ic.readSavableArray("points", new ControlPoint[]{});
    for (int i = 0; i < pointArray.length; i++) {
      points.add((ControlPoint) pointArray[i]);
    }
  }

  @Override
  public Curve clone() {
    try {
      Curve clone = (Curve)super.clone();
      clone.points = new LinkedList<>();
      points.forEach((p)-> clone.points.add(p.clone()));
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public boolean equals(Object o) {
    if (!(o instanceof Curve)) return false;

    Curve check = (Curve)o;

    if (points.size() != check.points.size()) return false;

    for (int i=0; i < points.size(); i++) {
      if (!points.get(i).equals(check.points.get(i)))
        return false;
    }

    return true;
  }
}
