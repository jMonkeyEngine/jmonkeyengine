package com.jme3.app.state;

import com.jme3.app.LegacyApplication;

class AppStateManagerTest {

    static class AttachTest extends GroovyTestCase {
        
        void testDuplicateId() {
            def state1 = new AbstractAppState("test1") {};
            def state2 = new AbstractAppState("test1") {};
            
            def app = new LegacyApplication();
            
            app.getStateManager().attach(state1);
            
            shouldFail(IllegalArgumentException) {
                app.getStateManager().attach(state2);
            }
        }
        
        void testDuplicateNullId() {
            // Make sure that two states without an ID can
            // still be registered. 
            def state1 = new AbstractAppState() {};
            def state2 = new AbstractAppState() {};
            
            def app = new LegacyApplication();
            
            app.getStateManager().attach(state1);
            app.getStateManager().attach(state2);
        }
    }

    static class GetStateWithIdTest extends GroovyTestCase {
        void testIdHit() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
            
            assertNotNull app.stateManager.getState("test1", AppState.class);
        }
        
        void testIdMiss() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
            
            assertNull app.stateManager.getState("test2", AppState.class);
        }
        
        void testDetached() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
            app.stateManager.detach(state);
            
            assertNull app.stateManager.getState("test2", AppState.class);
        }
    }
    
    static class StateForIdTest extends GroovyTestCase {
        void testIdHit() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
            
            assertNotNull app.stateManager.stateForId("test1", AppState.class);
        }
        
        void testIdMiss() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
 
            shouldFail(IllegalArgumentException) {           
                app.stateManager.stateForId("test2", AppState.class);
            }
        }
        
        void testDetached() {
            def state = new AbstractAppState("test1") {};          
            def app = new LegacyApplication();
            
            app.stateManager.attach(state);
            app.stateManager.detach(state);
            
            shouldFail(IllegalArgumentException) {           
                app.stateManager.stateForId("test2", AppState.class);
            }
        }
    }
}
