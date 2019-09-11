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

@Entity
public class PixelCanvas {
	@Id
	private int id;
	@Column(columnDefinition = "String default \"\"")
	private String canvas;

	public BufferedImage getCanvas() {
		if (canvas != null) {
			try {
				byte[] bytes = Base64.getDecoder().decode(canvas);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				return ImageIO.read(bais);
			} catch (IOException e) {
				Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
			}
		}
		BufferedImage bi = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.decode("#333333"));
		g2d.fillRect(0, 0, 512, 512);
		g2d.dispose();

		return bi;
	}

	public RestAction viewCanvas(TextChannel channel) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedImage canvas = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = canvas.createGraphics();

			g2d.drawImage(getCanvas().getScaledInstance(2048, 2048, 0), 0, 0, null);
			g2d.dispose();

			ImageIO.write(canvas, "png", baos);

			return channel.sendFile(baos.toByteArray(), "canvas.png");
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o canvas, estamos resolvendo isso.");
	}

	public RestAction viewChunk(TextChannel channel, int[] coords, int zoom) {
		int fac = (int) Math.pow(2, zoom);
		try {
			BufferedImage chunk = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = chunk.createGraphics();

			g2d.drawImage(getCanvas().getSubimage((coords[0] + 256) - (256 / fac), (256 - coords[1]) - (256 / fac), 512 / fac, 512 / fac).getScaledInstance(2048, 2048, 0), 0, 0, null);

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
		canvas.setRGB(coords[0] + 256, 256 - coords[1], color.getRGB());
		saveCanvas(canvas);

		return viewCanvas(channel);
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
