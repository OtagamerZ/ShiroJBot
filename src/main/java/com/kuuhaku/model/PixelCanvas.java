package com.kuuhaku.model;

import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.LogLevel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import javax.imageio.ImageIO;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static com.kuuhaku.utils.Helper.CANVAS_SIZE;

@Entity
public class PixelCanvas {
	@Id
	private int id;
	private String canvas = "";
	@Column(columnDefinition = "boolean default false")
	private boolean shelved;

	private BufferedImage getCanvas() {
		if (canvas != null) {
			try {
				byte[] bytes = Base64.getDecoder().decode(canvas);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				return ImageIO.read(bais);
			} catch (IOException e) {
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
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
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(getCanvas(), "png", baos);

			return channel.sendFile(baos.toByteArray(), "canvas.png");
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
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
		try {
			BufferedImage bi = new BufferedImage(CANVAS_SIZE / 2, CANVAS_SIZE / 2, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = bi.createGraphics();

			g2d.drawImage(getCanvas().getSubimage(section[0], section[1], 512, 512), 0, 0, null);
			g2d.dispose();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "png", baos);

			return channel.sendFile(baos.toByteArray(), "chunk_" + number + ".png");
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o canvas, estamos resolvendo isso.");
	}

	public RestAction viewChunk(TextChannel channel, int[] coords, int zoom, boolean section) {
		int fac = (int) Math.pow(2, zoom);
		int chunkSize = CANVAS_SIZE / fac;
		try {
			BufferedImage chunk = new BufferedImage(CANVAS_SIZE, CANVAS_SIZE, BufferedImage.TYPE_INT_RGB);
			BufferedImage canvas = new BufferedImage(CANVAS_SIZE + chunkSize, CANVAS_SIZE + chunkSize, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = canvas.createGraphics();

			g2d.drawImage(getCanvas(), (canvas.getWidth() / 2) - CANVAS_SIZE / 2, (canvas.getHeight() / 2) - CANVAS_SIZE / 2, null);

			g2d = chunk.createGraphics();
			int x = (CANVAS_SIZE / 2 / fac) + (coords[0] + CANVAS_SIZE / (section ? 4 : 2)) - (chunkSize / 2);
			int y = (CANVAS_SIZE / 2 / fac) + (CANVAS_SIZE / (section ? 4 : 2) - coords[1]) - (chunkSize / 2);
			g2d.drawImage(canvas.getSubimage(x, y, chunkSize, chunkSize).getScaledInstance(CANVAS_SIZE, CANVAS_SIZE, 0), 0, 0, null);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(chunk, "png", baos);

			return channel.sendFile(baos.toByteArray(), "chunk.png");
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
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
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(canvas, "png", baos);
			this.canvas = Base64.getEncoder().encodeToString(baos.toByteArray());
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
	}
}
