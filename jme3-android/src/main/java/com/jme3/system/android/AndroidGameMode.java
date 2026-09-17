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
package com.jme3.system.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reflection-based bridge to the Android Game Mode API of Android 12 (API 31) and newer.
 *
 * <p>The platform classes ({@code android.app.GameManager} and its game mode listener)
 * are accessed through reflection, so this class compiles and runs on older devices. On
 * devices where the API is unavailable, {@link #isSupported()} returns false,
 * {@link #getGameMode()} returns {@link GameMode#UNSUPPORTED} and registering a listener
 * reports {@link GameMode#UNSUPPORTED} once, the equivalent of the disabled game mode.</p>
 *
 * <p>Instances are normally created and managed by the Android harnesses, for example
 * {@code com.jme3.view.surfaceview.JmeSurfaceView} and
 * {@code com.jme3.app.AndroidHarnessFragment}. Applications that need the raw API can
 * create their own instance from any {@link Context}.</p>
 *
 * @see GameMode
 * @see OnGameModeChanged
 */
public class AndroidGameMode {

    private static final Logger logger = Logger.getLogger(AndroidGameMode.class.getName());
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final String GAME_MANAGER_CLASS_NAME = "android.app.GameManager";
    private static final String GAME_MODE_LISTENER_CLASS_NAME = "android.app.GameManager$OnGameModeChangedListener";
    private static final String GAME_SERVICE_NAME = "game";
    private static final String GET_GAME_MODE_METHOD_NAME = "getGameMode";
    private static final String REGISTER_LISTENER_METHOD_NAME = "registerGameModeChangedListener";
    private static final String UNREGISTER_LISTENER_METHOD_NAME = "unregisterGameModeChangedListener";
    private static final String GAME_MODE_CHANGED_METHOD_NAME = "onGameModeChanged";

    private final Object gameManager;
    private final Method getGameModeMethod;
    private final Method registerListenerMethod;
    private final Method unregisterListenerMethod;
    private final Class<?> listenerClass;
    private OnGameModeChanged listener;
    private Object listenerProxy;

    /**
     * Creates a bridge to the Game Mode API of the given context.
     *
     * @param context the Android context used to look up the game service
     */
    public AndroidGameMode(Context context) {
        Object manager = null;
        Method getGameMode = null;
        Method registerListener = null;
        Method unregisterListener = null;
        Class<?> gameModeListenerClass = null;

        try {
            Class<?> gameManagerClass = Class.forName(GAME_MANAGER_CLASS_NAME);
            gameModeListenerClass = Class.forName(GAME_MODE_LISTENER_CLASS_NAME);
            Object service = context.getSystemService(GAME_SERVICE_NAME);
            if (gameManagerClass.isInstance(service)) {
                getGameMode = gameManagerClass.getMethod(GET_GAME_MODE_METHOD_NAME);
                registerListener =
                        gameManagerClass.getMethod(REGISTER_LISTENER_METHOD_NAME, gameModeListenerClass);
                unregisterListener =
                        gameManagerClass.getMethod(UNREGISTER_LISTENER_METHOD_NAME, gameModeListenerClass);
                manager = service;
            }
        } catch (Throwable throwable) {
            manager = null;
            getGameMode = null;
            registerListener = null;
            unregisterListener = null;
            gameModeListenerClass = null;
            logger.log(Level.FINE, "The Android Game Mode API is not available on this device", throwable);
        }

        this.gameManager = manager;
        this.getGameModeMethod = getGameMode;
        this.registerListenerMethod = registerListener;
        this.unregisterListenerMethod = unregisterListener;
        this.listenerClass = gameModeListenerClass;
    }

    /**
     * Tests whether the platform Game Mode API is available, which requires Android 12
     * (API 31) or newer.
     *
     * @return true if the game mode can be read and observed, false otherwise
     */
    public boolean isSupported() {
        return gameManager != null;
    }

    /**
     * Reads the game mode currently selected for this application.
     *
     * @return the current game mode, or {@link GameMode#UNSUPPORTED} if it cannot be read
     */
    public GameMode getGameMode() {
        if (gameManager == null || getGameModeMethod == null) {
            return GameMode.UNSUPPORTED;
        }
        try {
            Object gameMode = getGameModeMethod.invoke(gameManager);
            if (gameMode instanceof Integer) {
                return GameMode.fromValue((Integer) gameMode);
            }
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "Unable to read the Android game mode", throwable);
        }
        return GameMode.UNSUPPORTED;
    }

    /**
     * Registers the listener notified when the platform game mode changes, replacing any
     * previously registered listener.
     *
     * <p>The current game mode is reported to the listener immediately after it is
     * registered, and the callback is always dispatched on the Android main thread. Pass
     * null to only unregister the previous listener.</p>
     *
     * @param listener the listener to notify, or null to unregister
     * @see OnGameModeChanged
     */
    public void setListener(OnGameModeChanged listener) {
        unregister();
        this.listener = listener;
        if (listener == null) {
            return;
        }
        register(listener);
        dispatch(getGameMode());
    }

    /**
     * Registers the platform listener. Does nothing if the Game Mode API is unavailable.
     */
    private void register(OnGameModeChanged listener) {
        if (gameManager == null || registerListenerMethod == null || listenerClass == null) {
            return;
        }

        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (GAME_MODE_CHANGED_METHOD_NAME.equals(method.getName())
                        && args != null && args.length == 1 && args[0] instanceof Integer) {
                    dispatch(GameMode.fromValue((Integer) args[0]));
                }
                return null;
            }
        };

        try {
            listenerProxy = Proxy.newProxyInstance(
                    AndroidGameMode.class.getClassLoader(), new Class<?>[]{listenerClass}, handler);
            registerListenerMethod.invoke(gameManager, listenerProxy);
        } catch (Throwable throwable) {
            listenerProxy = null;
            logger.log(Level.WARNING, "Unable to register the Android game mode listener", throwable);
        }
    }

    /**
     * Unregisters the platform listener, if any.
     */
    private void unregister() {
        if (gameManager != null && listenerProxy != null && unregisterListenerMethod != null) {
            try {
                unregisterListenerMethod.invoke(gameManager, listenerProxy);
            } catch (Throwable throwable) {
                logger.log(Level.WARNING, "Unable to unregister the Android game mode listener", throwable);
            }
        }
        listenerProxy = null;
    }

    /**
     * Notifies the registered listener on the Android main thread.
     */
    private void dispatch(final GameMode gameMode) {
        if (listener == null) {
            return;
        }

        Runnable notification = new Runnable() {
            @Override
            public void run() {
                OnGameModeChanged target = listener;
                if (target != null) {
                    target.onGameModeChanged(gameMode);
                }
            }
        };

        Looper looper = Looper.myLooper();
        if (looper != null && looper == Looper.getMainLooper()) {
            notification.run();
        } else {
            MAIN_HANDLER.post(notification);
        }
    }
}
