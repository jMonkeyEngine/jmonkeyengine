package com.jme3.app;

import com.jme3.IntegrationTest;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class AppSettingsIT {

    private static final String APPSETTINGS_KEY = "JME_AppSettingsTest";

    @Test
    public void testPreferencesSaveLoad() throws BackingStoreException {
        AppSettings settings = new AppSettings(false);
        settings.putBoolean("TestBool", true);
        settings.putInteger("TestInt", 123);
        settings.putString("TestStr", "HelloWorld");
        settings.putFloat("TestFloat", 123.567f);
        settings.put("TestObj", new Mesh()); // Objects not supported by preferences
        settings.save(APPSETTINGS_KEY);

        AppSettings loadedSettings = new AppSettings(false);
        loadedSettings.load(APPSETTINGS_KEY);

        assertEquals(true, loadedSettings.getBoolean("TestBool"));
        assertEquals(123, loadedSettings.getInteger("TestInt"));
        assertEquals("HelloWorld", loadedSettings.getString("TestStr"));
        assertEquals(123.567f, loadedSettings.get("TestFloat"));
    }

    @Test
    public void testStreamSaveLoad() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        AppSettings settings = new AppSettings(false);
        settings.putBoolean("TestBool", true);
        settings.putInteger("TestInt", 123);
        settings.putString("TestStr", "HelloWorld");
        settings.putFloat("TestFloat", 123.567f);
        settings.put("TestObj", new Mesh()); // Objects not supported by file settings
        settings.save(baos);

        AppSettings loadedSettings = new AppSettings(false);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        loadedSettings.load(bais);

        assertEquals(true, loadedSettings.getBoolean("TestBool"));
        assertEquals(123, loadedSettings.getInteger("TestInt"));
        assertEquals("HelloWorld", loadedSettings.getString("TestStr"));
        assertEquals(123.567f, loadedSettings.get("TestFloat"));
    }
}
