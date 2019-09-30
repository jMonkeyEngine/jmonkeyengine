package com.jme3.particles.valuetypes;

import com.jme3.export.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import java.io.IOException;

public class VectorValueType implements Savable, Cloneable {

  public enum Type {
    CONSTANT,
    CURVE,
    RANDOM,
    RANDOM_BETWEEN_CURVES
  }

  private Type type = Type.CONSTANT;
  private Vector3f value = new Vector3f();

  // used as a max value for random between constants
  private Vector3f second = null;

  // used for curve type
  private Curve x1 = null;
  private Curve y1 = null;
  private Curve z1 = null;

  // Used for random between two curves
  private Curve x2 = null;
  private Curve y2 = null;
  private Curve z2 = null;

  private transient Vector3f tempValue = new Vector3f();


  public VectorValueType() {

  }

  public VectorValueType(Vector3f v) {
    value = v;
  }

  public VectorValueType(Vector3f min, Vector3f max) {
    value = min;
    second = max;
    type = Type.RANDOM;
  }


  public Vector3f getValue3f(float time, float particleRandom, Vector3f store) {
    Vector3f result = null;
    if (store != null) {
      result = store;
    } else {
      result = new Vector3f();
    }

    switch (type) {
      case CONSTANT: result.set(value); break;
      case RANDOM:
        result.x = FastMath.interpolateLinear(FastMath.nextRandomFloat(), value.x, second.x);
        result.y = FastMath.interpolateLinear(FastMath.nextRandomFloat(), value.y, second.y);
        result.z = FastMath.interpolateLinear(FastMath.nextRandomFloat(), value.z, second.z);
      break;
      case CURVE:
        result.x = x1.getValue(time);
        result.y = y1.getValue(time);
        result.z = z1.getValue(time);
      break;
      case RANDOM_BETWEEN_CURVES:
        result.x = x1.getValue(time);
        result.y = y1.getValue(time);
        result.z = z1.getValue(time);

        tempValue.x = x2.getValue(time);
        tempValue.y = y2.getValue(time);
        tempValue.z = z2.getValue(time);

        result.interpolateLocal(tempValue, particleRandom);
      break;
      default: break;
    }

    return result;
  }

  public void setValue(Vector3f value) {
    this.value = value;
    x1 = y1 = z1 = null;
    x2 = y2 = z2 = null;
    value = second = null;
    this.type = Type.CONSTANT;
  }

  public void setValue(Vector3f min, Vector3f max) {
    this.value = min;
    this.second = max;
    x1 = y1 = z1 = null;
    x2 = y2 = z2 = null;
    value = second = null;
    this.type = Type.RANDOM;
  }

  public void setCurve(Curve x, Curve y, Curve z) {
    this.x1 = x;
    this.y1 = y;
    this.z1 = z;
    x2 = y2 = z2 = null;
    value = second = null;
    this.type = Type.CURVE;
  }

  public void setCurve(Curve x, Curve y, Curve z, Curve x2, Curve y2, Curve z2) {
    this.x1 = x;
    this.y1 = y;
    this.z1 = z;
    this.x2 = x2;
    this.y2 = y2;
    this.z2 = z2;
    value = second = null;
    this.type = Type.RANDOM_BETWEEN_CURVES;
  }

  public void set(VectorValueType v) {
    if (v.value != null) value = v.value.clone();
    if (v.second != null) second = v.second.clone();
    if (v.x1 != null) x1 = v.x1.clone();
    if (v.y1 != null) y1 = v.y1.clone();
    if (v.z1 != null) z1 = v.z1.clone();
    if (v.x2 != null) x1 = v.x2.clone();
    if (v.y2 != null) y1 = v.y2.clone();
    if (v.z2 != null) z1 = v.z2.clone();
  }

  public Vector3f getValue() {
    return value;
  }

  public Vector3f getMax() {
    return second;
  }

  public Curve getX1() {
    return x1;
  }

  public Curve getY1() {
    return y1;
  }

  public Curve getZ1() {
    return z1;
  }

  public Curve getX2() {
    return x2;
  }

  public Curve getY2() {
    return y2;
  }

  public Curve getZ2() {
    return z2;
  }

  public Type getType() {
    return type;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(value, "value", null);
    oc.write(second, "second", null);
    oc.write(x1, "x1", null);
    oc.write(y1, "y1", null);
    oc.write(z1, "z1", null);
    oc.write(x2, "x2", null);
    oc.write(y2, "y2", null);
    oc.write(z2, "z2", null);
    oc.write(type, "type", Type.CONSTANT);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    value = (Vector3f)ic.readSavable("value", null);
    second = (Vector3f)ic.readSavable("second", null);
    x1 = (Curve)ic.readSavable("x1", null);
    y1 = (Curve)ic.readSavable("y1", null);
    z1 = (Curve)ic.readSavable("z1", null);
    x2 = (Curve)ic.readSavable("x2", null);
    y2 = (Curve)ic.readSavable("y2", null);
    z2 = (Curve)ic.readSavable("z2", null);
    type = ic.readEnum("type", Type.class, Type.CONSTANT);
  }

  @Override
  public VectorValueType clone() {
    try {
      VectorValueType clone = (VectorValueType)super.clone();
      clone.type = type;
      if (value != null) clone.value = value.clone();
      if (second != null) clone.second = second.clone();
      if (x1 != null) clone.x1 = x1.clone();
      if (y1 != null) clone.y1 = y1.clone();
      if (z1 != null) clone.z1 = z1.clone();
      if (x2 != null) clone.x2 = x2.clone();
      if (y2 != null) clone.y2 = y2.clone();
      if (z2 != null) clone.z2 = z2.clone();
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public boolean equals(Object o) {
    if (!(o instanceof VectorValueType)) return false;

    VectorValueType check = (VectorValueType)o;

    if (type != check.type) return false;
    if (value != null && !value.equals(check.value) || value == null && check.value != null) return false;
    if (second != null && !second.equals(check.second) || second == null && check.second != null) return false;
    if (x1 != null && !x1.equals(check.x1) || x1 == null && check.x1 != null) return false;
    if (y1 != null && !y1.equals(check.y1) || y1 == null && check.y1 != null) return false;
    if (z1 != null && !z1.equals(check.z1) || z1 == null && check.z1 != null) return false;
    if (x2 != null && !x2.equals(check.x2) || x2 == null && check.x2 != null) return false;
    if (y2 != null && !y2.equals(check.y2) || y2 == null && check.y2 != null) return false;
    if (z2 != null && !z2.equals(check.z2) || z2 == null && check.z2 != null) return false;

    return true;
  }
}
