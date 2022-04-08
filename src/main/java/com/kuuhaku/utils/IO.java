/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2022  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.utils;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.BiConsumer;

public class IO {
	public static URL getResource(Class<?> klass, String path) {
		URL url = klass.getClassLoader().getResource(path);
		if (url == null) throw new NullPointerException();
		else return url;
	}

	public static InputStream getResourceAsStream(Class<?> klass, String path) {
		InputStream is = klass.getClassLoader().getResourceAsStream(path);
		if (is == null) throw new NullPointerException();
		else return is;
	}

	public static BufferedImage getResourceAsImage(Class<?> klass, String path) {
		byte[] bytes = Main.getCacheManager().getResourceCache().computeIfAbsent(path, s -> {
			InputStream is = klass.getClassLoader().getResourceAsStream(path);

			if (is == null) return new byte[0];
			else {
				try {
					return getBytes(ImageIO.read(is), path.split("\\.")[1]);
				} catch (IOException e) {
					return new byte[0];
				}
			}
		});

		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
			//return Helper.toCompatibleImage(ImageIO.read(bais));
			return ImageIO.read(bais);
		} catch (IOException e) {
			return null;
		}
	}

	public static File getResourceAsFile(Class<?> klass, String path) {
		try {
			return new File(getResource(klass, path).toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static byte[] getBytes(BufferedImage image) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageIO.write(image, "jpg", bos);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encoding) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageIO.write(image, encoding, bos);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return new byte[0];
		}
	}

	public static byte[] getBytes(BufferedImage image, String encode, float compression) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageWriter writer = ImageIO.getImageWritersByFormatName(encode).next();
				ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
				writer.setOutput(ios);

				ImageWriteParam param = writer.getDefaultWriteParam();
				if (param.canWriteCompressed()) {
					param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					param.setCompressionQuality(compression);
				}

				writer.write(null, new IIOImage(image, null, null), param);
			}


			return baos.toByteArray();
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return new byte[0];
		}
	}

	public static String atob(BufferedImage bi, String encoding) {
		return atob(getBytes(bi, encoding));
	}

	public static String atob(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static BufferedImage btoa(String b64) {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)))) {
			return ImageIO.read(bais);
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}

	public static byte[] btoc(String b64) {
		return Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
	}

	public static BufferedImage toColorSpace(BufferedImage in, int type) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), type);
		Graphics2D g2d = out.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.dispose();
		return out;
	}

	public static void forEachPixel(BufferedImage bi, BiConsumer<int[], Integer> act) {
		int x;
		int y;
		int i = 0;
		while (true) {
			x = i % bi.getWidth();
			y = i / bi.getWidth();

			if (x >= bi.getWidth() || y >= bi.getHeight()) break;

			act.accept(new int[]{x, y}, bi.getRGB(x, y));
			i++;
		}
	}

	public static int[] unpackRGB(int rgb) {
		return new int[]{
				(rgb >> 24) & 0xFF,
				(rgb >> 16) & 0xFF,
				(rgb >> 8) & 0xFF,
				rgb & 0xFF
		};
	}

	public static int packRGB(int a, int r, int g, int b) {
		return a << 24 | r << 16 | g << 8 | b;
	}

	public static int packRGB(int[] argb) {
		return argb[0] << 24 | argb[1] << 16 | argb[2] << 8 | argb[3];
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newMask.createGraphics();
		g2d.drawImage(mask, 0, 0, newMask.getWidth(), newMask.getHeight(), null);
		g2d.dispose();

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int[] rgb = unpackRGB(source.getRGB(x, y));

				int fac = unpackRGB(newMask.getRGB(x, y))[channel];
				source.setRGB(
						x,
						y,
						packRGB(fac, rgb[1], rgb[2], rgb[3])
				);
			}
		}
	}

	public static void applyMask(BufferedImage source, BufferedImage mask, int channel, boolean hasAlpha) {
		BufferedImage newMask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newMask.createGraphics();
		g2d.drawImage(mask, 0, 0, newMask.getWidth(), newMask.getHeight(), null);
		g2d.dispose();

		for (int y = 0; y < source.getHeight(); y++) {
			for (int x = 0; x < source.getWidth(); x++) {
				int[] rgb = unpackRGB(source.getRGB(x, y));

				int fac;
				if (hasAlpha) {
					fac = Math.min(rgb[0], unpackRGB(newMask.getRGB(x, y))[channel + 1]);
				} else
					fac = unpackRGB(newMask.getRGB(x, y))[channel + 1];
				source.setRGB(
						x,
						y,
						packRGB(fac, rgb[1], rgb[2], rgb[3])
				);
			}
		}
	}
}
