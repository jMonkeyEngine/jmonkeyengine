/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.animation;

import com.jme3.effect.ParticleEmitter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EffectTrack is a track to add to an existing animation, to emit particles
 * during animations for example : exhausts, dust raised by foot steps, shock
 * waves, lightnings etc...
 *
 * usage is
 * <pre>
 * AnimControl control model.getControl(AnimControl.class);
 * EffectTrack track = new EffectTrack(existingEmmitter, control.getAnim("TheAnim").getLength());
 * control.getAnim("TheAnim").addTrack(track);
 * </pre>
 *
 * if the emitter has emits 0 particles per seconds emmitAllPArticles will be
 * called on it at time 0 + startOffset. if it he it has more it will start
 * emit normally at time 0 + startOffset.
 *
 *
 * @author Nehon
 */
public class EffectTrack implements ClonableTrack {

    private static final Logger logger = Logger.getLogger(EffectTrack.class.getName());
    private ParticleEmitter emitter;
    private float startOffset = 0;
    private float particlesPerSeconds = 0;
    private float length = 0;
    private boolean emitted = false;
    private boolean initialized = false;
    //control responsible for disable and cull the emitter once all particles are gone
    private KillParticleControl killParticles = new KillParticleControl();

    public static class KillParticleControl extends AbstractControl {

        ParticleEmitter emitter;
        boolean stopRequested = false;
        boolean remove = false;

        public KillParticleControl() {
        }

        @Override
        public void setSpatial(Spatial spatial) {
            super.setSpatial(spatial);
            if (spatial != null) {
                if (spatial instanceof ParticleEmitter) {
                    emitter = (ParticleEmitter) spatial;
                } else {
                    throw new IllegalArgumentException("KillParticleEmitter can only ba attached to ParticleEmitter");
                }
            }


        }

        @Override
        protected void controlUpdate(float tpf) {
            if (remove) {
                emitter.removeControl(this);
                return;
            }
            if (emitter.getNumVisibleParticles() == 0) {
                emitter.setCullHint(CullHint.Always);
                emitter.setEnabled(false);
                emitter.removeControl(this);
                stopRequested = false;
            }
        }

        @Override
        public Object jmeClone() {
            KillParticleControl c = new KillParticleControl();
            //this control should be removed as it shouldn't have been persisted in the first place
            //In the quest to find the less hackish solution to achieve this,
            //making it remove itself from the spatial in the first update loop when loaded was the less bad.
            c.remove = true;
            c.spatial = spatial;
            return c;
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }

        @Override
        public Control cloneForSpatial(Spatial spatial) {

            KillParticleControl c = new KillParticleControl();
            //this control should be removed as it shouldn't have been persisted in the first place
            //In the quest to find the less hackish solution to achieve this,
            //making it remove itself from the spatial in the first update loop when loaded was the less bad.
            c.remove = true;
            c.setSpatial(spatial);
            return c;

        }
    };

