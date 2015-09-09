/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.utils.MaterialUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MouseInputListener;

/**
 *
 * @author Nehon
 */
public class ConnectionCurve extends JPanel implements ComponentListener, MouseInputListener, KeyListener, Selectable, PropertyChangeListener {

    protected Dot start;
    protected Dot end;
    private final Point[] points = new Point[7];
    private int pointsSize = 7;
    private int nbCurve = 2;
    private final CubicCurve2D[] curves = new CubicCurve2D[2];    
    private String key = "";
    protected MappingBlock mapping;

    private MouseEvent convertEvent(MouseEvent e) {
        MouseEvent me = null;
        //workaround for swing utilities removing mouse button when converting events.
        if (e instanceof MouseWheelEvent || e instanceof MenuDragMouseEvent) {
            SwingUtilities.convertMouseEvent(this, e, getDiagram());
        } else {
            Point p = SwingUtilities.convertPoint(this, new Point(e.getX(),
                    e.getY()),
                    getDiagram());

            me = new MouseEvent(getDiagram(),
                    e.getID(),
                    e.getWhen(),
                    e.getModifiers()
                    | e.getModifiersEx(),
                    p.x, p.y,
                    e.getXOnScreen(),
                    e.getYOnScreen(),
                    e.getClickCount(),
                    e.isPopupTrigger(),
                    e.getButton());
        }
        return me;
    }
    
    @SuppressWarnings("LeakingThisInConstructor")
    public ConnectionCurve(Dot start, Dot end) {

        if (start.getParamType() == Dot.ParamType.Output
                || (start.getParamType() == Dot.ParamType.Both && end.getParamType() != Dot.ParamType.Output)
                || (end.getParamType() == Dot.ParamType.Both && start.getParamType() != Dot.ParamType.Input)) {
            this.start = start;
            this.end = end;
        } else {
            this.start = end;
            this.end = start;
        }

        for (int i = 0; i < 7; i++) {
            points[i] = new Point();
        }
        for (int i = 0; i < nbCurve; i++) {
            curves[i] = new CubicCurve2D.Double();
        }
        resize(this.start, this.end);
        addMouseMotionListener(this);
        addMouseListener(this);
        addKeyListener(this);
        setFocusable(true);
        setOpaque(false);

    }

    private void translate(Point p, Point store) {
        store.x = p.x - getLocation().x - 1;
        store.y = p.y - getLocation().y - 1;
    }
    private final Point p1 = new Point();
    private final Point p2 = new Point();
    private final Point p3 = new Point();
    private final Point p4 = new Point();

    public String getKey() {
        return key;
    }

