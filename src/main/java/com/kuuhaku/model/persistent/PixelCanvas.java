/*
 * This file is part of Shiro J Bot.
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

package com.kuuhaku.model.persistent;

import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

@SuppressWarnings("rawtypes")
@Entity
public class PixelCanvas {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "TEXT")
	private String canvas;

	@Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean shelved = false;

	private BufferedImage getCanvas() {
		if (canvas != null) {
			try {
				byte[] bytes = Base64.getDecoder().decode(canvas);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				return ImageIO.read(bais);
			} catch (IOException e) {
				Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			}
		}
		BufferedImage bi = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.decode("#333333"));
		g2d.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
		g2d.dispose();

		return bi;
	}

	public RestAction viewCanvas(TextChannel channel) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(getCanvas(), "png", baos);

			return channel.sendFile(baos.toByteArray(), "canvas.png");
		} catch (IOException | IllegalArgumentException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o canvas, estamos resolvendo isso.");
	}

	public RestAction viewSection(TextChannel channel, int number) {
		int[] section;
		switch (number) {
			case 1:
				section = new int[]{0, 0};
				break;
			case 2:
				section = new int[]{512, 0};
				break;
			case 3:
				section = new int[]{0, 512};
				break;
			case 4:
				section = new int[]{512, 512};
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + number);
		}
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			BufferedImage bi = new BufferedImage(CANVAS_SIZE / 2, CANVAS_SIZE / 2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();

			g2d.drawImage(getCanvas().getSubimage(section[0], section[1], 512, 512), 0, 0, null);
			g2d.dispose();

			ImageIO.write(bi, "png", baos);

			return channel.sendFile(baos.toByteArray(), "chunk_" + number + ".png");
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o canvas, estamos resolvendo isso.");
	}

	public RestAction viewChunk(TextChannel channel, int[] coords, int zoom, boolean section) {
		int fac = (int) Math.pow(2, zoom);
		int chunkSize = CANVAS_SIZE / fac;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			BufferedImage chunk = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
			BufferedImage canvas = new BufferedImage(CANVAS_SIZE + chunkSize, CANVAS_SIZE + chunkSize, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = canvas.createGraphics();

			g2d.drawImage(getCanvas(), (canvas.getWidth() / 2) - CANVAS_SIZE / 2, (canvas.getHeight() / 2) - CANVAS_SIZE / 2, null);

			g2d = chunk.createGraphics();
			int x = (CANVAS_SIZE / 2 / fac) + (coords[0] + CANVAS_SIZE / (section ? 4 : 2)) - (chunkSize / 2);
			int y = (CANVAS_SIZE / 2 / fac) + (CANVAS_SIZE / (section ? 4 : 2) - coords[1]) - (chunkSize / 2);
			g2d.drawImage(canvas.getSubimage(x, y, chunkSize, chunkSize).getScaledInstance(CANVAS_SIZE, CANVAS_SIZE, 0), 0, 0, null);

			ImageIO.write(chunk, "png", baos);

			return channel.sendFile(baos.toByteArray(), "chunk.png");
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o chunk, estamos resolvendo isso.");
	}

	public RestAction addPixel(TextChannel channel, int[] coords, Color color) {
		BufferedImage canvas = getCanvas();
		canvas.setRGB(coords[0] + CANVAS_SIZE / 2, CANVAS_SIZE / 2 - coords[1], color.getRGB());
		saveCanvas(canvas);

		return viewChunk(channel, coords, 3, false);
	}

	private void saveCanvas(BufferedImage canvas) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(canvas, "png", baos);
			this.canvas = Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