    //Anim listener that stops the Emmitter when the animation is finished or changed.
    private class OnEndListener implements AnimEventListener {

        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
            stop();
        }

        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        }
    }

    /**
     * default constructor only for serialization
     */
    public EffectTrack() {
    }

    /**
     * Creates and EffectTrack
     *
     * @param emitter the emitter of the track
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     */
    public EffectTrack(ParticleEmitter emitter, float length) {
        this.emitter = emitter;
        //saving particles per second value
        this.particlesPerSeconds = emitter.getParticlesPerSec();
        //setting the emmitter to not emmit.
        this.emitter.setParticlesPerSec(0);
        this.length = length;
        //Marking the emitter with a reference to this track for further use in deserialization.
        setUserData(this);

    }

    /**
     * Creates and EffectTrack
     *
     * @param emitter the emitter of the track
     * @param length the length of the track (usually the length of the
     * animation you want to add the track to)
     * @param startOffset the time in second when the emitter will be triggered
     * after the animation starts (default is 0)
     */
    public EffectTrack(ParticleEmitter emitter, float length, float startOffset) {
        this(emitter, length);
        this.startOffset = startOffset;
    }

    /**
     * Internal use only
     *
     * @see Track#setTime(float, float, com.jme3.animation.AnimControl,
     * com.jme3.animation.AnimChannel, com.jme3.util.TempVars)
     */
    public void setTime(float time, float weight, AnimControl control, AnimChannel channel, TempVars vars) {

        if (time >= length) {
            return;
        }
        //first time adding the Animation listener to stop the track at the end of the animation
        if (!initialized) {
            control.addListener(new OnEndListener());
            initialized = true;
        }
        //checking fo time to trigger the effect
        if (!emitted && time >= startOffset) {
            emitted = true;
            emitter.setCullHint(CullHint.Dynamic);
            emitter.setEnabled(true);
            //if the emitter has 0 particles per seconds emmit all particles in one shot
            if (particlesPerSeconds == 0) {
                emitter.emitAllParticles();
                if (!killParticles.stopRequested) {
                    emitter.addControl(killParticles);
                    killParticles.stopRequested = true;
                }
            } else {
                //else reset its former particlePerSec value to let it emmit.
                emitter.setParticlesPerSec(particlesPerSeconds);
            }
        }
    }

    //stops the emmiter to emit.
    private void stop() {
        emitter.setParticlesPerSec(0);
        emitted = false;
        if (!killParticles.stopRequested) {
            emitter.addControl(killParticles);
            killParticles.stopRequested = true;
        }

    }

    /**
     * Return the length of the track
     *
     * @return length of the track
     */
    public float getLength() {
        return length;
    }

    @Override
    public float[] getKeyFrameTimes() {
        return new float[] { startOffset };
    }

    /**
     * Clone this track
     *
     * @return
     */
    @Override
    public Track clone() {
        return new EffectTrack(emitter, length, startOffset);
    }

    /**
     * This method clone the Track and search for the cloned counterpart of the
     * original emitter in the given cloned spatial. The spatial is assumed to
     * be the Spatial holding the AnimControl controlling the animation using
     * this Track.
     *
     * @param spatial the Spatial holding the AnimControl
     * @return the cloned Track with proper reference
     */
    @Override
    public Track cloneForSpatial(Spatial spatial) {
        EffectTrack effectTrack = new EffectTrack();
        effectTrack.particlesPerSeconds = this.particlesPerSeconds;
        effectTrack.length = this.length;
        effectTrack.startOffset = this.startOffset;

        //searching for the newly cloned ParticleEmitter
        effectTrack.emitter = findEmitter(spatial);
        if (effectTrack.emitter == null) {
            logger.log(Level.WARNING, "{0} was not found in {1} or is not bound to this track", new Object[]{emitter.getName(), spatial.getName()});
            effectTrack.emitter = emitter;
        }

        removeUserData(this);
        //setting user data on the new emmitter and marking it with a reference to the cloned Track.
        setUserData(effectTrack);
        effectTrack.emitter.setParticlesPerSec(0);
        return effectTrack;
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch( CloneNotSupportedException e ) {
            throw new RuntimeException("Error cloning", e);
        }
    }


    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        this.emitter = cloner.clone(emitter);
    }

    /**
     * recursive function responsible for finding the newly cloned Emitter
     *
     * @param spat
     * @return
     */
    private ParticleEmitter findEmitter(Spatial spat) {
        if (spat instanceof ParticleEmitter) {
            //spat is a PArticleEmitter
            ParticleEmitter em = (ParticleEmitter) spat;
            //getting the UserData TrackInfo so check if it should be attached to this Track
            TrackInfo t = (TrackInfo) em.getUserData("TrackInfo");
            if (t != null && t.getTracks().contains(this)) {
                return em;
            }
            return null;

        } else if (spat instanceof Node) {
            for (Spatial child : ((Node) spat).getChildren()) {
                ParticleEmitter em = findEmitter(child);
                if (em != null) {
                    return em;
                }
            }
        }
        return null;
    }

    public void cleanUp() {
        TrackInfo t = (TrackInfo) emitter.getUserData("TrackInfo");
        t.getTracks().remove(this);
        if (t.getTracks().isEmpty()) {
            emitter.setUserData("TrackInfo", null);
        }
    }

    /**
     *
     * @return the emitter used by this track
     */
    public ParticleEmitter getEmitter() {
        return emitter;
    }

    /**
     * Sets the Emitter to use in this track
     *
     * @param emitter
     */
    public void setEmitter(ParticleEmitter emitter) {
        if (this.emitter != null) {
            TrackInfo data = (TrackInfo) emitter.getUserData("TrackInfo");
            data.getTracks().remove(this);
        }
        this.emitter = emitter;
        //saving particles per second value
        this.particlesPerSeconds = emitter.getParticlesPerSec();
        //setting the emmitter to not emmit.
        this.emitter.setParticlesPerSec(0);
        setUserData(this);
    }

    /**
     *
     * @return the start offset of the track
     */
    public float getStartOffset() {
        return startOffset;
    }

    /**
     * set the start offset of the track
     *
     * @param startOffset
     */
    public void setStartOffset(float startOffset) {
        this.startOffset = startOffset;
    }

    private void setUserData(EffectTrack effectTrack) {
        //fetching the UserData TrackInfo.
        TrackInfo data = (TrackInfo) effectTrack.emitter.getUserData("TrackInfo");

        //if it does not exist, we create it and attach it to the emitter.
        if (data == null) {
            data = new TrackInfo();
            effectTrack.emitter.setUserData("TrackInfo", data);
        }

        //adding the given Track to the TrackInfo.
        data.addTrack(effectTrack);


    }

    private void removeUserData(EffectTrack effectTrack) {
        //fetching the UserData TrackInfo.
        TrackInfo data = (TrackInfo) effectTrack.emitter.getUserData("TrackInfo");

        //if it does not exist, we create it and attach it to the emitter.
        if (data == null) {
            return;
        }

        //removing the given Track to the TrackInfo.
        data.getTracks().remove(effectTrack);


    }

    /**
     * Internal use only serialization
     *
     * @param ex exporter
     * @throws IOException exception
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        //reseting the particle emission rate on the emitter before saving.
        emitter.setParticlesPerSec(particlesPerSeconds);
        out.write(emitter, "emitter", null);
        out.write(particlesPerSeconds, "particlesPerSeconds", 0);
        out.write(length, "length", 0);
        out.write(startOffset, "startOffset", 0);
        //Setting emission rate to 0 so that this track can go on being used.
        emitter.setParticlesPerSec(0);
    }

    /**
     * Internal use only serialization
     *
     * @param im importer
     * @throws IOException Exception
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        this.particlesPerSeconds = in.readFloat("particlesPerSeconds", 0);
        //reading the emitter even if the track will then reference its cloned counter part if it's loaded with the assetManager.
        //This also avoid null pointer exception if the model is not loaded via the AssetManager.
        emitter = (ParticleEmitter) in.readSavable("emitter", null);
        emitter.setParticlesPerSec(0);
        //if the emitter was saved with a KillParticleControl we remove it.
//        Control c = emitter.getControl(KillParticleControl.class);
//        if(c!=null){
//            emitter.removeControl(c);
//        }
        //emitter.removeControl(KillParticleControl.class);
        length = in.readFloat("length", length);
        startOffset = in.readFloat("startOffset", 0);
    }
}
