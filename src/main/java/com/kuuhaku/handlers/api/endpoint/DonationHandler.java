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
import com.kuuhaku.controller.postgresql.DonationDAO;
import com.kuuhaku.controller.postgresql.TokenDAO;
import com.kuuhaku.handlers.api.exception.UnauthorizedException;
import com.kuuhaku.model.enums.DonationBundle;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Donation;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
public class DonationHandler {

	@RequestMapping(value = "/webhook/donate", consumes = "application/json", produces = "application/json", method = RequestMethod.POST)
	public void handleDonation(@RequestHeader(value = "Authorization") String token, @RequestBody String payload) {
		if (!TokenDAO.validateToken(token)) throw new UnauthorizedException();

		JSONObject data = new JSONObject(payload);
		Account acc = AccountDAO.getAccount(data.optString("raw_buyer_id"));
		User u = Main.getInfo().getUserByID(acc.getUserId());
		DonationBundle db = DonationBundle.getById(data.optString("product_id", "null"));

		EmbedBuilder eb = new EmbedBuilder();

		if (u == null || db == null) {
			String reason;

			if (u == null) {
				reason = "Usuário com ID " + acc.getUserId() + " não foi encontrado.";
			} else {
				reason = "Pacote de doação `" + data.optString("product_id", "null") + "` não foi encontrado.";
			}

			eb.setColor(Color.red)
					.setTitle("Erro ao processar doação: " + reason)
					.setDescription("Por favor verifique a razão do ocorrido e entre em contato com o usuário em questão o mais breve possível.")
					.addField("Email de contato:", data.getString("buyer_email"), true);

			for (String d : ShiroInfo.getDevelopers()) {
				Main.getInfo().getUserByID(d).openPrivateChannel()
						.flatMap(c -> c.sendMessage(eb.build()))
						.queue();
			}

			return;
		}

		Donation d = new Donation(
				data.optString("txn_id"),
				data.optString("raw_buyer_id"),
				db,
				data.optFloat("price"),
				data.optString("status")
		);
		int amount = db.isCumulative() ? Math.round(db.getCredits() * d.getValue()) : db.getCredits();
		switch (d.getStatus()) {
			case "Completed" -> {
				acc.addCredit(amount, this.getClass());
				acc.addGem(db.getGems());

				AccountDAO.saveAccount(acc);

				eb.setColor(Helper.getRandomColor())
						.setTitle("Obrigada por me ajudar a ficar online! <3")
						.setDescription("Nós da OtagamerZ ficamos feliz que esteja apoiando o desenvolvimento de nossa querida bot Shiro, e agradecemos de coração por todo o tempo que você esteve conosco!")
						.addField("Você recebeu:",
								(amount > 0 ? "- " + amount + " créditos.\n" : "") +
										(db.getGems() > 0 ? "- " + db.getGems() + " gemas." : ""),
								true
						)
						.setFooter("Cod. da transação: " + String.join("_", d.getBundle().name().toLowerCase(), d.getTransaction(), d.getUid()));

				u.openPrivateChannel()
						.flatMap(c -> c.sendMessage(eb.build()))
						.queue(null, Helper::doNothing);

				for (String dev : ShiroInfo.getDevelopers()) {
					Main.getInfo().getUserByID(dev).openPrivateChannel()
							.flatMap(c -> c.sendMessage("**" + u.getAsTag() + "** fez uma doação de **R$ " + d.getValue() + "**."))
							.queue();
				}
			}
			case "Reversed", "Refunded" -> {
				if (acc.getBalance() < amount)
					acc.addLoan(amount);
				else
					acc.removeCredit(amount, this.getClass());

				if (acc.getGems() < db.getGems())
					acc.addLoan(10000 * db.getGems());
				else
					acc.removeGem(db.getGems());

				AccountDAO.saveAccount(acc);

				eb.setColor(Color.red)
						.setTitle("Sua doação foi recusada/reembolsada")
						.setDescription("Não foi possível confirmar seu pagamento ou houve um pedido de reembolso, a transação foi revertida e os créditos/gemas ganhos foram removidos de sua conta.")
						.addField("Revertido:",
								(amount > 0 ? "- " + amount + " créditos.\n" : "") +
										(db.getGems() > 0 ? "- " + db.getGems() + " gemas." : ""),
								true
						)
						.setFooter("Cod. da transação: " + String.join("_", d.getBundle().name().toLowerCase(), d.getTransaction(), d.getUid()));

				u.openPrivateChannel()
						.flatMap(c -> c.sendMessage(eb.build()))
						.queue(null, Helper::doNothing);

				for (String dev : ShiroInfo.getDevelopers()) {
					Main.getInfo().getUserByID(dev).openPrivateChannel()
							.flatMap(c -> c.sendMessage("**" + u.getAsTag() + "** teve sua doação de **R$ " + d.getValue() + "** recusada/reembolsada."))
							.queue();
				}
			}
			case "sub_ended" -> {
				eb.setColor(Helper.getRandomColor())
						.setTitle("Obrigada pelo seu apoio até agora1")
						.setDescription("Seu pacote mensal chegou ao fim, agradecemos pelo seu apoio e desejamos que continue utilizando a Shiro!")
						.setFooter("Cod. da transação: " + String.join("_", d.getBundle().name().toLowerCase(), d.getTransaction(), d.getUid()));

				u.openPrivateChannel()
						.flatMap(c -> c.sendMessage(eb.build()))
						.queue(null, Helper::doNothing);
			}
		}

		DonationDAO.register(d);
	}
}
