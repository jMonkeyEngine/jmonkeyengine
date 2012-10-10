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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openide.filesystems.*;

/** Does copy of objects on filesystems.
 *
 * @author Jaroslav Tulach
 */
final class Copy extends Object {
     private FileObject sourceRoot;
     private FileObject targetRoot;
     private Set thoseToCopy;
     private PathTransformation transformation;

     private Copy(FileObject source, FileObject target, Set thoseToCopy, PathTransformation transformation) {
         this.sourceRoot = source;
         this.targetRoot = target;
         this.thoseToCopy = thoseToCopy;
         this.transformation = transformation;
     }
     
    /** Does a selective copy of one source tree to another.
     * @param source file object to copy from
     * @param target file object to copy to
     * @param thoseToCopy set on which contains (relativeNameOfAFileToCopy)
     *   is being called to find out whether to copy or not
     * @throws IOException if coping fails
     */
    public static void copyDeep (FileObject source, FileObject target, Set thoseToCopy) 
    throws IOException {
        copyDeep(source, target, thoseToCopy, null);
    }
    
    public static void copyDeep (FileObject source, FileObject target, Set thoseToCopy, PathTransformation transformation) 
    throws IOException {
        Copy instance = new Copy(source, target, thoseToCopy, transformation);
        instance.copyFolder (instance.sourceRoot);
    }
    
    
    private void copyFolder (FileObject sourceFolder) throws IOException {        
        FileObject[] srcChildren = sourceFolder.getChildren();        
        for (int i = 0; i < srcChildren.length; i++) {
            FileObject child = srcChildren[i];
            if (child.isFolder()) {
                copyFolder (child);
                // make sure 'include xyz/.*' copies xyz folder's attributes
                if ((thoseToCopy.contains (child.getPath()) || thoseToCopy.contains (child.getPath() + "/")) && //NOI18N
                    child.getAttributes().hasMoreElements()
                ) {
                    copyFolderAttributes(child);
                }
            } else {                
                if (thoseToCopy.contains (child.getPath())) {
                    copyFile(child);                    
                }                
            }
        }
    }
    
    private void copyFolderAttributes(FileObject sourceFolder) throws IOException {
        FileObject targetFolder = FileUtil.createFolder (targetRoot, sourceFolder.getPath());
        if (sourceFolder.getAttributes ().hasMoreElements ()) {
            FileUtil.copyAttributes(sourceFolder, targetFolder);
        }
    }    
    
    private void copyFile(FileObject sourceFile) throws IOException {        
        String targetPath = (transformation != null) ? transformation.transformPath(sourceFile.getPath()) : sourceFile.getPath();
        boolean isTransformed = !targetPath.equals(sourceFile.getPath());
        FileObject tg = targetRoot.getFileObject(targetPath);
        try {
            if (tg == null) {
                // copy the file otherwise keep old content
                FileObject targetFolder = null;
                String name = null, ext = null;
                if (isTransformed) {
                    FileObject targetFile = FileUtil.createData(targetRoot, targetPath);                
                    targetFolder = targetFile.getParent();
                    name = targetFile.getName();
                    ext = targetFile.getExt();                                        
                    targetFile.delete();                    
                } else {
                    targetFolder = FileUtil.createFolder(targetRoot, sourceFile.getParent().getPath());
                    name = sourceFile.getName();
                    ext = sourceFile.getExt();                    
                }                
                tg = FileUtil.copyFile(sourceFile, targetFolder, name, ext);
            }
        } catch (IOException ex) {
            if (sourceFile.getNameExt().endsWith("_hidden")) {
                return;
            }
            throw ex;
        }
        FileUtil.copyAttributes(sourceFile, tg);        
    }
    
    public static void appendSelectedLines(File sourceFile, File targetFolder, String[] regexForSelection)
    throws IOException {        
        if (!sourceFile.exists()) {
            return;
        }
        Pattern[] linePattern = new Pattern[regexForSelection.length];
        for (int i = 0; i < linePattern.length; i++) {
            linePattern[i] = Pattern.compile(regexForSelection[i]);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();        
        File targetFile = new File(targetFolder,sourceFile.getName());
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        assert targetFolder.exists();
        
        if (!targetFile.exists()) {
            targetFile.createNewFile();
        } else {
            //read original content into  ByteArrayOutputStream
            FileInputStream targetIS = new FileInputStream(targetFile);
            try {
                FileUtil.copy(targetIS, bos);
            } finally {
                targetIS.close();
            }            
        }
        assert targetFile.exists();

        
        //append lines into ByteArrayOutputStream
        String line = null;        
        BufferedReader sourceReader = new BufferedReader(new FileReader(sourceFile));
        try {
            while ((line = sourceReader.readLine()) != null) {
                if (linePattern != null) {
                    for (int i = 0; i < linePattern.length; i++) {
                        Matcher m = linePattern[i].matcher(line);
                        if (m.matches()) {
                            bos.write(line.getBytes());
                            bos.write('\n');
                            break;
                        }                        
                    }                    
                } else {
                    bos.write(line.getBytes());
                    bos.write('\n');
                }
            }
        } finally {
            sourceReader.close();
        }

        ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
        FileOutputStream targetOS = new FileOutputStream(targetFile);
        try {
            FileUtil.copy(bin, targetOS);        
        } finally {
            bin.close();
            targetOS.close();
        }
    }
}
