/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme3.gde.codepalette.scene;
import com.jme3.gde.codepalette.JmePaletteUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import org.openide.text.ActiveEditorDrop;

/**
 *
 * @author normenhansen, zathras
 */
public class JmePaletteClickCrosshairs implements ActiveEditorDrop {

    public JmePaletteClickCrosshairs() {
    }

    private String createBody() {

        String body = " /**Pick a Target Using Fixed Crosshairs.<ol><li>Map the \"pick target\" action to a MouseButtonTrigger. <li>flyCam.setEnabled(true); <li>inputManager.setCursorVisible(false); <li>Implement click action in AnalogListener (TODO).</ol>\n */\n private AnalogListener analogListener = new AnalogListener() {\n   public void onAnalog(String name, float intensity, float tpf) {\n     if (name.equals(\"pick target\")) {\n       // Reset results list.\n       CollisionResults results = new CollisionResults();\n       // Aim the ray from camera location in camera direction\n       // (assuming crosshairs are in center of screen).\n       Ray ray = new Ray(cam.getLocation(), cam.getDirection());\n       // Collect intersections between ray and all nodes in results list.\n       rootNode.collideWith(ray, results);\n       // (Print the results so we see what is going on:)\n       for (int i = 0; i < results.size(); i++) {\n         // For each \"hit\", we know distance, impact point, geometry name.\n         float dist = results.getCollision(i).getDistance();\n         Vector3f pt = results.getCollision(i).getContactPoint();\n         String target = results.getCollision(i).getGeometry().getName();\n         System.out.println(\"Selection #\" + i + \": \" + target + \" at \" + pt + \", \" + dist + \" WU away.\");\n       }\n       // 5. Interact with target -- e.g. rotate the clicked geometry.\n       if (results.size() > 0) {\n         // The closest result is the target that the player picked:\n         Geometry target = results.getClosestCollision().getGeometry();\n         // Here comes the action:\n         target.rotate(0, - intensity, 0); // TODO\n       }\n     } \n   }\n }; \n";
        return body;
    }

    public boolean handleTransfer(JTextComponent targetComponent) {
        String body = createBody();
        try {
            JmePaletteUtilities.insert(body, targetComponent);
        } catch (BadLocationException ble) {
            return false;
        }
        return true;
    }

}
