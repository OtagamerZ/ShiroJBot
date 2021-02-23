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

package com.kuuhaku.model.enums;

import com.kuuhaku.controller.postgresql.AccountDAO;
import com.kuuhaku.controller.postgresql.RatingDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.SupportRating;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Arrays;

public enum SupportItem {
	TOKEN_TO_CREDIT(1, 1, new MessageEmbed.Field("1 - Converter para créditos (1 token)", "Troca 1 token por 1.000 créditos", false),
			(ch, sr, args) -> {
				Account acc = AccountDAO.getAccount(sr.getId());

				if (args.length > 0) {
					if (!StringUtils.isNumeric(args[1])) {
						ch.sendMessage(ShiroInfo.getLocale(I18n.PT).getString("err_invalid-amount")).queue();
						return;
					}

					int amount = Integer.parseInt(args[1]);

					if (sr.getThanksTokens() < amount) {
						ch.sendMessage("❌ | Você não possui tokens suficientes.").queue();
						return;
					}

					acc.addCredit(1000L * amount, SupportItem.class);
					sr.useThanksToken(amount);
					AccountDAO.saveAccount(acc);

					ch.sendMessage("Tokens convertidos com sucesso!").queue();
				} else {
					acc.addCredit(1000, SupportItem.class);
					sr.useThanksToken(1);
					AccountDAO.saveAccount(acc);

					ch.sendMessage("Token convertido com sucesso!").queue();
				}

				RatingDAO.saveRating(sr);
			}),
	TOKEN_TO_GEM(2, 100, new MessageEmbed.Field("2 - Converter para gema (100 tokens)", "Troca 100 tokens por 1 gema", false),
			(ch, sr, args) -> {
				Account acc = AccountDAO.getAccount(sr.getId());

				acc.addGem(1);
				sr.useThanksToken(1);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("Token convertido com sucesso!").queue();
				RatingDAO.saveRating(sr);
			});

	private final int id;
	private final int tokens;
	private final MessageEmbed.Field field;
	private final TriConsumer<TextChannel, SupportRating, String[]> action;

	SupportItem(int id, int tokens, MessageEmbed.Field field, TriConsumer<TextChannel, SupportRating, String[]> action) {
		this.id = id;
		this.tokens = tokens;
		this.field = field;
		this.action = action;
	}

	public int getId() {
		return id;
	}

	public int getTokens() {
		return tokens;
	}

	public MessageEmbed.Field getField() {
		return field;
	}

	public TriConsumer<TextChannel, SupportRating, String[]> getAction() {
		return action;
	}

	public static SupportItem getById(int id) {
		return Arrays.stream(values()).filter(vi -> vi.id == id).findFirst().orElse(null);
	}
}
