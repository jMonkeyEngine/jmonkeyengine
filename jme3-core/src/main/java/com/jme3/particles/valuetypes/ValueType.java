package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.FastMath;

import java.io.IOException;

/**
 * Value Type
 *
 * 4 Modes... constant, random, curve, random between two curves
 */
public class ValueType implements Savable, Cloneable {

  public enum Type {
    CONSTANT,
    RANDOM,
    CURVE,
    RANDOM_BETWEEN_CURVES
  }

  private Type type = Type.CONSTANT;
  private float value;
  private float max;
  private Curve curveOne;
  private Curve curveTwo;

  public ValueType() {

  }

  /**
   * Constructor for a constant value type
   * @param v - the constant to return for this field
   */
  public ValueType(float v) {
    value = v;
    type = Type.CONSTANT;
  }

  /**
   * Constructor for a random number between two values
   * @param min
   * @param max
   */
  public ValueType(float min, float max) {
    value = min;
    this.max = max;
    type = Type.RANDOM;
  }

  /**
   * Constructor for a curve value
   * @param curve
   */
  public ValueType(Curve curve) {
    curveOne = curve;
    type = Type.CURVE;
  }

  /**
   * Constructor for random between two curves value
   * @param curve
   */
  public ValueType(Curve curve, Curve curveTwo) {
    curveOne = curve;
    curveTwo = curveTwo;
    type = Type.RANDOM_BETWEEN_CURVES;
  }

  /**
   * Get Value
   * This is used during particle calculatons to get output values that can vary by time
   *
   * @param blendTime - How far along the particles lifecycle is 0-1
   * @param particleRandomValue - Generated every time a particle is born for calculations like between two curves
   * @return
   */
  public float getValue(float blendTime, float particleRandomValue) {
    if (type == Type.CONSTANT) return value;
    if (type == Type.RANDOM) return FastMath.nextRandomFloat() * (max - value) + value;
    if (type == Type.CURVE) return curveOne.getValue(blendTime);
    if (type == Type.RANDOM_BETWEEN_CURVES) {
      float f1 = curveOne.getValue(blendTime);
      float f2 = curveTwo.getValue(blendTime);
      if (f1 > f2) {
        float t = f2;
        f2 = f1;
        f1 = t;
      }

      return particleRandomValue * (f2 - f1) + f1;
    }
    return value;
  }

  public float getValue() {
    return value;
  }

  public float getMax() {
    return max;
  }

  public Curve getCurve() {
    return curveOne;
  }

  public Curve getCurveMax() {
    return curveTwo;
  }

  public void setValue(float value) {
    this.value = value;
    this.type = Type.CONSTANT;
    curveOne = null;
    curveTwo = null;
  }

  public void setMinMaxValue(float value, float max) {
    this.value = value;
    this.max = max;
    this.type = Type.RANDOM;
    curveOne = null;
    curveTwo = null;
  }

  public void setCurve(Curve curve) {
    this.curveOne = curve;
    this.type = Type.CURVE;
    curveTwo = null;
  }

  public void setBetweenCurves(Curve curve, Curve curveTwo) {
    this.curveOne = curve;
    this.curveTwo = curveTwo;
    this.type = Type.RANDOM_BETWEEN_CURVES;
  }

  public Type getType() {
    return type;
  }

  public void set(ValueType value) {
    this.type = value.type;
    this.value = value.value;
    this.max = value.max;
    this.curveOne = value.curveOne != null ? value.curveOne.clone() : null;
    this.curveTwo = value.curveTwo != null ? value.curveTwo.clone() : null;
  }

  @Override
  public ValueType clone() {
    try {
      ValueType clone = (ValueType)super.clone();
      clone.value = value;
      clone.max = max;
      clone.type = type;

      if (curveOne != null) clone.curveOne = curveOne.clone();
      if (curveTwo != null) clone.curveTwo = curveTwo.clone();
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }


  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(type, "type", Type.CONSTANT);
    oc.write(value, "value", 0.0f);
    oc.write(max, "max", 0.0f);
    oc.write(curveOne, "curveone", null);
    oc.write(curveTwo, "curvetwo", null);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    type = ic.readEnum("type", Type.class, Type.CONSTANT);
    value = ic.readFloat("value", 0.0f);
    max = ic.readFloat("max", 0.0f);
    curveOne = (Curve) ic.readSavable("curveone", null);
    curveTwo = (Curve) ic.readSavable("curvetwo", null);
  }

  public boolean equals(Object o) {
    if (!(o instanceof ValueType)) return false;

    ValueType check = (ValueType)o;

    if (type != check.type) return false;
    if (value != check.value) return false;
    if (max != check.max) return false;

    return true;
  }
}
