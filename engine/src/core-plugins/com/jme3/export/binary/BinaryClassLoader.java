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

package com.jme3.export.binary;

import java.io.IOException;
import java.util.logging.Logger;

import com.jme3.export.InputCapsule;
import com.jme3.export.Savable;

/**
 * This class is mis-named and is located in an inappropriate package:
 * It is not binary-specific (it is in fact used for XML format too), and it
 * is not a java.lang.ClassLoader, which is what "class loader" is for Java
 * developers.
 *
 * @author mpowell
 */
public class BinaryClassLoader {

    /**
     * fromName creates a new Savable from the provided class name. First registered modules
     * are checked to handle special cases, if the modules do not handle the class name, the
     * class is instantiated directly. 
     * @param className the class name to create.
     * @param inputCapsule the InputCapsule that will be used for loading the Savable (to look up ctor parameters)
     * @return the Savable instance of the class.
     * @throws InstantiationException thrown if the class does not have an empty constructor.
     * @throws IllegalAccessException thrown if the class is not accessable.
     * @throws ClassNotFoundException thrown if the class name is not in the classpath.
     * @throws IOException when loading ctor parameters fails
     */
    public static Savable fromName(String className, InputCapsule inputCapsule) throws InstantiationException, 
        IllegalAccessException, ClassNotFoundException, IOException {
            
        try {
            return (Savable)Class.forName(className).newInstance();
        }
        catch (InstantiationException e) {
        	Logger.getLogger(BinaryClassLoader.class.getName()).severe(
        			"Could not access constructor of class '" + className + "'! \n" +
        			"Some types need to have the BinaryImporter set up in a special way. Please doublecheck the setup.");
        	throw e;
        }
        catch (IllegalAccessException e) {
        	Logger.getLogger(BinaryClassLoader.class.getName()).severe(
        			e.getMessage() + " \n" +
                    "Some types need to have the BinaryImporter set up in a special way. Please doublecheck the setup.");
        	throw e;
        }
    }

}
