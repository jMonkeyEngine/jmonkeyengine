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

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial.CullHint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TestComboMoves extends SimpleApplication implements ActionListener {

    private HashSet<String> pressedMappings = new HashSet<String>();

    private ComboMove fireball;
    private ComboMoveExecution fireballExec;
    private BitmapText fireballText;

    private ComboMove shuriken;
    private ComboMoveExecution shurikenExec;
    private BitmapText shurikenText;

    private ComboMove jab;
    private ComboMoveExecution jabExec;
    private BitmapText jabText;

    private ComboMove punch;
    private ComboMoveExecution punchExec;
    private BitmapText punchText;

    private ComboMove currentMove = null;
    private float currentMoveCastTime = 0;
    private float time = 0;

    public static void main(String[] args){
        TestComboMoves app = new TestComboMoves();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);

        // Create debug text
        BitmapText helpText = new BitmapText(guiFont);
        helpText.setLocalTranslation(0, settings.getHeight(), 0);
        helpText.setText("Moves:\n" +
                         "Fireball: Down, Down+Right, Right\n"+
                         "Shuriken: Left, Down, Attack1(Z)\n"+
                         "Jab: Attack1(Z)\n"+
                         "Punch: Attack1(Z), Attack1(Z)\n");
        guiNode.attachChild(helpText);

        fireballText = new BitmapText(guiFont);
        fireballText.setColor(ColorRGBA.Orange);
        fireballText.setLocalTranslation(0, fireballText.getLineHeight(), 0);
        guiNode.attachChild(fireballText);

        shurikenText = new BitmapText(guiFont);
        shurikenText.setColor(ColorRGBA.Cyan);
        shurikenText.setLocalTranslation(0, shurikenText.getLineHeight()*2f, 0);
        guiNode.attachChild(shurikenText);

        jabText = new BitmapText(guiFont);
        jabText.setColor(ColorRGBA.Red);
        jabText.setLocalTranslation(0, jabText.getLineHeight()*3f, 0);
        guiNode.attachChild(jabText);

        punchText = new BitmapText(guiFont);
        punchText.setColor(ColorRGBA.Green);
        punchText.setLocalTranslation(0, punchText.getLineHeight()*4f, 0);
        guiNode.attachChild(punchText);

        inputManager.addMapping("Left",    new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right",   new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("Up",      new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down",    new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Attack1", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addListener(this, "Left", "Right", "Up", "Down", "Attack1");

        fireball = new ComboMove("Fireball");
        fireball.press("Down").notPress("Right").done();
        fireball.press("Right", "Down").done();
        fireball.press("Right").notPress("Down").done();
        fireball.notPress("Right", "Down").done();
        fireball.setUseFinalState(false); // no waiting on final state

        shuriken = new ComboMove("Shuriken");
        shuriken.press("Left").notPress("Down", "Attack1").done();
        shuriken.press("Down").notPress("Attack1").timeElapsed(0.11f).done();
        shuriken.press("Attack1").notPress("Left").timeElapsed(0.11f).done();
        shuriken.notPress("Left", "Down", "Attack1").done();

        jab = new ComboMove("Jab");
        jab.setPriority(0.5f); // make jab less important than other moves
        jab.press("Attack1").done();

        punch = new ComboMove("Punch");
        punch.press("Attack1").done();
        punch.notPress("Attack1").done();
        punch.press("Attack1").done();

        fireballExec = new ComboMoveExecution(fireball);
        shurikenExec = new ComboMoveExecution(shuriken);
        jabExec = new ComboMoveExecution(jab);
        punchExec = new ComboMoveExecution(punch);
    }

    @Override
    public void simpleUpdate(float tpf){
        time += tpf;

        // check every frame if any executions are expired
        shurikenExec.updateExpiration(time);
        shurikenText.setText("Shuriken Exec: " + shurikenExec.getDebugString());

        fireballExec.updateExpiration(time);
        fireballText.setText("Fireball Exec: " + fireballExec.getDebugString());

        jabExec.updateExpiration(time);
        jabText.setText("Jab Exec: " + jabExec.getDebugString());

        punchExec.updateExpiration(time);
        punchText.setText("Punch Exec: " + punchExec.getDebugString());

        if (currentMove != null){
            currentMoveCastTime -= tpf;
            if (currentMoveCastTime <= 0){
                System.out.println("DONE CASTING " + currentMove.getMoveName());
                currentMoveCastTime = 0;
                currentMove = null;
            }
        }
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed){
            pressedMappings.add(name);
        }else{
            pressedMappings.remove(name);
        }

        // the pressed mappings was changed. update combo executions
        List<ComboMove> invokedMoves = new ArrayList<ComboMove>();
        if (shurikenExec.updateState(pressedMappings, time)){
            invokedMoves.add(shuriken);
        }

        if (fireballExec.updateState(pressedMappings, time)){
            invokedMoves.add(fireball);
        }

        if (jabExec.updateState(pressedMappings, time)){
            invokedMoves.add(jab);
        }

        if (punchExec.updateState(pressedMappings, time)){
            invokedMoves.add(punch);
        }

        if (invokedMoves.size() > 0){
            // choose move with highest priority
            float priority = 0;
            ComboMove toExec = null;
            for (ComboMove move : invokedMoves){
                if (move.getPriority() > priority){
                    priority = move.getPriority();
                    toExec = move;
                }
            }
            if (currentMove != null && currentMove.getPriority() > toExec.getPriority()){
                return;
            }

            currentMove = toExec;
            currentMoveCastTime = currentMove.getCastTime();
            //System.out.println("CASTING " + currentMove.getMoveName());
        }
    }

}
