package com.kuuhaku.model.common.dunhun;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AreaMap {
	private final List<Floor> floors = new ArrayList<>();

	public Floor getFloor(int index) {
		return floors.get(index);
	}

	public Node getNode(int floor, int index) {
		return floors.get(floor).getNodes().get(index);
	}

	public AreaMap addNode(int floor, Function<AreaMap, Node> nodeMaker) {
		while (floor >= floors.size()) {
			floors.add(new Floor());
		}

		Node node = nodeMaker.apply(this);
		node.setDepth(floor);

		floors.get(floor).getNodes().add(node);
		return this;
	}

	public BufferedImage render(int width, int height) {
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int floorCount = floors.size();
		int sliceHeight = height / floorCount;
		for (int i = 0; i < floorCount; i++) {
			int y = sliceHeight * i + sliceHeight / 2;

			Floor fl = floors.get(i);
			fl.prepare(width, y);
		}

		for (int i = floors.size() - 1; i >= 0; i--) {
			Floor fl = floors.get(i);
			for (Node node : fl.getNodes()) {
				if (node.getParents().isEmpty()) continue;

				Point from = node.getPos();
				for (Node par : node.getParents()) {
					Point to = par.getPos();
					int distance = node.getDepth() - par.getDepth();

					g2d.setColor(Color.GRAY);
					g2d.setStroke(new BasicStroke(8));
					g2d.drawPolyline(
							new int[]{from.x, from.x, to.x},
							new int[]{from.y, to.y + (from.y - to.y) / (distance + 1), to.y},
							3
					);
				}
			}

			for (Node node : fl.getNodes()) {
				g2d.drawImage(node.getIcon(),
						node.getPos().x - Node.NODE_RADIUS / 2, node.getPos().y - Node.NODE_RADIUS / 2,
						Node.NODE_RADIUS, Node.NODE_RADIUS,
						null
				);
			}
		}

		g2d.dispose();
		return bi;
	}
}
