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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.persistent.Account;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
public class DiscordBotsListHandler {

	@RequestMapping(value = "/webhook/dbl", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
	public void handleVote(@RequestHeader(value = "Authorization") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		JSONObject body = new JSONObject(payload);

		int credit = body.getBoolean("isWeekend") ? 250 : 125;

		Account acc = AccountDAO.getAccount(body.getString("user"));

		if (!body.getString("type").equals("test")) acc.addCredit(credit + (25 * acc.getStreak()));

		try {
			MessageChannel chn = Main.getInfo().getUserByID(body.getString("user")).openPrivateChannel().complete();

			EmbedBuilder eb = new EmbedBuilder();

			eb.setThumbnail("https://i.imgur.com/A0jXqpe.png");
			eb.setTitle("Opa, obrigada por votar em mim! (combo " + acc.getStreak() + "/7 -> bônus " + 25 * acc.getStreak() + "c)");
			eb.setDescription("Como agradecimento, aqui estão " + credit + (body.getBoolean("isWeekend") ? " (bônus x2)" : "") + " créditos para serem utilizados nos módulos que utilizam o sistema de dinheiro.");
			eb.setFooter("Seus créditos: " + acc.getBalance(), "https://i.imgur.com/U0nPjLx.gif");
			eb.setColor(Color.cyan);

			chn.sendMessage(eb.build()).queue();
		} catch (RuntimeException ignore) {
		}

		acc.voted();

		AccountDAO.saveAccount(acc);
	}
}
