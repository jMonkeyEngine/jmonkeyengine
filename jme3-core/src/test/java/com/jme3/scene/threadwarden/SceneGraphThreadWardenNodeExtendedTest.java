package com.jme3.scene.threadwarden;

import com.jme3.material.Material;
import com.jme3.material.MatParamOverride;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
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
 * Parameterized tests for SceneGraphThreadWarden class.
 * These tests verify that various scene graph mutations are properly checked for thread safety.
 */
@RunWith(Parameterized.class)
public class SceneGraphThreadWardenNodeExtendedTest {

    private static ExecutorService executorService;

    private final String testName;
    private final Consumer<Node> action;

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
    public SceneGraphThreadWardenNodeExtendedTest(String testName, Consumer<Node> action) {
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
        MatParamOverride override = new MatParamOverride(VarType.Float, "TestParam", 1.0f);

        return Arrays.asList(new Object[][] {
            { 
                "setMaterial", 
                (Consumer<Node>) spatial -> spatial.setMaterial(mockMaterial)
            },
            { 
                "setLodLevel", 
                (Consumer<Node>) spatial -> spatial.setLodLevel(1)
            },
            { 
                "addMatParamOverride", 
                (Consumer<Node>) spatial -> spatial.addMatParamOverride(override)
            },
            { 
                "removeMatParamOverride", 
                (Consumer<Node>) spatial -> spatial.removeMatParamOverride(override)
            },
            { 
                "clearMatParamOverrides", 
                (Consumer<Node>) Spatial::clearMatParamOverrides
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

        // Create a child node and attach it to the root node
        Node child = new Node("child");
        rootNode.attachChild(child);

        // This should work fine since we're on the main thread
        action.accept(child);
    }

    /**
     * Test that scene graph mutation is fine on the main thread when the object is not attached to the root.
     */
    @Test
    public void testMutationOnMainThreadOnDetachedObject() {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node but don't attach it to the root node
        Node child = new Node("child");

        // This should work fine since we're on the main thread
        action.accept(child);
    }

    /**
     * Test that scene graph mutation is fine on a non-main thread when the object is not attached to the root.
     */
    @Test
    public void testMutationOnNonMainThreadOnDetachedObject() throws ExecutionException, InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node but don't attach it to the root node
        Node child = new Node("child");

        Future<Void> future = executorService.submit(() -> {
            // This should work fine since the node is not connected to the root node
            action.accept(child);
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

        // Create a child node and attach it to the root node
        Node child = new Node("child");
        rootNode.attachChild(child);

        Future<Void> future = executorService.submit(() -> {
            // This should fail because we're trying to modify a node that's connected to the scene graph
            action.accept(child);
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
