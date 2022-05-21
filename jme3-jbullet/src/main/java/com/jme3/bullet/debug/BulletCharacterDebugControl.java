/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * A physics-debug control used to visualize a PhysicsCharacter.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class BulletCharacterDebugControl extends AbstractPhysicsDebugControl {

    /**
     * character to visualize (not null)
     */
    protected final PhysicsCharacter body;
    /**
     * temporary storage for physics location
     */
    protected final Vector3f location = new Vector3f();
    protected final Quaternion rotation = new Quaternion();
    /**
     * shape for which geom was generated
     */
    protected CollisionShape myShape;
    /**
     * geometry to visualize myShape (not null)
     */
    protected Spatial geom;
    /**
     * physics scale for which geom was generated
     */
    final private Vector3f oldScale = new Vector3f();

    /**
     * Instantiate an enabled control to visualize the specified character.
     *
     * @param debugAppState which app state (not null, alias created)
     * @param body the character to visualize (not null, alias created)
     */

    public BulletCharacterDebugControl(BulletDebugAppState debugAppState, PhysicsCharacter body) {
        super(debugAppState);
        this.body = body;
        myShape = body.getCollisionShape();
        oldScale.set(myShape.getScale());

        this.geom = DebugShapeFactory.getDebugShape(myShape);
        this.geom.setName(body.toString());
        geom.setMaterial(debugAppState.DEBUG_PINK);
    }

    /**
     * Alter which spatial is controlled. Invoked when the control is added to
     * or removed from a spatial. Should be invoked only by a subclass or from
     * Spatial. Do not invoke directly from user code.
     *
     * @param spatial the spatial to control (or null)
     */
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

    /**
     * Update this control. Invoked once per frame during the logical-state
     * update, provided the control is enabled and added to a scene. Should be
     * invoked only by a subclass or by AbstractControl.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float tpf) {
        CollisionShape newShape = body.getCollisionShape();
        Vector3f newScale = newShape.getScale();
        if (myShape != newShape || !oldScale.equals(newScale)) {
            myShape = newShape;
            oldScale.set(newScale);

            Node node = (Node) spatial;
            node.detachChild(geom);

            geom = DebugShapeFactory.getDebugShape(myShape);
            geom.setName(body.toString());

            node.attachChild(geom);
        }
        geom.setMaterial(debugAppState.DEBUG_PINK);

        body.getPhysicsLocation(location);
        applyPhysicsTransform(location, Quaternion.IDENTITY);
    }

    /**
     * Render this control. Invoked once per frame, provided the
     * control is enabled and added to a scene. Should be invoked only by a
     * subclass or by AbstractControl.
     *
     * @param rm the render manager (not null)
     * @param vp the view port to render (not null)
     */
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }
}
