/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.upgrader;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.upgrade.AutoUpgrade;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class Upgrader {
    private static final Logger logger = Logger.getLogger(Upgrader.class.getName());
    public static void checkUpgrade(){
        try {
            logger.log(Level.INFO, "Start upgrade..");
            AutoUpgrade.main(new String[0]);
            logger.log(Level.INFO, "Finished upgrade.");
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
}
