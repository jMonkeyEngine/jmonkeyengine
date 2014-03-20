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
package com.jme3.bullet.debug;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 *
 * @author normenhansen
 */
public class BulletGhostObjectDebugControl extends AbstractPhysicsDebugControl {

    protected final PhysicsGhostObject body;
    protected final Vector3f location = new Vector3f();
    protected final Quaternion rotation = new Quaternion();
    protected CollisionShape myShape;
    protected Spatial geom;

    public BulletGhostObjectDebugControl(BulletDebugAppState debugAppState, PhysicsGhostObject body) {
        super(debugAppState);
        this.body = body;
        myShape = body.getCollisionShape();
        this.geom = DebugShapeFactory.getDebugShape(body.getCollisionShape());
        this.geom.setName(body.toString());
        this.geom.setName(body.toString());
        geom.setMaterial(debugAppState.DEBUG_YELLOW);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial != null && spatial instanceof Node) {
            Node node = (Node) spatial;
            node.attachChild(geom);
        } else if (spatial == null && this.spatial != null) {
            Node node = (Node) this.spatial;
            node.detachChild(geom);
        }
        super.setSpatial(spatial);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (myShape != body.getCollisionShape()) {
            Node node = (Node) this.spatial;
            node.detachChild(geom);
            geom = DebugShapeFactory.getDebugShape(body.getCollisionShape());
            geom.setMaterial(debugAppState.DEBUG_YELLOW);
            node.attachChild(geom);
        }
        applyPhysicsTransform(body.getPhysicsLocation(location), body.getPhysicsRotation(rotation));
        geom.setLocalScale(body.getCollisionShape().getScale());
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
