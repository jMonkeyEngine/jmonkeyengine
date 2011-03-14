/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.shader;

import com.jme3.asset.AssetKey;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

public class ShaderKey extends AssetKey<Shader> {

    protected String fragName;
    protected DefineList defines;
    protected String language;

    public ShaderKey(){
    }

    public ShaderKey(String vertName, String fragName, DefineList defines, String lang){
        super(vertName);
        this.fragName = fragName;
        this.defines = defines;
        this.language = lang;
    }

    @Override
    public String toString(){
        return "V="+name + " F=" + fragName + (defines != null ? defines : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }

        final ShaderKey other = (ShaderKey) obj;
        if (name.equals(other.name) && fragName.equals(other.fragName)){
//            return true;
            if (defines != null && other.defines != null)
                return defines.getCompiled().equals(other.defines.getCompiled());
            else if (defines != null || other.defines != null)
                return false;
            else
                return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + name.hashCode();
        hash = 41 * hash + fragName.hashCode();
        hash = 41 * hash + (defines != null ? defines.getCompiled().hashCode() : 0);
        return hash;
    }

    public DefineList getDefines() {
        return defines;
    }

    public String getVertName(){
        return name;
    }

    public String getFragName() {
        return fragName;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public void write(JmeExporter ex) throws IOException{
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(fragName, "fragment_name", null);
        oc.write(language, "language", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException{
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        fragName = ic.readString("fragment_name", null);
        language = ic.readString("language", null);
    }

}
