package com.dglab.cia.levelcreator;

import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author doc
 */
public class OBJSerializer {
	public void save(String path, Map<Site, List<PolygonSimple>> polygons) {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
			int meshNum = 1;
			int current = 0;

			for (List<PolygonSimple> polygonSimples : polygons.values()) {
				writer.write("o MapPart." + meshNum++);
				writer.newLine();

				List<Integer> faces = new ArrayList<>();

				for (PolygonSimple polygonSimple : polygonSimples) {
					int amount = 0;

					for (Point2D point : polygonSimple) {
						writer.write("v " + point.x + " 0 " + point.y);
						writer.newLine();
						amount++;
					}

					faces.add(amount);
				}

				writer.write("vn 0 1 0");
				writer.newLine();
				writer.newLine();

				writer.write("vt 0 0");
				writer.newLine();
				writer.newLine();

				for (Integer faceNumber : faces) {
					writer.write("f ");

					for (int i = current; i < current + faceNumber; i++) {
						writer.write(String.valueOf(i + 1) + "/1/1");

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
