package com.jme3.particles;

import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import java.io.IOException;

public abstract class EmitterShape implements Savable, Cloneable {

  protected float randomDirection = 0.0f;
  protected float orginDirection = 0.0f;
  protected float randomizePosition = 0.0f;


  protected transient Vector3f nextDirection = new Vector3f();
  protected transient Vector3f nextPosition = new Vector3f();
  protected transient Vector3f tempVec = new Vector3f();

  public abstract void setNext();
  public abstract void setNext(int index);
  public abstract int getIndex();
  public abstract Vector3f getNextTranslation();
  public abstract Vector3f getRandomTranslation();
  public abstract Vector3f getNextDirection();

  public abstract Spatial getDebugShape(Material mat, boolean ignoreTransforms);

  public float getRandomDirection() {
    return randomDirection;
  }

  public void setRandomDirection(float randomDirection) {
    this.randomDirection = randomDirection;
    if (randomDirection < 0) randomDirection = 0.0f;
    if (randomDirection > 1.0f) randomDirection = 1.0f;
  }

  public float getOrginDirection() {
    return orginDirection;
  }

  public void setOrginDirection(float orginDirection) {
    this.orginDirection = orginDirection;
    if (orginDirection < 0) orginDirection = 0.0f;
    if (orginDirection > 1.0f) orginDirection = 1.0f;
  }

  public float getRandomizePosition() {
    return randomizePosition;
  }

  public void setRandomizePosition(float randomizePosition) {
    this.randomizePosition = randomizePosition;
  }

  protected void applyRootBehaviors() {
    if (randomizePosition > 0) {
      nextPosition.add((randomizePosition * 2.0f * (FastMath.nextRandomFloat() - 0.5f)),
          (randomizePosition * 2.0f * (FastMath.nextRandomFloat() - 0.5f)),
          (randomizePosition * 2.0f * (FastMath.nextRandomFloat() - 0.5f)));
    }

    if (randomDirection > 0) {
      tempVec.set((2.0f * (FastMath.nextRandomFloat() - 0.5f)),
          (2.0f * (FastMath.nextRandomFloat() - 0.5f)),
          (2.0f * (FastMath.nextRandomFloat() - 0.5f)));
      nextDirection.x = nextDirection.x * (1.0f - randomDirection) + randomDirection * tempVec.x;
      nextDirection.y = nextDirection.y * (1.0f - randomDirection) + randomDirection * tempVec.y;
      nextDirection.z = nextDirection.z * (1.0f - randomDirection) + randomDirection * tempVec.z;
    }

    if (orginDirection > 0) {
      tempVec.set(nextPosition);
      tempVec.normalizeLocal();

      nextDirection.x = nextDirection.x * (1.0f - orginDirection) + orginDirection * tempVec.x;
      nextDirection.y = nextDirection.y * (1.0f - orginDirection) + orginDirection * tempVec.y;
      nextDirection.z = nextDirection.z * (1.0f - orginDirection) + orginDirection * tempVec.z;
    }
  }


  @Override
  public void write(JmeExporter ex) throws IOException {
    OutputCapsule oc = ex.getCapsule(this);
    oc.write(randomDirection, "randomdirection", 0.0f);
    oc.write(orginDirection, "orgindirection", 0.0f);
    oc.write(randomizePosition, "randomizeposition", 0.0f);
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    InputCapsule ic = im.getCapsule(this);
    randomDirection = ic.readFloat("randomdirection", 0.0f);
    orginDirection = ic.readFloat("orgindirection", 0.0f);
    randomizePosition = ic.readFloat("randomizeposition", 0.0f);

  }

  @Override
  public EmitterShape clone() {
    try {
      EmitterShape clone = (EmitterShape) super.clone();
      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }

  public boolean equals(Object o) {
    if (!(o instanceof EmitterShape)) return false;

    EmitterShape check = (EmitterShape)o;

    if (randomDirection != check.randomDirection) return false;
    if (orginDirection != check.orginDirection) return false;
    if (randomizePosition != check.randomizePosition) return false;


    return true;
  }

}
