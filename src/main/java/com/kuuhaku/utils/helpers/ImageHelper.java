package com.kuuhaku.utils.helpers;

import com.kuuhaku.Main;
import com.kuuhaku.model.common.GifFrame;
import com.kuuhaku.utils.GifSequenceWriter;
import com.kuuhaku.utils.functional.NContract;
import de.androidpit.colorthief.ColorThief;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.common.bytesource.ByteSourceInputStream;
import org.apache.commons.imaging.formats.gif.DisposalMethod;
import org.apache.commons.imaging.formats.gif.GifImageMetadata;
import org.apache.commons.imaging.formats.gif.GifImageMetadataItem;
import org.apache.commons.imaging.formats.gif.GifImageParser;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.CRC32;

public abstract class ImageHelper {
	public static Color colorThief(String url) throws IOException {
		BufferedImage icon = ImageIO.read(getImage(url));

		return colorThief(icon);
	}

	public static Color colorThief(BufferedImage image) {
		try {
			if (image != null) {
				int[] colors = ColorThief.getColor(image, 5, false);
				return new Color(colors[0], colors[1], colors[2]);
			} else return getRandomColor();
		} catch (NullPointerException e) {
			return getRandomColor();
		}
	}

	public static InputStream getImage(String link) throws IOException {
		return new URL(link).openStream();
	}


	public static Color reverseColor(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsv);
		hsv[0] = (hsv[0] * 360 + 180) / 360;

