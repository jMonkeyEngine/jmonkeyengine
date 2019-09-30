/*
 * Copyright (c) 2009-2019 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.particles;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.particles.emittershapes.EmitterMesh;
import com.jme3.particles.emittershapes.EmitterSphere;
import com.jme3.particles.influencers.ParticleInfluencer;
import com.jme3.particles.particle.ParticleData;
import com.jme3.particles.particle.ParticleDataMesh;
import com.jme3.particles.particle.ParticleDataPointMesh;
import com.jme3.particles.particle.ParticleDataTriMesh;
import com.jme3.particles.valuetypes.ColorValueType;
import com.jme3.particles.valuetypes.ValueType;
import com.jme3.particles.valuetypes.VectorValueType;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Emitter
 *
 * A particle emitter is a special kind of node which can be used to simulate a variety of special effects.
 *
 * @author t0neg0d
 * @author Jedic
 */
public class Emitter extends Node {

  private String name;
  private EmitterShape emitterShape = new EmitterMesh();
  // Particle Display Meshes
  private Class datameshType = ParticleDataTriMesh.class;
  private Mesh templateMesh = null;
  private ParticleDataMesh mesh;

  // Render info
  private Material material;
  private Geometry particleGeo;
  private BillboardMode billboardMode = BillboardMode.Camera;

  // debug nodes
  private Geometry testPartGeo;
  private Spatial testEmitterShape;
  private boolean TEST_EMITTER = false;
  private boolean TEST_PARTICLES = false;

  // ParticleData info
  private ParticleData[] particles;
  private int maxParticles;
  private int activeParticleCount = 0;
  private Map<String, ParticleInfluencer> influencerMap = new HashMap<>();


