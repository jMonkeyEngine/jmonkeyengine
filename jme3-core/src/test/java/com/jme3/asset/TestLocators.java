package com.jme3.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.audio.plugins.WAVLoader;
import com.jme3.system.JmeSystem;
import com.jme3.texture.plugins.AWTLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

public class TestLocators {

    @Test
    public void testAbsoluteLocators() {
        AssetManager am = JmeSystem.newAssetManager(TestLocators.class.getResource("/com/jme3/asset/Desktop.cfg"));
        am.registerLocator("/",  ClasspathLocator.class);
        am.registerLoader(WAVLoader.class, "wav");
        am.registerLoader(AWTLoader.class, "jpg");

        assertNotNull(am.loadAudio("Sound/Effects/Gun.wav"));
        assertNotNull(am.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
    }

    /**
     * Demonstrates loading a file from a custom {@link AssetLoader}
     */
    @Test
    public void testCustomLoader() {
        AssetManager am = new DesktopAssetManager(true);
        am.registerLocator("/", ClasspathLocator.class);
        am.registerLoader(TextLoader.class, "fnt");
        String result = (String)am.loadAsset("Interface/Fonts/Console.fnt");
        assertTrue(result.startsWith("info face=\"Lucida Console\" size=11 bold=0 italic=0 charset=\"\" unicode=1" +
                " stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=1,1 outline=0"));
    }

    @Test
    public void testManyLocators() throws IOException {
        Map<String, byte[]> fixtures = Map.of(
                "/first.zip", createZip("assets/first.txt", "First archive asset"),
                "/second.zip", createZip("assets/second.txt", "Second archive asset"),
                "/web/http.txt", "HTTP asset".getBytes(StandardCharsets.UTF_8));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/", exchange -> serveAsset(exchange, fixtures));
            server.start();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

            // Register only these locators, in this order, so misses exercise fallback.
            AssetManager am = new DesktopAssetManager();
            am.registerLocator(baseUrl + "/web/", UrlLocator.class);
            am.registerLocator(baseUrl + "/first.zip", HttpZipLocator.class);
            am.registerLocator(baseUrl + "/second.zip", HttpZipLocator.class);
            am.registerLocator("/", ClasspathLocator.class);

            assertNotNull(am.locateAsset(new AssetKey<>("Interface/Fonts/Default.fnt")),
                    "Failed to load from classpath");
            assertAssetContents(am, "assets/first.txt", "First archive asset");
            assertAssetContents(am, "assets/second.txt", "Second archive asset");
            assertAssetContents(am, "http.txt", "HTTP asset");
            assertNull(am.locateAsset(new AssetKey<>("missing-asset.txt")));
        } finally {
            server.stop(0);
        }
    }

    private static void assertAssetContents(AssetManager am, String name, String expected) throws IOException {
        AssetInfo info = am.locateAsset(new AssetKey<>(name));
        assertNotNull(info, "Failed to locate " + name);
        try (InputStream stream = info.openStream()) {
            assertNotNull(stream, "Failed to open " + name);
            assertEquals(expected, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static byte[] createZip(String name, String contents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            // Include a directory entry and keep the ZIP larger than the locator's 200-byte tail read.
            zip.putNextEntry(new ZipEntry("assets/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(name));
            zip.write(contents.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static void serveAsset(HttpExchange exchange, Map<String, byte[]> fixtures) throws IOException {
        try {
            byte[] data = fixtures.get(exchange.getRequestURI().getPath());
            if (data == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            int start = 0;
            int end = data.length - 1;
            int status = 200;
            String range = exchange.getRequestHeaders().getFirst("Range");
            if (range != null) {
                // HttpZipLocator requests a suffix for the ZIP footer, then explicit byte ranges.
                Matcher bounds = Pattern.compile("bytes=(\\d*)-(\\d*)").matcher(range);
                if (!bounds.matches() || (bounds.group(1).isEmpty() && bounds.group(2).isEmpty())) {
                    exchange.sendResponseHeaders(416, -1);
                    return;
                }
                if (bounds.group(1).isEmpty()) {
                    start = Math.max(0, data.length - Integer.parseInt(bounds.group(2)));
                } else {
                    start = Integer.parseInt(bounds.group(1));
                    if (!bounds.group(2).isEmpty()) {
                        end = Math.min(end, Integer.parseInt(bounds.group(2)));
                    }
                }
                if (start > end) {
                    exchange.sendResponseHeaders(416, -1);
                    return;
                }
                status = 206;
                exchange.getResponseHeaders().set("Content-Range",
                        "bytes " + start + "-" + end + "/" + data.length);
            }
            exchange.sendResponseHeaders(status, end - start + 1);
            exchange.getResponseBody().write(data, start, end - start + 1);
        } finally {
            exchange.close();
        }
    }
}
