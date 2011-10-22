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
import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 * @deprecated use spatial animation instead.
 */
@Deprecated
public class RotationTrack extends AbstractCinematicEvent {

    private static final Logger log = Logger.getLogger(RotationTrack.class.getName());
    private Quaternion startRotation = new Quaternion();
    private Quaternion endRotation = new Quaternion();
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

    public RotationTrack(Spatial spatial, Quaternion endRotation) {
        this.endRotation.set(endRotation);
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, Quaternion endRotation, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.endRotation.set(endRotation);
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, Quaternion endRotation, LoopMode loopMode) {
        super(loopMode);
        this.endRotation.set(endRotation);
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public RotationTrack(Spatial spatial, Quaternion endRotation, float initialDuration) {
        super(initialDuration);
        this.endRotation.set(endRotation);
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    @Override
    public void onPlay() {
        if (playState != playState.Paused) {
            startRotation.set(spatial.getWorldRotation());
        }
        if (initialDuration == 0 && spatial != null) {
            spatial.setLocalRotation(endRotation);
            stop();
        }
    }

    @Override
    public void onUpdate(float tpf) {
        if (spatial != null) {
            value = Math.min(time / initialDuration, 1.0f);         
            TempVars vars = TempVars.get();
            Quaternion q = vars.quat1;
            q.set(startRotation).slerp(endRotation, value);

            spatial.setLocalRotation(q);
            vars.release();
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
        endRotation = (Quaternion) ic.readSavable("endRotation", null);
    }
}
