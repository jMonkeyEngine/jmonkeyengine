/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.completion;

import com.jme3.gde.core.assets.ProjectAssetManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author normenhansen
 */
@MimeRegistration(mimeType = "text/x-java", service = CompletionProvider.class)
public class AssetCompletionProvider implements CompletionProvider {

    private static ImageIcon assetIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/assets.gif"));
    private static ImageIcon modelIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/model.gif"));
    private static ImageIcon soundIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/sound.gif"));
    private static ImageIcon materialIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/material.gif"));
    private static ImageIcon matDefIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/jme-logo.png"));
    private static ImageIcon fontIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/font.gif"));
    private static ImageIcon filterIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/filters/icons/eye.gif"));
    private static ImageIcon textureIcon =
            new ImageIcon(ImageUtilities.loadImage("com/jme3/gde/core/assets/nodes/icons/image.gif"));

    private enum AssetType {

        Invalid, Model, Material, Filter, MatDef, Texture, Sound, Font, Xml, Asset
    }

    public AssetCompletionProvider() {
    }

    @Override
    public CompletionTask createTask(int queryType, JTextComponent jtc) {
        if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {

                ProjectAssetManager manager = getProjectAssetManager(document);
                if (manager == null) {
                    Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "No assetManager found");
                    completionResultSet.finish();
                    return;
                }
                AssetType type = determineType(document, caretOffset);
                String filter = null;
                int startOffset = caretOffset - 1;
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    final int lineStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
                    final char[] line = bDoc.getText(lineStartOffset, caretOffset - lineStartOffset).toCharArray();
                    final int whiteOffset = indexOfInsertion(line);
                    filter = new String(line, whiteOffset + 1, line.length - whiteOffset - 1);
                    if (whiteOffset > 0) {
                        startOffset = lineStartOffset + whiteOffset + 1;
                    } else {
                        startOffset = lineStartOffset;
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Searching with filter {0}", filter);
                switch (type) {
                    case Model:
                        for (String string : manager.getModels()) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Material:
                        for (String string : manager.getMaterials()) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case MatDef:
                        for (String string : manager.getMatDefs()) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Texture:
                        for (String string : manager.getTextures()) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Sound:
                        for (String string : manager.getSounds()) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Font:
                        for (String string : manager.getFilesWithSuffix("fnt")) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Filter:
                        for (String string : manager.getFilesWithSuffix("j3f")) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Xml:
                        for (String string : manager.getFilesWithSuffix("xml")) {
                            if (string.startsWith(filter)) {
                                completionResultSet.addItem(new AssetCompletionItem(type, string, startOffset, caretOffset));
                            }
                        }
                        break;
                    case Invalid:
                        Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Not a valid code line for assets");
                        break;
                }
                completionResultSet.finish();
            }
        }, jtc);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String string) {
        return 0;
    }

    private AssetType determineType(Document document, int caretOffset) {
        try {
            final StyledDocument bDoc = (StyledDocument) document;
            final int lineStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
            final String line = bDoc.getText(lineStartOffset, caretOffset - lineStartOffset).trim();
            //TODO: more intelligence! :)
            if (hasLastCommand(line, ".loadModel(\"")) {
                return AssetType.Model;
            } else if (hasLastCommand(line, ".loadMaterial(\"")) {
                return AssetType.Material;
            } else if (hasLastCommand(line, ".loadFilter(\"")) {
                return AssetType.Filter;
            } else if (hasLastCommand(line, ".loadTexture(\"")) {
                return AssetType.Texture;
            } else if (hasLastCommand(line, ".loadSound(\"")) {
                return AssetType.Sound;
            } else if (hasLastCommand(line, ".loadFont(\"")) {
                return AssetType.Font;
            } else if (hasLastCommand(line, "new Material(")) {
                return AssetType.MatDef;
            } else if (hasLastCommand(line, "new AudioNode(")) {
                return AssetType.Sound;
            } else if (hasLastCommand(line, ".addXml(\"")) {
                return AssetType.Xml;
            } else if (hasLastCommand(line, ".fromXml(\"")) {
                return AssetType.Xml;
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return AssetType.Invalid;
    }

    private boolean hasLastCommand(String line, String command) {
        int idx = line.lastIndexOf(command);
        if (idx != -1) {
            int bIdx = line.indexOf(")", idx);
            if (bIdx == -1) {
                return true;
            }
        }
        return false;
    }

    private ProjectAssetManager getProjectAssetManager(Document doc) {
        Object sdp = doc.getProperty(Document.StreamDescriptionProperty);
        if (sdp instanceof FileObject) {
            Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Check FileObject for Project..");
            FileObject fobj = (FileObject) sdp;
            Project proj = FileOwnerQuery.getOwner(fobj);
            if (proj != null) {
                Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Project found, return ProjectAssetManager");
                return proj.getLookup().lookup(ProjectAssetManager.class);
            }
        }
        if (sdp instanceof DataObject) {
            Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Check DataObject for Project..");
            DataObject dobj = (DataObject) sdp;
            FileObject fobj = dobj.getPrimaryFile();
            Project proj = FileOwnerQuery.getOwner(fobj);
            if (proj != null) {
                Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "Project found, return ProjectAssetManager");
                return proj.getLookup().lookup(ProjectAssetManager.class);
            }
        }
        Logger.getLogger(AssetCompletionProvider.class.getName()).log(Level.FINE, "No Project found");
        return null;
    }

    private static int getRowFirstNonWhite(StyledDocument doc, int offset)
            throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1)
                        + ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    private static int indexOfWhite(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfInsertion(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (c == '"') {
                return i;
            }
        }
        return 0;
    }

    public static class AssetCompletionItem implements CompletionItem {

        private AssetType type;
        private String text;
        private static Color fieldColor = Color.decode("0x0000B2");
        private int dotOffset;
        private int caretOffset;

        public AssetCompletionItem(AssetType type, String text, int dotOffset, int caretOffset) {
            this.type = type;
            this.text = text;
            this.caretOffset = caretOffset;
            this.dotOffset = dotOffset;
        }

        @Override
        public int getPreferredWidth(Graphics graphics, Font font) {
            return CompletionUtilities.getPreferredWidth(text, null, graphics, font);
        }

        @Override
        public void render(Graphics g, Font defaultFont, Color defaultColor,
                Color backgroundColor, int width, int height, boolean selected) {
            ImageIcon icon = null;
            switch (type) {
                case Model:
                    icon = modelIcon;
                    break;
                case Material:
                    icon = materialIcon;
                    break;
                case MatDef:
                    icon = matDefIcon;
                    break;
                case Texture:
                    icon = textureIcon;
                    break;
                case Sound:
                    icon = soundIcon;
                    break;
                case Font:
                    icon = fontIcon;
                    break;
                case Filter:
                    icon = filterIcon;
                    break;
                case Asset:
                    icon = assetIcon;
                    break;
                case Invalid:
                    break;
                default:
                    //icon = assetIcon;
            }
            CompletionUtilities.renderHtml(icon, text, null, g, defaultFont,
                    (selected ? Color.white : fieldColor), width, height, selected);
        }

        @Override
        public CharSequence getSortText() {
            return text;
        }

        @Override
        public CharSequence getInsertPrefix() {
            return text;
        }

        @Override
        public void defaultAction(JTextComponent component) {
            try {
                StyledDocument doc = (StyledDocument) component.getDocument();
                //Here we remove the characters starting at the start offset
                //and ending at the point where the caret is currently found:
                doc.remove(dotOffset, caretOffset - dotOffset);
                doc.insertString(dotOffset, text, null);
                Completion.get().hideAll();
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        public void processKeyEvent(KeyEvent evt) {
        }

        @Override
        public CompletionTask createDocumentationTask() {
//            return new AsyncCompletionTask(new AsyncCompletionQuery() {
//                @Override
//                protected void query(CompletionResultSet completionResultSet, Document document, int i) {
//                    completionResultSet.setDocumentation(new CountriesCompletionDocumentation(CountriesCompletionItem.this));
//                    completionResultSet.finish();
//                }
//            });
            return null;
        }

        @Override
        public CompletionTask createToolTipTask() {
//            return new AsyncCompletionTask(new AsyncCompletionQuery() {
//                @Override
//                protected void query(CompletionResultSet completionResultSet, Document document, int i) {
//                    JToolTip toolTip = new JToolTip();
//                    toolTip.setTipText("Press Enter to insert \"" + text + "\"");
//                    completionResultSet.setToolTip(toolTip);
//                    completionResultSet.finish();
//                }
//            });
            return null;
        }

        public boolean instantSubstitution(JTextComponent component) {
            return false;
        }

        public int getSortPriority() {
            return 0;
        }
    }
}