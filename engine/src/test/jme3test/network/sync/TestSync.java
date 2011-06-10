package jme3test.network.sync;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.connection.Client;
import com.jme3.network.connection.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.sync.ClientSyncService;
import com.jme3.network.sync.EntityFactory;
import com.jme3.network.sync.ServerSyncService;
import com.jme3.network.sync.SyncEntity;
import com.jme3.network.sync.SyncMessage;
import java.io.IOException;

public class TestSync extends SimpleApplication implements EntityFactory {

    // Client Variables
    private Client client;
    private ClientSyncService clientSyncServ;
    
    // Server Variables
    private Server server;
    private ServerSyncService serverSyncServ;
    private BoxEntity serverBox;

    private Vector3f targetPos = new Vector3f();
    private float boxSpeed = 3f;
    private EmitterSphereShape randomPosSphere = new EmitterSphereShape(Vector3f.ZERO, 5);

    public static void main(String[] args){
        TestSync app = new TestSync();
        app.start();
    }
    
    public void simpleInitApp(){
        Serializer.registerClass(SyncMessage.class);

        // ----- Start Server -------
        try {
            server = new Server(5110, 5110);
            server.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Create SyncService for server
        serverSyncServ = server.getService(ServerSyncService.class);
        // Create server box entity (red)
        serverBox = new BoxEntity(assetManager, ColorRGBA.Red);
        serverSyncServ.addNpc(serverBox);
        rootNode.attachChild(serverBox);

        // Enable 10% packet drop rate and 200 ms latency
        serverSyncServ.setNetworkSimulationParams(0.1f, 200);


        // ------ Start Client -------
        try {
            client = new Client("localhost", 5110, 5110);
            client.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        clientSyncServ = client.getService(ClientSyncService.class);
        clientSyncServ.setEntityFactory(this);
    }

    /**
     * Create a new entity (for client)
     * @param entityType
     * @return
     */
    public SyncEntity createEntity(Class<? extends SyncEntity> entityType) {
        BoxEntity clientBox = new ClientBoxEntity(assetManager, ColorRGBA.Green);
        rootNode.attachChild(clientBox);
        return clientBox;
    }

    @Override
    public void simpleUpdate(float tpf){
        // --------- Update Client Sync ---------
        clientSyncServ.update(tpf);

        // --------- Update Server Sync ---------
        // if needed determine next box position
        if (serverBox.getLocalTranslation().distance(targetPos) < 0.1f){
            randomPosSphere.getRandomPoint(targetPos);
        }else{
            Vector3f velocity = new Vector3f(targetPos);
            velocity.subtractLocal(serverBox.getLocalTranslation());
            velocity.normalizeLocal().multLocal(boxSpeed);

            Vector3f newPos = serverBox.getLocalTranslation().clone();
            newPos.addLocal(velocity.mult(tpf));
            serverBox.setPosVel(newPos, velocity);
        }

        serverSyncServ.update(tpf);
    }

    @Override
    public void destroy(){
        super.destroy();
        try {
            client.disconnect();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
