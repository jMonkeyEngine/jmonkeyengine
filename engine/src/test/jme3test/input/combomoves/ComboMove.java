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

package jme3test.input.combomoves;

import java.util.ArrayList;
import java.util.List;

public class ComboMove {

    public static class ComboMoveState {
        
        private String[] pressedMappings;
        private String[] unpressedMappings;
        private float timeElapsed;

        public ComboMoveState(String[] pressedMappings, String[] unpressedMappings, float timeElapsed) {
            this.pressedMappings = pressedMappings;
            this.unpressedMappings = unpressedMappings;
            this.timeElapsed = timeElapsed;
        }

        public String[] getUnpressedMappings() {
            return unpressedMappings;
        }

        public String[] getPressedMappings() {
            return pressedMappings;
        }

        public float getTimeElapsed() {
            return timeElapsed;
        }
        
    }

    private String moveName;
    private List<ComboMoveState> states = new ArrayList<ComboMoveState>();
    private boolean useFinalState = true;
    private float priority = 1;
    private float castTime = 0.8f;

    private transient String[] pressed, unpressed;
    private transient float timeElapsed;

    public ComboMove(String moveName){
        this.moveName = moveName;
    }

    public float getPriority() {
        return priority;
    }

    public void setPriority(float priority) {
        this.priority = priority;
    }

    public float getCastTime() {
        return castTime;
    }

    public void setCastTime(float castTime) {
        this.castTime = castTime;
    }
    
    public boolean useFinalState() {
        return useFinalState;
    }

    public void setUseFinalState(boolean useFinalState) {
        this.useFinalState = useFinalState;
    }
    
    public ComboMove press(String ... pressedMappings){
        this.pressed = pressedMappings;
        return this;
    }

    public ComboMove notPress(String ... unpressedMappings){
        this.unpressed = unpressedMappings;
        return this;
    }

    public ComboMove timeElapsed(float time){
        this.timeElapsed = time;
        return this;
    }

    public void done(){
        if (pressed == null)
            pressed = new String[0];
        
        if (unpressed == null)
            unpressed = new String[0];

        states.add(new ComboMoveState(pressed, unpressed, timeElapsed));
        pressed = null;
        unpressed = null;
        timeElapsed = -1;
    }

    public ComboMoveState getState(int num){
        return states.get(num);
    }

    public int getNumStates(){
        return states.size();
    }

    public String getMoveName() {
        return moveName;
    }
    
}
