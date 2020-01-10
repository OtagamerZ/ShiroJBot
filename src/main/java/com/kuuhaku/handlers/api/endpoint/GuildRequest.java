package com.kuuhaku.handlers.api.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.controller.sqlite.GuildDAO;
import com.kuuhaku.handlers.api.exception.InvalidTokenException;
import com.kuuhaku.model.GuildConfig;
import com.kuuhaku.utils.Helper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GuildRequest {

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
		} catch (JsonProcessingException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		}
	}
}
