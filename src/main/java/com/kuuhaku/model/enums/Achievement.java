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

import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.controller.postgresql.WaifuDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.*;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Race;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.persistent.Account;
import com.kuuhaku.model.persistent.Couple;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.JSONObject;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.collections4.bag.HashBag;

import java.util.*;
import java.util.stream.Collectors;

public enum Achievement {
	SUPREME_WIZARD(Medal.PLATINUM,
			"O Mago Supremo", "Vença uma partida no tier Arquimago.", false),

	THE_COUNCIL(Medal.GOLD,
			"O Conselho", "Vença uma partida no tier Mestre ou superior.", false),
	BLITZKRIEG(Medal.GOLD,
			"Blitzkrieg", "Vença uma partida antes do 5º turno.", false),
	TIMED_BOMB(Medal.GOLD,
			"Bomba Relógio", "Vença a partida durante a morte-súbita III.", false),
	TRUE_JUSTICE(Medal.GOLD,
			"A Verdadeira Justiça", "Vença uma partida com Kira ou L. Lawliet em campo.", false),
	UNFORTUNATE_LUCK(Medal.GOLD,
			"Sorte Maldita", "Comece a partida com 3 campos na mão.", true),
	UNTOUCHABLE(Medal.GOLD,
			"Intocável", "Vença uma partida sem levar dano.", false),
	LAST_STAND(Medal.GOLD,
			"O Último Bastião", "Vença uma partida após sobreviver um ataque fatal.", false),

	SPOOKY_NIGHTS(Medal.SILVER,
			"Noites de Arrepio", "Vença uma partida em Outubro onde seu deck possua apenas criaturas malígnas (espírito, morto-vivo e demônio).", false),
	COUP_DE_GRACE(Medal.SILVER,
			"Golpe de Misericórdia", "Vença uma partida após esvaziar o deck do oponente.", false),

	STAND_UNITED(Medal.BRONZE,
			"Manter União", "Vença uma partida com um parceiro.", false),
	GREED(Medal.BRONZE,
			"Ganância", "Perca uma partida com mais de 20 de mana restante.", true),
	SLOTH(Medal.BRONZE,
			"Preguiça", "Enquanto no lado de baixo, encerre o primeiro turno sem fazer nada.", true),
	LOVERS(Medal.BRONZE,
			"Amantes", "Vença uma partida DUO com seu husbando/waifu.", false),
	JOURNEYS_BEGIN(Medal.BRONZE,
			"O Início da Jornada", "Vença uma partida ranqueada.", false),
	;

	private final Medal medal;
	private final String title;
	private final String description;
	private final boolean hidden;

	public enum Medal {
		PLATINUM(5),
		GOLD(3),
		SILVER(2),
		BRONZE(1);

		private final int value;

		Medal(int value) {
			this.value = value;
		}
	}

	Achievement(Medal medal, String title, String description, boolean hidden) {
		this.medal = medal;
		this.title = title;
		this.description = description;
		this.hidden = hidden;
	}

	public Medal getMedal() {
		return medal;
	}

	public int getValue() {
		return medal.value;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isInvalid(Shoukan game, Side side, boolean last) {
		Hand h = game.getHands().get(side);

		return switch (this) {
			case SUPREME_WIZARD -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield MatchMakingRatingDAO.getMMR(h.getUser().getId()).getTier() != RankedTier.ARCHMAGE;
				}

				yield true;
			}
			case THE_COUNCIL -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield MatchMakingRatingDAO.getMMR(h.getUser().getId()).getTier().getTier() < RankedTier.MASTER.getTier();
				}

				yield true;
			}
			case BLITZKRIEG -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield game.getRound() > 5;
				}

				yield true;
			}
			case TIMED_BOMB -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield game.getRound() != 125;
				}

				yield true;
			}
			case TRUE_JUSTICE -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield game.getArena().getSlots().get(side).stream()
							.map(SlotColumn::getTop)
							.filter(Objects::nonNull)
							.noneMatch(id -> Helper.equalsAny(id, 279, 280));
				}

				yield true;
			}
			case UNFORTUNATE_LUCK -> {
				JSONObject data = game.getAchData().computeIfAbsent(this, k -> new JSONObject());

				if (data.getBoolean("completed")) yield false;
				else if (game.getRound() > (side == Side.TOP ? 1 : 0)) yield true;

				data.put("completed", h.getCards().stream().filter(d -> d instanceof Field).count() >= 3);
				yield false;
			}
			case UNTOUCHABLE -> h.getHp() < h.getPrevHp();
			case LAST_STAND -> {
				JSONObject data = game.getAchData().computeIfAbsent(this, k -> new JSONObject());

				if (data.getBoolean("completed")) yield false;

				data.put("completed", h.getHp() == 1);
				yield false;
			}
			case SPOOKY_NIGHTS -> {
				Calendar c = Calendar.getInstance();
				if (c.get(Calendar.MONTH) != Calendar.OCTOBER) yield true;

				Set<Race> valid = EnumSet.of(Race.SPIRIT, Race.UNDEAD, Race.DEMON);
				Map<Race, Long> races = h.getRaceCount();
				for (Map.Entry<Race, Long> e : races.entrySet()) {
					if (!valid.contains(e.getKey()) && e.getValue() > 0) yield true;
				}

				yield false;
			}
			case COUP_DE_GRACE -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield (!game.getHands().get(side.getOther()).getRealDeque().isEmpty());
				}

				yield true;
			}
			case STAND_UNITED -> {
				if (!last) yield false;

				if (game.getHistory().getWinner().equals(side)) {
					yield (!game.isTeam());
				}

				yield true;
			}
			case GREED -> {
				if (!last) yield false;

				if (!game.getHistory().getWinner().equals(side)) {
					yield h.getMana() < 20;
				}

				yield true;
			}
			case SLOTH -> {
				JSONObject data = game.getAchData().computeIfAbsent(this, k -> new JSONObject());

				if (side == Side.TOP) yield true;
				else if (game.getRound() == 0 && side == Side.BOTTOM) {
					data.put("completed", game.isReroll());
				}

				yield (!data.getBoolean("completed"));
			}
			case LOVERS -> {
				if (!last) yield false;

				if (h instanceof TeamHand th) {
					if (game.getHistory().getWinner().equals(side)) {
						Couple c = WaifuDAO.getCouple(h.getUser().getId());

						if (c != null)
							yield (!th.getUsers().stream()
									.map(User::getId)
									.allMatch(id -> Helper.equalsAny(id, c.getHusbando(), c.getWaifu())));
					}
				}

				yield true;
			}
			case JOURNEYS_BEGIN -> {
				if (!last) yield false;

				yield (!game.getHistory().getWinner().equals(side));
			}
		};
	}

	public static HashBag<Medal> getMedalBag() {
		return Arrays.stream(values())
				.map(Achievement::getMedal)
				.collect(Collectors.toCollection(HashBag::new));
	}

	public String toString(Account acc) {
		if (!acc.getAchievements().contains(this)) {
			if (hidden) return "<:no_trophy:901193752406794280> - ???";
			else return "<:no_trophy:901193752406794280> - " + title;
		}

		return switch (medal) {
			case PLATINUM -> "<:platinum_trophy:901161662294409346> - " + title;
			case GOLD -> "<:gold_trophy:901161662265040966> - " + title;
			case SILVER -> "<:silver_trophy:901161662365716520> - " + title;
			case BRONZE -> "<:bronze_trophy:901161662298619934> - " + title;
		};
	}
}
