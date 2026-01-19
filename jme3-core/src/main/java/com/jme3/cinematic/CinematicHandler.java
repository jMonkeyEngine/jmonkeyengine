package com.jme3.cinematic;

import com.jme3.app.Application;
import com.jme3.cinematic.events.CinematicEvent;
import com.jme3.cinematic.events.CinematicEventListener;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;

/**
 * A interface that defines an object that can compose and coordinate cinematic events.
 */
public interface CinematicHandler extends CinematicEvent {

    /**
     * Adds a cinematic event to this cinematic at the given timestamp. This
     * operation returns a keyFrame
     *
     * @param timeStamp the time when the event will start after the beginning
     * of the cinematic
     * @param cinematicEvent the cinematic event
     * @return the keyFrame for that event.
     */
    KeyFrame addCinematicEvent(float timeStamp, CinematicEvent cinematicEvent);

    /**
     * Enqueue a cinematic event to a Cinematic. This is handy when you
     * want to chain events without knowing their durations.
     *
     * @param cinematicEvent the cinematic event to enqueue
     * @return the timestamp the event was scheduled.
     */
    float enqueueCinematicEvent(CinematicEvent cinematicEvent);

    /**
     * removes the first occurrence found of the given cinematicEvent.
     *
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    boolean removeCinematicEvent(CinematicEvent cinematicEvent);

    /**
     * removes the first occurrence found of the given cinematicEvent for the
     * given time stamp.
     *
     * @param timeStamp the timestamp when the cinematicEvent has been added
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    boolean removeCinematicEvent(float timeStamp, CinematicEvent cinematicEvent);

    /**
     * removes the first occurrence found of the given cinematicEvent for the
     * given keyFrame
     *
     * @param keyFrame the keyFrame returned by the addCinematicEvent method.
     * @param cinematicEvent the cinematicEvent to remove
     * @return true if the element has been removed
     */
    boolean removeCinematicEvent(KeyFrame keyFrame, CinematicEvent cinematicEvent);

    /**
     * Binds a camera to this Cinematic, tagged by a unique name. This method
     * creates and returns a CameraNode for the cam and attaches it to the scene.
     * The control direction is set to SpatialToCamera. This camera Node can
     * then be used in other events to handle the camera movements during
     * playback.
     *
     * @param cameraName the unique tag the camera should have
     * @param cam the scene camera.
     * @return the created CameraNode.
     */
    CameraNode bindCamera(String cameraName, Camera cam);

    /**
     * returns a cameraNode given its name
     *
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     * @return the cameraNode for this name
     */
    CameraNode getCamera(String cameraName);

    /**
     * Sets the active camera instantly (use activateCamera if you want to
     * schedule that event)
     *
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     */
    void setActiveCamera(String cameraName);

    /**
     * schedule an event that will activate the camera at the given time
     *
     * @param timeStamp the time to activate the cam
     * @param cameraName the camera name (as registered in
     * Cinematic#bindCamera())
     */
    void activateCamera(float timeStamp, String cameraName);

    /**
     * used internally put an eventdata in the cinematic
     *
     * @param type the type of data
     * @param key the key
     * @param object the data
     */
    void putEventData(String type, Object key, Object object);

    /**
     * used internally return and event data
     *
     * @param type the type of data
     * @param key the key
     * @return the pre-existing object, or null
     */
    Object getEventData(String type, Object key);

    /**
     * Used internally remove an eventData
     *
     * @param type the type of data
     * @param key the key of the data
     */
    void removeEventData(String type, Object key);

    /**
     * Clears all camera nodes bound to the cinematic from the scene node.
     * This method removes all previously bound CameraNodes and clears the
     * internal camera map, effectively detaching all cameras from the scene.
     */
    void clearCameras();

    /**
     * DO NOT implement this. This is a left-over from the previous problematic abstraction of the Cinematic
     * class. Kept just for backward compatibility.
     * 
     * @param app
     * @param cinematic
     */
    @Deprecated
    public default void initEvent(Application app, CinematicHandler cinematic) {

    }

    /**
     * Adds a CinematicEventListener to this handler.
     *
     * @param listener
     *            CinematicEventListener
     */
    void addListener(CinematicEventListener listener);

    /**
     * Removes a CinematicEventListener from this handler.
     *
     * @param listener
     *            CinematicEventListener
     */
    void removeListener(CinematicEventListener listener);

}