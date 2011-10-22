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
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 * @deprecated use spatial animation instead.
 */
@Deprecated
public class PositionTrack extends AbstractCinematicEvent {

    private static final Logger log = Logger.getLogger(PositionTrack.class.getName());
    private Vector3f startPosition;
    private Vector3f endPosition;
    private Spatial spatial;
    private String spatialName = "";
    private float value = 0;

    public PositionTrack() {
    }

    public PositionTrack(Spatial spatial, Vector3f endPosition) {
        this.endPosition = endPosition;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

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

    public PositionTrack(Spatial spatial, Vector3f endPosition, float initialDuration, LoopMode loopMode) {
        super(initialDuration, loopMode);
        this.endPosition = endPosition;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public PositionTrack(Spatial spatial, Vector3f endPosition, LoopMode loopMode) {
        super(loopMode);
        this.endPosition = endPosition;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    public PositionTrack(Spatial spatial, Vector3f endPosition, float initialDuration) {
        super(initialDuration);
        this.endPosition = endPosition;
        this.spatial = spatial;
        spatialName = spatial.getName();
    }

    @Override
    public void onPlay() {
        if (playState != playState.Paused) {
            startPosition = spatial.getWorldTranslation().clone();
        }
        if (initialDuration == 0 && spatial != null) {

            spatial.setLocalTranslation(endPosition);
        }
    }

    @Override
    public void onUpdate(float tpf) {
        if (spatial != null) {
            value = Math.min(time / initialDuration, 1.0f);
            Vector3f pos = FastMath.interpolateLinear(value, startPosition, endPosition);
            spatial.setLocalTranslation(pos);
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
        oc.write(endPosition, "endPosition", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        spatialName = ic.readString("spatialName", "");
        endPosition = (Vector3f) ic.readSavable("endPosition", null);
    }
}
