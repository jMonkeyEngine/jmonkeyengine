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
package com.jme3.gde.core.properties.preview;

import com.jme3.asset.TextureKey;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.PreviewRequest;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.SceneListener;
import com.jme3.gde.core.scene.SceneRequest;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.concurrent.Callable;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 *
 * @author Nehon
 */
public class TexturePreview implements SceneListener {

    private final ProjectAssetManager assetManager;
    private JComponent picPreview;
    private final Geometry quad;
    private final Geometry quad3D;
    private final Material material;
    private final Material material3D;

    public TexturePreview(ProjectAssetManager assetManager) {
        this.assetManager = assetManager;

        Quad quadMesh = new Quad(4.5f, 4.5f);
        Quad quadMesh3D = new Quad(4.5f, 4.5f);
        quadMesh3D.scaleTextureCoordinates(new Vector2f(4, 4));
        quad = new Geometry("previewQuad", quadMesh);
        quad.setLocalTranslation(new Vector3f(-2.25f, -2.25f, 0));
        quad3D = new Geometry("previewQuad", quadMesh3D);
        quad3D.setLocalTranslation(new Vector3f(-2.25f, -2.25f, 0));
        material3D = new Material(assetManager, "com/jme3/gde/core/properties/preview/tex3DThumb.j3md");
        material3D.setFloat("InvDepth", 1f / 16f);
        material3D.setInt("Rows", 4);
        material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        SceneApplication.getApplication().addSceneListener(this);
    }

    public void requestPreview(final String textureName, final String displayName, final int width, final int height, final JComponent picLabel, final JLabel infoLabel) {

        picPreview = picLabel;
        clearPreview();
        if (infoLabel != null) {
            infoLabel.setText(" Creating preview...");
        }

        SceneApplication.getApplication().enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                TextureKey key = new TextureKey(textureName);
                Texture t = assetManager.loadTexture(key);
                Spatial geom = quad;
                if (key.getTextureTypeHint() == Texture.Type.TwoDimensional) {
                    material.setTexture("ColorMap", t);
                    geom.setMaterial(material);                    
                    setLabel(infoLabel, displayName, t.getImage().getWidth(),  t.getImage().getHeight(), -1);                    
                } else if (key.getTextureTypeHint() == Texture.Type.ThreeDimensional) {
                    geom = quad3D;
                    material3D.setTexture("Texture", t);
                    geom.setMaterial(material3D);
                    setLabel(infoLabel, displayName + " (Texture3D)", t.getImage().getWidth(),  t.getImage().getHeight(),  t.getImage().getDepth());
                    
                } else if (key.getTextureTypeHint() == Texture.Type.CubeMap) {
                    geom = SkyFactory.createSky(assetManager, textureName, SkyFactory.EnvMapType.CubeMap);
                    setLabel(infoLabel, displayName + " (CubeMap)", t.getImage().getWidth(),  t.getImage().getHeight(),  -1);                        
                }

                PreviewRequest request = new PreviewRequest(TexturePreview.this, geom, width, height);
                request.getCameraRequest().setLocation(new Vector3f(0, 0, 5.3f));
                request.getCameraRequest().setLookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y.mult(-1));
                SceneApplication.getApplication().createPreview(request);

                return null;
            }

        });
    }

    public void cleanUp() {
        SceneApplication.getApplication().removeSceneListener(this);
    }

    @Override
    public void sceneOpened(SceneRequest request) {
    }

    @Override
    public void sceneClosed(SceneRequest request) {
    }

    private void setLabel(final JLabel label, final String text, final int width, final int height, final int depth) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (label != null) {
                    String labText = " " + text + "    w : " + width + "    h : " + height;
                    if (depth > 0) {
                        labText += "    d : " + depth;
                    }
                    label.setText(labText);
                }
            }
        });
    }
    
    private void clearPreview() {
        if (picPreview instanceof JLabel) {
            ((JLabel) picPreview).setIcon(null);
        }
        if (picPreview instanceof JButton) {
            ((JButton) picPreview).setIcon(null);
        }
    }

    @Override
    public void previewCreated(final PreviewRequest request) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (request.getRequester() == TexturePreview.this) {
                    final ImageIcon icon = new ImageIcon(request.getImage());
                    if (picPreview instanceof JLabel) {
                        ((JLabel) picPreview).setIcon(icon);
                    }
                    if (picPreview instanceof JButton) {
                        ((JButton) picPreview).setIcon(icon);
                    }
                }
            }
        });
    }
}
