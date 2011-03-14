/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.cinematic.events;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public class RotationTrack extends AbstractCinematicEvent {

    private static final Logger log = Logger.getLogger(RotationTrack.class.getName());
    private float[] startRotation;
    private float[] endRotation;
    private Spatial spatial;
    private String spatialName = "";
    private float value = 0;

    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        super.initEvent(app, cinematic);
        if (spatial == null) {
            spatial = cinematic.getScene().getChild(spatialName);
            if (spatial == null) {
            } else {
                log.log(Level.WARNING, "spatial {0} not found in the scene", spatialName);
            }
        }
    }

    public RotationTrack() {
    }

    public RotationTrack(Spatial spatial, float[] endRotation) {
        this.endRotation = endRotation;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, float[] endRotation, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.endRotation = endRotation;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, float[] endRotation, LoopMode loopMode) {
        super(loopMode);
        this.endRotation = endRotation;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, float[] endRotation, float initialDuration) {
        super(initialDuration);
        this.endRotation = endRotation;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    @Override
    public void onPlay() {
        if (playState != playState.Paused) {
            startRotation = spatial.getWorldRotation().toAngles(null);
        }
        if (duration == 0 && spatial != null) {
            spatial.setLocalRotation(new Quaternion().fromAngles(endRotation));
            stop();
        }
    }

    @Override
    public void onUpdate(float tpf) {
        if (spatial != null) {
            value += Math.min(tpf * speed / duration, 1.0f);
            float[] rot = new float[3];
            rot[0] = FastMath.interpolateLinear(value, startRotation[0], endRotation[0]);
            rot[1] = FastMath.interpolateLinear(value, startRotation[1], endRotation[1]);
            rot[2] = FastMath.interpolateLinear(value, startRotation[2], endRotation[2]);
            spatial.setLocalRotation(new Quaternion().fromAngles(rot));
        }
    }

    @Override
    public void onStop() {
        value = 0;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(spatialName, "spatialName", "");
        oc.write(endRotation, "endRotation", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        spatialName = ic.readString("spatialName", "");
        endRotation = ic.readFloatArray("endRotation", null);
    }
}
