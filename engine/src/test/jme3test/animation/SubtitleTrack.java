/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jme3test.animation;

import com.jme3.cinematic.events.GuiTrack;
import de.lessvoid.nifty.elements.render.TextRenderer;

/**
 *
 * @author Nehon
 */
public class SubtitleTrack extends GuiTrack{
    private String text="";

    public SubtitleTrack(String screen,float initialDuration, String text) {
        super(screen, initialDuration);
        this.text=text;
    }

    @Override
    public void onPlay() {
        super.onPlay();
		//REMY FIX THIS
        //nifty.getScreen(screen).findElementByName("text").getRenderer(TextRenderer.class).changeText(text);
    }








}
