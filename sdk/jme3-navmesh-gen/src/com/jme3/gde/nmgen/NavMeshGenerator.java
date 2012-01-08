package com.jme3.gde.nmgen;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.terrain.Terrain;
import java.io.IOException;
import java.nio.FloatBuffer;
import org.critterai.nmgen.IntermediateData;
import org.critterai.nmgen.NavmeshGenerator;
import org.critterai.nmgen.TriangleMesh;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

public class NavMeshGenerator implements Savable {

    private org.critterai.nmgen.NavmeshGenerator nmgen;
    private float cellSize = 1f;
    private float cellHeight = 1.5f;
    private float minTraversableHeight = 7.5f;
    private float maxTraversableStep = 1f;
    private float maxTraversableSlope = 48.0f;
    private boolean clipLedges = false;
    private float traversableAreaBorderSize = 1.2f;
    private int smoothingThreshold = 2;
    private boolean useConservativeExpansion = true;
    private int minUnconnectedRegionSize = 3;
    private int mergeRegionSize = 10;
    private float maxEdgeLength = 0;
    private float edgeMaxDeviation = 2.4f;
    private int maxVertsPerPoly = 6;
    private float contourSampleDistance = 25;
    private float contourMaxDeviation = 25;
    private IntermediateData intermediateData;
    private int timeout = 10000;

    public NavMeshGenerator() {
    }

    public void printParams() {
        System.out.println("Cell Size: " + cellSize);
        System.out.println("Cell Height: " + cellHeight);
        System.out.println("Min Trav. Height: " + minTraversableHeight);
        System.out.println("Max Trav. Step: " + maxTraversableStep);
        System.out.println("Max Trav. Slope: " + maxTraversableSlope);
        System.out.println("Clip Ledges: " + clipLedges);
        System.out.println("Trav. Area Border Size: " + traversableAreaBorderSize);
        System.out.println("Smooth Thresh.: " + smoothingThreshold);
        System.out.println("Use Cons. Expansion: " + useConservativeExpansion);
        System.out.println("Min Unconn. Region Size: " + minUnconnectedRegionSize);
        System.out.println("Merge Region Size: " + mergeRegionSize);
        System.out.println("Max Edge Length: " + maxEdgeLength);
        System.out.println("Edge Max Dev.: " + edgeMaxDeviation);
        System.out.println("Max Verts/Poly: " + maxVertsPerPoly);
        System.out.println("Contour Sample Dist: " + contourSampleDistance);
        System.out.println("Contour Max Dev.: " + contourMaxDeviation);
    }

    public void setIntermediateData(IntermediateData data) {
        this.intermediateData = data;
    }

    public Mesh optimize(Mesh mesh) {
        nmgen = new NavmeshGenerator(cellSize, cellHeight, minTraversableHeight,
                maxTraversableStep, maxTraversableSlope,
                clipLedges, traversableAreaBorderSize,
                smoothingThreshold, useConservativeExpansion,
                minUnconnectedRegionSize, mergeRegionSize,
                maxEdgeLength, edgeMaxDeviation, maxVertsPerPoly,
                contourSampleDistance, contourMaxDeviation);

        FloatBuffer pb = mesh.getFloatBuffer(Type.Position);
        IndexBuffer ib = mesh.getIndexBuffer();

        // copy positions to float array
        float[] positions = new float[pb.capacity()];
        pb.clear();
        pb.get(positions);

        // generate int array of indices
        int[] indices = new int[ib.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = ib.get(i);
        }


        TriangleMesh triMesh = buildNavMesh(positions, indices, intermediateData);
        if (triMesh == null) {
            return null;
        }

        int[] indices2 = triMesh.indices;
        float[] positions2 = triMesh.vertices;

        Mesh mesh2 = new Mesh();
        mesh2.setBuffer(Type.Position, 3, positions2);
        mesh2.setBuffer(Type.Index, 3, indices2);
        mesh2.updateBound();
        mesh2.updateCounts();

        return mesh2;
    }

