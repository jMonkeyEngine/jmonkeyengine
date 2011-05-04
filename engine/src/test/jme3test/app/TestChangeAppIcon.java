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
package jme3test.app;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.system.AppSettings;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class TestChangeAppIcon extends SimpleApplication {

    private static final Logger log=Logger.getLogger(TestChangeAppIcon.class.getName());

    public static void main(String[] args) {
        TestChangeAppIcon app = new TestChangeAppIcon();
        AppSettings settings = new AppSettings(true);

        try {
            Class<TestChangeAppIcon> clazz = TestChangeAppIcon.class;

            settings.setIcons(new BufferedImage[]{
                        ImageIO.read(clazz.getResourceAsStream("/Interface/icons/SmartMonkey256.png")),
                        ImageIO.read(clazz.getResourceAsStream("/Interface/icons/SmartMonkey128.png")),
                        ImageIO.read(clazz.getResourceAsStream("/Interface/icons/SmartMonkey32.png")),
                        ImageIO.read(clazz.getResourceAsStream("/Interface/icons/SmartMonkey16.png")),
                    });
        } catch (IOException e) {
            log.log(java.util.logging.Level.WARNING, "Unable to load program icons", e);
        }
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Write text on the screen (HUD)
        guiNode.detachAllChildren();
        BitmapText helloText = new BitmapText(guiFont);
        helloText.setText("The icon of the app should be a smart monkey!");
        helloText.setLocalTranslation(300, helloText.getLineHeight(), 0);
        guiNode.attachChild(helloText);
    }
}
