package com.dglab.cia.levelcreator;

import kn.uni.voronoitreemap.convexHull.JConvexHull;
import kn.uni.voronoitreemap.convexHull.JFace;
import kn.uni.voronoitreemap.convexHull.JVertex;
import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * User: kartemov
 * Date: 23.05.2016
 * Time: 21:42
 */
public class CIA {
    private Random random = new Random(1488);
    private JFrame frame;
    private PolygonSimple clipPoly;
    private PowerDiagram diagram;
    private OpenList sites;
    private ZoomAndPanListener zoomAndPanListener;

    private Map<Site, List<Point2D>> rawPoints = new HashMap<>();
    private Map<Site, List<PolygonSimple>> polygons = new HashMap<>();

    public CIA() {
        createDiagram(2000, 2000, 700);
        //createDiagram(100, 100, 2);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension windowSize = new Dimension(1000, 1000);

        View view = new View();

        frame = new JFrame("CIA");
        frame.getContentPane().add(view);
        frame.setLocation((screenSize.width - windowSize.width) / 2, (screenSize.height - windowSize.height) / 2);
        frame.setSize(windowSize);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        zoomAndPanListener = new ZoomAndPanListener(view);
        view.addMouseListener(zoomAndPanListener);
        view.addMouseMotionListener(zoomAndPanListener);
        view.addMouseWheelListener(zoomAndPanListener);

        frame.repaint();
    }

    private void createDiagram(double width, double height, int numPieces) {
        clipPoly = new PolygonSimple();

        for (int i = 0; i < 360; i++) {
            double angle = Math.toRadians(i);
            clipPoly.add(width / 2 + Math.cos(angle) * width, height / 2 + Math.sin(angle) * height);
        }

        Rectangle bounds = clipPoly.getBounds();

        sites = new OpenList();

        for (int i = 0; i < numPieces; i++) {
            sites.add(new Site(bounds.x + random.nextDouble() * bounds.width, bounds.y + random.nextDouble() * bounds.height, 1));
        }

        diagram = new PowerDiagram(sites, clipPoly);
        diagram.computeDiagram();

        findIntersections();
        findInternalPoints();
        findPolygons();

		new OBJSerializer().save("C:\\Users\\Toaru Shoujo\\Desktop\\m2.obj", polygons);
    }

    private void addPoint(Site site, Point2D point) {
        List<Point2D> points = rawPoints.get(site);

        if (points == null) {
            points = new ArrayList<>();
            rawPoints.put(site, points);

            for (Point2D polyPoint : site.getPolygon()) {
                points.add(polyPoint);
            }
        }

        points.add(point);
    }

    private void findIntersections() {
        Rectangle bounds = clipPoly.getBounds();

        for (double column = bounds.x - 64; column < bounds.x + bounds.width; column += 64) {
            Point2D lineStart = new Point2D(column, bounds.y);
            Point2D lineEnd = new Point2D(column, bounds.y + bounds.height);

            findLineIntersections(lineStart, lineEnd);
        }

        for (double row = bounds.y - 64; row < bounds.y + bounds.height; row += 64) {
            Point2D lineStart = new Point2D(bounds.x, row);
            Point2D lineEnd = new Point2D(bounds.x + bounds.width, row);

            findLineIntersections(lineStart, lineEnd);
        }
    }

    private void findLineIntersections(Point2D lineStart, Point2D lineEnd) {
        for (Site site : sites) {
            PolygonSimple polygon = site.getPolygon();

            if (polygon != null) {
                Point2D previous = null;

                List<Point2D> polyPoints = new ArrayList<>();

                polygon.forEach(polyPoints::add);
                polyPoints.add(polygon.iterator().next());

                for (Point2D point : polyPoints) {
                    if (previous != null) {
                        Point2D intersection = getLineIntersection(
                                previous.x, previous.y,
                                point.x, point.y,
                                lineStart.x, lineStart.y,
                                lineEnd.x, lineEnd.y
                        );

                        //intersection = PolygonSimple.getIntersectionOfSegmentAndLine(previous, point, lineStart, lineEnd);

                        if (intersection != null) {
                            addPoint(site, intersection);
                        }
                    }

                    previous = point;
                }
            }
        }
    }

    private void findInternalPoints() {
        for (Site site : sites) {
            PolygonSimple polygon = site.getPolygon();
            Rectangle bounds = clipPoly.getBounds();

            for (double column = bounds.x; column < bounds.x + bounds.width; column += 64) {
                for (double row = bounds.y; row < bounds.y + bounds.height; row += 64) {
                    if (polygon != null) {
                        if (polygon.contains(column, row)) {
                            addPoint(site, new Point2D(column, row));
                        }
                    }
                }
            }
        }
    }

