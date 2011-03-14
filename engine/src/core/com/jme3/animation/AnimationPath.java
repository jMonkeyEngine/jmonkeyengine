/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * 
 * @deprecated use {@link MotionPath}
 */
@Deprecated
public class AnimationPath extends AbstractControl {

    private boolean playing = false;
    private int currentWayPoint;
    private float currentValue;
    private List<Vector3f> wayPoints = new ArrayList<Vector3f>();
    private Node debugNode;
    private AssetManager assetManager;
    private List<AnimationPathListener> listeners;
    private Vector3f curveDirection;
    private Vector3f lookAt;
    private Vector3f upVector;
    private Quaternion rotation;
    private float duration = 5f;
    private List<Float> segmentsLength;
    private float totalLength;
    private List<Vector3f> CRcontrolPoints;
    private float speed;
    private float curveTension = 0.5f;
    private boolean loop = false;
    private boolean cycle = false;

    @Override
    protected void controlUpdate(float tpf) {
       
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }

    /**
     * Enum for the different type of target direction behavior
     */
    @Deprecated
    public enum Direction {

        /**
         * the target stay in the starting direction
         */
        None,
        /**
         * The target rotates with the direction of the path
         */
        Path,
        /**
         * The target rotates with the direction of the path but with the additon of a rtotation
         * you need to use the setRotation mathod when using this Direction
         */
        PathAndRotation,
        /**
         * The target rotates with the given rotation
         */
        Rotation,
        /**
         * The target looks at a point
         * You need to use the setLookAt method when using this direction
         */
        LookAt
    }
    private Direction directionType = Direction.None;

    @Deprecated
    public enum PathInterpolation {

        /**
         * Compute a linear path between the waypoints
         */
        Linear,
        /**
         * Compute a Catmull-Rom spline path between the waypoints
         * see http://www.mvps.org/directx/articles/catmull/
         */
        CatmullRom
    }
    private PathInterpolation pathInterpolation = PathInterpolation.CatmullRom;

    /**
     * Create an animation Path for this target
     * @param target
     */
    @Deprecated
    public AnimationPath(Spatial target) {
        super();
        this.spatial = target;
        target.addControl(this);
    }

    /**
     * don't use this contructor use AnimationPath(Spatial target)
     */
    @Deprecated
    public AnimationPath() {
        super();
    }

