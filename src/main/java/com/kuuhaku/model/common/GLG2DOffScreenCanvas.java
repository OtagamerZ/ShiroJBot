/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
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

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.kuuhaku.util.Graph;
import org.jogamp.glg2d.GLG2DCanvas;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class GLG2DOffScreenCanvas extends GLG2DCanvas {
	private Consumer<Graphics2D> renderAction = null;

	public GLG2DOffScreenCanvas(int width, int height) {
		super(new GLCapabilities(GLProfile.getGL2ES1(new EGLGraphicsDevice())));
		setSize(width, height);
	}

	public void render(Consumer<Graphics2D> act) {
		renderAction = act;
	}

	public BufferedImage extract() {
		if (renderAction == null) return new BufferedImage(0, 0, BufferedImage.TYPE_INT_ARGB);

		return Graph.extractImage(this);
	}
}
