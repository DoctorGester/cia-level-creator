package com.dglab.cia.levelcreator;

import delaunay.DelaunayTriangulator;
import delaunay.NotEnoughPointsException;
import delaunay.Triangle;
import delaunay.Vector2D;
import kn.uni.voronoitreemap.datastructure.OpenList;
import kn.uni.voronoitreemap.diagram.PowerDiagram;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: kartemov
 * Date: 23.05.2016
 * Time: 21:42
 */
public class CIA {
	private JFrame frame;
	private PolygonSimple clipPoly;
	private PowerDiagram diagram;
	private OpenList sites;
	private ZoomAndPanListener zoomAndPanListener;

	private Map<Site, List<Point2D>> rawPoints = new HashMap<>();
	private Map<Site, List<PolygonSimple>> polygons = new HashMap<>();

	public CIA() {
		createDiagram(2000, 2000);

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

	private void createDiagram(double width, double height) {
		clipPoly = new PolygonSimple();

		for (int i = 0; i < 360; i++) {
			double angle = Math.toRadians(i);
			clipPoly.add(width / 2 + Math.cos(angle) * width, height / 2 + Math.sin(angle) * height);
		}

		Rectangle bounds = clipPoly.getBounds();

		sites = new OpenList();

		for (int i = 0; i < 700; i++){
			sites.add(new Site(bounds.x + Math.random() * bounds.width, bounds.y + Math.random() * bounds.height, 1));
		}

		//for (int row = 0; row <)

		diagram = new PowerDiagram(sites, clipPoly);
		diagram.computeDiagram();

		findIntersections();
		findInternalPoints();
		findPolygons();
	}

	private void addPoint(Site site, Point2D point) {
		List<Point2D> points = rawPoints.get(site);

		if (points == null) {
			points = new ArrayList<>();
			rawPoints.put(site, points);

			for (Point2D polyPoint: site.getPolygon()) {
				points.add(polyPoint);
			}
		}

		points.add(point);
	}

	private void findIntersections() {
		Rectangle bounds = clipPoly.getBounds();

		for (double column = bounds.x; column < bounds.x + bounds.width; column += 64) {
			Point2D lineStart = new Point2D(column, bounds.y);
			Point2D lineEnd = new Point2D(column, bounds.y + bounds.height);

			findLineIntersections(lineStart, lineEnd);
		}

		for (double row = bounds.y; row < bounds.y + bounds.height; row += 64) {
			Point2D lineStart = new Point2D(bounds.x, row);
			Point2D lineEnd = new Point2D(bounds.x + bounds.width, row);

			findLineIntersections(lineStart, lineEnd);
		}
	}

	private void findLineIntersections(Point2D lineStart,Point2D lineEnd) {
		for (Site site : sites) {
			PolygonSimple polygon = site.getPolygon();

			if (polygon != null) {
				Point2D previous = null;

				for (Point2D point: polygon) {
					if (previous != null) {
						Point2D intersection = getLineIntersection(
								previous.x, previous.y,
								point.x, point.y,
								lineStart.x, lineStart.y,
								lineEnd.x, lineEnd.y
						);

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
		for (Map.Entry<Site, List<Point2D>> entry : rawPoints.entrySet()) {
			PolygonSimple polygon = entry.getKey().getPolygon();

			Rectangle bounds = clipPoly.getBounds();

			for (double column = bounds.x; column < bounds.x + bounds.width; column += 64) {
				for (double row = bounds.y; row < bounds.y + bounds.height; row += 64) {
					if (polygon.contains(column, row)) {
						entry.getValue().add(new Point2D(column, row));
					}
				}
			}
		}
	}

	private void findPolygons() {
		for (Map.Entry<Site, List<Point2D>> entry : rawPoints.entrySet()) {
			Vector<Vector2D> pointSet = new Vector<>();

			for (Point2D point2D : entry.getValue()) {
				pointSet.add(new Vector2D(point2D.x, point2D.y));
			}

			DelaunayTriangulator triangulator = null;

			try {
				triangulator = new DelaunayTriangulator(pointSet);
			} catch (NotEnoughPointsException e) {
				e.printStackTrace();
			}

			if (triangulator != null) {
				try {
					triangulator.compute();
				} catch (Exception e) {
					System.out.println(e);
					continue;
				}

				for (Triangle triangle : triangulator.getTriangleSet()) {
					Site site = entry.getKey();

					List<PolygonSimple> polygons = this.polygons.get(site);

					if (polygons == null) {
						polygons = new ArrayList<>();

						this.polygons.put(site, polygons);
					}

					PolygonSimple tri = new PolygonSimple();
					tri.add(new Point2D(triangle.a.x, triangle.a.y));
					tri.add(new Point2D(triangle.b.x, triangle.b.y));
					tri.add(new Point2D(triangle.c.x, triangle.c.y));

					polygons.add(tri);
				}
			}
			/*Point2D[] points = entry.getValue().toArray(new Point2D[entry.getValue().size()]);
			int N = points.length;

			for (int i = 0; i < N; i++) {
				for (int j = i+1; j < N; j++) {
					for (int k = j+1; k < N; k++) {
						boolean isTriangle = true;
						for (int a = 0; a < N; a++) {
							if (a == i || a == j || a == k) continue;

							if (pointInTriangle(points[a], points[i], points[j], points[k])) {
								isTriangle = false;
								break;
							}
						}

						if (isTriangle) {
							Site site = entry.getKey();

							List<PolygonSimple> polygons = this.polygons.get(site);

							if (polygons == null) {
								polygons = new ArrayList<>();

								this.polygons.put(site, polygons);
							}

							PolygonSimple tri = new PolygonSimple();
							tri.add(points[i]);
							tri.add(points[j]);
							tri.add(points[k]);

							polygons.add(tri);
						}
					}
				}
			}*/
		}
	}

	private class View extends JPanel {
		private void line(Graphics2D graphics2D, Point2D p1, Point2D p2) {
			graphics2D.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);

			Graphics2D graphics2D = (Graphics2D) g;

			graphics2D.setTransform(zoomAndPanListener.getCoordTransform());
			graphics2D.setColor(Color.BLACK);

			/*for (Site site : sites) {
				PolygonSimple polygon = site.getPolygon();

				if (polygon != null) {
					graphics2D.draw(polygon);
				}
			}*/

			Random random = new Random(1488);

			for (List<PolygonSimple> simples : polygons.values()) {
				final float hue = random.nextFloat();
				final float saturation = (random.nextInt(2000) + 1000) / 10000f;
				final float luminance = 0.9f;
				final Color color = Color.getHSBColor(hue, saturation, luminance);

				graphics2D.setColor(color);

				for (PolygonSimple simple : simples) {
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
			rawPoints.values().stream().flatMap(Collection::stream).forEach(point -> {
				graphics2D.fillOval((int) point.x - 2, (int) point.y - 2, 4, 4);
			});
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

	private static double sign (Point2D p1, Point2D p2, Point2D p3) {
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