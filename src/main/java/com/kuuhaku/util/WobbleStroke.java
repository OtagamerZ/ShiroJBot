package com.kuuhaku.util;/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Random;

public class WobbleStroke implements Stroke {
	private static final float FLATNESS = 1;

	private final Random rng;
	private final int width;

	public WobbleStroke(Random rng, int width) {
		this.rng = rng;
		this.width = width;
	}

	@Override
	public Shape createStrokedShape(Shape shape) {
		GeneralPath result = new GeneralPath();
		shape = new BasicStroke(width).createStrokedShape(shape);
		PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), FLATNESS);
		double moveX = 0, moveY = 0;
		double lastX = 0, lastY = 0;
		double thisX, thisY;
		int type;
		double next = 0;

		double[] points = new double[6];
		while (!it.isDone()) {
			type = it.currentSegment(points);
			switch (type) {
				case PathIterator.SEG_MOVETO:
					moveX = lastX = randomize(rng, points[0]);
					moveY = lastY = randomize(rng, points[1]);
					result.moveTo(moveX, moveY);
					next = 0;
					break;

				case PathIterator.SEG_CLOSE:
					points[0] = moveX;
					points[1] = moveY;

				case PathIterator.SEG_LINETO:
					thisX = randomize(rng, points[0]);
					thisY = randomize(rng, points[1]);
					double dx = thisX - lastX;
					double dy = thisY - lastY;
					double distance = Math.sqrt(dx * dx + dy * dy);
					if (distance >= next) {
						double r = 1.0 / distance;
						while (distance >= next) {
							double x = lastX + next * dx * r;
							double y = lastY + next * dy * r;
							result.lineTo(randomize(rng, x), randomize(rng, y));
							next++;
						}
					}
					next -= distance;
					lastX = thisX;
					lastY = thisY;
					break;
			}

			it.next();
		}

		return result;
	}

	private double randomize(Random rng, double fac) {
		return fac + rng.nextDouble() * 2 - 1;
	}
}