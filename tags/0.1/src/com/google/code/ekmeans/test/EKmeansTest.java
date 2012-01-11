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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Random;
import javax.swing.*;

public class EKmeansTest {

    private static final int RESOLUTION = 300;
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private JTextField nTextField;
    private JTextField kTextField;
    private JCheckBox equalCheckBox;
    private JTextField debugTextField;
    private JButton runButton;
    private JPanel canvaPanel;
    private JLabel statusBar;
    private Color[] colors;
    private EKmeans eKmeans;

    public EKmeansTest() {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(RESOLUTION + 100, RESOLUTION + 100));
        frame.setPreferredSize(new Dimension(RESOLUTION * 2, RESOLUTION * 2));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        frame.setContentPane(contentPanel);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        contentPanel.add(toolBar, BorderLayout.NORTH);

        JLabel nLabel = new JLabel("n:");
        toolBar.add(nLabel);

        nTextField = new JTextField("1000");
        toolBar.add(nTextField);

        JLabel kLabel = new JLabel("k:");
        toolBar.add(kLabel);

        kTextField = new JTextField("5");
        toolBar.add(kTextField);

        JLabel equalLabel = new JLabel("equal:");
        toolBar.add(equalLabel);

        equalCheckBox = new JCheckBox(" ");
        toolBar.add(equalCheckBox);

        JLabel debugLabel = new JLabel("debug:");
        toolBar.add(debugLabel);

        debugTextField = new JTextField("0");
        toolBar.add(debugTextField);

        runButton = new JButton();
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

    private void paint(Graphics g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        if (eKmeans == null) {
            return;
        }
        float widthRatio = (float) width / (float) RESOLUTION;
        float heightRatio = (float) height / (float) RESOLUTION;
        double[][] centroids = eKmeans.getCentroids();
        double[][] points = eKmeans.getPoints();
        int[] assignments = eKmeans.getAssignments();
        int[] counts = eKmeans.getCounts();
        boolean[] changes = eKmeans.getChanges();
        for (int i = 0; i < points.length; i++) {
            int assignment = assignments[i];
            if (assignment == -1) {
                continue;
            }
            int cx = (int) (widthRatio * centroids[assignment][0]);
            int cy = (int) (heightRatio * centroids[assignment][1]);
            int px = (int) (widthRatio * points[i][0]);
            int py = (int) (heightRatio * points[i][1]);
            g.setColor(colors[assignment]);
            g.drawRect(px - 2, py - 2, 4, 4);
            g.drawLine(cx, cy, px, py);
        }
        g.setColor(Color.RED);
        for (int i = 0; i < points.length; i++) {
            int assignment = assignments[i];
            if (assignment != -1) {
                continue;
            }
            int px = (int) (widthRatio * points[i][0]);
            int py = (int) (heightRatio * points[i][1]);
            g.drawRect(px - 2, py - 2, 4, 4);
        }
        for (int i = 0; i < centroids.length; i++) {
            int cx = (int) (widthRatio * centroids[i][0]);
            int cy = (int) (heightRatio * centroids[i][1]);
            g.setColor(Color.GREEN);
            g.drawLine(cx, cy - 2, cx, cy + 2);
            g.drawLine(cx - 2, cy, cx + 2, cy);
            int count = counts[i];
            if (changes[i]) {
                g.setColor(Color.RED);
            }
            g.drawString(String.valueOf(count), cx, cy);
        }
    }

    private void updateStatusBar(String status) {
        statusBar.setText(" " + status);
    }

    private static int nextInt(int n) {
        return Math.abs(RANDOM.nextInt() % n);
    }

    private void start() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                runButton.setEnabled(false);
                try {
                    EKmeansTest.this.run();
                } finally {
                    runButton.setEnabled(true);
                }
            }
        }).start();
    }

    private void run() {
        int n = Integer.parseInt(nTextField.getText());
        int k = Integer.parseInt(kTextField.getText());
        boolean equal = equalCheckBox.isSelected();
        final int debug = Integer.parseInt(debugTextField.getText());
        colors = new Color[k];
        for (int i = 0; i < k; i++) {
            int c = i * (225 / k);
            colors[i] = new Color(c, c, c);
            //colors[i] = new Color(nextInt(255), nextInt(255), nextInt(255));
        }
        double[][] centroids = new double[k][2];
        for (int i = 0; i < k; i++) {
            centroids[i][0] = nextInt(RESOLUTION);
            centroids[i][1] = nextInt(RESOLUTION);
        }
        double[][] points = new double[n][2];
        for (int i = 0; i < n; i++) {
            points[i][0] = nextInt(RESOLUTION);
            points[i][1] = nextInt(RESOLUTION);
        }
        eKmeans = new EKmeans(centroids, points);
        eKmeans.setEqual(equal);
        //eKmeans.setDistanceFunction(EKmeans.MANHATTAN_DISTANCE_FUNCTION);
        if (debug > 0) {
            eKmeans.setListener(new EKmeans.Listener() {

                @Override
                public void iteration(int iteration, int move) {
                    updateStatusBar(MessageFormat.format("iteration {0} move {1}", iteration, move));
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
        updateStatusBar(MessageFormat.format("EKmeans run in {0}ms", time));
        canvaPanel.repaint();
    }

    public static void main(String[] args) {
        new EKmeansTest();
    }
}
