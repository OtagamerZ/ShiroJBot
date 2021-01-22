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
import com.kuuhaku.controller.postgresql.ExceedDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.controller.postgresql.UpvoteDAO;
import com.kuuhaku.controller.sqlite.PStateDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.handlers.games.disboard.model.PoliticalState;
import com.kuuhaku.model.enums.ExceedEnum;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
public class DiscordBotsListHandler {

	@RequestMapping(value = "/webhook/dbl", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
	public void handleVote(@RequestHeader(value = "Authorization") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		JSONObject body = new JSONObject(payload);

		int credit = body.getBoolean("isWeekend") ? 500 : 250;

		Account acc = AccountDAO.getAccount(body.getString("user"));

		if (!body.getString("type").equals("test")) {
			acc.addCredit(credit + (100 * (acc.getStreak() + 1)), this.getClass());
			acc.voted();
		}

		User u = Main.getInfo().getUserByID(body.getString("user"));
		try {
			if (u != null) {
				MessageChannel chn = u.openPrivateChannel().complete();
				Helper.logger(this.getClass()).info(u.getName() + " acabou de votar!");

				EmbedBuilder eb = new EmbedBuilder();

				eb.setThumbnail("https://i.imgur.com/A0jXqpe.png");
				eb.setTitle("Opa, obrigada por votar em mim! (combo " + acc.getStreak() + "/7 -> bônus " + (100 * acc.getStreak()) + "c)");
				eb.setDescription("Como agradecimento, aqui estão " + Helper.separate(credit) + (body.getBoolean("isWeekend") ? " (bônus x2)" : "") + " créditos para serem utilizados nos módulos que utilizam o sistema de dinheiro.\n\n(Nota: você perderá os acúmulos de votos se houver uma diferença de 24h entre este e o próximo voto)");
				eb.setFooter("Seus créditos: " + Helper.separate(acc.getBalance()), "https://i.imgur.com/U0nPjLx.gif");
				eb.addField("Pode resgatar uma gema?", acc.getStreak() == 7 ? "SIM!!" : "Não", true);
				eb.setColor(Color.cyan);

				if (ExceedDAO.hasExceed(u.getId())) {
					PoliticalState ps = PStateDAO.getPoliticalState(ExceedEnum.getByName(ExceedDAO.getExceed(u.getId())));
					ps.modifyInfluence(5);
					PStateDAO.savePoliticalState(ps);

					eb.addField("Bonus ao seu Exceed", "Adicionalmente, seu Exceed recebeu 5 pontos de influência adicionais!", false);
				}

				chn.sendMessage(eb.build()).queue(null, Helper::doNothing);
			}
		} catch (RuntimeException e) {
			Helper.logger(this.getClass()).error(e + " | " + e.getStackTrace()[0]);
		} finally {
			if (u != null) UpvoteDAO.voted(u);
		}
	}
}
