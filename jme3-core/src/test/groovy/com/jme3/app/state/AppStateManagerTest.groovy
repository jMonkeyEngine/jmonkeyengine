package com.jme3.app.state;

import com.jme3.app.LegacyApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the state and ID based lookups of {@link AppStateManager}.
 */
class AppStateManagerTest {

    @Test
    void attachStateWithDuplicateIdThrows() {
        def state1 = new AbstractAppState("test1") {};
        def state2 = new AbstractAppState("test1") {};

        def app = new LegacyApplication();

        app.getStateManager().attach(state1);

        assertThrows(IllegalArgumentException, {
            app.getStateManager().attach(state2);
        } as Executable);
    }

    @Test
    void attachStatesWithoutIdSucceeds() {
        // Make sure that two states without an ID can
        // still be registered.
        def state1 = new AbstractAppState() {};
        def state2 = new AbstractAppState() {};

        def app = new LegacyApplication();

        app.getStateManager().attach(state1);
        app.getStateManager().attach(state2);
    }

    @Test
    void getStateByIdHit() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);

        assertNotNull(app.stateManager.getState("test1", AppState.class));
    }

    @Test
    void getStateByIdMiss() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);

        assertNull(app.stateManager.getState("test2", AppState.class));
    }

    @Test
    void getStateByIdDetached() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);
        app.stateManager.detach(state);

        assertNull(app.stateManager.getState("test2", AppState.class));
    }

    @Test
    void stateForIdHit() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);

        assertNotNull(app.stateManager.stateForId("test1", AppState.class));
    }

    @Test
    void stateForIdMiss() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);

        assertThrows(IllegalArgumentException, {
            app.stateManager.stateForId("test2", AppState.class);
        } as Executable);
    }

    @Test
    void stateForIdDetached() {
        def state = new AbstractAppState("test1") {};
        def app = new LegacyApplication();

        app.stateManager.attach(state);
        app.stateManager.detach(state);

        assertThrows(IllegalArgumentException, {
            app.stateManager.stateForId("test2", AppState.class);
        } as Executable);
    }
}
