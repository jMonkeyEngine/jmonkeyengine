package com.jme3.input.lwjgl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.util.BufferUtils;
import com.jme3.util.res.Resources;

public class SdlGameControllerDb {
    private static final Logger LOGGER = Logger.getLogger(SdlGameControllerDb.class.getName());

    public static ByteBuffer getGamecontrollerDb(String path) throws Exception {
        try ( InputStream gamecontrollerdbIs = Resources.getResourceAsStream(path)) {
            if(gamecontrollerdbIs == null) throw new Exception("Resource not found");

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            
            byte data[] = new byte[4096];
            int read;
            while ((read = gamecontrollerdbIs.read(data)) != -1) {
                bos.write(data, 0, read);
            }
            data = bos.toByteArray();
            
            ByteBuffer gamecontrollerdb = BufferUtils.createByteBuffer(data.length + 1);
            gamecontrollerdb.put(data);
            gamecontrollerdb.put((byte)0); // null-terminate
            gamecontrollerdb.flip();
            LOGGER.log(Level.INFO, "Loaded gamecontrollerdb from {0}", path);
            return gamecontrollerdb;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load "+path+" ", e);
            throw e;
        }
    }
}
