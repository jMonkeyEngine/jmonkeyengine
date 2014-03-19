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
public class JmePaletteExplosion implements ActiveEditorDrop {

    public JmePaletteExplosion() {
    }

    private String createBody() {

        String body = "    /** Explosion effect. Uses Texture from jme3-test-data library! */ \n    ParticleEmitter debrisEffect = new ParticleEmitter(\"Debris\", ParticleMesh.Type.Triangle, 10);\n    Material debrisMat = new Material(assetManager, \"Common/MatDefs/Misc/Particle.j3md\");\n    debrisMat.setTexture(\"Texture\", assetManager.loadTexture(\"Effects/Explosion/Debris.png\"));\n    debrisEffect.setMaterial(debrisMat);\n    debrisEffect.setImagesX(3); debrisEffect.setImagesY(3); // 3x3 texture animation\n    debrisEffect.setRotateSpeed(4);\n    debrisEffect.setSelectRandomImage(true);\n    debrisEffect.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 4, 0));\n    debrisEffect.setStartColor(new ColorRGBA(1f, 1f, 1f, 1f));\n    debrisEffect.setGravity(0f,6f,0f);\n    debrisEffect.getParticleInfluencer().setVelocityVariation(.60f);\n    rootNode.attachChild(debrisEffect);\n    debrisEffect.emitAllParticles();\n";
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
