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

package com.kuuhaku.model.persistent;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CanvasDAO;
import com.kuuhaku.model.enums.I18n;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import org.hibernate.annotations.DynamicUpdate;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.persistence.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

@Entity
@DynamicUpdate
@Table(name = "pixelcanvas")
public class PixelCanvas {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@Column(columnDefinition = "TEXT")
	private String canvas;

	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean shelved = false;

	public BufferedImage getCanvas() {
		if (canvas != null) {
			byte[] bytes = Base64.getDecoder().decode(canvas);
			try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
				BufferedImage canvas = ImageIO.read(bais);

				if (canvas.getWidth() != CANVAS_SIZE || canvas.getHeight() != CANVAS_SIZE) {
					BufferedImage bi = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d = bi.createGraphics();
					g2d.setColor(Color.decode("#333333"));
					g2d.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
					g2d.drawImage(canvas, 0, 0, null);
					g2d.dispose();

					return bi;
				}

				return canvas;
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

	public String getRawCanvas() {
		byte[] bytes = Base64.getDecoder().decode(canvas);
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			BufferedImage img = ImageIO.read(bais);

			if (img.getWidth() != CANVAS_SIZE || img.getHeight() != CANVAS_SIZE) {
				BufferedImage bi = getCanvas();
				saveCanvas(bi);
			}

			return canvas;
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
			return canvas;
		}
	}

	public String getAsCoordinates() {
		JSONArray ja = new JSONArray();

		BufferedImage bi = getCanvas();

		for (int y = 0; y < bi.getHeight(); y++) {
			for (int x = 0; x < bi.getWidth(); x++) {
				JSONObject jo = new JSONObject();

				jo.put("id", (1024 * y) + x);
				jo.put("x", x);
				jo.put("y", y);
				jo.put("color", String.format("#%06X", bi.getRGB(x, y) & 0xFFFFFF));

				ja.put(jo);
			}
		}

		return ja.toString();
	}

	public RestAction<?> viewCanvas(TextChannel channel) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(getCanvas(), "png", baos);

			return channel.sendFile(baos.toByteArray(), "canvas.png");
		} catch (IOException | IllegalArgumentException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas"));
	}

	public RestAction<?> viewSection(TextChannel channel, int number) {
		int[] section = switch (number) {
			case 1 -> new int[]{0, 0};
			case 2 -> new int[]{512, 0};
			case 3 -> new int[]{0, 512};
			case 4 -> new int[]{512, 512};
			default -> throw new IllegalStateException("Unexpected value: " + number);
		};
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
		return channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas"));
	}

	public RestAction<?> viewChunk(TextChannel channel, int[] coords, int zoom, boolean section) {
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
		return channel.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_canvas-chunk"));
	}

	public RestAction<?> addPixel(TextChannel channel, int[] coords, Color color) {
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

	public static synchronized void addPixel(int[] coords, Color color) {
		PixelCanvas pc = Main.getInfo().getCanvas();

		BufferedImage canvas = pc.getCanvas();
		canvas.setRGB(coords[0], coords[1], color.getRGB());
		pc.saveCanvas(canvas);

		CanvasDAO.saveCanvas(pc);
	}

	public boolean isShelved() {
		return shelved;
	}

	public void setShelved(boolean shelved) {
		this.shelved = shelved;
	}
}
