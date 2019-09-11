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
		BufferedImage bi = new BufferedImage(1000, 500, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.setColor(Color.decode("#333333"));
		g2d.fillRect(0, 0, 1000, 500);
		g2d.dispose();

		return bi;
	}

	public RestAction addPixel(TextChannel channel, int[] coords, Color color) {
		try {
			BufferedImage canvas = getCanvas();
			Graphics2D g2d = canvas.createGraphics();
			g2d.setColor(color);
			g2d.fillRect(coords[0], coords[1], 1, 1);
			g2d.dispose();
			saveCanvas(canvas);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(canvas, "png", baos);

			return channel.sendFile(baos.toByteArray(), "canvas.png");
		} catch (IOException e) {
			Helper.log(this.getClass(), LogLevel.ERROR, e + " | " + e.getStackTrace()[0]);
		}
		return channel.sendMessage(":x: | Erro ao recuperar o canvas, estamos resolvendo isso.");
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
