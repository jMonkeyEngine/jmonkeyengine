package com.jme3.gde.textureeditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class ColorController {

    public static ColorController create() {
        return new ColorController();
    }
    private final JPanel COMPONENT;
    private Color background = Color.WHITE;
    private Color foreground = Color.BLACK;

    protected ColorController() {
        final JPanel display = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w3 = getWidth() / 3;
                int h3 = getHeight() / 3;
                g.setColor(ColorController.this.getBackground());
                g.fillRect(w3, h3, 2 * w3, 2 * h3);
                g.setColor(Color.BLACK);
                g.drawRect(w3, h3, 2 * w3, 2 * h3);
                g.setColor(ColorController.this.getForeground());
                g.fillRect(0, 0, 2 * w3, 2 * h3);
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, 2 * w3, 2 * h3);
            }
        };
        display.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                int w3 = display.getWidth() / 3;
                int h3 = display.getHeight() / 3;
                if (new Rectangle(0, 0, 2 * w3, 2 * h3).contains(e.getPoint())) {
                    chooseColor(foreground);
                } else if (new Rectangle(w3, h3, 2 * w3, 2 * h3).contains(e.getPoint())) {
                    chooseColor(background);
                }
            }
        });
        Dimension dim = new Dimension(40, 40);
        display.setMinimumSize(dim);
        display.setPreferredSize(dim);
        display.setMaximumSize(dim);
        COMPONENT = display;
    }

    public Color getBackground() {
        return background;
    }

    public Color getForeground() {
        return foreground;
    }

    public void setBackground(Color color) {
        background = color;
        COMPONENT.repaint();
    }

    public void setForeground(Color color) {
        foreground = color;
        COMPONENT.repaint();
    }

    public JComponent getComponent() {
        return COMPONENT;
    }

    private void chooseColor(Color source) {
        Color initial = source;
        String title = source == foreground ? "Foreground Color" : "Background Color";
        Frame parent = JOptionPane.getFrameForComponent(COMPONENT);
        Color choice = JColorChooser.showDialog(parent, title, initial);
        if (choice != null) {
            if (source == foreground) {
                setForeground(choice);
            } else {
                setBackground(choice);
            }
        }
    }
}
