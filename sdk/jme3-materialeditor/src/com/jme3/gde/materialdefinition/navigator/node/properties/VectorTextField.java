/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node.properties;

import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;

/**
 *
 * @author Nehon
 */
public class VectorTextField extends JTextField implements KeyListener, MouseListener, FocusListener {

    private int index = 0;
    private int capacity = 4;
    private boolean pressed = false;
    private String backup;

    public VectorTextField(int capacity) {
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addFocusListener(this);
        addMouseListener(this);
        setFocusTraversalKeysEnabled(false);
        this.capacity = capacity;
        backup = "[";
        for (int i = 0; i < capacity; i++) {
            backup += "0";
            if (i != capacity - 1) {
                backup += ",";
            }
        }
        backup += "]";
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            if (checkValidity()) {
                backup = getText();
                if (e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
                    index = index - 1;

                } else {
                    index = index + 1;
                }
                if (index == capacity) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
                    return;
                }
                if (index == -1) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
                    return;
                }

            } else {
                setText(backup);
                Logger.getLogger(VectorTextField.class.getName()).log(Level.WARNING, "Invalid format");
            }
            findSelection(0);
        }
    }

    private boolean checkValidity() {
        String[] values = extractValues();
        return values.length == capacity;

    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        init();
        pressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        init();
        index = findSelection(getCaretPosition());
        pressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (!pressed) {
            init();
            findSelection(0);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    private void init() {
        index = 0;
        if (getText().trim().equals("")) {
            setText(backup);
            setCaretPosition(1);
        }

    }

  

    public void clear() {
        setText("");
    }

    private int findSelection(int caretPosition) {
        String[] values = extractValues();
        int start = 0;// = 1;
        int end = 0;// = values[0].length() + start;
        if (caretPosition == getText().length()) {
            caretPosition--;
        }
        int i;
        if (caretPosition != 0) {
            for (i = 0; caretPosition > end; i++) {
                start = end + 1;
                end = values[i].length() + start;
            }
            i--;
        } else {
            for (i = 0; i <= index; i++) {
                start = end + 1;
                end = values[i].length() + start;
            }
        }
        select(start, end);
        return i;
    }

    private String[] extractValues() {
        String text = getText().replaceAll("[\\[\\]]", "");
        String[] values = text.split(",");
        return values;
    }
}