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

package jme3test.particles;

import com.jme3.anim.tween.Tween;
import com.jme3.app.SimpleApplication;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.particles.Emitter;
import com.jme3.particles.ParticleTweens;
import com.jme3.particles.emittershapes.*;
import com.jme3.particles.influencers.*;
import com.jme3.particles.particle.ParticleDataPointMesh;
import com.jme3.particles.valuetypes.*;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;

import java.io.IOException;

public class TestEmitter extends SimpleApplication {

    public static void main(String[] args){
        TestEmitter app = new TestEmitter();
        app.start();
    }

    private Geometry floor;
    private BitmapText hudText;
    private Emitter emitter;
    private Tween emitterTween;
    private double tweenAmount = 0.0f;
    private boolean debug = false;

    private Material getPartMat(String path) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        Texture tex = assetManager.loadTexture(path);
        mat.setTexture("Texture", tex);
        return mat;
    }


    public void addFloor() {
        if (floor != null) {
            rootNode.attachChild(floor);
        } else {
            Box b = new Box(8, 0.01f, 8);
            floor = new Geometry("Box", b);
            TangentBinormalGenerator.generate(b);
            Material mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
            mat.setTexture("DiffuseMap",
                assetManager.loadTexture("Effects/Particles/grid.png"));
            floor.setMaterial(mat);
            floor.setLocalTranslation(0, 0, 0);
            rootNode.attachChild(floor);


            SpotLight spot = new SpotLight();
            spot.setSpotRange(100f);                           // distance
            spot.setSpotInnerAngle(15f * FastMath.DEG_TO_RAD); // inner light cone (central beam)
            spot.setSpotOuterAngle(35f * FastMath.DEG_TO_RAD); // outer light cone (edge of the light)
            spot.setColor(ColorRGBA.White.mult(1.3f));         // light color
            spot.setPosition(new Vector3f(0, 10, 0));               // shine from camera loc
            spot.setDirection(new Vector3f(0, -1, 0));             // shine forward from camera loc
            spot.setColor(new ColorRGBA(0.6f, 0.4f, 0.95f, 1.0f));
            rootNode.addLight(spot);

            DirectionalLight sun = new DirectionalLight();
            sun.setDirection(new Vector3f(1, -1, -2).normalizeLocal());
            sun.setColor(ColorRGBA.White);
            rootNode.addLight(sun);
        }
    }

    private void addSimpleEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100);

        emitter.setStartSpeed(new ValueType(6.5f));
        emitter.setLifeMinMax(new ValueType(2.0f), new ValueType(2.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(1);
        emitter.setShape(new EmitterCone());
        ((EmitterCone)emitter.getShape()).setRadius(0.005f);

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }

    private void addGradientEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new ColorInfluencer());

        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient().addGradPoint(new ColorRGBA(1.0f, 0.0f, 0.0f, 1.0f), 0.0f)
                          .addGradPoint(new ColorRGBA(0.0f, 1.0f, 0.0f, 1.0f), 0.5f)
                          .addGradPoint(new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f), 1.0f)
        ));

        emitter.setStartSpeed(new ValueType(4.5f));
        emitter.setLifeMinMax(new ValueType(0.9f), new ValueType(0.9f));
        emitter.setEmissionsPerSecond(5);
        emitter.setParticlesPerEmission(1);
        emitter.setShape(new EmitterCone());

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }

    private void addSizeEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new SizeInfluencer()
        );

        emitter.setStartSize(new ValueType(0.5f));
        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 0.0f), new Vector2f(0.3f, 0.0f))
            .addControlPoint(new Vector2f(0.3f, 1.0f), new Vector2f(0.5f, 1.0f), new Vector2f(0.7f, 1.0f))
            .addControlPoint(new Vector2f(0.7f, 0.0f), new Vector2f(1.0f, 0.0f), null)
        ));

        emitter.setStartSpeed(new ValueType(4.5f));
        emitter.setLifeMinMax(new ValueType(0.9f), new ValueType(0.9f));
        emitter.setEmissionsPerSecond(5);
        emitter.setParticlesPerEmission(1);
        emitter.setShape(new EmitterCone());

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }

    private void addSimplePhysics() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            //new VelocityInfluencer(),
            new ColorInfluencer(),
            new SizeInfluencer(),
            new GravityInfluencer(),
            new BasicPhysicsInfluencer(floor)
        );

        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(
            new Curve()
                .addControlPoint(null, new Vector2f(0, 1), new Vector2f(0.4f, 1.0f))
                .addControlPoint(new Vector2f(0.4f, 1.0f), new Vector2f(1.0f, 1), null)
        ));
        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient()
                .addGradPoint(new ColorRGBA(1, 1, 1, 1.0f), 0.0f)
                .addGradPoint(new ColorRGBA(1, 1, 1, 1.0f), 0.8f)
                .addGradPoint(new ColorRGBA(1, 1, 1, 0.0f), 1.0f)
        ));

        emitter.setStartSize(new ValueType(0.2f));

        emitter.setStartSpeed(new ValueType(6.5f));
        //emitter.getInfluencer(SizeInfluencer.class).setMinMax(0.5f, 1.0f);
        emitter.setLifeMinMax(new ValueType(2.0f), new ValueType(2.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(1);
        emitter.setEnabled(true);
        emitter.setShape(new EmitterCone());
        ((EmitterCone)emitter.getShape()).setRadius(0.005f);
        emitter.rotate(0, 0, FastMath.PI * 0.8f);
        emitter.addControl(new Control() {
            private Spatial parent;
            @Override
            public Control cloneForSpatial(Spatial spatial) {
                return null;
            }

            @Override
            public void setSpatial(Spatial spatial) {
                parent = spatial;
            }

            @Override
            public void update(float v) {
                parent.rotate(0, 0, v);
            }

            @Override
            public void render(RenderManager renderManager, ViewPort viewPort) {

            }

            @Override
            public void write(JmeExporter jmeExporter) throws IOException {

            }

            @Override
            public void read(JmeImporter jmeImporter) throws IOException {

            }
        });
        emitter.setParticleMeshType(ParticleDataPointMesh.class, null);
        emitter.setParticlesFollowEmitter(false);
        emitter.getMaterial().setBoolean("PointSprite", true);
        emitter.setQueueBucket(RenderQueue.Bucket.Translucent);

        emitter.setLocalTranslation(0, 3.0f, 0);
        rootNode.attachChild(emitter);
    }

    private void addTrailEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            //new VelocityInfluencer(),
            new ColorInfluencer(),
            new SizeInfluencer(),
            new GravityInfluencer(),
            new TrailInfluencer(),
            new BasicPhysicsInfluencer(floor)
        );

        emitter.getInfluencer(TrailInfluencer.class).setTrailSize(new ValueType(0.05f));
        emitter.getInfluencer(TrailInfluencer.class).setTrailLife(0.5f);
        emitter.getInfluencer(TrailInfluencer.class).setTrailmat(getPartMat("Effects/Particles/part_beam.png"));
        emitter.getInfluencer(TrailInfluencer.class).setColorOverLifetime(new ColorValueType(new Gradient()
            .addGradPoint(new ColorRGBA(1, 1, 1, 1), 0.0f)
            .addGradPoint(new ColorRGBA(1, 1, 1, 0.5f), 0.8f)
            .addGradPoint(new ColorRGBA(1, 1, 1, 0.0f), 1.0f)
        ));

        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(
            new Curve()
                .addControlPoint(null, new Vector2f(0, 1), new Vector2f(0.4f, 1.0f))
                .addControlPoint(new Vector2f(0.4f, 1.0f), new Vector2f(1.0f, 1), null)
        ));
        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient()
                .addGradPoint(new ColorRGBA(1, 1, 1, 1.0f), 0.0f)
                .addGradPoint(new ColorRGBA(1, 1, 1, 1.0f), 0.8f)
                .addGradPoint(new ColorRGBA(1, 1, 1, 0.0f), 1.0f)
        ));

        emitter.setStartSize(new ValueType(0.2f));
        emitter.setStartSpeed(new ValueType(6.5f));
        //emitter.getInfluencer(SizeInfluencer.class).setMinMax(0.5f, 1.0f);
        emitter.setLifeMinMax(new ValueType(2.0f), new ValueType(2.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(1);
        emitter.setShape(new EmitterCone());
        ((EmitterCone)emitter.getShape()).setRadius(0.005f);
        //emitter.setParticlesFollowEmitter(false);
//        emitter.setParticleMeshType(ParticleDataPointMesh.class, null);
//        emitter.getMaterial().setBoolean("PointSprite", true);
//        emitter.setQueueBucket(RenderQueue.Bucket.Translucent);

        emitter.setLocalTranslation(0, 2.0f, 0);
//        emitter.rotate(0, 0, FastMath.PI * 0.8f);
//        emitter.setLocalTranslation(3, 3.0f, 0);
        //emitter.setDebug(assetManager, true, true);
        rootNode.attachChild(emitter);
    }


    private void addVelocityEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new ColorInfluencer(),
            new VelocityInfluencer()
        );

        Curve x = new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 1.0f), new Vector2f(0.2f, 1.0f))
            .addControlPoint(new Vector2f(0.2f, 1.0f), new Vector2f(0.3f, 1.0f), new Vector2f(0.5f, 1.0f))
            .addControlPoint(new Vector2f(0.3f, 0.1f), new Vector2f(0.4f, 0.1f), new Vector2f(0.6f, 0.1f))
            .addControlPoint(new Vector2f(0.8f, 0.0f), new Vector2f(1.0f, 0.0f), null);

        VectorValueType valueType = new VectorValueType();
        valueType.setCurve(x, x.clone(), x.clone());
        emitter.getInfluencer(VelocityInfluencer.class).setLinear(valueType);

        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient().addGradPoint(new ColorRGBA(0.8f, 0.6f, 0.0f, 0.6f), 0.0f)
                .addGradPoint(new ColorRGBA(0.8f, 0.0f, 0.0f, 0.5f), 0.8f)
                .addGradPoint(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.0f), 1.0f)
        ));

        emitter.setStartSpeed(new ValueType(20.5f));
        emitter.setLifeMinMax(new ValueType(1.0f), new ValueType(1.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(20);
        EmitterCircle circle = new EmitterCircle();
        circle.setOrginDirection(1.0f);
        emitter.setShape(circle);

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }


    private void addOrbitalVelocityEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new ColorInfluencer(),
            new SizeInfluencer(),
            new VelocityInfluencer()
        );

        Curve y = new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 3.0f), new Vector2f(0.2f, 3.0f))
            .addControlPoint(new Vector2f(0.8f, 3.0f), new Vector2f(1.0f, 3.0f), null);

        Curve xz = new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 1.0f), new Vector2f(0.2f, 1.0f))
            .addControlPoint(new Vector2f(0.8f, 1.0f), new Vector2f(1.0f, 1.0f), null);

        VectorValueType valueType = new VectorValueType();
        valueType.setCurve(xz.clone(), y, xz);
        emitter.getInfluencer(VelocityInfluencer.class).setOrbital(valueType);
        emitter.getInfluencer(VelocityInfluencer.class).setOrbitalRotations(new VectorValueType(new Vector3f(8, 8, 8)));

        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 0.0f), new Vector2f(0.3f, 0.0f))
            .addControlPoint(new Vector2f(0.3f, 1.0f), new Vector2f(0.5f, 1.0f), new Vector2f(0.7f, 1.0f))
            .addControlPoint(new Vector2f(0.7f, 0.0f), new Vector2f(1.0f, 0.0f), null)
        ));

        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient().addGradPoint(new ColorRGBA(0.8f, 0.6f, 0.0f, 0.6f), 0.0f)
                .addGradPoint(new ColorRGBA(0.8f, 0.0f, 0.0f, 0.5f), 0.8f)
                .addGradPoint(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.0f), 1.0f)
        ));

        emitter.setStartSize(new ValueType(0.7f));
        emitter.setStartSpeed(new ValueType(3.5f));
        emitter.setLifeMinMax(new ValueType(3.0f), new ValueType(3.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(5);
        emitter.setShape(new EmitterCone());

        emitter.setLocalTranslation(-0.5f, 0.5f, -0.5f);
        rootNode.attachChild(emitter);
    }


    private void addDestinationEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new SizeInfluencer(),
            new PreferredDestinationInfluencer()
        );

        emitter.getInfluencer(PreferredDestinationInfluencer.class)
            .setPreferredDestination(new VectorValueType(new Vector3f(5, 0, 0)));

        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 0.0f), new Vector2f(0.3f, 0.1f))
            .addControlPoint(new Vector2f(0.3f, 1.0f), new Vector2f(0.5f, 1.0f), new Vector2f(0.7f, 1.0f))
            .addControlPoint(new Vector2f(0.7f, 1.0f), new Vector2f(1.0f, 1.0f), null)
        ));

        emitter.setStartColor(new ColorValueType());
        emitter.setStartSize(new ValueType(0.7f));
        emitter.setStartSpeed(new ValueType(6.5f));
        emitter.setLifeMinMax(new ValueType(4.0f), new ValueType(4.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(5);
        emitter.setShape(new EmitterCone());

        emitter.setLocalTranslation(-2.5f, 0.5f, 0);
        rootNode.attachChild(emitter);
    }

    private void addSpriteEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_flame.png"), 200,
            new ColorInfluencer(),
            new SpriteInfluencer(),
            new SizeInfluencer(),
            new RandomInfluencer(),
            new RotationLifetimeInfluencer(),
            new VelocityInfluencer()
        );

        Curve x = new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 1.0f), new Vector2f(0.2f, 1.0f))
            .addControlPoint(new Vector2f(0.8f, 0.0f), new Vector2f(1.0f, 0.0f), null);

        VectorValueType valueType = new VectorValueType();
        valueType.setCurve(x, x.clone(), x.clone());
        emitter.getInfluencer(VelocityInfluencer.class).setLinear(valueType);
        emitter.getInfluencer(SpriteInfluencer.class).setSpriteCols(2);
        emitter.getInfluencer(SpriteInfluencer.class).setSpriteRows(2);
        emitter.getInfluencer(RotationLifetimeInfluencer.class).setSpeedOverLifetime(new VectorValueType(new Vector3f(0, 0, 3.0f)));

        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient().addGradPoint(new ColorRGBA(0.8f, 0.6f, 0.0f, 0.6f), 0.0f)
                .addGradPoint(new ColorRGBA(0.8f, 0.0f, 0.0f, 0.5f), 0.8f)
                .addGradPoint(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.0f), 1.0f)
        ));


        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 1.0f), new Vector2f(0.3f, 1.0f))
            .addControlPoint(new Vector2f(0.7f, 0.0f), new Vector2f(1.0f, 0.0f), null)
        ));

        emitter.setStartSpeed(new ValueType(6.5f));
        emitter.setLifeMinMax(new ValueType(0.5f), new ValueType(1.0f));
        emitter.setEmissionsPerSecond(30);
        emitter.setParticlesPerEmission(4);
        emitter.setShape(new EmitterLine(3));
        emitter.setStartRotation(new VectorValueType(new Vector3f(0, 0, -FastMath.PI), new Vector3f(0, 0, FastMath.PI)));

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }


    private void addMeshEmitterTest() {
        Spatial model = assetManager.loadModel("Models/Jaime/Jaime.j3o");
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100,
            new ColorInfluencer(),
            new SizeInfluencer()
        );

        emitter.getInfluencer(ColorInfluencer.class).setColorOverTime(new ColorValueType(
            new Gradient().addGradPoint(new ColorRGBA(0.8f, 0.6f, 0.0f, 0.6f), 0.0f)
                .addGradPoint(new ColorRGBA(0.8f, 0.0f, 0.0f, 0.5f), 0.8f)
                .addGradPoint(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.0f), 1.0f)
        ));


        emitter.getInfluencer(SizeInfluencer.class).setSizeOverTime(new ValueType(new Curve()
            .addControlPoint(null, new Vector2f(0.0f, 1.0f), new Vector2f(0.3f, 1.0f))
            .addControlPoint(new Vector2f(0.7f, 0.5f), new Vector2f(1.0f, 0.5f), null)
        ));

        emitter.setStartSize(new ValueType(0.1f));
        emitter.setStartSpeed(new ValueType(0.1f));
        emitter.setLifeMinMax(new ValueType(1.0f), new ValueType(1.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(4);
        model.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    emitter.setShape(new EmitterMesh(((Geometry)spatial).getMesh()));
                }
            }
        });

        model.rotate(0, FastMath.PI, 0);
        model.scale(3);
        emitter.scale(3);
        rootNode.attachChild(model);
        rootNode.attachChild(emitter);
    }


    private void addTweenEmitter() {
        emitter = new Emitter("test", getPartMat("Effects/Particles/part_light.png"), 100);

        emitter.setStartSize(new ValueType(0.2f));
        emitter.setStartSpeed(new ValueType(0.0f));
        emitter.setLifeMinMax(new ValueType(2.0f), new ValueType(2.0f));
        emitter.setEmissionsPerSecond(20);
        emitter.setParticlesPerEmission(1);
        emitter.setShape(new EmitterCone());
        ((EmitterCone)emitter.getShape()).setRadius(0.005f);

        tweenAmount = 0.0f;
        emitterTween = ParticleTweens.startSpeed(emitter, 1.0, 0.0f, 6.5f);

        emitter.setLocalTranslation(0, 0.5f, 0);
        rootNode.attachChild(emitter);
    }

    private int currentTest = 0;

    private String buildDisplayString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Current Test: ");

        switch (currentTest) {
            case 0: builder.append("Basic Emission"); break;
            case 1: builder.append("Gradient Test"); break;
            case 2: builder.append("Size Test"); break;
            case 3: builder.append("Physics Test"); break;
            case 4: builder.append("Particle Trail Test"); break;
            case 5: builder.append("Velocity Test"); break;
            case 6: builder.append("Orbital Velocity Test"); break;
            case 7: builder.append("Destination Test"); break;
            case 8: builder.append("Sprite Test"); break;
            case 9: builder.append("Mesh Emitter Test"); break;
            case 10: builder.append("Tween Test"); break;
            default: builder.append("Default"); break;
        }

        builder.append("\n\nControls:\n");
        builder.append("1 - Previous Test\n");
        builder.append("2 - Next Test\n");
        builder.append("3 - Enable/Disable Debug\n");

        if (currentTest == 10) {
            builder.append("4 - Decrease Tween Value\n");
            builder.append("5 - Increase Tween Value\n");
            builder.append("Tween Amount: ");
            builder.append(tweenAmount);
        }
