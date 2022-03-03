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

package com.kuuhaku.command.commands.discord.information;

import com.github.ygimenez.method.Pages;
import com.github.ygimenez.model.InteractPage;
import com.github.ygimenez.model.Page;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.MatchDAO;
import com.kuuhaku.handlers.games.tabletop.games.shoukan.enums.Side;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.persistent.MatchHistory;
import com.kuuhaku.model.records.MatchInfo;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Command(
		name = "dadosdapartida",
		aliases = {"matchstats", "mstats", "estatisticas"},
		usage = "req_id",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
public class MatchStatsCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
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
					Map<Side, String> players = new HashMap<>();
					for (Map.Entry<String, Side> e : mh.getPlayers().entrySet()) {
						players.compute(e.getValue(),
								(k, v) -> {
									if (v == null) {
										return Helper.getUsername(e.getKey());
									} else {
										return Helper.properlyJoin().apply(List.of(v, Helper.getUsername(e.getKey())));
									}
								}
						);
					}

					sb.append("(%s)\n`%sID: %s` - %s **VS** %s **(%s)**\n\n".formatted(
							mh.getTimestamp().format(Helper.FULL_DATE_FORMAT),
							mh.isRanked() ? "\uD83D\uDC51 " : "",
							mh.getId(),
							players.get(Side.BOTTOM),
							players.get(Side.TOP),
							mh.getWinner() == mh.getPlayers().get(author.getId()) ? "V" : "D"
					));
				}

				eb.setDescription(sb.toString());
				pages.add(new InteractPage(eb.build()));
			}

			channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
					Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
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
		for (Map.Entry<String, Side> e : mh.getPlayers().entrySet()) {
			players.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
		}

		Map<String, MatchInfo> stats = mh.getStats();

		boolean botWO = mh.isWo() && mh.getWinner() != Side.BOTTOM;
		boolean topWO = mh.isWo() && mh.getWinner() != Side.TOP;

		EmbedBuilder eb = new ColorlessEmbedBuilder();
		if (stats.size() == 2) {
			String p1 = players.get(Side.BOTTOM).get(0);
			String p1Name = Helper.getUsername(p1);

			String p2 = players.get(Side.TOP).get(0);
			String p2Name = Helper.getUsername(p2);

			MatchInfo bot = stats.get(p1);
			MatchInfo top = stats.get(p2);

			eb.setTitle("Partida de " + p1Name + " VS " + p2Name)
					.setDescription("""
									Jogada em %s (%s)
																		
									__**Ordem de jogada**__
									1º: %s %s
									2º: %s %s
									""".formatted(
									mh.getTimestamp().format(Helper.DATE_FORMAT),
									mh.isRanked() ? "Ranqueada" : "Normal",
									p1Name,
									bot.winner() ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
									p2Name,
									top.winner() ? "(VENCEDOR)" : topWO ? "(W.O.)" : ""
							)
					)
					.addField("Duração", mh.getRounds().size() + " turnos", true)
					.addField("Eficiencia de " + p1Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(bot.manaEff() * 100, 1),
							Helper.roundToString(bot.damageEff() * 100, 1),
							Helper.roundToString(bot.sustainEff() * 100, 1)
					), true)
					.addField("Eficiencia de " + p2Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(top.manaEff() * 100, 1),
							Helper.roundToString(top.damageEff() * 100, 1),
							Helper.roundToString(top.sustainEff() * 100, 1)
					), true);
		} else {
			String p1 = players.get(Side.BOTTOM).get(0);
			String p1Name = Helper.getUsername(p1);

			String p2 = players.get(Side.TOP).get(0);
			String p2Name = Helper.getUsername(p2);

			String p3 = players.get(Side.BOTTOM).get(1);
			String p3Name = Helper.getUsername(p3);

			String p4 = players.get(Side.TOP).get(1);
			String p4Name = Helper.getUsername(p4);

			MatchInfo bot1 = stats.get(p1);
			MatchInfo top1 = stats.get(p2);
			MatchInfo bot2 = stats.get(p3);
			MatchInfo top2 = stats.get(p4);

			eb.setTitle("Partida de " + Helper.properlyJoin().apply(List.of(p1Name, p3Name)) + " VS " + Helper.properlyJoin().apply(List.of(p2Name, p4Name)))
					.setDescription("""
									Jogada em %s (%s)
																		
									__**Ordem de jogada**__
									1º: %s %s
									2º: %s %s
									3º: %s %s
									4º: %s %s
									""".formatted(
									mh.getTimestamp().format(Helper.DATE_FORMAT),
									mh.isRanked() ? "Ranqueada" : "Normal",
									p1Name,
									bot1.winner() ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
									p2Name,
									top1.winner() ? "(VENCEDOR)" : topWO ? "(W.O.)" : "",
									p3Name,
									bot2.winner() ? "(VENCEDOR)" : botWO ? "(W.O.)" : "",
									p4Name,
									top2.winner() ? "(VENCEDOR)" : topWO ? "(W.O.)" : ""
							)
					)
					.addField("Duração", mh.getRounds().size() + " turnos", true)
					.addField("Eficiencia de " + p1Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(bot1.manaEff() * 100, 1),
							Helper.roundToString(bot1.damageEff() * 100, 1),
							Helper.roundToString(bot1.sustainEff() * 100, 1)
					), true)
					.addField("Eficiencia de " + p2Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(top1.manaEff() * 100, 1),
							Helper.roundToString(top1.damageEff() * 100, 1),
							Helper.roundToString(top1.sustainEff() * 100, 1)
					), true)
					.addField("Eficiencia de " + p3Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(bot2.manaEff() * 100, 1),
							Helper.roundToString(bot2.damageEff() * 100, 1),
							Helper.roundToString(bot2.sustainEff() * 100, 1)
					), true)
					.addField("Eficiencia de " + p4Name, """
							Eficiência de mana: %s%%
							Dano X turno: %s%%
							Vida X turno: %s%%
							""".formatted(
							Helper.roundToString(top2.manaEff() * 100, 1),
							Helper.roundToString(top2.damageEff() * 100, 1),
							Helper.roundToString(top2.sustainEff() * 100, 1)
					), true);
		}

		channel.sendMessageEmbeds(eb.build()).queue();
	}
}
