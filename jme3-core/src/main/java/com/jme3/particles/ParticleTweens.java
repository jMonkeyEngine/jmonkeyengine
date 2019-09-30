package com.jme3.particles;

import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.tween.Tween;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.particles.influencers.GravityInfluencer;
import com.jme3.particles.valuetypes.ColorValueType;
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.particles.valuetypes.VectorValueType;

/**
 * Particle Tweens
 *
 * This is a helper class that holds a bunch of standard tweens for use in the animation system.
 *
 * @author Jedic
 */
public class ParticleTweens {

  public static Tween startSize(Emitter emitter, double length, float startSize, float endSize) {
    return new ValueTween(length, emitter.getStartSize(), startSize, endSize);
  }

  public static Tween lifeMin(Emitter emitter, double length, float min, float max) {
    return new ValueTween(length, emitter.getLifeMin(), min, max);
  }

  public static Tween lifeMax(Emitter emitter, double length, float min, float max) {
    return new ValueTween(length, emitter.getLifeMax(), min, max);
  }

  public static Tween startSpeed(Emitter emitter, double length, float min, float max) {
    return new ValueTween(length, emitter.getStartSpeed(), min, max);
  }

  public static Tween gravity(Emitter emitter, double length, Vector3f min, Vector3f max) {
    GravityInfluencer gravityInfluencer = emitter.getInfluencer(GravityInfluencer.class);
    return new VectorValueTween(length, min, max, gravityInfluencer.getGravity());
  }

  public static Tween startColor(Emitter emitter, double length, ColorRGBA min, ColorRGBA max) {
    return new ColorValueTween(length, min, max,emitter.getStartColor());
  }

  public static Tween emissions(Emitter emitter, double length, int min, int max) {
    return new AbstractTween(length){
      @Override
      protected void doInterpolate(double t) {
        emitter.setEmissionsPerSecond((int) (min + (max - min) * t));
      }
    };
  }

  private static class ValueTween extends AbstractTween {
    private float min;
    private float max;
    private ValueType typeTarget;

    public ValueTween(double length, ValueType target, float min, float max) {
      super(length);
      this.min = min;
      this.max = max;
      this.typeTarget = target;
    }

    @Override
    protected void doInterpolate(double t) {
      typeTarget.setValue((float) (min + (max - min) * t));
    }
  }

  private static class VectorValueTween extends AbstractTween {
    private Vector3f min;
    private Vector3f max;
    private VectorValueType typeTarget;
    private Vector3f temp = new Vector3f();

    public VectorValueTween(double length, Vector3f min, Vector3f max, VectorValueType typeTarget) {
      super(length);
      this.min = min;
      this.max = max;
      this.typeTarget = typeTarget;
    }

    @Override
    protected void doInterpolate(double t) {
      temp.x = (float)(min.x + (max.x - min.x)*t);
      temp.y = (float)(min.y + (max.y - min.y)*t);
      temp.z = (float)(min.z + (max.z - min.z)*t);
      typeTarget.setValue(temp);
    }
  }


  private static class ColorValueTween extends AbstractTween {
    private ColorRGBA min;
    private ColorRGBA max;
    private ColorValueType typeTarget;
    private ColorRGBA temp = new ColorRGBA();

    public ColorValueTween(double length, ColorRGBA min, ColorRGBA max, ColorValueType typeTarget) {
      super(length);
      this.min = min;
      this.max = max;
      this.typeTarget = typeTarget;
    }

    @Override
    protected void doInterpolate(double t) {
      temp.r = (float)(min.r + (max.r - min.r)*t);
      temp.g = (float)(min.g + (max.g - min.g)*t);
      temp.b = (float)(min.b + (max.b - min.b)*t);
      temp.a = (float)(min.a + (max.a - min.a)*t);
      typeTarget.setColor(temp);
    }
  }

}
