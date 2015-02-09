/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.materialdefinition.fileStructure.leaves.MappingBlock;
import com.jme3.gde.materialdefinition.utils.MaterialUtils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuDragMouseEvent;
import javax.swing.event.MouseInputListener;

/**
 *
 * Kept this class in case of.
 * This is the old staright connection class, now ConnectionCurve is used
 * @author Nehon
 */
@Deprecated
public class ConnectionStraight extends JPanel implements ComponentListener, MouseInputListener, KeyListener, Selectable, PropertyChangeListener {

    protected Dot start;
    protected Dot end;
    private Point[] points = new Point[6];
    private int pointsSize = 6;
    private Corner[] corners = new Corner[6];
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

    private enum Corner {

        RightBottom,
        BottomRight,
        BottomLeft,
        LeftBottom,
        RightTop,
        TopRight,
        LeftTop,
        TopLeft,
        Top,
        Bottom,
        None,}

    public ConnectionStraight(Dot start, Dot end) {


        if (start.getParamType() == Dot.ParamType.Output
                || (start.getParamType() == Dot.ParamType.Both && end.getParamType() != Dot.ParamType.Output)
                || (end.getParamType() == Dot.ParamType.Both && start.getParamType() != Dot.ParamType.Input)) {
            this.start = start;
            this.end = end;
        } else {
            this.start = end;
            this.end = start;
        }

        for (int i = 0; i < 6; i++) {
            points[i] = new Point();
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
    private Point p1 = new Point();
    private Point p2 = new Point();
    private Point tp1 = new Point();
    private Point bp1 = new Point();
    private Point tp2 = new Point();
    private Point bp2 = new Point();

    @Override
    protected void paintBorder(Graphics g) {
//        super.paintBorder(g);
//
//        g.setColor(Color.GRAY);
//        g.drawLine(0, 0, getWidth(), 0);
//        g.drawLine(getWidth(), 0, getWidth(), getHeight() - 1);
//        g.drawLine(getWidth(), getHeight() - 1, 0, getHeight() - 1);
//        g.drawLine(0, getHeight() - 1, 0, 0);
    }

    public String getKey() {
        return key;
    }

    protected void makeKey(MappingBlock mapping, String techName) {
        this.mapping = mapping;
        key = MaterialUtils.makeKey(mapping, techName);
    }

    private void adjustCorners(Corner corner, Point tp, Point bp) {
        switch (corner) {
            case LeftTop:
            case TopLeft:
                tp.x -= 1;
                bp.x += 1;
                tp.y += 1;
                bp.y -= 1;
                break;
            case RightBottom:
            case BottomRight:
                tp.x += 1;
                bp.x -= 1;
                tp.y -= 1;
                bp.y += 1;
                break;
            case RightTop:
            case TopRight:
                tp.x -= 1;
                bp.x += 1;
                tp.y -= 1;
                bp.y += 1;
                break;
            case LeftBottom:
            case BottomLeft:
                tp.x += 1;
                bp.x -= 1;
                tp.y += 1;
                bp.y -= 1;
                break;
            case None:
                tp.y -= 1;
                bp.y += 1;
                break;
            case Top:
                tp.x -= 1;
                bp.x += 1;
                break;
            case Bottom:
                tp.x += 1;
                bp.x -= 1;
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (paintDebug) {
            for (int i = 0; i < pointsSize - 1; i++) {
                translate(points[i], p1);
                p1.x -= MARGIN;
                p1.y -= MARGIN;
                translate(points[i + 1], p2);
                p2.x += MARGIN;
                p2.y += MARGIN;
                g.setColor(Color.GRAY);
                g.drawLine(p1.x, p1.y, p2.x, p1.y);
                g.drawLine(p2.x, p1.y, p2.x, p2.y);
                g.drawLine(p2.x, p2.y, p1.x, p2.y);
                g.drawLine(p1.x, p2.y, p1.x, p1.y);


            }

            paintDebug = false;
        }

        for (int i = 0; i < pointsSize - 1; i++) {

            g.setColor(Color.YELLOW);
            translate(points[i], p1);
            translate(points[i + 1], p2);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);


            if (getDiagram().selectedItem == this) {
                g.setColor(Color.CYAN);
            } else {
                g.setColor(Color.GRAY);
            }
            tp1.setLocation(p1);
            bp1.setLocation(p1);
            tp2.setLocation(p2);
            bp2.setLocation(p2);
            adjustCorners(corners[i], tp1, bp1);
            adjustCorners(corners[i + 1], tp2, bp2);
            g.drawLine(tp1.x, tp1.y, tp2.x, tp2.y);
            g.drawLine(bp1.x, bp1.y, bp2.x, bp2.y);

        }

    }
    public final static int MARGIN = 10;

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
            if (startLocation.y <= endLocation.y) {
                corners[0] = Corner.Bottom;
                corners[1] = Corner.BottomRight;
                corners[2] = Corner.None;
            } else {
                corners[0] = Corner.Top;
                corners[1] = Corner.TopRight;
                corners[2] = Corner.None;
            }
        } else if (end.getParamType() == Dot.ParamType.Both) {
            endLocation.x = startLocation.x + MARGIN * 2;
            pointsSize = 3;
            points[0].setLocation(startLocation);
            points[1].x = endLocation.x;
            points[1].y = startLocation.y;
            points[2].setLocation(endLocation);
            if (startLocation.y <= endLocation.y) {
                corners[0] = Corner.None;
                corners[1] = Corner.RightBottom;
                corners[2] = Corner.Bottom;
            } else {
                corners[0] = Corner.None;
                corners[1] = Corner.RightTop;
                corners[2] = Corner.Top;
            }
        } else if (startLocation.x + MARGIN <= endLocation.x - MARGIN) {
            pointsSize = 4;
            points[0].setLocation(startLocation);
            points[1].x = getVMiddleStart();
            points[1].y = startLocation.y;
            points[2].x = getVMiddleStart();
            points[2].y = endLocation.y;
            corners[0] = Corner.None;
            corners[3] = Corner.None;
            points[3].setLocation(endLocation);
            if (startLocation.y <= endLocation.y) {
                corners[1] = Corner.RightBottom;
                corners[2] = Corner.BottomRight;
            } else {
                corners[1] = Corner.RightTop;
                corners[2] = Corner.TopRight;
            }

        } else {
            pointsSize = 6;
            points[0].setLocation(startLocation);
            points[5].setLocation(endLocation);
            points[1].x = getVMiddleStart();
            points[1].y = startLocation.y;

            points[4].x = getVMiddleEnd();
            points[4].y = endLocation.y;
            corners[0] = Corner.None;
            corners[5] = Corner.None;
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

                if (startLocation.y + MARGIN > endLocation.y - MARGIN) {
                    points[3].x = getVMiddleEndClampedLeft();
                    points[4].x = getVMiddleEndClampedLeft();

                } else {
                    points[3].x = getVMiddleEnd();
                    points[4].x = getVMiddleEnd();
                }

                points[3].y = getHBottom();

                corners[1] = Corner.RightBottom;
                corners[2] = Corner.BottomLeft;
                corners[3] = Corner.LeftTop;
                corners[4] = Corner.TopRight;

            } else {

                points[2].x = getVMiddleStart();
                points[2].y = getHMiddle();

                points[3].x = getVMiddleEnd();
                points[3].y = getHMiddle();


                if (startLocation.y <= endLocation.y) {
                    corners[1] = Corner.RightBottom;
                    corners[2] = Corner.BottomLeft;
                    corners[3] = Corner.LeftBottom;
                    corners[4] = Corner.BottomRight;
                } else {
                    corners[1] = Corner.RightTop;
                    corners[2] = Corner.TopLeft;
                    corners[3] = Corner.LeftTop;
                    corners[4] = Corner.TopRight;
                }
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
        MouseEvent me = null;
        me = convertEvent(e);
        getDiagram().dispatchEvent(me);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dispatchEventToDiagram(e);
    }
    private boolean paintDebug = false;

    private void debug() {
        paintDebug = true;
        repaint();
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
        boolean selected = false;
        requestFocusInWindow(true);
        for (int i = 0; i < pointsSize - 1; i++) {
            translate(points[i], p1);
            translate(points[i + 1], p2);
            if (p1.x > p2.x || p1.y > p2.y) {
                tp1.setLocation(p1);
                p1.setLocation(p2);
                p2.setLocation(tp1);
            }

            p1.x -= MARGIN / 2;
            p1.y -= MARGIN / 2;

            p2.x += MARGIN / 2;
            p2.y += MARGIN / 2;


            if (e.getX() >= p1.x && e.getX() <= p2.x
                    && e.getY() >= p1.y && e.getY() <= p2.y) {
                selected = true;
            }
        }

        if (selected) {
            getDiagram().select(this);
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
            if (diag.selectedItem == this) {
                diag.removeSelectedConnection();
            }
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
        MappingBlock mapping = (MappingBlock) evt.getSource();
        key = MaterialUtils.makeKey(mapping, getDiagram().getCurrentTechniqueName());
    }
}
