/*
 * This file is part of ekmeans.
 *
 * ekmeans is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * ekmeans is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Foobar. If not, see <http://www.gnu.org/licenses/>.
 * 
 * ekmeans  Copyright (C) 2012  Pierre-David Belanger <pierredavidbelanger@gmail.com>
 * 
 * Contributor(s): Pierre-David Belanger <pierredavidbelanger@gmail.com>
 */
package com.google.code.ekmeans;

import java.util.Arrays;

public class EKmeans {

    public static interface Listener {

        void iteration(int iteration, int move);
    }

    public static interface DistanceFunction {

        double distance(double[] p1, double[] p2);
    }
    public static final DistanceFunction EUCLIDEAN_DISTANCE_FUNCTION = new DistanceFunction() {

        @Override
        public double distance(double[] p1, double[] p2) {
            double s = 0;
            for (int d = 0; d < p1.length; d++) {
                s += Math.pow(Math.abs(p1[d] - p2[d]), 2);
            }
            double d = Math.sqrt(s);
            return d;
        }
    };
    public static final DistanceFunction MANHATTAN_DISTANCE_FUNCTION = new DistanceFunction() {

        @Override
        public double distance(double[] p1, double[] p2) {
            double s = 0;
            for (int d = 0; d < p1.length; d++) {
                s += Math.abs(p1[d] - p2[d]);
            }
            return s;
        }
    };
    protected double[][] centroids;
    protected double[][] points;
    protected int idealCount;
    protected double[][] distances;
    protected int[] assignments;
    protected boolean[] changes;
    protected int[] counts;
    protected boolean[] dones;
    protected int iteration;
    protected boolean equal;
    protected DistanceFunction distanceFunction;
    protected Listener listener;

    public EKmeans(double[][] centroids, double[][] points) {
        this.centroids = centroids;
        this.points = points;
        if (centroids.length > 0) {
            idealCount = points.length / centroids.length;
        } else {
            idealCount = 0;
        }
        distances = new double[centroids.length][points.length];
        assignments = new int[points.length];
        Arrays.fill(assignments, -1);
        changes = new boolean[centroids.length];
        Arrays.fill(changes, true);
        counts = new int[centroids.length];
        dones = new boolean[centroids.length];
        iteration = 128;
        equal = false;
        distanceFunction = EUCLIDEAN_DISTANCE_FUNCTION;
        listener = null;
    }

    public double[][] getCentroids() {
        return centroids;
    }

    public double[][] getPoints() {
        return points;
    }

    public double[][] getDistances() {
        return distances;
    }

    public int[] getAssignments() {
        return assignments;
    }

    public boolean[] getChanges() {
        return changes;
    }

    public int[] getCounts() {
        return counts;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public boolean isEqual() {
        return equal;
    }

    public void setEqual(boolean equal) {
        this.equal = equal;
    }

    public DistanceFunction getDistanceFunction() {
        return distanceFunction;
    }

    public void setDistanceFunction(DistanceFunction distanceFunction) {
        this.distanceFunction = distanceFunction;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void run() {
        calculateDistances();
        int move = makeAssignments();
        int i = 0;
        while (move > 0 && i++ < iteration) {
            if (points.length >= centroids.length) {
                move = fillEmptyCentroids();
//                calculateDistances();
            }
            moveCentroids();
            calculateDistances();
            move += makeAssignments();
            if (listener != null) {
                listener.iteration(i, move);
            }
        }
    }

    protected void calculateDistances() {
        for (int c = 0; c < centroids.length; c++) {
            if (!changes[c]) {
                continue;
            }
            double[] centroid = centroids[c];
            for (int p = 0; p < points.length; p++) {
                double[] point = points[p];
                distances[c][p] = distanceFunction.distance(centroid, point);
            }
            changes[c] = false;
        }
    }

    protected int makeAssignments() {
        int move = 0;
        Arrays.fill(counts, 0);
        for (int p = 0; p < points.length; p++) {
            int nc = nearestCentroid(p);
            if (nc == -1) {
                continue;
            }
            if (assignments[p] != nc) {
                if (assignments[p] != -1) {
                    changes[assignments[p]] = true;
                }
                changes[nc] = true;
                assignments[p] = nc;
                move++;
            }
            counts[nc]++;
            if (equal && counts[nc] > idealCount) {
                move += remakeAssignments(nc);
            }
        }
        return move;
    }

    protected int remakeAssignments(int cc) {
        int move = 0;
        double md = Double.MAX_VALUE;
        int nc = -1;
        int np = -1;
        for (int p = 0; p < points.length; p++) {
            if (assignments[p] != cc) {
                continue;
            }
            for (int c = 0; c < centroids.length; c++) {
                if (c == cc || dones[c]) {
                    continue;
                }
                double d = distances[c][p];
                if (d < md) {
                    md = d;
                    nc = c;
                    np = p;
                }
            }
        }
        if (nc != -1 && np != -1) {
            if (assignments[np] != nc) {
                if (assignments[np] != -1) {
                    changes[assignments[np]] = true;
                }
                changes[nc] = true;
                assignments[np] = nc;
                move++;
            }
            counts[cc]--;
            counts[nc]++;
            if (counts[nc] > idealCount) {
                dones[cc] = true;
                move += remakeAssignments(nc);
                dones[cc] = false;
            }
        }
        return move;
    }

    protected int nearestCentroid(int p) {
        double md = Double.MAX_VALUE;
        int nc = -1;
        for (int c = 0; c < centroids.length; c++) {
            double d = distances[c][p];
            if (d < md) {
                md = d;
                nc = c;
            }
        }
        return nc;
    }

    protected int nearestPoint(int inc, int fromc) {
        double md = Double.MAX_VALUE;
        int np = -1;
        for (int p = 0; p < points.length; p++) {
            if (assignments[p] != inc) {
                continue;
            }
            double d = distances[fromc][p];
            if (d < md) {
                md = d;
                np = p;
            }
        }
        return np;
    }

    protected int largestCentroid(int except) {
        int lc = -1;
        int mc = 0;
        for (int c = 0; c < centroids.length; c++) {
            if (c == except) {
                continue;
            }
            if (counts[c] > mc) {
                lc = c;
            }
        }
        return lc;
    }

    protected int fillEmptyCentroids() {
        int move = 0;
        for (int c = 0; c < centroids.length; c++) {
            if (counts[c] == 0) {
                int lc = largestCentroid(c);
                int np = nearestPoint(lc, c);
                assignments[np] = c;
                counts[c]++;
                counts[lc]--;
                changes[c] = true;
                changes[lc] = true;
                move++;
            }
        }
        return move;
    }

    protected void moveCentroids() {
        for (int c = 0; c < centroids.length; c++) {
            if (!changes[c]) {
                continue;
            }
            double[] centroid = centroids[c];
            int n = 0;
            Arrays.fill(centroid, 0);
            for (int p = 0; p < points.length; p++) {
                if (assignments[p] != c) {
                    continue;
                }
                double[] point = points[p];
                n++;
                for (int d = 0; d < centroid.length; d++) {
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