    @Deprecated
    public Control cloneForSpatial(Spatial spatial) {
        AnimationPath path = new AnimationPath(spatial);
        for (Iterator<Vector3f> it = wayPoints.iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            path.addWayPoint(vector3f);
        }
        for (Iterator<AnimationPathListener> it = listeners.iterator(); it.hasNext();) {
            AnimationPathListener animationPathListener = it.next();
            path.addListener(animationPathListener);
        }
        return path;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
    float eps = 0.0001f;

    /**
     * Cal each update, don't call this method, it's for internal use only
     * @param tpf
     */
    @Override
    public void update(float tpf) {

        if (enabled) {
            if (playing) {
                spatial.setLocalTranslation(interpolatePath(tpf));
                computeTargetDirection();

                if (currentValue >= 1.0f) {
                    currentValue = 0;
                    currentWayPoint++;
                    triggerWayPointReach(currentWayPoint);
                }
                if (currentWayPoint == wayPoints.size() - 1) {
                    if (loop) {
                        currentWayPoint = 0;
                    } else {
                        stop();
                    }
                }
            }
        }
    }

    private Vector3f interpolatePath(float tpf) {
        Vector3f temp = null;
        float val;
        switch (pathInterpolation) {
            case CatmullRom:

                val = tpf * speed;
                currentValue += eps;
                temp = FastMath.interpolateCatmullRom(currentValue, curveTension, CRcontrolPoints.get(currentWayPoint), CRcontrolPoints.get(currentWayPoint + 1), CRcontrolPoints.get(currentWayPoint + 2), CRcontrolPoints.get(currentWayPoint + 3));
                float dist = temp.subtract(spatial.getLocalTranslation()).length();

                while (dist < val) {
                    currentValue += eps;
                    temp = FastMath.interpolateCatmullRom(currentValue, curveTension, CRcontrolPoints.get(currentWayPoint), CRcontrolPoints.get(currentWayPoint + 1), CRcontrolPoints.get(currentWayPoint + 2), CRcontrolPoints.get(currentWayPoint + 3));
                    dist = temp.subtract(spatial.getLocalTranslation()).length();
                }
                if (directionType == Direction.Path || directionType == Direction.PathAndRotation) {
                    curveDirection = temp.subtract(spatial.getLocalTranslation()).normalizeLocal();
                }
                break;
            case Linear:
                val = duration * segmentsLength.get(currentWayPoint) / totalLength;
                currentValue = Math.min(currentValue + tpf / val, 1.0f);
                temp = FastMath.interpolateLinear(currentValue, wayPoints.get(currentWayPoint), wayPoints.get(currentWayPoint + 1));
                curveDirection = wayPoints.get(currentWayPoint + 1).subtract(wayPoints.get(currentWayPoint)).normalizeLocal();
                break;
            default:
                break;
        }
        return temp;
    }

    private void computeTargetDirection() {
        switch (directionType) {
            case Path:
                Quaternion q = new Quaternion();
                q.lookAt(curveDirection, Vector3f.UNIT_Y);
                spatial.setLocalRotation(q);
                break;
            case LookAt:
                if (lookAt != null) {
                    spatial.lookAt(lookAt, upVector);
                }
                break;
            case PathAndRotation:
                if (rotation != null) {
                    Quaternion q2 = new Quaternion();
                    q2.lookAt(curveDirection, Vector3f.UNIT_Y);
                    q2.multLocal(rotation);
                    spatial.setLocalRotation(q2);
                }
                break;
            case Rotation:
                if (rotation != null) {
                    spatial.setLocalRotation(rotation);
                }
                break;
            case None:
                break;
            default:
                break;
        }
    }

    private void attachDebugNode(Node root) {
        if (debugNode == null) {
            debugNode = new Node("AnimationPathFor" + spatial.getName());
            Material m = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
            for (Iterator<Vector3f> it = wayPoints.iterator(); it.hasNext();) {
                Vector3f cp = it.next();
                Geometry geo = new Geometry("box", new Box(cp, 0.3f, 0.3f, 0.3f));
                geo.setMaterial(m);
                debugNode.attachChild(geo);

            }
            switch (pathInterpolation) {
                case CatmullRom:
                    debugNode.attachChild(CreateCatmullRomPath());
                    break;
                case Linear:
                    debugNode.attachChild(CreateLinearPath());
                    break;
                default:
                    debugNode.attachChild(CreateLinearPath());
                    break;
            }

            root.attachChild(debugNode);
        }
    }

    private Geometry CreateLinearPath() {

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Blue);

        float[] array = new float[wayPoints.size() * 3];
        short[] indices = new short[(wayPoints.size() - 1) * 2];
        int i = 0;
        int cpt = 0;
        int k = 0;
        int j = 0;
        for (Iterator<Vector3f> it = wayPoints.iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            array[i] = vector3f.x;
            i++;
            array[i] = vector3f.y;
            i++;
            array[i] = vector3f.z;
            i++;
            if (it.hasNext()) {
                k = j;
                indices[cpt] = (short) k;
                cpt++;
                k++;
                indices[cpt] = (short) k;
                cpt++;
                j++;
            }
        }

        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, array);
        lineMesh.setBuffer(VertexBuffer.Type.Index, (wayPoints.size() - 1) * 2, indices);
        lineMesh.updateBound();
        lineMesh.updateCounts();

