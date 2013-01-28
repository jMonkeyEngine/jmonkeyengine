/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.assets;

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.ExtensionList;
import org.openide.loaders.FileEntry;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;

/**
 *
 * @author normenhansen
 */
public class BinaryModelFileLoader extends MultiFileLoader {

    public static final String PROP_EXTENSIONS = "extensions"; // NOI18N
    public static final String FILE_EXTENSION = "j3o";
    public static final String INFO_EXTENSION = FILE_EXTENSION + "data";
    public static final String MIME_TYPE = "application/jme3model";
    private static final long serialVersionUID = -4579746482156153693L;

    public BinaryModelFileLoader() {
        super("com.jme3.gde.core.assets.BinaryModelDataObject");
    }

    @Override
    protected String actionsContext() {
        return "Loaders/application/jme3model/Actions";
    }

    protected MultiDataObject createMultiObject(FileObject primaryFile)
            throws DataObjectExistsException, IOException {
        return new BinaryModelDataObject(primaryFile, this);
    }

    @Override
    protected void initialize() {
        super.initialize();
        getExtensions();
    }

    /**
     * For a given file find the primary file.
     *
     * @param fo the file to find the primary file for
     * @return the primary file for this file or null if this file is not
     * recognized by this loader.
     */
    protected FileObject findPrimaryFile(FileObject fo) {
        // never recognize folders.
        if (fo.isFolder()) {
            return null;
        }
        String ext = fo.getExt();
        if (ext.equalsIgnoreCase(INFO_EXTENSION)) {
            FileObject info = FileUtil.findBrother(fo, FILE_EXTENSION);
            if (info != null) {
                return info;
            } else {
                return null;
            }
        }
        if (getExtensions().isRegistered(fo)) {
            return fo;
        }
        return null;
    }

    /**
     * Create the primary file entry. Primary files are the j3o files.
     *
     * @param primaryFile primary file recognized by this loader
     * @return primary entry for that file
     */
    protected MultiDataObject.Entry createPrimaryEntry(
            MultiDataObject obj, FileObject primaryFile) {
        return new FileEntry(obj, primaryFile);
    }

    /**
     * Create a secondary file entry. Secondary files are properties files,
     * which should also be retained (so, not a FileEntry.Numb object)
     *
     * @param secondaryFile secondary file to create entry for
     * @return the entry
     */
    protected MultiDataObject.Entry createSecondaryEntry(
            MultiDataObject obj, FileObject secondaryFile) {
        return new FileEntry(obj, secondaryFile);
    }

    /**
     * @return The list of extensions this loader recognizes.
     */
    public ExtensionList getExtensions() {
        ExtensionList extensions = (ExtensionList) getProperty(PROP_EXTENSIONS);
        if (extensions == null) {
            extensions = new ExtensionList();
            extensions.addExtension(FILE_EXTENSION);
            extensions.addExtension(INFO_EXTENSION);
            extensions.addMimeType(MIME_TYPE);
            putProperty(PROP_EXTENSIONS, extensions, false);
        }
        return extensions;
    }

    /**
     * Sets the extension list for this data loader.
     *
     * @param ext new list of extensions.
     */
    public void setExtensions(ExtensionList ext) {
        putProperty(PROP_EXTENSIONS, ext, true);
    }
}
