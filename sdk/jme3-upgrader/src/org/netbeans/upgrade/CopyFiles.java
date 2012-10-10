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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.netbeans.util.Util;
import org.openide.filesystems.FileUtil;
import org.openide.util.EditableProperties;

/** Does copy of files according to include/exclude patterns.
 *
 * @author Jiri Skrivanek
 */
final class CopyFiles extends Object {

    private File sourceRoot;
    private File targetRoot;
    private EditableProperties currentProperties;
    private Set<String> includePatterns = new HashSet<String>();
    private Set<String> excludePatterns = new HashSet<String>();
    private static final Logger LOGGER = Logger.getLogger(CopyFiles.class.getName());

    private CopyFiles(File source, File target, File patternsFile) {
        this.sourceRoot = source;
        this.targetRoot = target;
        try {
            InputStream is = new FileInputStream(patternsFile);
            Reader reader = new InputStreamReader(is, "utf-8"); // NOI18N
            readPatterns(reader);
            reader.close();
        } catch (IOException ex) {
            // show error message and continue
            JDialog dialog = Util.createJOptionDialog(new JOptionPane(ex, JOptionPane.ERROR_MESSAGE), ex.getMessage());
            dialog.setVisible(true);
            return;
        }
    }

    public static void copyDeep(File source, File target, File patternsFile) throws IOException {
        CopyFiles copyFiles = new CopyFiles(source, target, patternsFile);
        LOGGER.fine("Copying from: " + copyFiles.sourceRoot + "\nto: " + copyFiles.targetRoot);  //NOI18N
        copyFiles.copyFolder(copyFiles.sourceRoot);
    }

    private void copyFolder(File sourceFolder) throws IOException {
        File[] srcChildren = sourceFolder.listFiles();
        if (srcChildren == null) {
            LOGGER.info(sourceFolder + " is not a directory or is invalid.");  //NOI18N
            return ;
        }
        for (File child : srcChildren) {
            if (child.isDirectory()) {
                copyFolder(child);
            } else {
                copyFile(child);
            }
        }
    }

    /** Returns slash separated path relative to given root. */
    private static String getRelativePath(File root, File file) {
        String result = file.getAbsolutePath().substring(root.getAbsolutePath().length());
        result = result.replace('\\', '/');  //NOI18N
        if (result.startsWith("/") && !result.startsWith("//")) {  //NOI18N
            result = result.substring(1);
        }
        return result;
    }

