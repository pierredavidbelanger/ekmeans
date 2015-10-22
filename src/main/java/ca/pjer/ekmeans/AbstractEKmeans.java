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
package ca.pjer.ekmeans;

import java.util.Arrays;

public class AbstractEKmeans<Centroid, Point> {

    public interface Listener {

        void iteration(int iteration, int move);

    }

    public interface DistanceFunction<Centroid, Point> {

        void distance(boolean[] changed, double[][] distances, Centroid[] centroids, Point[] points);

    }

    public interface CenterFunction<Centroid, Point> {

        void center(boolean[] changed, int[] assignments, Centroid[] centroids, Point[] points);

    }

    protected final Centroid[] centroids;
    protected final Point[] points;
    protected final boolean equal;
    protected final DistanceFunction<Centroid, Point> distanceFunction;
    protected final CenterFunction<Centroid, Point> centerFunction;
    protected final Listener listener;

    protected final int idealCount;
    protected final double[][] distances;
    protected final int[] assignments;
    protected final boolean[] changed;
    protected final int[] counts;
    protected final boolean[] done;

    public AbstractEKmeans(Centroid[] centroids, Point[] points, boolean equal, DistanceFunction<Centroid, Point> distanceFunction, CenterFunction<Centroid, Point> centerFunction, Listener listener) {
        this.centroids = centroids;
        this.points = points;
        this.distanceFunction = distanceFunction;
        this.centerFunction = centerFunction;
        if (centroids.length > 0) {
            idealCount = points.length / centroids.length;
        } else {
            idealCount = 0;
        }
        distances = new double[centroids.length][points.length];
        assignments = new int[points.length];
        Arrays.fill(assignments, -1);
        changed = new boolean[centroids.length];
        Arrays.fill(changed, true);
        counts = new int[centroids.length];
        done = new boolean[centroids.length];
        this.equal = equal;
        this.listener = listener;
    }

    public int[] run() {
        return run(128);
    }

    public int[] run(int iteration) {
        calculateDistances();
        int move = makeAssignments();
        int i = 0;
        while (move > 0 && i++ < iteration) {
            if (points.length >= centroids.length) {
                move = fillEmptyCentroids();
            }
            moveCentroids();
            calculateDistances();
            move += makeAssignments();
            if (listener != null) {
                listener.iteration(i, move);
            }
        }
        return assignments;
    }

    protected void calculateDistances() {
        distanceFunction.distance(changed, distances, centroids, points);
        Arrays.fill(changed, false);
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
                    changed[assignments[p]] = true;
                }
                changed[nc] = true;
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
        double md = Double.POSITIVE_INFINITY;
        int nc = -1;
        int np = -1;
        for (int p = 0; p < points.length; p++) {
            if (assignments[p] != cc) {
                continue;
            }
            for (int c = 0; c < centroids.length; c++) {
                if (c == cc || done[c]) {
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
                    changed[assignments[np]] = true;
                }
                changed[nc] = true;
                assignments[np] = nc;
                move++;
            }
            counts[cc]--;
            counts[nc]++;
            if (counts[nc] > idealCount) {
                done[cc] = true;
                move += remakeAssignments(nc);
                done[cc] = false;
            }
        }
        return move;
    }

    protected int nearestCentroid(int p) {
        double md = Double.POSITIVE_INFINITY;
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
        double md = Double.POSITIVE_INFINITY;
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
                changed[c] = true;
                changed[lc] = true;
                move++;
            }
        }
        return move;
    }

    protected void moveCentroids() {
        centerFunction.center(changed, assignments, centroids, points);
    }
}
