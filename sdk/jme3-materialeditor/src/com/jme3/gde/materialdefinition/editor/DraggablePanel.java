/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class DraggablePanel extends JPanel implements MouseListener, MouseMotionListener {

    protected int svdx, svdy, svdex, svdey;
    private boolean vertical = false;
    protected Diagram diagram;

    public DraggablePanel(boolean vertical) {
        this();
        this.vertical = vertical;
    }

    public DraggablePanel() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON2) {
            svdx = getLocation().x;
            if (!vertical) {
                svdex = e.getXOnScreen();
            }
            svdy = getLocation().y;
            svdey = e.getYOnScreen();
            e.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!SwingUtilities.isMiddleMouseButton(e)) {
            int xoffset = 0;
            if (!vertical) {
                xoffset = e.getLocationOnScreen().x - svdex;
            }
            int yoffset = e.getLocationOnScreen().y - svdey;
            setLocation(Math.max(0, svdx + xoffset), Math.max(0, svdy + yoffset));
            e.consume();
        }
    }

    public Diagram getDiagram() {
        return diagram;
    }

    public void setDiagram(Diagram diagram) {
        this.diagram = diagram;
    }
}
