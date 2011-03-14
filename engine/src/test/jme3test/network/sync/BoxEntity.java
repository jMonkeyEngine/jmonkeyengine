package jme3test.network.sync;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.network.sync.Sync;
import com.jme3.network.sync.SyncEntity;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class BoxEntity extends Geometry implements SyncEntity {

    protected @Sync Vector3f pos;
    protected @Sync Vector3f vel;
    
    public BoxEntity(AssetManager assetManager, ColorRGBA color){
        super("Box", new Box(1,1,1));
        setMaterial(new Material(assetManager, "Common/MatDefs/Misc/SolidColor.j3md"));
        getMaterial().setColor("Color", color);
    }

    public void setPosVel(Vector3f pos, Vector3f vel){
        setLocalTranslation(pos);
        this.pos = pos;
        this.vel = vel;
    }

    public void onRemoteCreate() {
    }

    public void onRemoteUpdate(float latencyDelta) {
    }

    public void onRemoteDelete() {
        removeFromParent();
    }

    public void onLocalUpdate() {
    }

    public void interpolate(float blendAmount) {
    }

    public void extrapolate(float tpf) {
    }
}
