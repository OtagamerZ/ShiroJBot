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
import com.kuuhaku.model.persistent.tournament.Tournament;
import com.kuuhaku.utils.Helper;
import com.kuuhaku.utils.ShiroInfo;
import net.dv8tion.jda.api.entities.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(
		name = "definirwo",
		aliases = {"setwo"},
		usage = "req_tournament-id",
		category = Category.SUPPORT
)
public class DeclareWoCommand implements Executable {

	@Override
	public void execute(User author, Member member, String argsAsText, String[] args, Message message, TextChannel channel, Guild guild, String prefix) {
		if (args.length < 1) {
			channel.sendMessage("❌ | É necessário informar o ID do torneio.").queue();
			return;
		} else if (args.length < 2) {
			channel.sendMessage("❌ | É necessário informar a ID do participante.").queue();
			return;
		}

		Tournament t = TournamentDAO.getTournament(Integer.parseInt(args[0]));
		Participant p = t.getPartLookup().get(args[1]);
		if (p == null) {
			channel.sendMessage("❌ | Não há nenhum participante com esse ID.").queue();
			return;
		}

		channel.sendMessage("Você está prestes a definir que `" + Helper.getUsername(args[1]) + "` não compareceu ao torneio `" + t.getName() + "`, deseja confirmar?").queue(
				s -> Pages.buttonize(s, Map.of(Helper.parseEmoji(Helper.ACCEPT), wrapper -> {
							t.getParticipants().remove(p);
							if (!t.getBench().isEmpty()) {
								Participant sub = new Participant(new ArrayList<>(t.getBench()).get(0), t);
								sub.setIndex(p.getIndex());
								sub.setPoints(p.getPoints());

								t.getParticipants().add(sub);
								t.getBench().remove(args[1]);
								try {
									Main.getInfo().getUserByID(sub.getUid()).openPrivateChannel()
											.flatMap(c -> c.sendMessage("Um dos participantes não compareceu, você foi adicionado às chaves."))
											.queue(null, Helper::doNothing);
								} catch (RuntimeException ignore) {
								}
							}

							TournamentDAO.save(t);

							s.delete().queue(null, Helper::doNothing);
							channel.sendMessage("✅ | W.O. registrado com sucesso!").queue();
						}), ShiroInfo.USE_BUTTONS, true, 1, TimeUnit.MINUTES
						, u -> u.getId().equals(author.getId())
				), Helper::doNothing
		);
	}
}
