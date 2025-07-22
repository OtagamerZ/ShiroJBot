package com.kuuhaku.model.common.dunhun;

import java.awt.*;
import java.util.function.Consumer;

public abstract class Graph {
	public static void applyTransformed(Graphics2D g2d, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, 0, new Point(), 1, 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, int x, int y, Consumer<Graphics2D> action) {
		applyTransformed(g2d, x, y, 0, new Point(), 1, 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, double ang, Point axis, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, ang, axis, 1, 1, action);
	}

	public static void applyTransformed(Graphics2D g2d, double scale, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, 0, new Point(), scale, scale, action);
	}

	public static void applyTransformed(Graphics2D g2d, double scaleX, double scaleY, Consumer<Graphics2D> action) {
		applyTransformed(g2d, 0, 0, 0, new Point(), scaleX, scaleY, action);
	}

	public static void applyTransformed(Graphics2D g, int x, int y, double ang, Point axis, double scaleX, double scaleY, Consumer<Graphics2D> action) {
		Graphics2D g2d = (Graphics2D) g.create();

		g2d.translate(x, y);
		g2d.rotate(Math.toRadians(ang), axis.x, axis.y);
		g2d.scale(scaleX, scaleY);
		action.accept(g2d);

		g2d.dispose();
	}
}
