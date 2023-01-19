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

package com.kuuhaku.util;

import com.kuuhaku.Constants;
import com.kuuhaku.Main;
import com.luciad.imageio.webp.WebPWriteParam;
import okio.Buffer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class IO {
	public static InputStream getResourceAsStream(String path) {
		InputStream is = IO.class.getClassLoader().getResourceAsStream(path);
		if (is == null) throw new NullPointerException();
		else return is;
	}

	public static BufferedImage getResourceAsImage(String path) {
		byte[] bytes = Main.getCacheManager().computeResource(path, (k, v) -> {
			if (v != null && v.length > 0) return v;

			try (InputStream is = IO.class.getClassLoader().getResourceAsStream(path)) {
				if (is == null) return new byte[0];
				else return IOUtils.toByteArray(is);
			} catch (IOException e) {
				return new byte[0];
			}
		});

		return imageFromBytes(bytes);
	}

	public static File getResourceAsFile(String path) {
		try {
			URL url = IO.class.getClassLoader().getResource(path);
			if (url == null) return null;
			else return new File(url.toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	public static BufferedImage getImage(String url) {
		byte[] bytes = Main.getCacheManager().computeResource(url, (k, v) -> {
			if (v != null && v.length > 0) return v;

			try {
				return getBytes(ImageIO.read(new URL(url)), getImageType(url));
			} catch (IllegalArgumentException | IOException e) {
				return new byte[0];
			}
		});

		return imageFromBytes(bytes);
	}

	public static byte[] getBytes(BufferedImage image) {
		return getBytes(image, "jpg", 0.8f);
	}

	public static byte[] getBytes(BufferedImage image, String encoding) {
		return getBytes(image, encoding, 0.8f);
	}

	public static byte[] getBytes(BufferedImage image, String encoding, float quality) {
		try (Buffer buf = new Buffer()) {
			ImageWriter writer = ImageIO.getImageWritersByFormatName(encoding).next();
			ImageOutputStream ios = ImageIO.createImageOutputStream(buf.outputStream());
			writer.setOutput(ios);

			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				if (param instanceof WebPWriteParam webp) {
					webp.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
				} else {
					param.setCompressionType(param.getCompressionTypes()[0]);
				}

				param.setCompressionQuality(quality);
			}

			try {
				writer.write(null, new IIOImage(image, null, null), param);
			} finally {
				writer.dispose();
				ios.flush();
			}

			return buf.readByteArray();
		} catch (IOException e) {
			Constants.LOGGER.error(e, e);
			return new byte[0];
		}
	}

	public static BufferedImage imageFromBytes(byte[] bytes) {
		if (bytes.length == 0) return null;

		try (Buffer buf = new Buffer().write(bytes)) {
			return Graph.toCompatibleImage(ImageIO.read(buf.inputStream()));
		} catch (IOException e) {
			return null;
		}
	}

	public static String atob(BufferedImage bi, String encoding) {
		return atob(getBytes(bi, encoding));
	}

	public static String atob(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static BufferedImage btoa(String b64) {
		try (Buffer buf = new Buffer().write(Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8)))) {
			return ImageIO.read(buf.inputStream());
		} catch (IOException | NullPointerException e) {
			return null;
		}
	}

	public static byte[] btoc(String b64) {
		return Base64.getDecoder().decode(b64.getBytes(StandardCharsets.UTF_8));
	}

	public static String readString(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			return null;
		}
	}

	public static byte[] compress(String data) throws IOException {
		return compress(data.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] compress(byte[] bytes) throws IOException {
		Buffer buf = new Buffer();
		GZIPOutputStream gzip = new GZIPOutputStream(buf.outputStream());

		try (gzip; buf) {
			gzip.write(bytes);
			gzip.finish();

			return buf.readByteArray();
		}
	}

	public static String uncompress(byte[] compressed) throws IOException {
		Buffer buf = new Buffer().write(compressed);
		GZIPInputStream gzip = new GZIPInputStream(buf.inputStream());

		try (gzip; buf) {
			return new String(IOUtils.toByteArray(gzip), StandardCharsets.UTF_8);
		}
	}

	public static String getImageType(String url) {
		try {
			HttpHead req = new HttpHead(url);

			try (CloseableHttpResponse res = API.HTTP.execute(req)) {
				Header h = res.getLastHeader(HttpHeaders.CONTENT_TYPE);


				if (h != null) {
					return h.getValue().substring(h.getValue().indexOf("/") + 1);
				} else {
					return null;
				}
			}
		} catch (IOException e) {
			return null;
		}
	}

	public static long getImageSize(String url) {
		try {
			HttpHead req = new HttpHead(url);

			try (CloseableHttpResponse res = API.HTTP.execute(req)) {
				Header h = res.getLastHeader(HttpHeaders.CONTENT_LENGTH);

				if (h != null) {
					return NumberUtils.toLong(h.getValue(), 0);
				} else {
					return 0;
				}
			}
		} catch (IOException e) {
			return 0;
		}
	}
}
