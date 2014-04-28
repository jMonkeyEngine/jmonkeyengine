/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.utils;

import com.jme3.gde.materialdefinition.dialog.AddNodeDialog;
import com.jme3.gde.materialdefinition.icons.Icons;
import com.jme3.shader.ShaderNodeDefinition;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 *
 * @author m327836
 */
public class DocFormatter {

    private static DocFormatter instance;
    private Style regStyle = StyleContext.getDefaultStyleContext().
            getStyle(StyleContext.DEFAULT_STYLE);

    private DocFormatter() {
    }

    public static DocFormatter getInstance() {
        if (instance == null) {
            instance = new DocFormatter();
        }
        return instance;
    }

    private void makeStyles(StyledDocument doc) {
        Style s1 = doc.addStyle("regular", regStyle);
        StyleConstants.setFontFamily(s1, "SansSerif");
        Style s2 = doc.addStyle("bold", s1);       
        StyleConstants.setBold(s2, true);
        Style icon = doc.addStyle("input", s1);
        StyleConstants.setAlignment(icon, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(icon, 8);
        StyleConstants.setIcon(icon, Icons.in);
        Style icon2 = doc.addStyle("output", s1);
        StyleConstants.setAlignment(icon2, StyleConstants.ALIGN_CENTER);
        StyleConstants.setSpaceAbove(icon2, 8);
        StyleConstants.setIcon(icon2, Icons.out);


    }

    public static void addDoc(ShaderNodeDefinition def, StyledDocument doc) {

        if (doc.getStyle("regular") == null) {
            getInstance().makeStyles(doc);
        }

        try {
            String[] lines = def.getDocumentation().split("\\n");
            doc.insertString(doc.getLength(), "Shader type : " + def.getType().toString() + "\n", doc.getStyle("regular"));

            for (int i = 0; i < def.getShadersLanguage().size(); i++) {
                doc.insertString(doc.getLength(), "Shader : " + def.getShadersLanguage().get(i) + " " + def.getShadersPath().get(i) + "\n", doc.getStyle("regular"));
            }
//            doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));

            for (String string : lines) {
                String l = string.trim() + "\n";
                if (l.startsWith("@input")) {
                    l = l.substring(6).trim();
                    int spaceIdx = l.indexOf(' ');
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("input"));
                    doc.insertString(doc.getLength(), l.substring(0, spaceIdx), doc.getStyle("bold"));
                    doc.insertString(doc.getLength(), l.substring(spaceIdx), doc.getStyle("regular"));

                } else if (l.startsWith("@output")) {
                    l = l.substring(7).trim();
                    int spaceIdx = l.indexOf(' ');
                    doc.insertString(doc.getLength(), "\n", doc.getStyle("regular"));
                    doc.insertString(doc.getLength(), " ", doc.getStyle("output"));
                    doc.insertString(doc.getLength(), l.substring(0, spaceIdx), doc.getStyle("bold"));
                    doc.insertString(doc.getLength(), l.substring(spaceIdx), doc.getStyle("regular"));

                } else {
                    doc.insertString(doc.getLength(), l, doc.getStyle("regular"));
                }

            }
        } catch (BadLocationException ex) {
            Logger.getLogger(AddNodeDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
