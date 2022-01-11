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
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.command.Slashed;
import com.kuuhaku.controller.postgresql.MatchMakingRatingDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.annotations.Requires;
import com.kuuhaku.model.annotations.SlashCommand;
import com.kuuhaku.model.annotations.SlashGroup;
import com.kuuhaku.model.common.ColorlessEmbedBuilder;
import com.kuuhaku.model.common.MatchMaking;
import com.kuuhaku.model.enums.LobbyType;
import com.kuuhaku.model.enums.RankedQueue;
import com.kuuhaku.model.enums.RankedTier;
import com.kuuhaku.model.exceptions.ValidationException;
import com.kuuhaku.model.persistent.MatchMakingRating;
import com.kuuhaku.model.records.DuoLobby;
import com.kuuhaku.model.records.SoloLobby;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Command(
		name = "saguao",
		aliases = {"lobby"},
		usage = "req_queue-exit",
		category = Category.INFO
)
@Requires({
		Permission.MESSAGE_EMBED_LINKS,
		Permission.MESSAGE_ADD_REACTION
})
@SlashGroup("shoukan")
@SlashCommand(name = "lobby", args = {
		"{\"name\": \"sair\", \"description\": \"Deseja sair do saguão atual?\", \"type\": \"BOOLEAN\", \"required\": false}",
		"{\"name\": \"tipo\", \"description\": \"Tipo de saguão a ser exibido (SOLO/DUO)\", \"type\": \"STRING\", \"required\": false}"
})
public class LobbyCommand implements Executable, Slashed {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length > 0 && Helper.equalsAny(args[0], "sair", "exit")) {
			MatchMakingRating mmr = MatchMakingRatingDAO.getMMR(author.getId());
			MatchMaking mm = Main.getInfo().getMatchMaking();

			if (mm.getLobbyType(mmr) == LobbyType.SOLO) {
				mm.getSoloLobby().removeIf(sl -> sl.mmr().equals(mmr));

				if (mmr.getJoins() >= 3) {
					mmr.block((int) Math.pow(2, mmr.getJoins() - 2), ChronoUnit.MINUTES);
					MatchMakingRatingDAO.saveMMR(mmr);

					channel.sendMessage("Você saiu do saguão SOLO com sucesso.\n:no_entry_sign: | Devido a cancelamentos frequentes, você está bloqueado de entrar no saguão por %s.".formatted(Helper.toStringDuration(mmr.getRemainingBlock()))).queue();
				} else if (mmr.getJoins() == 2) {
					channel.sendMessage("Você saiu do saguão SOLO com sucesso.\n:warning: | O próximo cancelamento de fila resultará em bloqueio de saguão.").queue();
				} else {
					channel.sendMessage("Você saiu do saguão SOLO com sucesso.").queue();
				}
			} else if (mm.getLobbyType(mmr) == LobbyType.DUO) {
				mm.getDuoLobby().removeIf(rd -> rd.duo().p1().equals(mmr) || rd.duo().p2().equals(mmr));

				if (mmr.getJoins() >= 3) {
					mmr.block((int) Math.pow(2, mmr.getJoins() - 2), ChronoUnit.MINUTES);
					MatchMakingRatingDAO.saveMMR(mmr);

					channel.sendMessage("Você saiu do saguão DUO com sucesso.\n:no_entry_sign: | Devido a cancelamentos frequentes, você está bloqueado de entrar no saguão por %s.".formatted(Helper.toStringDuration(mmr.getRemainingBlock()))).queue();
				} else if (mmr.getJoins() == 2) {
					channel.sendMessage("Você saiu do saguão DUO com sucesso.\n:warning: | O próximo cancelamento de fila resultará em bloqueio de saguão.").queue();
				} else {
					channel.sendMessage("Você saiu do saguão DUO com sucesso.").queue();
				}
			} else {
				channel.sendMessage("❌ | Você não está em nenhum saguão.").queue();
				return;
			}
			return;
		} else if (args.length < 1 || !Helper.equalsAny(args[0], "solo", "duo")) {
			channel.sendMessage("❌ | Você precisa informar o tipo de fila que deseja entrar (`SOLO` ou `DUO`)").queue();
			return;
		}

		RankedQueue rq = RankedQueue.valueOf(args[0].toUpperCase(Locale.ROOT));
		List<Page> pages = new ArrayList<>();

		switch (rq) {
			case SOLO -> {
				List<List<SoloLobby>> lobby = Helper.chunkify(Main.getInfo().getMatchMaking().getSoloLobby(), 10);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle("Saguão do Shoukan ranqueado (%s | %s jogadores)".formatted(rq.name(),
								Main.getInfo().getMatchMaking().getSoloLobby().size()
						));

				StringBuilder sb = new StringBuilder();
				for (List<SoloLobby> chunk : lobby) {
					sb.setLength(0);
					for (SoloLobby sl : chunk)
						sb.append("%s | %s (%s)\n".formatted(
								sl.unlocked().get() ? "\\✅" : "\\❌",
								sl.mmr().getUser().getName(),
								sl.mmr().getTier().getName()
						));

					eb.setDescription(sb.toString());
					pages.add(new InteractPage(eb.build()));
				}

				if (pages.isEmpty()) {
					channel.sendMessage("❌ | Não há ninguém no saguão SOLO no momento.").queue();
					return;
				}

				channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
						Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
				);
			}
			case DUO -> {
				List<List<DuoLobby>> lobby = Helper.chunkify(Main.getInfo().getMatchMaking().getDuoLobby(), 10);

				EmbedBuilder eb = new ColorlessEmbedBuilder()
						.setTitle("Saguão do Shoukan ranqueado (%s | %s equipes)".formatted(rq.name(),
								Main.getInfo().getMatchMaking().getDuoLobby().size()
						));

				StringBuilder sb = new StringBuilder();
				for (List<DuoLobby> chunk : lobby) {
					sb.setLength(0);
					for (DuoLobby dl : chunk)
						sb.append("%s | %s (%s)\n".formatted(
								dl.unlocked().get() ? "\\✅" : "\\❌",
								dl.duo().p1().getUser().getName() + " | " + dl.duo().p2().getUser().getName(),
								RankedTier.getTierName(dl.duo().getAvgTier(), false)
						));

					eb.setDescription(sb.toString());
					pages.add(new InteractPage(eb.build()));
				}

				if (pages.isEmpty()) {
					channel.sendMessage("❌ | Não há ninguém no saguão DUO no momento.").queue();
					return;
				}

				channel.sendMessageEmbeds((MessageEmbed) pages.get(0).getContent()).queue(s ->
						Pages.paginate(s, pages, ShiroInfo.USE_BUTTONS, 1, TimeUnit.MINUTES, u -> u.getId().equals(author.getId()))
				);
			}
		}
	}

	@Override
	public String toCommand(SlashCommandEvent evt) {
		OptionMapping exit = evt.getOption("sair");
		OptionMapping type = evt.getOption("tipo");

		if (exit != null && exit.getAsBoolean()) {
			if (type != null)
				throw new ValidationException("❌ | Você não deve informar o tipo do saguão se deseja sair.");

			return "sair";
		} else {
			String tp = type == null ? "" : type.getAsString();
			if (!Helper.equalsAny(tp, "SOLO", "DUO"))
				throw new ValidationException("❌ | O tipo deve ser `SOLO` ou `DUO`.");

			return tp;
		}
	}
}
