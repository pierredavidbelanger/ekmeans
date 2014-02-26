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
package com.google.code.ekmeans.test;

import com.google.code.ekmeans.EKmeans;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import javax.swing.*;

public class EKmeansTest {

    private static final int RESOLUTION = 300;
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private JToolBar toolBar;
    private JTextField nTextField;
    private JTextField kTextField;
    private JCheckBox equalCheckBox;
    private JTextField debugTextField;
    private JPanel canvaPanel;
    private JLabel statusBar;
    private double[][] points = null;
    private double[][] minmaxs = null;
    private EKmeans eKmeans = null;
    private String[] lines = null;

    public EKmeansTest() {
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
            @Override
            public void actionPerformed(ActionEvent ae) {
                csvImport();
            }
        });
        toolBar.add(csvImportButton);

        JButton csvExportButton = new JButton();
        csvExportButton.setAction(new AbstractAction(" Export CSV ") {
            @Override
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
            @Override
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
            @Override
            public void actionPerformed(ActionEvent ae) {
                start();
            }
        });
        toolBar.add(runButton);

        canvaPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                EKmeansTest.this.paint(g, getWidth(), getHeight());
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
            minmaxs = new double[][]{
                {Double.MAX_VALUE, Double.MAX_VALUE},
                {Double.MIN_VALUE, Double.MIN_VALUE}
            };
            java.util.List points = new ArrayList();
            java.util.List lines = new ArrayList();
            BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                String[] pointString = line.split(",");
                double[] point = new double[2];
                point[0] = Double.parseDouble(pointString[0].trim());
                point[1] = Double.parseDouble(pointString[1].trim());
                points.add(point);
                if (point[0] < minmaxs[0][0]) {
                    minmaxs[0][0] = point[0];
                }
                if (point[1] < minmaxs[0][1]) {
                    minmaxs[0][1] = point[1];
                }
                if (point[0] > minmaxs[1][0]) {
                    minmaxs[1][0] = point[0];
                }
                if (point[1] > minmaxs[1][1]) {
                    minmaxs[1][1] = point[1];
                }
            }
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
            double[][] points = eKmeans.getPoints();
            int[] assignments = eKmeans.getAssignments();
            if (lines != null) {
                for (int i = 0; i < points.length; i++) {
                    writer.printf(Locale.ENGLISH, "%d,%s%n", assignments[i], lines[i]);
                }
            } else {
                for (int i = 0; i < points.length; i++) {
                    writer.printf(Locale.ENGLISH, "%d,%f,%f%n", assignments[i], points[i][0], points[i][1]);
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
        minmaxs = new double[][]{
            {Double.MAX_VALUE, Double.MAX_VALUE},
            {Double.MIN_VALUE, Double.MIN_VALUE}
        };
        for (int i = 0; i < n; i++) {
            points[i][0] = RANDOM.nextDouble();
            points[i][1] = RANDOM.nextDouble();
            if (points[i][0] < minmaxs[0][0]) {
                minmaxs[0][0] = points[i][0];
            }
            if (points[i][1] < minmaxs[0][1]) {
                minmaxs[0][1] = points[i][1];
            }
            if (points[i][0] > minmaxs[1][0]) {
                minmaxs[1][0] = points[i][0];
            }
            if (points[i][1] > minmaxs[1][1]) {
                minmaxs[1][1] = points[i][1];
            }
        }
        canvaPanel.repaint();
        enableToolBar(true);
    }

    private void start() {
        if (points == null) {
            random();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                enableToolBar(false);
                try {
                    EKmeansTest.this.run();
                } finally {
                    enableToolBar(true);
                }
            }
        }).start();
    }

    private void run() {
        int k = Integer.parseInt(kTextField.getText());
        boolean equal = equalCheckBox.isSelected();
        final int debug = Integer.parseInt(debugTextField.getText());
        double[][] centroids = new double[k][2];
        for (int i = 0; i < k; i++) {
            centroids[i][0] = minmaxs[0][0] + ((minmaxs[1][0] - minmaxs[0][0]) * RANDOM.nextDouble());
            centroids[i][1] = minmaxs[0][1] + ((minmaxs[1][1] - minmaxs[0][1]) * RANDOM.nextDouble());
        }
        eKmeans = new EKmeans(centroids, points);
        eKmeans.setEqual(equal);
        if (debug > 0) {
            eKmeans.setListener(new EKmeans.Listener() {
                @Override
                public void iteration(int iteration, int move) {
                    statusBar.setText(MessageFormat.format("iteration {0} move {1}", iteration, move));
                    canvaPanel.repaint();
                    try {
                        Thread.sleep(debug);
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                }
            });
        }
        long time = System.currentTimeMillis();
        eKmeans.run();
        time = System.currentTimeMillis() - time;
        statusBar.setText(MessageFormat.format("EKmeans run in {0}ms", time));
        canvaPanel.repaint();
    }

    private void paint(Graphics g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        if (minmaxs == null) {
            return;
        }
        double widthRatio = (double) (width - 6) / (minmaxs[1][0] - minmaxs[0][0]);
        double heightRatio = (double) (height - 6) / (minmaxs[1][1] - minmaxs[0][1]);
        if (points == null) {
            return;
        }
        g.setColor(Color.BLACK);
        for (int i = 0; i < points.length; i++) {
            int px = 3 + (int) (widthRatio * (points[i][0] - minmaxs[0][0]));
            int py = 3 + (int) (heightRatio * (points[i][1] - minmaxs[0][1]));
            g.drawRect(px - 2, py - 2, 4, 4);
        }
        if (eKmeans == null) {
            return;
        }
        double[][] centroids = eKmeans.getCentroids();
        int[] assignments = eKmeans.getAssignments();
        int[] counts = eKmeans.getCounts();
        int s = 225 / centroids.length;
        for (int i = 0; i < points.length; i++) {
            int assignment = assignments[i];
            if (assignment == -1) {
                continue;
            }
            int cx = 3 + (int) (widthRatio * (centroids[assignment][0] - minmaxs[0][0]));
            int cy = 3 + (int) (heightRatio * (centroids[assignment][1] - minmaxs[0][1]));
            int px = 3 + (int) (widthRatio * (points[i][0] - minmaxs[0][0]));
            int py = 3 + (int) (heightRatio * (points[i][1] - minmaxs[0][1]));
            int c = assignment * s;
            g.setColor(new Color(c, c, c));
            g.drawLine(cx, cy, px, py);
        }
        g.setColor(Color.GREEN);
        for (int i = 0; i < centroids.length; i++) {
            int cx = 3 + (int) (widthRatio * (centroids[i][0] - minmaxs[0][0]));
            int cy = 3 + (int) (heightRatio * (centroids[i][1] - minmaxs[0][1]));
            g.drawLine(cx, cy - 2, cx, cy + 2);
            g.drawLine(cx - 2, cy, cx + 2, cy);
            int count = counts[i];
            g.drawString(String.valueOf(count), cx, cy);
        }
    }

    public static void main(String[] args) {
        new EKmeansTest();
    }
}
