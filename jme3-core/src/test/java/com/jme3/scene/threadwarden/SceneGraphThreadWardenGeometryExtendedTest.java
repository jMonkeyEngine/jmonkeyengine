package com.jme3.scene.threadwarden;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Parameterized tests for SceneGraphThreadWarden class with Geometry objects.
 * These tests verify that various scene graph mutations are properly checked for thread safety.
 */
@RunWith(Parameterized.class)
public class SceneGraphThreadWardenGeometryExtendedTest {

    private static ExecutorService executorService;

    private final String testName;
    private final Consumer<Geometry> action;

    @SuppressWarnings({"ReassignedVariable", "AssertWithSideEffects"})
    @BeforeClass
    public static void setupClass() {
        // Make sure assertions are enabled
        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            throw new RuntimeException("WARNING: Assertions are not enabled! Tests may not work correctly.");
        }
    }

    @Before
    public void setup() {
        executorService = newSingleThreadDaemonExecutor();
    }

    @After
    public void tearDown() {
        executorService.shutdown();
        SceneGraphThreadWarden.reset();
    }

    /**
     * Constructor for the parameterized test.
     * 
     * @param testName A descriptive name for the test
     * @param action The action to perform on the spatial
     */
    public SceneGraphThreadWardenGeometryExtendedTest(String testName, Consumer<Geometry> action) {
        this.testName = testName;
        this.action = action;
    }

    /**
     * Define the parameters for the test.
     * Each parameter is a pair of (test name, action to perform on spatial).
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        Material mockMaterial = Mockito.mock(Material.class);
        Box box = new Box(1, 1, 1);

        return Arrays.asList(new Object[][] {
            { 
                "setMaterial", 
                (Consumer<Geometry>) spatial -> spatial.setMaterial(mockMaterial)
            },
            { 
                "setMesh", 
                (Consumer<Geometry>) spatial -> spatial.setMesh(box)
            },
            { 
                "setLodLevel", 
                (Consumer<Geometry>) spatial -> {
                    // Need to set a mesh with LOD levels first
                    Mesh mesh = new Box(1, 1, 1);
                    mesh.setLodLevels(new com.jme3.scene.VertexBuffer[]{
                        mesh.getBuffer(com.jme3.scene.VertexBuffer.Type.Index)
                    });
                    spatial.setMesh(mesh);
                    spatial.setLodLevel(0);
                }
            },
            { 
                "removeFromParent", 
                (Consumer<Geometry>) Geometry::removeFromParent
            }
        });
    }

    /**
     * Test that scene graph mutation is fine on the main thread when the object is attached to the root.
     */
    @Test
    public void testMutationOnMainThreadOnAttachedObject() {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a geometry and attach it to the root node
        Geometry geometry = new Geometry("geometry", new Box(1, 1, 1));
        rootNode.attachChild(geometry);

        // This should work fine since we're on the main thread
        action.accept(geometry);
    }

    /**
     * Test that scene graph mutation is fine on the main thread when the object is not attached to the root.
     */
    @Test
    public void testMutationOnMainThreadOnDetachedObject() {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a geometry but don't attach it to the root node
        Geometry geometry = new Geometry("geometry", new Box(1, 1, 1));

        // This should work fine since we're on the main thread
        action.accept(geometry);
    }

    /**
     * Test that scene graph mutation is fine on a non-main thread when the object is not attached to the root.
     */
    @Test
    public void testMutationOnNonMainThreadOnDetachedObject() throws ExecutionException, InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a geometry but don't attach it to the root node
        Geometry geometry = new Geometry("geometry", new Box(1, 1, 1));

        Future<Void> future = executorService.submit(() -> {
            // This should work fine since the geometry is not connected to the root node
            action.accept(geometry);
            return null;
        });

        // This should complete without exceptions
        future.get();
    }

    /**
     * Test that scene graph mutation is not allowed on a non-main thread when the object is attached to the root.
     */
    @Test
    public void testMutationOnNonMainThreadOnAttachedObject() throws InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a geometry and attach it to the root node
        Geometry geometry = new Geometry("geometry", new Box(1, 1, 1));
        rootNode.attachChild(geometry);

        Future<Void> future = executorService.submit(() -> {
            // This should fail because we're trying to modify a geometry that's connected to the scene graph
            action.accept(geometry);
            return null;
        });

        try {
            future.get();
            fail("Expected an IllegalThreadSceneGraphMutation exception");
        } catch (ExecutionException e) {
            // This is expected - verify it's the right exception type
            assertTrue("Expected IllegalThreadSceneGraphMutation, got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof IllegalThreadSceneGraphMutation);
        }
    }

    /**
     * Creates a single-threaded executor service with daemon threads.
     */
    private static ExecutorService newSingleThreadDaemonExecutor() {
        return Executors.newSingleThreadExecutor(daemonThreadFactory());
    }

    /**
     * Creates a thread factory that produces daemon threads.
     */
    private static ThreadFactory daemonThreadFactory() {
        return r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        };
    }
}