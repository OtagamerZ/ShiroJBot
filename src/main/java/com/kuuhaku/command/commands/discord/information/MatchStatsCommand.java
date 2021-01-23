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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.Page;
import com.github.ygimenez.type.PageType;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Command;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.MatchHistory;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NonNls;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MatchStatsCommand extends Command {

	public MatchStatsCommand(String name, String description, Category category, boolean requiresMM) {
		super(name, description, category, requiresMM);
	}

	public MatchStatsCommand(@NonNls String name, @NonNls String[] aliases, String description, Category category, boolean requiresMM) {
		super(name, aliases, description, category, requiresMM);
	}

	public MatchStatsCommand(String name, String usage, String description, Category category, boolean requiresMM) {
		super(name, usage, description, category, requiresMM);
	}

	public MatchStatsCommand(String name, String[] aliases, String usage, String description, Category category, boolean requiresMM) {
		super(name, aliases, usage, description, category, requiresMM);
	}

	@Override
	public void execute(User author, Member member, String rawCmd, String[] args, Message message, MessageChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			List<Page> pages = new ArrayList<>();
			List<MatchHistory> hist = MatchDAO.getMatchesByPlayer(author.getId());

			if (hist.size() == 0) {
				channel.sendMessage("❌ | Você não possui nenhuma partida armazenada.").queue();
				return;
			}

			hist.sort(Comparator.comparingInt(MatchHistory::getId).reversed());
			List<List<MatchHistory>> history = Helper.chunkify(hist, 10);

			EmbedBuilder eb = new ColorlessEmbedBuilder()
					.setTitle(":floppy_disk: | Histórico de partidas de " + author.getName())
					.setFooter("Só são armazenados dados das partidas dos últimos 30 dias.");

			StringBuilder sb = new StringBuilder();
			for (List<MatchHistory> chunk : history) {
				sb.setLength(0);

				for (MatchHistory mh : chunk) {
					LocalDate date = mh.getTimestamp().toInstant().atZone(ZoneId.of("GMT-3")).toLocalDate();
					Map<Side, String> players = mh.getPlayers().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
					sb.append("(**%s**) `ID: %s` - %s **VS** %s\n".formatted(
							date.format(Helper.onlyDate),
							mh.getId(),
							checkUser(players.get(Side.BOTTOM)),
							checkUser(players.get(Side.TOP))
					));
				}

				eb.setDescription(sb.toString());
				pages.add(new Page(PageType.EMBED, eb.build()));
			}

			channel.sendMessage((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
			);
			return;
		} else if (!StringUtils.isNumeric(args[0])) {
			channel.sendMessage("❌ | O ID deve ser numérico.").queue();
			return;
		}

		MatchHistory mh = MatchDAO.getMatch(Integer.parseInt(args[0]));
		if (mh == null) {
			channel.sendMessage("❌ | Partida não encontrada.").queue();
			return;
		}

		LocalDate date = mh.getTimestamp().toInstant().atZone(ZoneId.of("GMT-3")).toLocalDate();
		Map<Side, String> players = mh.getPlayers().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
		Map<Side, Pair<String, Map<String, Integer>>> result = MatchMakingRating.calcMMR(mh);

		String p1 = checkUser(players.get(Side.BOTTOM));
		String p2 = checkUser(players.get(Side.TOP));
		JSONObject stats = getStats(mh);
		JSONObject bottom = stats.getJSONObject("BOTTOM");
		JSONObject top = stats.getJSONObject("TOP");

		boolean wo = result.get(Side.BOTTOM).getRight().get("hp") != 0 && result.get(Side.TOP).getRight().get("hp") != 0;
		boolean p1WO = wo && mh.getRounds().size() % 2 == 0;
		boolean p2WO = wo && mh.getRounds().size() % 2 != 0;

		EmbedBuilder eb = new ColorlessEmbedBuilder()
				.setTitle("Partida de " + p1 + " VS " + p2)
				.addField("Jogada em", date.format(Helper.onlyDate), true)
				.addField("Ordem de jogada", """
						1º: %s %s
						2º: %s %s
						""".formatted(
						p1,
						mh.getWinner() == Side.BOTTOM ? "(VENCEDOR)" : p1WO ? "(W.O.)" : "",
						p2,
						mh.getWinner() == Side.TOP ? "(VENCEDOR)" : p2WO ? "(W.O.)" : ""
				), true)
				.addField("Duração", mh.getRounds().size() + " turnos", true)
				.addField("Eficiencia de " + p1 + " (BAIXO)", """
						Eficiência de mana: %s%%
						Dano X turno: %s%%
						Vida X turno: %s%%
						""".formatted(
						Math.round(bottom.getDouble("manaEff") * 100),
						Math.round(bottom.getDouble("damageEff") * 100),
						Math.round(bottom.getDouble("sustainEff") * 100)
				), true)
				.addField("Eficiencia de " + p2 + " (CIMA)", """
						Eficiência de mana: %s%%
						Dano X turno: %s%%
						Vida X turno: %s%%
						""".formatted(
						Math.round(top.getDouble("manaEff") * 100),
						Math.round(top.getDouble("damageEff") * 100),
						Math.round(top.getDouble("sustainEff") * 100)
				), true);

		channel.sendMessage(eb.build()).queue();
	}

	private String checkUser(String id) {
		try {
			return Main.getInfo().getUserByID(id).getName();
		} catch (Exception e) {
			return "Desconhecido";
		}
	}

	private JSONObject getStats(MatchHistory history) {
		JSONObject out = new JSONObject();
		Map<Side, Pair<String, Map<String, Integer>>> result = MatchMakingRating.calcMMR(history);
		for (Side s : Side.values()) {
			Side other = s == Side.TOP ? Side.BOTTOM : Side.TOP;
			Map<String, Integer> yourResult = result.get(s).getRight();
			Map<String, Integer> hisResult = result.get(other).getRight();
			int spentMana = yourResult.get("mana");
			int damageDealt = hisResult.get("hp");

			if (history.getWinner() == s) {
				double manaEff = 1 + Math.max(-0.75, Math.min(spentMana * 0.5 / 5, 0.25));
				double damageEff = (double) -damageDealt / yourResult.size();
				double expEff = 5000d / yourResult.size();
				double sustainEff = 1 + yourResult.get("hp") / 5000f;

				out.put(s.name(), new JSONObject() {{
					put("manaEff", manaEff);
					put("damageEff", damageEff / expEff);
					put("sustainEff", sustainEff);
				}});
			} else if (history.getWinner() == other) {
				double manaEff = 1 + Math.max(-0.75, Math.min(5 * 0.5 / spentMana, 0.25));
				double damageEff = (double) -damageDealt / yourResult.size();
				double expEff = 5000d / yourResult.size();
				double sustainEff = 1 + yourResult.get("hp") / 5000d;

				out.put(s.name(), new JSONObject() {{
					put("manaEff", manaEff);
					put("damageEff", damageEff / expEff);
					put("sustainEff", sustainEff);
				}});
			}
		}

		return out;
	}
}