    /** Copy source file to target file. It creates necessary sub folders.
     * @param sourceFile source file
     * @param targetFile target file
     * @throws java.io.IOException if copying fails
     */
    private static void copyFile(File sourceFile, File targetFile) throws IOException {
        ensureParent(targetFile);
        InputStream ins = null;
        OutputStream out = null;
        try {
            ins = new FileInputStream(sourceFile);
            out = new FileOutputStream(targetFile);
            FileUtil.copy(ins, out);
        } finally {
            if (ins != null) {
                ins.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    /** Copy given file to target root dir if matches include/exclude patterns.
     * If properties pattern is applicable, it copies only matching keys.
     * @param sourceFile source file
     * @throws java.io.IOException if copying fails
     */
    private void copyFile(File sourceFile) throws IOException {
        String relativePath = getRelativePath(sourceRoot, sourceFile);
        currentProperties = null;
        boolean includeFile = false;
        Set<String> includeKeys = new HashSet<String>();
        Set<String> excludeKeys = new HashSet<String>();
        for (String pattern : includePatterns) {
            if (pattern.contains("#")) {  //NOI18N
                includeKeys.addAll(matchingKeys(relativePath, pattern));
            } else {
                if (relativePath.matches(pattern)) {
                    includeFile = true;
                    includeKeys.clear();  // include entire file
                    break;
                }
            }
        }
        if (includeFile || !includeKeys.isEmpty()) {
            // check excludes
            for (String pattern : excludePatterns) {
                if (pattern.contains("#")) {  //NOI18N
                    excludeKeys.addAll(matchingKeys(relativePath, pattern));
                } else {
                    if (relativePath.matches(pattern)) {
                        includeFile = false;
                        includeKeys.clear();  // exclude entire file
                        break;
                    }
                }
            }
        }
        LOGGER.log(Level.FINEST, "{0}, includeFile={1}, includeKeys={2}, excludeKeys={3}", new Object[]{relativePath, includeFile, includeKeys, excludeKeys});  //NOI18N
        if (!includeFile && includeKeys.isEmpty()) {
            // nothing matches
            return;
        }

        File targetFile = new File(targetRoot, relativePath);
        LOGGER.log(Level.FINE, "Path: {0}", relativePath);  //NOI18N
        if (includeKeys.isEmpty() && excludeKeys.isEmpty()) {
            // copy entire file
            copyFile(sourceFile, targetFile);
        } else {
            if (!includeKeys.isEmpty()) {
                currentProperties.keySet().retainAll(includeKeys);
            }
            currentProperties.keySet().removeAll(excludeKeys);
            // copy just selected keys
            LOGGER.log(Level.FINE, "  Only keys: {0}", currentProperties.keySet());
            OutputStream out = null;
            try {
                ensureParent(targetFile);
                out = new FileOutputStream(targetFile);
                currentProperties.store(out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /** Returns set of keys matching given pattern.
     * @param relativePath path relative to sourceRoot
     * @param propertiesPattern pattern like file.properties#keyPattern
     * @return set of matching keys, never null
     * @throws IOException if properties cannot be loaded
     */
    private Set<String> matchingKeys(String relativePath, String propertiesPattern) throws IOException {
        Set<String> matchingKeys = new HashSet<String>();
        String[] patterns = propertiesPattern.split("#", 2);
        String filePattern = patterns[0];
        String keyPattern = patterns[1];
        if (relativePath.matches(filePattern)) {
            if (currentProperties == null) {
                currentProperties = getProperties(relativePath);
            }
            for (String key : currentProperties.keySet()) {
                if (key.matches(keyPattern)) {
                    matchingKeys.add(key);
                }
            }
        }
        return matchingKeys;
    }

    /** Returns properties from relative path.
     * @param relativePath relative path
     * @return properties from relative path.
     * @throws IOException if cannot open stream
     */
    private EditableProperties getProperties(String relativePath) throws IOException {
        EditableProperties properties = new EditableProperties(false);
        InputStream in = null;
        try {
            in = new FileInputStream(new File(sourceRoot, relativePath));
            properties.load(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return properties;
    }

    /** Creates parent of given file, if doesn't exist. */
    private static void ensureParent(File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Cannot create folder: " + parent.getAbsolutePath());  //NOI18N
            }
        }
    }

    /** Reads the include/exclude set from a given reader.
     * @param r reader
     */
    private void readPatterns(Reader r) throws IOException {
        BufferedReader buf = new BufferedReader(r);
        for (;;) {
            String line = buf.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {  //NOI18N
                continue;
            }
            if (line.startsWith("include ")) {  //NOI18N
                line = line.substring(8);
                if (line.length() > 0) {
                    includePatterns.addAll(parsePattern(line));
                }
            } else if (line.startsWith("exclude ")) {  //NOI18N
                line = line.substring(8);
                if (line.length() > 0) {
                    excludePatterns.addAll(parsePattern(line));
                }
            } else {
                throw new java.io.IOException("Wrong line: " + line);  //NOI18N
            }
        }
    }

    enum ParserState {

        START,
        IN_KEY_PATTERN,
        AFTER_KEY_PATTERN,
        IN_BLOCK
    }

    /** Parses given compound string pattern into set of single patterns.
     * @param pattern compound pattern in form filePattern1#keyPattern1#|filePattern2#keyPattern2#|filePattern3
     * @return set of single patterns containing just one # (e.g. [filePattern1#keyPattern1, filePattern2#keyPattern2, filePattern3])
     */
    private static Set<String> parsePattern(String pattern) {
        Set<String> patterns = new HashSet<String>();
        if (pattern.contains("#")) {  //NOI18N
            StringBuilder partPattern = new StringBuilder();
            ParserState state = ParserState.START;
            int blockLevel = 0;
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                switch (state) {
                    case START:
                        if (c == '#') {
                            state = ParserState.IN_KEY_PATTERN;
                            partPattern.append(c);
                        } else if (c == '(') {
                            state = ParserState.IN_BLOCK;
                            blockLevel++;
                            partPattern.append(c);
                        } else if (c == '|') {
                            patterns.add(partPattern.toString());
                            partPattern = new StringBuilder();
                        } else {
                            partPattern.append(c);
                        }
                        break;
                    case IN_KEY_PATTERN:
                        if (c == '#') {
                            state = ParserState.AFTER_KEY_PATTERN;
                        } else {
                            partPattern.append(c);
                        }
                        break;
                    case AFTER_KEY_PATTERN:
                        if (c == '|') {
                            state = ParserState.START;
                            patterns.add(partPattern.toString());
                            partPattern = new StringBuilder();
                        } else {
                            assert false : "Wrong OptionsExport pattern " + pattern + ". Only format like filePattern1#keyPattern#|filePattern2 is supported.";  //NOI18N
                        }
                        break;
                    case IN_BLOCK:
                        partPattern.append(c);
                        if (c == ')') {
                            blockLevel--;
                            if (blockLevel == 0) {
                                state = ParserState.START;
                            }
                        }
                        break;
                }
            }
            patterns.add(partPattern.toString());
        } else {
            patterns.add(pattern);
        }
        return patterns;
    }
}
