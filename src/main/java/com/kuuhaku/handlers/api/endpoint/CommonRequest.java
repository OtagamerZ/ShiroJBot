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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@RestController
public class CommonRequest {
	@RequestMapping(value = "/collection", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody
	byte[] serveCollectionImage(@RequestParam(value = "id") String id) throws IOException {
		File f = new File(Main.getInfo().getCollectionsFolder(), id + ".jpg");
		if (!f.exists()) throw new FileNotFoundException();
		return Helper.getBytes(ImageIO.read(f));
	}

	@RequestMapping(value = "/card", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
	public @ResponseBody
	byte[] serveCardImage(@RequestParam(value = "name") String name, @RequestParam(value = "anime", defaultValue = "") String anime) throws IOException {
		File f = new File(System.getenv("CARDS_PATH") + (anime == null ? "" : "-new/" + anime), name + ".png");
		if (!f.exists()) throw new FileNotFoundException();
		return Helper.getBytes(ImageIO.read(f), "png");
	}
}
