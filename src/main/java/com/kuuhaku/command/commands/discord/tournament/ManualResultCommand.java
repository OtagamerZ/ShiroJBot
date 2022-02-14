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

package com.kuuhaku.command.commands.discord.tournament;

import com.github.ygimenez.method.Pages;
import com.kuuhaku.Main;
import com.kuuhaku.command.Category;
import com.kuuhaku.command.Executable;
import com.kuuhaku.controller.postgresql.TournamentDAO;
import com.kuuhaku.model.annotations.Command;
import com.kuuhaku.model.persistent.tournament.Participant;
import com.kuuhaku.model.persistent.tournament.Phase;
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Command(
		name = "definirresultado",
		aliases = {"setresult"},
		usage = "req_id-phase-winner",
		category = Category.SUPPORT
)
public class ManualResultCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do torneio.").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage("❌ | É necessário informar a ID da fase.").queue();
			return;
		} else if (args.length < 3) {
			channel.sendMessage("❌ | É necessário informar o índice do vencedor.").queue();
			return;
		}

		try {
			Tournament t = TournamentDAO.getTournament(Integer.parseInt(args[0]));

			int phase = Integer.parseInt(args[1]);
			if (phase == t.getBracket().getPhases().size() - 1) {
				int index = Integer.parseInt(args[2]);
				List<Participant> match = t.getTPMatch();
				if (!match.stream().allMatch(p -> p.getPhase() == phase)) {
					channel.sendMessage("❌ | Essa partida já foi encerrada.").queue();
					return;
				}

				User winner = Main.getInfo().getUserByID(match.get(index).getUid());
				String matchName = match.stream()
						.map(Participant::getUid)
						.map(Helper::getUsername)
						.collect(Collectors.joining(" VS "));

				channel.sendMessage("Você está prestes a definir `" + winner.getName() + "` como vencedor da partida `" + matchName + "`, deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									t.setTPResult(index);
									TournamentDAO.save(t);

									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("✅ | Resultado registrado com sucesso!").queue();
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			} else {
				Phase p = t.getPhase(phase);

				int index = Integer.parseInt(args[2]);
				Pair<Participant, Participant> match = p.getMatch(t, index);
				if (!Helper.equalsAll(phase, match.getLeft().getPhase(), match.getRight().getPhase())) {
					channel.sendMessage("❌ | Essa partida já foi encerrada.").queue();
					return;
				}

				User winner = Main.getInfo().getUserByID(p.getParticipants(t).get(index).getUid());
				String matchName = Arrays.stream(new String[]{match.getLeft().getUid(), match.getRight().getUid()})
						.map(s -> s.equals("BYE") ? "_" + s + "_" : Helper.getUsername(s))
						.collect(Collectors.joining(" VS "));

				channel.sendMessage("Você está prestes a definir `" + winner.getName() + "` como vencedor da partida `" + matchName + "`, deseja confirmar?").queue(
						s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
									t.setResult(phase, index);
									TournamentDAO.save(t);

									s.delete().queue(null, Helper::doNothing);
									channel.sendMessage("✅ | Resultado registrado com sucesso!").queue();
								}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
								, u -> u.getId().equals(author.getId())
						), Helper::doNothing
				);
			}
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			channel.sendMessage("❌ | Índice inválido.").queue();
		}
	}
}
