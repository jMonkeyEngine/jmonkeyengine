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
package com.jme3.gde.core.sceneexplorer.nodes;

import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.scene.Spatial;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public class ClipboardSpatial implements Transferable, ClipboardOwner {

    private byte[] data;

    public ClipboardSpatial(Spatial spat){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            BinaryExporter.getInstance().save(spat, out);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        data= out.toByteArray();
    }

    public ClipboardSpatial(byte[] spatial) {
        data = spatial;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{new DataFlavor(Spatial.class, "Spatial")};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return new DataFlavor(Spatial.class, "Spatial").equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws
            UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            return null;
//            throw new UnsupportedFlavorException(flavor);
        }
        return createSpatial();
    }

    public void lostOwnership(java.awt.datatransfer.Clipboard clip,
            java.awt.datatransfer.Transferable tr) {
        return;
    }

    public Spatial createSpatial(){
        try {
            BinaryImporter importer=BinaryImporter.getInstance();
            //TODO: unsafe..
            importer.setAssetManager(SceneApplication.getApplication().getCurrentSceneRequest().getManager().getManager());
            return (Spatial)importer.load(data);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

}
