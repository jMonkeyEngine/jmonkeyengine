package com.jme3.anim.tween.action;

import com.jme3.anim.tween.ContainsTweens;
import com.jme3.anim.tween.Tween;
import com.jme3.util.SafeArrayList;

import java.util.List;

public class BaseAction extends Action {

    private Tween tween;

    public BaseAction(Tween tween) {
        this.tween = tween;
        setLength(tween.getLength());
        List<Action> subActions = new SafeArrayList<>(Action.class);
        gatherActions(tween, subActions);
        actions = new Action[subActions.size()];
        subActions.toArray(actions);
    }

    private void gatherActions(Tween tween, List<Action> subActions) {
        if (tween instanceof Action) {
            subActions.add((Action) tween);
        } else if (tween instanceof ContainsTweens) {
            Tween[] tweens = ((ContainsTweens) tween).getTweens();
            for (Tween t : tweens) {
                gatherActions(t, subActions);
            }
        }
    }

    @Override
    public boolean interpolate(double t) {
        return tween.interpolate(t);
    }
}
