package com.jme3.particles.emittershapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.particles.EmitterShape;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import java.io.IOException;

public class EmitterCone extends EmitterShape {

  private float angle = FastMath.PI / 4.0f;
  private float radius = 1.0f;
  private float length = 5.0f;
  private float arc = 2.0f * FastMath.PI;
  private float radiusThickness = 1.0f;
  private boolean emitFromVolume = false;

  private transient Quaternion temp = new Quaternion();

  public EmitterCone() {

  }

  @Override
  public Spatial getDebugShape(Material mat, boolean ignoreTransforms) {
    float height = emitFromVolume ? length : 1.0f;
    float secondRad = (float) (radius + Math.atan(angle) * (height));
    Cylinder cylinder = new Cylinder(2, 20, radius, secondRad, height, false, false);
    Geometry geometry = new Geometry("DebugShape", cylinder);
    geometry.setMaterial(mat);
    geometry.rotate(FastMath.PI / 2.0f, 0.0f, 0.0f);
    //geometry.setIgnoreTransform(ignoreTransforms);
    return geometry;
  }


  @Override
  public void setNext() {
    nextPosition.set(1, 0, 0);
    temp.set(Quaternion.IDENTITY);
    temp.fromAngleAxis(arc * FastMath.nextRandomFloat(), Vector3f.UNIT_Y);
    temp.mult(nextPosition, nextPosition);

    if (emitFromVolume) {
      float height = FastMath.nextRandomFloat() * length;
      float calcRadius = (float) (radius + Math.atan(angle) * height);

      // now generate length
      float v = FastMath.nextRandomFloat();
      float len = calcRadius * (v * radiusThickness + (1 - radiusThickness));
      nextPosition.multLocal(len);
      nextPosition.y = height;

    } else {

      // now generate length
      float v = FastMath.nextRandomFloat();
      float len = radius * (v * radiusThickness + (1 - radiusThickness));
      nextPosition.multLocal(len);

    }

    nextDirection.set(0, 1, 0);
    temp.fromAngleAxis(angle * FastMath.nextRandomFloat(), Vector3f.UNIT_Z);
    temp.mult(nextDirection, nextDirection);
    temp.fromAngleAxis(arc * FastMath.nextRandomFloat(), Vector3f.UNIT_Y);
    temp.mult(nextDirection, nextDirection);

    nextDirection.normalizeLocal();

    applyRootBehaviors();
  }

  public float getAngle() {
    return angle;
  }

  public void setAngle(float angle) {
    this.angle = angle;
  }

  public float getRadius() {
    return radius;
  }

  public void setRadius(float radius) {
    this.radius = radius;
  }

  public float getLength() {
    return length;
  }

  public void setLength(float length) {
    this.length = length;
  }

  public float getArc() {
    return arc;
  }

  public void setArc(float arc) {
    this.arc = arc;
  }

  public float getRadiusThickness() {
    return radiusThickness;
  }

  public void setRadiusThickness(float radiusThickness) {
    this.radiusThickness = radiusThickness;
  }

  public boolean isEmitFromVolume() {
    return emitFromVolume;
  }

  public void setEmitFromVolume(boolean emitFromVolume) {
    this.emitFromVolume = emitFromVolume;
  }

  @Override
  public void setNext(int index) {
    setNext();
  }

  @Override
  public int getIndex() {
    return -1;
  }

  @Override
  public Vector3f getNextTranslation() {
    return nextPosition;
  }

  @Override
  public Vector3f getRandomTranslation() {
    return nextPosition;
  }

  @Override
  public Vector3f getNextDirection() {
    return nextDirection;
  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(radius, "radius", 1.0f);
    oc.write(radiusThickness, "radiusthickness", 1.0f);
    oc.write(arc, "arc", FastMath.PI);
    oc.write(angle, "angle", FastMath.PI / 4.0f);
    oc.write(length, "length", 5.0f);
    oc.write(emitFromVolume, "emitfromvolume", false);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    radius = ic.readFloat("radius", 1.0f);
    radiusThickness = ic.readFloat("radiusthickness", 1.0f);
    arc = ic.readFloat("arc", FastMath.PI);
    angle = ic.readFloat("angle", FastMath.PI / 4.0f);
    length = ic.readFloat("length", 5.0f);
    emitFromVolume = ic.readBoolean("emitfromvolume", false);


  }


  @Override
  public EmitterCone clone() {
    try {
      EmitterCone clone = (EmitterCone) super.clone();
      return clone;
    } catch (Exception e) {
      throw new AssertionError();
    }
  }


  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof EmitterCone)) return false;


    EmitterCone check = (EmitterCone)o;

    if (radius != check.radius) return false;
    if (radiusThickness != check.radiusThickness) return false;
    if (arc != check.arc) return false;
    if (length != check.length) return false;
    if (emitFromVolume != check.emitFromVolume) return false;


    return true;
  }
}
