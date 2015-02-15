/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.materialdefinition.fileStructure.leaves.InputMappingBlock;
import com.jme3.gde.materialdefinition.fileStructure.leaves.OutputMappingBlock;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Nehon
 */
public class OutBusPanel extends DraggablePanel implements ComponentListener, Selectable, InOut {

    private Color color = new Color(220, 220, 70);
    private String name = "";
    private final InnerPanel panel;
    private final MatPanel preview;
    private final Shader.ShaderType type;

    public OutBusPanel(String name, Shader.ShaderType type) {
        super(true);
        this.type = type;
        if (type == Shader.ShaderType.Fragment) {
            this.color = new Color(114, 200, 255);
        }
        setBounds(0, 0, 300, 50);
        JLabel title = new JLabel();
        this.name = name;
        title.setFont(new java.awt.Font("Impact", 1, 15)); // NOI18N
        title.setForeground(new java.awt.Color(153, 153, 153));
        title.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        title.setText(name);
        setOpaque(false);
        panel = new InnerPanel();

        javax.swing.GroupLayout outBusPanel1Layout = new javax.swing.GroupLayout(this);
        this.setLayout(outBusPanel1Layout);
        outBusPanel1Layout.setHorizontalGroup(
                outBusPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outBusPanel1Layout.createSequentialGroup()
                .addContainerGap(70, 70)
                .addComponent(panel, 20, 200, Short.MAX_VALUE)
                .addComponent(title, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)));
        outBusPanel1Layout.setVerticalGroup(
                outBusPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(outBusPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(title, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                .addContainerGap())
                .addGroup(outBusPanel1Layout.createSequentialGroup()
                .addContainerGap(20, 20)
                .addComponent(panel, 10, 10, 10)
                .addContainerGap()));

        preview = new MatPanel();
        addComponentListener(preview);

    }

    @Override
    public void setDiagram(final Diagram diagram) {
        super.setDiagram(diagram);
        // preview.setBounds(350,300,128,100);
        diagram.add(preview);
        preview.update(this);
        preview.setExpandActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                diagram.displayBackdrop();
            }
        });
        
    }
    
    public Shader.ShaderType getType(){
        return type;
    }
    
    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
                RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int[] xs = {38, width - 30, width - 30, width, width - 30, width - 30, 38, 38};
        int[] ys = {10, 10, 0, getHeight() / 2, getHeight(), getHeight() - 10, getHeight() - 10, 10};

        Polygon p = new Polygon(xs, ys, 8);

        if (diagram.selectedItem == this) {
            int[] xs2 = {0, width - 30, width - 30, width, width - 32, width - 32, 0, 0};
            int[] ys2 = {10, 10, 0, getHeight() / 2 + 2, getHeight(), getHeight() - 8, getHeight() - 8, 10};

            Polygon p2 = new Polygon(xs2, ys2, 8);
            g.setPaint(new GradientPaint(0, 0, new Color(0.6f, 0.6f, 1.0f, 0.9f), 0, getHeight(), new Color(0.6f, 0.6f, 1.0f, 0.5f)));
            g.fillPolygon(p2);
        }

        Color c1 = new Color(50, 50, 50, 255);
        Color c2 = new Color(50, 50, 50, 80);
        g.setPaint(new GradientPaint(0, 0, c1, width, 0, c2));
        g.fillPolygon(p);
        g.fillRect(0, 10, 3, getHeight() - 20);
        g.fillRect(5, 10, 6, getHeight() - 20);
        g.fillRect(13, 10, 9, getHeight() - 20);
        g.fillRect(24, 10, 12, getHeight() - 20);


    }

    @Override
    public void componentResized(ComponentEvent e) {
        setSize(e.getComponent().getWidth(), 50);
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (dispatchToInnerPanel(e)) {
            return;
        }
        super.mousePressed(e);
        diagram.select(this);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (panel.dragging == false) {
            super.mouseDragged(e);
        }
    }

    protected void draggingDot(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), panel);
        if (panel.contains(p)) {
            MouseEvent me = SwingUtilities.convertMouseEvent(this, e, panel);
            panel.mouseEntered(me);
        } else {
            MouseEvent me = SwingUtilities.convertMouseEvent(this, e, panel);
            panel.mouseExited(me);
        }
    }

    public Dot getConnectPoint() {
        return panel;
    }

    public void updatePreview(Material mat, String technique) {
        if (type == Shader.ShaderType.Fragment) {
            preview.showMaterial(mat,technique);
        } else {
            Material vmat = mat.clone();            
            vmat.getAdditionalRenderState().setWireframe(true);
            preview.showMaterial(vmat,technique);
        }
    }

    @Override
    public String getName() {
        return "Global";
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        diagram.fixSize();
        MouseEvent me = SwingUtilities.convertMouseEvent(this, e, panel);
        panel.mouseReleased(me);
        getDiagram().getEditorParent().savePositionToMetaData(getKey(), 0, getLocation().y);

    }

    public String getKey() {
        return name;
    }

    private boolean dispatchToInnerPanel(MouseEvent e) {
        Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), panel);
        if (panel.contains(p)) {
            MouseEvent me = SwingUtilities.convertMouseEvent(this, e, panel);
            panel.dispatchEvent(me);
            if (me.isConsumed()) {
                return true;
            }
        }
        return false;
    }

    public void addInputMapping(InputMappingBlock block) {
    }

    public void removeInputMapping(InputMappingBlock block) {
    }

    public void addOutputMapping(OutputMappingBlock block) {
    }

    public void removeOutputMapping(OutputMappingBlock block) {
    }

    class InnerPanel extends Dot {

        boolean over = false;
        boolean dragging = false;

        public InnerPanel() {
            this.shaderType = OutBusPanel.this.type;            
            setOpaque(false);
            setNode(OutBusPanel.this);
            setParamType(Dot.ParamType.Both);
            setType("vec4");
            setText(name);
        }

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Color c1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
            Color c2 = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
            g.setPaint(new LinearGradientPaint(0, 0, 0, getHeight(), new float[]{0, 0.5f, 1}, new Color[]{c2, c1, c2}));
            g.fillRoundRect(1, 1, getWidth() - 1, getHeight() - 1, 10, 10);

        }

        @Override
        public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            dragging = true;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            e.consume();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
            dragging = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (!over) {
                super.mouseEntered(e);
                over = true;
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (over) {
                super.mouseExited(e);
                over = false;
            }
        }
    }
}
