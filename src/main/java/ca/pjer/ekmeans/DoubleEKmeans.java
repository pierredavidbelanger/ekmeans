package ca.pjer.ekmeans;

import java.util.Arrays;

public class DoubleEKmeans extends AbstractEKmeans<double[], double[]> {

    public interface DoubleDistanceFunction {

        double distance(double[] p1, double[] p2);

    }

    public static final DoubleDistanceFunction EUCLIDEAN_DISTANCE_FUNCTION = new DoubleDistanceFunction() {

        public double distance(double[] p1, double[] p2) {
            double s = 0;
            for (int d = 0; d < p1.length && d < p2.length; d++) {
                s += Math.pow(Math.abs(p1[d] - p2[d]), 2);
            }
            return Math.sqrt(s);
        }
    };

    public static final DoubleDistanceFunction MANHATTAN_DISTANCE_FUNCTION = new DoubleDistanceFunction() {

        public double distance(double[] p1, double[] p2) {
            double s = 0;
            for (int d = 0; d < p1.length && d < p2.length; d++) {
                s += Math.abs(p1[d] - p2[d]);
            }
            return s;
        }
    };

    protected static class DistanceFunction implements AbstractEKmeans.DistanceFunction<double[], double[]> {

        private final DoubleDistanceFunction doubleDistanceFunction;

        public DistanceFunction(DoubleDistanceFunction doubleDistanceFunction) {
            this.doubleDistanceFunction = doubleDistanceFunction;
        }

        public void distance(boolean[] changed, double[][] distances, double[][] centroids, double[][] points) {
            for (int c = 0; c < centroids.length; c++) {
                if (!changed[c]) continue;
                double[] centroid = centroids[c];
                for (int p = 0; p < points.length; p++) {
                    double[] point = points[p];
                    distances[c][p] = doubleDistanceFunction.distance(centroid, point);
                }
            }
        }
    }

    protected static class CenterFunction implements AbstractEKmeans.CenterFunction<double[], double[]> {

        public void center(boolean[] changed, int[] assignments, double[][] centroids, double[][] points) {
            for (int c = 0; c < centroids.length; c++) {
                if (!changed[c]) continue;
                double[] centroid = centroids[c];
                int n = 0;
                for (int p = 0; p < points.length; p++) {
                    if (assignments[p] != c) continue;
                    double[] point = points[p];
                    if (n++ == 0) Arrays.fill(centroid, 0d);
                    for (int d = 0; d < centroid.length && d < point.length; d++) {
                        centroid[d] += point[d];
                    }
                }
                if (n > 0) {
                    for (int d = 0; d < centroid.length; d++) {
                        centroid[d] /= n;
                    }
                }
            }
        }
    }

    public DoubleEKmeans(double[][] centroids, double[][] points, boolean equal, DoubleDistanceFunction doubleDistanceFunction, Listener listener) {
        super(centroids, points, equal, new DistanceFunction(doubleDistanceFunction), new CenterFunction(), listener);
    }
}
