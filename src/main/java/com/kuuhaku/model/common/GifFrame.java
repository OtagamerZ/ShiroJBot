/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2020  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.model.common;

import com.kuuhaku.utils.Helper;
import org.apache.commons.imaging.formats.gif.DisposalMethod;

import java.awt.*;
import java.awt.image.BufferedImage;

public class GifFrame {
	private final BufferedImage frame;
	private final DisposalMethod disposal;
	private final int width;
	private final int height;
	private final int offsetX;
	private final int offsetY;
	private final int delay;
	private BufferedImage adjustedFrame = null;

	public GifFrame(BufferedImage frame, DisposalMethod disposal, int width, int height, int offsetX, int offsetY, int delay) {
		this.frame = frame;
		this.disposal = disposal;
		this.width = width;
		this.height = height;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.delay = delay;
	}

	public BufferedImage getFrame() {
		return frame;
	}

	public BufferedImage getAdjustedFrame() {
		if (adjustedFrame != null) return adjustedFrame;

		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(frame, offsetX, offsetY, null);
		g2d.dispose();

		adjustedFrame = bi;
		return bi;
	}

	public BufferedImage rescaleFrame(int width, int height) {
		adjustedFrame = Helper.scaleAndCenterImage(getAdjustedFrame(), width, height);
		return adjustedFrame;
	}

	public void applyOverlay(BufferedImage overlay) {
		Graphics2D g2d = getAdjustedFrame().createGraphics();
		g2d.drawImage(overlay, 0, 0, null);
		g2d.dispose();
	}

	public DisposalMethod getDisposal() {
		return disposal;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public int getOffsetY() {
		return offsetY;
	}

	public int getDelay() {
		return delay;
	}
}
