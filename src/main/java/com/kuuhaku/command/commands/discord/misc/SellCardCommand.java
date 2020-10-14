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

package com.kuuhaku.command.commands.discord.misc;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Equipment;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SellCardCommand extends Command {

	public SellCardCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public SellCardCommand(String name, String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public SellCardCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public SellCardCommand(@NonNls String name, @NonNls String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		Kawaipon kp = KawaiponDAO.getKawaipon(author.getId());

		if (args.length < 2) {
			channel.sendMessage("❌ | Você precisa informar uma carta, o tipo (`N` = normal, `C` = cromada) e o preço dela. Se for vender um equipamento, basta informar a carta e o preço dela.").queue();
			return;
		} else if (args.length == 2) {
			Equipment eq = CardDAO.getEquipment(args[0]);

			if (eq == null) {
				channel.sendMessage("❌ | Esse equipamento não existe, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getAllEquipmentNames().toArray(String[]::new)) + "`?").queue();
				return;
			}

			if (!kp.getEquipments().contains(eq)) {
				channel.sendMessage("❌ | Você não pode vender um equipamento que não possui!").queue();
				return;
			}

			try {
				boolean hasLoan = AccountDAO.getAccount(kp.getUid()).getLoan() > 0;
				int price = Integer.parseInt(args[1]);
				int min = eq.getTier() * (hasLoan ? Helper.BASE_CARD_PRICE * 2 : Helper.BASE_CARD_PRICE / 2);

				if (price < min) {
					if (hasLoan)
						channel.sendMessage("❌ | Como você possui uma dívida ativa, você não pode vender esse equipamento por menos que " + min + " créditos.").queue();
					else
						channel.sendMessage("❌ | Você não pode vender esse equipamento por menos que " + min + " créditos.").queue();
					return;
				}

				String hash = Helper.generateHash(guild, author);
				ShiroInfo.getHashes().add(hash);
				Main.getInfo().getConfirmationPending().put(author.getId(), true);
				channel.sendMessage("Este equipamento sairá da sua coleção, você ainda poderá comprá-lo novamente pelo mesmo preço. Deseja mesmo anunciá-lo?").queue(s -> {
					Pages.buttonize(s, Map.of(Helper.ACCEPT, (member1, message1) -> {
						if (!ShiroInfo.getHashes().remove(hash)) return;
						Main.getInfo().getConfirmationPending().invalidate(author.getId());
						if (member1.getId().equals(author.getId())) {
							kp.removeEquipment(eq);
							KawaiponDAO.saveKawaipon(kp);

							EquipmentMarket em = new EquipmentMarket(author.getId(), eq, price);
							EquipmentMarketDAO.saveCard(em);

							s.delete().flatMap(d -> channel.sendMessage(":white_check_mark: | Equipamento anunciado com sucesso!")).queue();
						}
					}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
						ShiroInfo.getHashes().remove(hash);
						Main.getInfo().getConfirmationPending().invalidate(author.getId());
					});
				});
			} catch (NumberFormatException e) {
				channel.sendMessage("❌ | O valor máximo é " + Integer.MAX_VALUE + " créditos!").queue();
			}
			return;
		} else if (!StringUtils.isNumeric(args[2])) {
			channel.sendMessage("❌ | O preço precisa ser um valor inteiro.").queue();
			return;
		} else if (!Helper.equalsAny(args[1], "N", "C")) {
			channel.sendMessage("❌ | Você precisa informar o tipo da carta que deseja vender (`N` = normal, `C` = cromada).").queue();
			return;
		} else if (Main.getInfo().getConfirmationPending().getIfPresent(author.getId()) != null) {
			channel.sendMessage("❌ | Você possui um comando com confirmação pendente, por favor resolva-o antes de usar este comando novamente.").queue();
			return;
		}

		Card c = CardDAO.getCard(args[0], false);

		boolean foil = args[1].equalsIgnoreCase("C");

		if (c == null) {
			channel.sendMessage("❌ | Essa carta não existe, você não quis dizer `" + Helper.didYouMean(args[0], CardDAO.getAllCardNames().toArray(String[]::new)) + "`?").queue();
			return;
		}

		KawaiponCard card = kp.getCard(c, foil);

		if (card == null) {
			channel.sendMessage("❌ | Você não pode vender uma carta que não possui!").queue();
			return;
		}

		try {
			boolean hasLoan = AccountDAO.getAccount(kp.getUid()).getLoan() > 0;
			int price = Integer.parseInt(args[2]);
			int min = c.getRarity().getIndex() * (hasLoan ? Helper.BASE_CARD_PRICE * 2 : Helper.BASE_CARD_PRICE / 2) * (foil ? 2 : 1);

			if (price < min) {
				if (hasLoan)
					channel.sendMessage("❌ | Como você possui uma dívida ativa, você não pode vender essa carta por menos que " + min + " créditos.").queue();
				else
					channel.sendMessage("❌ | Você não pode vender essa carta por menos que " + min + " créditos.").queue();
				return;
			}

			String hash = Helper.generateHash(guild, author);
			ShiroInfo.getHashes().add(hash);
			Main.getInfo().getConfirmationPending().put(author.getId(), true);
			channel.sendMessage("Esta carta sairá da sua coleção, você ainda poderá comprá-la novamente pelo mesmo preço. Deseja mesmo anunciá-la?").queue(s -> {
				Pages.buttonize(s, Map.of(Helper.ACCEPT, (mb, ms) -> {
					if (!ShiroInfo.getHashes().remove(hash)) return;
					Main.getInfo().getConfirmationPending().invalidate(author.getId());
					if (mb.getId().equals(author.getId())) {
						kp.removeCard(card);
						KawaiponDAO.saveKawaipon(kp);

						CardMarket cm = new CardMarket(author.getId(), card, price);
						CardMarketDAO.saveCard(cm);

						s.delete().flatMap(d -> channel.sendMessage(":white_check_mark: | Carta anunciada com sucesso!")).queue();
					}
				}), true, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()), ms -> {
					ShiroInfo.getHashes().remove(hash);
					Main.getInfo().getConfirmationPending().invalidate(author.getId());
				});
			});
		} catch (NumberFormatException e) {
			channel.sendMessage("❌ | O valor máximo é " + Integer.MAX_VALUE + " créditos!").queue();
		}
	}
}