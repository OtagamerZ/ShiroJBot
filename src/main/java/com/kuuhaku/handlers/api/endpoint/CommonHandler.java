/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2021  Yago Gimenez (KuuHaKu)
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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.Main;
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

@RestController
public class CommonHandler {
	@RequestMapping(value = "/collection.jpg", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<byte[]> serveCollectionImage(@RequestParam(value = "id") String id, @RequestParam(value = "m", defaultValue = "img") String method) throws IOException {
		File f = new File(Main.getInfo().getCollectionsFolder(), id + ".jpg");
		if (!f.exists()) throw new FileNotFoundException();
		byte[] bytes = FileUtils.readFileToByteArray(f);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(method.equals("file") ? MediaType.APPLICATION_OCTET_STREAM : MediaType.IMAGE_JPEG);
		headers.setContentLength(bytes.length);

		return new HttpEntity<>(bytes, headers);
	}

	@RequestMapping(value = "/card", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<byte[]> serveCardImage(HttpServletResponse res, @RequestParam(value = "anime", defaultValue = "") String anime, @RequestParam(value = "name", defaultValue = "") String name, @RequestParam(value = "m", defaultValue = "img") String method) throws IOException {
		anime = anime.toUpperCase(Locale.ROOT);
		name = name.toUpperCase(Locale.ROOT);

		if (method.equals("file")) {
			if (anime.isBlank()) {
				res.sendRedirect("/download");
			} else {
				res.sendRedirect("/download?anime=" + anime);
			}

			return new HttpEntity<>(new byte[0]);
		}

		try {
			URL pageUrl = this.getClass().getClassLoader().getResource("template.html");
			if (pageUrl == null) throw new IllegalArgumentException();

			String page = Files.readString(Path.of(pageUrl.toURI()), StandardCharsets.UTF_8).replace("%;", "%%;");
			String item = "<li><a href=\"%s\">%s</a></li>\n";

			if (anime.isBlank()) {
				page = page.replace("<table>", "<ul>")
						.replace("</table>", "</ul>");

				File f = new File(System.getenv("CARDS_PATH"));
				if (!f.exists()) throw new FileNotFoundException();

				StringBuilder sb = new StringBuilder();

				String[] available = Arrays.stream(Helper.getOr(f.listFiles(File::isDirectory), new File[0]))
						.map(File::getName)
						.sorted()
						.toArray(String[]::new);

				for (String s : available) {
					sb.append(item.formatted("?anime=" + s, s));
				}

				byte[] bytes = page.formatted(
						"Animes disponíveis",
						"",
						sb.toString()
				).getBytes(StandardCharsets.UTF_8);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.TEXT_HTML);
				headers.setContentLength(bytes.length);

				return new HttpEntity<>(bytes, headers);
			} else if (name.isBlank()) {
				item = """
						<td>
						    <a href="?anime={0}&name={1}">
						        <img alt="{0}" src="?anime={0}&name={1}"/>
						    </a>
						    <div>{1}</div>
						</td>
						""";

				File f = new File(System.getenv("CARDS_PATH") + anime);
				if (!f.exists()) throw new FileNotFoundException();

				StringBuilder sb = new StringBuilder();

				String[] available = Arrays.stream(Helper.getOr(f.listFiles(fl -> fl.isFile() && !fl.getName().startsWith(".")), new File[0]))
						.map(fl -> fl.getName().replace(".png", ""))
						.sorted()
						.toArray(String[]::new);

				for (int i = 0; i < available.length; i++) {
					String s = available[i];
					if (i % 5 == 0) {
						if (i > 0) sb.append("\n</tr>\n");
						sb.append("\n<tr>\n");
					}
					sb.append(MessageFormat.format(item, anime, s));
				}

				byte[] bytes = page.formatted(
						"Imagens de " + CardDAO.getUltimate(anime).getName(),
						"""
								<a href="?anime=%s&m=file" onclick="document.getElementById('wait').style.visibility = 'visible'">(download)</a>
								""".formatted(anime),
						sb.toString()
				).getBytes(StandardCharsets.UTF_8);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.TEXT_HTML);
				headers.setContentLength(bytes.length);

				return new HttpEntity<>(bytes, headers);
			} else {
				File f = new File(System.getenv("CARDS_PATH") + anime, name + ".png");
				if (!f.exists()) throw new FileNotFoundException();
				byte[] bytes = FileUtils.readFileToByteArray(f);

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.IMAGE_PNG);
				headers.setContentLength(bytes.length);

				return new HttpEntity<>(bytes, headers);
			}
		} catch (URISyntaxException e) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.IMAGE_PNG);
			headers.setContentLength(new byte[0].length);

			return new HttpEntity<>(new byte[0], headers);
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	@RequestMapping(value = "/download", method = RequestMethod.GET)
	public @ResponseBody
	void downloadCardImage(HttpServletResponse res, @RequestParam(value = "anime", defaultValue = "") String anime) throws IOException {
		if (anime.isBlank() || anime.equalsIgnoreCase("ALL")) {
			throw new IllegalArgumentException();
		}

		anime = anime.toUpperCase(Locale.ROOT);

		File f = new File(System.getenv("CARDS_PATH") + anime);
		if (!f.exists()) throw new FileNotFoundException();

		File tmp = Helper.compressDir(f);
		if (tmp == null) throw new FileNotFoundException();

		ContentDisposition cd = ContentDisposition.attachment()
				.filename("kawaipon-" + anime.toLowerCase(Locale.ROOT) + ".7z")
				.build();

		res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
		res.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(tmp.toPath())));
		res.setHeader(HttpHeaders.CONTENT_DISPOSITION, cd.toString());

		try (FileInputStream fis = new FileInputStream(tmp)) {
			Helper.stream(fis, res.getOutputStream());
		} finally {
			tmp.delete();
		}
	}

	@RequestMapping(value = "/image", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<byte[]> serveImage(@RequestParam(value = "id") String id) throws IOException {
		File f = new File(Main.getInfo().getTemporaryFolder(), id + ".jpg");
		if (!f.exists()) throw new FileNotFoundException();
		byte[] bytes = FileUtils.readFileToByteArray(f);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG);
		headers.setContentLength(bytes.length);

		return new HttpEntity<>(bytes, headers);
	}

	@RequestMapping(value = "/readme", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<String> serveReadme() throws IOException {
		File f = new File("README.md");
		if (!f.exists()) throw new FileNotFoundException();

		String readme = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
		headers.setContentLength(readme.length());

		return new HttpEntity<>(readme, headers);
	}

	@RequestMapping(value = "/embedjson", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<String> serveEmbedJson() throws IOException {
		File f = new File("embed_example.json");
		if (!f.exists()) throw new FileNotFoundException();

		String embedJson = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));

		return new HttpEntity<>(embedJson, headers);
	}

	@RequestMapping(value = "/customanswer", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<String> serveCustomAnswerJson() throws IOException {
		File f = new File("customanswer_example.json");
		if (!f.exists()) throw new FileNotFoundException();

		String embedJson = FileUtils.readFileToString(f, StandardCharsets.UTF_8);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));

		return new HttpEntity<>(embedJson, headers);
	}
}
