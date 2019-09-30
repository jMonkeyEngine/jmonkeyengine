package com.jme3.particles.emittershapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.particles.EmitterShape;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Sphere;

import java.io.IOException;

public class EmitterLine extends EmitterShape {

  private float radius = 1.0f;

  private transient Vector3f nextDirection = new Vector3f();
  private transient Vector3f nextPosition = new Vector3f();

  public EmitterLine() {

  }


  public EmitterLine(float radius) {
    this.radius = radius;
  }


  @Override
  public Spatial getDebugShape(Material mat, boolean ignoreTransforms) {
    Line line = new Line(new Vector3f(-radius, 0, 0), new Vector3f(radius, 0, 0));
    Geometry geometry = new Geometry("DebugShape", line);
    geometry.setMaterial(mat);
    //geometry.setIgnoreTransform(ignoreTransforms);
    return geometry;
  }

  @Override
  public void setNext() {
    nextPosition.set(radius * (2.0f * (FastMath.nextRandomFloat() - 0.5f)), 0, 0);
    nextDirection.set(0, 1, 0);

    applyRootBehaviors();
  }

  public float getRadius() {
    return radius;
  }

  public void setRadius(float radius) {
    this.radius = radius;
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
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    radius = ic.readFloat("radius", 1.0f);

  }

  @Override
  public EmitterLine clone() {
    try {
      EmitterLine clone = (EmitterLine) super.clone();
      return clone;
    } catch (Exception e) {
      throw new AssertionError();
    }
  }


  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof EmitterLine)) return false;


    EmitterLine check = (EmitterLine)o;

    if (radius != check.radius) return false;


    return true;
  }
}
