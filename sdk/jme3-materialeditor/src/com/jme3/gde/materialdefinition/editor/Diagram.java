/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.editor;

import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.materialdefinition.dialog.AddAttributeDialog;
import com.jme3.gde.materialdefinition.dialog.AddMaterialParameterDialog;
import com.jme3.gde.materialdefinition.dialog.AddNodeDialog;
import com.jme3.gde.materialdefinition.dialog.AddWorldParameterDialog;
import com.jme3.gde.materialdefinition.fileStructure.ShaderNodeBlock;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderNodeDefinition;
import com.jme3.shader.ShaderNodeVariable;
import com.jme3.shader.UniformBinding;
import com.jme3.shader.VarType;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 *
 * @author Nehon
 */
public class Diagram extends JPanel implements MouseListener, MouseMotionListener, ComponentListener {

    protected Dot draggedFrom;
    protected Dot draggedTo;
    protected Selectable selectedItem;
    protected List<Connection> connections = new ArrayList<Connection>();
    protected List<NodePanel> nodes = new ArrayList<NodePanel>();
    protected List<OutBusPanel> outBuses = new ArrayList<OutBusPanel>();
    private MyMenu contextMenu = new MyMenu("Add");
    private MatDefEditorlElement parent;
    private String currentTechniqueName;

    public Diagram() {

        addMouseListener(this);
        addMouseMotionListener(this);
        createPopupMenu();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {

        for (OutBusPanel outBusPanel : outBuses) {
            Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), outBusPanel);
            if (outBusPanel.contains(p)) {
                MouseEvent me = SwingUtilities.convertMouseEvent(this, e, outBusPanel);
                outBusPanel.dispatchEvent(me);
                if (me.isConsumed()) {
                    return;
                }
            }
        }

        for (Connection connection : connections) {
            MouseEvent me = SwingUtilities.convertMouseEvent(this, e, connection);
            connection.select(me);
            if (me.isConsumed()) {
                return;
            }
        }

