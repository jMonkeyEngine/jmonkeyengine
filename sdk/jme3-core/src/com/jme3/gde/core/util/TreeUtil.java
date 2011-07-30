package com.jme3.gde.core.util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author Nehon
 */
public class TreeUtil {

    public static void createTree(JTree jTree1, String[] leaves) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("", true);
        jTree1.setModel(new DefaultTreeModel(root));
        jTree1.setRootVisible(false);
        DefaultMutableTreeNode currentNode = root;
        Map<String, DefaultMutableTreeNode> pathList = new HashMap<String, DefaultMutableTreeNode>();
        for (int i = 0; i < leaves.length; i++) {

            String[] s = leaves[i].split("/");
            for (int j = 0; j < s.length; j++) {

                DefaultMutableTreeNode node = new DefaultMutableTreeNode(s[j], j != s.length - 1);
                String path = getPath(currentNode.getUserObjectPath()) + getPath(node.getUserObjectPath());
                DefaultMutableTreeNode pathNode = pathList.get(path);

                if (pathNode == null) {
                    pathList.put(path, node);
                    currentNode.add(node);
                } else {
                    node = pathNode;
                }

                currentNode = node;
            }
            currentNode = root;
        }
    }

    /**
     * @param tree com.sun.java.swing.JTree
     * @param start com.sun.java.swing.tree.DefaultMutableTreeNode
     */
    public static void expandTree(JTree tree, TreeNode start, int level) {
        for (Enumeration children = start.children(); children.hasMoreElements();) {
            DefaultMutableTreeNode dtm = (DefaultMutableTreeNode) children.nextElement();
            //System.out.println(dtm.getUserObject()+" "+dtm.getDepth());
            if (!dtm.isLeaf() && dtm.getLevel() <= level) {
                //
                TreePath tp = new TreePath(dtm.getPath());
                tree.expandPath(tp);
                //
                expandTree(tree, dtm, level);
            }
        }
        return;
    }

    public static String getPath(Object[] str) {
        String res = "";
        for (int i = 0; i < str.length; i++) {

            res += str[i].toString() + (i == 0 || i == str.length ? "" : "/");
        }
        return res;
    }

    public static TreePath buildTreePath(JTree tree, TreePath parent, String[] nodes, int startdepth, boolean expandable) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        String o = node.toString();
        


        // If equal, go down the branch
        if (o.equals(nodes[startdepth])) {
            // If at end, return match
            if (startdepth == nodes.length - 1) {
                return parent;
            }

            // Traverse children
            if (node.getChildCount() >= 0) {
                for (Enumeration e = node.children(); e.hasMoreElements();) {
                    TreeNode n = (TreeNode) e.nextElement();     
                        TreePath path = parent.pathByAddingChild(n);
                        if (n.isLeaf() && expandable) {
                            return parent;
                        }
                        TreePath result = buildTreePath(tree, path, nodes, startdepth + 1,expandable);
                        // Found a match
                        if (result != null) {
                            return result;
                        }
                    

                }
            }
        }

        // No match at this branch
        return null;
    }
}
