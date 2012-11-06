package com.jme3.gde.welcome;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

/**
 *
 * @author normenhansen
 */
public class GradPanel extends JPanel {

    @Override
    protected void paintComponent(Graphics grphcs) {
        super.paintComponent(grphcs);
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2d = (Graphics2D) grphcs;
        GradientPaint gp = new GradientPaint(
                0, 0, Color.WHITE,
                0, h, Color.LIGHT_GRAY);

        g2d.setPaint(gp);
        g2d.fillRect(0, 0, w, h);
    }
}
