/*
 * This file is part of Shiro J Bot.
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
import com.kuuhaku.controller.mysql.AccountDAO;
import com.kuuhaku.controller.mysql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.persistent.Account;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
public class DiscordBotsListHandler {

	@RequestMapping(value = "/webhook/dbl", method = RequestMethod.POST)
	public void handleVote(@RequestHeader(value = "Authorization") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		JSONObject body = new JSONObject(payload);

		int credit = body.getBoolean("isWeekend") ? 250 : 125;

		Account acc = AccountDAO.getAccount(body.getString("user"));

		acc.addCredit(credit);

		AccountDAO.saveAccount(acc);

		try {
			MessageChannel chn = Main.getInfo().getUserByID(body.getString("user")).openPrivateChannel().complete();

			EmbedBuilder eb = new EmbedBuilder();

			eb.setThumbnail("https://i.imgur.com/A0jXqpe.png");
			eb.setTitle("Olá, obrigado por votar em mim!");
			eb.setDescription("Como agradecimento, aqui estão " + credit + (body.getBoolean("isWeekend") ? " (bônus x2)" : "") + " créditos para serem utilizados nos módulos que utilizam o sistema de dinheiro.");
			eb.setFooter("Seus créditos: " + acc.getBalance());
			eb.setColor(Color.green);

			chn.sendMessage(eb.build()).queue();
		} catch (RuntimeException ignore) {
		}
	}
}
