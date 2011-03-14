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
package com.jme3.gde.core.util;

import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;

/**
 * Initialization:<br/>
 * DataObjectSaveNode saveNode=new DataObjectSaveNode(dateObject);<br/>
 * setActivatedNodes(new org.openide.nodes.Node[]{saveNode});<br/>
 * To set save state:<br/>
 * saveNode.setSaveCookie(saveCookie);<br/>
 * saveNode.removeSaveCookie();<br/>
 * @author normenhansen
 */
public class DataObjectSaveNode extends DataNode {

    protected SaveCookie cook;

    public DataObjectSaveNode(DataObject object) {
        super(object, Children.LEAF);
    }

    public void setSaveCookie(SaveCookie cookie) {
        removeSaveCookie();
        cook = cookie;
        getCookieSet().assign(SaveCookie.class, cookie);
        getDataObject().setModified(true);
    }

    public void removeSaveCookie() {
        if (cook != null) {
            getCookieSet().assign(SaveCookie.class);
        }
        getDataObject().setModified(false);
    }

    @Override
    public String toString() {
        if (cook != null)
            return "DataObjectSaveNode("+cook.toString()+" - "+getDisplayName()+")";
        else
            return "DataObjectSaveNode("+getDisplayName()+")";
    }
}
