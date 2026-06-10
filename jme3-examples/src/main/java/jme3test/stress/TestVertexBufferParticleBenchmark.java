package jme3test.stress;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

public class TestVertexBufferParticleBenchmark extends SimpleApplication implements ActionListener {

    private static final int DEFAULT_PARTICLES = 2_000_000;
    private static final int DEFAULT_MOVING_PARTICLES = 200_000;

    private final int particleCount = Integer.getInteger("particle.count", DEFAULT_PARTICLES);
    private final int movingCount = Math.min(Integer.getInteger("particle.moving", DEFAULT_MOVING_PARTICLES), particleCount);
    private final float[] x = new float[particleCount];
    private final float[] y = new float[particleCount];
    private final float[] vx = new float[particleCount];
    private final float[] vy = new float[particleCount];

    private Mesh mesh;
    private VertexBuffer positionBuffer;
    private FloatBuffer positions;
    private BitmapText hud;
    private boolean partialUpdates = Boolean.parseBoolean(System.getProperty("particle.partial", "true"));
    private int movingStart;
    private float statsTime;
    private int statsFrames;
    private float fps;

    public static void main(String[] args) {
        TestVertexBufferParticleBenchmark app = new TestVertexBufferParticleBenchmark();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Partial VertexBuffer Particle Benchmark");
        settings.setResolution(1280, 720);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(200f);
        cam.setLocation(new Vector3f(0f, 0f, 260f));
        initParticles();
        initMesh();
        initHud();

        inputManager.addMapping("TogglePartial", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "TogglePartial");
    }

    @Override
    public void simpleUpdate(float tpf) {
        updateParticles(tpf);
        updateStats(tpf);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && "TogglePartial".equals(name)) {
            partialUpdates = !partialUpdates;
        }
    }

    private void initParticles() {
        int columns = (int) Math.ceil(Math.sqrt(particleCount));
        float spacing = 0.45f;
        float half = columns * spacing * 0.5f;

        for (int i = 0; i < particleCount; i++) {
            x[i] = (i % columns) * spacing - half;
            y[i] = (i / columns) * spacing - half;
            vx[i] = 15f + (i % 37) * 0.17f;
            vy[i] = 10f + (i % 53) * 0.13f;
        }
    }

    private void initMesh() {
        positions = BufferUtils.createFloatBuffer(particleCount * 3);
        for (int i = 0; i < particleCount; i++) {
            putPosition(i);
        }

        mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Points);
        positionBuffer = new VertexBuffer(VertexBuffer.Type.Position);
        positionBuffer.setupData(VertexBuffer.Usage.Stream, 3, VertexBuffer.Format.Float, positions);
        mesh.setBuffer(positionBuffer);
        mesh.setBound(new BoundingBox(Vector3f.ZERO, 140f, 140f, 20f));
        mesh.updateCounts();

        Geometry geometry = new Geometry("particles", mesh);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", ColorRGBA.Cyan);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
        rootNode.attachChild(geometry);
    }

    private void initHud() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hud = new BitmapText(guiFont);
        hud.setSize(18f);
        hud.setLocalTranslation(12f, settings.getHeight() - 12f, 0f);
        guiNode.attachChild(hud);
    }

    private void updateParticles(float tpf) {
        int start = movingStart;
        for (int n = 0; n < movingCount; n++) {
            int i = (start + n) % particleCount;
            x[i] += vx[i] * tpf;
            y[i] += vy[i] * tpf;

            if (x[i] > 120f || x[i] < -120f) {
                vx[i] = -vx[i];
            }
            if (y[i] > 120f || y[i] < -120f) {
                vy[i] = -vy[i];
            }
            putPosition(i);
        }

        if (partialUpdates) {
            int firstLen = Math.min(movingCount, particleCount - start);
            positionBuffer.markElementsDirty(start, firstLen);
            if (firstLen < movingCount) {
                positionBuffer.markElementsDirty(0, movingCount - firstLen);
            }
        } else {
            positionBuffer.updateData(positions);
        }

        movingStart = (movingStart + movingCount) % particleCount;
    }

    private void putPosition(int particle) {
        int offset = particle * 3;
        positions.put(offset, x[particle]);
        positions.put(offset + 1, y[particle]);
        positions.put(offset + 2, 0f);
    }

    private void updateStats(float tpf) {
        statsTime += tpf;
        statsFrames++;
        if (statsTime >= 0.5f) {
            fps = statsFrames / statsTime;
            statsTime = 0f;
            statsFrames = 0;
        }

        hud.setText("Particles: " + particleCount
                + "  Moving/frame: " + movingCount
                + "  Upload: " + (partialUpdates ? "partial dirty regions" : "full updateData")
                + "  FPS: " + FastMath.floor(fps)
                + "\nSPACE toggles partial/full. Set -Dparticle.count, -Dparticle.moving, -Dparticle.partial.");
    }
}
