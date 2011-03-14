package com.jme3.gde.textureeditor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

public interface EditorToolTarget {

    JComponent getImageCanvas();

    float getScaleX();
    float getScaleY();
    BufferedImage getCurrentImage();

    public void setForeground(Color picked);

    public void setBackground(Color picked);

    void spawnEditor(BufferedImage editedImage);
}
