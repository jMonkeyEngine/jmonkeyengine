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
