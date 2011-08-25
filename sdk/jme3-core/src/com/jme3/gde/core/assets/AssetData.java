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

import com.jme3.asset.AssetKey;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class AssetData extends Properties {

    private AssetDataObject file;
    private String extension = "jmpdata";

    public AssetData(AssetDataObject file) {
        this.file = file;
    }

    public AssetData(AssetDataObject file, String extension) {
        this.file = file;
        this.extension = extension;
    }

    public AssetKey<?> getAssetKey() {
        return file.getAssetKey();
    }
    
    public void setAssetKey(AssetKey key){
        file.setAssetKeyData(key);
    }

    public Object loadAsset() {
        return file.loadAsset();
    }

    public void saveAsset() throws IOException {
        file.saveAsset();
    }

    @Override
    public synchronized String getProperty(String key) {
        return super.getProperty(key);
    }

    @Override
    public synchronized String getProperty(String key, String defaultValue) {
//        loadProperties();
        return super.getProperty(key, defaultValue);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        Object obj= super.setProperty(key, value);
//        try {
//            saveProperties();
//        } catch (FileAlreadyLockedException ex) {
//            Exceptions.printStackTrace(ex);
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
        return obj;
    }

    public void loadProperties() {
        clear();
        FileObject myFile = FileUtil.findBrother(file.getPrimaryFile(), extension);
        if (myFile == null) {
            return;
        }
        InputStream in = null;
        try {
            in = myFile.getInputStream();
            try {
                load(in);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public void saveProperties() throws FileAlreadyLockedException, IOException {
        OutputStream out = null;
        FileLock lock = null;
        try {
            FileObject pFile = file.getPrimaryFile();
            FileObject myFile = FileUtil.findBrother(pFile, extension);
            if (myFile == null) {
                myFile = FileUtil.createData(pFile.getParent(), pFile.getName() + "." + extension);
            }
            lock = myFile.lock();
            out = myFile.getOutputStream(lock);
            store(out, "");
        } finally {
            if (out != null) {
                out.close();
            }
            if (lock != null) {
                lock.releaseLock();
            }
        }
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