		return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
	}


	public static String getRandomHexColor() {
		String[] colorTable = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(colorTable[MathHelper.clamp(MathHelper.rng(16), 0, 16)]);
		}
		return "#" + sb;
	}

	public static Color textToColor(String text) {
		CRC32 crc = new CRC32();
		crc.update(text.getBytes(StandardCharsets.UTF_8));
		return Color.decode("#%06x".formatted(crc.getValue() & 0xFFFFFF));
	}

	public static Color getRandomColor() {
		return Color.decode("#%06x".formatted(MathHelper.rng(0xFFFFFF)));
	}

	public static Color getRandomColor(long seed) {
		return Color.decode("#%06x".formatted(MathHelper.rng(0xFFFFFF, seed)));
	}

	public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
		int width = imgSize.width;
		int height = imgSize.height;

		if (imgSize.width > boundary.width) {
			width = boundary.width;
			height = (width * imgSize.height) / imgSize.width;
		}

		if (height > boundary.height) {
			height = boundary.height;
			width = (height * imgSize.width) / imgSize.height;
		}

		return new Dimension(width, height);
	}

	public static byte[] getBytes(BufferedImage image) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (BufferedOutputStream bos = new BufferedOutputStream(baos)) {
				ImageIO.write(image, "jpg", bos);
			}

			return baos.toByteArray();
		} catch (IOException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
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
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
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
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
			return new byte[0];
		}
	}

	public static void drawRotated(Graphics2D g2d, BufferedImage bi, int x, int y, double deg) {
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(deg), x, y);
		g2d.drawImage(bi, 0, 0, null);
		g2d.setTransform(old);
	}

	public static void writeRotated(Graphics2D g2d, String s, int x, int y, double deg) {
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(deg), x, y);
		g2d.drawString(s, 0, -10);
		g2d.setTransform(old);
	}

	public static void darkenImage(float fac, BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getHeight(); x++) {
				Color rgb = new Color(image.getRGB(x, y), true);
				int r = MathHelper.clamp(Math.round(rgb.getRed() * fac), 0, 255);
				int g = MathHelper.clamp(Math.round(rgb.getGreen() * fac), 0, 255);
				int b = MathHelper.clamp(Math.round(rgb.getBlue() * fac), 0, 255);
				image.setRGB(x, y, new Color(r, g, b, rgb.getAlpha()).getRGB());
			}
		}
	}

	public static BufferedImage scaleImage(BufferedImage image, int w, int h) {
		double thumbRatio = (double) w / (double) h;
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		double aspectRatio = (double) imageWidth / (double) imageHeight;

		if (thumbRatio > aspectRatio) {
			h = (int) (w / aspectRatio);
		} else {
			w = (int) (h * aspectRatio);
		}

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, 0, 0, w, h, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage scaleImage(BufferedImage image, int prcnt) {
		int w = image.getWidth() / prcnt;
		int h = image.getHeight() / prcnt;

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, 0, 0, w, h, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage scaleAndCenterImage(BufferedImage image, int w, int h) {
		image = scaleImage(image, w, h);

		int offX = Math.min((image.getWidth() - w) / -2, 0);
		int offY = Math.min((image.getHeight() - h) / -2, 0);

		BufferedImage newImage = new BufferedImage(w, h, image.getType());
		Graphics2D g2d = newImage.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2d.drawImage(image, offX, offY, null);
		g2d.dispose();

		return newImage;
	}

	public static BufferedImage removeAlpha(BufferedImage input) {
		BufferedImage bi = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2d.drawImage(input, 0, 0, null);
		g2d.dispose();

		return bi;
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

	public static BufferedImage toColorSpace(BufferedImage in, int type) {
		BufferedImage out = new BufferedImage(in.getWidth(), in.getHeight(), type);
		Graphics2D g2d = out.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.dispose();
		return out;
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

	public static File writeAndGet(BufferedImage bi, String name, String extension) {
		File tempFolder = Main.getInfo().getTemporaryFolder();
		File f = new File(tempFolder, name + "." + extension);

		try {
			ImageIO.write(bi, extension, f);
		} catch (IOException e) {
			try {
				ImageIO.write(bi, extension.equals("png") ? "jpg" : "png", f);
			} catch (IOException ignore) {
			}
		}

		return f;
	}

	public static File writeAndGet(BufferedImage bi, String name, String extension, File parent) {
		File f = new File(parent, name + "." + extension);

		try {
			ImageIO.write(bi, extension, f);
		} catch (IOException ignore) {
		}

		return f;
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

	public static void forEachFrame(List<BufferedImage> frames, Consumer<Graphics2D> act) {
		for (BufferedImage frame : frames) {
			Graphics2D g2d = frame.createGraphics();
			act.accept(g2d);
			g2d.dispose();
		}
	}

	public static CompletableFuture<Void> forEachFrame(List<BufferedImage> frames, ExecutorService exec, Consumer<Graphics2D> act) {
		NContract<Void> con = new NContract<>(frames.size());
		for (BufferedImage frame : frames) {
			exec.execute(() -> {
				Graphics2D g2d = frame.createGraphics();
				act.accept(g2d);
				g2d.dispose();

				con.addSignature(0, null);
			});
		}

		return con;
	}

	public static BufferedImage applyOverlay(BufferedImage in, BufferedImage overlay) {
		BufferedImage bi = new BufferedImage(in.getWidth(), in.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(in, 0, 0, null);
		g2d.drawImage(overlay, 0, 0, null);
		g2d.dispose();

		return bi;
	}

	public static List<GifFrame> readGif(String url) throws IOException, ImageReadException {
		ByteSourceInputStream bsis = new ByteSourceInputStream(getImage(url), "temp");

		GifImageParser gip = new GifImageParser();
		GifImageMetadata gim = (GifImageMetadata) gip.getMetadata(bsis);
		List<BufferedImage> frames = gip.getAllBufferedImages(bsis);
		List<GifImageMetadataItem> metas = gim.getItems();
		List<GifFrame> out = new ArrayList<>();

		for (int i = 0; i < Math.min(frames.size(), metas.size()); i++) {
			BufferedImage frame = frames.get(i);
			GifImageMetadataItem meta = metas.get(i);

			out.add(new GifFrame(
							frame,
							meta.getDisposalMethod(),
							gim.getWidth(),
							gim.getHeight(),
							meta.getLeftPosition(),
							meta.getTopPosition(),
							meta.getDelay() * 10
					)
			);
		}

		return out;
	}

	public static List<GifFrame> readGif(String url, boolean uncompress) throws IOException, ImageReadException {
		ByteSourceInputStream bsis = new ByteSourceInputStream(getImage(url), "temp");

		GifImageParser gip = new GifImageParser();
		GifImageMetadata gim = (GifImageMetadata) gip.getMetadata(bsis);
		List<GifImageMetadataItem> metas = gim.getItems();

		List<BufferedImage> frames = gip.getAllBufferedImages(bsis);
		if (uncompress) {
			List<BufferedImage> source = List.copyOf(frames);
			BufferedImage bi = deepCopy(source.get(0));

			BufferedImage finalBi = bi;
			frames = new ArrayList<>() {{
				add(deepCopy(finalBi));
			}};
			Graphics2D g = bi.createGraphics();
			DisposalMethod method = DisposalMethod.UNSPECIFIED;

			for (int i = 1; i < source.size(); i++) {
				GifImageMetadataItem meta = metas.get(i);

				switch (method) {
					case RESTORE_TO_BACKGROUND -> {
						g.dispose();
						bi = deepCopy(source.get(0));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
					case RESTORE_TO_PREVIOUS -> {
						g.dispose();
						bi = deepCopy(frames.get(Math.max(0, i - 1)));
						g = bi.createGraphics();

						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
					default -> {
						BufferedImage frame = source.get(i);
						g.drawImage(frame, 0, 0, null);
						frames.add(deepCopy(bi));
					}
				}

				method = meta.getDisposalMethod();
			}

			g.dispose();
		}

		List<GifFrame> out = new ArrayList<>();

		for (int i = 0; i < Math.min(frames.size(), metas.size()); i++) {
			BufferedImage frame = frames.get(i);
			GifImageMetadataItem meta = metas.get(i);

			out.add(new GifFrame(
							frame,
							uncompress ? DisposalMethod.RESTORE_TO_BACKGROUND : meta.getDisposalMethod(),
							gim.getWidth(),
							gim.getHeight(),
							meta.getLeftPosition(),
							meta.getTopPosition(),
							meta.getDelay() * 10
					)
			);
		}

		return out;
	}

	public static void makeGIF(File f, List<GifFrame> frames) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						frame.getDelay(),
						0
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						frame.getDelay(),
						repeat
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay, int compress) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}

		if (compress > 0) try {
			Process p = Runtime.getRuntime().exec("mogrify -layers 'optimize' -fuzz " + compress + "% " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}
	}

	public static void makeGIF(File f, List<GifFrame> frames, int repeat, int delay, int compress, int colors) throws IOException {
		try (ImageOutputStream ios = new FileImageOutputStream(f)) {
			GifSequenceWriter gif = new GifSequenceWriter(ios, BufferedImage.TYPE_INT_ARGB);
			for (GifFrame frame : frames) {
				gif.writeToSequence(
						frame.getAdjustedFrame(),
						frame.getDisposal().ordinal(),
						delay == -1 ? frame.getDelay() : delay,
						repeat
				);
			}
			gif.finish();
		}

		if (compress > 0) try {
			Process p = Runtime.getRuntime().exec("mogrify -layers 'optimize' -fuzz " + compress + "% " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}

		if (colors > 0) try {
			Process p = Runtime.getRuntime().exec("gifsicle -O3 --colors " + colors + " --lossy=30 " + f.getAbsolutePath());
			p.waitFor();
		} catch (InterruptedException ignore) {
		}
	}

	public static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(bi.getRaster().createCompatibleWritableRaster());
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	public static boolean hasAlpha(BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, y) & 0xFF000000) < 0xFF000000)
					return true;
			}
		}

		return false;
	}

	public static void drawSquareLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
		int half = x1 + (x2 - x1) / 2;

		g2d.drawPolyline(
				new int[]{x1, half, half, x2},
				new int[]{y1, y1, y2, y2},
				4
		);
	}

	public static void drawCenteredString(Graphics2D g2d, String str, int x, int y, int width, int height) {
		int xOffset = width / 2 - g2d.getFontMetrics().stringWidth(str) / 2;
		int yOffset = height / 2 + g2d.getFont().getSize() / 2;
		g2d.drawString(str, x + xOffset, y + yOffset);
	}

	public static int toLuma(int r, int g, int b) {
		return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
	}

	public static int toLuma(int[] rgb) {
		return (int) (0.2126 * rgb[1] + 0.7152 * rgb[2] + 0.0722 * rgb[3]);
	}

	public static int toLuma(int rgb) {
		return (int) (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF));
	}
}
