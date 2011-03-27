/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.angelfont;

import java.awt.image.BufferedImage;

/**
 *
 * @author normenhansen
 */
public class AngelFont {
    private BufferedImage image;
    private String description;

    public AngelFont() {
    }

    public AngelFont(BufferedImage image, String description) {
        this.image = image;
        this.description = description;
    }

    /**
     * @return the image
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * @param image the image to set
     */
    public void setImage(BufferedImage image) {
        this.image = image;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    

}
