package com.kuuhaku.utils.helpers;

import com.kuuhaku.Main;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class FileHelper {
	public static void keepMaximumNFiles(File folder, int maximum) {
		if (!folder.isDirectory()) return;
		List<Pair<File, FileTime>> files = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
				.map(f -> {
					FileTime time;
					try {
						time = Files.getLastModifiedTime(f.toPath());
					} catch (IOException e) {
						time = null;
					}
					return Pair.of(f, time);
				})
				.collect(Collectors.toList());

		files.removeIf(p -> p.getRight() == null);

		if (files.size() <= maximum) return;

		files.sort(Comparator.comparing(Pair::getRight));
		while (files.size() > maximum) {
			File file = files.remove(0).getLeft();

			if (!file.delete()) {
				MiscHelper.logger(FileHelper.class).warn("Failed to delete file at " + file.toPath().getFileName());
			}
		}
	}

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
					return ImageHelper.getBytes(ImageIO.read(is), path.split("\\.")[1]);
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

	public static byte[] serialize(Object obj) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
			oos.close();

			return baos.toByteArray();
		}
	}

	public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes); ObjectInputStream bis = new ObjectInputStream(bais)) {
			return bis.readObject();
		}
	}

	public static void stream(InputStream input, OutputStream output) throws IOException {
		byte[] data = new byte[2048];
		int read;
		while ((read = input.read(data)) >= 0) {
			output.write(data, 0, read);
		}

		output.flush();
	}

	public static File compressDir(File file) throws IOException {
		if (file.isDirectory()) {
			Path source = file.toPath();
			File tmp = File.createTempFile("files-all_" + System.currentTimeMillis(), null);
			SevenZOutputFile szof = new SevenZOutputFile(tmp);

			try (szof) {
				Files.walkFileTree(source, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						if (attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;

						Path rel = source.relativize(file);

						try {
							SevenZArchiveEntry entry = szof.createArchiveEntry(file.toFile(), rel.toString());
							szof.putArchiveEntry(entry);
							szof.write(FileUtils.readFileToByteArray(file.toFile()));
							szof.closeArchiveEntry();
						} catch (IOException e) {
							MiscHelper.logger(this.getClass()).error("Error compressing file " + file);
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) {
						MiscHelper.logger(this.getClass()).error("Error opening file " + file);
						return FileVisitResult.CONTINUE;
					}
				});

				szof.finish();

				return tmp;
			}
		}

		return null;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public static byte[] compress(File file) throws IOException {
		if (file.isDirectory()) {
			Path source = file.toPath();
			File tmp = File.createTempFile("files-all_" + System.currentTimeMillis(), null);
			SevenZOutputFile szof = new SevenZOutputFile(tmp);

			try (szof) {
				Files.walkFileTree(source, new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						if (attrs.isSymbolicLink()) return FileVisitResult.CONTINUE;

						Path rel = source.relativize(file);

						try {
							SevenZArchiveEntry entry = szof.createArchiveEntry(file.toFile(), rel.toString());
							szof.putArchiveEntry(entry);
							szof.write(FileUtils.readFileToByteArray(file.toFile()));
							szof.closeArchiveEntry();
						} catch (IOException e) {
							MiscHelper.logger(this.getClass()).error("Error compressing file " + file);
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) {
						MiscHelper.logger(this.getClass()).error("Error opening file " + file);
						return FileVisitResult.CONTINUE;
					}
				});

				szof.finish();

				try {
					return FileUtils.readFileToByteArray(tmp);
				} finally {
					tmp.delete();
				}
			}
		} else {
			return compress(FileUtils.readFileToByteArray(file));
		}
	}

	public static byte[] compress(String data) throws IOException {
		return compress(data.getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] compress(byte[] bytes) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
		GZIPOutputStream gzip = new GZIPOutputStream(baos);

		try (gzip; baos) {
			gzip.write(bytes);
			return baos.toByteArray();
		}
	}

	public static String uncompress(byte[] compressed) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
		GZIPInputStream gis = new GZIPInputStream(bais);
		byte[] bytes = IOUtils.toByteArray(gis);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	public static int findStringInFile(File f, String str) {
		try (Scanner scanner = new Scanner(f, StandardCharsets.UTF_8)) {
			int i = -1;
			while (scanner.hasNextLine()) {
				i++;
				if (scanner.nextLine().equals(str)) {
					return i;
				}
			}
		} catch (IOException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return -1;
	}

	public static int findStringInFile(File f, String str, Function<String, String> mapper) {
		try (Scanner scanner = new Scanner(f, StandardCharsets.UTF_8)) {
			int i = -1;
			while (scanner.hasNextLine()) {
				i++;
				if (mapper.apply(scanner.nextLine()).equals(str)) {
					return i;
				}
			}
		} catch (IOException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return -1;
	}

	public static String getLineFromFile(File f, int line) {
		try (Stream<String> stream = Files.lines(f.toPath(), StandardCharsets.UTF_8)) {
			return stream.skip(line).findFirst().orElse("");
		} catch (IOException e) {
			MiscHelper.logger(MiscHelper.class).error(e + " | " + e.getStackTrace()[0]);
		}

		return null;
	}
}
