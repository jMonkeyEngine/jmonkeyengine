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
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A CompoundCollisionShape allows combining multiple base shapes
 * to generate a more sophisticated shape.
 * @author normenhansen
 */
public class CompoundCollisionShape extends CollisionShape {

    protected ArrayList<ChildCollisionShape> children = new ArrayList<ChildCollisionShape>();

    public CompoundCollisionShape() {
        objectId = createShape();//new CompoundShape();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
    }

    /**
     * adds a child shape at the given local translation
     * @param shape the child shape to add
     * @param location the local location of the child shape
     */
    public void addChildShape(CollisionShape shape, Vector3f location) {
//        Transform transA = new Transform(Converter.convert(new Matrix3f()));
//        Converter.convert(location, transA.origin);
//        children.add(new ChildCollisionShape(location.clone(), new Matrix3f(), shape));
//        ((CompoundShape) objectId).addChildShape(transA, shape.getObjectId());
        addChildShape(shape, location, new Matrix3f());
    }

    /**
     * adds a child shape at the given local translation
     * @param shape the child shape to add
     * @param location the local location of the child shape
     */
    public void addChildShape(CollisionShape shape, Vector3f location, Matrix3f rotation) {
        if(shape instanceof CompoundCollisionShape){
            throw new IllegalStateException("CompoundCollisionShapes cannot have CompoundCollisionShapes as children!");
        }
//        Transform transA = new Transform(Converter.convert(rotation));
//        Converter.convert(location, transA.origin);
//        Converter.convert(rotation, transA.basis);
        children.add(new ChildCollisionShape(location.clone(), rotation.clone(), shape));
        addChildShape(objectId, shape.getObjectId(), location, rotation);
//        ((CompoundShape) objectId).addChildShape(transA, shape.getObjectId());
    }

    private void addChildShapeDirect(CollisionShape shape, Vector3f location, Matrix3f rotation) {
        if(shape instanceof CompoundCollisionShape){
            throw new IllegalStateException("CompoundCollisionShapes cannot have CompoundCollisionShapes as children!");
        }
//        Transform transA = new Transform(Converter.convert(rotation));
//        Converter.convert(location, transA.origin);
//        Converter.convert(rotation, transA.basis);
        addChildShape(objectId, shape.getObjectId(), location, rotation);
//        ((CompoundShape) objectId).addChildShape(transA, shape.getObjectId());
    }

    /**
     * removes a child shape
     * @param shape the child shape to remove
     */
    public void removeChildShape(CollisionShape shape) {
        removeChildShape(objectId, shape.getObjectId());
//        ((CompoundShape) objectId).removeChildShape(shape.getObjectId());
        for (Iterator<ChildCollisionShape> it = children.iterator(); it.hasNext();) {
            ChildCollisionShape childCollisionShape = it.next();
            if (childCollisionShape.shape == shape) {
                it.remove();
            }
        }
    }

    public List<ChildCollisionShape> getChildren() {
        return children;
    }

    private native long createShape();
    
    private native long addChildShape(long objectId, long childId, Vector3f location, Matrix3f rotation);
    
    private native long removeChildShape(long objectId, long childId);
    
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.writeSavableArrayList(children, "children", new ArrayList<ChildCollisionShape>());
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        children = capsule.readSavableArrayList("children", new ArrayList<ChildCollisionShape>());
        setScale(scale);
        setMargin(margin);
        loadChildren();
    }

    private void loadChildren() {
        for (Iterator<ChildCollisionShape> it = children.iterator(); it.hasNext();) {
            ChildCollisionShape child = it.next();
            addChildShapeDirect(child.shape, child.location, child.rotation);
        }
    }

}