    private void findPolygons() {
        int rowSize = 64;
        Rectangle bounds = clipPoly.getBounds();

        for (double column = bounds.x; column < bounds.x + bounds.width; column += rowSize) {
            for (double row = bounds.y; row < bounds.y + bounds.height; row += rowSize) {
                Point2D topLeft = new Point2D(column - 1, row - 1);
                Point2D topRight = new Point2D(column + rowSize + 1, row - 1);
                Point2D botRight = new Point2D(column + rowSize + 1, row + rowSize + 1);
                Point2D botLeft = new Point2D(column - 1, row + rowSize + 1);

                Rectangle2D rectangle2D = new Rectangle.Double(column - 1, row -1, rowSize + 2, rowSize + 2);
                PolygonSimple polygonSimple = new PolygonSimple();
                polygonSimple.add(topLeft);
                polygonSimple.add(topRight);
                polygonSimple.add(botRight);
                polygonSimple.add(botLeft);

                for (Map.Entry<Site, List<Point2D>> entry : rawPoints.entrySet()) {
                    List<Point2D> hull = new ArrayList<>();

                    for (Point2D point2D : entry.getValue()) {
                        if (rectangle2D.contains(point2D.x, point2D.y)) {
                            hull.add(point2D);
                        }
                    }

                    if (hull.size() > 2) {
                        List<Point2D> hullResult = QuickHull.quickHull(hull);

                        PolygonSimple result = new PolygonSimple();
                        hullResult.forEach(result::add);

                        Site site = entry.getKey();

                        List<PolygonSimple> polygons = this.polygons.get(site);

                        if (polygons == null) {
                            polygons = new ArrayList<>();

                            this.polygons.put(site, polygons);
                        }

                        polygons.add(result);
                    }
                }
            }
        }
    }

    private class View extends JPanel {
        private void line(Graphics2D graphics2D, Point2D p1, Point2D p2) {
            graphics2D.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            Random random = new Random(1488);

            Graphics2D graphics2D = (Graphics2D) g;

            graphics2D.setTransform(zoomAndPanListener.getCoordTransform());

            for (List<PolygonSimple> simples : polygons.values()) {
                final float hue = random.nextFloat();
                final float saturation = (random.nextInt(2000) + 1000) / 10000f;
                final float luminance = 0.9f;
                final Color color = Color.getHSBColor(hue, saturation, luminance);
                Color darker = color.darker();

                for (PolygonSimple simple : simples) {
                    graphics2D.setColor(color);
                    graphics2D.fill(simple);
                    graphics2D.setColor(darker);
                    graphics2D.draw(simple);
                }
            }

			/*Random random = new Random();

			for (List<Point2D> points : rawPoints.values()) {
				final float hue = random.nextFloat();
				final float saturation = (random.nextInt(2000) + 1000) / 10000f;
				final float luminance = 0.9f;
				final Color color = Color.getHSBColor(hue, saturation, luminance);

				graphics2D.setColor(color);

				for (Point2D point : points) {
					graphics2D.fillOval((int) point.x - 2, (int) point.y - 2, 4, 4);
				}
			}*/
            /*graphics2D.setColor(Color.red);

            rawPoints.values().stream().flatMap(Collection::stream).forEach(point -> {
                graphics2D.fillOval((int) point.x - 1, (int) point.y - 1, 2, 2);
            });*/
        }
    }

    public static Point2D getLineIntersection(
            double p0_x, double p0_y, double p1_x, double p1_y,
            double p2_x, double p2_y, double p3_x, double p3_y
    ) {
        double s02_x, s02_y, s10_x, s10_y, s32_x, s32_y, s_numer, t_numer, denom, t;
        s10_x = p1_x - p0_x;
        s10_y = p1_y - p0_y;
        s32_x = p3_x - p2_x;
        s32_y = p3_y - p2_y;

        denom = s10_x * s32_y - s32_x * s10_y;
        if (denom == 0)
            return null; // Collinear
        boolean denomPositive = denom > 0;

        s02_x = p0_x - p2_x;
        s02_y = p0_y - p2_y;
        s_numer = s10_x * s02_y - s10_y * s02_x;
        if ((s_numer < 0) == denomPositive)
            return null; // No collision

        t_numer = s32_x * s02_y - s32_y * s02_x;
        if ((t_numer < 0) == denomPositive)
            return null; // No collision

        if (((s_numer > denom) == denomPositive) || ((t_numer > denom) == denomPositive))
            return null; // No collision
        // Collision detected
        t = t_numer / denom;

        return new Point2D(p0_x + (t * s10_x), p0_y + (t * s10_y));
    }


    public Point2D getLineIntersection2(
            double x1,double y1,double x2,double y2,
            double x3, double y3, double x4,double y4
    ) {
        double d = (x1-x2)*(y3-y4) - (y1-y2)*(x3-x4);
        if (d == 0) return null;

        double xi = ((x3-x4)*(x1*y2-y1*x2)-(x1-x2)*(x3*y4-y3*x4))/d;
        double yi = ((y3-y4)*(x1*y2-y1*x2)-(y1-y2)*(x3*y4-y3*x4))/d;

        Point2D p = new Point2D(xi,yi);
        if (xi < Math.min(x1,x2) || xi > Math.max(x1,x2)) return null;
        if (xi < Math.min(x3,x4) || xi > Math.max(x3,x4)) return null;
        return p;
    }


    private static double sign(Point2D p1, Point2D p2, Point2D p3) {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y);
    }

    private static boolean pointInTriangle(Point2D pt, Point2D v1, Point2D v2, Point2D v3) {
        PolygonSimple poly = new PolygonSimple();
        poly.add(v1);
        poly.add(v2);
        poly.add(v3);

        if (true)
            return poly.contains(pt);

        boolean b1, b2, b3;

        b1 = sign(pt, v1, v2) < 0.0f;
        b2 = sign(pt, v2, v3) < 0.0f;
        b3 = sign(pt, v3, v1) < 0.0f;

        return ((b1 == b2) && (b2 == b3));
    }

    public static void main(String[] args) {
        new CIA();
    }
}