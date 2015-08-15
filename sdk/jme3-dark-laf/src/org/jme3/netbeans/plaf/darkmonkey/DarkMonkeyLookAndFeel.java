/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jme3.netbeans.plaf.darkmonkey;

import com.nilo.plaf.nimrod.NimRODTheme;
import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;

/**
 * The DarkMonkey look and feel class Extends the Nimrod LAF, which in turn,
 * extends Metal.  The version of Nimrod used is 1.2b obtained from: <br/>
 * <a src="http://nilogonzalez.es/nimrodlf/download-en.html">
 * http://nilogonzalez.es/nimrodlf/download-en.html</a>
 * <p> A copy of the jar and source used for this project is in the ext/ folder.
 * </p>
 * 
 * @author Charles Anderson
 */
public class DarkMonkeyLookAndFeel extends com.nilo.plaf.nimrod.NimRODLookAndFeel{
    
    public static final String dmLAFDefault = "DarkMonkey.theme";
    protected static NimRODTheme nrTheme = new NimRODTheme();
    
    public DarkMonkeyLookAndFeel(){
        super();
        // Todo: replace following code with proper loading
        //  From DarkMonkey.theme
        NimRODTheme nt = new NimRODTheme();
            
        nt.setBlack(Color.decode("#E8EAE0"));
        nt.setWhite(Color.decode("#262626"));
        nt.setPrimary1(Color.decode("#77411D"));
        nt.setPrimary2(Color.decode("#9E5F28"));
        nt.setPrimary3(Color.decode("#948519"));
        nt.setSecondary1(Color.decode("#303030"));
        nt.setSecondary2(Color.decode("#3A3A3A"));
        nt.setSecondary3(Color.decode("#515151"));
        nt.setFrameOpacity(180);
        nt.setMenuOpacity(219);
        nt.setFont(Font.decode("DejaVu Sans Condensed-PLAIN-12"));
        
        setCurrentTheme(nt);
        
    }
    
    /**
     * This method override, getID() returns the String "DarkMonkey" for 
     * registering this Look And Feel with the UImanager.
     * @return String "DarkMonkey"
     */
    @Override
    public String getID() {
        return "DarkMonkey";
    }

    /**
     * This method override, getName() returns the String "DarkMonkey" for 
     * its Look and Feel Name. I don't know that this is important, but is
     * overridden anyway, for completion.
     * @return String "DarkMonkey"
     */
    @Override
    public String getName() {
        return "DarkMonkey";
    }

    /**
     * This method override, getDescription() returns the String 
     * "Look and Feel DarkMonkey - 2015, based on NimROD 2007" for 
     * instances of future programming that might use it as a tool tip or 
     * small descriptor in their Look and Feel modules.
     * @return String "Look and Feel DarkMonkey - 2015, based on NimROD 2007"
     */
    @Override
    public String getDescription() {
        return "Look and Feel DarkMonkey - 2015, based on NimROD 2007";
    }
    
       
    @Override
    protected void initClassDefaults( UIDefaults table) {
        super.initClassDefaults( table);
        /*
        for( Enumeration en = table.keys(); en.hasMoreElements(); ) {
            System.out.println( "[" + en.nextElement() + "]");
        }
        */
    }
    
    @Override
    protected void initComponentDefaults( UIDefaults table) {
        super.initComponentDefaults( table);
        
        table.put("Tree.collapsedIcon", DarkMonkeyIconFactory.getTreeCollapsedIcon());
        table.put("Tree.expandedIcon", DarkMonkeyIconFactory.getTreeExpandedIcon());
        // 
        /*
        for( Enumeration en = table.keys(); en.hasMoreElements(); ) {
            System.out.println( "[" + en.nextElement() + "]");
        }
        */
             
    }

}
