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
import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.controller.postgresql.UpvoteDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.io.File;

@RestController
public class DiscordBotsListHandler {

	@RequestMapping(value = "/webhook/dbl", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public void handleVote(@RequestHeader(value = "Authorization") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		JSONObject body = new JSONObject(payload);

		int credit = body.getBoolean("isWeekend") ? 500 : 250;

		Account acc = AccountDAO.getAccount(body.getString("user"));
		if (acc.hasVoted(false)) return;

		if (!body.getString("type").equals("test")) {
			acc.addCredit(credit + (100L * (acc.getStreak() + 1)), this.getClass());
			acc.voted();
		}

		User u = Main.getInfo().getUserByID(body.getString("user"));
		try {
			if (u != null) {
				MessageChannel chn = u.openPrivateChannel().complete();
				Helper.logger(this.getClass()).info(u.getName() + " acabou de votar!");

				File icon = Helper.getResourceAsFile(this.getClass(), "assets/gem/gem_" + acc.getStreak() + ".png");
				assert icon != null;

				EmbedBuilder eb = new EmbedBuilder()
						.setThumbnail("attachment://gem.png")
						.setTitle("Opa, obrigada por votar em mim! (combo " + acc.getStreak() + "/7 -> bônus " + (100 * acc.getStreak()) + "c)")
						.setDescription("Como agradecimento, aqui estão " + Helper.separate(credit) + (body.getBoolean("isWeekend") ? " (bônus x2)" : "") + " CR para serem utilizados nos módulos que utilizam o sistema de dinheiro.\n\n(Nota: você perderá os acúmulos de votos se houver uma diferença de 24h entre este e o próximo voto)")
						.setFooter("Seus CR: " + Helper.separate(acc.getBalance()), "https://i.imgur.com/U0nPjLx.gif")
						.addField("Pode resgatar uma gema?", acc.getStreak() == 7 ? "SIM!!" : "Não", true)
						.setColor(Color.cyan);

				chn.sendMessageEmbeds(eb.build())
						.addFile(icon, "gem.png")
						.queue(null, Helper::doNothing);
			}
		} catch (RuntimeException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		} finally {
			if (u != null) UpvoteDAO.voted(u);
		}
	}

	public static void retry(String uid) {
		Main.getInfo().getDblApi().getVotingMultiplier()
				.thenAccept(mult -> {
					int credit = mult.isWeekend() ? 500 : 250;

					Account acc = AccountDAO.getAccount(uid);

					acc.addCredit(credit + (100L * (acc.getStreak() + 1)), DiscordBotsListHandler.class);
					acc.voted();

					User u = Main.getInfo().getUserByID(uid);
					try {
						if (u != null) {
							MessageChannel chn = u.openPrivateChannel().complete();
							Helper.logger(DiscordBotsListHandler.class).info(u.getName() + " teve o voto contabilizado!");

							EmbedBuilder eb = new EmbedBuilder();

							eb.setThumbnail("https://i.imgur.com/A0jXqpe.png");
							eb.setTitle("Opa, obrigada por votar em mim! (combo " + acc.getStreak() + "/7 -> bônus " + (100 * acc.getStreak()) + "c)");
							eb.setDescription("Como agradecimento, aqui estão " + Helper.separate(credit) + (mult.isWeekend() ? " (bônus x2)" : "") + " CR para serem utilizados nos módulos que utilizam o sistema de dinheiro.\n\n(Nota: você perderá os acúmulos de votos se houver uma diferença de 24h entre este e o próximo voto)");
							eb.setFooter("Seus CR: " + Helper.separate(acc.getBalance()), "https://i.imgur.com/U0nPjLx.gif");
							eb.addField("Pode resgatar uma gema?", acc.getStreak() == 7 ? "SIM!!" : "Não", true);
							eb.setColor(Color.cyan);

							chn.sendMessageEmbeds(eb.build()).queue(null, Helper::doNothing);
						}
					} catch (RuntimeException e) {
						Helper.logger(DiscordBotsListHandler.class).error(e + " | " + e.getStackTrace()[0]);
					} finally {
						if (u != null) UpvoteDAO.voted(u);
					}
				});
	}
}