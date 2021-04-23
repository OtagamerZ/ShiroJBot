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

package com.kuuhaku.handlers.api.endpoint;

import com.kuuhaku.Main;
import com.kuuhaku.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
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
	@RequestMapping(value = "/collection", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<byte[]> serveCollectionImage(@RequestParam(value = "id") String id) throws IOException {
		File f = new File(Main.getInfo().getCollectionsFolder(), id + ".jpg");
		if (!f.exists()) throw new FileNotFoundException();
		byte[] bytes = FileUtils.readFileToByteArray(f);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.IMAGE_JPEG);
		headers.setContentLength(bytes.length);

		return new HttpEntity<>(bytes, headers);
	}

	@RequestMapping(value = "/card", method = RequestMethod.GET)
	public @ResponseBody
	HttpEntity<byte[]> serveCardImage(@RequestParam(value = "name", defaultValue = "") String name, @RequestParam(value = "anime", defaultValue = "") String anime) throws IOException {
		name = name.toUpperCase(Locale.ROOT);
		anime = anime.toUpperCase(Locale.ROOT);

		try {
			URL pageUrl = this.getClass().getClassLoader().getResource("template.html");
			if (pageUrl == null) throw new IllegalArgumentException();

			String page = Files.readString(Path.of(pageUrl.toURI()), StandardCharsets.UTF_8);
			String item = "<li><a href=\"%s\">%s</a></li>\n";

			if (anime.isBlank()) {
				page = page.replace("<table>", "<ul>")
						.replace("</table>", "</ul>")
						.replace("%;", "%%;");

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

				byte[] bytes = page.formatted(sb.toString()).getBytes(StandardCharsets.UTF_8);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.TEXT_HTML);
				headers.setContentLength(bytes.length);

				return new HttpEntity<>(bytes, headers);
			} else if (name.isBlank()) {
				page = page.replace("%;", "%%;");
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

				byte[] bytes = page.formatted(sb.toString()).getBytes(StandardCharsets.UTF_8);
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
}
