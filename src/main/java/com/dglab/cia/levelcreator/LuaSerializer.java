package com.dglab.cia.levelcreator;

import kn.uni.voronoitreemap.j2d.Point2D;
import kn.uni.voronoitreemap.j2d.PolygonSimple;
import kn.uni.voronoitreemap.j2d.Site;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * @author doc
 */
public class LuaSerializer {
	public void save(String path, Collection<Site> sites, Rectangle bounds) {
		double offsetX = 1000;//bounds.x - bounds.width / 2;
		double offsetY = 1000;//bounds.y - bounds.height / 2;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
			writer.write("-- Generated on "
					+ DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
			writer.newLine();
			writer.write("local polys = {}"); writer.newLine();
			writer.write("local poly"); writer.newLine();

			for (Site site : sites) {
				PolygonSimple polygon = site.getPolygon();

				writer.write("poly = Polygon()"); writer.newLine();
				writer.write(
						"poly:setOrigin("
						+ (polygon.getCentroid().x - offsetX)
						+ ", "
						+ (polygon.getCentroid().y - offsetY)
						+ ")"
				);
				writer.newLine();

				for (Point2D point : polygon) {
					double x = point.x - offsetX;
					double y = point.y - offsetY;
					writer.write("poly:addPoint(" + x + ", " + y + ")");
					writer.newLine();
				}

				writer.write("table.insert(polys, poly)"); writer.newLine();
			}

			writer.write("return polys");
		} catch (IOException e) {
			e.printStackTrace();;
		}
	}
}
