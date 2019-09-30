package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

import java.io.IOException;

public class ColorValueType implements Savable, Cloneable {

  public enum Type {
    CONSTANT,
    RANDOM_COLOR,
    GRADIENT,
    RANDOM_BETWEEN_COLORS,
    RANDOM_BETWEEN_GRADIENTS
  }

  private Type type = Type.CONSTANT;
  private ColorRGBA color;
  private ColorRGBA colorTwo;
  private Gradient gradient;
  private Gradient gradientTwo;

  // temp variables
  private transient ColorRGBA temp = new ColorRGBA();

  public ColorValueType() {
    type = Type.RANDOM_COLOR;
  }

  public ColorValueType(ColorRGBA color) {
    this.color = color;
    this.type = Type.CONSTANT;
  }

  public ColorValueType(Gradient gradient) {
    this.gradient = gradient;
    type = Type.GRADIENT;
  }

  public ColorRGBA getColor() {
    return color;
  }

  public void setColor(ColorRGBA color) {
    this.color = color;
    this.colorTwo = null;
    this.gradient = null;
    this.gradientTwo = null;
    type = Type.CONSTANT;
  }

  public void setColorRange(ColorRGBA color, ColorRGBA colorTwo) {
    this.color = color;
    this.colorTwo = colorTwo;
    this.gradient = null;
    this.gradientTwo = null;
    type = Type.RANDOM_BETWEEN_COLORS;
  }

  public void setGradient(Gradient gradient) {
    this.color = null;
    this.colorTwo = null;
    this.gradient = gradient;
    this.gradientTwo = null;
    type = Type.GRADIENT;
  }

  public void setGradients(Gradient gradient, Gradient gradientTwo) {
    this.color = null;
    this.colorTwo = null;
    this.gradient = gradient;
    this.gradientTwo = gradientTwo;
    type = Type.RANDOM_BETWEEN_GRADIENTS;
  }

  public void set(ColorValueType v) {
    if (v.color != null) this.color = v.color.clone();
    if (v.colorTwo != null) this.colorTwo = v.colorTwo.clone();
    if (v.gradient != null) this.gradient = v.gradient.clone();
    if (v.gradientTwo != null) this.gradientTwo = v.gradientTwo.clone();
    type = v.type;
  }

  public ColorRGBA getColorTwo() {
    return colorTwo;
  }

  public Gradient getGradient() {
    return gradient;
  }

  public Gradient getGradientTwo() {
    return gradientTwo;
  }

  public Type getType() {
    return type;
  }

  public ColorRGBA getValueColor(float time, float particleRandom, ColorRGBA store) {
    ColorRGBA output = store == null ? new ColorRGBA() : store;

    switch (type) {
      case CONSTANT: output.set(color); break;
      case RANDOM_COLOR: output.set(ColorRGBA.randomColor()); break;
      case GRADIENT: gradient.getValueColor(time, output); break;
      case RANDOM_BETWEEN_GRADIENTS:
        gradient.getValueColor(time, output);
        gradientTwo.getValueColor(time, store);
        output.interpolateLocal(store, particleRandom);
        break;
      case RANDOM_BETWEEN_COLORS:
        output.r = FastMath.interpolateLinear(FastMath.nextRandomFloat(), color.r, colorTwo.r);
        output.g = FastMath.interpolateLinear(FastMath.nextRandomFloat(), color.g, colorTwo.g);
        output.b = FastMath.interpolateLinear(FastMath.nextRandomFloat(), color.b, colorTwo.b);
        output.a = FastMath.interpolateLinear(FastMath.nextRandomFloat(), color.a, colorTwo.a);
      break;
      default: break;
    }

    return output;
  }


  @Override
  public ColorValueType clone() {
    try {
      ColorValueType clone = (ColorValueType)super.clone();
      if (color != null ) clone.color = color.clone();
      if (colorTwo != null ) clone.colorTwo = colorTwo.clone();
      if (gradient != null ) clone.gradient = gradient.clone();
      if (gradientTwo != null ) clone.gradientTwo = gradientTwo.clone();
      clone.type = type;
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(color, "color", null);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    color = (ColorRGBA)ic.readSavable("color", null);
  }

  public boolean equals(Object o) {
    if (!(o instanceof ColorValueType)) return false;

    ColorValueType check = (ColorValueType)o;

    if (type != check.type) return false;
    if (color != null && !color.equals(check.color) || color == null && check.color != null) return false;
    if (colorTwo != null && !colorTwo.equals(check.colorTwo) || colorTwo == null && check.colorTwo != null) return false;
    if (gradient != null && !gradient.equals(check.gradient) || gradient == null && check.gradient != null) return false;
    if (gradientTwo != null && !gradientTwo.equals(check.gradientTwo) || gradientTwo == null && check.gradientTwo != null) return false;

    return true;
  }
}
