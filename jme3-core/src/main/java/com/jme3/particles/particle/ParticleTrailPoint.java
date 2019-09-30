package com.jme3.particles.particle;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class ParticleTrailPoint {
  public Vector3f position = new Vector3f();
  public Vector3f velocity = new Vector3f();
  public float size;
  public ColorRGBA color = new ColorRGBA();
  public float life = 0;

  public ParticleTrailPoint() {

  }

  public ParticleTrailPoint(Vector3f p, Vector3f vel, float size, ColorRGBA color, float life) {
    position.set(p);
    velocity.set(vel);
    this.size = size;
    this.color.set(color);
    this.life = life;
  }
}
