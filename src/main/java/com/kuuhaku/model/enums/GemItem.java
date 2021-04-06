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
import com.kuuhaku.utils.TriFunction;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.stream.Collectors;

public enum GemItem {
	CONVERT_CREDITS(
			"Converter para créditos", "Converte N gemas em 10.000 créditos cada",
			1,
			(mb, chn, args) -> {
				Account acc = AccountDAO.getAccount(mb.getId());

				int amount = 1;
				if (args.length > 1) {
					try {
						amount = Integer.parseInt(args[1]);
					} catch (NumberFormatException ignore) {
					}
				}

				if (acc.getGems() < amount) {
					chn.sendMessage("❌ | Você não possui gemas suficientes.").queue();
					return false;
				}

				acc.addCredit(10000L * amount, GemItem.class);
				acc.removeGem(amount - 1);
				AccountDAO.saveAccount(acc);

				chn.sendMessage(amount + " gemas convertidas em " + (10000 * amount) + " creditos!").queue();
				return true;
			}
	),
	CARD_ROLL(
			"Rodar carta", "Troca uma carta por outra aleatória que você não tenha",
			1,
			(mb, chn, args) -> {
				if (args.length < 2) {
					chn.sendMessage("❌ | Você precisa informar uma carta.").queue();
					return false;
				}

				Kawaipon kp = KawaiponDAO.getKawaipon(mb.getId());
				Card c = CardDAO.getCard(args[1], false);
				CardStatus cs = Helper.checkStatus(kp);

				if (cs == CardStatus.NO_CARDS) {
					chn.sendMessage("❌ | Você já coletou todas as cartas que existem, parabéns!").queue();
					return false;
				} else if (c == null) {
					chn.sendMessage("❌ | Essa carta não existe.").queue();
					return false;
				} else if (args.length > 2 && !Helper.equalsAny(args[2], "N", "C")) {
					chn.sendMessage("❌ | O tipo da carta deve ser `N` ou `C` (`N` = normal, `C` = cromada).").queue();
					return false;
				}

				boolean foil = args.length > 2 && args[2].equalsIgnoreCase("C");
				KawaiponCard card = kp.getCard(c, foil);

				if (card == null) {
					chn.sendMessage("❌ | Você não pode rodar uma carta que não possui!").queue();
					return false;
				} else if (cs != CardStatus.ALL_CARDS) {
					if (cs == CardStatus.NORMAL_CARDS && !foil) {
						chn.sendMessage("❌ | Você já coletou todas as cartas normais que existem, parabéns!").queue();
						return false;
					} else if (cs == CardStatus.FOIL_CARDS && foil) {
						chn.sendMessage("❌ | Você já coletou todas as cartas cromadas que existem, parabéns!").queue();
						return false;
					}
				}

				List<Card> cards = CardDAO.getCards()
						.stream()
						.filter(cd -> !kp.getCards().contains(new KawaiponCard(cd, foil)))
						.collect(Collectors.toList());

				Card chosen = cards.get(Helper.rng(cards.size(), true));

				card.setCard(chosen);
				KawaiponDAO.saveKawaipon(kp);

				chn.sendMessage("Você rodou a carta " + card.getName() + " e obteve....**" + card.getName() + " (" + card.getCard().getRarity().toString() + ")**!").queue();
				return true;
			}
	),
	CARD_FOIL(
			"Melhoria de carta", "Transforma uma carta em cromada",
			5,
			(mb, chn, args) -> {
				if (args.length < 2) {
					chn.sendMessage("❌ | Você precisa informar uma carta.").queue();
					return false;
				}

				Kawaipon kp = KawaiponDAO.getKawaipon(mb.getId());
				Card c = CardDAO.getCard(args[1], false);
				CardStatus cs = Helper.checkStatus(kp);

				if (cs == CardStatus.FOIL_CARDS) {
					chn.sendMessage("❌ | Você já coletou todas as cartas cromadas que existem, parabéns!").queue();
					return false;
				} else if (c == null) {
					chn.sendMessage("❌ | Essa carta não existe.").queue();
					return false;
				}

				KawaiponCard card = kp.getCard(c, false);

				if (card == null) {
					chn.sendMessage("❌ | Você não pode rodar uma carta que não possui!").queue();
					return false;
				} else if (kp.getCard(c, true) != null) {
					chn.sendMessage("❌ | Você já possui essa carta cromada!").queue();
					return false;
				}

				card.setFoil(true);
				KawaiponDAO.saveKawaipon(kp);

				return true;
			}
	),
	ANIMATED_BACKGROUND(
			"Fundo de perfil animado", "Permite usar GIFs como fundo de perfil",
			10,
			(mb, chn, args) -> {
				Account acc = AccountDAO.getAccount(mb.getId());

				if (acc.hasAnimatedBg()) {
					chn.sendMessage("❌ | Você já possui fundo animado habilitado.").queue();
					return false;
				}

				acc.setAnimatedBg(true);
				AccountDAO.saveAccount(acc);

				return true;
			}
	),
	STASH_SLOT(
			"Aumentar capacidade de decks reserva", "Libera 1 espaço extra nos seus decks reserva (máximo 10 slots)",
			20,
			(mb, chn, args) -> {
				Account acc = AccountDAO.getAccount(mb.getId());

				if (acc.getStashCapacity() >= 10) {
					chn.sendMessage("❌ | Você já alcançou a capacidade máxima de decks reserva.").queue();
					return false;
				}

				acc.setStashCapacity(acc.getStashCapacity() + 1);
				AccountDAO.saveAccount(acc);

				return true;
			}
	);

	private final String name;
	private final String desc;
	private final int price;
	private final TriFunction<Member, TextChannel, String[], Boolean> effect;

	GemItem(String name, String desc, int price, TriFunction<Member, TextChannel, String[], Boolean> effect) {
		this.name = name;
		this.desc = desc;
		this.price = price;
		this.effect = effect;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public int getPrice() {
		return price;
	}

	public TriFunction<Member, TextChannel, String[], Boolean> getEffect() {
		return effect;
	}
}
