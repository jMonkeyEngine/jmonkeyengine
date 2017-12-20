package com.jme3.math;

/**
 * Created by Nehon on 23/04/2017.
 */
public class MathUtils {

    public static Quaternion log(Quaternion q, Quaternion store) {
        float a = FastMath.acos(q.w);
        float sina = FastMath.sin(a);

        store.w = 0;
        if (sina > 0) {
            store.x = a * q.x / sina;
            store.y = a * q.y / sina;
            store.z = a * q.z / sina;
        } else {
            store.x = 0;
            store.y = 0;
            store.z = 0;
        }
        return store;
    }

    public static Quaternion exp(Quaternion q, Quaternion store) {

        float len = FastMath.sqrt(q.x * q.x + q.y * q.y + q.z * q.z);
        float sinLen = FastMath.sin(len);
        float cosLen = FastMath.cos(len);

        store.w = cosLen;
        if (len > 0) {
            store.x = sinLen * q.x / len;
            store.y = sinLen * q.y / len;
            store.z = sinLen * q.z / len;
        } else {
            store.x = 0;
            store.y = 0;
            store.z = 0;
        }
        return store;
    }

    //! This version of slerp, used by squad, does not check for theta > 90.
    public static Quaternion slerpNoInvert(Quaternion q1, Quaternion q2, float t, Quaternion store) {
        float dot = q1.dot(q2);

        if (dot > -0.95f && dot < 0.95f) {
            float angle = FastMath.acos(dot);
            float sin1 = FastMath.sin(angle * (1 - t));
            float sin2 = FastMath.sin(angle * t);
            float sin3 = FastMath.sin(angle);
            store.x = (q1.x * sin1 + q2.x * sin2) / sin3;
            store.y = (q1.y * sin1 + q2.y * sin2) / sin3;
            store.z = (q1.z * sin1 + q2.z * sin2) / sin3;
            store.w = (q1.w * sin1 + q2.w * sin2) / sin3;
            System.err.println("real slerp");
        } else {
            // if the angle is small, use linear interpolation
            store.set(q1).nlerp(q2, t);
            System.err.println("nlerp");
        }
        return store;
    }

    public static Quaternion slerp(Quaternion q1, Quaternion q2, float t, Quaternion store) {

        float dot = (q1.x * q2.x) + (q1.y * q2.y) + (q1.z * q2.z)
                + (q1.w * q2.w);

        if (dot < 0.0f) {
            // Negate the second quaternion and the result of the dot product
            q2.x = -q2.x;
            q2.y = -q2.y;
            q2.z = -q2.z;
            q2.w = -q2.w;
            dot = -dot;
        }

        // Set the first and second scale for the interpolation
        float scale0 = 1 - t;
        float scale1 = t;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if (dot < 0.9f) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            float theta = FastMath.acos(dot);
            float invSinTheta = 1f / FastMath.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = FastMath.sin((1 - t) * theta) * invSinTheta;
            scale1 = FastMath.sin((t * theta)) * invSinTheta;

            // Calculate the x, y, z and w values for the quaternion by using a
            // special
            // form of linear interpolation for quaternions.
            store.x = (scale0 * q1.x) + (scale1 * q2.x);
            store.y = (scale0 * q1.y) + (scale1 * q2.y);
            store.z = (scale0 * q1.z) + (scale1 * q2.z);
            store.w = (scale0 * q1.w) + (scale1 * q2.w);
        } else {
            store.x = (scale0 * q1.x) + (scale1 * q2.x);
            store.y = (scale0 * q1.y) + (scale1 * q2.y);
            store.z = (scale0 * q1.z) + (scale1 * q2.z);
            store.w = (scale0 * q1.w) + (scale1 * q2.w);
            store.normalizeLocal();
        }
        // Return the interpolated quaternion
        return store;
    }

//    //! Given 3 quaternions, qn-1,qn and qn+1, calculate a control point to be used in spline interpolation
//    private static Quaternion spline(Quaternion qnm1, Quaternion qn, Quaternion qnp1, Quaternion store, Quaternion tmp) {
//        store.set(-qn.x, -qn.y, -qn.z, qn.w);
//        //store.set(qn).inverseLocal();
//        tmp.set(store);
//
//        log(store.multLocal(qnm1), store);
//        log(tmp.multLocal(qnp1), tmp);
//        store.addLocal(tmp).multLocal(1f / -4f);
//        exp(store, tmp);
//        store.set(tmp).multLocal(qn);
//
//        return store.normalizeLocal();
//        //return qn * (((qni * qnm1).log() + (qni * qnp1).log()) / -4).exp();
//    }

    //! Given 3 quaternions, qn-1,qn and qn+1, calculate a control point to be used in spline interpolation
    private static Quaternion spline(Quaternion qnm1, Quaternion qn, Quaternion qnp1, Quaternion store, Quaternion tmp) {
        Quaternion invQn = new Quaternion(-qn.x, -qn.y, -qn.z, qn.w);


        log(invQn.mult(qnp1), tmp);
        log(invQn.mult(qnm1), store);
        store.addLocal(tmp).multLocal(-1f / 4f);
        exp(store, tmp);
        store.set(qn).multLocal(tmp);

        return store.normalizeLocal();
        //return qn * (((qni * qnm1).log() + (qni * qnp1).log()) / -4).exp();
    }


    //! spherical cubic interpolation
    public static Quaternion squad(Quaternion q0, Quaternion q1, Quaternion q2, Quaternion q3, Quaternion a, Quaternion b, float t, Quaternion store) {

        spline(q0, q1, q2, a, store);
        spline(q1, q2, q3, b, store);

        slerp(a, b, t, store);
        slerp(q1, q2, t, a);
        return slerp(a, store, 2 * t * (1 - t), b);
        //slerpNoInvert(a, b, t, store);
        //slerpNoInvert(q1, q2, t, a);
        //return slerpNoInvert(a, store, 2 * t * (1 - t), b);

//        quaternion c = slerpNoInvert(q1, q2, t),
//                d = slerpNoInvert(a, b, t);
//        return slerpNoInvert(c, d, 2 * t * (1 - t));
    }


}
