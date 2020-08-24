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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
public class CommonRequest {
	private final static Cache<String, byte[]> imageCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

	@RequestMapping(value = "/cdn", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody
	byte[] serveImage(@RequestParam(value = "id", defaultValue = "") String code) {
		System.out.println(code);
		return imageCache.getIfPresent(code);
	}

	public static Cache<String, byte[]> getImageCache() {
		return imageCache;
	}
}
