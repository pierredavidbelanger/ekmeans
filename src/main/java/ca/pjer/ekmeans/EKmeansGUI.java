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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class EKmeansGUI {

    private class DoubleEKmeansExt extends DoubleEKmeans {

        public DoubleEKmeansExt(double[][] centroids, double[][] points, boolean equal, DoubleDistanceFunction doubleDistanceFunction, Listener listener) {
            super(centroids, points, equal, doubleDistanceFunction, listener);
        }

        public int[] getAssignments() {
            return assignments;
        }

        public int[] getCounts() {
            return counts;
        }
    }

    private static final int MIN = 0;
    private static final int MAX = 1;
    private static final int LEN = 2;

    private static final int X = 0;
    private static final int Y = 1;

    private static final int RESOLUTION = 300;
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private JToolBar toolBar;
    private JTextField nTextField;
    private JTextField kTextField;
    private JCheckBox equalCheckBox;
    private JTextField debugTextField;
    private JPanel canvaPanel;
    private JLabel statusBar;
    private double[][] centroids = null;
    private double[][] points = null;
    private double[][] minmaxlens = null;
    private DoubleEKmeansExt eKmeans = null;
    private String[] lines = null;

    public EKmeansGUI() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(RESOLUTION + 100, RESOLUTION + 100));
        frame.setPreferredSize(new Dimension(RESOLUTION * 2, RESOLUTION * 2));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        frame.setContentPane(contentPanel);

        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        contentPanel.add(toolBar, BorderLayout.NORTH);

        JButton csvImportButton = new JButton();
        csvImportButton.setAction(new AbstractAction(" Import CSV ") {
            public void actionPerformed(ActionEvent ae) {
                csvImport();
            }
        });
        toolBar.add(csvImportButton);

        JButton csvExportButton = new JButton();
        csvExportButton.setAction(new AbstractAction(" Export CSV ") {
            public void actionPerformed(ActionEvent ae) {
                csvExport();
            }
        });
        toolBar.add(csvExportButton);

        JLabel nLabel = new JLabel("n:");
        toolBar.add(nLabel);

        nTextField = new JTextField("1000");
        toolBar.add(nTextField);

        JButton randomButton = new JButton();
        randomButton.setAction(new AbstractAction(" Random ") {
            public void actionPerformed(ActionEvent ae) {
                random();
            }
        });
        toolBar.add(randomButton);

        JLabel kLabel = new JLabel("k:");
        toolBar.add(kLabel);

        kTextField = new JTextField("5");
        toolBar.add(kTextField);

        JLabel equalLabel = new JLabel("equal:");
        toolBar.add(equalLabel);

        equalCheckBox = new JCheckBox("");
        toolBar.add(equalCheckBox);

        JLabel debugLabel = new JLabel("debug:");
        toolBar.add(debugLabel);

        debugTextField = new JTextField("0");
        toolBar.add(debugTextField);

        JButton runButton = new JButton();
        runButton.setAction(new AbstractAction(" Start ") {
            public void actionPerformed(ActionEvent ae) {
                start();
            }
        });
        toolBar.add(runButton);

        canvaPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                EKmeansGUI.this.paint(g, getWidth(), getHeight());
            }
        };
        contentPanel.add(canvaPanel, BorderLayout.CENTER);

        statusBar = new JLabel(" ");
        contentPanel.add(statusBar, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);
    }

    private void enableToolBar(boolean enabled) {
        for (Component c : toolBar.getComponents()) {
            c.setEnabled(enabled);
        }
    }

    private void csvImport() {
        enableToolBar(false);
        eKmeans = null;
        lines = null;
        try {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showOpenDialog(toolBar);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            minmaxlens = new double[][]{
                {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
                {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
                {0d, 0d}
            };
            java.util.List points = new ArrayList();
            java.util.List lines = new ArrayList();
            BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                String[] pointString = line.split(",");
                double[] point = new double[2];
                point[X] = Double.parseDouble(pointString[X].trim());
                point[Y] = Double.parseDouble(pointString[Y].trim());
                System.out.println(point[X] + ", " + point[Y]);
                points.add(point);
                if (point[X] < minmaxlens[MIN][X]) {
                    minmaxlens[MIN][X] = point[X];
                }
                if (point[Y] < minmaxlens[MIN][Y]) {
                    minmaxlens[MIN][Y] = point[Y];
                }
                if (point[X] > minmaxlens[MAX][X]) {
                    minmaxlens[MAX][X] = point[X];
                }
                if (point[Y] > minmaxlens[MAX][Y]) {
                    minmaxlens[MAX][Y] = point[Y];
                }
            }
            minmaxlens[LEN][X] = minmaxlens[MAX][X] - minmaxlens[MIN][X];
            minmaxlens[LEN][Y] = minmaxlens[MAX][Y] - minmaxlens[MIN][Y];
            reader.close();
            this.points = (double[][]) points.toArray(new double[points.size()][]);
            nTextField.setText(String.valueOf(this.points.length));
            this.lines = (String[]) lines.toArray(new String[lines.size()]);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            canvaPanel.repaint();
            enableToolBar(true);
        }
    }

    private void csvExport() {
        if (eKmeans == null) {
            return;
        }
        enableToolBar(false);
        try {
            JFileChooser chooser = new JFileChooser();
            int returnVal = chooser.showSaveDialog(toolBar);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
            }
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(chooser.getSelectedFile())));
            double[][] points = this.points;
            int[] assignments = eKmeans.getAssignments();
            if (lines != null) {
                for (int i = 0; i < points.length; i++) {
                    writer.printf(Locale.ENGLISH, "%d,%s%n", assignments[i], lines[i]);
                }
            } else {
                for (int i = 0; i < points.length; i++) {
                    writer.printf(Locale.ENGLISH, "%d,%f,%f%n", assignments[i], points[i][X], points[i][Y]);
                }
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            canvaPanel.repaint();
            enableToolBar(true);
        }
    }

    private void random() {
        enableToolBar(false);
        eKmeans = null;
        lines = null;
        int n = Integer.parseInt(nTextField.getText());
        points = new double[n][2];
        minmaxlens = new double[][]{
            {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY},
            {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY},
            {0d, 0d}
        };
        for (int i = 0; i < n; i++) {
            points[i][X] = RANDOM.nextDouble();
            points[i][Y] = RANDOM.nextDouble();
            if (points[i][X] < minmaxlens[MIN][X]) {
                minmaxlens[MIN][X] = points[i][X];
            }
            if (points[i][Y] < minmaxlens[MIN][Y]) {
                minmaxlens[MIN][Y] = points[i][Y];
            }
            if (points[i][X] > minmaxlens[MAX][X]) {
                minmaxlens[MAX][X] = points[i][X];
            }
            if (points[i][Y] > minmaxlens[MAX][Y]) {
                minmaxlens[MAX][Y] = points[i][Y];
            }
        }
        minmaxlens[LEN][X] = minmaxlens[MAX][X] - minmaxlens[MIN][X];
        minmaxlens[LEN][Y] = minmaxlens[MAX][Y] - minmaxlens[MIN][Y];
        canvaPanel.repaint();
        enableToolBar(true);
    }

    private void start() {
        if (points == null) {
            random();
        }
        new Thread(new Runnable() {
            public void run() {
                enableToolBar(false);
                try {
                    EKmeansGUI.this.run();
                } finally {
                    enableToolBar(true);
                }
            }
        }).start();
    }

    private void run() {
        try {
            URL url = new URL("http://staticmap.openstreetmap.de/staticmap.php?center=" + (minmaxlens[MIN][X] + (minmaxlens[LEN][X] / 2d)) + "," + (minmaxlens[MIN][Y] + (minmaxlens[LEN][Y] / 2d)) + "&zoom=14&size=" +  canvaPanel.getWidth() + "x" + canvaPanel.getHeight() + "&maptype=mapnik");
            System.out.println("url:" + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        int k = Integer.parseInt(kTextField.getText());
        boolean equal = equalCheckBox.isSelected();
        int debugTmp = 0;
        try {
            debugTmp = Integer.parseInt(debugTextField.getText());
        } catch (NumberFormatException ignore) {
        }
        final int debug = debugTmp;
        centroids = new double[k][2];
        for (int i = 0; i < k; i++) {
//            centroids[i][X] = minmaxlens[MIN][X] + (minmaxlens[LEN][X] * RANDOM.nextDouble());
//            centroids[i][Y] = minmaxlens[MIN][Y] + (minmaxlens[LEN][Y] * RANDOM.nextDouble());
            centroids[i][X] = minmaxlens[MIN][X] + (minmaxlens[LEN][X] / 2d);
            centroids[i][Y] = minmaxlens[MIN][Y] + (minmaxlens[LEN][Y] / 2d);
        }
        AbstractEKmeans.Listener listener = null;
        if (debug > 0) {
            listener = new AbstractEKmeans.Listener() {
                public void iteration(int iteration, int move) {
                    statusBar.setText(MessageFormat.format("iteration {0} move {1}", iteration, move));
                    canvaPanel.repaint();
                    try {
                        Thread.sleep(debug);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        eKmeans = new DoubleEKmeansExt(centroids, points, equal, DoubleEKmeans.EUCLIDEAN_DISTANCE_FUNCTION, listener);
        long time = System.currentTimeMillis();
        eKmeans.run();
        time = System.currentTimeMillis() - time;
        statusBar.setText(MessageFormat.format("EKmeans run in {0}ms", time));
        canvaPanel.repaint();
    }

    private void paint(Graphics g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        if (minmaxlens == null) {
            return;
        }
        double widthRatio = (width - 6d) / minmaxlens[LEN][X];
        double heightRatio = (height - 6d) / minmaxlens[LEN][Y];
        if (points == null) {
            return;
        }
        g.setColor(Color.BLACK);
        for (int i = 0; i < points.length; i++) {
            int px = 3 + (int) (widthRatio * (points[i][X] - minmaxlens[MIN][X]));
            int py = 3 + (int) (heightRatio * (points[i][Y] - minmaxlens[MIN][Y]));
            g.drawRect(px - 2, py - 2, 4, 4);
        }
        if (eKmeans == null) {
            return;
        }
        int[] assignments = eKmeans.getAssignments();
        int[] counts = eKmeans.getCounts();
        int s = 225 / centroids.length;
        for (int i = 0; i < points.length; i++) {
            int assignment = assignments[i];
            if (assignment == -1) {
                continue;
            }
            int cx = 3 + (int) (widthRatio * (centroids[assignment][X] - minmaxlens[MIN][X]));
            int cy = 3 + (int) (heightRatio * (centroids[assignment][Y] - minmaxlens[MIN][Y]));
            int px = 3 + (int) (widthRatio * (points[i][X] - minmaxlens[MIN][X]));
            int py = 3 + (int) (heightRatio * (points[i][Y] - minmaxlens[MIN][Y]));
            int c = assignment * s;
            g.setColor(new Color(c, c, c));
            g.drawLine(cx, cy, px, py);
        }
        g.setColor(Color.GREEN);
        for (int i = 0; i < centroids.length; i++) {
            int cx = 3 + (int) (widthRatio * (centroids[i][X] - minmaxlens[MIN][X]));
            int cy = 3 + (int) (heightRatio * (centroids[i][Y] - minmaxlens[MIN][Y]));
            g.drawLine(cx, cy - 2, cx, cy + 2);
            g.drawLine(cx - 2, cy, cx + 2, cy);
            int count = counts[i];
            g.drawString(String.valueOf(count), cx, cy);
        }
    }

    public static void main(String[] args) {
        new EKmeansGUI();
    }
}