//        builder.append("W - Forward\n");
//        builder.append("S - Backwards\n");
//        builder.append("A - Strafe Left\n");
//        builder.append("D - Strafe Right\n");

        return builder.toString();
    }

    private void setTest() {
        rootNode.detachAllChildren();
        addFloor();
        tweenAmount = 0.0f;
        emitterTween = null;

        if (currentTest < 0) currentTest = 10;
        if (currentTest > 10) currentTest = 0;

        if (currentTest == 0) {
            addSimpleEmitter();
        }
        else if (currentTest == 1) {
            addGradientEmitter();
        }
        else if (currentTest == 2) {
            addSizeEmitter();
        }
        else if (currentTest == 3) {
            addSimplePhysics();
        }
        else if (currentTest == 4) {
            addTrailEmitter();
        }
        else if (currentTest == 5) {
            addVelocityEmitter();
        }
        else if (currentTest == 6) {
            addOrbitalVelocityEmitter();
        }
        else if (currentTest == 7) {
            addDestinationEmitter();
        }
        else if (currentTest == 8) {
            addSpriteEmitter();
        }
        else if (currentTest == 9) {
            addMeshEmitterTest();
        }
        else if (currentTest == 10) {
            addTweenEmitter();
        }

        if (debug) {
            emitter.setDebug(assetManager, debug, debug);
        }

        hudText.setText(buildDisplayString());
    }

    @Override
    public void simpleInitApp() {

        hudText = new BitmapText(guiFont, false);
        hudText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
        hudText.setColor(ColorRGBA.White);                             // font color
        //hudText.setText(buildDisplayString());             // the text
        hudText.setLocalTranslation(10, settings.getHeight() - hudText.getLineHeight(), 0); // position
        guiNode.attachChild(hudText);


        getCamera().setLocation(new Vector3f(10, 10, -10));
        getCamera().lookAt(new Vector3f(), new Vector3f(0, 1, 0));
        getFlyByCamera().setMoveSpeed(15.0f);
        getFlyByCamera().setDragToRotate(true);

        setTest();

        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("next".equals(name) && !isPressed) {
                    currentTest += 1;
                    setTest();
                }
            }
        }, "next");
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("prev".equals(name) && !isPressed) {
                    currentTest -= 1;
                    setTest();
                }
            }
        }, "prev");
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("debug".equals(name) && !isPressed) {
                    debug = !debug;
                    emitter.setDebug(assetManager, debug, debug);
                }
            }
        }, "debug");
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("tweenIncrease".equals(name) && !isPressed && emitterTween != null) {
                    tweenAmount += 0.05f;
                    if (tweenAmount > 1) tweenAmount = 1;
                    emitterTween.interpolate(tweenAmount);
                    hudText.setText(buildDisplayString());
                }
            }
        }, "tweenIncrease");
        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if ("tweenDecrease".equals(name) && !isPressed && emitterTween != null) {
                    tweenAmount -= 0.05f;
                    if (tweenAmount < 0) tweenAmount = 0.0f;
                    emitterTween.interpolate(tweenAmount);
                    hudText.setText(buildDisplayString());

                }
            }
        }, "tweenDecrease");
//
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("next", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("prev", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("tweenIncrease", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("tweenDecrease", new KeyTrigger(KeyInput.KEY_5));

    }

}
