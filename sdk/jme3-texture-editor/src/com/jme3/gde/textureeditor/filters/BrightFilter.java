package com.jme3.gde.textureeditor.filters;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import com.jme3.gde.textureeditor.ImageEditorComponent;

public class BrightFilter implements BufferedImageFilter {

    public static BrightFilter create() {
        return new BrightFilter();
    }
    private final int[] lookup;

    protected BrightFilter() {
        int[] arr = new int[256];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (int) (Math.sqrt(i / 255.0) * 255.0);
        }
        lookup = arr;
    }

    public BufferedImage filter(BufferedImage source, Object... args) {
        ImageEditorComponent parent = (ImageEditorComponent) args[0];
        final BufferedImage sourceIcon = parent.createIcon(256, 256);
        final JLabel label = new JLabel(new ImageIcon(sourceIcon));
        final JSlider slider = new JSlider(0, 200, 100);
        slider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (!slider.getValueIsAdjusting()) {
                    label.setIcon(new ImageIcon(doFilter(sourceIcon, slider.getValue() / 100f)));
                }
            }
        });
        slider.setOrientation(JSlider.HORIZONTAL);
        JPanel sliderContainer = new JPanel(new GridLayout(1, 1));
        sliderContainer.setBorder(BorderFactory.createTitledBorder("Brightness value"));
        sliderContainer.add(slider);
        JPanel labelContainer = new JPanel(new GridBagLayout());
        GridBagConstraints lim = new GridBagConstraints();
        lim.gridx = lim.gridy = 0;
        labelContainer.add(label, lim);
        labelContainer.setBorder(BorderFactory.createTitledBorder("Preview"));
        JPanel container = new JPanel(new BorderLayout());
        container.add(labelContainer, BorderLayout.CENTER);
        container.add(sliderContainer, BorderLayout.SOUTH);

        int choice = JOptionPane.showConfirmDialog(parent.getComponent(), container, "Brightness Filter", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        if (choice == JOptionPane.OK_OPTION) {
            return doFilter(source, slider.getValue() / 100f);
        } else {
            return null;
        }
    }

    private BufferedImage doFilter(BufferedImage source, float factor) {
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < source.getWidth(); i++) {
            for (int j = 0; j < source.getHeight(); j++) {
                int rgb = source.getRGB(i, j);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                r = Math.min(255, Math.round(lookup[r] * factor));
                g = Math.min(255, Math.round(lookup[g] * factor));
                b = Math.min(255, Math.round(lookup[b] * factor));
                rgb = (a << 24) + (r << 16) + (g << 8) + b;
                dest.setRGB(i, j, rgb);
            }
        }
        return dest;
    }
}
