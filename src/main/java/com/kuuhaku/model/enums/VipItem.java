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
import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Card;
import com.kuuhaku.model.persistent.Kawaipon;
import com.kuuhaku.model.persistent.KawaiponCard;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum VipItem {
	CONVERT_CREDITS(1, 1, new MessageEmbed.Field("1 - Converter para créditos (1 gema)", "Troca 1 gema por 10000 créditos", false),
			(ch, acc, args) -> {
				acc.addCredit(10000, VipItem.class);
				acc.removeGem(1);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("Gema convertida com sucesso!").queue();
			}),
	CARD_ROLL(2, 1, new MessageEmbed.Field("2 - Rodar carta (1 gema)", "Troca a carta por outra aleatória que você não tenha", false),
			(ch, acc, args) -> {
				if (args.length < 2) {
					ch.sendMessage("❌ | Você precisa informar uma carta.").queue();
					return;
				}

				Kawaipon kp = KawaiponDAO.getKawaipon(acc.getUserId());
				Card c = CardDAO.getCard(args[1], false);

				CardStatus cs = Helper.checkStatus(kp);

				if (cs == CardStatus.NO_CARDS) {
					ch.sendMessage("❌ | Você já coletou todas as cartas que existem, parabéns!").queue();
					return;
				}

				if (c == null) {
					ch.sendMessage("❌ | Essa carta não existe.").queue();
					return;
				}

				if (args.length < 3) {
					ch.sendMessage("❌ | Você precisa informar uma carta e o tipo (`N` = normal, `C` = cromada).").queue();
					return;
				} else if (!Helper.equalsAny(args[2], "N", "C")) {
					ch.sendMessage("❌ | Você precisa informar o tipo da carta que deseja rodar (`N` = normal, `C` = cromada).").queue();
					return;
				}
				KawaiponCard card = kp.getCard(c, args[2].equalsIgnoreCase("C"));
				KawaiponCard oldCard = new KawaiponCard(c, args[2].equalsIgnoreCase("C"));

				if (card == null) {
					ch.sendMessage("❌ | Você não pode rodar uma carta que não possui!").queue();
					return;
				}

				boolean foil = cs != CardStatus.NORMAL_CARDS && (args[1].equalsIgnoreCase("C") || cs == CardStatus.FOIL_CARDS);

				List<Card> cards = CardDAO.getCards()
						.stream()
						.filter(cd -> !kp.getCards().contains(new KawaiponCard(cd, foil)))
						.collect(Collectors.toList());

				if (cards.size() == 0) {
					ch.sendMessage("❌ | Você já possui todas as cartas " + (foil ? "cromadas" : "normais") + ", parabéns!").queue();
					return;
				}

				Card chosen = cards.get(Helper.rng(cards.size(), true));

				kp.removeCard(card);
				card.setCard(chosen);
				kp.addCard(card);

				KawaiponDAO.saveKawaipon(kp);
				acc.removeGem(1);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("✅ | Você rodou a carta " + oldCard.getName() + " com sucesso e conseguiu....**" + card.getName() + " (" + card.getCard().getRarity().toString() + ")**!").queue();
			}),
	CARD_FOIL(3, 5, new MessageEmbed.Field("3 - Melhoria de carta (5 gemas)", "Transforma uma carta em cromada", false),
			(ch, acc, args) -> {
				if (args.length < 2) {
					ch.sendMessage("❌ | Você precisa informar uma carta.").queue();
					return;
				}

				Kawaipon kp = KawaiponDAO.getKawaipon(acc.getUserId());
				Card c = CardDAO.getCard(args[1], false);

				if (c == null) {
					ch.sendMessage("❌ | Essa carta não existe.").queue();
					return;
				}

				KawaiponCard card = kp.getCard(c, false);
				KawaiponCard oldCard = new KawaiponCard(c, false);

				if (card == null) {
					ch.sendMessage("❌ | Você não pode cromar uma carta que não possui!").queue();
					return;
				}

				kp.removeCard(card);
				card.setFoil(true);
				kp.addCard(card);

				KawaiponDAO.saveKawaipon(kp);
				acc.removeGem(5);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("✅ | Você cromou a carta " + oldCard.getName() + " com sucesso!").queue();
			}),
	ANIMATED_BACKGROUND(4, 10, new MessageEmbed.Field("4 - Fundo de perfil animado (10 gemas)", "Permite usar GIFs como fundo de perfil", false),
			(ch, acc, args) -> {
				acc.setAnimatedBg(true);
				acc.removeGem(10);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("Fundo de perfil animado habilitado com sucesso!").queue();
			}),
	STASH_SLOT(5, 20, new MessageEmbed.Field("5 - Aumentar capacidade de decks reserva (20 gemas)", "Libera 1 espaço extra nos seus decks reserva", false),
			(ch, acc, args) -> {
				acc.setStashCapacity(acc.getStashCapacity() + 1);
				acc.removeGem(20);
				AccountDAO.saveAccount(acc);

				ch.sendMessage("Capacidade aumentada com sucesso!").queue();
			});

	private final int id;
	private final int gems;
	private final MessageEmbed.Field field;
	private final TriConsumer<TextChannel, Account, String[]> action;

	VipItem(int id, int gems, MessageEmbed.Field field, TriConsumer<TextChannel, Account, String[]> action) {
		this.id = id;
		this.gems = gems;
		this.field = field;
		this.action = action;
	}

	public int getId() {
		return id;
	}

	public int getGems() {
		return gems;
	}

	public MessageEmbed.Field getField() {
		return field;
	}

	public TriConsumer<TextChannel, Account, String[]> getAction() {
		return action;
	}

	public static VipItem getById(int id) {
		return Arrays.stream(values()).filter(vi -> vi.id == id).findFirst().orElse(null);
	}
}
