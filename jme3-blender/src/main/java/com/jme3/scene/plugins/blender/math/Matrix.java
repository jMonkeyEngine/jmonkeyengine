package com.jme3.scene.plugins.blender.math;

import java.text.DecimalFormat;

import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

import com.jme3.math.FastMath;

/**
 * Encapsulates a 4x4 matrix
 *
 *
 */
public class Matrix extends SimpleMatrix {
    private static final long serialVersionUID = 2396600537315902559L;

    public Matrix(int rows, int cols) {
        super(rows, cols);
    }

    /**
     * Copy constructor
     */
    public Matrix(SimpleMatrix m) {
        super(m);
    }
    
    public Matrix(double[][] data) {
        super(data);
    }
    
    public static Matrix identity(int size) {
        Matrix result = new Matrix(size, size);
        CommonOps.setIdentity(result.mat);
        return result;
    }
    
    public Matrix pseudoinverse() {
        return this.pseudoinverse(1);
    }
    
    @SuppressWarnings("unchecked")
    public Matrix pseudoinverse(double lambda) {
        SimpleSVD<SimpleMatrix> simpleSVD = this.svd();
        
        SimpleMatrix U = simpleSVD.getU();
        SimpleMatrix S = simpleSVD.getW();
        SimpleMatrix V = simpleSVD.getV();
        
        int N = Math.min(this.numRows(),this.numCols());
        double maxSingular = 0;
        for( int i = 0; i < N; ++i ) {
            if( S.get(i, i) > maxSingular ) {
                maxSingular = S.get(i, i);
            }
        }
        
        double tolerance = FastMath.DBL_EPSILON * Math.max(this.numRows(),this.numCols()) * maxSingular;
        for(int i=0;i<Math.min(S.numRows(), S.numCols());++i) {
            double a = S.get(i, i);
            if(a <= tolerance) {
                a = 0;
            } else {
                a = a/(a * a + lambda * lambda);
            }
            S.set(i, i, a);
        }
        return new Matrix(V.mult(S.transpose()).mult(U.transpose()));
    }
    
    public void setColumn(Vector3d col, int column) {
        this.setColumn(column, 0, col.x, col.y, col.z);
    }
    
    /**
     * Just for some debug informations in order to compare the results with the scilab computation program.
     * @param name the name of the matrix
     * @param m the matrix to print out
     * @return the String format of the matrix to easily input it to Scilab
     */
    public String toScilabString(String name, SimpleMatrix m) {
        String result = name + " = [";
        
        for(int i=0;i<m.numRows();++i) {
            for(int j=0;j<m.numCols();++j) {
                result += m.get(i, j) + " ";
            }
            result += ";";
        }
        
        return result;
    }
    
    /**
     * @return a String representation of the matrix
     */
    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.0000");
        StringBuilder buf = new StringBuilder();
        for (int r = 0; r < this.numRows(); ++r) {
            buf.append("\n| ");
            for (int c = 0; c < this.numCols(); ++c) {
                buf.append(df.format(this.get(r, c))).append(' ');
            }
            buf.append('|');
        }
        return buf.toString();
    }
    
    public void setTranslation(Vector3d translation) {
        this.setColumn(translation, 3);
    }
    
    /**
     * Sets the scale.
     * 
     * @param scale
     *            the scale vector to set
     */
    public void setScale(Vector3d scale) {
        this.setScale(scale.x, scale.y, scale.z);
    }
    
    /**
     * Sets the scale.
     * 
     * @param x
     *            the X scale
     * @param y
     *            the Y scale
     * @param z
     *            the Z scale
     */
    public void setScale(double x, double y, double z) {
        Vector3d vect1 = new Vector3d(this.get(0, 0), this.get(1, 0), this.get(2, 0));
        vect1.normalizeLocal().multLocal(x);
        this.set(0, 0, vect1.x);
        this.set(1, 0, vect1.y);
        this.set(2, 0, vect1.z);

        vect1.set(this.get(0, 1), this.get(1, 1), this.get(2, 1));
        vect1.normalizeLocal().multLocal(y);
        this.set(0, 1, vect1.x);
        this.set(1, 1, vect1.y);
        this.set(2, 1, vect1.z);

        vect1.set(this.get(0, 2), this.get(1, 2), this.get(2, 2));
        vect1.normalizeLocal().multLocal(z);
        this.set(0, 2, vect1.x);
        this.set(1, 2, vect1.y);
        this.set(2, 2, vect1.z);
    }
    
    /**
     * <code>setRotationQuaternion</code> builds a rotation from a
     * <code>Quaternion</code>.
     * 
     * @param quat
     *            the quaternion to build the rotation from.
     * @throws NullPointerException
     *             if quat is null.
     */
    public void setRotationQuaternion(DQuaternion quat) {
        quat.toRotationMatrix(this);
    }
    
    public DTransform toTransform() {
        DTransform result = new DTransform();
        result.setTranslation(this.toTranslationVector());
        result.setRotation(this.toRotationQuat());
        result.setScale(this.toScaleVector());
        return result;
    }
    
    public Vector3d toTranslationVector() {
        return new Vector3d(this.get(0, 3), this.get(1, 3), this.get(2, 3));
    }
    
    public DQuaternion toRotationQuat() {
        DQuaternion quat = new DQuaternion();
        quat.fromRotationMatrix(this.get(0, 0), this.get(0, 1), this.get(0, 2), this.get(1, 0), this.get(1, 1), this.get(1, 2), this.get(2, 0), this.get(2, 1), this.get(2, 2));
        return quat;
    }
    
    /**
     * Retrieves the scale vector from the matrix and stores it into a given
     * vector.
     */
    public Vector3d toScaleVector() {
        Vector3d result = new Vector3d();
        this.toScaleVector(result);
        return result;
    }
    
    /**
     * Retrieves the scale vector from the matrix and stores it into a given
     * vector.
     * 
     * @param vector the vector where the scale will be stored
     */
    public void toScaleVector(Vector3d vector) {
        double scaleX = Math.sqrt(this.get(0, 0) * this.get(0, 0) + this.get(1, 0) * this.get(1, 0) + this.get(2, 0) * this.get(2, 0));
        double scaleY = Math.sqrt(this.get(0, 1) * this.get(0, 1) + this.get(1, 1) * this.get(1, 1) + this.get(2, 1) * this.get(2, 1));
        double scaleZ = Math.sqrt(this.get(0, 2) * this.get(0, 2) + this.get(1, 2) * this.get(1, 2) + this.get(2, 2) * this.get(2, 2));
        vector.set(scaleX, scaleY, scaleZ);
    }
}
