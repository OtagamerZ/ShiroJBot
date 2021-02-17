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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.model.persistent.GuildConfig;
import com.kuuhaku.utils.Helper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GuildHandler {

	@RequestMapping(value = "/guild/get", method = RequestMethod.GET)
	public GuildConfig requestGuild(@RequestParam(value = "id") String id) {
		return GuildDAO.getGuildById(id);
	}

	@RequestMapping(value = "/guild/update", method = RequestMethod.POST)
	public void updateGuild(@RequestParam(value = "guild") String guild, @RequestParam(value = "token") String token) {
		try {
			ObjectMapper mapper = new ObjectMapper();

			GuildConfig gc = mapper.readValue(guild, GuildConfig.class);

			if (TokenDAO.validateToken(token)) {
				GuildDAO.updateGuildSettings(gc);
			} else {
				throw new InvalidTokenException();
			}
		} catch (IOException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
