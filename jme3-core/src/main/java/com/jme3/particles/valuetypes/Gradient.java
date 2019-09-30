package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.ColorRGBA;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Gradient
 * This represents a gradient which can be used with the particle system
 *
 * @author Jedic
 */
public class Gradient implements Savable, Cloneable {

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    GradPoint[] pointArray = points.toArray(new GradPoint[points.size()]);
    oc.write(pointArray, "points", new GradPoint[]{});
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    Savable[] pointArray = (Savable[]) ic.readSavableArray("points", new GradPoint[]{});
    for (int i = 0; i < pointArray.length; i++) {
      points.add((GradPoint) pointArray[i]);
    }
  }

  private ArrayList<GradPoint> points = new ArrayList<>();

  public Gradient() {

  }

  public Gradient addGradPoint(ColorRGBA color, float x) {
    points.add(new GradPoint(color, x));
    sort();
    return this;
  }

  public int getSize() {
    return points.size();
  }

  public GradPoint getPoint(int index) {
    return points.get(index);
  }

  public void sort() {
    points.sort((c1, c2)->{
      if (c1.x < c2.x) return -1;
      else if (c1.x > c2.x) return 1;
      else return 0;
    });

  }

  public ColorRGBA getValueColor(float percent, ColorRGBA store) {
    // find which points we are in between
    GradPoint lastPoint = null;
    GradPoint currentPoint = null;
    for (int i = 0; i < points.size(); i++) {
      lastPoint = currentPoint;
      currentPoint = points.get(i);

      if (currentPoint.x >= percent) {
        // now get the interpolated value
        if (lastPoint == null && currentPoint != null) {
          // just use the current points y value
          if (store == null) {
            return new ColorRGBA(currentPoint.color);
          } else {
            store.set(currentPoint.color);
            return store;
          }
        } else if (lastPoint != null && currentPoint != null) {
          // Calculate the percent distance we are in between the two points
          float perc = (percent - lastPoint.x) / (currentPoint.x - lastPoint.x);

          if (store == null) {
            return new ColorRGBA().interpolateLocal(lastPoint.color, currentPoint.color, perc);
          } else {
            store.interpolateLocal(lastPoint.color, currentPoint.color, perc);
            return store;
          }
        }
      }
    }

    // we must be past the last point?
    if (currentPoint != null && currentPoint.x < percent) {
      if (store == null) {
        return new ColorRGBA(currentPoint.color);
      } else {
        store.set(currentPoint.color);
        return store;
      }
    }


    if (store == null) {
      return new ColorRGBA(ColorRGBA.White);
    } else {
      store.set(ColorRGBA.White);
      return store;
    }
  }

  @Override
  public Gradient clone() {
    try {
      Gradient gradient = (Gradient) super.clone();
      gradient.points = new ArrayList<>();
      points.forEach((p)-> gradient.points.add(p.clone()));
      return gradient;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }


  public boolean equals(Object o) {
    if (!(o instanceof Gradient)) return false;

    Gradient check = (Gradient)o;

    if (points.size() != check.points.size()) return false;

    for (int i=0; i < points.size(); i++) {
      if (!points.get(i).equals(check.points.get(i)))
        return false;
    }

    return true;
  }
}
