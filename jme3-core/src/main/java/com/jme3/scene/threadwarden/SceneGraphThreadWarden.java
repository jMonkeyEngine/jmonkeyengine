package com.jme3.scene.threadwarden;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Thread warden keeps track of mutations to the scene graph and ensures that they are only done on the main thread.
 * IF the parent node is marked as being reserved for the main thread (which basically means it's connected to the
 * root node)
 * <p>
 *     Only has an effect if asserts are on
 * </p>
 */
public class SceneGraphThreadWarden {

    /**
     * If THREAD_WARDEN_ENABLED is true AND asserts are on the checks are made.
     * This parameter is here to allow asserts to run without thread warden checks (by setting this parameter to false)
     */
    public static boolean THREAD_WARDEN_ENABLED = true;

    public static boolean ASSERTS_ENABLED = false;

    static{
        //noinspection AssertWithSideEffects
        assert ASSERTS_ENABLED = true;
    }

    public static Thread mainThread;
    public static final Set<Object> nodesThatAreMainThreadReserved = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * Marks the given node as being reserved for the main thread.
     * Additionally, sets the current thread as the main thread (if it hasn't already been set)
     * @param rootNode the root node of the scene graph. This is used to determine if a spatial is a child of the root node.
     *                 (Can add multiple "root" nodes, e.g. gui nodes or overlay nodes)
     */
    public static void setup(Node rootNode){
        if(checksDisabled()){
            return;
        }
        Thread thisThread = Thread.currentThread();
        if(mainThread != null && mainThread != thisThread ){
            throw new IllegalStateException("The main thread has already been set to " + mainThread.getName() + " but now it's being set to " + Thread.currentThread().getName());
        }
        mainThread = thisThread;
        setTreeRestricted(rootNode);
    }

    /**
     * Runs through the entire tree and sets the restriction state of all nodes below the given node
     * @param spatial the node (and children) to set the restriction state of
     */
    private static void setTreeRestricted(Spatial spatial){
        nodesThatAreMainThreadReserved.add(spatial);
        if(spatial instanceof Node){
            for(Spatial child : ((Node) spatial).getChildren()){
                setTreeRestricted(child);
            }
        }
    }

    /**
     * Releases this tree from being only allowed to be mutated on the main thread
     * @param spatial the node (and children) to release the restriction state of.
     */
    private static void setTreeNotRestricted(Spatial spatial){
        nodesThatAreMainThreadReserved.remove(spatial);
        if(spatial instanceof Node){
            for(Spatial child : ((Node) spatial).getChildren()){
                setTreeNotRestricted(child);
            }
        }
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean updateRequirement(Spatial spatial, Node newParent){
        if(checksDisabled()){
            return true;
        }

        boolean shouldNowBeRestricted = newParent !=null && nodesThatAreMainThreadReserved.contains(newParent);
        boolean wasPreviouslyRestricted = nodesThatAreMainThreadReserved.contains(spatial);

        if(shouldNowBeRestricted || wasPreviouslyRestricted ){
            assertOnCorrectThread(spatial);
        }

        if(shouldNowBeRestricted == wasPreviouslyRestricted){
            return true;
        }
        if(shouldNowBeRestricted){
            setTreeRestricted(spatial);
        }else{
            setTreeNotRestricted(spatial);
        }

        return true; // return true so can be a "side effect" of an assert
    }

    public static void reset(){
        nodesThatAreMainThreadReserved.clear();
        mainThread = null;
    }

    private static boolean checksDisabled(){
       return !THREAD_WARDEN_ENABLED || !ASSERTS_ENABLED;
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean assertOnCorrectThread(Spatial spatial){
        if(checksDisabled()){
            return true;
        }
        if(nodesThatAreMainThreadReserved.contains(spatial)){
            if(Thread.currentThread() != mainThread){
                throw new IllegalThreadSceneGraphMutation("The spatial " + spatial + " was mutated on a thread other than the main thread, was mutated on " + Thread.currentThread().getName());
            }
        }
        return true; // return true so can be a "side effect" of an assert
    }

}

