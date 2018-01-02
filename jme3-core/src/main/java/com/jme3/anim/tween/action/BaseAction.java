package com.jme3.anim.tween.action;

import com.jme3.anim.tween.ContainsTweens;
import com.jme3.anim.tween.Tween;
import com.jme3.util.SafeArrayList;

public class BaseAction extends Action {

    private Tween tween;
    private SafeArrayList<Action> subActions = new SafeArrayList<>(Action.class);

    public BaseAction(Tween tween) {
        this.tween = tween;
        setLength(tween.getLength());
        gatherActions(tween);
    }

    private void gatherActions(Tween tween) {
        if (tween instanceof Action) {
            subActions.add((Action) tween);
        } else if (tween instanceof ContainsTweens) {
            Tween[] tweens = ((ContainsTweens) tween).getTweens();
            for (Tween t : tweens) {
                gatherActions(t);
            }
        }
    }

    @Override
    public boolean subInterpolate(double t) {
        return tween.interpolate(t);
    }
}