    private TriangleMesh buildNavMesh(float[] positions, int[] indices, IntermediateData intermediateData) {
        MeshBuildRunnable runnable = new MeshBuildRunnable(positions, indices, intermediateData);
        try {
            execute(runnable, timeout);
        } catch (TimeoutException ex) {
            DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("NavMesh Generation timed out."));
        }
        return runnable.getTriMesh();
    }

    private static void execute(Thread task, long timeout) throws TimeoutException {
        task.start();
        try {
            task.join(timeout);
        } catch (InterruptedException e) {
        }
        if (task.isAlive()) {
            task.interrupt();
            throw new TimeoutException();
        }
    }

    private static void execute(Runnable task, long timeout) throws TimeoutException {
        Thread t = new Thread(task, "Timeout guard");
        t.setDaemon(true);
        execute(t, timeout);
    }

    public Mesh terrain2mesh(Terrain terr) {
        float[] heights = terr.getHeightMap();
        int length = heights.length;
        int side = (int) FastMath.sqrt(heights.length);
        float[] vertices = new float[length * 3];
        int[] indices = new int[(side - 1) * (side - 1) * 6];

        Vector3f scale = ((Node) terr).getWorldScale().clone();
        Vector3f trans = ((Node) terr).getWorldTranslation().clone();
        trans.x -= terr.getTerrainSize() / 2f;
        trans.z -= terr.getTerrainSize() / 2f;
        float offsetX = trans.x * scale.x;
        float offsetZ = trans.z * scale.z;

        // do vertices
        int i = 0;
        for (int z = 0; z < side; z++) {
            for (int x = 0; x < side; x++) {
                vertices[i++] = x + offsetX;
                vertices[i++] = heights[z * side + x] * scale.y;
                vertices[i++] = z + offsetZ;
            }
        }

        // do indexes
        i = 0;
        for (int z = 0; z < side - 1; z++) {
            for (int x = 0; x < side - 1; x++) {
                // triangle 1
                indices[i++] = z * side + x;
                indices[i++] = (z + 1) * side + x;
                indices[i++] = (z + 1) * side + x + 1;
                // triangle 2
                indices[i++] = z * side + x;
                indices[i++] = (z + 1) * side + x + 1;
                indices[i++] = z * side + x + 1;
            }
        }

        Mesh mesh2 = new Mesh();
        mesh2.setBuffer(Type.Position, 3, vertices);
        mesh2.setBuffer(Type.Index, 3, indices);
        mesh2.updateBound();
        mesh2.updateCounts();

        return mesh2;
    }

    /**
     * @return The height resolution used when sampling the source mesh. Value must be > 0.
     */
    public float getCellHeight() {
        return cellHeight;
    }

    /**
     * @param cellHeight - The height resolution used when sampling the source mesh. Value must be > 0.
     */
    public void setCellHeight(float cellHeight) {
        this.cellHeight = cellHeight;
    }

    /**
     * @return The width and depth resolution used when sampling the the source mesh.
     */
    public float getCellSize() {
        return cellSize;
    }

    /**
     * @param cellSize - The width and depth resolution used when sampling the the source mesh.
     */
    public void setCellSize(float cellSize) {
        this.cellSize = cellSize;
    }

    public boolean isClipLedges() {
        return clipLedges;
    }

    public void setClipLedges(boolean clipLedges) {
        this.clipLedges = clipLedges;
    }

    public float getContourMaxDeviation() {
        return contourMaxDeviation;
    }

    public void setContourMaxDeviation(float contourMaxDeviation) {
        this.contourMaxDeviation = contourMaxDeviation;
    }

    public float getContourSampleDistance() {
        return contourSampleDistance;
    }

    public void setContourSampleDistance(float contourSampleDistance) {
        this.contourSampleDistance = contourSampleDistance;
    }

    public float getEdgeMaxDeviation() {
        return edgeMaxDeviation;
    }

    public void setEdgeMaxDeviation(float edgeMaxDeviation) {
        this.edgeMaxDeviation = edgeMaxDeviation;
    }

    public float getMaxEdgeLength() {
        return maxEdgeLength;
    }

    public void setMaxEdgeLength(float maxEdgeLength) {
        this.maxEdgeLength = maxEdgeLength;
    }

    public float getMaxTraversableSlope() {
        return maxTraversableSlope;
    }

    public void setMaxTraversableSlope(float maxTraversableSlope) {
        this.maxTraversableSlope = maxTraversableSlope;
    }

    public float getMaxTraversableStep() {
        return maxTraversableStep;
    }

    public void setMaxTraversableStep(float maxTraversableStep) {
        this.maxTraversableStep = maxTraversableStep;
    }

    public int getMaxVertsPerPoly() {
        return maxVertsPerPoly;
    }

    public void setMaxVertsPerPoly(int maxVertsPerPoly) {
        this.maxVertsPerPoly = maxVertsPerPoly;
    }

    public int getMergeRegionSize() {
        return mergeRegionSize;
    }

    public void setMergeRegionSize(int mergeRegionSize) {
        this.mergeRegionSize = mergeRegionSize;
    }

    public float getMinTraversableHeight() {
        return minTraversableHeight;
    }

    public void setMinTraversableHeight(float minTraversableHeight) {
        this.minTraversableHeight = minTraversableHeight;
    }

    public int getMinUnconnectedRegionSize() {
        return minUnconnectedRegionSize;
    }

    public void setMinUnconnectedRegionSize(int minUnconnectedRegionSize) {
        this.minUnconnectedRegionSize = minUnconnectedRegionSize;
    }

    public NavmeshGenerator getNmgen() {
        return nmgen;
    }

    public void setNmgen(NavmeshGenerator nmgen) {
        this.nmgen = nmgen;
    }

    public int getSmoothingThreshold() {
        return smoothingThreshold;
    }

    public void setSmoothingThreshold(int smoothingThreshold) {
        this.smoothingThreshold = smoothingThreshold;
    }

    public float getTraversableAreaBorderSize() {
        return traversableAreaBorderSize;
    }

    public void setTraversableAreaBorderSize(float traversableAreaBorderSize) {
        this.traversableAreaBorderSize = traversableAreaBorderSize;
    }

    public boolean isUseConservativeExpansion() {
        return useConservativeExpansion;
    }

    public void setUseConservativeExpansion(boolean useConservativeExpansion) {
        this.useConservativeExpansion = useConservativeExpansion;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(cellSize, "cellSize", 1f);
        oc.write(cellHeight, "cellHeight", 1.5f);
        oc.write(minTraversableHeight, "minTraversableHeight", 7.5f);
        oc.write(maxTraversableStep, "maxTraversableStep", 1f);
        oc.write(maxTraversableSlope, "maxTraversableSlope", 48f);
        oc.write(clipLedges, "clipLedges", false);
        oc.write(traversableAreaBorderSize, "traversableAreaBorderSize", 1.2f);
        oc.write(smoothingThreshold, "smoothingThreshold", 2);
        oc.write(useConservativeExpansion, "useConservativeExpansion", true);
        oc.write(minUnconnectedRegionSize, "minUnconnectedRegionSize", 3);
        oc.write(mergeRegionSize, "mergeRegionSize", 10);
        oc.write(maxEdgeLength, "maxEdgeLength", 0);
        oc.write(edgeMaxDeviation, "edgeMaxDeviation", 2.4f);
        oc.write(maxVertsPerPoly, "maxVertsPerPoly", 6);
        oc.write(contourSampleDistance, "contourSampleDistance", 25);
        oc.write(contourMaxDeviation, "contourMaxDeviation", 25);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        cellSize = ic.readFloat("cellSize", 1f);
        cellHeight = ic.readFloat("cellHeight", 1.5f);
        minTraversableHeight = ic.readFloat("minTraversableHeight", 7.5f);
        maxTraversableStep = ic.readFloat("maxTraversableStep", 1f);
        maxTraversableSlope = ic.readFloat("maxTraversableSlope", 48f);
        clipLedges = ic.readBoolean("clipLedges", false);
        traversableAreaBorderSize = ic.readFloat("traversableAreaBorderSize", 1.2f);
        smoothingThreshold = (int) ic.readFloat("smoothingThreshold", 2);
        useConservativeExpansion = ic.readBoolean("useConservativeExpansion", true);
        minUnconnectedRegionSize = (int) ic.readFloat("minUnconnectedRegionSize", 3);
        mergeRegionSize = (int) ic.readFloat("mergeRegionSize", 10);
        maxEdgeLength = ic.readFloat("maxEdgeLength", 0);
        edgeMaxDeviation = ic.readFloat("edgeMaxDeviation", 2.4f);
        maxVertsPerPoly = (int) ic.readFloat("maxVertsPerPoly", 6);
        contourSampleDistance = ic.readFloat("contourSampleDistance", 25);
        contourMaxDeviation = ic.readFloat("contourMaxDeviation", 25);
    }

    private class MeshBuildRunnable implements Runnable {

        private float[] positions;
        private int[] indices;
        private IntermediateData intermediateData;
        private TriangleMesh triMesh;

        public MeshBuildRunnable(float[] positions, int[] indices, IntermediateData intermediateData) {
            this.positions = positions;
            this.indices = indices;
            this.intermediateData = intermediateData;
        }

        @Override
        public void run() {
            triMesh = nmgen.build(positions, indices, intermediateData);
        }

        public TriangleMesh getTriMesh() {
            return triMesh;
        }
    }

    public static class TimeoutException extends Exception {

        /** Create an instance */
        public TimeoutException() {
        }
    }
}
