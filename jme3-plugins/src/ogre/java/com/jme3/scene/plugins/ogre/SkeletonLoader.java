/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.ogre;

import com.jme3.anim.*;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.xml.SAXUtil;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class SkeletonLoader extends DefaultHandler implements AssetLoader {

    private static final Logger logger = Logger.getLogger(SceneLoader.class.getName());
    //private AssetManager assetManager;
    private Stack<String> elementStack = new Stack<>();
    private HashMap<Integer, Joint> indexToJoint = new HashMap<>();
    private HashMap<String, Joint> nameToJoint = new HashMap<>();
    private TransformTrack track;
    private ArrayList<TransformTrack> tracks = new ArrayList<>();
    private AnimClip animClip;
    private ArrayList<AnimClip> animClips;
    private Joint joint;
    private Armature armature;
    private ArrayList<Float> times = new ArrayList<>();
    private ArrayList<Vector3f> translations = new ArrayList<>();
    private ArrayList<Quaternion> rotations = new ArrayList<>();
    private ArrayList<Vector3f> scales = new ArrayList<>();
    private float time = -1;
    private Vector3f position;
    private Quaternion rotation;
    private Vector3f scale;
    private float angle;
    private Vector3f axis;
    private List<Joint> unusedJoints = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attribs) throws SAXException {
        if (qName.equals("position") || qName.equals("translate")) {
            position = SAXUtil.parseVector3(attribs);
        } else if (qName.equals("rotation") || qName.equals("rotate")) {
            angle = SAXUtil.parseFloat(attribs.getValue("angle"));
        } else if (qName.equals("axis")) {
            assert elementStack.peek().equals("rotation")
                    || elementStack.peek().equals("rotate");
            axis = SAXUtil.parseVector3(attribs);
        } else if (qName.equals("scale")) {
            scale = SAXUtil.parseVector3(attribs);
        } else if (qName.equals("keyframe")) {
            assert elementStack.peek().equals("keyframes");
            time = SAXUtil.parseFloat(attribs.getValue("time"));
        } else if (qName.equals("keyframes")) {
            assert elementStack.peek().equals("track");
        } else if (qName.equals("track")) {
            assert elementStack.peek().equals("tracks");
            String jointName = SAXUtil.parseString(attribs.getValue("bone"));
            joint = nameToJoint.get(jointName);
            track = new TransformTrack(joint, null, null, null, null);
        } else if (qName.equals("boneparent")) {
            assert elementStack.peek().equals("bonehierarchy");
            String jointName = attribs.getValue("bone");
            String parentName = attribs.getValue("parent");
            Joint joint = nameToJoint.get(jointName);
            Joint parent = nameToJoint.get(parentName);
            parent.addChild(joint);
        } else if (qName.equals("bone")) {
            assert elementStack.peek().equals("bones");

            // insert bone into indexed map
            joint = new Joint(attribs.getValue("name"));
            int id = SAXUtil.parseInt(attribs.getValue("id"));
            indexToJoint.put(id, joint);
            nameToJoint.put(joint.getName(), joint);
        } else if (qName.equals("tracks")) {
            assert elementStack.peek().equals("animation");
            tracks.clear();
            unusedJoints.clear();
            unusedJoints.addAll(nameToJoint.values());
        } else if (qName.equals("animation")) {
            assert elementStack.peek().equals("animations");
            String name = SAXUtil.parseString(attribs.getValue("name"));
            //float length = SAXUtil.parseFloat(attribs.getValue("length"));
            animClip = new AnimClip(name);
        } else if (qName.equals("bonehierarchy")) {
            assert elementStack.peek().equals("skeleton");
        } else if (qName.equals("animations")) {
            assert elementStack.peek().equals("skeleton");
            animClips = new ArrayList<>();
        } else if (qName.equals("bones")) {
            assert elementStack.peek().equals("skeleton");
        } else if (qName.equals("skeleton")) {
            assert elementStack.size() == 0;
        }
        elementStack.add(qName);
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (qName.equals("translate") || qName.equals("position") || qName.equals("scale")) {
        } else if (qName.equals("axis")) {
        } else if (qName.equals("rotate") || qName.equals("rotation")) {
            rotation = new Quaternion();
            axis.normalizeLocal();
            rotation.fromAngleNormalAxis(angle, axis);
            angle = 0;
            axis = null;
        } else if (qName.equals("bone")) {
            joint.getLocalTransform().setTranslation(position);
            joint.getLocalTransform().setRotation(rotation);
            if (scale != null) {
                joint.getLocalTransform().setScale(scale);
            }
            joint = null;
            position = null;
            rotation = null;
            scale = null;
        } else if (qName.equals("bonehierarchy")) {
            Joint[] joints = new Joint[indexToJoint.size()];
            // find joints without a parent and attach them to the armature
            // also assign the joints to the jointList
            for (Map.Entry<Integer, Joint> entry : indexToJoint.entrySet()) {
                Joint joint = entry.getValue();
                joints[entry.getKey()] = joint;
            }
            indexToJoint.clear();
            armature = new Armature(joints);
            armature.saveBindPose();
            armature.saveInitialPose();
        } else if (qName.equals("animation")) {
            animClips.add(animClip);
            animClip = null;
        } else if (qName.equals("track")) {
            if (track != null) { // if track has keyframes
                tracks.add(track);
                unusedJoints.remove(joint);
                track = null;
            }
        } else if (qName.equals("tracks")) {
            //nameToJoint contains the joints with no track
            for (Joint j : unusedJoints) {
                AnimMigrationUtils.padJointTracks(tracks, j);
            }
            TransformTrack[] trackList = tracks.toArray(new TransformTrack[tracks.size()]);
            animClip.setTracks(trackList);
            tracks.clear();
        } else if (qName.equals("keyframe")) {
            assert time >= 0;
            assert position != null;
            assert rotation != null;

            times.add(time);
            translations.add(position.addLocal(joint.getLocalTranslation()));
            rotations.add(joint.getLocalRotation().mult(rotation, rotation));
            if (scale != null) {
                scales.add(scale.multLocal(joint.getLocalScale()));
            }else{
                scales.add(new Vector3f(1,1,1));
            }
            time = -1;
            position = null;
            rotation = null;
            scale = null;
        } else if (qName.equals("keyframes")) {
            if (times.size() > 0) {
                float[] timesArray = new float[times.size()];
                for (int i = 0; i < timesArray.length; i++) {
                    timesArray[i] = times.get(i);
                }

                Vector3f[] transArray = translations.toArray(new Vector3f[translations.size()]);
                Quaternion[] rotArray = rotations.toArray(new Quaternion[rotations.size()]);
                Vector3f[] scalesArray = scales.toArray(new Vector3f[scales.size()]);
                
                track.setKeyframes(timesArray, transArray, rotArray, scalesArray);
            } else {
                track = null;
            }

            times.clear();
            translations.clear();
            rotations.clear();
            scales.clear();
        } else if (qName.equals("skeleton")) {
            nameToJoint.clear();
        }
        assert elementStack.peek().equals(qName);
        elementStack.pop();
    }

    /**
     * Reset the SkeletonLoader in case an error occurred while parsing XML.
     * This allows future use of the loader even after an error.
     */
    private void fullReset() {
        elementStack.clear();
        indexToJoint.clear();
        nameToJoint.clear();
        track = null;
        tracks.clear();
        animClip = null;
        if (animClips != null) {
            animClips.clear();
        }

        joint = null;
        armature = null;
        times.clear();
        rotations.clear();
        translations.clear();
        time = -1;
        position = null;
        rotation = null;
        scale = null;
        angle = 0;
        axis = null;
    }

    public Object load(InputStream in) throws IOException {
        try {
            
            // Added by larynx 25.06.2011
            // Android needs the namespace aware flag set to true 
            // Kirill 30.06.2011
            // Now, hack is applied for both desktop and android to avoid
            // checking with JmeSystem.
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader xr = factory.newSAXParser().getXMLReader();  
                         
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            InputStreamReader r = new InputStreamReader(in);
            xr.parse(new InputSource(r));
            if (animClips == null) {
                animClips = new ArrayList<AnimClip>();
            }
            AnimData data = new AnimData(armature, animClips);
            armature = null;
            animClips = null;
            return data;
        } catch (SAXException | ParserConfigurationException ex) {
            IOException ioEx = new IOException("Error while parsing Ogre3D dotScene");
            ioEx.initCause(ex);
            fullReset();
            throw ioEx;
        }
        
    }

    @Override
    public Object load(AssetInfo info) throws IOException {
        //AssetManager assetManager = info.getManager();
        InputStream in = null;
        try {
            in = info.openStream();
            return load(in);
        } finally {
            if (in != null){
                in.close();
            }
        }
    }
}
