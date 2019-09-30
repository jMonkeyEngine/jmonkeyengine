package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.ColorRGBA;

import java.io.IOException;

/**
 * Grad Point
 * This represents a gradient point entry for use in the gradient value type
 *
 * @author Jedic
 */
public class GradPoint implements Savable, Cloneable {
  public ColorRGBA color;
  public float x;

  public GradPoint(ColorRGBA color, float x) {
    this.color = color;
    this.x = x;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(x, "x", 0.0f);
    oc.write(color, "color", ColorRGBA.White);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    x = ic.readFloat("x", 0.0f);
    color = (ColorRGBA) ic.readSavable("color", new ColorRGBA());
  }

  @Override
  public GradPoint clone() {
    try {
      GradPoint p = (GradPoint)super.clone();
      p.x = x;
      p.color = color.clone();
      return p;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }


  public boolean equals(Object o) {
    if (!(o instanceof GradPoint)) return false;

    GradPoint check = (GradPoint)o;

    if (x != check.x) return false;

    if (color != null && !color.equals(check.color)
        || color == null && check.color != null) {
      return false;
    }

    return true;
  }
}