        Geometry lineGeometry = new Geometry("line", lineMesh);
        lineGeometry.setMaterial(mat);
        return lineGeometry;
    }

    private Geometry CreateCatmullRomPath() {

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/WireColor.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        int nbSubSegments = 10;

        float[] array = new float[(((wayPoints.size() - 1) * nbSubSegments) + 1) * 3];
        short[] indices = new short[((wayPoints.size() - 1) * nbSubSegments) * 2];
        int i = 0;
        int cptCP = 0;
        for (Iterator<Vector3f> it = wayPoints.iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            array[i] = vector3f.x;
            i++;
            array[i] = vector3f.y;
            i++;
            array[i] = vector3f.z;
            i++;
            if (it.hasNext()) {
                for (int j = 1; j < nbSubSegments; j++) {
                    Vector3f temp = FastMath.interpolateCatmullRom((float) j / nbSubSegments, curveTension, CRcontrolPoints.get(cptCP),
                            CRcontrolPoints.get(cptCP + 1), CRcontrolPoints.get(cptCP + 2), CRcontrolPoints.get(cptCP + 3));
                    array[i] = temp.x;
                    i++;
                    array[i] = temp.y;
                    i++;
                    array[i] = temp.z;
                    i++;
                }
            }
            cptCP++;
        }

        i = 0;
        int k = 0;
        for (int j = 0; j < ((wayPoints.size() - 1) * nbSubSegments); j++) {
            k = j;
            indices[i] = (short) k;
            i++;
            k++;
            indices[i] = (short) k;
            i++;
        }



        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, array);
        lineMesh.setBuffer(VertexBuffer.Type.Index, ((wayPoints.size() - 1) * nbSubSegments) * 2, indices);
        lineMesh.updateBound();
        lineMesh.updateCounts();

        Geometry lineGeometry = new Geometry("line", lineMesh);
        lineGeometry.setMaterial(mat);
        return lineGeometry;
    }

    private void initCatmullRomWayPoints(List<Vector3f> list) {
        if (CRcontrolPoints == null) {
            CRcontrolPoints = new ArrayList<Vector3f>();
        } else {
            CRcontrolPoints.clear();
        }
        int nb = list.size() - 1;

        if (cycle) {
            CRcontrolPoints.add(list.get(list.size() - 2));
        } else {
            CRcontrolPoints.add(list.get(0).subtract(list.get(1).subtract(list.get(0))));
        }

        for (Iterator<Vector3f> it = list.iterator(); it.hasNext();) {
            Vector3f vector3f = it.next();
            CRcontrolPoints.add(vector3f);
        }
        if (cycle) {
            CRcontrolPoints.add(list.get(1));
        } else {
            CRcontrolPoints.add(list.get(nb).add(list.get(nb).subtract(list.get(nb - 1))));
        }

    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        //nothing to render
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList((ArrayList) wayPoints, "wayPoints", null);
        oc.write(lookAt, "lookAt", Vector3f.ZERO);
        oc.write(upVector, "upVector", Vector3f.UNIT_Y);
        oc.write(rotation, "rotation", Quaternion.IDENTITY);
        oc.write(duration, "duration", 5f);
        oc.write(directionType, "directionType", Direction.None);
        oc.write(pathInterpolation, "pathInterpolation", PathInterpolation.CatmullRom);
        float list[]=new float[segmentsLength.size()];
        for (int i=0;i<segmentsLength.size();i++) {
            list[i]=segmentsLength.get(i);
        }
        oc.write(list, "segmentsLength", null);
      
        oc.write(totalLength, "totalLength", 0);
        oc.writeSavableArrayList((ArrayList) CRcontrolPoints, "CRControlPoints", null);
        oc.write(speed, "speed", 0);
        oc.write(curveTension, "curveTension", 0.5f);
        oc.write(cycle, "cycle", false);
        oc.write(loop, "loop", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);

        wayPoints = (ArrayList<Vector3f>) in.readSavableArrayList("wayPoints", null);
        lookAt = (Vector3f) in.readSavable("lookAt", Vector3f.ZERO);
        upVector = (Vector3f) in.readSavable("upVector", Vector3f.UNIT_Y);
        rotation = (Quaternion) in.readSavable("rotation", Quaternion.IDENTITY);
        duration = in.readFloat("duration", 5f);
        float list[]=in.readFloatArray("segmentsLength", null);
        if (list!=null){
            segmentsLength=new ArrayList<Float>();
            for (int i=0;i<list.length;i++) {
                segmentsLength.add(new Float(list[i]));
            }
        }
        directionType=in.readEnum("directionType",Direction.class, Direction.None);
        pathInterpolation= in.readEnum("pathInterpolation", PathInterpolation.class,PathInterpolation.CatmullRom);
        totalLength = in.readFloat("totalLength", 0);
        CRcontrolPoints = (ArrayList<Vector3f>) in.readSavableArrayList("CRControlPoints", null);
        speed = in.readFloat("speed", 0);
        curveTension = in.readFloat("curveTension", 0.5f);
        cycle = in.readBoolean("cycle", false);
        loop = in.readBoolean("loop", false);
    }

    /**
     * plays the animation
     */
    @Deprecated
    public void play() {
        playing = true;
    }

    /**
     * pauses the animation
     */
    @Deprecated
    public void pause() {
        playing = false;
    }

    /**
     * stops the animation, next time play() is called the animation will start from the begining.
     */
    @Deprecated
    public void stop() {
        playing = false;
        currentWayPoint = 0;
    }

    /**
     * Addsa waypoint to the path
     * @param wayPoint a position in world space
     */
    @Deprecated
    public void addWayPoint(Vector3f wayPoint) {
        if (wayPoints.size() > 2 && this.cycle) {
            wayPoints.remove(wayPoints.size() - 1);
        }
        wayPoints.add(wayPoint);
        if (wayPoints.size() >= 2 && this.cycle) {
            wayPoints.add(wayPoints.get(0));
        }
        if (wayPoints.size() > 1) {
            computeTotalLentgh();
        }
    }


    private void computeTotalLentgh() {
        totalLength = 0;
        float l = 0;
        if (segmentsLength == null) {
            segmentsLength = new ArrayList<Float>();
        } else {
            segmentsLength.clear();
        }
        if (pathInterpolation == PathInterpolation.Linear) {
            if (wayPoints.size() > 1) {
                for (int i = 0; i < wayPoints.size() - 1; i++) {
                    l = wayPoints.get(i + 1).subtract(wayPoints.get(i)).length();
                    segmentsLength.add(l);
                    totalLength += l;
                }
            }
        } else {
            initCatmullRomWayPoints(wayPoints);
            computeCatmulLength();
        }
    }

    private void computeCatmulLength() {
        float l = 0;
        if (wayPoints.size() > 1) {
            for (int i = 0; i < wayPoints.size() - 1; i++) {
                l = getCatmullRomP1toP2Length(CRcontrolPoints.get(i),
                        CRcontrolPoints.get(i + 1), CRcontrolPoints.get(i + 2), CRcontrolPoints.get(i + 3), 0, 1);
                segmentsLength.add(l);
                totalLength += l;
            }
        }
        speed = totalLength / duration;
    }

    /**
     * retruns the length of the path in world units
     * @return the length
     */
    @Deprecated
    public float getLength() {
        return totalLength;
    }
    //Compute lenght of p1 to p2 arc segment
    //TODO extract to FastMath class

    private float getCatmullRomP1toP2Length(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, float startRange, float endRange) {

        float epsilon = 0.001f;
        float middleValue = (startRange + endRange) * 0.5f;
        Vector3f start = p1;
        if (startRange != 0) {
            start = FastMath.interpolateCatmullRom(startRange, curveTension, p0, p1, p2, p3);
        }
        Vector3f end = p2;
        if (endRange != 1) {
            end = FastMath.interpolateCatmullRom(endRange, curveTension, p0, p1, p2, p3);
        }
        Vector3f middle = FastMath.interpolateCatmullRom(middleValue, curveTension, p0, p1, p2, p3);
        float l = end.subtract(start).length();
        float l1 = middle.subtract(start).length();
        float l2 = end.subtract(middle).length();
        float len = l1 + l2;
        if (l + epsilon < len) {
            l1 = getCatmullRomP1toP2Length(p0, p1, p2, p3, startRange, middleValue);
            l2 = getCatmullRomP1toP2Length(p0, p1, p2, p3, middleValue, endRange);
        }
        l = l1 + l2;
        return l;
    }

    /**
     * returns the waypoint at the given index
     * @param i the index
     * @return returns the waypoint position
     */
    @Deprecated
    public Vector3f getWayPoint(int i) {
        return wayPoints.get(i);
    }

    /**
     * remove the waypoint from the path
     * @param wayPoint the waypoint to remove
     */
    @Deprecated
    public void removeWayPoint(Vector3f wayPoint) {
        wayPoints.remove(wayPoint);
        if (wayPoints.size() > 1) {
            computeTotalLentgh();
        }
    }

    /**
     * remove the waypoint at the given index from the path
     * @param i the index of the waypoint to remove
     */
    @Deprecated
    public void removeWayPoint(int i) {
        removeWayPoint(wayPoints.get(i));
    }

    /**
     * returns an iterator on the waypoints collection
     * @return
     */
    @Deprecated
    public Iterator<Vector3f> iterator() {
        return wayPoints.iterator();
    }

    /**
     * return the type of path interpolation for this path
     * @return the path interpolation
     */
    @Deprecated
    public PathInterpolation getPathInterpolation() {
        return pathInterpolation;
    }

    /**
     * sets the path interpolation for this path
     * @param pathInterpolation
     */
    @Deprecated
    public void setPathInterpolation(PathInterpolation pathInterpolation) {
        this.pathInterpolation = pathInterpolation;
        computeTotalLentgh();
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }
    }

    /**
     * disable the display of the path and the waypoints
     */
    @Deprecated
    public void disableDebugShape() {

        debugNode.detachAllChildren();
        debugNode = null;
        assetManager = null;
    }

    /**
     * enable the display of the path and the waypoints
     * @param manager the assetManager
     * @param rootNode the node where the debug shapes must be attached
     */
    @Deprecated
    public void enableDebugShape(AssetManager manager, Node rootNode) {
        assetManager = manager;
        computeTotalLentgh();
        attachDebugNode(rootNode);
    }

    /**
     * Adds an animation pathListener to the path
     * @param listener the AnimationPathListener to attach
     */
    @Deprecated
    public void addListener(AnimationPathListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<AnimationPathListener>();
        }
        listeners.add(listener);
    }

    /**
     * remove the given listener
     * @param listener the listener to remove
     */
    @Deprecated
    public void removeListener(AnimationPathListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * return the number of waypoints of this path
     * @return
     */
    @Deprecated
    public int getNbWayPoints() {
        return wayPoints.size();
    }

    private void triggerWayPointReach(int wayPointIndex) {
        for (Iterator<AnimationPathListener> it = listeners.iterator(); it.hasNext();) {
            AnimationPathListener listener = it.next();
            listener.onWayPointReach(this, wayPointIndex);
        }
    }

    /**
     * returns the direction type of the target
     * @return the direction type
     */
    @Deprecated
    public Direction getDirectionType() {
        return directionType;
    }

    /**
     * Sets the direction type of the target
     * On each update the direction given to the target can have different behavior
     * See the Direction Enum for explanations
     * @param directionType the direction type
     */
    @Deprecated
    public void setDirectionType(Direction directionType) {
        this.directionType = directionType;
    }

    /**
     * Set the lookAt for the target
     * This can be used only if direction Type is Direction.LookAt
     * @param lookAt the position to look at
     * @param upVector the up vector
     */
    @Deprecated
    public void setLookAt(Vector3f lookAt, Vector3f upVector) {
        this.lookAt = lookAt;
        this.upVector = upVector;
    }

    /**
     * returns the rotation of the target
     * @return the rotation quaternion
     */
    @Deprecated
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * sets the rotation of the target
     * This can be used only if direction Type is Direction.PathAndRotation or Direction.Rotation
     * With PathAndRotation the target will face the direction of the path multiplied by the given Quaternion.
     * With Rotation the rotation of the target will be set with the given Quaternion.
     * @param rotation the rotation quaternion
     */
    @Deprecated
    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    @Deprecated
    public float getDuration() {
        return duration;
    }

    /**
     * Sets the duration of the animation
     * @param duration
     */
    @Deprecated
    public void setDuration(float duration) {
        this.duration = duration;
        speed = totalLength / duration;
    }

    @Deprecated
    public float getCurveTension() {
        return curveTension;
    }

    /**
     * sets the tension of the curve (only for catmull rom) 0.0 will give a linear curve, 1.0 a round curve
     * @param curveTension
     */
    @Deprecated
    public void setCurveTension(float curveTension) {
        this.curveTension = curveTension;
        computeTotalLentgh();
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }
    }

    /**
     * Sets the path to be a cycle
     * @param cycle
     */
    @Deprecated
    public void setCycle(boolean cycle) {

        if (wayPoints.size() >= 2) {
            if (this.cycle && !cycle) {
                wayPoints.remove(wayPoints.size() - 1);
            }
            if (!this.cycle && cycle) {
                wayPoints.add(wayPoints.get(0));
                System.out.println("adding first wp");
            }
            this.cycle = cycle;
            computeTotalLentgh();
            if (debugNode != null) {
                Node parent = debugNode.getParent();
                debugNode.removeFromParent();
                debugNode.detachAllChildren();
                debugNode = null;
                attachDebugNode(parent);
            }
        } else {
            this.cycle = cycle;
        }
    }

    @Deprecated
    public boolean isCycle() {
        return cycle;
    }

    /**
     * returs true is the animation loops
     * @return
     */
    @Deprecated
    public boolean isLoop() {
        return loop;
    }

    /**
     * Loops the animation
     * @param loop
     */
    @Deprecated
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    @Override
    public String toString() {
        return "AnimationPath{" + "playing=" + playing + "currentWayPoint=" + currentWayPoint + "currentValue=" + currentValue + "wayPoints=" + wayPoints + "debugNode=" + debugNode + "assetManager=" + assetManager + "listeners=" + listeners + "curveDirection=" + curveDirection + "lookAt=" + lookAt + "upVector=" + upVector + "rotation=" + rotation + "duration=" + duration + "segmentsLength=" + segmentsLength + "totalLength=" + totalLength + "CRcontrolPoints=" + CRcontrolPoints + "speed=" + speed + "curveTension=" + curveTension + "loop=" + loop + "cycle=" + cycle + "directionType=" + directionType + "pathInterpolation=" + pathInterpolation + "eps=" + eps + '}';
    }

    
}