    protected void makeKey(MappingBlock mapping, String techName) {
        this.mapping = mapping;
        key = MaterialUtils.makeKey(mapping, techName);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = ((Graphics2D) g);
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(new BasicStroke(4));
        Path2D.Double path1 = new Path2D.Double();
        if (getDiagram().getSelectedItems().contains(this)) {
            g.setColor(SELECTED_COLOR);
        } else {
            g.setColor(VERY_DARK_GREY);
        }
       

        if (pointsSize < 4) {
            translate(points[0], p1);
            translate(points[1], p2);
            translate(points[1], p3);
            translate(points[2], p4);
            path1.moveTo(p1.x, p1.y);
            path1.curveTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
            curves[0].setCurve(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
            nbCurve = 1;
        }

        for (int i = 0; i < pointsSize - 3; i += 3) {

            translate(points[i], p1);
            translate(points[i + 1], p2);
            translate(points[i + 2], p3);
            translate(points[i + 3], p4);
            path1.moveTo(p1.x, p1.y);
            path1.curveTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
            if (i > 1) {
                curves[1].setCurve(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
                nbCurve = 2;
            } else {
                curves[0].setCurve(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y);
                nbCurve = 1;
            }

        }

        ((Graphics2D) g).draw(path1);
        g2.setStroke(new BasicStroke(2));
       
        if (getDiagram().getSelectedItems().contains(this)) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(LIGHT_GREY);
        }
        
        ((Graphics2D) g).draw(path1);
    }
    private final static Color LIGHT_GREY = new Color(190, 190, 190);
    private final static Color VERY_DARK_GREY = new Color(5, 5, 5);
    private final static Color SELECTED_COLOR = new Color(0.8f, 0.8f, 1.0f, 1.0f);
    
    public final static int MARGIN = 15;

    private int getOffset() {
        return 5 * start.getIndex();
    }

    private int getHMiddle() {
        int st = start.getNode().getLocation().y + start.getNode().getHeight();
        int diff = end.getNode().getLocation().y - st;
        return st + diff / 2 + getOffset();

    }

    private int getVMiddleStart() {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();
        return startLocation.x + Math.max(MARGIN, (endLocation.x - startLocation.x) / 2) + getOffset();
    }

    private int getVMiddleStartNoMargin() {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();
        return startLocation.x + ((endLocation.x - startLocation.x) / 2) + getOffset();
    }

    private int getVMiddleStartClampedRight() {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();
        int right = end.getNode().getLocation().x + end.getNode().getWidth() + MARGIN;
        int loc = startLocation.x + Math.max(MARGIN, (endLocation.x - startLocation.x) / 2);
        return Math.max(loc, right) + getOffset();
    }

    private int getVMiddleEnd() {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();
        return endLocation.x - Math.max(0, Math.max(MARGIN, (endLocation.x - startLocation.x) / 2) + getOffset());

    }

    private int getVMiddleEndClampedLeft() {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();
        int left = start.getNode().getLocation().x - MARGIN;//+ end.getNode().getWidth() + MARGIN;
        int loc = endLocation.x - Math.max(0, Math.max(MARGIN, (endLocation.x - startLocation.x) / 2));
        return Math.min(loc, left) + getOffset();

    }

    private int getHBottom() {
        int endBottom = end.getNode().getLocation().y + end.getNode().getHeight() + MARGIN;
        int startBottom = start.getNode().getLocation().y + start.getNode().getHeight() + MARGIN;
        return Math.max(endBottom, startBottom) + getOffset();

    }

    public final void resize(Dot start, Dot end) {
        Point startLocation = start.getStartLocation();
        Point endLocation = end.getEndLocation();

        if (start.getParamType() == Dot.ParamType.Both) {
            startLocation.x = endLocation.x - MARGIN * 2;
            pointsSize = 3;
            points[0].setLocation(startLocation);
            points[1].x = startLocation.x;
            points[1].y = endLocation.y;
            points[2].setLocation(endLocation);
        } else if (end.getParamType() == Dot.ParamType.Both) {
            endLocation.x = startLocation.x + MARGIN * 2;
            pointsSize = 3;
            points[0].setLocation(startLocation);
            points[1].x = endLocation.x;
            points[1].y = startLocation.y;
            points[2].setLocation(endLocation);
        } else if (startLocation.x + MARGIN <= endLocation.x - MARGIN) {
            pointsSize = 4;
            points[0].setLocation(startLocation);
            points[1].x = getVMiddleStart();
            points[1].y = startLocation.y;
            points[2].x = getVMiddleStart();
            points[2].y = endLocation.y;
            points[3].setLocation(endLocation);
        } else if (startLocation.x <= endLocation.x) {
            pointsSize = 4;
            points[0].setLocation(startLocation);
            points[1].x = getVMiddleStartNoMargin();
            points[1].y = startLocation.y;
            points[2].x = getVMiddleStartNoMargin();
            points[2].y = endLocation.y;
            points[3].setLocation(endLocation);
        } else {
            pointsSize = 7;
            points[0].setLocation(startLocation);
            points[6].setLocation(endLocation);
            points[1].x = getVMiddleStart();
            points[1].y = startLocation.y;

            points[5].x = getVMiddleEnd();
            points[5].y = endLocation.y;
            if ((start.getNode().getLocation().y + start.getNode().getHeight() + MARGIN
                    > end.getNode().getLocation().y - MARGIN)
                    && (end.getNode().getLocation().y + end.getNode().getHeight() + MARGIN
                    > start.getNode().getLocation().y - MARGIN)) {

                if (startLocation.y + MARGIN <= endLocation.y - MARGIN) {
                    points[1].x = getVMiddleStartClampedRight();
                    points[2].x = getVMiddleStartClampedRight();
                } else {
                    points[1].x = getVMiddleStart();
                    points[2].x = getVMiddleStart();
                }
                points[2].y = getHBottom();
                points[4].y = getHBottom();

                if (startLocation.y + MARGIN > endLocation.y - MARGIN) {
                    points[4].x = getVMiddleEndClampedLeft();
                    points[5].x = getVMiddleEndClampedLeft();
                    points[3].x = points[4].x + (points[2].x - points[4].x) / 2;

                } else {
                    points[4].x = getVMiddleEnd();
                    points[5].x = getVMiddleEnd();
                    points[3].x = points[4].x + (points[2].x - points[4].x) / 2;
                }

                points[3].y = getHBottom();

            } else {

                points[2].x = getVMiddleStart();
                points[2].y = getHMiddle();
                points[3].x = points[4].x + (points[2].x - points[4].x) / 2;
                points[3].y = getHMiddle();
                points[4].x = getVMiddleEnd();
                points[4].y = points[3].y;
            }
        }
        updateBounds();
    }

    private void updateBounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int i = 0; i < pointsSize; i++) {
            if (points[i].x < minX) {
                minX = points[i].x;
            }
            if (points[i].y < minY) {
                minY = points[i].y;
            }

            if (points[i].x > maxX) {
                maxX = points[i].x;
            }
            if (points[i].y > maxY) {
                maxY = points[i].y;
            }
        }
        maxX += MARGIN;
        maxY += MARGIN;
        minX -= MARGIN;
        minY -= MARGIN;

        setLocation(minX, minY);
        setSize(maxX - minX, maxY - minY);
    }

    private Diagram getDiagram() {
        return (Diagram) start.getDiagram();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dispatchEventToDiagram(e);
    }

    private void dispatchEventToDiagram(MouseEvent e) {
        MouseEvent me = convertEvent(e);
        getDiagram().dispatchEvent(me);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dispatchEventToDiagram(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        dispatchEventToDiagram(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dispatchEventToDiagram(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dispatchEventToDiagram(e);
    }

    public void select(MouseEvent e) {

        requestFocusInWindow(true);
        int margin = MARGIN / 2;
        boolean selected = false;

        for (int i = 0; i < nbCurve && !selected; i++) {
            selected = curves[i].intersects(e.getX() - margin, e.getY() - margin, e.getX() + margin, e.getY() + margin);
        }

        if (selected) {
            getDiagram().select(this, e.isShiftDown() || e.isControlDown());
            e.consume();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {
            Diagram diag = getDiagram();
            diag.removeSelected();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public void componentResized(ComponentEvent e) {
    }

    public void componentMoved(ComponentEvent e) {
        resize(start, end);
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void propertyChange(PropertyChangeEvent evt) {
        MappingBlock map = (MappingBlock) evt.getSource();
        key = MaterialUtils.makeKey(map, getDiagram().getCurrentTechniqueName());
    }

    public Dot getStart() {
        return start;
    }

    public Dot getEnd() {
        return end;
    }
    
    
}
