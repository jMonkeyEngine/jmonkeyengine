package com.jme3.util;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;


import java.util.concurrent.TimeUnit;

/**
 * 
 * @author Julien Seinturier - COMEX SA - <a href="http://www.seinturier.fr">http://www.seinturier.fr</a>
 *
 */
public class VRUtil {

    private static final long SLEEP_PRECISION = TimeUnit.MILLISECONDS.toNanos(4);
    private static final long SPIN_YIELD_PRECISION = TimeUnit.MILLISECONDS.toNanos(2);
    
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private VRUtil() {
    }

    public static void sleepNanos(long nanoDuration) {
        final long end = System.nanoTime() + nanoDuration; 
        long timeLeft = nanoDuration; 
        do { 
            try {
                if (timeLeft > SLEEP_PRECISION) {
                    Thread.sleep(1); 
                } else if (timeLeft > SPIN_YIELD_PRECISION) {
                    Thread.sleep(0); 
                }
            } catch(Exception e) { }
            timeLeft = end - System.nanoTime(); 
        } while (timeLeft > 0); 
    }
        

    
    
    public static void convertMatrix4toQuat(Matrix4f in, Quaternion out) {
        // convert rotation matrix to quat
        out.fromRotationMatrix(in.m00, in.m01, in.m02, in.m10, in.m11, in.m12, in.m20, in.m21, in.m22);
        // flip the pitch
        out.set(-out.getX(), out.getY(), -out.getZ(), out.getW());
    }
        
    public static Quaternion FastFullAngles(Quaternion use, float yaw, float pitch, float roll) {
        float angle;
        float sinRoll, sinPitch, sinYaw, cosRoll, cosPitch, cosYaw;
        angle = roll * 0.5f;
        sinPitch = (float)Math.sin(angle);
        cosPitch = (float)Math.cos(angle);
        angle = yaw * 0.5f;
        sinRoll = (float)Math.sin(angle);
        cosRoll = (float)Math.cos(angle);
        angle = pitch * 0.5f;
        sinYaw = (float)Math.sin(angle);
        cosYaw = (float)Math.cos(angle);

        // variables used to reduce multiplication calls.
        float cosRollXcosPitch = cosRoll * cosPitch;
        float sinRollXsinPitch = sinRoll * sinPitch;
        float cosRollXsinPitch = cosRoll * sinPitch;
        float sinRollXcosPitch = sinRoll * cosPitch;

        use.set((cosRollXcosPitch * sinYaw + sinRollXsinPitch * cosYaw),
                (sinRollXcosPitch * cosYaw + cosRollXsinPitch * sinYaw),
                (cosRollXsinPitch * cosYaw - sinRollXcosPitch * sinYaw),
                (cosRollXcosPitch * cosYaw - sinRollXsinPitch * sinYaw));

        return use;     
    }    
    
    public static Quaternion stripToYaw(Quaternion q) {
        float yaw;
        float w = q.getW();
        float x = q.getX();
        float y = q.getY();
        float z = q.getZ();
        float sqx = x*x;
        float sqy = y*y;
        float sqz = z*z;
        float sqw = w*w;
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = x * y + z * w;
        if (test > 0.499 * unit) { // singularity at north pole
            yaw = 2 * FastMath.atan2(x, w);
        } else if (test < -0.499 * unit) { // singularity at south pole
            yaw = -2 * FastMath.atan2(x, w);
        } else {
            yaw = FastMath.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // roll or heading 
        }
        FastFullAngles(q, yaw, 0f, 0f);
        return q;
    }    
}
