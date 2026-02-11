/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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

package com.jme3.input.android;

import android.opengl.GLSurfaceView;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

/**
 * <code>AndroidInputHandler26</code> extends <code>AndroidInputHandler24</code> to
 * add the onCapturedPointer events that where added in Android rev 26.<br>
 * The onCapturedPointer events are received when mouse is grabbed/captured.
 *
 * @author joliver82
 */
public class AndroidInputHandler26 extends AndroidInputHandler24 implements View.OnCapturedPointerListener {

    public AndroidInputHandler26() {
        super();
        mouseInput = new AndroidMouseInput26(this);
    }

    protected void removeListeners(GLSurfaceView view) {
        super.removeListeners(view);
        view.setOnCapturedPointerListener(null);
    }

    @Override
    protected void addListeners(GLSurfaceView view) {
        super.addListeners(view);
        view.setOnCapturedPointerListener(this);
    }

    @Override
    public boolean onCapturedPointer(View view, MotionEvent event) {
        if (view != getView()) {
            return false;
        }

        boolean consumed = false;
        boolean isMouse = ((event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE);
        if (isMouse && mouseInput != null) {
            consumed = ((AndroidMouseInput26)mouseInput).onCapturedPointer(event);
        }

        return consumed;
    }
}
