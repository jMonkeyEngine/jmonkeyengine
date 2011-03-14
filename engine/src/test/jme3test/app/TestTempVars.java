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

package jme3test.app;

import com.jme3.util.TempVars;

public class TestTempVars {

    public static void methodThatUsesTempVars(){
        TempVars vars = TempVars.get();

        assert vars.lock();
        {
            vars.vect1.set(123,999,-55);
        }
        assert vars.unlock();
    }

    public static void main(String[] args){
        System.err.println("NOTE: This test simulates a data corruption attempt\n" +
                           " in the engine. If assertions are enabled (-ea VM flag), the \n" +
                           "data corruption will be detected and displayed below.");

        TempVars vars = TempVars.get();

        assert vars.lock();
        {
            // do something with temporary vars
            vars.vect1.addLocal(vars.vect2);
        }
        assert vars.unlock();



        assert vars.lock();
        {
            // set a value
            vars.vect1.set(1,1,1);

            // method that currupts the value
            methodThatUsesTempVars();

            // read the value
            System.out.println(vars.vect1);
        }
        assert vars.unlock();
    }

}
