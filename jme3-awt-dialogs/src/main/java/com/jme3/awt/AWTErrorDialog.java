/*
 * Copyright (c) 2009-2022 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.awt;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Simple dialog for displaying error messages,
 * 
 * @author kwando
 */
public class AWTErrorDialog extends JDialog {
    public static String DEFAULT_TITLE = "Error in application";
    public static int PADDING = 8;
    
    /**
     * Create a new Dialog with a title and a message.
     *
     * @param message the message to display
     * @param title the title to display
     */
    protected AWTErrorDialog(String message, String title) {
        setTitle(title);
        setSize(new Dimension(600, 400));
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);   
        
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        
        JTextArea textArea = new JTextArea();
        textArea.setText(message);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(PADDING, PADDING, PADDING, PADDING));
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        final JDialog dialog = this;
        JButton button = new JButton(new AbstractAction("OK"){
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        add(button, BorderLayout.SOUTH);
    }
    
    protected AWTErrorDialog(String message){
        this(message, DEFAULT_TITLE);
    }
    
    /**
     * Show a dialog with the provided message.
     *
     * @param message the message to display
     */
    public static void showDialog(String message) {
        AWTErrorDialog dialog = new AWTErrorDialog(message);
        dialog.setVisible(true);
    }
}
