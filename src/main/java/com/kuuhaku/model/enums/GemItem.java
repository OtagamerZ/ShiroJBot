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

package com.kuuhaku.model.enums;

import com.kuuhaku.controller.postgresql.CardDAO;
import com.kuuhaku.controller.postgresql.DynamicParameterDAO;
import com.kuuhaku.controller.postgresql.KawaiponDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.Hero;
import com.kuuhaku.model.persistent.*;
import com.kuuhaku.utils.helpers.CollectionHelper;
import com.kuuhaku.utils.helpers.MiscHelper;
import com.kuuhaku.utils.functional.TriFunction;
import com.kuuhaku.utils.helpers.LogicHelper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.stream.Collectors;

public enum GemItem {
	CONVERT_CREDITS(
			"Converter para CR", "Converte N gemas em 10.000 CR cada",
			1,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());

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
				acc.save();

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
				CardStatus cs = MiscHelper.checkStatus(kp);

				if (cs == CardStatus.NO_CARDS) {
					chn.sendMessage("❌ | Você já coletou todas as cartas que existem, parabéns!").queue();
					return false;
				} else if (c == null) {
					chn.sendMessage("❌ | Essa carta não existe.").queue();
					return false;
				} else if (args.length > 2 && !LogicHelper.equalsAny(args[2], "N", "C")) {
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

				Card chosen = CollectionHelper.getRandomEntry(cards);

				card.setCard(chosen);
				KawaiponDAO.saveKawaipon(kp);

				chn.sendMessage("Você rodou a carta " + card.getName() + " e obteve....**" + card.getName() + " (" + card.getCard().getRarity().toString() + ")**!").queue();
				return true;
			}
	),
	SEED_REROLL(
			"Aleatorizar seed (max: 3 por semana)", "Aleatoriza a seed do seu herói atual (altera missões, efeitos e perks disponíveis)",
			1,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());
				if (acc.getWeeklyRolls() <= 0) {
					chn.sendMessage("❌ | Você não possui rolls semanais restantes.").queue();
					return false;
				}

				Hero h = KawaiponDAO.getHero(mb.getId());
				if (h == null) {
					chn.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
					return false;
				}

				acc.setWeeklyRolls(acc.getWeeklyRolls() - 1);
				acc.save();

				h.randomizeSeed();
				KawaiponDAO.saveHero(h);

				return true;
			}
	),
	CARD_STASH_SIZE(
			"Aumentar capacidade do armazém pessoal", "Aumenta a quantidade máxima de cartas armazenadas em seu estoque pessoal em 15",
			2,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());

				acc.setCardStashCapacity(acc.getCardStashCapacity() + 15);
				acc.save();

				return true;
			}
	),
	STATS_REROLL(
			"Resetar atributos", "Zera os atributos do seu herói, permitindo realocá-los",
			3,
			(mb, chn, args) -> {
				Hero h = KawaiponDAO.getHero(mb.getId());
				if (h == null) {
					chn.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
					return false;
				}

				h.resetStats();
				KawaiponDAO.saveHero(h);

				return true;
			}
	),
	PERK_REROLL(
			"Resetar perks", "Zera as perks selecionadas, permitindo realocá-las",
			3,
			(mb, chn, args) -> {
				Hero h = KawaiponDAO.getHero(mb.getId());
				if (h == null) {
					chn.sendMessage("❌ | Você não possui ou não selecionou um herói.").queue();
					return false;
				}

				h.getPerks().clear();
				KawaiponDAO.saveHero(h);

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
				CardStatus cs = MiscHelper.checkStatus(kp);

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
	GODS_BLESSING(
			"Bênção dos deuses", "Sua próxima síntese normal estará garantida de ser uma das últimas 10 cartas adicionados.",
			6,
			(mb, chn, args) -> {
				DynamicParameter dp = DynamicParameterDAO.getParam("blessing_" + mb.getId());

				if (!dp.getValue().isBlank()) {
					chn.sendMessage("❌ | Você já possui uma bênção ativa.").queue();
					return false;
				}

				DynamicParameterDAO.setParam("blessing_" + mb.getId(), "gods");

				return true;
			}
	),
	ANIMATED_BACKGROUND(
			"Fundo de perfil animado", "Permite usar GIFs como fundo de perfil",
			10,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());

				if (acc.hasAnimatedBg()) {
					chn.sendMessage("❌ | Você já possui fundo animado habilitado.").queue();
					return false;
				}

				acc.setAnimatedBg(true);
				acc.save();

				return true;
			}
	),
	DECK_STASH_SLOT(
			"Aumentar capacidade de decks reserva", "Libera 1 espaço extra nos seus decks reserva (máximo 10 slots)",
			15,
			(mb, chn, args) -> {
				Account acc = Account.find(Account.class, mb.getId());

				if (acc.getDeckStashCapacity() >= 10) {
					chn.sendMessage("❌ | Você já alcançou a capacidade máxima de decks reserva.").queue();
					return false;
				}

				acc.setDeckStashCapacity(acc.getDeckStashCapacity() + 1);
				acc.save();

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
