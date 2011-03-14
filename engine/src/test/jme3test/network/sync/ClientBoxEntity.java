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

        private static float interpolateCubic(float v0, float v1, float v2, float v3, float x){
            float p = (v3 - v2) - (v0 - v1);
            float q = (v0 - v1) - p;
            float r = v2 - v0;
            float s = v1;

            return p * x * x * x
                 + q * x * x
                 + r * x
                 + s;
        }

        private static Vector3f interpolateCubic(Vector3f v0, Vector3f v1,
                                                 Vector3f v2, Vector3f v3,
                                                 float x){
            Vector3f vec = new Vector3f();
            vec.x = interpolateCubic(v0.x, v1.x, v2.x, v3.x, x);
            vec.y = interpolateCubic(v0.y, v1.y, v2.y, v3.y, x);
            vec.z = interpolateCubic(v0.z, v1.z, v2.z, v3.z, x);
            return vec;
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