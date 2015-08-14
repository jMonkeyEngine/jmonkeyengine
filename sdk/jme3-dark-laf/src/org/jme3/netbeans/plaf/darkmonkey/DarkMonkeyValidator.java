/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jme3.netbeans.plaf.darkmonkey;

import java.awt.EventQueue;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.openide.modules.OnStart;
import org.openide.windows.OnShowing;

/**
 * This is something that would be used once the nbm format is no longer
 * needed and is absorbed by SDK downloadable.
 * @author charles
 */
@OnStart
public class DarkMonkeyValidator implements Runnable{

    @Override
    public void run() {
        
        //assert EventQueue.isDispatchThread(); // this is for @OnShowing
        //JOptionPane.showMessageDialog(null,"Hello from the Validator");
    }
    
}
