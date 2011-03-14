package com.jme3.gde.textureeditor.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.event.MouseInputAdapter;
import com.jme3.gde.textureeditor.EditorTool;
import com.jme3.gde.textureeditor.EditorToolTarget;

public class CropTool extends MouseInputAdapter implements EditorTool {

    public static CropTool create() {
        return new CropTool();
    }
    private EditorToolTarget target;
    private Stroke stroke = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] { 4, 4 }, 0);
    private Rectangle track;
    private Point last;

    protected CropTool() {}

    public void install(EditorToolTarget t) {
        target = t;
        target.getImageCanvas().addMouseListener(this);
        target.getImageCanvas().addMouseMotionListener(this);
    }

    public void uninstall(EditorToolTarget t) {
        target.getImageCanvas().removeMouseListener(this);
        target.getImageCanvas().removeMouseMotionListener(this);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        last = e.getPoint();
        track = new Rectangle(last.x, last.y, 1, 1);
        target.getImageCanvas().repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int x = (int)(track.x / target.getScaleX());
        int y = (int)(track.y / target.getScaleY());
        int w = (int)(track.width / target.getScaleX());
        int h = (int)(track.height / target.getScaleY());
        BufferedImage source = target.getCurrentImage();
        int type = source.getType();
        if(type == BufferedImage.TYPE_CUSTOM) {
            type = BufferedImage.TYPE_INT_ARGB;
        }
        BufferedImage dest = new BufferedImage(w, h, type);
        dest.createGraphics().drawImage(source.getSubimage(x, y, w, h), 0, 0, null);
        target.spawnEditor(dest);
        track = null;
        target.getImageCanvas().repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point current = e.getPoint();
        int x = Math.min(last.x, current.x);
        int y = Math.min(last.y, current.y);
        int w = Math.abs(current.x - last.x);
        int h = Math.abs(current.y - last.y);
        track.setRect(x, y, w, h);
        track = track.intersection(new Rectangle(0, 0, target.getImageCanvas().getWidth() - 1, target.getImageCanvas().getHeight() - 1));
        target.getImageCanvas().repaint();
    }

    public void drawTrack(Graphics2D g, int width, int height, float scaleX, float scaleY) {
        if(track != null) {
            g.setPaint(Color.GREEN);
            g.setStroke(stroke);
            g.draw(track);
        }
    }
}
