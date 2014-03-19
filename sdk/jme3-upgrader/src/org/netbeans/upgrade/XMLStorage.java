/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.upgrade;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;

import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


public class XMLStorage {

    private static final Map<Color,String> colorToName = new HashMap<Color,String> ();
    private static final Map<String, Color> nameToColor = new HashMap<String, Color> ();
    private static final Map<String, Integer> nameToFontStyle = new HashMap<String, Integer> ();
    private static final Map<Integer, String> fontStyleToName = new HashMap<Integer, String> ();
    static {
        colorToName.put (Color.black, "black");
        nameToColor.put ("black", Color.black);
        colorToName.put (Color.blue, "blue");
        nameToColor.put ("blue", Color.blue);
        colorToName.put (Color.cyan, "cyan");
        nameToColor.put ("cyan", Color.cyan);
        colorToName.put (Color.darkGray, "darkGray");
        nameToColor.put ("darkGray", Color.darkGray);
        colorToName.put (Color.gray, "gray");
        nameToColor.put ("gray", Color.gray);
        colorToName.put (Color.green, "green");
        nameToColor.put ("green", Color.green);
        colorToName.put (Color.lightGray, "lightGray");
        nameToColor.put ("lightGray", Color.lightGray);
        colorToName.put (Color.magenta, "magenta");
        nameToColor.put ("magenta", Color.magenta);
        colorToName.put (Color.orange, "orange");
        nameToColor.put ("orange", Color.orange);
        colorToName.put (Color.pink, "pink");
        nameToColor.put ("pink", Color.pink);
        colorToName.put (Color.red, "red");
        nameToColor.put ("red", Color.red);
        colorToName.put (Color.white, "white");
        nameToColor.put ("white", Color.white);
        colorToName.put (Color.yellow, "yellow");
        nameToColor.put ("yellow", Color.yellow);
        
        nameToFontStyle.put ("plain", Integer.valueOf (Font.PLAIN));
        fontStyleToName.put (Integer.valueOf (Font.PLAIN), "plain");
        nameToFontStyle.put ("bold", Integer.valueOf (Font.BOLD));
        fontStyleToName.put (Integer.valueOf (Font.BOLD), "bold");
        nameToFontStyle.put ("italic", Integer.valueOf (Font.ITALIC));
        fontStyleToName.put (Integer.valueOf (Font.ITALIC), "italic");
        nameToFontStyle.put ("bold+italic", Integer.valueOf (Font.BOLD + Font.ITALIC));
        fontStyleToName.put (Integer.valueOf (Font.BOLD + Font.ITALIC), "bold+italic");
    }
    
    static String colorToString (Color color) {
	if (colorToName.containsKey (color))
	    return (String) colorToName.get (color);
	return Integer.toHexString (color.getRGB ());
    }
    
    static Color stringToColor (String color) throws Exception {
        if (color.startsWith ("#")) 
            color = color.substring (1);
	if (nameToColor.containsKey (color))
	    return (Color) nameToColor.get (color);
        try {
            return new Color ((int) Long.parseLong (color, 16));
        } catch (NumberFormatException ex) {
            throw new Exception ();
        }
    }
    
    
    // generics support methods ................................................
    
    private static RequestProcessor requestProcessor = new RequestProcessor ("XMLStorage");
    
    static void save (final FileObject fo, final String content) {
        if (fo == null) throw new NullPointerException ();
        if (content == null) throw new NullPointerException ();
        requestProcessor.post (new Runnable () {
            public void run () {
                try {
                    FileLock lock = fo.lock ();
                    try {
                        OutputStream os = fo.getOutputStream (lock);
                        Writer writer = new OutputStreamWriter (os, "UTF-8"); // NOI18N
                        try {
                            writer.write (content);
                        } finally {
                            writer.close ();
                        } 
                    } finally {
                        lock.releaseLock ();
                    }
                } catch (IOException ex) {
                    ErrorManager.getDefault ().notify (ex);
                }
            }
        });
    }
    
    static Object load (InputStream is, String name, Handler handler) {
        try {
            try {
                XMLReader reader = XMLUtil.createXMLReader ();
                reader.setEntityResolver (handler);
                reader.setContentHandler (handler);
                reader.parse (new InputSource (is));
                return handler.getResult ();
            } finally {
                is.close ();
            }
        } catch (SAXException ex) {
	    if (System.getProperty ("org.netbeans.optionsDialog") != null) {
                System.out.println("File: " + name);
                ex.printStackTrace ();
            }
            return handler.getResult ();
        } catch (IOException ex) {
	    if (System.getProperty ("org.netbeans.optionsDialog") != null) {
                System.out.println("File: " + name);
                ex.printStackTrace ();
            }
            return handler.getResult ();
	} catch (Exception ex) {
	    if (System.getProperty ("org.netbeans.optionsDialog") != null) {
                System.out.println("File: " + name);
                ex.printStackTrace ();
            }
            return handler.getResult ();
        }
    }
    
    static StringBuffer generateHeader () {
        StringBuffer sb = new StringBuffer ();
        sb.append ("<?xml version=\"1.0\"?>\n\n");
        return sb;
    }
    
    static void generateFolderStart (
        StringBuffer sb, 
        String name, 
        Attribs attributes, 
        String indentation
    ) {
        sb.append (indentation).append ('<').append (name);
        if (attributes != null) {
            if (!attributes.oneLine) sb.append ('\n');
            else sb.append (' ');
            generateAttributes (sb, attributes, indentation + "    ");
            if (!attributes.oneLine) sb.append (indentation);
            sb.append (">\n");
        } else
            sb.append (">\n");
    }
    
    static void generateFolderEnd (StringBuffer sb, String name, String indentation) {
        sb.append (indentation).append ("</").append (name).append (">\n");
    }
    
    static void generateLeaf (
        StringBuffer sb, 
        String name, 
        Attribs attributes, 
        String indentation
    ) {
        sb.append (indentation).append ('<').append (name);
        if (attributes != null) {
            if (!attributes.oneLine) sb.append ('\n');
            else sb.append (' ');
            generateAttributes (sb, attributes, indentation + "    ");
            if (!attributes.oneLine) sb.append (indentation);
            sb.append ("/>\n");
        } else
            sb.append ("/>\n");
    }
    
    private static void generateAttributes (
        StringBuffer sb, 
        Attribs attributes, 
        String indentation
    ) {
        if (attributes == null) return;
        int i, k = attributes.names.size ();
        for (i = 0; i < k; i++) {
            if (!attributes.oneLine)
                sb.append (indentation);
            sb.append (attributes.names.get (i)).append ("=\"").
                append (attributes.values.get (i)).append ('\"');
            if (!attributes.oneLine)
                sb.append ("\n");
            else
            if (i < (k - 1))
                sb.append (' ');
        }
    }
    
    static class Handler extends DefaultHandler {
        private Object result;
        void setResult (Object result) {
            this.result = result;
        }
        Object getResult () {
            return result;
        }
    }
    
    static class Attribs {
        private List<String> names = new ArrayList<String> ();
        private List<String> values = new ArrayList<String> ();
        private boolean oneLine;
        
        Attribs (boolean oneLine) {
            this.oneLine = oneLine;
        }
        
        void add (String name, String value) {
            int i = names.indexOf (name);
            if (i >= 0) {
                names.remove (i);
                values.remove (i);
            }            names.add (name);
            values.add (value);
        }
        
        void clear () {
            names.clear ();
            values.clear ();
        }
    }
}
