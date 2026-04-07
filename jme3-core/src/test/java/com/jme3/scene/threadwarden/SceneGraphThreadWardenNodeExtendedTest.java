package com.jme3.scene.threadwarden;

import com.jme3.material.Material;
import com.jme3.material.MatParamOverride;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Parameterized tests for SceneGraphThreadWarden class.
 * These tests verify that various scene graph mutations are properly checked for thread safety.
 */
public class SceneGraphThreadWardenNodeExtendedTest {

    private static ExecutorService executorService;

    @SuppressWarnings({"ReassignedVariable", "AssertWithSideEffects"})
    @BeforeAll
    public static void setupClass() {
        // Make sure assertions are enabled
        boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            throw new RuntimeException("WARNING: Assertions are not enabled! Tests may not work correctly.");
        }
    }

    @BeforeEach
    public void setup() {
        executorService = newSingleThreadDaemonExecutor();
    }

    @AfterEach
    public void tearDown() {
        executorService.shutdown();
        SceneGraphThreadWarden.reset();
    }

    /**
     * Define the parameters for the test.
     * Each parameter is a pair of (test name, action to perform on spatial).
     */
    static Stream<Arguments> data() {
        Material mockMaterial = Mockito.mock(Material.class);
        MatParamOverride override = new MatParamOverride(VarType.Float, "TestParam", 1.0f);

        return Stream.of(
                Arguments.of("setMaterial", (Consumer<Node>) spatial -> spatial.setMaterial(mockMaterial)),
                Arguments.of("setLodLevel", (Consumer<Node>) spatial -> spatial.setLodLevel(1)),
                Arguments.of("addMatParamOverride", (Consumer<Node>) spatial -> spatial.addMatParamOverride(override)),
                Arguments.of("removeMatParamOverride", (Consumer<Node>) spatial -> spatial.removeMatParamOverride(override)),
                Arguments.of("clearMatParamOverrides", (Consumer<Node>) Spatial::clearMatParamOverrides)
        );
    }

    /**
     * Test that scene graph mutation is fine on the main thread when the object is attached to the root.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testMutationOnMainThreadOnAttachedObject(String testName, Consumer<Node> action) {
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testMutationOnMainThreadOnDetachedObject(String testName, Consumer<Node> action) {
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testMutationOnNonMainThreadOnDetachedObject(String testName, Consumer<Node> action)
            throws ExecutionException, InterruptedException {
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    public void testMutationOnNonMainThreadOnAttachedObject(String testName, Consumer<Node> action)
            throws InterruptedException {
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
            assertTrue(e.getCause() instanceof IllegalThreadSceneGraphMutation,
                    "Expected IllegalThreadSceneGraphMutation, got: " + e.getCause().getClass().getName());
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
