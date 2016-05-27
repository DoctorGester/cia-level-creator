package com.dglab.cia.levelcreator;

import com.sun.javafx.geom.Vec3d;
import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author doc
 */
public class OBJSerializer {
	private static class Face {
		int vertNumber;
		int normalIndex;
		String material;

		public Face(int vertNumber, int normalIndex, String material) {
			this.vertNumber = vertNumber;
			this.normalIndex = normalIndex;
			this.material = material;
		}
	}

	private Vec3d writeSide(BufferedWriter writer, Point2D from, Point2D to) throws IOException {
		writer.write("v " + from.x + " 0 " + from.y); writer.newLine();
		writer.write("v " + to.x + " 0 " + to.y); writer.newLine();
		writer.write("v " + to.x + " -60 " + to.y); writer.newLine();
		writer.write("v " + from.x + " -60 " + from.y); writer.newLine();

		Vec3d normal = new Vec3d();
		normal.cross(new Vec3d(0, -1, 0), new Vec3d(to.x - from.x, 0, to.y - from.y));

		return normal;
	}

	private List<Point2D> reverseOrder(PolygonSimple polygonSimple) {
		List<Point2D> result = new ArrayList<>();
		for (Point2D point2D : polygonSimple) {
			result.add(point2D);
		}

		Collections.reverse(result);

		return result;
	}

	public void save(String path, Map<Site, List<PolygonSimple>> polygons) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
			int meshNum = 1;
			int current = 0;
			int currentNormal = 0;

			for (Map.Entry<Site, List<PolygonSimple>> entry : polygons.entrySet()) {
				writer.write("o MapPart." + meshNum++ + ".vmat");
				writer.newLine();

				List<Face> faces = new ArrayList<>();

				PolygonSimple mainPolygon = entry.getKey().getPolygon();

				for (PolygonSimple polygonSimple : entry.getValue()) {
					int amount = 0;

					for (Point2D point : polygonSimple) {
						writer.write("v " + point.x + " 0 " + point.y);
						writer.newLine();
						amount++;
					}

					faces.add(new Face(amount, 1, "materials/blends/mid_bottom_radiant001_rocky.vmat"));
				}

				for (Point2D point : mainPolygon) {
					writer.write("v " + point.x + " -60 " + point.y);
					writer.newLine();
				}

				faces.add(new Face(mainPolygon.getNumPoints(), 2, "materials/blends/mod_radiant_path_destruction_000.vmat"));

				Point2D previous = null;

				List<Vec3d> normals = new ArrayList<>();

				for (Point2D point : mainPolygon) {
					if (previous != null) {
						normals.add(writeSide(writer, previous, point));
						faces.add(new Face(4, currentNormal++, "materials/blends/mod_radiant_path_destruction_000.vmat"));
					}

					previous = point;
				}

				normals.add(writeSide(writer, previous, mainPolygon.iterator().next()));
				faces.add(new Face(4, currentNormal++, "materials/blends/mod_radiant_path_destruction_000.vmat"));

				writer.write("vn 0 1 0"); writer.newLine();
				writer.write("vn 0 -1 0"); writer.newLine();

				for (Vec3d normal : normals) {
					writer.write("vn " + normal.x + " " + normal.y + " " + normal.z); writer.newLine();
				}

				writer.newLine();

				writer.write("vt 0 0");
				writer.newLine();
				writer.newLine();

				for (Face face : faces) {
					int faceNumber = face.vertNumber;
					writer.write("usemtl " + face.material);
					writer.newLine();
					writer.write("f ");

					for (int i = current; i < current + faceNumber; i++) {
						writer.write((i + 1) + "/1/" + (face.normalIndex + 3));

						if (i != current + faceNumber - 1) {
							writer.write(" ");
						}
					}

					writer.newLine();

					current += faceNumber;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();;
		}

	}
}
