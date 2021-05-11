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
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.MatchInfo;
import com.kuuhaku.model.persistent.MatchHistory;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.utils.Helper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "dadosdapartida",
		aliases = {"matchstats", "mstats", "estatisticas"},
		usage = "req_id",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_MANAGE,
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class MatchStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String command, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			List<Page> pages = new ArrayList<>();
			List<MatchHistory> hist = MatchDAO.getMatchesByPlayer(author.getId());

			if (hist.isEmpty()) {
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
					Map<Side, String> players = mh.getPlayers().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
					sb.append("(%s)\n`%sID: %s` - %s **VS** %s **(%s)**\n\n".formatted(
							mh.getTimestamp().format(Helper.fullDateFormat),
							mh.isRanked() ? "\uD83D\uDC51 " : "",
							mh.getId(),
							checkUser(players.get(Side.BOTTOM)),
							checkUser(players.get(Side.TOP)),
							mh.getWinner() == mh.getPlayers().get(author.getId()) ? "V" : "D"
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


		Map<Side, List<String>> players = new HashMap<>();
		for (Map.Entry<String, Side> entry : mh.getPlayers().entrySet()) {
			players.computeIfAbsent(entry.getValue(), s -> new ArrayList<>())
					.add(entry.getKey());
		}

		JSONObject stats = getStats(mh);
		JSONObject bottom = stats.getJSONObject("BOTTOM");
		JSONObject top = stats.getJSONObject("TOP");

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		if (bottom.keySet().size() == 1) {
			String p1 = checkUser(players.get(Side.BOTTOM).get(0));
			String p2 = checkUser(players.get(Side.TOP).get(0));
			JSONObject p1Stats = bottom.getJSONObject(players.get(Side.BOTTOM).get(0));
			JSONObject p2Stats = top.getJSONObject(players.get(Side.TOP).get(0));
			boolean botWO = mh.isWo() && mh.getWinner() != Side.BOTTOM;
			boolean topWO = mh.isWo() && mh.getWinner() != Side.TOP;

			eb.setTitle("Partida de " + p1 + " VS " + p2)
					.addField("Jogada em", mh.getTimestamp().format(Helper.dateFormat), true)
					.addField("Tipo", mh.isRanked() ? "Ranqueada" : "Normal", true)
					.addField("Ordem de jogada", """
							1º: %s %s
							2º: %s %s
							""".formatted(
							p1,
							mh.getWinner() == Side.BOTTOM ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
							p2,
							mh.getWinner() == Side.TOP ? "(VENCEDOR)" : topWO ? "(W.O.)" : ""
					), true)
					.addField("Duração", mh.getRounds().size() + " turnos", true)
					.addField("Eficiencia de " + p1, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p1Stats.getDouble("manaEff") * 100),
							Math.round(p1Stats.getDouble("damageEff") * 100),
							Math.round(p1Stats.getDouble("sustainEff") * 100)
					), false)
					.addField("Eficiencia de " + p2, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p2Stats.getDouble("manaEff") * 100),
							Math.round(p2Stats.getDouble("damageEff") * 100),
							Math.round(p2Stats.getDouble("sustainEff") * 100)
					), false);
		} else {
			String p1 = checkUser(players.get(Side.BOTTOM).get(0));
			String p2 = checkUser(players.get(Side.TOP).get(0));
			String p3 = checkUser(players.get(Side.BOTTOM).get(1));
			String p4 = checkUser(players.get(Side.TOP).get(1));
			JSONObject p1Stats = bottom.getJSONObject(players.get(Side.BOTTOM).get(0));
			JSONObject p2Stats = top.getJSONObject(players.get(Side.TOP).get(0));
			JSONObject p3Stats = bottom.getJSONObject(players.get(Side.BOTTOM).get(1));
			JSONObject p4Stats = top.getJSONObject(players.get(Side.TOP).get(1));
			boolean botWO = mh.isWo() && mh.getWinner() != Side.BOTTOM;
			boolean topWO = mh.isWo() && mh.getWinner() != Side.TOP;

			eb.setTitle("Partida de " + p1 + " e " + p3 + " VS " + p2 + " e " + p4)
					.addField("Jogada em", mh.getTimestamp().format(Helper.dateFormat), true)
					.addField("Tipo", mh.isRanked() ? "Ranqueada" : "Normal", true)
					.addField("Ordem de jogada", """
							1º: %s %s
							2º: %s %s
							3º: %s %s
							4º: %s %s
							""".formatted(
							p1,
							mh.getWinner() == Side.BOTTOM ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
							p2,
							mh.getWinner() == Side.TOP ? "(VENCEDOR)" : topWO ? "(W.O.)" : "",
							p3,
							mh.getWinner() == Side.BOTTOM ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
							p4,
							mh.getWinner() == Side.TOP ? "(VENCEDOR)" : topWO ? "(W.O.)" : ""
					), true)
					.addField("Duração", mh.getRounds().size() + " turnos", true)
					.addField("Eficiencia de " + p1, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p1Stats.getDouble("manaEff") * 100),
							Math.round(p1Stats.getDouble("damageEff") * 100),
							Math.round(p1Stats.getDouble("sustainEff") * 100)
					), false)
					.addField("Eficiencia de " + p2, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p2Stats.getDouble("manaEff") * 100),
							Math.round(p2Stats.getDouble("damageEff") * 100),
							Math.round(p2Stats.getDouble("sustainEff") * 100)
					), false)
					.addField("Eficiencia de " + p3, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p3Stats.getDouble("manaEff") * 100),
							Math.round(p3Stats.getDouble("damageEff") * 100),
							Math.round(p3Stats.getDouble("sustainEff") * 100)
					), false)
					.addField("Eficiencia de " + p4, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Math.round(p4Stats.getDouble("manaEff") * 100),
							Math.round(p4Stats.getDouble("damageEff") * 100),
							Math.round(p4Stats.getDouble("sustainEff") * 100)
					), false);
		}

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
		Map<Side, List<MatchInfo>> result = MatchMakingRating.calcMMR(history);
		for (Side s : Side.values()) {
			Side other = s == Side.TOP ? Side.BOTTOM : Side.TOP;

			for (MatchInfo info : result.get(s)) {
				Map<String, Integer> yourResult = info.getInfo();
				Map<String, Integer> theirResult = Helper.mergeInfo(result.get(other)).getInfo();
				int spentMana = yourResult.get("mana");
				int damageDealt = theirResult.get("hp");

				if (history.getWinner() == s) {
					double manaEff = 1 + Math.max(-0.75, Math.min(spentMana * 0.5 / 5, 0.25));
					double damageEff = (double) -damageDealt / yourResult.size();
					double expEff = 5000d / yourResult.size();
					double sustainEff = 1 + yourResult.get("hp") / 5000f;

					JSONObject data = Helper.getOr(out.optJSONObject(s.name()), new JSONObject());

					data.put(info.getId(), new JSONObject() {{
						put("manaEff", manaEff);
						put("damageEff", damageEff / expEff);
						put("sustainEff", sustainEff);
					}});

					out.put(s.name(), data);
				} else if (history.getWinner() == other) {
					double manaEff = 1 + Math.max(-0.75, Math.min(5 * 0.5 / spentMana, 0.25));
					double damageEff = (double) -damageDealt / yourResult.size();
					double expEff = 5000d / yourResult.size();
					double sustainEff = 1 + yourResult.get("hp") / 5000d;

					JSONObject data = Helper.getOr(out.optJSONObject(s.name()), new JSONObject());

					data.put(info.getId(), new JSONObject() {{
						put("manaEff", manaEff);
						put("damageEff", damageEff / expEff);
						put("sustainEff", sustainEff);
					}});

					out.put(s.name(), data);
				}
			}
		}

		return out;
	}
}