  // start attributes
  private ValueType startSpeed = new ValueType(1.0f);
  private ColorValueType startColor = new ColorValueType(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f));
  private VectorValueType startRotation = new VectorValueType(new Vector3f(0, 0, 0));
  private ValueType startSize = new ValueType(1.0f);
  private ValueType lifeMin = new ValueType(0.5f);
  private ValueType lifeMax = new ValueType(1.0f);

  // Emitter info
  private int nextIndex = 0;
  private float targetInterval = .00015f, currentInterval = 0;
  private int emissionsPerSecond, totalParticlesThisEmission, particlesPerEmission;
  private boolean useStaticParticles = false;
  private boolean useRandomEmissionPoint = false;
  private boolean particlesFollowEmitter = true;
  private boolean enabled = true;


  // used for emitters with a definitive start and end time
  private float duration = 5.0f;

  // alows the emitter to keep emitting particles forever
  private boolean looping = true;

  // pre simulate the particle system so it starts as if it has been running
  private boolean preCalculate = false;

  // used to keep track of the current systems life
  private float currentDuration = 0.0f;

  // Used to control the emitter node


  public Emitter() {
    setBatchHint(BatchHint.Never);
    setQueueBucket(RenderQueue.Bucket.Translucent);
  }

  /**
   * Creates a new instance of the Emitter class
   *
   * @param name The name of the particles (used as the output Node name
   * containing the ParticleDataMesh)
   * @param maxParticles The maximum number of particles handled by the particles
   * @param influencers The list of ParticleInfluencer's to add to the particles
   * control
   */
  public Emitter(String name, Material particleMat, int maxParticles, ParticleInfluencer... influencers) {
    super(name);
    this.maxParticles = maxParticles;
    material = particleMat;

    for (ParticleInfluencer pi : influencers) {
      addInfluencer(pi, false);
    }

    initParticles(datameshType, templateMesh);

    // add the emitter control
    addControl(new EmitterControl());
    setBatchHint(BatchHint.Never);
    setQueueBucket(RenderQueue.Bucket.Translucent);

  }

  public Emitter(String name, Material particleMat, int maxParticles) {
    super(name);
    this.maxParticles = maxParticles;
    material = particleMat;

    initParticles(datameshType, templateMesh);

    // add the emitter control
    addControl(new EmitterControl());
    setBatchHint(BatchHint.Never);
    setQueueBucket(RenderQueue.Bucket.Translucent);

  }

  public void setParticleMeshType(Class type, Mesh mesh) {
    datameshType = type;
    templateMesh = mesh;
    initParticles(datameshType, templateMesh);
  }

  private <T extends ParticleDataMesh> void initParticles(Class<T> t, Mesh template) {
    try {
      this.mesh = t.newInstance();
      if (template != null) {
        this.mesh.extractTemplateFromMesh(template);
      }

      initParticles();
      attachChildGeo();
    } catch (InstantiationException ex) {
      Logger.getLogger(Emitter.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      Logger.getLogger(Emitter.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void initParticles() {
    particles = new ParticleData[maxParticles];

    for (int i = 0; i < maxParticles; i++) {
      particles[i] = new ParticleData();
      particles[i].emitter = this;
      particles[i].index = i;
      particles[i].reset();
    }

    // initialize particle data
    mesh.initParticleData(this, maxParticles);

    // Run model init for any sort of special setup that needs to be done on initialization
    for (ParticleInfluencer influencer : influencerMap.values()) {
      influencer.initializeInfluencer(this);
    }
  }
  
  public void reset() {
    for (int i = 0; i < maxParticles; i++) {
      particles[i].reset();
    }
    
    activeParticleCount = 0;
    nextIndex = 0;
    currentDuration = 0.0f;
  }

  /**
   * Sets the particle particles shape to the specified Emitter Shape
   *
   * @param shape The Emitter shape to generate particles from
   */
  public final void setShape(EmitterShape shape) {
    emitterShape = shape;
  }

  /**
   * Returns the current ParticleData Emitter's EmitterShape
   *
   * @return The EmitterShape
   */
  public EmitterShape getShape() {
    return emitterShape;
  }

  /**
   * Specifies the number of times the particle particles will emit particles over
   * the course of one second
   *
   * @param emissionsPerSecond The number of particle emissions per second, must be at least 1
   */
  public void setEmissionsPerSecond(int emissionsPerSecond) {
    if (emissionsPerSecond > 0) {
      this.emissionsPerSecond = emissionsPerSecond;
      targetInterval = 1f / emissionsPerSecond;
    }
  }
  
  public int getEmissionsPerSecond() {
    return emissionsPerSecond;
  }

  /**
   * Specifies the number of particles to be emitted per emission.
   *
   * @param particlesPerEmission The number of particle to emit per emission, must be greater or equal to 1
   */
  public void setParticlesPerEmission(int particlesPerEmission) {
    this.particlesPerEmission = particlesPerEmission;
  }
  
  public int getParticlesPerEmission() {
    return particlesPerEmission;
  }

  /**
   * Particles are created as staticly placed, with no velocity. Particles set
   * to static with remain in place and follow the particles shape's animations.
   *
   * @param useStaticParticles
   */
  public void setUseStaticParticles(boolean useStaticParticles) {
    this.useStaticParticles = useStaticParticles;
  }

  /**
   * Returns if particles are flagged as static
   *
   * @return Current state of static particle flag
   */
  public boolean getUseStaticParticles() {
    return this.useStaticParticles;
  }

  /**
   * Particles are effected by updates to the translation of the particles node.
   * This option is set to false by default
   *
   * @param particlesFollowEmitter Particles should/should not update according
   * to the particles node's translation updates
   */
  public void setParticlesFollowEmitter(boolean particlesFollowEmitter) {
    this.particlesFollowEmitter = particlesFollowEmitter;
    if (particleGeo != null) {
      particleGeo.setIgnoreTransform(!particlesFollowEmitter);
    }
  }

  /**
   * Returns if the particles are set to update according to the particles node's
   * translation updates
   *
   * @return Current state of the follows particles flag
   */
  public boolean getParticlesFollowEmitter() {
    return this.particlesFollowEmitter;
  }

  public void setUseRandomEmissionPoint(boolean useRandomEmissionPoint) {
    this.useRandomEmissionPoint = useRandomEmissionPoint;
  }

  public boolean getUseRandomEmissionPoint() {
    return this.useRandomEmissionPoint;
  }

  // Life Cycle
  /**
   * Sets the inner and outer bounds of the time a particle will remain alive
   * (active)
   *
   * @param lifeMin The minimum time a particle must remain alive once emitted
   * @param lifeMax The maximum time a particle can remain alive once emitted
   */
  public void setLifeMinMax(ValueType lifeMin, ValueType lifeMax) {
    this.lifeMin = lifeMin;
    this.lifeMax = lifeMax;
  }

  /**
   * Sets the inner and outter bounds of the time a particle will remain alive
   * (active) to a fixed duration of time
   *
   * @param life The fixed duration an emitted particle will remain alive
   */
  public void setLifeFixedDuration(float life) {
    this.lifeMin = new ValueType(life);
    this.lifeMax = new ValueType(life);
  }

  /**
   * Sets the outter bounds of the time a particle will remain alive (active)
   *
   * @param lifeMax The maximum time a particle can remain alive once emitted
   */
  public void setLifeMax(ValueType lifeMax) {
    this.lifeMax = lifeMax;
  }

  /**
   * Returns the maximum time a particle can remain alive once emitted.
   *
   * @return The maximum time a particle can remain alive once emitted
   */
  public ValueType getLifeMax() {
    return this.lifeMax;
  }

  /**
   * Sets the inner bounds of the time a particle will remain alive (active)
   *
   * @param lifeMin The minimum time a particle must remain alive once emitted
   */
  public void setLifeMin(ValueType lifeMin) {
    this.lifeMin = lifeMin;
  }

  /**
   * Returns the minimum time a particle must remain alive once emitted
   *
   * @return The minimum time a particle must remain alive once emitted
   */
  public ValueType getLifeMin() {
    return this.lifeMin;
  }

  /**
   * Returns the starting size of the particle once it is emitted
   * @return the particles start size
   */
  public ValueType getStartSize() {
    return startSize;
  }

  /**
   * Sets the starting size of the particle once emitted
   * @param startSize - the start size
   */
  public void setStartSize(ValueType startSize) {
    this.startSize = startSize;
  }

  public void setEmitterTestMode(boolean showEmitterShape, boolean showParticleMesh) {
    this.TEST_EMITTER = showEmitterShape;
    this.TEST_PARTICLES = showParticleMesh;
  }

  public int getMaxParticles() {
    return this.maxParticles;
  }

  public void setMaxParticles(int count) {
    this.maxParticles = count;


    // init particles
    initParticles(datameshType, templateMesh);

  }

  /**
   * Adds a new ParticleData influencer to the chain of influencers that will
   * effect particles
   *
   * @param influencer The particle influencer to add to the chain
   */
  public final void addInfluencer(ParticleInfluencer influencer) {
    addInfluencer(influencer, true);
  }

  public final void addInfluencer(ParticleInfluencer influencer, boolean reload) {
    influencerMap.put(influencer.getClass().getName(), influencer);

    if (reload) {
      initParticles(datameshType, templateMesh);
    }
  }

  /**
   * Returns the current chain of particle influencers
   *
   * @return The Collection of particle influencers
   */
  public Collection<ParticleInfluencer> getInfluencerMap() {
    return this.influencerMap.values();
  }

  /**
   * Returns the influencer with a given name
   * @param type
   * @return
   */
  public ParticleInfluencer getInfluencer(String type) {
    return influencerMap.get(type);
  }

  /**
   * Returns the first instance of a specified ParticleData influencer type
   *
   * @param <T>
   * @param c
   * @return
   */
  public <T extends ParticleInfluencer> T getInfluencer(Class<T> c) {
    return (T) influencerMap.get(c.getName());
  }

  /**
   * Enables the particle particles. The particles is disabled by default.
   *
   * @param enabled Activate/deactivate the particles
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public ValueType getStartSpeed() {
    return startSpeed;
  }

  public void setStartSpeed(ValueType startSpeed) {
    this.startSpeed = startSpeed;
  }

  public VectorValueType getStartRotation() {
    return startRotation;
  }

  public void setStartRotation(VectorValueType startRotation) {
    this.startRotation = startRotation;
  }

  public float getDuration() {
    return duration;
  }

  public void setDuration(float duration) {
    this.duration = duration;
  }

  public boolean isLooping() {
    return looping;
  }

  public void setLooping(boolean looping) {
    this.looping = looping;
  }

  public boolean isPreCalculate() {
    return preCalculate;
  }

  public void setPreCalculate(boolean preCalculate) {
    this.preCalculate = preCalculate;
  }

  public float getCurrentDuration() {
    return currentDuration;
  }

  public ColorValueType getStartColor() {
    return startColor;
  }

  public void setStartColor(ColorValueType startColor) {
    this.startColor = startColor;
  }

  public Material getParticleMat() {
    return material;
  }

  public void setParticleMat(Material mat) {
    this.material = mat;

    if (particleGeo != null) particleGeo.setMaterial(mat);
  }

  public ParticleDataMesh getMesh() {
    return mesh;
  }

  public Material getMaterial() {
    return material;
  }

  @Override
  public void setMaterial(Material material) {
    this.material = material;
  }

  public BillboardMode getBillboardMode() {
    return billboardMode;
  }

  public void setBillboardMode(BillboardMode billboardMode) {
    this.billboardMode = billboardMode;
  }

  public void setDebug(AssetManager manager, boolean debugEmitter, boolean debugParticles) {
    if (!debugEmitter && testEmitterShape != null) {
      testEmitterShape.removeFromParent();
      testEmitterShape = null;
    }

    if (!debugParticles && testPartGeo != null) {
      testPartGeo.removeFromParent();
      testPartGeo = null;
    }

    Material mat = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Blue);
    mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    mat.getAdditionalRenderState().setWireframe(true);

    if (debugEmitter) {
      testEmitterShape = emitterShape.getDebugShape(mat, !particlesFollowEmitter);
      attachChild(testEmitterShape);
      testEmitterShape.setMaterial(mat);

    }

    if (debugParticles) {
      testPartGeo = new Geometry("ParticleDebugMesh");
      testPartGeo.setMesh(mesh);
      attachChild(testPartGeo);
      testPartGeo.setIgnoreTransform(!particlesFollowEmitter);
      testPartGeo.setMaterial(mat);
    }
  }

  private void attachChildGeo() {

    this.detachAllChildren();

    if (particleGeo != null) {
      particleGeo.removeFromParent();
      particleGeo = null;
    }

    if (particleGeo == null && material != null) {
      particleGeo = new Geometry();
      particleGeo.setMesh(mesh);
      particleGeo.setMaterial(material);
      particleGeo.setIgnoreTransform(!particlesFollowEmitter);
      attachChild(particleGeo);
    }


    if (particleGeo != null && particleGeo.getParent() == null) {
      attachChild(particleGeo);
    }

    if (testPartGeo != null) {
      attachChild(testPartGeo);
    }


    // attach debug?

  }

  public void updateEmitter(float tpf) {
    //long t = System.currentTimeMillis();
    if (enabled) {
      currentDuration += tpf;

      if (currentDuration > duration && looping) {
        currentDuration -= duration;
      }

      //if (delay <= 0.0f ) {
        for (ParticleData p : particles) {
          if (p.active) {
            p.update(tpf);
          }
        }

        currentInterval += tpf;

        if (currentDuration <= duration) {
          // check for particle emission
          if (currentInterval >= targetInterval) {
            totalParticlesThisEmission = calcParticlesPerEmission();
            for (int i = 0; i < totalParticlesThisEmission; i++) {
              emitNextParticle();
            }
            currentInterval -= targetInterval;
          }

          // run any sort of influencer updates we may need
          for (ParticleInfluencer influencer : influencerMap.values()) {
            influencer.updateGlobal(tpf);
          }

        }

        updateModelBound();
    }
    //System.out.println("Output: " + (System.currentTimeMillis() - t));
  }

  private int calcParticlesPerEmission() {
    if (particlesPerEmission == 0) {
      return 0;
    }

    int total =  (int) (currentInterval / targetInterval * particlesPerEmission);
    
    return total > particlesPerEmission ? particlesPerEmission : total;
  }

  /**
   * Emits the next available (non-active) particle
   */
  public void emitNextParticle() {
    if (nextIndex != -1 && nextIndex < maxParticles) {
      particles[nextIndex].initialize(lifeMin, lifeMax);
      particles[nextIndex].startlife =
          (lifeMax.getValue(0, particles[nextIndex].randomValue) - lifeMin.getValue(0, particles[nextIndex].randomValue)) * FastMath.nextRandomFloat()
              + lifeMin.getValue(0, particles[nextIndex].randomValue);
      int searchIndex = nextIndex;
      while (particles[searchIndex].active) {
        searchIndex++;
        if (searchIndex > particles.length - 1) {
          searchIndex = -1;
          break;
        }
      }
      nextIndex = searchIndex;
    }
  }

  /**
   * Emits all non-active particles
   */
  public void emitAllParticles() {
    for (ParticleData p : particles) {
      if (!p.active) {
        p.initialize(lifeMin, lifeMax);
      }
    }
  }

  /**
   * Deactivates and resets the specified particle
   *
   * @param p The particle to reset
   */
  public void killParticle(ParticleData p) {
    for (ParticleData particle : particles) {
      if (particle == p) {
        p.reset();
      }
    }
  }

  public int getActiveParticleCount() {
    return activeParticleCount;
  }

  public void incActiveParticleCount() {
    activeParticleCount++;
  }

  public void decActiveParticleCount() {
    activeParticleCount--;
  }

  /**
   * Deactivates and resets the specified particle
   *
   * @param index The index of the particle to reset
   */
  public void killParticle(int index) {
    particles[index].reset();
  }

  /**
   * This method should not be called. Particles call this method to help track
   * the next available particle index
   *
   * @param index The index of the particle that was just reset
   */
  public void setNextIndex(int index) {
    if (index < nextIndex || nextIndex == -1) {
      nextIndex = index;
    }
  }

  public void renderEmitter(RenderManager rm, ViewPort vp) {
    Camera cam = vp.getCamera();

    if (mesh.getClass() == ParticleDataPointMesh.class) {
      float C = cam.getProjectionMatrix().m00;
      C *= cam.getWidth() * 0.5f;

      // send attenuation params
      material.setFloat("Quadratic", C);
    }

    Matrix3f inverseRotation = Matrix3f.IDENTITY;

    for (Spatial s : this.getChildren()) {
      if (s instanceof Geometry) {
        Geometry g = (Geometry)s;
        if (g.getMesh() instanceof ParticleDataMesh) {
          ((ParticleDataMesh)g.getMesh()).updateParticleData(particles, cam, inverseRotation);
        }
      }
    }

  }

  @Override
  public void write(JmeExporter ex) throws IOException {
    super.write(ex);
    OutputCapsule oc = ex.getCapsule(this);

    oc.write(name, "name", null);
    oc.writeStringSavableMap(influencerMap, "influencers", null);
    oc.write(maxParticles, "maxParticles", 30);
    oc.write(targetInterval, "targetInterval", .00015f);
    oc.write(currentInterval, "currentInterval", 0f);
    oc.write(emissionsPerSecond, "emissionsPerSecond", 20);
    oc.write(particlesPerEmission, "particlesPerEmission", 0);
    oc.write(useStaticParticles, "useStaticParticles", false);
    oc.write(useRandomEmissionPoint, "useRandomEmissionPoint", false);
    oc.write(material, "material", null);
    oc.write(billboardMode, "billboardMode", BillboardMode.Camera);
    oc.write(particlesFollowEmitter, "particlesFollowEmitter", false);
    oc.write(startColor, "startColor", null);
    oc.write(startRotation, "startRotation", null);
    oc.write(startSpeed, "startSpeed", null);
    oc.write(startSize, "startsize", new ValueType(1.0f));
    oc.write(lifeMin, "lifeMin", null);
    oc.write(lifeMax, "lifeMax", null);
    oc.write(duration, "duration", 5.0f);
    oc.write(looping, "looping", true);
    oc.write(preCalculate, "preCalculate", false);
    oc.write(enabled, "enabled", false);
    oc.write(emitterShape, "emitterShape", null);
    oc.write(templateMesh, "templateMesh", null);

    // we are using this just to save what class we used
    oc.write(mesh, "datameshType", new ParticleDataTriMesh());
  }

  @Override
  public void read(JmeImporter im) throws IOException {
    super.read(im);
    InputCapsule ic = im.getCapsule(this);
    name = ic.readString("name", null);
    influencerMap = (Map<String, ParticleInfluencer>) ic.readStringSavableMap("influencers", new HashMap());
    maxParticles = ic.readInt("maxParticles", 30);
    targetInterval = ic.readFloat("targetInterval", .00015f);
    currentInterval = ic.readFloat("currentInterval", 0f);
    emissionsPerSecond = ic.readInt("emissionsPerSecond", 20);
    particlesPerEmission = ic.readInt("particlesPerEmission", 0);
    useStaticParticles = ic.readBoolean("useStaticParticles", false);
    useRandomEmissionPoint = ic.readBoolean("useRandomEmissionPoint", false);
    material = (Material) ic.readSavable("material", null);
    billboardMode = ic.readEnum("billboardMode", BillboardMode.class, BillboardMode.Camera);
    particlesFollowEmitter = ic.readBoolean("particlesFollowEmitter", false);
    startColor = (ColorValueType)ic.readSavable("startColor", new ColorValueType());
    startSpeed = (ValueType) ic.readSavable("startSpeed", new ValueType());
    startRotation = (VectorValueType) ic.readSavable("startRotation", new VectorValueType());
    startSize = (ValueType)ic.readSavable("startsize", new ValueType(1.0f));
    lifeMin = (ValueType) ic.readSavable("lifeMin", new ValueType(0.5f));
    lifeMax = (ValueType) ic.readSavable("lifeMax", new ValueType(1.0f));
    duration = ic.readFloat("duration", 5.0f);
    looping = ic.readBoolean("looping", true);
    preCalculate = ic.readBoolean("preCalculate", false);
    enabled = ic.readBoolean("enabled", false);
    emitterShape = (EmitterShape) ic.readSavable("emitterShape", new EmitterSphere());
    templateMesh = (Mesh) ic.readSavable("templateMesh", null);

    // small hack to get the mesh type to be used later
    datameshType = ((ParticleDataMesh) ic.readSavable("datameshType", new ParticleDataTriMesh())).getClass();

    // set control
    EmitterControl control = this.getControl(EmitterControl.class);
    if (control == null) {
      this.addControl(new EmitterControl());
    } else {
      control.setSpatial(this);
    }

    initParticles(datameshType, templateMesh);
  }
  
}