        selectedItem = null;
        repaint();
    }

    public void refreshPreviews(Material mat) {
        for (OutBusPanel outBusPanel : outBuses) {
            outBusPanel.updatePreview(mat);
        }
    }
    Point clickLoc = new Point(0, 0);

    @Override
    public void mouseReleased(MouseEvent e) {
        if (draggedFrom != null && draggedFrom.getNode() instanceof OutBusPanel) {
            MouseEvent me = SwingUtilities.convertMouseEvent(this, e, draggedFrom.getNode());
            draggedFrom.getNode().dispatchEvent(me);
            if (me.isConsumed()) {
                return;
            }
        }
        if (e.getButton() != MouseEvent.BUTTON3) {
            dispatchToOutBuses(e);
        } else {
            contextMenu.show(this, e.getX(), e.getY());
            clickLoc.setLocation(e.getX(), e.getY());
        }

    }

    public MatDefEditorlElement getEditorParent() {
        return parent;
    }

    public void addConnection(Connection conn) {
        connections.add(conn);
        add(conn);
        for (OutBusPanel bus : outBuses) {
            setComponentZOrder(bus, getComponentCount() - 1);
        }
        repaint();
    }

    public void notifyMappingCreation(Connection conn) {
        parent.makeMapping(conn);
    }

    public void addNode(NodePanel node) {
        add(node);
        node.setTechName(currentTechniqueName);
        node.setDiagram(this);
        nodes.add(node);
        setComponentZOrder(node, 0);
        node.addComponentListener(this);
    }

    public void addOutBus(OutBusPanel bus) {
        outBuses.add(bus);
        bus.setDiagram(this);
        add(bus);
        setComponentZOrder(bus, getComponentCount() - 1);
        addComponentListener(bus);
        bus.componentResized(new ComponentEvent(this, ActionEvent.ACTION_PERFORMED));
        bus.revalidate();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    protected void removeSelectedConnection() {
        if (selectedItem instanceof Connection) {
            Connection selectedConnection = (Connection) selectedItem;
            removeConnection(selectedConnection);
            selectedItem = null;
            parent.notifyRemoveConnection(selectedConnection);
        }
    }

    private String fixNodeName(String name) {
        return fixNodeName(name, 0);
    }

    private String fixNodeName(String name, int count) {
        for (NodePanel nodePanel : nodes) {
            if ((name + (count == 0 ? "" : count)).equals(nodePanel.getName())) {
                return fixNodeName(name, count + 1);
            }
        }
        return name + (count == 0 ? "" : count);
    }

    public void addNodesFromDefs(List<ShaderNodeDefinition> defList, String path, Point clickPosition) {
        int i = 0;
        for (ShaderNodeDefinition def : defList) {
            ShaderNodeBlock sn = new ShaderNodeBlock(def, path);
            sn.setName(fixNodeName(sn.getName()));

            NodePanel np = new NodePanel(sn, def);
            addNode(np);
            np.setLocation(clickPosition.x + i * 150, clickPosition.y);
            sn.setSpatialOrder(np.getLocation().x);
            i++;
            np.revalidate();
            getEditorParent().notifyAddNode(sn, def);
        }
        repaint();
    }

    public void addMatParam(String type, String name, Point point) {
        String fixedType = type;
        if (type.equals("Color")) {
            fixedType = "Vector4";
        }
        ShaderNodeVariable param = new ShaderNodeVariable(VarType.valueOf(fixedType).getGlslType(), name);
        NodePanel np = new NodePanel(param, NodePanel.NodeType.MatParam);
        addNode(np);
        np.setLocation(point.x, point.y);
        np.revalidate();
        repaint();
        getEditorParent().notifyAddMapParam(type, name);
    }

    public void addWorldParam(UniformBinding binding, Point point) {

        ShaderNodeVariable param = new ShaderNodeVariable(binding.getGlslType(), binding.name());
        NodePanel np = new NodePanel(param, NodePanel.NodeType.WorldParam);
        addNode(np);
        np.setLocation(point.x, point.y);
        np.revalidate();
        repaint();
        getEditorParent().notifyAddWorldParam(binding.name());
    }

    public void addAttribute(String name, String type, Point point) {
        ShaderNodeVariable param = new ShaderNodeVariable(type, "Attr", name);
        NodePanel np = new NodePanel(param, NodePanel.NodeType.Attribute);
        addNode(np);
        np.setLocation(point.x, point.y);
        np.revalidate();
        repaint();
    }

    protected void removeSelectedNode() {
        if (selectedItem instanceof NodePanel) {
            int result = JOptionPane.showConfirmDialog(null, "Delete this node and all its mappings?", "Delete Shader Node", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                NodePanel selectedNode = (NodePanel) selectedItem;
                nodes.remove(selectedNode);
                for (Iterator<Connection> it = connections.iterator(); it.hasNext();) {
                    Connection conn = it.next();
                    if (conn.start.getNode() == selectedNode || conn.end.getNode() == selectedNode) {
                        it.remove();
                        conn.end.disconnect();
                        conn.start.disconnect();
                        remove(conn);
                    }
                }

                remove(selectedNode);
                selectedItem = null;
                repaint();
                parent.notifyRemoveNode(selectedNode);
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggedFrom == null) {
            if (selectedItem instanceof OutBusPanel) {
                OutBusPanel bus = (OutBusPanel) selectedItem;
                MouseEvent me = SwingUtilities.convertMouseEvent(this, e, bus);
                bus.dispatchEvent(me);
            }
        }
    }

    protected void draggingDot(MouseEvent e) {
        for (OutBusPanel outBusPanel : outBuses) {
            Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), outBusPanel);
            if (outBusPanel.contains(p)) {
                MouseEvent me = SwingUtilities.convertMouseEvent(this, e, outBusPanel);
                outBusPanel.draggingDot(me);
                if (me.isConsumed()) {
                    return;
                }
            }
        }
    }

    public Connection connect(Dot start, Dot end) {
        Connection conn = new Connection(start, end);
        start.connect(conn);
        end.connect(conn);


        addConnection(conn);

        return conn;
    }

    public NodePanel getNodePanel(String key) {
        for (NodePanel nodePanel : nodes) {
            if (nodePanel.getKey().equals(key)) {
                return nodePanel;
            }
        }
        return null;
    }

    public OutBusPanel getOutBusPanel(String key) {
        for (OutBusPanel out : outBuses) {
            if (out.getKey().equals(key)) {
                return out;
            }
        }
        return null;
    }

    /**
     * selection from the editor. Select the item and notify the topComponent
     *
     * @param selectable
     */
    public void select(Selectable selectable) {
        parent.selectionChanged(doSelect(selectable));
    }

    /**
     * do select the item and repaint the diagram
     *
     * @param selectable
     * @return
     */
    private Selectable doSelect(Selectable selectable) {
        this.selectedItem = selectable;
        if (selectable instanceof Component) {
            ((Component) selectable).requestFocusInWindow();
        }
        repaint();
        return selectable;
    }

    /**
     * find the item with the given key and select it without notifying the
     * topComponent
     *
     * @param key
     * @return
     */
    public Selectable select(String key) {

        for (NodePanel nodePanel : nodes) {
            if (nodePanel.getKey().equals(key)) {
                return doSelect(nodePanel);
            }
        }

        for (Connection connection : connections) {
            if (connection.getKey().equals(key)) {
                return doSelect(connection);
            }
        }

        for (OutBusPanel outBusPanel : outBuses) {
            if (outBusPanel.getKey().equals(key)) {
                return doSelect(outBusPanel);
            }
        }
        return doSelect(null);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        dispatchToOutBuses(e);
    }

    private JMenuItem createMenuItem(String text, Icon icon) {
        JMenuItem item = new JMenuItem(text, icon);
        item.setFont(new Font("Tahoma", 1, 10)); // NOI18N
        return item;
    }

    
    public void clear(){
        removeAll();
        outBuses.clear();
        connections.clear();
        nodes.clear();
    }
    
    private void createPopupMenu() {
        contextMenu.setFont(new Font("Tahoma", 1, 10)); // NOI18N
        contextMenu.setOpaque(true);
        Border titleUnderline = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK);
        TitledBorder labelBorder = BorderFactory.createTitledBorder(
                titleUnderline, contextMenu.getLabel(),
                TitledBorder.LEADING, TitledBorder.ABOVE_TOP, contextMenu.getFont(), Color.BLACK);

        contextMenu.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        contextMenu.setBorder(BorderFactory.createCompoundBorder(contextMenu.getBorder(),
                labelBorder));


        JMenuItem nodeItem = createMenuItem("Node", Icons.node);
        nodeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddNodeDialog d = new AddNodeDialog(null, true, parent.obj.getLookup().lookup(ProjectAssetManager.class), Diagram.this, clickLoc);
                d.setLocationRelativeTo(null);
                d.setVisible(true);
            }
        });

        contextMenu.add(nodeItem);
        contextMenu.add(createSeparator());
        JMenuItem matParamItem = createMenuItem("Material Parameter", Icons.mat);
        matParamItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddMaterialParameterDialog d = new AddMaterialParameterDialog(null, true, Diagram.this, clickLoc);
                d.setLocationRelativeTo(null);
                d.setVisible(true);
            }
        });
        contextMenu.add(matParamItem);
        JMenuItem worldParamItem = createMenuItem("World Parameter", Icons.world);
        worldParamItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddWorldParameterDialog d = new AddWorldParameterDialog(null, true, Diagram.this, clickLoc);
                d.setLocationRelativeTo(null);
                d.setVisible(true);
            }
        });
        contextMenu.add(worldParamItem);
        JMenuItem attributeItem = createMenuItem("Attribute", Icons.attrib);
        attributeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddAttributeDialog d = new AddAttributeDialog(null, true, Diagram.this, clickLoc);
                d.setLocationRelativeTo(null);
                d.setVisible(true);
            }
        });
        contextMenu.add(attributeItem);
        contextMenu.add(createSeparator());
        JMenuItem outputItem = createMenuItem("Output color", Icons.output);
        contextMenu.add(outputItem);
        outputItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                OutBusPanel p2 = new OutBusPanel("color" + (outBuses.size() - 1), Shader.ShaderType.Fragment);
                p2.setBounds(0, 350 + 50 * (outBuses.size() - 1), p2.getWidth(), p2.getHeight());

                addOutBus(p2);

            }
        });
    }

    private JSeparator createSeparator() {
        JSeparator jsep = new JSeparator(JSeparator.HORIZONTAL);
        jsep.setBackground(Color.BLACK);
        return jsep;
    }

    private void dispatchToOutBuses(MouseEvent e) {
        for (OutBusPanel outBusPanel : outBuses) {
            Point p = SwingUtilities.convertPoint(this, e.getX(), e.getY(), outBusPanel);
            if (outBusPanel.contains(p)) {
                MouseEvent me = SwingUtilities.convertMouseEvent(this, e, outBusPanel);
                outBusPanel.dispatchEvent(me);
                if (me.isConsumed()) {
                    return;
                }
            }
        }
    }

    private void removeConnection(Connection selectedConnection) {
        connections.remove(selectedConnection);
        selectedConnection.end.disconnect();
        selectedConnection.start.disconnect();
        remove(selectedConnection);
    }

    private class MyMenu extends JPopupMenu {

        public MyMenu(String label) {
            super(label);
        }

        @Override
        protected void paintComponent(Graphics g) {
            //Color c1 = new Color(100, 100, 100, 255);
            Color bg = new Color(175, 175, 175);
            g.setColor(bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void fixSize() {
        int maxWidth = minWidth;
        int maxHeight = minHeight;

        for (NodePanel nodePanel : nodes) {
            int w = nodePanel.getLocation().x + nodePanel.getWidth() + 150;
            if (w > maxWidth) {
                maxWidth = w;
            }
            int h = nodePanel.getLocation().y + nodePanel.getHeight();
            if (h > maxHeight) {
                maxHeight = h;
            }
        }
        for (OutBusPanel outBusPanel : outBuses) {
            int h = outBusPanel.getLocation().y + outBusPanel.getHeight();
            if (h > maxHeight) {
                maxHeight = h;
            }
        }
        setPreferredSize(new Dimension(maxWidth, maxHeight));
        revalidate();
    }
    int minWidth = 0;
    int minHeight = 0;

    public void componentResized(ComponentEvent e) {
        minWidth = e.getComponent().getWidth() - 2;
        minHeight = e.getComponent().getHeight() - 2;
        fixSize();
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    public void setParent(MatDefEditorlElement parent) {
        this.parent = parent;
    }

    public void setCurrentTechniqueName(String currentTechniqueName) {
        this.currentTechniqueName = currentTechniqueName;
    }

    public String getCurrentTechniqueName() {
        return currentTechniqueName;
    }
}
