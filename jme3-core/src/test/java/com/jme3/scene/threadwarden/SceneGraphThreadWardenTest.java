package com.jme3.scene.threadwarden;

import com.jme3.scene.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for SceneGraphThreadWarden class.
 * These tests verify that:
 * - Normal node mutation is fine on the main thread
 * - Node mutation on nodes not connected to the root node is fine even on a non main thread
 * - Adding a node to the scene graph (indirectly) connected to the root node isn't fine on a non main thread
 * - Adding a node currently attached to a root node to a different node isn't fine on a non main thread
 */
public class SceneGraphThreadWardenTest {

    private static ExecutorService executorService;

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
     * Test that normal node mutation is fine on the main thread.
     */
    @Test
    public void testNormalNodeMutationOnMainThread() {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // This should work fine since we're on the main thread
        Node child = new Node("child");
        rootNode.attachChild(child);

        // Add another level of children
        Node grandchild = new Node("grandchild");
        child.attachChild(grandchild);

        // Detach should also work fine
        child.detachChild(grandchild);
        rootNode.detachChild(child);
    }

    /**
     * Test that node mutation on nodes not connected to the root node is fine even on a non main thread.
     * <p>
     *     This is a use case where a thread is preparing things for later attachment to the scene graph.
     * </p>
     */
    @Test
    public void testNodeMutationOnNonConnectedNodesOnNonMainThread() throws ExecutionException, InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        Future<Node> nonConnectedNodeFuture = executorService.submit(() -> {
            // This should work fine since these nodes are not connected to the root node
            Node parent = new Node("parent");
            Node child = new Node("child");
            parent.attachChild(child);

            // Add another level of children
            Node grandchild = new Node("grandchild");
            child.attachChild(grandchild);

            return parent;
        });

        // Get the result to ensure the task completed without exceptions
        Node nonConnectedNode = nonConnectedNodeFuture.get();

        // Now we can attach it to the root node on the main thread
        rootNode.attachChild(nonConnectedNode);
    }

    /**
     * Test that adding a node to the scene graph connected to the root node in a non main thread leads to an
     * exception.
     */
    @Test
    public void testAddingNodeToSceneGraphOnNonMainThread() throws InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node and attach it to the root node
        Node child = new Node("child");
        rootNode.attachChild(child);

        Future<Void> illegalMutationFuture = executorService.submit(() -> {
            // This should fail because we're trying to add a node to a node that's connected to the scene graph
            Node grandchild = new Node("grandchild");
            child.attachChild(grandchild);
            return null;
        });

        try {
            illegalMutationFuture.get();
            fail("Expected an IllegalThreadSceneGraphMutation exception");
        } catch (ExecutionException e) {
            // This is expected - verify it's the right exception type
            assertTrue("Expected IllegalThreadSceneGraphMutation, got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof IllegalThreadSceneGraphMutation);
        }
    }

    /**
     * Test that adding a node currently attached to a root node to a different node leads to an exception.
     * <p>
     *     This is testing an edge case where you think you'd working with non-connected nodes, but in reality
     *     one of your nodes is already attached to the scene graph (and you're attaching it to a different node which will
     *     detach it from the scene graph).
     * </p>
     */
    @Test
    public void testMovingNodeAttachedToRootOnNonMainThread() throws InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create two child nodes and attach them to the root node
        Node child1 = new Node("child1");
        Node child2 = new Node("child2");

        rootNode.attachChild(child2);

        Future<Void> illegalMutationFuture = executorService.submit(() -> {
            // This should fail because we're trying to move a node that's connected to the root node
            child1.attachChild(child2); // This implicitly detaches child2 from rootNode
            return null;
        });

        try {
            illegalMutationFuture.get();
            fail("Expected an IllegalThreadSceneGraphMutation exception");
        } catch (ExecutionException e) {
            // This is expected - verify it's the right exception type
            assertTrue("Expected IllegalThreadSceneGraphMutation, got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof IllegalThreadSceneGraphMutation);
        }
    }

    /**
     * Test that detaching a node releases it from thread protection.
     */
    @Test
    public void testDetachmentReleasesProtection() throws ExecutionException, InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node and attach it to the root node
        Node child = new Node("child");
        rootNode.attachChild(child);

        // Now detach it from the root node
        child.removeFromParent();

        // Now we should be able to modify it on another thread
        Future<Void> legalMutationFuture = executorService.submit(() -> {
            Node grandchild = new Node("grandchild");
            child.attachChild(grandchild);
            return null;
        });

        // This should complete without exceptions
        legalMutationFuture.get();
    }

    /**
     * Test that adding a child to the root node also restricts the grandchild.
     * This test will add a grandchild to a child BEFORE adding the child to the root,
     * then try (and fail) to make an illegal on-thread change to the grandchild.
     */
    @Test
    public void testAddingAChildToTheRootNodeAlsoRestrictsTheGrandChild() throws InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node and a grandchild node
        Node child = new Node("child");
        Node grandchild = new Node("grandchild");

        // Attach the grandchild to the child BEFORE adding the child to the root
        child.attachChild(grandchild);

        // Now attach the child to the root node
        rootNode.attachChild(child);

        // Try to make an illegal on-thread change to the grandchild
        Future<Void> illegalMutationFuture = executorService.submit(() -> {
            // This should fail because the grandchild is now restricted
            Node greatGrandchild = new Node("greatGrandchild");
            grandchild.attachChild(greatGrandchild);
            return null;
        });

        try {
            illegalMutationFuture.get();
            fail("Expected an IllegalThreadSceneGraphMutation exception");
        } catch (ExecutionException e) {
            // This is expected - verify it's the right exception type
            assertTrue("Expected IllegalThreadSceneGraphMutation, got: " + e.getCause().getClass().getName(),
                    e.getCause() instanceof IllegalThreadSceneGraphMutation);
        }
    }

    /**
     * Test that removing a child from the root node also unrestricts the grandchild.
     * This test will add a child with a grandchild to the root node, then remove the child
     * and verify that the grandchild can be modified on a non-main thread.
     */
    @Test
    public void testRemovingAChildFromTheRootNodeAlsoUnrestrictsTheGrandChild() throws ExecutionException, InterruptedException {
        Node rootNode = new Node("root");
        SceneGraphThreadWarden.setup(rootNode);

        // Create a child node and a grandchild node
        Node child = new Node("child");
        Node grandchild = new Node("grandchild");

        // Attach the grandchild to the child
        child.attachChild(grandchild);

        // Attach the child to the root node
        rootNode.attachChild(child);

        // Now remove the child from the root node
        child.removeFromParent();

        // Try to make a change to the grandchild on a non-main thread
        Future<Void> legalMutationFuture = executorService.submit(() -> {
            // This should succeed because the grandchild is no longer restricted
            Node greatGrandchild = new Node("greatGrandchild");
            grandchild.attachChild(greatGrandchild);
            return null;
        });

        // This should complete without exceptions
        legalMutationFuture.get();
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
