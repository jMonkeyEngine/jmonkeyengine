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

package jme3test.collision;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class RayTrace {

    private BufferedImage image;
    private Camera cam;
    private Spatial scene;
    private CollisionResults results = new CollisionResults();
    private JFrame frame;
    private JLabel label;

    public RayTrace(Spatial scene, Camera cam, int width, int height){
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.scene = scene;
        this.cam = cam;
    }

    public void show(){
        frame = new JFrame("HDR View");
        label = new JLabel(new ImageIcon(image));
        frame.getContentPane().add(label);
        frame.setLayout(new FlowLayout());
        frame.pack();
        frame.setVisible(true);
    }

    public void update(){
        int w = image.getWidth();
        int h = image.getHeight();

        float wr = (float) cam.getWidth()  / image.getWidth();
        float hr = (float) cam.getHeight() / image.getHeight();

        scene.updateGeometricState();

        for (int y = 0; y < h; y++){
            for (int x = 0; x < w; x++){
                Vector2f v = new Vector2f(x * wr,y * hr);
                Vector3f pos = cam.getWorldCoordinates(v, 0.0f);
                Vector3f dir = cam.getWorldCoordinates(v, 0.3f);
                dir.subtractLocal(pos).normalizeLocal();

                Ray r = new Ray(pos, dir);

                results.clear();
                scene.collideWith(r, results);
                if (results.size() > 0){
                    image.setRGB(x, h - y - 1, 0xFFFFFFFF);
                }else{
                    image.setRGB(x, h - y - 1, 0xFF000000);
                }
            }
        }

        label.repaint();
    }

}
