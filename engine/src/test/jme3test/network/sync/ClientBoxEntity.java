package jme3test.network.sync;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class ClientBoxEntity extends BoxEntity {

        private Vector3f prevPos = new Vector3f();

        public ClientBoxEntity(AssetManager assetManager, ColorRGBA color){
            super(assetManager, color);
        }

        @Override
        public void onRemoteCreate() {
            System.out.println("ClientBoxEntity created");
        }

        @Override
        public void onRemoteDelete() {
            System.out.println("ClientBoxEntity deleted");
        }

        @Override
        public void onRemoteUpdate(float latencyDelta) {
            prevPos.set(getLocalTranslation());
            pos.addLocal(vel.mult(latencyDelta));
        }

        @Override
        public void interpolate(float blendAmount) {
            if (pos != null){
                getLocalTranslation().interpolate(prevPos, pos, blendAmount);
                setLocalTranslation(getLocalTranslation());
            }
        }

        @Override
        public void extrapolate(float tpf) {
            if (pos != null){
                pos.addLocal(vel.mult(tpf));
                setLocalTranslation(pos);
            }
        }
    }