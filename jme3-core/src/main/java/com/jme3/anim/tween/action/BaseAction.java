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
package com.jme3.anim.tween.action;

import com.jme3.anim.tween.ContainsTweens;
import com.jme3.anim.tween.Tween;
import com.jme3.util.SafeArrayList;
import java.util.List;

/**
 * A simple implementation for the interface #{@link Action},
 * used as a helper class for {@link Action} to extract and gather actions from a tween and interpolate it.
 * <p>
 * An example showing two clip actions running in parallel at 2x of their ordinary speed
 * by the help of BaseAction on a new Animation Layer :
 * <pre class="prettyprint">
 * //create a base action from a tween.
 * final BaseAction action = new BaseAction(Tweens.parallel(clipAction0, clipAction1));
 * //set the action properties - utilized within the #{@link Action} class.
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
     * Instantiates an action from a tween.
     *
     * @param tween a tween to extract the actions from.
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
     * Extracts the actions from a tween.
     *
     * @param tween      the tween to extract the actions from.
     * @param subActions a collection to gather the extracted actions.
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
        return tween.interpolate(t);
    }
}
