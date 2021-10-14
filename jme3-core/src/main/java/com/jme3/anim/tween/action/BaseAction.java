package com.jme3.anim.tween.action;

import com.jme3.anim.tween.ContainsTweens;
import com.jme3.anim.tween.Tween;
import com.jme3.util.SafeArrayList;

import java.util.List;

/**
 * A Concrete fast Implementation for the interface #{@link Action},
 * used to create a subset of actions gathered from the extracted actions of the assigned tween.
 *
 * Example of operation :
 * <pre class="prettyprint">
 * //create a base action from a tween.
 * final BaseAction action = new BaseAction(Tweens.parallel(clipAction0, clipAction1));
 * //set the action properties - utilized within the #{@link Action} class.
 * baseAction.setLength(10f);
 * baseAction.setSpeed(2f);
 * //register the action as an observer to the animComposer control.
 * animComposer.addAction("basicAction", action);
 * //make a new Layer for a basic armature mask
 * animComposer.makeLayer(ActionState.class.getSimpleName(), new ArmatureMask());
 * //run the action within this layer
 * animComposer.setCurrentAction("basicAction", ActionState.class.getSimpleName());
 * </pre>
 * <b>Created by Nehon.</b>
 */
public class BaseAction extends Action {

    final private Tween tween;

    /**
     * Gathers the actions from a tween into a subset collection #{@link Action#actions},
     * which joins anim actions hierarchy later on when instantiated by #{@link BlendableAction} implementation entites.
     * @param tween the tween to join the actions collection, either raw tween using #{@link com.jme3.anim.tween.Tweens} utility or #{@link Action} tween.
     */
    public BaseAction(Tween tween) {
        this.tween = tween;
        setLength(tween.getLength());
        List<Action> subActions = new SafeArrayList<>(Action.class);
        gatherActions(tween, subActions);
        actions = new Action[subActions.size()];
        subActions.toArray(actions);
    }

    /**
     * Used to add the tween actions only if they are types of anim #{@link Action} class.
     * Otherwise if they are in a raw tween state #{@link Tween}.
     * then fetch the children actions of this tween & add them to subset of actions collection.
     * @param tween the tween to add, either raw tween using #{@link com.jme3.anim.tween.Tweens} utility or #{@link Action} tween.
     * @param subActions the collection to gather the tween actions into #{@link Action#actions} collection.
     */
    private void gatherActions(Tween tween, List<Action> subActions) {
        if (tween instanceof Action) {
            subActions.add((Action) tween);
        } else if (tween instanceof ContainsTweens) {
            //extract the tween action from the tween
            Tween[] tweens = ((ContainsTweens) tween).getTweens();
            for (Tween t : tweens) {
                gatherActions(t, subActions);
            }
        }
    }

    @Override
    public boolean interpolate(double t) {
        /*interpolate -> calls doInterpolate(t:Double) -> which interpolate between
        the concurrent transform matrices starting from the current transform matrix based on frame times.*/
        return tween.interpolate(t);
    }
}
